# ⚠️ DELETE BEFORE MERGE — Report for the connector-ng developers: 6.x → 7.1.0 database migration

This file is a hand-over report, not documentation. **Delete it, and the folder `DELETE-BEFORE-MERGE/` with the patches, before this
branch is merged.** Everything needed is in this file, the zip in `DELETE-BEFORE-MERGE/`, this branch and the tool repository.

It covers the database migration tool for connector 6.x → 7.1.0, the documentation added to connector-ng, the defects found in 7.1.0
(with patches for 7.1.1), the limitations of the tool and the open to-dos.

## 1. Summary

* A new, separate tool, **`connector-db-migration`**, copies the data of a connector 6.x installation (6.0.x/6.1.x, schema `V6.0.2`)
  into a new connector **7.1.0** database and its S3 bucket. It works across engines: sources are PostgreSQL, MySQL, MariaDB, Oracle
  and H2; targets are PostgreSQL, MySQL, MariaDB and Oracle 23. It also converts the 6.x `connector.properties` into a draft 7.1.0
  `application.properties`. It reads the 6.x database only, through the published 6.x persistence model, and writes the published
  7.1.0 schema.
* The **connector-ng installation guide** gets a complete migration chapter: the cut-over procedure (rehearsal, gateway switch,
  flush, migration, start, rollback), the limitations, the data mapping, the finding codes of the report, the target storages and the
  connector defects found.
* **Six defects of 7.1.0** were found while building and black-box testing the tool. Each has a patch with a failing test, the fix
  and a changelog line for **7.1.1**, made against `develop` (= the 7.1.0 tag, `4ea94b21`).
* Testing:
  * every 6.x engine version × every 7.1.0 engine (40 pairs), on every push;
  * failures that happen in practice: lost connections, 6.x still running, SIGTERM followed by `--resume`, wrong time zone,
    locked H2 file, large payloads;
  * the published 7.1.0 image started on the migrated data of all four target engines;
  * two end-to-end black-box cut-overs with two real 6.1.0 installations exchanging messages through two Domibus gateways.

## 2. Where everything is

| What | Location |
|---|---|
| Migration tool (code, tests, CI, Docker image, tool docs) | Repository `connector-db-migration`, branch `main`. **TODO: final organisation and URL** (it is still a private repository of the author and will move). |
| connector-ng documentation changes and this report | Branch `docs/db-migration-tool` of the connector-ng fork, based on the branch `db-migration`. **TODO: final location** of the branch or pull request. |
| Patches for 7.1.1 | `DELETE-BEFORE-MERGE/connector-ng-7.1.1-patches.zip` in this branch: six files `0001-…patch` to `0006-…patch` and their `README.md`, in the folder `connector-ng-7.1.1-patches/`. **Delete the folder `DELETE-BEFORE-MERGE/` before merging**; the patches are meant for a separate fix branch from `develop`. |
| Tool documentation (Markdown) | `connector-db-migration/README.md` and `connector-db-migration/docs/`: `CUTOVER.md`, `LIMITATIONS.md`, `MAPPING.md`, `ISSUES.md`, `STORAGE.md`, `UPSTREAM-ISSUES.md`, `TESTING.md`, `TEST-PLAN.md`. |
| Same documentation in the connector-ng guide (AsciiDoc) | `connector-documentation/src/docs/asciidoc/installation/migrate-database-from-6.adoc` and `installation/migrate-database-from-6/{limitations,mapping,findings,storage,defects}.adoc`. |
| CI of the tool | `.github/workflows/ci.yml` and `release.yml` in the tool repository (GitHub Actions). |

The AsciiDoc sub-pages were generated from the tool's Markdown files and must be kept in sync; each starts with a comment saying so.

## 3. The migration tool

### What it does

* **Commands:**
  * `analyze`: a dry run that writes only a report;
  * `migrate`: with `--resume` after an interruption;
  * `verify`: an independent check of the result;
  * `flush-status`: has 6.x finished its work (database, Artemis queues, dead letters);
  * `convert-config`;
  * `extract-keys`: the keys of encrypted 6.x files;
  * `schema <ENGINE>`: the 7.1.0 DDL with the PostgreSQL correction;
  * `storage-providers`;
  * `interactive`: a guided mode.
