#!/usr/bin/env bash
# Small Domibus admin REST helper: login, P-Mode upload, message filter routing.
#   domibus.sh upload-pmode FILE
#   domibus.sh filters                    print the message filter
#   domibus.sh route PLUGIN               route every message to PLUGIN (DC_PUSH_PLUGIN or Jms)
#   domibus.sh messages                   list the last messages (id, role, status)
set -euo pipefail
GW="${GW_URL:-http://localhost:30010/domibus}"
JAR="$(mktemp)"; trap 'rm -f "$JAR"' EXIT
login() {
  for i in $(seq 1 60); do
    if curl -sf -c "$JAR" -b "$JAR" -H 'Content-Type: application/json' -X POST "$GW/rest/security/authentication" \
         -d '{"username":"admin","password":"123456"}' >/dev/null; then break; fi
    sleep 5
  done
  XSRF=$(awk '/XSRF-TOKEN/{print $7}' "$JAR")
}
# Domibus prefixes JSON responses with )]}', (JSON hijacking protection)
call() { curl -sf -c "$JAR" -b "$JAR" -H "X-XSRF-TOKEN: $XSRF" "$@" | sed "1s/^)]}',\$//"; }
login
case "$1" in
  upload-pmode)
    call -F "file=@$2" -F "description=lab self-send" "$GW/rest/pmode"; echo ;;
  filters)
    call "$GW/rest/messagefilters" | python3 -c 'import sys,json; d=json.load(sys.stdin); [print(f["index"] if "index" in f else "-", f["backendName"], f.get("routingCriterias")) for f in d.get("messageFilterEntries",d)]' ;;
  route)
    call "$GW/rest/messagefilters" > /tmp/filters.$$.json
    python3 - "$2" /tmp/filters.$$.json > /tmp/filters.$$.new <<'PY'
import sys, json
want, path = sys.argv[1], sys.argv[2]
d = json.load(open(path))
entries = d["messageFilterEntries"] if isinstance(d, dict) else d
entries.sort(key=lambda f: 0 if f["backendName"] == want else 1)
for i, f in enumerate(entries):
    f["index"] = i + 1
print(json.dumps(entries))
PY
    call -X PUT -H 'Content-Type: application/json' --data @/tmp/filters.$$.new "$GW/rest/messagefilters" >/dev/null
    rm -f /tmp/filters.$$.json /tmp/filters.$$.new
    "$0" filters ;;
  messages)
    call "$GW/rest/messagelog?page=0&pageSize=${2:-50}&orderBy=received&asc=false" | python3 -c '
import sys,json
d=json.load(sys.stdin)
for m in d["messageLogEntries"]:
    print(m.get("messageId"), m.get("mshRole"), m.get("messageStatus"), m.get("action"), m.get("backend") or "")' ;;
  *) echo "unknown command"; exit 64 ;;
esac
