# Report: duplicate ebMS ids across the 6.1.0 → 7.1.0 upgrade

**Date:** 2026-11-16 · **Lab:** [README.md](README.md) · **Run:** `./run-lab.sh`, from scratch, one shot, exit code 0

## Question

A connector that sends messages to itself (one gateway, one connector) stores every message twice with the **same ebMS id**:
the copy it sent (BACKEND → GATEWAY) and the copy it received (GATEWAY → BACKEND). Does the upgrade from connector 6.1.0 to
7.1.0 with the migration tool keep these messages, and does 7.1.0 keep working with them, without losing any message?

## Answer

**Yes, when the cut-over procedure of the installation guide is followed.** The duplicate ebMS ids migrate unchanged,
7.1.0 matches evidences to the right copy, and no message was lost.

**One exception:** messages and evidences that still wait for a backend in pull mode when 6.x is stopped. They are migrated,
but 7.1.0 does not deliver them correctly (details below). The procedure already requires to let pull backends fetch
everything first. The guide now says this explicitly.

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
| t1 (3 messages) | Finished in 6.x, then migrated | ✅ all 3 confirmed in 7.1.0, history complete |
| t3 (3 messages) | Received in 6.x; RETRIEVAL sent to **7.1.0** after the upgrade | ✅ evidence matched to the migrated sent copy, backend got it |
| t4 (3 messages) | RETRIEVAL sent during the cut-over; the evidence waited in ActiveMQ and was consumed by **7.1.0** | ✅ evidence matched to the migrated sent copy, backend got it |
| t5 (3 messages) | Sent and finished on 7.1.0 after the upgrade (new duplicate ids created by 7.1.0) | ✅ all 3 confirmed |
| t2 (3 messages) | Received by 6.x, **not downloaded by the backend before the cut-over** (against the procedure) | ⚠️ message and evidences arrive, but **without the business document**; see finding 1 |

Checks (`scripts/verify.sh`):

* Backend: 12 of 12 messages (t1, t3, t4, t5) came back exactly once with identical content and have their RETRIEVAL evidence.
* Migration: 24 of 24 messages migrated, 0 errors, no `DUPLICATE_EBMS_ID` finding. The tool's `verify`: 24/24 messages,
  21/21 files in S3, 102/102 transport steps readable.
* 7.1.0 database: 15 ebMS ids are shared by a sent and a received copy, **none was changed** by the migration. Every sent
  message of t1, t3, t4 and t5 is confirmed.
* No message in the dead letter queue.

## Findings

1. **Migration tool: deliveries still waiting for a pull backend are not usable after the upgrade.** Seen only for messages and
   evidences that 6.x had ready for the backend when it was stopped (t2, plus three RETRIEVAL notices about received t4 copies).
   * The received message is delivered **without its business document**. 6.x kept the extracted document in a
     `DOMIBUS_CONNECTOR_BIGDATA` row that only the pending transport step refers to, and the tool does not copy it. The
     document is still inside the migrated ASiC-S container, so nothing is lost for good, but the backend does not get it.
   * Evidences waiting for the backend **cannot be acknowledged**: 7.1.0 answers the backend's `acknowledgeMessage` with an
     error (`NullPointerException: "evidence" is null`) and offers them again forever. The tool gives these evidences new ids
     (`step-evidence|…`) instead of the ids of the migrated evidence rows (`evidence|…`).
   * **What to do:** follow the procedure: let pull backends fetch everything before stopping 6.x. Fix in the tool: reuse the
     evidence ids and migrate the referenced document. 7.1.0 should also not fail on a missing evidence (patch candidate).
2. **Migration tool: the draft `application.properties` contains the database URL and S3 endpoint the tool used**
   (here `localhost:33307`, `localhost:9000`), without a `# REVIEW` marker. When 7.1.0 runs on another machine or in a
   container, it cannot start until they are corrected. The lab corrects them in step 9.
3. **Expected behaviour, not a defect:** in pull mode, 7.1.0 does not create the DELIVERY evidence by itself
   (`auto-trigger-delivery-evidences` acts on push delivery only). The backend must send DELIVERY before RETRIEVAL, as in 6.x.

Findings 1 and 2 are also listed in the developer report (`MIGRATION-TOOL-REPORT.md`).
