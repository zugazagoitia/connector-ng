#!/usr/bin/env bash
# One-shot lab: a backend that sends every message to itself through ONE connector and ONE gateway,
# so that the outbound and the inbound copy of each message share the same ebMS id. The connector
# is upgraded from 6.1.0 to 7.1.0 with the database migration tool in the middle of the exchange,
# following the cut-over procedure of the installation guide, and the lab then checks that no
# message was lost.
#
# Requirements: Docker (compose v2), JDK 21 + Maven (backend app), the migration tool fat jar
# (MIGRATION_JAR, built with `./gradlew :migration-cli:shadowJar` in connector-db-migration).
#
# Scenario (tags of the backend messages):
#   t1  sent, received, DELIVERY + RETRIEVAL, all in 6.x                     -> finished before the upgrade
#   t3  sent, received, DELIVERY in 6.x; RETRIEVAL triggered on 7.1.0        -> evidence crosses the upgrade
#   t4  sent, received, DELIVERY in 6.x; RETRIEVAL triggered on 6.x AFTER the gateway was switched to the
#       JMS plugin: the RETRIEVAL evidence waits in ActiveMQ and is consumed by 7.1.0 after the upgrade
#   t2  sent and received by 6.x, but the backend never pulled them before the cut-over
#       -> must still be delivered to the backend by 7.1.0
#   t5  sent and finished entirely on 7.1.0 (self-send also works after the upgrade)
set -euo pipefail
LAB="$(cd "$(dirname "$0")" && pwd)"
cd "$LAB"
MIGRATION_JAR="${MIGRATION_JAR:-$LAB/../../../connector-db-migration/migration-cli/build/libs/connector-db-migration-1.0.0-SNAPSHOT-all.jar}"
TZ6=Europe/Vienna
N="${N:-3}"
WAIT="${WAIT:-300}"
LOG="$LAB/work/run.log"

step() { printf '\n\033[1;34m=== %s\033[0m\n' "$*" | tee -a "$LOG"; }
be6() { PHASE=6x ./scripts/backend.sh "$@" 2>&1 | grep -E '^\[backend|TIMEOUT|Exception|^sent|^backend-|checked' | tee -a "$LOG"; return "${PIPESTATUS[0]}"; }
be7() { PHASE=7x ./scripts/backend.sh "$@" 2>&1 | grep -E '^\[backend|TIMEOUT|Exception|^sent|^backend-|checked' | tee -a "$LOG"; return "${PIPESTATUS[0]}"; }
mig() { java -jar "$MIGRATION_JAR" "$@" 2>&1 | grep -v '^Picked up JAVA_TOOL_OPTIONS' | tee -a "$LOG"; return "${PIPESTATUS[0]}"; }
sql6() { docker compose exec -T connector6-db mariadb -uconnector -pconnector connector6 -e "$1"; }
queue_size() {
  curl -s -u admin:admin -H "Origin: http://localhost" \
    "http://localhost:8161/api/jolokia/read/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=$1/QueueSize" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin).get("value",0))'
}
wait_log() { # service, regex, seconds
  for _ in $(seq 1 $(( $3 / 5 ))); do docker compose logs "$1" 2>&1 | grep -qE "$2" && return 0; sleep 5; done
  echo "timeout waiting for $1: $2"; docker compose logs --tail 80 "$1"; return 1
}
SOURCE_OPTS=(--legacy-config work/connector6/config/connector.properties --legacy-home work/connector6
             --source-url jdbc:mariadb://localhost:33306/connector6 --source-user connector --source-password=connector
             --source-timezone "$TZ6")
TARGET_OPTS=(--target-url 'jdbc:mysql://localhost:33307/connector' --target-user connector --target-password=connector
             --s3-endpoint http://localhost:9000 --s3-region us-east-1 --s3-bucket attachments
             --s3-access-key admin --s3-secret-key=admin123)

[ -f "$MIGRATION_JAR" ] || { echo "migration tool not found: $MIGRATION_JAR (set MIGRATION_JAR)"; exit 1; }

step "0. clean start: remove containers, volumes and ./work, prepare configuration, build the backend"
docker compose down -v --remove-orphans >/dev/null 2>&1 || true
./scripts/prepare.sh
: > "$LOG"
(cd backend && mvn -q -B package -DskipTests)

step "1. start ActiveMQ, Domibus 5.1.9 (connector plugin + JMS plugin) and connector 6.1.0"
docker compose up -d activemq gateway-db connector6-db
docker compose up -d gateway
wait_log gateway "Server startup in" 600
./scripts/domibus.sh upload-pmode gateway/pmode-selfsend.xml | tee -a "$LOG"
./scripts/domibus.sh route DC_PUSH_PLUGIN | tee -a "$LOG"
docker compose up -d connector6
wait_log connector6 "Started DomibusConnectorStarter in [0-9]{2}" 600

step "2. 6.x: send t1,t3,t4 to ourselves, pull the copies, DELIVERY for all, RETRIEVAL for t1"
be6 submit t1 "$N"; be6 submit t3 "$N"; be6 submit t4 "$N"
be6 await-received t1,t3,t4 "$WAIT"
for t in t1 t3 t4; do be6 trigger DELIVERY "$t"; done
be6 await-evidence DELIVERY t1,t3,t4 "$WAIT"
be6 trigger RETRIEVAL t1
be6 await-evidence RETRIEVAL t1 "$WAIT"