* **Delivery:** an executable JAR (Java 21) and a Docker image; every option can also be set as a `MIGRATION_<OPTION>` variable.
* **Data mapping:**
  * lanes → business domains (the active P-Mode per lane becomes the domain's processing mode; elements of older P-Modes go to a
    disabled `legacy_6x_archive` domain);
  * messages, AS4 properties, contents → attachments in S3, evidences, errors;
  * transport steps → one step per message with the status history;
  * users → new random passwords (6.x SHA-512 hashes cannot be used by 7.1.0).
* **Encrypted 6.x filesystem payloads** are decrypted with the per-file keys that 6.x kept in the JSON of each message (transport
  steps), in a key file or in the Artemis journal.
* **Report:** `migration-report.md`/`.json` with counters and finding codes, `truncated-values.jsonl` and `changed-values.jsonl`
  (nothing is shortened or changed silently), `user-credentials.properties`, and the draft `application.properties` with `# REVIEW`
  markers.
* **Hardening:**
  * a fingerprint of the source before and after the run (detects a 6.x that is still running);
  * retries and a clean stop on lost connections;
  * idempotent resume with deterministic S3 keys;
  * a heap guard;
  * uploads bounded by a byte budget (16 threads by default);
  * acceptance of a target that 7.1.0 already initialised;
  * unique backend and ebMS ids;
  * a time-zone check against the `EventTime` of evidences;
  * an XA readiness check of the target database;
  * a refusal before writing when the target schema rejects the storage value.

### How it was tested

* **Unit and integration tests:** a scenario catalogue of genuine 6.x databases, built with the 6.1.0 initial scripts and the real 6.x
  classes, plus randomised installations. Every row and every S3 object is compared byte for byte.
* **Hardening and volume:** `HardeningIT` (13 tests) covers failure situations that happen in practice. `VolumeIT` migrated 20 000 messages at about 180 messages per
  second, with all systems on one host.
* **End to end:** the published `connector-ng:7.1.0` image is started on the migrated data of PostgreSQL, MySQL, MariaDB and Oracle,
  also when 7.1.0 initialised the database first, and reads data through its REST API. The CLI is tested as a process (exit codes,
  SIGTERM + `--resume`, guided mode), and the Docker image is tested too.
* **CI:** 40 source/target pairs, volume, behaviour, MongoDB storage, e2e, CLI process and Docker jobs. All green on the last push.
* **Black box:** two parties, each with Domibus 5.1.9 and the e-CODEX connector plugin, plus a 6.1.0 connector built from the tag
  and backends in pull mode with WS-Security. About 70 messages were exchanged per side. Both parties were then cut over as the guide
  describes, to PostgreSQL 17 and MySQL 8.4; blue was also migrated again to MariaDB 12.3. Results:
  * the migrated 7.1.0 consumed what waited in the JMS plugin queue;
  * evidences were correlated with migrated messages;
  * the unchanged 6.x backend clients worked against 7.1.0;
  * the rollback restarted 6.x on its untouched database.
  Details: `docs/TESTING.md` of the tool.

### Limitations (summary; full list in `LIMITATIONS.md` and the guide)

* **Not migrated:**
  * messages still in the 6.x Artemis queues or dead letter queues;
  * messages waiting in the Domibus connector plugin;
  * encrypted files whose key exists nowhere (on 6.1.0 the files received from the gateway lose their keys after processing);
  * password hashes;
  * the 6.x processing state of unfinished messages;
  * evidences without XML;
  * older P-Modes of a lane (only their referenced elements are kept);
  * a P-Mode 7.1.0 rejects.
* **Changed:**
  * values longer than the 7.1.0 columns (originals in `truncated-values.jsonl`);
  * duplicate backend and ebMS ids (suffixed; originals in `changed-values.jsonl`);
  * transport attempts collapsed into one step;
  * empty required values (`unknown`).
