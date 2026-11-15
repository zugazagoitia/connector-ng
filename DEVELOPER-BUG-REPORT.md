# e-CODEX Connector NG 7.1.0: developer bug report

Date: 2026-11-13

**Tested:** connector built from branch `docs-resolve-pending-points` (TODO: final repository location) (upstream `develop` at `4ea94b21`, version 7.1.0), run as the Docker image and as the JAR distribution. Backing services: MySQL 8.4, MariaDB 12.3, PostgreSQL 17, Oracle 23 free, ActiveMQ Classic 6.2.0, MinIO.

These are software issues found while testing the documentation. They are deliberately kept out of the user docs.

**Notes on this report**

- Severity is the testers' rating where the findings give one. Where a finding has no rating, the severity here is provisional and assigned from the stated impact.
- Verification status is taken from the findings: "Runtime" means reproduced on running software; "Code reading" means checked from source only.
- Duplicates from several test packages are merged. Tester references (for example WP2-02) point to the internal test-package entries.

## Summary

| ID | Severity | Area | Title |
|---|---|---|---|
| BUG-01 | high | Security | P-Mode truststore password returned in clear text |
| BUG-02 | high | Security | Default CORS settings make every request with an Origin header fail with HTTP 500 |
| BUG-03 | high | Security | Deactivated user can keep renewing access with a refresh token |
| BUG-05 | high | Message processing | Dead-letter count and stock ActiveMQ dead-letter queue do not match |
| BUG-06 | high | Message processing | Redelivery settings apply to two queues only; redelivery delay ignored |
| BUG-07 | high | Message processing | Inbound NON_DELIVERY fallback never works; bad inbound message ends in the DLQ |
| BUG-08 | high | Message processing | Outbound SUBMISSION_REJECTION is rolled back with the failed transaction |
| BUG-09 | high | Message processing | Routing match-clause grammar rejects documented values; no startup validation |
| BUG-10 | high | Message processing | C2C test message sending is broken |
| BUG-11 | high | Configuration properties | `connector.s3.auto-create-bucket` condition uses a property name that is never bound |
| BUG-12 | high | Configuration properties | Transaction log location cannot be set through the template property |
| BUG-13 | high | Configuration properties | `connector.scheduling.enabled=false` has no effect |
| BUG-14 | high | Configuration properties | Malformed P-Mode does not stop startup |
| BUG-15 | high | Configuration properties | Effective default of `auto-trigger-delivery-evidences` is false, declared default is true (question) |
| BUG-16 | high | Database scripts | PostgreSQL schema script aborts: duplicate constraint name |
| BUG-17 | high | Template and compose files | Shipped template push-address placeholder prevents startup |
| BUG-18 | high | Trusted lists/DSS | EU LOTL and Belgian TL cannot be parsed; trust-list names are aliases; RTR URL returns 404 |
| BUG-19 | medium | Security | Access tokens cannot be revoked |
| BUG-20 | medium | Security | No login rate limiting or lock-out |
| BUG-23 | medium | Security | `connector.admin.enabled` has no effect |
| BUG-24 | medium | Message processing | Outbound pipeline can run before the staged message is visible |
| BUG-25 | medium | Configuration properties | Unresolved `${VAR}` placeholders are used as literal values |
| BUG-26 | medium | Configuration properties | Unreachable or misconfigured external services are detected after startup, or not at all |
| BUG-27 | medium | Configuration properties | Keystores, truststores, key aliases and link names are not validated at startup |
| BUG-28 | medium | Configuration properties | Queue names are partly hard-coded; `gateway-reception-queue` not configurable |
| BUG-29 | medium | Configuration properties | Value `-` cannot disable a file-deletion job |
| BUG-31 | medium | Configuration properties | `X-Forwarded-Proto` ignored by default behind a TLS proxy |
| BUG-32 | medium | Configuration properties | Link partners: push-address and encryption-alias required even for pull; sender-mode not validated |
| BUG-33 | medium | Database scripts | XA prerequisites for MySQL, MariaDB, Oracle and PostgreSQL are not checked or are reported late |
| BUG-34 | medium | Packaging and distribution | ZIP drops execute permission on `start.sh` |
| BUG-35 | medium | Packaging and distribution | `lib/` folder is never loaded; its driver versions differ from the JAR |
| BUG-36 | medium | REST API | Outbound message with only file parts in `businessContent` is rejected as malformed |
| BUG-37 | medium | REST API | Rejected sender certificate returns HTTP 500 instead of a client error |
| BUG-38 | medium | Trusted lists/DSS | Business-document truststore-enabled and unknown trusted-list source not validated at startup |
| BUG-39 | medium | Build and versions | Generated property reference lacks defaults and omits some properties |
| BUG-40 | low | Security | Security matchers permit paths that do not exist |
| BUG-41 | low | Security | Token of a deleted user returns HTTP 500 |
| BUG-42 | low | Security | `connector.auth.security.jwt.issuer` is mandatory but unused |
| BUG-43 | low | Message processing | Unresolved-message clean-up leaves a transport step |
| BUG-44 | low | Message processing | Evidence trigger always builds a success evidence |
| BUG-45 | low | Message processing | Duplicate `backendMessageIdentifier` and unchecked `service.type` accepted (question) |
| BUG-46 | low | Configuration properties | ANSI colour codes written to non-terminal log output |
| BUG-47 | low | Configuration properties | Invalid log4j2 configuration URI warning in JAR run (question) |
| BUG-48 | low | Database scripts | PostgreSQL script reports 19 errors when the database owner is another user |
| BUG-49 | low | Template and compose files | `docker compose up -d` starts all nine services, including optional ones |
| BUG-50 | low | Template and compose files | `docker/connector/config/` is not git-ignored |
| BUG-51 | low | Template and compose files | ActiveMQ compose healthcheck is permanently unhealthy |
| BUG-53 | low | REST API | Unknown role names silently ignored; user creation without password returns HTTP 500 |
| BUG-54 | low | Build and versions | Versions in the test matrix and changelog differ from the shipped JAR (question) |
| BUG-55 | high | Message processing | Inbound container check requires the partner signing certificate in the connector's own signing keystore |
| BUG-56 | high | REST API | REST backend submission returns HTTP 500 unless a link partner with `certificate-dn=cn=alice` exists |
| BUG-57 | low | Template and compose files | Development compose file: gateway can start before its database is initialised |

---

## High

### BUG-01 P-Mode truststore password returned in clear text

Severity: high | Area: Security | Verification: Runtime

