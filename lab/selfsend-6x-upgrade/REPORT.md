# Report: duplicate ebMS ids across the 6.1.0 → 7.1.0 upgrade

**Date:** 2026-11-16 · **Lab:** [README.md](README.md) · **Run:** `./run-lab.sh`, from scratch, one shot, exit code 0

## Question

A connector that sends messages to itself (one gateway, one connector) stores every message twice with the **same ebMS id**:
the copy it sent (BACKEND → GATEWAY) and the copy it received (GATEWAY → BACKEND). Does the upgrade from connector 6.1.0 to
7.1.0 with the migration tool keep these messages, and does 7.1.0 keep working with them, without losing any message?

## Answer

**Yes.** The duplicate ebMS ids migrate unchanged, 7.1.0 matches every evidence to the right copy, and no message was lost,
also for messages that were still waiting for the backend during the cut-over.

This needs the migration tool with the fixes of branch `claude/duplicate-ebms-ids-migration-swf0gi` of `connector-db-migration`.
The first runs, with the tool before these fixes, lost the document of the messages still waiting for the backend and blocked
their evidences (finding 1). Messages handled as the cut-over procedure says passed with both versions.

## Setup

* Images from `scm.ecodex.eu`: `connector:6.1.0`, `connector-ng:7.1.0`, `gateway:edge` (Domibus 5.1.9). The e-CODEX
  connector plugin 4.6.0 (push) shipped in the gateway image is configured for 6.x, and the default JMS plugin for 7.x.
* P-Mode: party `service_blue_ecodex` is initiator and responder, so every message comes back to the same connector.
  Domibus keeps the same message id for the sent and the received message.
* A small backend application (`alice`, SOAP with WS-Security, pull mode) sends messages to itself, downloads the copies,
  sends DELIVERY and RETRIEVAL evidences, and records everything to check it at the end.
* Migration tool `connector-db-migration`: 6.x on MariaDB 10.11 → 7.1.0 on MySQL 8.4 + MinIO.

## Results

| Scenario | What happens | Result |
|---|---|---|
| t1 (3 messages) | Finished in 6.x, then migrated | ✅ confirmed in 7.1.0, history complete |
| t3 (3 messages) | Received in 6.x; RETRIEVAL sent to **7.1.0** after the upgrade | ✅ evidence matched to the migrated sent copy, backend got it |
| t4 (3 messages) | RETRIEVAL sent during the cut-over; the evidence waited in ActiveMQ and was consumed by **7.1.0** | ✅ evidence matched to the migrated sent copy, backend got it |
| t5 (3 messages) | Sent and finished on 7.1.0 after the upgrade (new duplicate ids created by 7.1.0) | ✅ confirmed |
| t2 (3 messages) | Received by 6.x, **not downloaded by the backend before the cut-over** (against the procedure) | ✅ delivered by 7.1.0 with identical XML and document, DELIVERY and RETRIEVAL received |

Checks (`scripts/verify.sh`):

* Backend: 15 of 15 messages came back exactly once with identical content and have their RETRIEVAL evidence. Every
  evidence could be acknowledged; nothing in the dead letter queue.
* Migration: 24 of 24 messages migrated, 0 errors, no `DUPLICATE_EBMS_ID` finding. The tool's `verify`: 24/24 messages,
  27/27 files in S3, 102/102 transport steps readable.
* 7.1.0 database: 15 ebMS ids are shared by a sent and a received copy, **none was changed** by the migration. Every sent
  message is confirmed.

## Findings

1. **Migration tool: deliveries still waiting for a pull backend at the cut-over** (fixed on the tool branch).
   * The received message was delivered **without its business document**: 6.x kept the extracted document (and the PDF
     token) in `DOMIBUS_CONNECTOR_BIGDATA` rows that only the pending transport step refers to. The tool now migrates them for
     messages 6.x had not delivered yet (`CONTENT_FROM_TRANSPORT_STEP`). Delivered messages are left as 6.x cleaned them.
   * Evidences waiting for the backend **could not be acknowledged**: 7.1.0 answered `acknowledgeMessage` with an error
     (`NullPointerException: "evidence" is null`) and offered them again forever, because the tool gave them new ids. The tool
     now reuses the id of the migrated evidence row.
   * 7.1.0 itself should not fail when such a row is missing (reported for connector-ng).
2. **Migration tool: the draft `application.properties` contains the database URL and S3 endpoint the tool used** (here
   `localhost:33307`, `localhost:9000`). They now carry a `# REVIEW` marker. When 7.1.0 runs elsewhere (here: in a container)
   they must be changed; the lab does it in step 9.
3. **Expected behaviour, not a defect:** in pull mode, 7.1.0 does not create the DELIVERY evidence by itself
   (`auto-trigger-delivery-evidences` acts on push delivery only). The backend sends DELIVERY before RETRIEVAL, as in 6.x.

The findings are also in the developer report (`MIGRATION-TOOL-REPORT.md`).
