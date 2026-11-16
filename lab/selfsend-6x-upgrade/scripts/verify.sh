#!/usr/bin/env bash
# Final checks of the lab, after the upgrade.
#  A. backend ledger: every message the backend sent (6.x and 7.1.0) came back exactly once, with the
#     same content, and has its RETRIEVAL evidence
#  B. 7.1.0 database: every 6.x message is there, the ebMS ids shared by the sent and received copy were
#     kept unchanged (no "#6x-" suffix), and every outbound message is CONFIRMED
#  C. migration report: no DUPLICATE_EBMS_ID finding, 0 errors; verify passed
#  D. dead letters on the 7.1.0 broker
set -uo pipefail
LAB="$(cd "$(dirname "$0")/.." && pwd)"
cd "$LAB"
fail=0
sql7() { docker compose exec -T connector7-db mysql -uconnector -pconnector connector -e "$1" 2>/dev/null; }

echo "## A. backend ledger (all tags)"
PHASE=7x ./scripts/backend.sh verify t1,t2,t3,t4,t5 2>/dev/null | grep -v '^Picked up' || fail=1

echo
echo "## B. connector 7.1.0 database"
sql7 "select direction, status, count(*) n from connector_messages group by direction, status order by 1,2;"
sql7 "select count(*) as ebms_ids_shared_by_sent_and_received_copy from (select a.ebms_message_identifier
        from connector_message_as4_properties a join connector_messages m on m.id=a.message_id
        group by a.ebms_message_identifier having count(distinct m.direction)=2) x;"
suffixed=$(sql7 "select count(*) from connector_message_as4_properties where ebms_message_identifier like '%#6x-%'" -N | tr -dc 0-9)
echo "ebMS ids changed by the migration (#6x- suffix): $suffixed"; [ "${suffixed:-1}" = 0 ] || fail=1
open=$(sql7 "select count(*) from connector_messages where direction='BACKEND_TO_GATEWAY' and status<>'CONFIRMED' and backend_message_identifier like 'backend-t%'" -N | tr -dc 0-9)
echo "lab messages (BACKEND_TO_GATEWAY) not CONFIRMED: $open"; [ "${open:-1}" = 0 ] || fail=1
sql7 "select m.direction, m.status, m.backend_message_identifier, a.ebms_message_identifier,
        group_concat(e.type order by e.id) evidences
      from connector_messages m join connector_message_as4_properties a on a.message_id=m.id
      left join connector_message_evidences e on e.message_id=m.id
      where m.backend_message_identifier like 'backend-t%' or m.backend_message_identifier like 'inbound-t%'
         or m.direction='GATEWAY_TO_BACKEND'
      group by m.id order by a.ebms_message_identifier, m.direction;"

echo
echo "## C. migration report"
if [ -f work/migration/migrate/migration-report.json ]; then
  python3 - <<'PY' || fail=1
import json, sys
r = json.load(open("work/migration/migrate/migration-report.json"))
counters = r.get("counters", {})
codes = r.get("issueCounts", {})
print("outcome:", r.get("outcome"))
print("messages migrated:", counters.get("messages.migrated"), " source messages:", counters.get("source.Message"))
for k in sorted(codes): print(" ", k, codes[k])
bad = [k for k in codes if k.endswith("DUPLICATE_EBMS_ID") or k.startswith("ERROR")]
if bad or counters.get("messages.migrated") != counters.get("source.Message"):
    print("FAIL:", bad); sys.exit(1)
PY
fi
if [ -f work/migration/verify.log ]; then
  echo "migration tool verify:"; grep -E "INFO|WARN|ERROR|exit code" work/migration/verify.log | tail -8
  grep -q "verify exit code 0" work/migration/verify.log || fail=1
fi
echo
echo "## D. dead letters on the broker"
curl -s -u admin:admin -H "Origin: http://localhost" \
  "http://localhost:8161/api/jolokia/search/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=*DLQ*" \
  | python3 -c 'import sys,json; v=json.load(sys.stdin).get("value",[]); print(v if v else "no DLQ")'

echo
[ $fail = 0 ] && echo "RESULT: PASS - no message lost" || echo "RESULT: FAIL - see above"
exit $fail