- **Observed:** `PATCH /api/v1/admin/processing-modes/{uuid}/truststore` returns HTTP 200 with the truststore password. `GET /api/v1/admin/processing-modes` also returns the password in clear text.
- **Expected:** Truststore passwords are not returned by the API.
- **Evidence:** `PATCH .../truststore` returns `{"filename":"connector-truststore.jks","password":"12345",...}`; `GET /api/v1/admin/processing-modes` returns `"password":"12345"`. Code: `WebSecurityConfiguration`.
- **Suggested fix:** Never return the password.
- **Tester references:** WP2-02, plan F18.

### BUG-02 Default CORS settings make every request with an Origin header fail with HTTP 500

Severity: high | Area: Security | Verification: Runtime

- **Observed:** With the `connector.cors.*` properties removed, `OPTIONS` and `GET` requests with `Origin: http://localhost:3000` return HTTP 500.
- **Expected:** Per the platform configuration chapter under test, the default allows every origin.
- **Evidence:** Log: `IllegalArgumentException: When allowCredentials is true, allowedOrigins cannot contain the special value "*"`. With the template value: allowed origin returns 200 with ACAO headers, other origins return 403, requests without Origin return 200. Code: `ConnectorCorsProperties` (default `*` with `allowCredentials=true`).
- **Suggested fix:** Use `allowedOriginPatterns`, or default `allow-credentials` to false.
- **Tester references:** WP2-01.

### BUG-03 Deactivated user can keep renewing access with a refresh token

Severity: high | Area: Security | Verification: Runtime

- **Observed:** After a user is disabled (`PATCH {"enabled":false}`), `POST /api/v1/auth/refresh` still returns HTTP 200 with a new access token. Login after disabling returns 401 "Your account is inactive". Deleting the user makes refresh return 401.
- **Expected:** A disabled user cannot obtain new access tokens. The documentation as fixed says deleting the account is the way to lock a user out.
- **Evidence:** Code: `ConnectorRefreshUserRefreshTokenService` does not check `enabled`.
- **Suggested fix:** None given beyond the code location; the implied fix is to check `enabled` in the refresh service.
- **Tester references:** WP2-03.

### BUG-05 Dead-letter count and stock ActiveMQ dead-letter queue do not match

Severity: high | Area: Message processing | Verification: Runtime

- **Observed:** With stock `apache/activemq-classic:6.2.0`, a garbage message posted to `outbound-message-staging-queue` ends in `ActiveMQ.DLQ` (size 1). `ActiveMQ.DLQ.connector.queues.outbound-message-staging-queue` stays at size 0, and the statistics report `dlqCount 0`. With a per-queue dead-letter strategy the per-queue DLQs fill and `dlqCount` is 1 (verified for five queues). The statistics cover eight hard-coded queue names. Browsing the statistics creates empty `ActiveMQ.DLQ.*` queues.
- **Expected:** The failed-message count reflects failed messages in the default broker setup, as the operations chapter under test describes.
- **Evidence:** `ActiveMQ.DLQ size=1`, `ActiveMQ.DLQ.connector.queues.outbound-message-staging-queue size=0`, stats `dlqCount 0`. Code: `ConnectorJmsQueueStatsProvider.dlqNameOf`.
- **Suggested fix:** Configure the broker with `individualDeadLetterStrategy queuePrefix="ActiveMQ.DLQ." useQueueForQueueMessages="true"` (verified snippet in the operations chapter), or make the statistics read `ActiveMQ.DLQ`.
- **Tester references:** WP2-16, WP5-13 (related).

### BUG-06 Redelivery settings apply to two queues only; redelivery delay ignored

Severity: high | Area: Message processing | Verification: Runtime

- **Observed:** With `max-delivery-attempts=2`, outbound staging and processing make 2 attempts, while backend-delivery, inbound staging and the inbound evidence trigger make 7 each. With `max-delivery-attempts=3, redelivery-delay=5s, multiplier=1.0`, all 3 attempts are logged within the same second. A C2C message failing at staging makes 5 attempts within one second, then goes to `ActiveMQ.DLQ`.
- **Expected:** The redelivery settings apply to all queues, and the delay is honoured between attempts.
- **Evidence:** Code: `ActiveMQConfig`. A `RedeliveryPolicyMap` has entries only for outbound staging and processing. The map's default entry overrides `factory.setRedeliveryPolicy` for all other queues.
- **Suggested fix:** None given beyond the code location.
- **Tester references:** WP2-17, WP4-07.

### BUG-07 Inbound NON_DELIVERY fallback never works; bad inbound message ends in the DLQ

Severity: high | Area: Message processing | Verification: Runtime

- **Observed:** An injected inbound business message with a non-ZIP ASiC-S payload fails seven times with `Security exception during inbound message`, followed by `ConnectorEvidenceException: message [...] has no prior evidence of type [RELAY_REMMD_ACCEPTANCE] required for the next step`. Final state: status `CREATED`, `backendName` null, only SUBMISSION_ACCEPTANCE present, `ActiveMQ.DLQ` size 1, nothing on `domibus.backend.jms.inQueue`.
- **Expected:** The bad inbound message receives a NON_DELIVERY evidence, as the inbound evidence chapter under test describes.
- **Evidence:** The catch block in `ConnectorInboundMessagePipeline.process` passes the original message, which lacks the RELAY_REMMD_ACCEPTANCE created by the acceptance step, to `ConnectorInboundMessageNonDeliveryStep`. `ConnectorEvidenceToolkitImpl` (around lines 287 and 306) requires that evidence.
- **Suggested fix:** Pass the message that already has the acceptance evidence to the non-delivery step, and do not rethrow or roll back the NON_DELIVERY.
- **Tester references:** WP5-01.

### BUG-08 Outbound SUBMISSION_REJECTION is rolled back with the failed transaction

Severity: high | Area: Message processing | Verification: Runtime for the transient case; the permanent case is inferred

- **Observed:** On a transient failure (message `183ca2d6`), the log shows the rejection being generated and the message being rejected. The redelivery then succeeds, and the final state is `SUBMITTED` with `rejectedAt` null and only SUBMISSION_ACCEPTANCE recorded, so the rejection was rolled back. For a permanent failure, the code path implies redelivery until the DLQ with no rejection sent to the backend; this was not reproduced.
- **Expected:** A failed outbound message is rejected to the backend with SUBMISSION_REJECTION.
- **Evidence:** `ConnectorOutboundMessagePipeline.process`: the catch block creates the rejection and submits it, then throws `ConnectorGatewaySubmissionException`. The listener `ConnectorJmsOutboundMessagePipelineListener` is `@Transactional`.
- **Suggested fix:** Decide whether the rejection must survive. Options from the inputs: commit it separately, or do not rethrow.
- **Tester references:** WP5-02.

### BUG-09 Routing match-clause grammar rejects documented values; no startup validation; null-unsafe `startswith`