* **Requires:**
  * 6.x stopped;
  * the right `--source-timezone`;
  * an empty (or only 7.1.0-initialised) target;
  * Oracle 23 as an Oracle target;
  * a target database ready for XA.
* **Time ambiguity:** 6.x stored local times without offset. A time in the hour repeated when daylight saving time ends can mean two
  instants and can come out one hour off.
* **Duration** depends mostly on network round trips: about 3 per payload stored in the 6.x database. Run the tool next to the 6.x
  database (see "Duration and network latency" in the guide).
* **The converted configuration** is a draft: values without a certain 6.x equivalent keep 7.1.0 template values (`# REVIEW`),
  routing rules per backend are merged, and keystores must be copied and possibly converted. Only startability is verified, not
  correctness for the environment.
* **Storage:** 7.1.0 reads attachments from S3 only. The filesystem and MongoDB GridFS providers of the tool are prepared for later
  releases and refused against the 7.1.0 schema.

## 4. Changes to the connector-ng documentation (branch `docs/db-migration-tool`)

| File | Change |
|---|---|
| `installation/migrate-database-from-6.adoc` (new) | The migration chapter: what is migrated and what not, the cut-over procedure (rehearse, prepare, switch the gateway, flush, migrate, start, roll back), options, Docker usage, the checklist before the first start, the report, how the tool was tested. |
| `installation/migrate-database-from-6/limitations.adoc` (new) | Full limitations, including "Duration and network latency" and "The converted configuration". |
| `installation/migrate-database-from-6/mapping.adoc` (new) | How every 6.x table becomes 7.1.0 rows. |
| `installation/migrate-database-from-6/findings.adoc` (new) | Every finding code of the report and what to do. |
| `installation/index.adoc` | Includes the new chapter. |
| `installation/introduction.adoc`, `index.adoc` | Point upgraders to the data migration before the configuration migration. |
| `installation/database.adoc` | The data migration is now covered. |
| `configuration/migrate-from-6-to-7.adoc` | Refers to the data migration; a note on the file deletion threshold and migrated history. |
| `changelog/changelog-7.x.x.adoc`, `CHANGELOG.md` | A `7.1.1 - unreleased` section, "Added": the migration documentation. The 7.1.0 defects are not mentioned here; their lines are in the patches. |
| `DEVELOPER-BUG-REPORT.md` | Only the personal repository reference was replaced by a TODO. |

The chapter renders with Asciidoctor without warnings. Two pre-existing errors are unrelated: two other chapters include the
distribution's `application.properties`, which only the real docs build provides.

## 5. Defects of 7.1.0 and the patches for 7.1.1

| Patch | Defect | Test added |
|---|---|---|
| `0001-fix-dbscripts-…` | `dbscripts/initial/postgres.sql` fails on PostgreSQL: the check on `connector_messages.status` reuses the name `connector_messages_direction_check`; only 3 of 19 tables are created. | `PostgresInitialScriptIT` (Testcontainers PostgreSQL 17) runs the shipped script. |
| `0002-fix-persistence-…` | Evidence correlation looks messages up by backend message id and ebMS id with single-result queries; neither column is unique, and a duplicate makes every evidence for it fail with `IncorrectResultSizeDataAccessException`. The newest message now wins. | `ConnectorDBMessageRepositoryTest.FindReferencedBusinessMessage` (two tests). |
| `0003-fix-rest-…` | `GET /api/v1/admin/messages?size=500` (or `size=0`, `page=-1`) answers 500; it now answers 400. | `ConnectorMessageAdminControllerTest.ListMessages` (parameterized). |
| `0004-fix-pmode-…` | Services with the same value and different types: message verification and AS4 storage look the service up by name only and fail. Now by name and type. | `ConnectorDBProcessingModeServiceRepositoryTest`; verifier test updated. |
| `0005-fix-config-…` | `push-address` is `@NotNull` for every backend partner, also pull partners that are never called. | `LinkPartnerPropertiesValidationTest` (new). |
| `0006-fix-evidence-…` | An evidence that can no longer change a message (for example DELIVERY after an evidence timeout) is logged as ignored, but the JMS listener's transaction is already rollback-only. The trigger is redelivered and ends in `ActiveMQ.DLQ`. Found in the black-box cut-over. | `ConnectorMessageEvidenceVerifierTransactionTest` (new; fails with `UnexpectedRollbackException` without the fix). |

