# e-CODEX Connector NG: documentation gap report

- **Repository:** `github.com/e-codex/connector-ng`, branch `develop` @ `90f5c13` (2026-10-28, "fix: correct post-download processing (#337)")
- **Note:** the `zugazagoitia/connector-ng` fork was 36 commits behind upstream `develop`. This review and the patch are based on **upstream** `develop`.
- **Scope:** `README.md`, `CHANGELOG.md`, the whole `connector-documentation/src/docs/asciidoc` tree (5 books, about 3,200 lines), the sample `connector-distribution/.../config/application.properties`, `docker-compose.yml`, the `Dockerfile` and the distribution build, all checked against the source code (`@ConfigurationProperties` classes, `@Value`/`@JmsListener` placeholders, security configuration, controllers).
- **Severity:** 🔴 wrong or dangerous (a user following the docs gets a broken or insecure result) · 🟠 missing or misleading · 🟡 language or cosmetic.
- **Line numbers** refer to the *original* upstream files.
- **Revision 2** (this version) applies two extra rules:
  - The shipped `application.properties` is treated strictly as a **template**: none of its values may be presented as a given in the docs.
  - Every 7.x statement about 6.x was checked against the **6.x repository** (`github.com/e-codex/connector`, `develop` @ `07ac39a`, release 6.1.0).
  - Sections **13** (template review), **14** (6.x comparison) and **15** (corrections to revision 1) are new. Other sections have been updated where the findings changed.

---

## 0. Executive summary

| # | Top findings | Sev. |
|---|---|---|
| 1 | The documented property names `connector.message-processing.outbound-verification-mode` and `inbound-verification-mode` **are not bound**. The code binds `outbound-message-verification-mode` and `inbound-message-verification-mode`. The docs, the migration guide and the sample config all use the wrong names, so the setting is silently ignored. | 🔴 |
| 2 | Several REST endpoints are **public, with no authentication**: `/api/v1/messages/outbound`, `/evidence-trigger`, `/attachments/upload`, `/evidences/{uuid}/download`, all of `/actuator/**` (with `show-details=always`) and Swagger. None of this is documented. Every REST-submitted message is attributed to the partner with DN `cn=alice` (hard-coded TODO). | 🔴 |
| 3 | There is **no security guidance**. The template ships test credentials that the docs did not flag as such (`admin`/`123456`, keystores `12345`), C2C test mode is **on by default**, CORS defaults to `*` with credentials, the ActiveMQ setting is `packages.trust-all=true`, and the shipped config uses `ddl-auto=update`. | 🔴 |
| 4 | The installation guides contradict each other and themselves: `.env` vs `docker.env`, "mutual TLS" vs "no native mTLS", "tables created automatically" vs "ddl-auto is for dev only", different registry paths, different DB version lists, and the JAR download link resolves to `7.1.0-SNAPSHOT-SNAPSHOT`. | 🔴 |
| 5 | A fresh install **fails to start**. The sample config points to `file:./config/pmode/pmode-blue.xml`, which the distribution doesn't ship, and the initializer aborts on an unreadable file. This is not documented. | 🔴 |
| 6 | The Gateway JMS chapter says the `activeMQ.*` properties go into the connector's `application.properties`. They belong in **Domibus `domibus.properties`**. The host examples include a scheme (`https://…`, `http://blue-activemq`), which produces invalid `tcp://http://…` URLs. | 🔴 |
| 7 | The README quick start is broken: the image tag `connector-ng:latest` ≠ compose `connector-ng:activemq`, the DB URL uses host port `30006` inside the network, the S3 endpoint is `localhost`, a "database at :40000" doesn't exist, and it says "Artemis" where the broker is ActiveMQ Classic. | 🔴 |
| 8 | About **35 configuration properties are undocumented**: CORS, auto-trigger delivery evidence, JTA/Atomikos, multipart, actuator, OTel, Swagger, the cert verifier (`trusted-list-source`, `ignore-store`…), DSS `tl-sources`/`signing-certs`/`policy-oid`, sender modes `pull`/`passive`, and more. | 🟠 |
| 9 | The changelog is incomplete. About 30 user-visible changes since 7.0.0 are missing, including **3 breaking API changes**. `CHANGELOG.md` and the AsciiDoc changelog are duplicated by hand. | 🟠 |
| 10 | The architecture docs reference **8 class names that don't exist** (stale after the #300/#301 refactors), and the DSS chapter links to a non-existent `dss_tools.html`. | 🟠 |
| 11 | **Template values the code silently depends on**: the business domain identifier `default_business_domain` is hard-coded for all gateway, SOAP and routing traffic, and `cn=alice` is **hard-coded in Java** for the REST backend API. Users who follow the instruction to replace the template values break inbound processing or the REST API without any hint in the docs (§13). | 🔴 |
| 12 | **`certificate-dn` must be written in lower case**: the template comment "lower- or UPPERcase is ignored" is false. The caller DN is lower-cased, but the configured value is compared verbatim, so a DN copied from a certificate (`CN=…`) never matches and the backend is rejected (§13). | 🔴 |
| 13 | The **6.x → 7.x migration guide was wrong on key points** when checked against 6.x: message processing existed as `processing.*` (the guide said "new in 7.x"), routing changed (`enabled` default true → **false**; only one rule per backend), and the P-Mode verification defaults and meaning were swapped. 6.x features such as evidence timeouts, content deletion and storage encryption are gone without mention (§14). | 🔴 |

---

## 1. Undocumented (or wrongly documented) configuration

### 1.1 Properties the code reads that are not documented