Severity: high | Area: Message processing | Verification: Parser harness against the domain JAR; startup checks at runtime; backend-resolution effect from code reading

- **Observed:** Against `connector-domain-7.1.0.jar`, `equals(FromPartyIdType, 'x')` fails to parse (tokenised as FromPartyId plus junk). Values `'Form A'`, `'a+b'`, `'a=b'`, `''`, unquoted and double-quoted values, `EQUALS(...)` and `servicename` all fail to parse. A three-operand `|(a,b,c)` parses and ignores the third operand; trailing garbage is ignored. `startswith(FinalRecipient, ...)` on a null value throws NullPointerException. At startup, `match-clause=equals(ServiceName,Test)` is accepted with no syntax check, and unknown `link-name` or `default-backend-name` values are accepted too.
- **Expected:** The documented grammar and values are accepted, and invalid clauses are rejected at startup.
- **Evidence:** `TokenType.java` (VALUE regex `'[\w:@_\-~\./#\?]+'`, enum order); `ConnectorPropertyMessageRoutingConfigurationProvider` parses rules per message. `getDefaultBackendName()` builds and parses all rules, so a bad clause breaks backend resolution of every inbound message not resolved by conversation, even with routing disabled.
- **Suggested fix:** Validate clauses at startup, fix the `FromPartyIdType` tokenisation, make `startswith` null-safe, and use a strict parser.
- **Tester references:** WP3-07.

### BUG-10 C2C test message sending is broken

Severity: high | Area: Message processing | Verification: Runtime

- **Observed:** On the test instances (ports 31000 and 32050): without `conversationIdentifier`, HTTP 500 with `conversationIdentifier is marked non-null but is null`. Without `aesType`, HTTP 400. With both, HTTP 201, but staging then fails with `null value in column "status" of relation "connector_messages"`, five attempts follow, and the message lands in `ActiveMQ.DLQ`. Nothing is stored.
- **Expected:** A test message sent with the documented request is accepted and processed.
- **Evidence:** `ConnectorSendOutboundC2CTestMessageService.java:105` (no `.status(...)`). `ConnectorTestMessageAS4Properties`: `conversationIdentifier` is optional in the request but required in practice.
- **Suggested fix:** None given beyond the code location.
- **Tester references:** WP4-02.

### BUG-11 `connector.s3.auto-create-bucket` condition uses a property name that is never bound

Severity: high | Area: Configuration properties | Verification: Runtime

- **Observed:** With the documented property set to `false` and the bucket absent, the log shows `Creating S3 bucket [...]` and the bucket is created. With `connector.s3.auto-create-bucket=false`, no check is made, the missing bucket stays missing, and startup continues.
- **Expected:** The documented `connector.file.storage.s3.auto-create-bucket=false` disables automatic bucket creation.
- **Evidence:** `ConnectorS3BucketInitializer.java:37-41` uses `@ConditionalOnProperty(name = "connector.s3.auto-create-bucket")`, but the bound property is `connector.file.storage.s3.auto-create-bucket`.
- **Suggested fix:** Fix the property name in the condition.
- **Tester references:** WP1b-01, WP3-01.

### BUG-12 Transaction log location cannot be set through the template property

Severity: high | Area: Configuration properties | Verification: Runtime

- **Observed:** `spring.jta.log-file=./data/jtalog` in the template has no effect. Atomikos writes `tmlog<N>.log` and `tmlog.lck` to the working directory (`/app` in the container, `standalone/` for the JAR). In the container without an override, only `/app/data/tlcache` is in the data volume. With `SPRING_JTA_ATOMIKOS_PROPERTIES_LOG_BASE_DIR=/app/data/jtalog`, the log is written to `/app/data/jtalog/`. A second container on the same volume fails with `The specified log seems to be in use already`. `spring.jta.log-dir` also has no effect. A root-owned bind mount on `/app/data` gives the same misleading message. The image also creates an empty, unused `/app/transaction-logs`.
- **Expected:** The transaction log is stored under the configured data directory, as the template and docs suggest.
- **Evidence:** `tmlog1.log` and `tmlog.lck` in `/app`; `/app/data` contains only `tlcache`.
- **Suggested fix:** Use `spring.jta.atomikos.properties.log-base-dir` (verified working); remove the unused `transaction-logs` directory from the Dockerfile; fix the template; correct the misleading message.
- **Tester references:** WP1b-05, WP1b-06, WP2-20, plan F11.

### BUG-13 `connector.scheduling.enabled=false` has no effect

Severity: high | Area: Configuration properties | Verification: Runtime (private JAR)

- **Observed:** With `scheduling.enabled=false`, an orphan-clean-up cron of `*/5 * * * * *` and a 1 s threshold, an uploaded file was purged: `Orphaned attachments cleanup done: [1] deleted` 13 s later.
- **Expected:** `connector.scheduling.enabled=false` switches off the scheduled jobs, as the template comment describes.
- **Evidence:** `connector-bootstrapper/.../ConnectorApplication.java:23` has `@EnableScheduling` unconditionally.
- **Suggested fix:** Remove `@EnableScheduling` from `ConnectorApplication`, or make it conditional.
- **Note:** Plan item F32 stated that this switch works. That was wrong and is corrected here.
- **Tester references:** WP4-01.

### BUG-14 Malformed P-Mode does not stop startup

Severity: high | Area: Configuration properties | Verification: Runtime

- **Observed:** On a fresh database with `pmode.file` set to `<notapmode>`, the connector starts. The log shows `ERROR ... Error parsing processing mode`, then `DEBUG ... Business domain [default_business_domain] already has a processing mode`. The domain stays without a P-Mode.
- **Expected:** Startup fails fast for an unparsable P-Mode, and parse errors are reported as such.
- **Evidence:** `ConnectorBusinessDomainInitializer.java:139` catches every `ConnectorProcessingModeException`, including parse errors.
- **Suggested fix:** Fail fast; distinguish parse errors from "already exists".
- **Tester references:** WP3-02.

### BUG-15 Effective default of `auto-trigger-delivery-evidences` is false; declared default is true (question)

Severity: high | Area: Configuration properties | Verification: Code reading only (runtime not verified; needs a working push backend)

- **Observed:** `ConnectorMessageProcessingProperties` declares `@DefaultValue("true")`, but `ConnectorJmsBackendMessageDeliveryListener.java:57` reads `@Value("${connector.message-processing.auto-trigger-delivery-evidences:false}")`. When the property is absent, `/api/v1/admin/configurations/message-processing` reports `true`, while no DELIVERY evidence is auto-triggered. Only push deliveries trigger it; the pull `acknowledgeMessage` never does (`ConnectorAckMessageTransportStepService`).
- **Expected:** The reported default matches the behaviour.
- **Evidence:** Source lines above.
- **Suggested fix:** Read the flag from the properties bean in the listener, or require the property to be set explicitly.
- **Note:** The inputs say the effective default is false (correction to plan item F19).
- **Question for maintainers:** Which default is intended, `true` or `false`?
- **Tester references:** WP3-04, WP4-08, WP5-05, plan F19.