Each patch also adds its line to `CHANGELOG.md` (`## [7.1.1] - unreleased` → `### Fixed`) and to `changelog-7.x.x.adoc`
(`== 7.1.1 (unreleased)` → `==== Fixes`). The first patch creates these sections.

**Apply** (from a clone of connector-ng; take the zip from this branch first):

```bash
git show origin/docs/db-migration-tool:DELETE-BEFORE-MERGE/connector-ng-7.1.1-patches.zip > /tmp/patches.zip   # or copy it
unzip -o /tmp/patches.zip -d /tmp
git fetch origin
git switch -c fix/7.1.1 origin/develop
git am /tmp/connector-ng-7.1.1-patches/0*.patch
# develop moved on and a patch does not apply:
#   git am --3way ... ; resolve ; git add ... ; git am --continue
```

**Verify:**

```bash
./gradlew :connector-domain:test :connector-application:test :connector-infrastructure:test checkstyleMain checkstyleTest
./gradlew :connector-integrationtest:integrationTest --tests '*PostgresInitialScriptIT'   # needs Docker
```

The series was applied with `git am` to a fresh `develop` and passes the commands above. Merging this documentation branch and the
patches both creates a `7.1.1` changelog section: merge the two sections by hand.

Not patched (documented instead): the purge of unresolved messages (`connector.file-deletion.unresolved-messages.threshold`, `7d`
in the template) would delete most of a migrated history; the migration tool sets `36500d` in the draft.

## 6. To-dos for 7.1.1 and later

**connector-ng (7.1.1 candidates):**

1. Review and apply the six patches above.
2. **Filesystem attachment storage** (and possibly MongoDB GridFS). 7.1.0 reads attachments from S3 only, and the schema accepts only
   `storage = 'S3_BUCKET'`. The migration tool can already write to a local directory (`filesystem` provider, flat or sharded
   layout, storage value `FILESYSTEM`) and to MongoDB GridFS (`MONGODB_GRIDFS`). Both values are assumptions until connector-ng
   defines them. Needed:
   * a storage port implementation in connector-ng;
   * the new values in the check constraint of `connector_message_attachments.storage` in the four initial scripts;
   * the configuration properties;
   * agreeing the values with the tool (its `storage-value` option overrides them).
3. Consider failing fast at startup, or at least warning, when the database is not ready for XA. On PostgreSQL with
   `max_prepared_transactions=0`, 7.1.0 starts, then moves every gateway message to `ActiveMQ.DLQ`. It is documented in
   `database.adoc`, but easy to miss.
4. Consider a safer default for `connector.file-deletion.unresolved-messages.threshold` for migrated installations, or a switch to
   disable the job.
5. Add a changelog line for any further 7.1.1 change in both changelogs (kept in sync).

**Migration tool:**

1. **TODO:** move the repository to its final organisation and replace the TODO links in the guide and in this report.
   Configure the CI secrets for the e-CODEX registry (`ECODEX_REGISTRY_USER`/`ECODEX_REGISTRY_PASSWORD`) and publish a release
   (`release.yml`: a tag `vX.Y.Z` builds the JAR and the image).
2. After patch 0005 is released, the converter no longer needs the placeholder `push-address` for pull partners; after patch 0001,
   the PostgreSQL correction in the tool's schema creation becomes unnecessary. Both should stay as long as 7.1.0 is a target.
3. Performance for database-stored payloads: read the payloads of a message in one query instead of one short transaction each.
   This needs a memory cap, because 6.x often records the payload size as `-1`.
4. Test data: the volume test keeps the generated installation in memory, which limits it to about 20 000 messages in the test JVM;
   seed in a streaming way for larger runs.
