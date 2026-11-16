# Lab: duplicate ebMS ids across the 6.1.0 → 7.1.0 upgrade (self-sending connector)

One gateway and one connector. The P-Mode makes party `service_blue_ecodex` both initiator and responder, so every message
the backend submits goes connector → gateway → same gateway → connector → backend. The sent copy and the received copy of
a message carry **the same ebMS id** (one per direction). The lab upgrades the connector from 6.1.0 to 7.1.0 with the
migration tool in the middle of the exchange, following the cut-over procedure of the installation guide, and checks that
no message is lost.

Results of the last run: [REPORT.md](REPORT.md).

## Components

| Service | Image | Role |
|---|---|---|
| `gateway` | `scm.ecodex.eu/docker/gateway:edge` (Domibus 5.1.9) | e-CODEX connector push plugin 4.6.0 (6.x link, from the image) and the default JMS plugin (7.x link) |
| `gateway-db`, `activemq` | `mysql:8.4`, `apache/activemq-classic:6.2.0` | Domibus database and broker |
| `connector6`, `connector6-db` | `scm.ecodex.eu/docker/connector:6.1.0`, `mariadb:10.11` | connector 6.1.0: gateway link `gwwspushplugin`, backend link `wsbackendplugin`, payloads in the database |
| `connector7`, `connector7-db`, `minio` | `scm.ecodex.eu/docker/connector-ng:7.1.0`, `mysql:8.4`, MinIO | connector 7.1.0 after the migration |
| backend (host) | `backend/` (Java 21, CXF) | national backend `alice` in pull mode, SOAP with WS-Security; keeps a ledger of everything sent and received |
| migration tool (host) | `connector-db-migration` fat jar | `flush-status`, `analyze`, `migrate`, `verify` |

The connector and gateway images are pulled from `scm.ecodex.eu`, nothing is built locally except the small backend app.
Keystores come from the 7.1.0 image (`connector_blue`, `alice`) and the connector-ng gateway sample (`gw_blue`);
`scripts/prepare.sh` assembles them in `work/`.

## Run

Requirements: Docker with compose v2, JDK 21, Maven, and the migration tool jar
(`./gradlew :migration-cli:shadowJar` in `connector-db-migration`).

```bash
MIGRATION_JAR=/path/to/connector-db-migration-<version>-all.jar ./run-lab.sh     # about 15 minutes, from scratch
FROM=9 ./run-lab.sh          # only the 7.1.0 part again, on the migrated data
./scripts/verify.sh          # the final checks
docker compose down -v       # clean up
```

Ports on the host: gateway 30010 (`/domibus`, admin/123456), connector 6.x 9081, connector 7.1.0 30000, ActiveMQ console 8161,
MinIO 9000, 6.x database 33306, 7.1.0 database 33307.

## Scenario

| Tag | Before the upgrade (6.1.0) | During the cut-over | After the upgrade (7.1.0) |
|---|---|---|---|
| t1 | sent, copy received, DELIVERY, RETRIEVAL | | (finished history) |
| t3 | sent, copy received, DELIVERY | | backend sends RETRIEVAL for the migrated copy; the evidence must reach the migrated sent message |
| t4 | sent, copy received, DELIVERY | gateway switched to the JMS plugin, then RETRIEVAL sent by 6.x; the evidence waits in ActiveMQ | 7.1.0 consumes it and must match it to the migrated sent message |
| t5 | | | sent, received, DELIVERY, RETRIEVAL on 7.1.0 |
| t2 | sent, copy received by 6.x but **not pulled** by the backend (against the procedure) | | pulled from 7.1.0, DELIVERY, RETRIEVAL (edge case, reported separately) |

The cut-over follows `CUTOVER.md`: message filter of Domibus switched to the JMS plugin, `flush-status`, 6.x stopped,
`analyze`, `migrate`, `verify`, 7.1.0 started with the generated `application.properties`.

## What is checked (`scripts/verify.sh`)

* Backend ledger: every message sent in t1, t3, t4 and t5 came back exactly once, with the same XML content and the same
  document bytes, and the backend received its RETRIEVAL evidence.
* 7.1.0 database: the ebMS ids shared by the two copies are unchanged (no `#6x-` suffix), every outbound lab message is
  confirmed.
* Migration report: all messages migrated, no `DUPLICATE_EBMS_ID`, no error; `verify` of the tool passed.
* Dead letters on the broker and acknowledgements the backend could not make.