### BUG-16 PostgreSQL schema script aborts: duplicate constraint name

Severity: high | Area: Database scripts | Verification: Runtime

- **Observed:** In `dbscripts/initial/postgres.sql`, the `status` check constraint of `connector_messages` reuses the name `connector_messages_direction_check`. The script fails with `ERROR: check constraint ... already exists` and only 12 of 19 tables are created. In a copy with the second constraint renamed, all 19 tables are created, `ddl-auto=validate` passes, and columns, indexes and constraints match the Hibernate-created schema. `mysql.sql` and `mariadb.sql` run cleanly (19 tables); `oracle.sql` passes validation on Oracle 23 free.
- **Expected:** The script creates all 19 tables.
- **Evidence:** `postgres.sql:113,120`.
- **Suggested fix:** Rename the second constraint.
- **Tester references:** WP1a-02, plan F08.

### BUG-17 Shipped template push-address placeholder prevents startup

Severity: high | Area: Template and compose files | Verification: Plan section 4 confirmed; the inputs do not state the test method

- **Observed:** The shipped template value `connector.link.backend[0].link-partners[0].properties.push-address=http://localhost:port/...` fails validation before anything else, with `push-address must be an absolute http(s) URL`.
- **Expected:** Not stated in the inputs; the template is expected to start once a real URL is set, or the error to name the value to change.
- **Evidence:** Plan section 4, item F01.
- **Suggested fix:** None given beyond documenting that users must set a real URL, for example `http://localhost:8080/services/domibusConnectorDeliveryWebservice`.
- **Tester references:** plan F01.

### BUG-18 EU LOTL and Belgian TL cannot be parsed; trust-list names are aliases; RTR URL returns 404

Severity: high | Area: Trusted lists/DSS | Verification: Runtime (EU LOTL success path not verified)

- **Observed:** With the EU LOTL configured, the log shows `Unable to parse binaries ... SigningCertificateV2` and `Configured trust source [eu] with 0 trusted public keys`. The Belgian TL behaves the same way. Two trust-list names (`at` and `other`) cause both downloads to run for each name. `https://www.signatur.rtr.at/currenttl.xml` returns HTTP 404; the same URL is in the template (line 316) and in the integration-test resources. The online refresh runs at every start (online loader expiration 0), and the cache is used only as a fallback.
- **Expected:** Each configured name selects its own trust source, and the configured EU LOTL yields trusted keys.
- **Evidence:** `ConnectorDssTrustedListLoader.buildTrustSource(name)` ignores `name`. `ConnectorDssLotlFactory` and `TrustedListFactory` collect all groups.
- **Suggested fix:** Use the working URL `vertrauensliste.xml` (HTTP 200, used in the docs by the testers). The template URL still needs fixing in code. No other fix is given in the inputs.
- **Tester references:** WP4-03.

---

## Medium

### BUG-19 Access tokens cannot be revoked

Severity: medium | Area: Security | Verification: Runtime

- **Observed:** After logout, a password change, or deactivation, an old access token still returns HTTP 200 on `GET /api/v1/auth/me`. A password change does not revoke the refresh token (refresh returns 200). After secret rotation, the old access token returns 401 but the old refresh token still returns 200.
- **Expected:** Not stated in the inputs; implied by the logout and change-password features.
- **Evidence:** `ConnectorJwtAuthenticationFilter` and `ConnectorJwtParser` check only signature, expiry and subject.
- **Suggested fix:** None given in the inputs.
- **Tester references:** WP2-04.

### BUG-20 No login rate limiting or lock-out

Severity: medium | Area: Security | Verification: Runtime

- **Observed:** Twelve wrong logins each return 401; a correct login afterwards returns 200.
- **Expected:** Not stated in the inputs; repeated failed logins are expected to be throttled.
- **Evidence:** Login sequence above.
- **Suggested fix:** None in the code; the security guidance under test recommends rate limiting at the proxy.
- **Tester references:** WP2-10.

### BUG-23 `connector.admin.enabled` has no effect

Severity: medium | Area: Security | Verification: Runtime (WP2); code reading (WP3)

- **Observed:** With `enabled=false` and a new admin name `admin2` but the same e-mail, the log shows `Administrator user [admin2] already registered.` and login returns 401. With a new e-mail, the user is registered and login returns 200 despite `enabled=false`. On restart, the log shows `Administrator user [admin] already registered.` The admin values are used only at creation.
- **Expected:** `enabled=false` creates a disabled admin, as the user docs under test describe.
- **Evidence:** `ConnectorAdminUserInitializer.java:180-190` sets `.enabled(Boolean.TRUE)`; the property is never read.
- **Suggested fix:** None given beyond the code location.
- **Tester references:** WP2-06, WP3-06.

### BUG-24 Outbound pipeline can run before the staged message is visible

Severity: medium | Area: Message processing | Verification: Runtime (one occurrence in seven outbound submissions)

- **Observed:** For message `183ca2d6`, the first pipeline attempt failed in the security step with `NullPointerException: "updatedMessage" is null`, then in the rejection step with `"foundMessage" is null`. JMS redelivery succeeded seconds later.
- **Expected:** The pipeline sees the staged message on its first attempt.
- **Evidence:** Outbound staging to processing (`ConnectorOutboundBusinessMessageStagerService` and the pipeline listener).
- **Suggested fix:** Publish to the processing queue only after commit, or make the XA ordering explicit.
- **Tester references:** WP5-04.

### BUG-25 Unresolved `${VAR}` placeholders are used as literal values

Severity: medium | Area: Configuration properties | Verification: Runtime

- **Observed:** With variables removed from `connector.env`: `DB_URL` gives `'url' must start with "jdbc"`; `DB_USERNAME` gives `password authentication failed for user "${DB_USERNAME}"`; `BROKER_URL` gives `URISyntaxException ... ${BROKER_URL}`; `S3_BUCKET` gives `"${S3_BUCKET}" must be a valid S3 bucket name`; `S3_ENDPOINT` gives an invalid URI error; `JWT_SECRET_KEY` gives a `WeakKeyException` (136 bits, the literal text); `TEMP_DIR_LOCATION` unset in the JAR gives a "must be a writable directory" error. `DB_DIALECT`, `S3_REGION`, `S3_ACCESS_KEY` and `DB_DRIVER_CLASSNAME` missing: the connector starts. Only `connector.queues.*` placeholders produce `Could not resolve placeholder`.
- **Expected:** Not stated in the inputs. The previous documentation expected `Could not resolve placeholder` errors.
- **Evidence:** Messages above.
- **Suggested fix:** None given in the inputs.
- **Tester references:** WP1b-02.