5. Run a large migration against a real remote S3 and a remote database to tune `--upload-threads` and `--batch-size` on real
   latencies.
6. **Found by the self-send lab (`lab/selfsend-6x-upgrade` on branch `claude/duplicate-ebms-ids-migration-swf0gi`, see its `REPORT.md`); fixed in the tool on branch `claude/duplicate-ebms-ids-migration-swf0gi` (commit `c6db30c`), to review and merge:**
   * Evidence transport steps rebuilt from 6.x (`EvidenceStepMapper`) give their evidences new ids
     (`DeterministicIds.uuid("step-evidence", …)`) instead of the ids of the migrated evidence rows (`uuid("evidence", <6.x id>)`).
     For a step still `READY_FOR_PULL`, 7.1.0 then fails on `acknowledgeMessage` (`ConnectorDBMessageEvidenceRepository
     #setDeliveredToLinkPartnerAt`: `NullPointerException`, "evidence" is null) and offers the step again forever. Fix: map the
     step's evidence to the migrated evidence row (same business message and type). connector-ng should also check for null there.
   * Inbound messages still waiting for a pull backend lose their business document: 6.x stored the document extracted from the
     ASiC-S container in a `DOMIBUS_CONNECTOR_BIGDATA` row referenced only by the pending transport step
     (`messageContent.document.document.reference`), not by `DOMIBUS_CONNECTOR_MSG_CONT`. Fix: migrate it as `BUSINESS_DOCUMENT`
     (and `Token.pdf` as `PDF_TOKEN`) when the message has no business document row.
   * The draft `application.properties` gets the target JDBC URL and the S3 endpoint as the tool saw them (for example
     `localhost`), without a `# REVIEW` marker. Keep the `${DB_URL}`/`${S3_ENDPOINT}` placeholders or mark the lines.

## 7. Defects of connector 6.x relevant to the migration

* 6.0.0–6.1.0 never store the key of encrypted filesystem payloads in `DOMIBUS_CONNECTOR_MSG_CONT.CONTENT` (an inverted `hasText`
  check). The key survives only in the message JSON (transport steps), so files 6.x received from a gateway cannot be decrypted after
  processing.
* The 6.1.0 persistence enum lacks the role `ACTUATOR` written by the 6.x UI (the tool reads users with SQL).
* 6.x removes the files of finished messages without always marking the content rows deleted (the tool reports it as INFO).
* A real 6.1.0 has two default lanes (`defaultMessageLane` and `defaultBusinessDomain`); P-Modes uploaded in 6.1.0 belong to
  `defaultBusinessDomain`. Both become `default_business_domain`.

**Kept out of the user guide.** The guide describes no connector defects and only mentions S3 as target storage. The defect
write-ups and the description of the unfinished storage providers that were first drafted as guide pages are in the appendix
below; the 7.1.0 PostgreSQL script workaround (`awk` command) was removed from `installation/database.adoc`, since patch 0001 fixes
the script.

## Appendix A. Material moved out of the user guide

### A.1 Defect write-ups

Each section can be filed as an issue as it stands. The tool works around all of them; the workaround is noted. For the six connector-ng defects that can be fixed in code (PostgreSQL script, single-result lookups, page size, service type, push-address, late evidences) patches with a regression test and a changelog line for 7.1.1 were prepared against `develop` (7.1.0) and handed over separately; 7.1.0 is released, so the fixes are proposed for 7.1.1.

#### connector-ng 7.1.0: `dbscripts/initial/postgres.sql` fails on PostgreSQL

**Steps:** create an empty PostgreSQL 17 database and run `psql -v ON_ERROR_STOP=1 -f dbscripts/initial/postgres.sql`.

**Result:** `ERROR: check constraint "connector_messages_direction_check" already exists` at line 140. Only 3 of 19 tables exist.

**Cause:** in `create table connector_messages`, the check on `status` is declared as `constraint connector_messages_direction_check`, the name already used by the check on `direction`.

**Fix:** rename the second constraint to `connector_messages_status_check`.

