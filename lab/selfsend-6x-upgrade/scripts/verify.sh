#!/usr/bin/env bash
# Final checks of the lab, after the upgrade.
#  A. backend ledger, scenarios that follow the cut-over procedure (t1, t3, t4, t5): every message the
#     backend sent came back exactly once with the same content, and has its RETRIEVAL evidence  -> must pass
#  A2. edge case t2 (copies not pulled before the cut-over, against the procedure)          -> reported only
#  B. 7.1.0 database: the ebMS ids shared by the sent and received copy were kept (no "#6x-" suffix) and every
#     outbound lab message of the procedure scenarios is confirmed
#  C. migration report: no DUPLICATE_EBMS_ID finding, no ERROR, all messages migrated; tool verify passed
#  D. dead letters and acknowledgements the backend could not make
set -uo pipefail
LAB="$(cd "$(dirname "$0")/.." && pwd)"
cd "$LAB"
fail=0
sql7() { docker compose exec -T connector7-db mysql -uconnector -pconnector connector -e "$1" 2>/dev/null; }

echo "## A. backend ledger: scenarios that follow the cut-over procedure (t1, t3, t4, t5)"
PHASE=7x ./scripts/backend.sh verify t1,t3,t4,t5 2>/dev/null | grep -v '^Picked up' || fail=1

echo
echo "## A2. edge case t2: copies still waiting for the pull backend at the cut-over (reported, not part of the verdict)"
PHASE=7x ./scripts/backend.sh verify t2 2>/dev/null | grep -v '^Picked up'

echo
echo "## B. connector 7.1.0 database"
sql7 "select direction, status, count(*) n from connector_messages group by direction, status order by 1,2;"
sql7 "select count(*) as ebms_ids_shared_by_sent_and_received_copy from (select a.ebms_message_identifier
        from connector_message_as4_properties a join connector_messages m on m.id=a.message_id
        group by a.ebms_message_identifier having count(distinct m.direction)=2) x;"
suffixed=$(sql7 "select count(*) from connector_message_as4_properties where ebms_message_identifier like '%#6x-%'" -N | tr -dc 0-9)
echo "ebMS ids changed by the migration (#6x- suffix): $suffixed"; [ "${suffixed:-1}" = 0 ] || fail=1
# 7.1.0 sets status DELETED when it removes the files of a confirmed message; confirmed_at stays
open=$(sql7 "select count(*) from connector_messages where direction='BACKEND_TO_GATEWAY' and confirmed_at is null
               and backend_message_identifier regexp '^backend-t[1345]-'" -N | tr -dc 0-9)
echo "outbound lab messages t1,t3,t4,t5 without confirmed_at: $open"; [ "${open:-1}" = 0 ] || fail=1
sql7 "select m.direction, m.status, m.confirmed_at is not null confirmed, m.backend_message_identifier, a.ebms_message_identifier,
        group_concat(e.type order by e.id) evidences
      from connector_messages m join connector_message_as4_properties a on a.message_id=m.id
      left join connector_message_evidences e on e.message_id=m.id
      group by m.id order by a.ebms_message_identifier, m.direction;"

echo
echo "## C. migration report and tool verify"
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
  grep -E "INFO  (Messages|S3|Transport|Verification)|exit code" work/migration/verify.log | tail -6
  grep -q "verify exit code 0" work/migration/verify.log || fail=1
fi

echo
echo "## D. dead letters and failed acknowledgements"
for q in ActiveMQ.DLQ; do
  n=$(curl -s -u admin:admin -H "Origin: http://localhost" \
    "http://localhost:8161/api/jolokia/read/org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=$q/QueueSize" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin).get("value",0))' 2>/dev/null)
  echo "$q: ${n:-0} message(s)"
done
python3 - <<'PY'
import json
l = json.load(open("work/backend/ledger.json"))
f = l.get("failedAcks", {})
print(f"transports the backend could not download/acknowledge: {len(f)}")
for k, v in f.items(): print("  ", k, "->", v[:150])
PY

echo
[ $fail = 0 ] && echo "RESULT: PASS - no message lost in the procedure scenarios (t1, t3, t4, t5); see A2/D for the t2 edge case" \
              || echo "RESULT: FAIL - see above"
exit $fail