### BUG-26 Unreachable or misconfigured external services are detected after startup, or not at all

Severity: medium | Area: Configuration properties | Verification: Runtime

- **Observed:** A wrong S3 secret gives `S3Exception: Forbidden ... 403` after `Started ConnectorApplication`, and the process exits. An unreachable S3 endpoint gives `SdkClientException ... Connection refused` after the Started line, and the process exits. A wrong region (`eu-nowhere-9`) starts fine. With the endpoint line removed or `S3_ENDPOINT` empty, the AWS endpoint is used and the 403 appears after startup. An unreachable broker (`tcp://localhost:61999`) does not stop startup; the log repeats `Could not connect to broker URL` 27 times in 25 s.
- **Expected:** Not stated in the inputs; the failures are documented as startup errors.
- **Evidence:** The bucket check is an `ApplicationRunner`, which explains the order (testers' analysis).
- **Suggested fix:** None given in the inputs.
- **Tester references:** WP1b-03, WP1b-04, WP3-11.

### BUG-27 Keystores, truststores, key aliases and link names are not validated at startup

Severity: medium | Area: Configuration properties | Verification: Runtime

- **Observed:** These all start: a wrong evidence key alias, a wrong evidence key password, a wrong container alias, a wrong link keystore password, a wrong link alias, a wrong P-Mode truststore password, a missing link keystore path, a missing link truststore path, and a wrong private-key alias. A wrong evidence keystore password fails at use with `DSSException: Keystore was tampered with, or password was incorrect`. A missing evidence keystore fails at startup with `Failed to load JKS keystore`. A truststore with a `.dat` extension fails with `Cannot determine the keystore type`. Two links with the same `link-config.name` (and publish-address `/backend2`) start. A duplicate partner name fails at startup with `Duplicate link partner name detected`.
- **Expected:** The link-partner chapter says "a path that does not exist stops the connector"; a missing link truststore path contradicts that. Other expectations are not stated in the inputs.
- **Evidence:** Messages above. The wrong-P-Mode-truststore case is also reported via the API: `truststore.password=WRONG` returns 201.
- **Suggested fix:** Open the truststore when registering a P-Mode, and validate keystores and aliases at startup.
- **Tester references:** WP1b-13, WP3-03, WP3-09, WP4-10, WP4-12.

### BUG-28 Queue names are partly hard-coded; `gateway-reception-queue` is not configurable

Severity: medium | Area: Configuration properties | Verification: Runtime for renaming and statistics; resolved-queue listener consequence from code only

- **Observed:** All 11 queues renamed to `wp4.*` work for outbound messages. The listener for the resolved-message clean-up queue stays on its old name. `/admin/jms/queues/stats` reports the hard-coded default names. `/admin/configurations/queues` omits inbound-staging, resolved and reception. Removing the reply queue or inbound-staging gives `Could not resolve placeholder`; a blank value gives `must not be blank`.
- **Expected:** Configured queue names are used by all publishers, listeners and statistics.
- **Evidence:** `ConnectorMessageResolvedListener` (hard-coded listener); `ConnectorJmsQueueStatsProvider` (hard-coded names). `gateway-reception-queue` is not in `ConnectorQueueProperties` and is absent from the generated reference.
- **Suggested fix:** Use the configured names in the resolved-message listener and in the statistics; add `gateway-reception-queue` to `ConnectorQueueProperties`.
- **Tester references:** WP4-06, plan F25.

### BUG-29 Value `-` cannot disable a file-deletion job

Severity: medium | Area: Configuration properties | Verification: Runtime

- **Observed:** `connector.file-deletion.orphan.cronValid ... cron must be a valid Spring cron expression` is reported for `-`. The value `0 0 0 31 2 *` starts (the testers' workaround: a cron that never fires).
- **Expected:** A deletion job can be disabled. The inputs do not say what the documentation promised for `-`.
- **Evidence:** `DeletionRule.isCronValid`: `CronExpression.isValidExpression("-")` is false.
- **Suggested fix:** The `0 0 0 31 2 *` workaround documented by the testers.
- **Tester references:** WP4-04.

### BUG-31 `X-Forwarded-Proto` ignored by default behind a TLS proxy

Severity: medium | Area: Configuration properties | Verification: Runtime

- **Observed:** Behind an nginx TLS proxy, `/v3/api-docs` shows the server URL `http://localhost`, and the WSDL schema location also uses `http://`. With `server.forward-headers-strategy=framework` (environment `SERVER_FORWARD_HEADERS_STRATEGY`), the URLs become `https://localhost` and `https://localhost/services/backend?wsdl=...`. The nginx line `proxy_set_header Host $host` strips the port.
- **Expected:** Not stated in the inputs; implied by the proxy documentation.
- **Evidence:** Server URLs above.
- **Suggested fix:** Set `server.forward-headers-strategy=framework` (documented workaround).
- **Tester references:** WP1b-12.

### BUG-32 Link partners: push-address and encryption-alias required even for pull; sender-mode not validated

Severity: medium | Area: Configuration properties | Verification: Runtime

- **Observed:** A pull partner without `properties.push-address` fails with `linkPartners[1].properties.pushAddress ... must not be null`, so the documented pull example does not start. `properties.encryption-alias` is mandatory (`must not be blank`). Removing `encrypt-alias` gives `must not be null`. An omitted `sender-mode` starts, and the connector treats it as pull (the listener checks `== PUSH`). `PUSH` in upper case works; `foo` gives an enum conversion error. A `ftp://` push-address gives `pushAddressValid false`.
- **Expected:** The documented pull example starts.
- **Evidence:** Messages above.
- **Suggested fix:** None given in the inputs beyond adding a push-address to the documented pull example.
- **Tester references:** plan F22, WP3-09.

### BUG-33 XA prerequisites are not checked, or are reported late

Severity: medium | Area: Database scripts | Verification: Runtime

- **Observed:** MySQL 8.4 without `pinGlobalTxToPhysicalConnection=true` in the JDBC URL fails on first start with `XAER_INVAL: Invalid arguments (or unsupported command)`. MariaDB 12.3.2 behaves the same on a fresh database; on an existing database login returns HTTP 500. Without `XA_RECOVER_ADMIN` on MySQL, the connector logs `XAER_RMERR ... Error while retrieving xids` every 10 s. MariaDB needs no grant (`XA RECOVER` works). Oracle without grants logs the same message every 10 s; after grants from SYS, there are no warnings. With `max_prepared_transactions=0` on PostgreSQL, the application starts and admin login works, but the first outbound message returns HTTP 500 with `prepared transactions are disabled`.
- **Expected:** Not stated in the inputs. The testers note that the previous documentation implied a startup failure for `max_prepared_transactions=0`.
- **Evidence:** Messages above.
- **Suggested fix:** None given beyond the documentation changes.
- **Tester references:** WP1a-01, WP1a-04, WP1a-05, plan F02, F06, F07.

### BUG-34 ZIP drops execute permission on `start.sh`

Severity: medium | Area: Packaging and distribution | Verification: Runtime

- **Observed:** In the ZIP, `standalone/start.sh` and `start.bat` have mode 0644. `./start.sh` returns `Permission denied`. The documentation says the file ships with execute permission.
- **Expected:** `start.sh` is executable after unpacking, as the documentation says.
- **Evidence:** Mode 0644 of `standalone/start.sh`.
- **Suggested fix:** Fix the Zip task so it keeps the mode of the script.
- **Tester references:** WP1b-10, plan F09.

### BUG-35 `lib/` folder is never loaded; its driver versions differ from the JAR

Severity: medium | Area: Packaging and distribution | Verification: Runtime (JAR contents and image); the loader behaviour is from the testers' code reading

- **Observed:** `lib/` holds mariadb 4.0.0, mysql 9.7.0, ojdbc11 23.26.4.0.0 and postgresql 42.7.14. The JAR bundles mysql 9.7.0, mariadb 3.5.10, postgresql 42.7.14 and ojdbc11 23.26.3.0.0. The JAR's Main-Class is `JarLauncher`, so `-Dloader.path=./lib` in `start.sh` is ignored; `start.bat` does not pass it; the Docker image does not copy `lib/`.
- **Expected:** Not stated in the inputs; the driver versions should match the compatibility matrix, and `lib/` should either be loaded or not shipped.
- **Evidence:** Jar and folder listings above.
- **Suggested fix:** Maintainer decision (the inputs do not choose one).
- **Tester references:** WP1a-07, WP1b-10, plan F31.

### BUG-36 Outbound message with only file parts in `businessContent` is rejected as malformed

Severity: medium | Area: REST API | Verification: Runtime

- **Observed:** The documented `curl` for `POST /api/v1/messages/outbound` returns `400 {"message":"The [businessContent] is malformed or incomplete"}`. It returns 201 when `-F "businessContent.businessDocument.aesType=SIGNATURE_BASED"` (or `AUTHENTICATION_BASED`) is added. Any text part of `businessDocument` has the same effect; `detachedSignature.signature` with `mimeType=BINARY` and no `aesType` returns 201.
- **Expected:** The documented example is accepted.
- **Evidence:** Response body above.
- **Suggested fix:** None given beyond the code location (binding of a nested object that has only file parts).
- **Tester references:** plan F16, WP5-06.

### BUG-37 Rejected sender certificate returns HTTP 500 instead of a client error

Severity: medium | Area: REST API | Verification: Runtime

- **Observed:** With the partner DN changed to `cn=bob`, `POST /api/v1/messages/outbound` returns `{"status":500,"error":"Internal Server Error"}`. The reason appears only in the log: `ConnectorLinkPartnerException: No link partner found for certificateDn: cn=alice`.
- **Expected:** A client error (4xx) that gives the reason. The testers' note reads "should be 4xx".
- **Evidence:** Log line above.
- **Suggested fix:** Return a 4xx response for this case.
- **Tester references:** WP2-15.

### BUG-38 Business-document truststore-enabled and unknown trusted-list source not validated at startup

Severity: medium | Area: Trusted lists/DSS | Verification: Runtime

- **Observed:** With the business-document truststore removed, the connector starts; at runtime it logs `Trust store is set to enabled, but it is not configured!` (`ConnectorDssCertificateVerifier:171`). An unknown trusted-list-source name starts (a warning appears at validation time). `business-document.authentication-validation.identity-provider` is always mandatory (`authenticationValidation ... must not be null`), although the docs say the block is optional.
- **Expected:** Misconfiguration is rejected at startup. The identity-provider requirement should match the docs; the inputs do not say which is intended.
- **Evidence:** Messages above.
- **Suggested fix:** None given in the inputs.
- **Tester references:** WP4-09, plan F23.

### BUG-39 Generated property reference lacks defaults and omits some properties

Severity: medium | Area: Build and versions | Verification: Inspection of the generated metadata; not rebuilt by the testers

- **Observed:** The generated reference has no defaults for inherited or nested fields: `digest-algorithm`, `keystore.type`, and `ocsp/crl/aia-enabled` show empty cells instead of `sha256` and similar. Lists and maps get a single row. `gateway-reception-queue` and `scheduling.enabled` are absent.
- **Expected:** Not stated in the inputs; the reference is expected to list the bound properties with their defaults.
- **Evidence:** Metadata inspection (testers' note on `build.gradle.kts`).
- **Suggested fix:** Rebuild the reference after the metadata is fixed (the testers' DOC-FIXED change was not built).
- **Tester references:** WP4-15.

---

## Low

### BUG-40 Security matchers permit paths that do not exist

Severity: low | Area: Security | Verification: Runtime

- **Observed:** `GET /api/v1/services` is `permitAll` but returns 404. `GET /api/v1/evidences/{uuid}/download` returns 404; the existing endpoint `GET /api/v1/admin/evidences/{uuid}/download` returns 401 without a JWT. `/swagger-resources` returns 404, and `/swagger-ui.html` returns 401 (only `/swagger-ui/index.html` works).
- **Expected:** The security configuration matches the endpoints that exist.
- **Evidence:** Status codes above.
- **Suggested fix:** Remove the stale matchers.
- **Tester references:** plan F12, F13; WP2-11.

### BUG-41 Token of a deleted user returns HTTP 500

Severity: low | Area: Security | Verification: Runtime

- **Observed:** After a user is deleted, `GET /api/v1/admin/messages` with that user's token returns 500. The filter does not handle `UsernameNotFoundException`.
- **Expected:** Not stated in the inputs; the refresh endpoint returns 401 for a deleted user, which suggests the same here.
- **Evidence:** Response status above.
- **Suggested fix:** Handle the exception in the filter.
- **Tester references:** WP2-09.

### BUG-42 `connector.auth.security.jwt.issuer` is mandatory but unused

Severity: low | Area: Security | Verification: Runtime (decoded token) and configuration check

- **Observed:** The property must not be blank and has no default (`must not be blank`), but the issued tokens have no `iss` claim. Payload: `{"sub":"admin","iat":...,"exp":...,"roles":["ROLE_ADMIN"],"userId":...}`. The parser does not require it.
- **Expected:** The property is either used or optional. The user docs under test describe it as identifying the installation.
- **Evidence:** Decoded payload above; `ConnectorJwtGenerator` has no `issuer(...)` call.
- **Suggested fix:** None given beyond the documentation note "unused in this version".
- **Tester references:** WP2-05, plan F21.

### BUG-43 Unresolved-message clean-up leaves a transport step

Severity: low | Area: Message processing | Verification: Runtime

- **Observed:** With the unresolved clean-up at cron 5 s and threshold 1 s, the log shows `[1] messages deleted ... files: [4] deleted`. The `connector_messages`, attachments, evidences and business-documents tables are empty afterwards, but one transport step (an evidence push to the backend) remains.
- **Expected:** No transport step remains after the unresolved-message clean-up. Not stated explicitly in the inputs; the testers record the leftover step as a defect.
- **Evidence:** Log line above.
- **Suggested fix:** None given in the inputs.
- **Tester references:** WP4-05.

### BUG-44 Evidence trigger always builds a success evidence

Severity: low | Area: Message processing | Verification: Code reading only

- **Observed:** `ConnectorOutboundEvidenceMessageProcessorService.execute` contains the comment `// TODO check the type of evidence`, and calls `evidenceCreator.createSuccess(...)` also for NON_DELIVERY and NON_RETRIEVAL. No rejection reason is given.
- **Expected:** Not stated in the inputs; the TODO asks for the evidence type to be checked.
- **Evidence:** Source comment above.
- **Suggested fix:** None given beyond the TODO.
- **Tester references:** WP5-08.

### BUG-45 Duplicate `backendMessageIdentifier` and unchecked `service.type` accepted (question)

Severity: low | Area: Message processing | Verification: Runtime

- **Observed:** Two messages with the same `backendMessageIdentifier` are both accepted (HTTP 201). A message with `service.type` set to `wrong` is accepted (HTTP 201). The service is looked up by name only in `ConnectorBusinessMessageVerifierService.processServiceAndActionVerification`.
- **Question for maintainers:** Is accepting these values intended? The documentation under test describes this as the behaviour "in this version".
- **Evidence:** Responses above.
- **Suggested fix:** None given.
- **Tester references:** WP5-07, WP5-06.

### BUG-46 ANSI colour codes written to non-terminal log output

Severity: low | Area: Configuration properties | Verification: Runtime

- **Observed:** `log4j2.xml` writes ANSI colour codes also when the output is not a terminal; the codes appear in `run.log`.
- **Expected:** Not stated in the inputs; plain text is expected in non-interactive output.
- **Evidence:** Content of `run.log`.
- **Suggested fix:** None given.
- **Tester references:** WP2-20.

### BUG-47 Invalid log4j2 configuration URI warning in JAR run (question)

Severity: low | Area: Configuration properties | Verification: Runtime

- **Observed:** The JAR run starts with `main WARN Invalid URI file:config/log4j2.xml`, twice. The cause is `logging.config=file:./config/log4j2.xml`. The testers call it cosmetic and it is not documented. A read-only config mount works.
- **Question for maintainers:** Is the `logging.config` value intended?
- **Evidence:** Log lines above.
- **Suggested fix:** None given.
- **Tester references:** WP1b-09.

### BUG-48 PostgreSQL script reports 19 errors when the database owner is another user

Severity: low | Area: Database scripts | Verification: Runtime

- **Observed:** With a database owned by another user, the script logs `must be able to SET ROLE "connector"` 19 times. The tables are still created.
- **Expected:** Not stated in the inputs.
- **Evidence:** Error text above.
- **Suggested fix:** None given.
- **Tester references:** WP1a-03.

### BUG-49 `docker compose up -d` starts all nine services, including optional ones

Severity: low | Area: Template and compose files | Verification: Status not stated in the inputs (plan section 4 confirmed)

- **Observed:** `docker compose up -d` starts all nine services, including the optional MariaDB, PostgreSQL and Oracle containers (multi-GB image pulls). The `profiles` entries in the compose file are commented out.
- **Expected:** Optional services start only when requested.
- **Evidence:** Compose file (`profiles` commented out).
- **Suggested fix:** None given in the inputs beyond naming the services in the README.
- **Tester references:** plan F03.

### BUG-50 `docker/connector/config/` is not git-ignored

Severity: low | Area: Template and compose files | Verification: Status not stated in the inputs (plan section 4 confirmed)

- **Observed:** README step 3 copies keystores and configuration into `docker/connector/config/`, which is not git-ignored and shows as untracked.
- **Expected:** The directory is ignored, so keystores and passwords are not committed.
- **Evidence:** `git status` shows the directory as untracked (plan section 4, F04).
- **Suggested fix:** Add the directory to `.gitignore`.
- **Tester references:** plan F04.

### BUG-51 ActiveMQ compose healthcheck is permanently unhealthy

Severity: low | Area: Template and compose files | Verification: Status not stated in the inputs (plan section 4 confirmed)

- **Observed:** `curl -f http://localhost:8161` returns 401, so the ActiveMQ healthcheck is permanently `unhealthy`. The connector waits for `service_started`, so this is harmless.
- **Expected:** Not stated in the inputs.
- **Evidence:** HTTP 401 from the healthcheck URL.
- **Suggested fix:** None given in the inputs beyond a README note.
- **Tester references:** plan F05.

### BUG-53 Unknown role names silently ignored; user creation without password returns HTTP 500

Severity: low | Area: REST API | Verification: Runtime

- **Observed:** `POST` user with `"roles":["USER"]` returns 201 with `"roles":[]`. `POST` user without `password` returns 500. A password of `12345` returns 400 `Password must be between 6 and 255 characters`. The valid role names are `ROLE_ADMIN`, `ROLE_USER` and `ROLE_LOAD_TESTER`.
- **Expected:** Not stated in the inputs; an unknown role is expected to be rejected, and a missing password a client error.
- **Evidence:** Responses above.
- **Suggested fix:** None given in the inputs.
- **Tester references:** WP2-07.

### BUG-54 Versions in the test matrix and changelog differ from the shipped JAR (question)

Severity: low | Area: Build and versions | Verification: JAR inspection; the matrix and changelog are statements, not runtime tests

- **Observed:** The testing matrix states MySQL Connector/J 8.0.33, MariaDB Connector 4.0.0, Oracle 23.26.4.0.0 and Tomcat 11.0.15. The changelog states Spring Boot 4.1.0. The shipped JAR contains Spring Boot 4.1.2, Tomcat 11.0.27, mysql-connector-j 9.7.0, mariadb-java-client 3.5.10, ojdbc11 23.26.3.0.0, activemq-client 6.2.10, Atomikos 6.0.1, DSS 6.1 and CXF 4.1.0.
- **Question for maintainers:** Which versions were tested and released?
- **Evidence:** Versions above.
- **Suggested fix:** None given.
- **Tester references:** WP1a-07, WP1a-12, plan F31.

---

## Found in the two-connector end-to-end test (2026-11-13, official 7.1.0 image and ZIP)

Set-up: connector "blue" from `scm.ecodex.eu/docker/connector-ng:7.1.0` (MySQL), connector "red" from `connector-distribution-7.1.0.zip` (PostgreSQL), two Domibus gateways, own PKCS12 key material. Messages were exchanged in both directions (REST submit on red, SOAP submit on blue, SOAP pull and acknowledge on both sides). Verification: runtime.

### BUG-55 Inbound container check requires the partner's signing certificate in the connector's own signing keystore

Severity: high | Area: Message processing | Verification: Runtime

- **Observed:** With the partner's signing certificate only in the P-Mode truststore, every inbound business message is rejected: `ConnectorContainerException: Failed to load or parse incoming connector container`, caused by `IllegalStateException: Signature [...] certificate is not in the configured certificate source`. The message then hits BUG-07 (`has no prior evidence of type [RELAY_REMMD_ACCEPTANCE]`), stays in status `CREATED`, and the sender stays in `SUBMITTED`. After importing the partner certificate as a trusted entry into the keystore of `connector.container.signature.keystore`, the same message is accepted.
- **Expected:** Only the P-Mode truststore is used to trust partner signing certificates; the signing keystore holds the connector's own key only.
- **Evidence:** `ConnectorContainerDocumentSignatureChecker.check()` builds the certificate source from `connectorContainerProperties.getSignature().getKeystore()` (`createCertificateSource(signature.getKeystore())`) and `verifyCertificateSource` requires the signing certificate to be in it. The sample `connector-keystore.jks` contains both `connector_blue` and `connector_red`, which hides the problem with the test material.

### BUG-56 REST backend submission returns HTTP 500 unless a link partner with `certificate-dn=cn=alice` exists

Severity: high | Area: REST API | Verification: Runtime

- **Observed:** On a connector whose only link partner has its own certificate DN, `POST /api/v1/messages/outbound` returns `{"status":500,"error":"Internal Server Error"}`; the log shows `No link partner found for certificateDn: cn=alice`. After declaring an additional link partner with `certificate-dn=cn=alice`, the same request returns 201 and the message is attributed to that partner.
- **Expected:** The REST API identifies the calling backend, or rejects the call with a client error.
- **Evidence:** `ConnectorMessageController` calls `backendClientVerifier.getBackendClient("cn=alice")` (see also the related entry on the HTTP 500 for a rejected sender).

### BUG-57 Development compose file: the gateway can start before its database is initialised

Severity: low | Area: Template and compose files | Verification: Runtime

- **Observed:** The `gateway-db` health check (`mysqladmin ping`) succeeds while the init scripts are still running, so the gateway may start too early and fail with `Communications link failure`. With a health check that queries a table created by the scripts, both gateways started reliably.
- **Expected:** The gateway waits until the database initialisation has finished.

### Confirmed with the official 7.1.0 artefacts

`start.sh` in the released ZIP has mode 0644 (BUG-34); the released template has the `push-address` placeholder (BUG-17); `XAER_RMERR` warnings until `XA_RECOVER_ADMIN` is granted on MySQL; a pull acknowledgement does not trigger a `DELIVERY` evidence although the template sets `auto-trigger-delivery-evidences=true` (see BUG-15).

## Not verified

The testers could not check the following. Items marked "code reading" were checked from source only.

- Live backend SOAP call with WS-Security and PKCS12 keystores (BUG-27 context). SOAP rows in the runtime table were checked against the code only (for example "No link partner found for certificateDn"); no SOAP client was used.
- Whether backend traffic logging writes payloads in clear text (CXF `LoggingFeature`); no SOAP client was used.
- Permanent outbound pipeline failure and its SUBMISSION_REJECTION (BUG-08); only the transient case was observed.
- A successful end-to-end evidence trigger with a valid ASiC-S container (needs an inbound message with a valid container).
- The EU LOTL success path (BUG-18); the parsing failure was observed, not the successful load.
- A real proxy with user and password settings.
- The Domibus side of the queue renames (BUG-28): the gateway webapp in the test stack failed at startup. The consequence of renaming the resolved-message queue was checked from code only.
- Pull acknowledgement triggering a delivery evidence (BUG-15): code reading only.
- Runtime behaviour of `ebms-id-generator-enabled=false` (code reading only).
- Runtime inbound routing with the routing grammar (BUG-09): the failure path was checked from code.
- Functional test of `spring.activemq.packages.trust-all=false` (code reading only).
- Runtime behaviour of inbound verification mode `CREATE`.
- Relaxed-binding environment overrides for template properties in the configuration tests (the transaction-log case in BUG-12 used one and worked).
- What happens when a wrong key is first used (BUG-27).
- Refresh-token clean-up job execution (not observable without expired tokens).
- The nginx `client_max_body_size` between 200 and 210 MB.
- Minimum Docker Engine 24+ and Compose 2 versions; login to the image registry and the pull of the registry image; Windows `start.bat`.
- Health status of JMS in the actuator output.
- Migration guide 6.x column: only partly checked against the 6.1.0 tag.
- WSDL compatibility with 4.x and 6.x backends.
- Admin portal screens (processing modes).
- Documentation build checks: the Asciidoctor render of the database chapter was not run in this round.
- Persistent outbound pipeline failure other than the transient case (BUG-24 occurred once).

## Triaged as expected behaviour (2026-11-13)

- A. Any authenticated user may replace the truststore of a P-Mode; only creating a P-Mode requires the ADMIN role (former BUG-01, the clear-text password part remains BUG-01).
- B. `mustChangePassword` in the login response is advisory, a flag for the administration portal; the API does not block other calls (former BUG-04).
- C. The Actuator `health` and `info` endpoints are public, and the health template shows full details (former BUG-21).
- D. Inbound connector-to-connector test messages are intercepted whenever `connector.routing.enabled=true`; `connector.c2ctests.enabled` only controls sending test messages (former BUG-22).
- E. Business-domain values and the admin account from `application.properties` are applied only when first created; later changes do not update existing records (former BUG-30).
- F. The unresolved-messages clean-up permanently deletes the message and its database rows, including unconfirmed inbound messages; the orphan job deletes attachment rows (former BUG-43, deletion part).