| Property | Default (code) | Where read | Status after patch |
|---|---|---|---|
| `connector.message-processing.auto-trigger-delivery-evidences` | **`false`** via `@Value` (`ConnectorJmsBackendMessageDeliveryListener:56`), but `true` in `ConnectorMessageProcessingProperties`. The two defaults disagree. | inbound delivery | Documented. Default mismatch flagged (§7). |
| `connector.cors.allowed-origins` / `allowed-methods` / `allowed-headers` / `allow-credentials` / `max-age` | `*` / all / `*` / `true` / `3600` | `ConnectorCorsProperties` | Documented in the new *Platform* chapter. |
| `connector.business-document.allowed-advanced-system-types` | all values | `ConnectorBusinessDocumentProperties` | Documented. |
| `connector.business-document.signature.validation.{truststore-enabled, trusted-list-source, ignore-store-enabled, ignore-store.*}` | `true`, –, `false`, – | `ConnectorCertificateVerifierProperties` | Documented. This is the missing link between "trust list sources" and validation; the DSS chapter mentions it but never says how. |
| `connector.container.signature.validation.*` (`constraints-xml`, `trusted-list-source`, `ignore-store*`) | `classpath:/102853/constraint.xml` | `ContainerSignatureProperties` | Documented. `constraints-xml` was in the template but not in the docs. **Revision 2:** inbound containers are validated with the **P-Mode truststore** (`ConnectorContainerDocumentSignatureChecker.resolveTruststore`), so `validation.truststore.*` / `truststore-enabled` have no effect there. This is now documented; revision 1 had wrongly listed `truststore.*`. |
| `connector.dss.trust-list-sources.<k>.tl-sources[i].tl-url` / `.signing-certs.*` | – | `DssTlSourceProperties` | Documented (was only in the full example). |
| `connector.dss.trust-list-sources.<k>.lotl-sources[i].signing-certs.*` | – | `DssLotlSourceProperties` | Documented. |
| `connector.dss.time-stamp-servers.<k>.policy-oid` | – | `DssTimestampServerProperties` | Documented. |
| `connector.dss.cache.refresh-cron` | `0 0 */6 * * *` | `DssCacheProperties` (**never read**) | Documented as "accepted, not used yet". |
| `connector.dss.proxy.http(s).user` / `password` / `excluded-hosts` | – | DSS `ProxyProperties` | Mentioned. Field names should be verified against the DSS version. |
| `connector.link.backend[n].link-partners[m].sender-mode` = `pull` / `passive` | – | `ConnectorLinkMode` | Documented. Previously only `push` was mentioned, although pull/download is a 7.1 feature. |
| `connector.link.backend[n].link-config.properties.endpoint.{keystore,truststore}.type` | `JKS` | `KeystoreProperties` | Documented. |
| `connector.link.backend[n].link-partners[m].receiver-mode` | – | `LinkPartnerProperties` (`// TODO: remove`) | Left out on purpose; listed in §8. |
| `connector.routing.default-backend-name` | `default_backend` | `ConnectorMessageRouterService` | Added to the table (was only in the example). |
| `spring.jta.*`, `spring.jta.atomikos.*` | – | Atomikos starter | Documented (Platform). |
| `spring.activemq.*`, `spring.jms.listener.*` | – | Spring Boot | Documented (Platform). Previously only in the migration guide. |
| `spring.servlet.multipart.*`, `server.tomcat.max-part-count`, `server.port`, `server.address`, `cxf.path` | – | Spring Boot | Documented (Platform). |
| `springdoc.*`, `management.*` (actuator, OTLP), `logging.config`, `spring.banner.location` | – | Spring Boot | Documented (Platform). |
| `TEMP_DIR_LOCATION` and the other env variables | – | sample config | Full env-var table added. Previously each guide showed a different subset, and the containerized guide used `ADMIN_PASSWORD`, which the sample config **doesn't read**. |

### 1.2 Documented properties that are wrong or have no effect

| Doc location | Problem | Sev. |
|---|---|---|
| `message-processing-configuration.adoc:51-59`, `migrate-from-6-to-7.adoc:99-109,129-130`, sample `application.properties:212,215` | `outbound-verification-mode` / `inbound-verification-mode` are **not bound**. The field names are `outboundMessageVerificationMode` / `inboundMessageVerificationMode`. Commit #294 made them settable but the docs were not aligned. | 🔴 |
| `evidence-configuration.adoc:21-24,61`, `migrate…:300-302,345`, sample config `:249` | `payload-digest-algorithm` (docs) and `digest-algorithm` (sample) **both have no effect**. `ConnectorSignatureProperties` has getters only, so SHA-256/RSA are fixed. | 🔴 |
| `migrate-from-6-to-7.adoc:43-46,55` | `connector.business-domains.defaults[0].pmode=` is not a property. The real ones are `pmode.file`, `pmode.truststore` and `pmode.truststore-password`. | 🔴 |
| `migrate-from-6-to-7.adoc:38-41` | Says `enabled` defaults to `true`. The code says **`false`** (`DefaultBusinessDomainProperties`). | 🟠 |
| `migrate-from-6-to-7.adoc:139-151,185` | `connector.file.storage.s3.enable` doesn't exist, and there is no local-FS fallback. This contradicts `s3-configuration.adoc:9`. | 🔴 |
| `connector-queue-configuration.adoc`, `gateway-queue-configuration.adoc` | Marked "Optional: Yes, by default…". In reality, every `@JmsListener` uses `${connector.queues.X}` **without a default**, so a missing property breaks startup. Also, **renaming has no effect on producers**: `ConnectorQueueProperties` fields are `final`, so publishers always use the default names. A user who renames a queue gets a split-brain setup. | 🔴 |
| sample config `:75-80` | The commented `connector.activemq.address-settings.*` can't be set (fields are `final`, no setters). | 🟠 |
| `dss-configuration.adoc:26` | `cache.location` is listed as optional. The class has `@NotNull` but no `@Validated`, so it falls back to `java.io.tmpdir/dss-cache`. Now documented with that default. | 🟡 |
| `dss-configuration.adoc:115` | Table row uses the literal `eu.lotl-sources[0]` instead of `*key*.lotl-sources[*index*]`. | 🟡 |
| `user.adoc:16` | Says the JWT secret is "Base64-encoded". It is used as raw UTF-8 bytes (`Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`). | 🟠 |
| `user.adoc:26` vs sample config | The doc default is `60m` (correct per code); the sample uses `15m`. The table didn't make this clear. | 🟡 |
| `link-partner-configuration.adoc:21` | Wrong prefix: says `connector.message-processing.*` instead of `connector.link.*`. | 🟠 |
| `business-domain-configuration.adoc:20` | Prefix typo: `connector.connector.business-domains.*`. | 🟡 |
| `link-partner-configuration.adoc:78-81` | Example `publish-address=/backend_1`. The security filter only permits `/services/backend`, so any other value gets **HTTP 401**. | 🔴 |
| `backend-routing-configuration.adoc` | Not documented: rules are stored in a map keyed by `link-name`, so **a second rule for the same partner overwrites the first**. Routing applies to the default business domain only. Conversation-based routing takes precedence. | 🟠 |
| `c2ctests.adoc` | Doesn't say C2C interception only works when `connector.routing.enabled=true` (the test rule is added only then). | 🟠 |
| `gateway-configuration.adoc:10,20,68` | These are Domibus properties, not connector `application.properties`. The host examples include a scheme. | 🔴 |
| `message-processing-configuration.adoc:32` | A stray `#` inside a table cell (copied from the properties file). | 🟡 |
| `evidence-configuration.adoc:29,56` | `classpath:keystores/...` in the doc vs `file:./config/keystores/...` in the sample. | 🟡 |

