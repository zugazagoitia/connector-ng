#!/usr/bin/env bash
# Runs the lab backend application (built by scripts/build-backend.sh).
#   PHASE=6x -> connector 6.1.0 at http://localhost:9081/services/backend
#   PHASE=7x -> connector 7.1.0 at http://localhost:30000/services/backend
set -euo pipefail
LAB="$(cd "$(dirname "$0")/.." && pwd)"
PHASE="${PHASE:-6x}"
if [ "$PHASE" = 6x ]; then URL=http://localhost:9081/services/backend; else URL=http://localhost:30000/services/backend; fi
export PHASE CONNECTOR_URL="${CONNECTOR_URL:-$URL}" KEYSTORE="$LAB/work/backend/backend-keystore.jks" LEDGER="${LEDGER:-$LAB/work/backend/ledger.json}"
exec java -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -cp "$LAB/backend/target/selfsend-backend.jar:$LAB/backend/target/lib/*" lab.SelfSendBackend "$@"