**Workaround:** the migration tool renames it when it creates the schema. The installation guide gives an `awk` command.

#### connector-ng 7.1.0: single-result lookups on ids that are not unique

`ConnectorMessageJpaRepository#findByBackendMessageIdentifier` and `#findByAs4PropertiesEbmsMessageIdentifierAndDirection` return one entity, but neither column is unique. Two messages with the same backend message id make evidence correlation fail with `IncorrectResultSizeDataAccessException`. This happens when a backend resends with the same id, which 6.x accepted. `findByStepOrMessageOrRemoteSystemIdentifier` (transport steps) behaves the same.

**Suggestion:** add unique constraints, or return the newest match.

**Workaround:** the tool keeps the id on the newest message and suffixes the older ones (`#6x-<connector message id>`, reported as `DUPLICATE_BACKEND_MESSAGE_ID` / `DUPLICATE_EBMS_ID`). It also collapses the 6.x transport attempts into one step per message.

#### connector-ng 7.1.0: services and parties looked up by part of their key

`ConnectorProcessingModeServiceJpaRepository#findByNameAndProcessingModeBusinessDomainIdentifier` ignores the service type. A P-Mode with two services of the same value and different types is accepted at upload, but messages for those services fail.

**Workaround:** the tool reports such P-Modes (`PMODE_AMBIGUOUS_FOR_7X`).

#### connector-ng 7.1.0: page size above 100 answers HTTP 500

`GET /api/v1/admin/messages?size=500` returns 500 (`IllegalArgumentException: size must be between 1 and 100` from `ConnectorPageRequest`) instead of 400.

#### connector-ng 7.1.0: purge of unresolved messages and migrated history (documentation)

`connector.file-deletion.unresolved-messages.threshold=7d` (template) deletes every message without `confirmed_at` or `rejected_at` that is older than 7 days, rows and files. On a database migrated from 6.x this is most of the history.

**Suggestion:** a note in the upgrade documentation, or a way to disable the job.

#### connector-ng 7.1.0: push-address required for pull backends

`LinkPartnerDetailProperties.pushAddress` is `@NotNull`, so 7.1.0 does not start unless every backend partner has a `push-address`. A passive (pull) partner is never called: 7.1.0 makes its messages ready for pull (`ConnectorJmsBackendMessageDeliveryListener`). `LinkPartnerProperties` already requires the address for push partners.

**Fix:** drop the `@NotNull`.

**Workaround:** the tool writes a marked placeholder (`https://pull-mode-backend.invalid/`) for pull partners.

#### connector-ng 7.1.0: a late evidence ends in the broker's dead letter queue

`ConnectorMessageEvidenceVerifierService` is `@Transactional` and throws `ConnectorEvidenceNotRelevantException` for an evidence that cannot change the message any more (for example DELIVERY after the NON_DELIVERY of an evidence timeout). The inbound evidence processor catches it and logs "Evidence ... ignored", but the transaction of the JMS listener is already marked rollback-only: the commit fails, the evidence trigger is redelivered 7 times and ends in `ActiveMQ.DLQ`. Seen in the black-box cut-over for messages 6.x had rejected by its evidence timeout.

**Fix:** `@Transactional(noRollbackFor = ConnectorEvidenceNotRelevantException.class)` on the verifier.

**Workaround:** none in the tool; such dead letters can be deleted from `ActiveMQ.DLQ` (their evidence would have been ignored).

#### connector 6.0.0–6.1.0: encryption key of filesystem payloads never stored

`MsgContentPersistenceService#storeObjectIntoMsgCont` contains `if (!StringUtils.hasText(ref.getText())) { msgCont.setContent(...) }`. The condition is inverted, so the key text of an encrypted file (`FileBasedLargeFileReference#getText()`) is never written to `DOMIBUS_CONNECTOR_MSG_CONT.CONTENT`. After a message is reloaded from the database, 6.x itself cannot decrypt its payloads (for example on a manual retry). The key survives only in the JSON of the message (queues, `DC_TRANSPORT_STEP.TRANSPORTED_MESSAGE`).