---

## 2. Unclear or missing instructions

| Location | Issue | Patch |
|---|---|---|
| `standalone-containerized.adoc:77` vs `:134` | Creates `.env` but runs `--env-file docker.env`. | Unified to `connector.env`. |
| `standalone-containerized.adoc:110` | Says "communicates … using mutual TLS". This contradicts `mtls.adoc:20` (no native mTLS). | Removed. |
| `standalone-containerized.adoc:32` | Says the connector "creates its own tables automatically", contradicting `database.adoc:47`. | Removed; points to the DB chapter. |
| `standalone-containerized.adoc:28-29,55,61,137` | Three different registry paths (`scm.ecodex.eu/docker/…`, `…/artifactory/docker/…`, `…/artifactory/ecodex-docker-snapshots/…`). The snapshot workflow pushes to `scm.ecodex.eu/ecodex-docker-snapshots`. No `docker login` step. | Unified, `docker login` added. **Maintainers must confirm the release path** (the release workflow doesn't show the registry). |
| `standalone-containerized.adoc` | No mention of persisting `/app/data` (JTA log for XA crash recovery, TL cache), the non-root UID 1000 permissions, `localhost`-inside-container pitfalls, or the missing `pmode/` folder. | Added an image layout table, a volume mount and warnings. |
| `standalone-jar.adoc:37-38` | Snapshot link uses `{project-version}-SNAPSHOT` but project-version is already `7.1.0-SNAPSHOT`. The release link points `libs-release` at a SNAPSHOT version. | Replaced with versioned URL patterns. |
| `standalone-jar.adoc:43-45` | `./gradlew build` runs all tests (Docker needed for integration tests) and has no JDK prerequisite. Only `standalone/` was mentioned, not the ZIP. | Fixed. |
| `standalone-jar.adoc:50-62` | Layout is wrong: `start.sh` actually sits next to `bin/`, inside `standalone/`. It omits `start.bat`, `log4j2.xml`, `banner.txt`, `dbscripts/` and `documentation/`. | Corrected tree. |
| `standalone-jar.adoc` | Doesn't say the sample config **requires environment variables** (`${DB_URL}` etc.), which a JAR user must export, or the app fails with "Could not resolve placeholder". | Added. |
| `standalone-jar.adoc:84` | `sudo chmod +x` is unnecessary (the build already sets `rwxr-xr-x`). No Windows instructions, and `start.bat` lacks `-Dloader.path`. | Fixed; the `start.bat` difference is noted. |
| `database.adoc:24-27` | XA privileges are only given for MySQL; nothing for PostgreSQL (`max_prepared_transactions`, which is only in compose) or Oracle. The sentence ends with "…alternative transaction-handling strategies;" and is never finished. | Per-vendor table; sentence rewritten. |
| `database.adoc` | No upgrade path between releases (only `initial` scripts exist) and no 6.x data migration statement. | Added a section. |
| `database.adoc` vs sample config | The DB chapter says `ddl-auto=update` is dev-only, but the shipped sample uses it. | Explicit warning plus `ddl-auto=none` guidance. |
| `business-domain-configuration.adoc:171,180` | `<<port>>` placeholder in curl (looks like an AsciiDoc xref). There's no mention of `mustChangePassword` blocking the first admin call. | Fixed. |
| `README.md:33` | `DB_URL=jdbc:mysql://connector-ng-db:30006/…`: 30006 is the *host* port; inside the network it is 3306. | Fixed. |
| `README.md:38` | `S3_ENDPOINT=http://localhost:9000` from inside the container. | Changed to `http://minio-s3:9000`. |
| `README.md:18` vs `docker-compose.yml:200` | Image tag mismatch (`connector-ng:latest` vs `connector-ng:activemq`). | Fixed. |
| `README.md:103` | "Database at http://localhost:40000" doesn't exist. | Removed; real ports listed. |
| `README.md` | No bucket-creation step (MinIO doesn't auto-create it) and no pmode step. | Added. |
| `DB_DRIVER_CLASSNAME` | The README/env samples use `com.mysql.cj.jdbc.MysqlXADataSource` (an XA DataSource, not a `java.sql.Driver`), while the sample config comments list `com.mysql.cj.jdbc.Driver`. | Both shown, flagged as an **open question** (§9). |

---

## 3. Language improvements (all applied in the patch)

- Typos: "PostgresSQL", "Table of Content", "they untested", "definitiv", "THe password", "Set By default to:", "bellow" (compose), "compatiblity" (sample config comment, left as is), "see<<…>>" (missing space), "( URL, credentials" (spacing).
- Sentence fragments: "The connector uses the DSS library. Which can be configured:" · "Brief summary of what changed in the configuration file." (a **placeholder left in the migration guide's Overview**) · "…alternative transaction-handling strategies;".
- Hedged or speculative wording copied from the properties comments into the docs and sample: "presumably ignored", "likely used for things like conversation IDs", "RELAXED *suggests* the connector is more tolerant". Replaced with what the code actually does.
- Inconsistent casing and naming: e-Codex / e-CODEX, ASIC-S / ASiC-S, Minio / MinIO, Active MQ / ActiveMQ, Mysql, Pmode / P-Mode / PMode, Adopt Open JDK (discontinued; the image uses Eclipse Temurin), tl / TL, crl/ocsp.
- "Optional = No" was used for properties that have defaults, and "Optional = Yes" for properties that are mandatory. A legend was added to the configuration index and the tables were corrected.
- Changelog: "*Message* — **Missing** security validation for incoming business message containers" listed under *Added* reads like a bug report. Reworded.
- Formatting: most tables lacked `options="header"`; bold markup leaked into inline code (`*key*`, `*secret*`); README "NOTE" boxes were fenced code blocks instead of GitHub alerts; mixed `*`/`-` bullets in the AsciiDoc changelog.

---

## 4. Flow and indexing of information

| Issue | Patch |
|---|---|
| No single **prerequisites** chapter. Each install option had its own inconsistent list (MySQL 8+/MariaDB 10.6+ vs MySQL 8/MariaDB vs a 4-DB table), and ports and certificates were described nowhere. | New `installation/prerequisites.adoc` (components, ports, key material, install order). |
| JAR came before Docker, although Docker is "Recommended". | Reordered: containerized first. |
| Configuration book order was arbitrary: business domain → message processing → DSS → container → … with the gateway (Domibus) broker and the migration guide mixed in. | Regrouped: Platform → Storage → Auth → Domains → Links → Routing → Processing → Evidence/Container/BusinessDoc/DSS → Queues → Gateway → C2C → Migration. |
| `user.adoc` lacked the `header.adoc` include and a `cols` spec, unlike all other chapters. | Fixed. |
| Cross-book links used `link:../configuration/index.adoc`, which stays `.adoc` in the HTML output. One link pointed to a non-existent `../architecture/dss_tools.html`. | Removed or replaced (verified: 0 broken anchors and 0 dangling relative links in the rendered HTML). |
| The evidence chapter duplicated message-processing and queue properties. | Replaced with cross-references. |
| The Architecture book had no overview (modules, external systems, pipeline). | New `architecture/overview.adoc` with a PlantUML context diagram and a module table. |
| Two parallel changelogs (`CHANGELOG.md` and `changelog-7.x.x.adoc`) maintained by hand and already out of sync (formatting). | Synced, with a "keep both in sync" note. Recommendation: generate one from the other (§6). |
| README didn't link to the real documentation at all. | Added a documentation index. |

---

## 5. Additional improvements identified (not all in the patch)

1. **`docker-compose.yml`**: the `connector-ng-maria-db`, `connector-ng-postgres-db` and `connector-ng-oracle-db` services have no `networks: ecodex`, so the connector **can't reach them**. Oracle mounts `./docker/oracle/init`, which doesn't exist. All four DBs start at once; use `profiles:`. *(Only the dbscripts path comment was fixed in the patch.)*
2. **Dockerfile**: `lib/` (JDBC drivers) isn't copied. This is harmless today because the drivers are also inside the boot jar, but `-Dloader.path=./lib` then points to nothing. Worth either copying it or dropping the flag. `start.bat` lacks `-Dloader.path`.
3. **MySQL driver coordinates**: `mysql:mysql-connector-java:8.0.33` are relocated and deprecated; use `com.mysql:mysql-connector-j`.
4. **Generate the property reference**: add `spring-boot-configuration-processor` and render `META-INF/spring-configuration-metadata.json` into the docs. This would have caught §1.2 automatically. Spring Boot can also fail on unknown keys (`ignoreUnknownFields=false`) under the `connector.*` prefixes.
5. **Docs CI**: the docs build only runs as part of `build`. Add a job that runs `asciidoctor --failure-level=WARN` plus a link check on PRs touching `connector-documentation/**`.
6. **Missing repository files**: `SECURITY.md` (vulnerability disclosure), `CONTRIBUTING.md` (commitlint/conventional commits are enforced by CI but undocumented), and a `docker.env.example` instead of an inline README block.
7. **API documentation**: there is no narrative guide for the REST backend API (outbound message multipart structure, pull/download/ack flow) or the SOAP API (WSDL location, WS-Policy expectations, push delivery contract). Swagger alone doesn't explain the flows.
8. **Operations guide** (missing entirely): logging (`log4j2.xml` levels), monitoring endpoints, backup (DB + S3 + `data/jtalog`), DLQ handling for failed JMS messages, TL refresh (restart needed), certificate rotation, and scaling (can two instances share a broker and DB?).
9. **Glossary**: AS4, ebMS, P-Mode, REM, LOTL/TL, ASiC-S, AES types, trust-ok token, business domain, link partner.

---

## 6. Prerequisites and installation options

| Gap | Patch |
|---|---|
| DB versions were inconsistent across 4 places (database.adoc: PG 18; matrix and compose: PG 17; containerized: "MySQL 8+, MariaDB 10.6+" with no PG/Oracle; JAR: "MySQL 8, MariaDB"). | One table in `database.adoc`, referenced from Prerequisites; PG aligned to 17 (the tested version). |
| Java runtime: there was no statement that the image bundles Temurin 21 and that a JDK is only needed for building. | Added. |
| Gateway prerequisites: the Domibus version and the **default JMS plugin** requirement were not stated. | Added (5.1.x, JMS plugin queues). |
| Broker: ActiveMQ Classic 6.x was stated, but the README said "Artemis". | Fixed. |
| Network/outbound access: DSS needs outbound HTTP(S) for TLs and CRL/OCSP (always enabled, see `ConnectorCertificateVerifierProperties`). This was not stated. | Added. |
| Registry access: an account is needed for `scm.ecodex.eu`. | Added. |
| No "verify installation" step for the JAR beyond URLs; the Docker guide lacked the actuator health check. | Health and WSDL checks added to both. |
| **Still missing** (needs maintainer input): sizing (CPU/RAM/heap), a supported OS list for the JAR, a Kubernetes/Helm option (the intro says "any hosting platform"), HA/clustering, and a systemd unit example. | Listed only. |

---

## 7. Change log

**Missing user-visible changes between `7.0.0` and `develop`** (from `git log 7.0.0..develop`, 111 non-dependency commits), now added to both changelogs:

- 🔴 **Breaking (REST):** multipart key `businessContent.contentFile` → `businessContent.xmlContent` (#276).
- 🔴 **Breaking (REST):** `/api/v1/auth/refresh` no longer takes the access token (#297).
- 🔴 **Breaking (config/behaviour):** authentication is now mandatory, and the message-processing properties became overridable (#294), exposing the wrong property names (§1.2).
- Pull mode / ready-for-download / bulk download / acknowledgement (#332, #334–#337).
- Client ownership check on ack/retrieve (#299); forced password change at first login (#222).
- Filters (direction, date range, status, identifier, service/action), reports and stats per business domain, P-Mode download, truststore update and cert info, queue stats, evidence and attachment download.
- DB indexes (#253) and LOB/ebMS column changes (#275, #293), which imply **schema changes with no upgrade script**.
- HTTP 400 instead of 403 for invalid enums (#316); evidence trigger made transactional (#329).

**Structural problems:** the release date was `0000-00-00` (now "Unreleased"); there was no "Upgrade notes / Breaking" section (added); in the Markdown, nested items were rendered as `***bold-italic***` top-level bullets (fixed); "Technical" was a different heading level in the md and the adoc (fixed). Two hand-maintained copies remain: generating `CHANGELOG.md` from the adoc (or vice versa), or from conventional commits (e.g. git-cliff), is recommended.

---

## 8. Pending implementations (should be documented as limitations)

| Item | Evidence | Documented now? |
|---|---|---|
| Native mTLS | `mtls.adoc:20` | Already documented; expanded with an Nginx example. |
| REST backend client identification (`cn=alice` hard-coded) | `ConnectorMessageController:75,93`, `ConnectorBackendClientVerifier:50` | ✅ in Security guidance (IMPORTANT box) |
| Local filesystem storage | `s3-configuration.adoc:10` | ✅ (migration guide no longer claims a fallback) |
| Periodic TL refresh (`refresh-cron` unused) | `DssCacheProperties`, `dss-configuration.adoc:130` | ✅ |
| Time-stamping not used | `dss-configuration.adoc:154` | Already documented |
| Configurable queue names and redelivery policy | `final` fields in `ConnectorQueueProperties`, `ActiveMQAddressSettingsProperties` | ✅ |
| Configurable evidence/container digest and signature algorithms | getter-only `ConnectorSignatureProperties` | ✅ |
| Per-business-domain routing | `ConnectorPropertyMessageRoutingConfigurationProvider` builds rules only for `DEFAULT_BUSINESS_DOMAIN_ID` | ✅ |
| `CREATE` verification mode | `ConnectorBusinessMessageVerifierService.processCreateVerification` rejects everything | ✅ |
| Actuator `metrics` (CXF incompatible with Boot 4) | sample config `:136-139` | ✅ |
| Retry of FAILED transport steps | `ConnectorAckMessageTransportStepService:43` TODO | ❌ (needs confirmation) |
| Streaming of large files (DSS `InMemoryDocument`, `readAllBytes`) | `ConnectorContainerBuilder:219`, `ConnectorContainerValidator:423,447` | ❌ Recommend documenting a maximum tested payload size. |
| Cleanup of the S3 ASiC-S object on validation failure | `ConnectorContainerValidator:146` | ❌ |
| Password rotation policy | `ConnectorRegisterUserService:78` | ❌ |
| REST/JMS backend integration ("foundation laid" in 7.0.0) | changelog | ❌ The current state is unclear. |
| Removal of `receiver-mode` | `LinkPartnerProperties:29` | ❌ |
| Evidence timeouts (6.x `connector.controller.evidence.*`) | No equivalent in 7.x code | ✅ listed in the migration guide as a missing feature |
| Automatic content deletion (6.x `connector.controller.content.*`) | No equivalent in 7.x code | ✅ listed in the migration guide |
| Multi-business-domain processing for gateway/SOAP traffic | `DEFAULT_BUSINESS_DOMAIN_ID` hard-coded (§13.2) | ✅ documented as a constraint |
| REST backend client authentication | `cn=alice` hard-coded | ✅ |

---

## 9. Security guidance documentation

Before the patch there was **no security chapter**. The only security content was `mtls.adoc` (about 20 lines) and two warnings (S3 credentials, JWT/admin defaults). The new `installation/security-guidance.adoc` covers:

- A 14-point **hardening checklist** (keystores, JWT secret, admin credentials, reverse proxy, public endpoints, Swagger, Actuator, CORS, C2C, link traffic logging, broker hardening, DB least privilege, S3 scoping, secret injection).
- The **default credentials inventory** (sample config, keystores, compose stack, unauthenticated JMX in compose).
- Authentication details: HMAC secret semantics, refresh tokens, role matrix.
- The full **list of unauthenticated endpoints**, as derived from `WebSecurityConfiguration`.
- The `cn=alice` REST limitation and the 401 pitfall when `publish-address` ≠ `/backend`.
- Transport security, validation modes, logging and personal data, JTA log persistence, the non-root container user, and vulnerability reporting.

**Security-relevant code and config observations for maintainers** (not fixable in docs alone):

1. 🔴 `/api/v1/messages/outbound`, `/evidence-trigger`, `/attachments/upload` and `/evidences/{uuid}/download` are `permitAll()` and attribute to `cn=alice`. Anyone who can reach the port can submit messages on behalf of a backend.
2. 🔴 `/actuator/**` is `permitAll()` with `management.endpoint.health.show-details=always` in the sample. That is fine for `health`, but any endpoint later added to `exposure.include` becomes public.
3. 🟠 `ConnectorCorsProperties` defaults to `allowedOrigins=*` with `allowCredentials=true`. Spring rejects that combination at request time; the sample overrides it with `http://localhost:3000`, which is also not production-ready.
4. 🟠 `spring.activemq.packages.trust-all=true` in the sample enables Java deserialization of arbitrary classes from the broker.
5. 🟠 `connector.c2ctests.enabled` defaults to `true`.
6. 🟡 The vulnerability contact in the new chapter (`e-codex@eulisa.europa.eu`) is taken from the Dockerfile `maintainer` label. **Please confirm** it is the right disclosure channel.

---

## 10. Code issues surfaced while reviewing (outside the patch)

| # | Issue | File |
|---|---|---|
| C1 | **`RELAXED` verification performs more checks than `STRICT`**: STRICT checks service/action only, RELAXED also checks from/to parties. This looks inverted relative to the names and Javadoc. | `ConnectorBusinessMessageVerifierService:62-75` |
| C2 | `auto-trigger-delivery-evidences` default is `false` in `@Value` and `true` in the properties class (the admin configuration endpoint may report the wrong value). | `ConnectorJmsBackendMessageDeliveryListener:56`, `ConnectorMessageProcessingProperties` |
| C3 | Queue names: listeners read the property, publishers use `final` defaults. Renaming breaks the flow. `gateway-reception-queue` is missing from `ConnectorQueueProperties`. | `ConnectorQueueProperties` |
| C4 | `ActiveMQAddressSettingsProperties`: `final` fields, no setters, so it can't be configured despite the `@ConfigurationProperties` annotation and the sample comments. | |
| C5 | `ConnectorSignatureProperties`: no setters, so the documented digest/encryption algorithms can't be configured. | |
| C6 | The `/services/backend` matcher is hard-coded while `publish-address` is configurable. | `WebSecurityConfiguration` |
| C7 | Routing rules map is keyed by link name (silent overwrite) and is built only for the default business domain; `getDefaultBackendName` may NPE for other domains (`.get(id)` returns null). | `ConnectorPropertyMessageRoutingConfigurationProvider`, `ConnectorMessageRouterService` |
| C8 | `DssCacheProperties.refreshCron` is never used. | |
| C9 | `certificate-dn` is not lower-cased when loaded, but the caller DN is, so any upper-case letter in the configured DN never matches. This is a regression from the 6.x behaviour, where case was ignored. | `ConnectorPropertyLinkPartnerProvider:85-92`, `ConnectorFindLinkPartnerService:36` |
| C10 | All gateway, SOAP, evidence and routing traffic is bound to the hard-coded `default_business_domain`. | `ConnectorJmsGatewayMessageListener:124,137`, `MessageHelpers:76`, `ConnectorInboundEvidenceMessageReceiverService:76` |
| C11 | The REST backend API resolves the caller from the hard-coded `cn=alice`. | `ConnectorMessageController:75,93` |
| C12 | The routing `enabled` default changed from `true` (6.x) to `false`, and multiple rules per backend are no longer possible. These are behaviour changes, not just documentation. | `ConnectorMessageRoutingProperties`, `ConnectorPropertyMessageRoutingConfigurationProvider` |

---

## 11. Open questions for maintainers

1. What is the official **release image path**: `scm.ecodex.eu/docker/connector-ng:<tag>`? Is a `latest` tag pushed?
2. Should `DB_DRIVER_CLASSNAME` be the JDBC driver or the XA DataSource class? The project currently uses both.
3. Is C1 (STRICT/RELAXED) intended? If yes, the names deserve an explanation; if not, it's a bug.
4. Should the verification-mode properties be renamed in code (keeping the documented names) instead of in the docs? The patch follows the code.
5. Which upgrade path is supported from 7.0.0 to 7.1.0 for the database schema?
6. Is `e-codex@eulisa.europa.eu` the vulnerability disclosure contact?
7. Is it intended that all gateway and SOAP traffic is bound to `default_business_domain`? If multi-domain support is planned, the docs should state the roadmap.
8. Should `certificate-dn` be lower-cased when it is loaded (`ConnectorPropertyLinkPartnerProvider`), to restore the 6.x case-insensitive behaviour? This is a one-line code fix.
9. Are the 6.x **evidence timeouts** (automatic `RelayREMMDRejection`, `NonDelivery` and `NonRetrieval` when no evidence arrives) and **content deletion** intentionally dropped, or planned for 7.x?
10. Were the swapped verification defaults and meaning (6.x: incoming `STRICT`, outgoing `RELAXED`, and `STRICT` stricter) intended?

---

## 12. About the patch

- **Files:**
  - `connector-ng-docs.patch`: 3 commits; apply with `git am` on top of upstream `develop` @ `90f5c13`.
  - `connector-ng-docs-squashed.diff`: the same changes as one diff; apply with `git apply`.
  - Both were verified to apply cleanly. `git am` prints a harmless whitespace warning for an intermediate commit; the final result is clean.
- **Pushed to:** `zugazagoitia/connector-ng`, branch `ccr-f7d06d15-j78r5p`.
- **39 files changed** (+2,574 / −845 lines).
  - **New chapters (6):**
    - Installation: prerequisites, certificates, security guidance, troubleshooting.
    - Configuration: *Preparing application.properties*, platform.
  - **New architecture page:** an overview.
  - **Updated:** all other chapters, `README.md`, `CHANGELOG.md`, the template `application.properties` and one comment in `docker-compose.yml`.
- **Changes to the template `application.properties`:**
  - A header now states that it is a template.
  - The verification-mode keys are renamed. This changes nothing at runtime, because the new keys carry the defaults.
  - The non-bound `digest-algorithm` line is removed.
  - The false `certificate-dn` comment is corrected.
  - The other edits are comments only.
- **Validation:**
  - All five books render with Asciidoctor 2.0.26 with no warnings or errors (`-v`).
  - The rendered HTML has 0 broken internal anchors and 0 dangling relative links; the original had 2 dangling links.
  - No markup leaks: no stray bold inside inline code.
  - The Gradle `asciidoctorAll` task could not be run here because Maven Central rate-limited the dependency download (HTTP 429), so PlantUML diagrams were not rendered. Please run `./gradlew :connector-documentation:build` once locally.

---

## 13. Template review: `application.properties` is a placeholder

**Principle applied:** the shipped file contains placeholders and values from the e-CODEX "blue" test environment: test partner `alice`, keystores with password `12345`, `pmode-blue.xml`, country `BL`, `TestProvider`, `ERV`, `Unknown Street 1`, `admin`/`123456`, and so on. The documentation now:

1. Opens the configuration guide with a new chapter, **"Preparing application.properties"**. It classifies every template block as *Replace*, *Environment*, *Review* or *Keep*, and says how to determine each value: from your P-Mode, your certificates (`keytool -list` / `-printcert`), your backend teams, and your organisation data.
2. Distinguishes, in every table, a real **default** (the value used when the property is absent, taken from the code) from a **template value** (which is never a default).
3. Uses `<placeholders>` instead of template values in every example. Template values are only named to say that they must be replaced.
4. Lists the values that must match other components (gateway JMS plugin, P-Mode parties and services, backend certificates, routing ↔ link partner names).
5. Adds a **Certificates** chapter (adapted from 6.x) describing how to produce and exchange each key and certificate, and a **Troubleshooting** chapter listing the errors a user gets when template values are left in place.

### 13.1 Template values that the documentation presented as givens (now fixed)

| Template value | Where it was presented as a given | Fix |
|---|---|---|
| `admin` / `123456` / `admin@test.com` | README, user chapter ("default"), security guide | Described as template values that must be set before the first start. |
| `server.port=30000` | "Default port 30000" in several places | Template value. The Spring Boot default is 8080; the image exposes 30000. |
| `connector.auth.security.jwt.expiration=15m` | User chapter example | The code default is 60m; the value must follow your security policy. |
| `connector_blue`, `12345`, `connector-keystore.jks` … | Evidence, container, link and business-document examples | `<signing-key-alias>`, `${…PASSWORD}`, `<…>.p12` plus generation steps. |
| `backend_alice`, `cn=alice`, `alice` alias, `http://localhost:port/…` | Link-partner and routing examples | `<backend-…>` placeholders and a derivation procedure. |
| `equals(ServiceName,'Test')` | Routing example | Placeholder services from your P-Mode. |
| `TestProvider`, `BL`, `ERV` | Business-document examples | Explained as test values (BL = "blue" test country, ERV = Austrian test IdP). |
| `Unknown Street 1`, `Connector NG`, `mailto:gateway@connector.example` | Evidence issuer examples | Must be your legal address and your gateway party from the P-Mode. In 6.x the party was taken from the P-Mode. |
| `connector.ecodex.eu` suffixes | Message-processing examples | Replace with a domain of your organisation. (This is also the code default, which is equally inappropriate.) |
| `pmode-blue.xml`, `truststore-blue.jks` | Business-domain examples, install steps | `<your-pmode>.xml`. The file isn't shipped, so startup aborts if the template is kept. |
| `http://localhost:3000` CORS, `ddl-auto=update`, `show-details=always`, Swagger on, `packages.trust-all=true`, `logging-enabled=true` | Platform/security text | Marked *Review*, with production values given. |
| `dss.nowina.lu/pki-factory` TSAs | DSS examples | Flagged as public **test** TSAs, not for production. |

### 13.2 Template values the code silently relies on (the reverse problem)

Users are rightly told to replace template values, but some of them are **assumed by the code**. Changing them breaks the Connector without a clear error. These are now documented in the *Keep* category and in the Troubleshooting chapter:

| Value | Evidence in code | Effect of changing it |
|---|---|---|
| Business domain identifier `default_business_domain` | `ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID` is used by `ConnectorJmsGatewayMessageListener:124,137`, the SOAP `MessageHelpers:76`, `ConnectorInboundEvidenceMessageReceiverService:76` and routing | Inbound gateway messages and SOAP submissions fail with "Processing mode … not found". 🔴 |
| `cn=alice` (your example) | Not a property: **hard-coded in Java** in `ConnectorMessageController:75,93` (`getBackendClient("cn=alice")`) | Once the test partner is replaced (as required), the REST backend API rejects every submission with "No link partner found for certificateDn: cn=alice". Re-adding a partner with that DN would let anyone submit on its behalf, so the docs now say not to. The SOAP API is unaffected: it reads the DN from the WS-Security principal (`ConnectorBackendWebServiceController:408`). 🔴 |
| `certificate-dn` letter case | `ConnectorFindLinkPartnerService:36` lower-cases the caller DN; `ConnectorPropertyLinkPartnerProvider.findByCertificateDn` compares with `Objects.equals` against the configured value, which is **not** lower-cased | Any upper-case letter in the configured DN, e.g. copied as `CN=…` from the certificate, gets the backend rejected. The template comment said case is ignored; it is now corrected. 🔴 |
| `publish-address=/backend`, `cxf.path=/services` | `WebSecurityConfiguration` permits only `/services/backend` | HTTP 401 for all SOAP calls. |
| `connector.queues.*` | Producers use `final` defaults | Split between producer and consumer queues. |
| `connector.routing.default-backend-name` | Fallback `default_backend` (`ConnectorMessageRouterService`) | Removing it routes to a partner that doesn't exist. |
| `server.port` | Spring Boot default 8080 vs image `EXPOSE 30000` | Port mismatch in Docker. |

### 13.3 Recommendations for the template itself (not in the patch)

- Use **fail-fast placeholders** instead of working test values. For example, use `${ADMIN_PASSWORD}`, `${EVIDENCE_KEYSTORE_PASSWORD}` and `${ORGANISATION_COUNTRY}` with no default, so that an unedited template doesn't start with test identities. This also fixes the security issue of the `123456` admin password.
- Ship the test keystores and the `alice` partner in a separate `application-test.properties` / test profile, not in the default file.
- Leave `connector.c2ctests.*` and `connector.routing.*` explicit (not commented), so the effective behaviour is visible.
- Consider `ignoreUnknownFields=false` on the `connector.*` property classes. 6.x failed fast on invalid properties (`connector.common.fail-on-invalid-property=true`).

---

## 14. Comparison with connector 6.x (`github.com/e-codex/connector`)

Reviewed: `develop` @ `07ac39a` (6.1.0), the `domibusConnectorDocumentation` tree (about 60 AsciiDoc pages), the template `domibusConnectorDocumentation/src/main/config/properties/connector.properties`, and the `@ConfigurationProperties` classes.

### 14.1 Errors found in the 7.x migration guide (all fixed in the patch)

| 7.x guide said | 6.x reality (code) | Sev. |
|---|---|---|
| Message processing "new in 7.x, no equivalent in 6.x" | `processing.*` (`ConnectorMessageProcessingProperties`, prefix `processing`): `send-generated-evidences-to-backend`, `ebms-id-generator-enabled`, `ebms-id-suffix` (default `ecodex.eu`), `outgoing-p-mode-verification-mode` (default **RELAXED**), `incoming-p-mode-verification-mode` (default **STRICT**). 7.x swapped both defaults. | 🔴 |
| Verification modes (implicit: same meaning) | The 6.x doc `verify_pmodes_step.adoc` says STRICT requires *all* attributes (service and type, action, party id, type and role), while RELAXED fills in missing ones. In 7.x, STRICT checks only service/action and RELAXED also checks parties. | 🔴 |
| "Message routing unchanged, no migration action required" | 6.x `DCMessageRoutingConfigurationProperties.enabled = true`; 7.x defaults to **false**, so a migrated file silently stops routing. 6.x allowed several rules per backend (with `priority`); 7.x keeps one per `link-name`. 6.x also had gateway rules. | 🔴 |
| C2C tests "new in 7.x" | 6.x `c2ctests.*` (no `connector.` prefix; `service.service-type`; `respond-with-delivery-evidence`; default business XML/PDF). | 🟠 |
| Admin account "new in 7.x" | 6.x `connector.init.user.*` with a **random** initial password logged to console and a mandatory change. 7.x requires a configured password, so the template ships `123456`. | 🟠 |
| P-Mode via business domains "new" | 6.x `connector.init.pmode.*` (upload at first start); default lane `defaultBusinessDomain` (7.x: `default_business_domain`). | 🟠 |
| JTA "replaces the 6.x transaction manager" (revision 1 wording) | 6.x already used Atomikos (`spring.jta.atomikos.*`); only 7.0.0 used Narayana. | 🟡 |
| Datasource | 6.x documented `spring.datasource.xa.data-source-class-name` (and JNDI for the Tomcat WAR). This answers open question 2: the 7.x template's use of `MysqlXADataSource` as `driver-class-name` departs from the 6.x/Spring convention. | 🟠 |

### 14.2 6.x settings and features with no 7.x equivalent (now listed in the migration guide)

| 6.x | Status in 7.x |
|---|---|
| `connector.controller.evidence.*` (evidence **timeouts**: `relayREMMDTimeout`, `deliveryTimeout`, `retrievalTimeout`, warn timeouts, `checkTimeout`) | **Missing feature.** No automatic rejection/non-delivery evidences when the expected evidence doesn't arrive. |
| `connector.controller.content.*` (**content deletion**) | **Missing feature.** No automatic purge; use S3 lifecycle rules and DB housekeeping. |
| `connector.persistence.*` (FS or DB storage, **AES encryption** of stored content) | Replaced by S3; the Connector no longer encrypts content itself. |
| `connector.link.gateway.*` (WS pull/push plugin, `pull-interval`) | Replaced by the Domibus JMS plugin. |
| Certificate verifier `ocsp-enabled`, `crl-enabled`, `aia-enabled` | Hard-wired to `true`. |
| `allow-system-type-override-by-client` | → `allowed-advanced-system-types`. |
| `connector.instance-name`, `connector.stage.*`, `connector.common.fail-on-invalid-property` | Removed (7.x silently ignores unknown properties). |
| `connector.ui.*`, session cookie, Tomcat WAR/JNDI, Liquibase (experimental) | Removed. |
| Evidence issuer AS4 party and container truststore "set by P-Mode upload" | The issuer must now be configured manually; the container truststore is still the P-Mode truststore. |

### 14.3 Documentation content in 6.x missing from 7.x

| 6.x page | Action |
|---|---|
| `installation/certificates.adoc`, `installation/backend_configuration.adoc` (keytool procedures, "the DN identifies the backend") | **Ported**, adapted, as `installation/certificates.adoc`. |
| `architecture/backend_link_mechanisms.adoc` (push/push, push/pull, acknowledged pull: `listPendingMessageIds` / `getMessageById` / `acknowledgeMessage`) | **Ported** into the link partner chapter. The same operations exist in the 7.x WSDL. |
| `installation/startup_errors.adoc` | **Ported** and extended as `installation/troubleshooting.adoc`, with real 7.x error messages. |
| `architecture/message_processing_steps/verify_pmodes_step.adoc` | Used to document the changed semantics. |
| `configuration/logging.adoc`, `monitoring.adoc`, `ha.adoc`, `database_cleaning.adoc` | **Not ported.** The 7.x behaviour (HA with a shared broker and DB, cleanup) needs maintainer input. Recommended as an *Operations guide*. |
| `architecture/storage_encryption_of_data.adoc`, `payload_handling.adoc`, `multiusecasepmode.adoc` | Not ported. Storage encryption no longer applies; multi-use-case P-Modes in 7.x are limited by the hard-coded default domain (§13.2). |
| `versioning_and_compatibility/versioning.adoc`, `upgrade/upgrade-*.adoc` (per-version upgrade manual) | Not ported. A 7.x versioning policy and a per-release *Upgrade* book are recommended; the 7.1 upgrade notes are currently only in the changelog. |
| `domibusConnectorAPI/.../interface_description.adoc` (SOAP API description) | Not ported. A 7.x API guide (SOAP and REST) is still missing (§5.7). |
| `ui/**` (administration UI help) | Not applicable: the 7.x portal is a separate application. Its documentation should live with it. |
| Aggregated Spring property list (`aggregated-spring-properties.html`) | Recommended again for 7.x via `spring-boot-configuration-processor` (§5.4). |

---

## 15. Corrections to revision 1

| Item | Revision 1 | Revision 2 |
|---|---|---|
| Container validation truststore | Documented `connector.container.signature.validation.truststore.*` as the truststore for inbound containers | **Wrong.** The code uses the P-Mode truststore. Corrected in the container, business-domain, prerequisites and certificates chapters. |
| `cn=alice` | Described as a REST limitation | Now explained as a **hard-coded Java value**, unrelated to the template, which makes the REST API unusable once the template partners are replaced. A warning against re-creating a `cn=alice` partner was added. |
| 6.x migration statements | Kept the guide's claims "message processing new", "routing unchanged", "JTA replaced" | Corrected against the 6.x code (§14.1). |
| Examples | Several examples still used template values (`12345`, `connector_blue`, `backend_alice`, `TestProvider`…) | Replaced by placeholders throughout, including the migration guide. |
| Open question 2 (driver class) | Open | Partly answered by 6.x practice (`spring.datasource.xa.data-source-class-name`); documented. Still needs confirmation for 7.x. |