step "3. 6.x: send t2 to ourselves; the backend does not pull them before the cut-over"
be6 submit t2 "$N"
for _ in $(seq 1 60); do   # 4N copies received from the gateway: t1, t3, t4 and t2
  n=$(sql6 "select count(*) from DOMIBUS_CONNECTOR_MESSAGE where DIRECTION_TARGET='BACKEND'" -N 2>/dev/null | tr -dc 0-9)
  [ "${n:-0}" -ge $((4 * N)) ] && break; sleep 5
done
sleep 20   # let the RELAY_REMMD evidences of t2 arrive as well

step "4. 6.x state before the cut-over: messages that share an ebMS id (sent copy + received copy)"
sql6 "select DIRECTION_TARGET, count(*) from DOMIBUS_CONNECTOR_MESSAGE group by DIRECTION_TARGET;
      select count(*) as ebms_ids_shared_by_both_directions from (select EBMS_MESSAGE_ID from DOMIBUS_CONNECTOR_MESSAGE
        where EBMS_MESSAGE_ID is not null group by EBMS_MESSAGE_ID having count(distinct DIRECTION_TARGET)=2) x;" | tee -a "$LOG"

step "5. cut-over (CUTOVER.md section 3): switch the Domibus message filter to the JMS plugin"
./scripts/domibus.sh route Jms | tee -a "$LOG"

step "6. 6.x still running: RETRIEVAL for t4 -> the evidence returns through the gateway into the JMS plugin queue"
be6 trigger RETRIEVAL t4
for _ in $(seq 1 60); do q=$(queue_size domibus.backend.jms.outQueue); [ "${q:-0}" -ge "$N" ] && break; sleep 5; done
echo "domibus.backend.jms.outQueue holds $(queue_size domibus.backend.jms.outQueue) message(s) waiting for 7.1.0" | tee -a "$LOG"

step "7. flush-status: everything except the t2 deliveries the backend has not pulled must be finished"
set +e
mig flush-status "${SOURCE_OPTS[@]}" --list; rc=$?
set -e
echo "flush-status exit code $rc (2 expected: t2 messages and evidences wait for the pull backend; decision: migrate them in their state)" | tee -a "$LOG"

step "8. stop 6.x, start the 7.1.0 database and MinIO, analyze, migrate, verify"
docker compose stop connector6
docker compose up -d minio connector7-db
for _ in $(seq 1 60); do docker compose exec -T connector7-db mysqladmin ping -uroot -pconnector >/dev/null 2>&1 && break; sleep 3; done
sleep 5
mig analyze "${SOURCE_OPTS[@]}" --report-dir work/migration/analyze
mig migrate "${SOURCE_OPTS[@]}" "${TARGET_OPTS[@]}" --report-dir work/migration/migrate
mig verify "${SOURCE_OPTS[@]}" "${TARGET_OPTS[@]}" --report-dir work/migration/verify

step "9. configure and start connector 7.1.0 with the generated application.properties"
cp work/migration/migrate/application.properties work/connector7/config/application.properties
cat > work/connector7/connector7.env <<EOF
DB_URL=jdbc:mysql://connector7-db:3306/connector?pinGlobalTxToPhysicalConnection=true
DB_DRIVER_CLASSNAME=com.mysql.cj.jdbc.MysqlXADataSource
DB_DIALECT=org.hibernate.dialect.MySQLDialect
DB_USERNAME=connector
DB_PASSWORD=connector
JWT_SECRET_KEY=$(head -c 48 /dev/urandom | base64 | tr -d '\n/+=' | head -c 48)
S3_ACCESS_KEY=admin
S3_SECRET_KEY=admin123
S3_REGION=us-east-1
S3_BUCKET=attachments
S3_ENDPOINT=http://minio:9000
TEMP_DIR_LOCATION=/app/temp
BROKER_URL=tcp://activemq:61616
BROKER_USER=admin
BROKER_PASSWORD=admin
EOF
chown -R 1000:1000 work/connector7 2>/dev/null || true
docker compose up -d connector7
wait_log connector7 "Started .* in [0-9.]+ seconds|APPLICATION FAILED TO START" 600
docker compose logs connector7 2>&1 | grep -q "APPLICATION FAILED TO START" && { docker compose logs --tail 60 connector7; exit 1; }
sleep 20
echo "domibus.backend.jms.outQueue after the start of 7.1.0: $(queue_size domibus.backend.jms.outQueue)" | tee -a "$LOG"

step "10. 7.1.0: t4 RETRIEVAL evidences (queued during the cut-over) must reach the backend"
be7 await-evidence RETRIEVAL t4 "$WAIT" || true

step "11. 7.1.0: the backend pulls the t2 messages 6.x never delivered, and the open evidences"
be7 await-received t2 "$WAIT" || true

step "12. 7.1.0: RETRIEVAL for t3 (received in 6.x) and t2 (received now)"
be7 trigger RETRIEVAL t3 || true
be7 trigger RETRIEVAL t2 || true
be7 await-evidence RETRIEVAL t3,t2 "$WAIT" || true

step "13. 7.1.0: new self-sent messages t5"
be7 submit t5 "$N" || true
be7 await-received t5 "$WAIT" || true
be7 trigger RETRIEVAL t5 || true
be7 await-evidence RETRIEVAL t5 "$WAIT" || true

step "14. verification"
./scripts/verify.sh | tee -a "$LOG"