**Workaround:** the migration tool collects the keys from the transport steps, the Artemis journal and a key file.

#### connector 6.1.0: persistence enum lacks the ACTUATOR role

`eu.ecodex.connector.persistence.model.enums.UserRole` has `ADMIN` and `USER`, but the UI (`domibusConnectorUILib`) writes `ACTUATOR`. Reading a user with that role through the persistence module fails with `No enum constant ... ACTUATOR`.

**Workaround:** the tool reads users with SQL.

### A.2 Target storages other than S3 (not ready; refused against 7.1.0)

Connector 7.1.0 keeps attachment files in an S3 compatible bucket and records `S3_BUCKET` in `connector_message_attachments.storage`. The 7.1.0 documentation announces local filesystem storage for a later release. The tool writes files through a pluggable provider, so a migration to a 7.x release with another storage needs only a new provider, not a new tool.

| Provider (`--storage`) | Storage value | Module | Readable by 7.1.0 |
|---|---|---|---|
| `s3` (default) | `S3_BUCKET` | `migration-core` | yes |
| `filesystem` | `FILESYSTEM` | `migration-core` | no, prepared |
| `mongodb` (GridFS) | `MONGODB_GRIDFS` | `migration-storage-mongodb` | no, prepared |

`connector-db-migration storage-providers` lists the providers on the class path with their options.

#### Using a provider other than S3

```bash
connector-db-migration migrate ... \
  --storage filesystem --storage-option path=/srv/connector/files,layout=sharded \
  --allow-unsupported-storage

connector-db-migration migrate ... \
  --storage mongodb --storage-option uri=mongodb://user:pw@mongo:27017,database=connector,bucket=attachments \
  --allow-unsupported-storage
```

Before writing anything, the tool reads which storage values the target schema accepts: the check constraint on PostgreSQL and Oracle, the enum on MySQL and MariaDB. For the 7.1.0 schema that is `S3_BUCKET` only, so other providers are refused. With `--allow-unsupported-storage` the run continues; the target schema must then accept the value, as a later 7.x schema would or as an operator adapted one (the tests drop the constraint, see `TargetSchemas`). `--storage-option storage-value=<VALUE>` changes the recorded value when a 7.x release names its storage differently.

The `storage` property of the attachments inside the transport step JSON gets the same value.

##### filesystem

| Option | Meaning |
|---|---|
| `path` | Root directory (required). |
| `layout` | `flat`: `<path>/<identifier>`, the same key as in S3 (default). `sharded`: `<path>/<first two characters>/<identifier>`. |
| `create-directory` | Create the root when missing (default `true`). |

Files are written under a temporary name and moved into place, so an interrupted run never leaves a partial file under its final name. Re-runs (`--resume`) overwrite.

##### mongodb

| Option | Meaning |
|---|---|
| `uri` | MongoDB connection string (required). |
| `database` | Database (default `connector`). |
| `bucket` | GridFS bucket (default `attachments`). |

One GridFS file per attachment, named after its identifier, with the content type in the metadata. Older files of the same name are deleted only after the new upload completes.

#### Writing a provider

1. Implement `eu.ecodex.connector.migration.storage.ObjectStoreProvider`: `id()`, `description()`, `options()`, `storageValue()` (the value 7.x records for that storage), and `create(StorageSettings)`, which returns an `ObjectStore` (`prepare`, `put`, `get`, `size`, `describe`, `close`).
1. Register it in `META-INF/services/eu.ecodex.connector.migration.storage.ObjectStoreProvider`.
1. Put the module on the class path of the CLI. `migration-cli` depends on `migration-storage-mongodb` with `runtimeOnly`; the fat jar merges the service files.
1. Test it like `GridFsMigrationIT`: seed a scenario, call `TargetSchemas.createWithAnyStorage`, migrate, and check with `MigrationHarness.verify`. The check compares every stored object byte by byte through your `ObjectStore`.

`put` must be idempotent: identifiers are derived from 6.x ids, and `--resume` uploads the same keys again.
