# 04 – Configuration, secrets, deployment and supply chain

Scope: `connector-distribution/src/main/resources/config/application.properties`, `Dockerfile`, `docker-compose.yml`, `docker/**`, `gradle/libs.versions.toml`, `build.gradle.kts`, `.github/**`. Read-only audit of the code at branch HEAD (75b18f6d). Secrets are redacted beyond 4 characters; the shipped values are short placeholders (`1234…`, `1234…`) and are quoted only as far as needed.

## Security model

- **Configuration.** The connector is a Spring Boot app started by `bin/start.sh:13` with `--spring.config.location=./config/`. Production config is the shipped `config/application.properties`. DB, JMS, S3 and the JWT signing key are read from environment placeholders (`${DB_PASSWORD}`, `${BROKER_PASSWORD}`, `${S3_SECRET_KEY}`, `${JWT_SECRET_KEY}`, … – `application.properties:28-32,63-67,423`). They have no defaults, so startup fails if they are unset.
- **Not externalised.** Every keystore/truststore/private-key password, the admin account and the truststore password are literal values in the properties file (all `12345`, admin `123456`). Matching sample `.jks` files are committed to git next to it.
- **Container.** Multi-stage `Dockerfile` (Gradle build → `eclipse-temurin:21-jre-jammy`), runs as non-root user `connector` (UID 1000). Config is volume-mounted at `/app/config` by compose.
- **Local/dev stack.** `docker-compose.yml` starts MinIO, ActiveMQ, MySQL ×2, MariaDB, Postgres, Oracle, a Domibus gateway and the connector on one bridge network, with most services also published on the host. Credentials are hardcoded in the compose file; connector env comes from an untracked `docker.env` (`*.env` is git-ignored, `.gitignore:43`).
- **Supply chain.** Dependencies are pinned in a version catalog and `spring-boot`/`cxf`/`dss` BOMs. GitHub Actions are partly SHA-pinned; most of the CI/release/security logic lives in reusable workflows in the external `e-CODEX/workflows` repository. Dependabot covers gradle, docker and github-actions.
- **Trust assumption (needs confirmation):** the shipped files are meant as samples and operators override them; nothing in the repo enforces or checks that.

## Findings table

| ID | Title | Severity |
|----|-------|----------|
| CFG-01 | Default admin account `admin`/`123456` enabled in shipped config | High |
| CFG-02 | Hardcoded keystore/key/truststore passwords (`12345`) in shipped config | High |
| CFG-03 | Sample private keys committed and used by default config | High |
| CFG-04 | `spring.activemq.packages.trust-all=true` (JMS deserialization) | High |
| CFG-05 | Docker-compose publishes DBs/broker/MinIO/gateway to host with default creds | High |
| CFG-06 | ActiveMQ JMX exposed without auth or TLS; extra hardcoded broker admin | High |
| CFG-07 | Domibus gateway config: default passwords, MySQL without TLS | Medium |
| CFG-08 | Swagger UI / OpenAPI docs enabled by default | Medium |
| CFG-09 | Actuator health shows full details; server binds 0.0.0.0 | Medium |
| CFG-10 | `ddl-auto=update` and relaxed inbound verification as defaults | Medium |
| CFG-11 | CORS fixed to `http://localhost:3000`; backend SOAP logging enabled | Low |
| CFG-12 | Outdated MySQL JDBC driver `mysql-connector-java 8.0.33` | Medium |
| CFG-13 | Dockerfile: unpinned base images, no HEALTHCHECK, JRE image with full build context copy | Medium |
| CFG-14 | Floating `latest`-style/mutable image tags in compose | Low |
| CFG-15 | GitHub Actions: unpinned third-party actions and `@main` reusable workflows | Medium |
| CFG-16 | CI `unit-test` job grants `contents: write` on pull_request/push | Medium |
| CFG-17 | `secrets: inherit` into reusable Sonar workflow on `pull_request` | Medium |
| CFG-18 | Missing top-level `permissions:` in most workflows | Low |
| CFG-19 | Extra Maven repo (Shibboleth) over HTTPS without content restriction; no dependency verification/lock | Low |
| CFG-20 | Dependabot noted as sound; Spring Boot / library versions current | Info |

## Detailed findings

### CFG-01 – Default admin credentials enabled — High
- **Evidence:** `connector-distribution/src/main/resources/config/application.properties:417-418` (comment "default password 123456"), `:427-430` (`connector.admin.username=admin`, `connector.admin.password=1234…`, `connector.admin.enabled=true`, `connector.admin.email=admin@test.com`).
- **Why it matters:** anyone who knows the public repo can log in to the backend/admin API of any deployment that did not override this, and the property is a plaintext literal, not an env var. Whether a forced change on first login exists is outside this scope (needs confirmation in auth review).
- **Fix:** make the password `${ADMIN_PASSWORD}` with no default (or generate a one-time password) and set `enabled=false` unless provided.

### CFG-02 – Hardcoded keystore passwords — High
- **Evidence:** `application.properties:166` (pmode truststore), `:239`, `:246` (evidence signature keystore/key), `:322`, `:330` (container signature), `:348` (document validation truststore), `:371`, `:373`, `:376` (backend endpoint keystore/key/truststore) – all `1234…`. Same value in `docker/gateway/config/conf/domibus/domibus.properties:64,71,81` and in test config `connector-integrationtest/src/test/resources/application-test.properties:169,172,204`.
- **Why it matters:** a single well-known password protects signing keys used for evidences and message containers; combined with CFG-03 the private keys are fully recoverable.
- **Fix:** replace with `${…}` env/secret placeholders for every `*.password` and document that the sample values must not be used.

### CFG-03 – Sample private keys in repo and wired as defaults — High
- **Evidence:** tracked files `connector-distribution/src/main/resources/config/keystores/{connector-keystore,backend-keystore}.jks`, `connector-infrastructure/src/main/resources/keystores/*.jks`, `docker/gateway/config/conf/domibus/keystores/{gateway,gwlink}-keystore.jks`; referenced at `application.properties:163-166,238,321,347,370-376` (alias `connector_blue`).
- **Why it matters:** the keys are public; any deployment running with the distribution keystores signs evidences/containers with a publicly known key and trusts publicly known peers. The `connector-infrastructure/src/main/resources` copies are also on the production classpath (needs confirmation whether they are packaged into the runnable jar).
- **Fix:** ship no private keys in the distribution; generate per install or require operator-supplied keystore paths; move test keystores to `src/test`.

### CFG-04 – JMS trust-all packages — High
- **Evidence:** `application.properties:69` `spring.activemq.packages.trust-all=true`.
- **Why it matters:** allows ObjectMessage deserialization of any class from the broker, so anyone able to publish to the broker (see CFG-05/06) gets a classic Java deserialization gadget path.
- **Fix:** remove and set `spring.activemq.packages.trusted` to the specific packages, or avoid ObjectMessage.

### CFG-05 – Compose publishes backing services with default credentials — High
- **Evidence:** `docker-compose.yml:6-11` MinIO 9000/9001 `admin`/`admin…`; `:26-33` ActiveMQ 8161/61616 `admin`/`admin`; `:56-64` gateway MySQL root/user `gateway` on `30007`; `:104-113` connector MySQL `connector`/`connector` on `30006`; `:131-139` MariaDB `30008`; `:149-160` Postgres `30032`; `:167-175` Oracle `oracle`/`connector` on `30021`; `:91-92` gateway `30010`; `:200-201` connector `30000`. No `127.0.0.1:` bind prefix.
- **Why it matters:** ports bind to all host interfaces; on any reachable host the DBs (including root), broker and object store are accessible with guessable credentials. `expose:` is redundant next to `ports:`.
- **Fix:** bind to `127.0.0.1:`, drop unneeded `ports:`, and take credentials from `${VAR}`/an untracked env file.

### CFG-06 – ActiveMQ JMX unauthenticated and extra admin account — High
- **Evidence:** `docker-compose.yml:29-30,34-41` ports 1099/1199 published; `jmxremote.ssl=false`, `jmxremote.authenticate=false`. `docker/gateway/config/conf/domibus/internal/activemq.xml:134` `authenticationUser username="admin" password="1234…" groups="admins,users"`; `domibus.properties:695-698` `activeMQ.username=admin`/`activeMQ.password=admin`.
- **Why it matters:** unauthenticated RMI/JMX on a broker gives remote code execution to anyone who can reach the port.
- **Fix:** do not publish 1099/1199 (or enable JMX auth+TLS) and remove the hardcoded broker admin.

### CFG-07 – Domibus gateway config — Medium
- **Evidence:** `docker-compose.yml:85-86` DB user/password `gateway`/`gateway`; `domibus.properties:103` JDBC URL has `useSSL=false&allowPublicKeyRetrieval=true`; `domibus.properties:296` `domibus.passwordPolicy.defaultUser.autogeneratePassword=false` (default Domibus admin keeps its well-known default password – needs confirmation); `docker/gateway/config/conf/domibus/domains/*/…-domibus.properties` contain placeholder passwords `keystore_password`/`private_key_password` (templates, Info). Gateway image is `scm.ecodex.eu/docker/gateway:edge` (`docker-compose.yml:79`), a mutable tag from a private registry.
- **Why it matters:** gateway admin console and DB are reachable with default credentials; DB traffic is unencrypted and public-key retrieval enabled (MITM-able).
- **Fix:** autogenerate or set the Domibus admin password, enable TLS on the JDBC URL, pin the gateway image by version/digest.

### CFG-08 – Swagger UI / API docs enabled — Medium
- **Evidence:** `application.properties:121-125` `springdoc.swagger-ui.enabled=true`, `springdoc.api-docs.enabled=true` (comment itself says to disable in production).
- **Why it matters:** exposes full API schema to unauthenticated clients unless otherwise restricted (needs confirmation of security rules on `/swagger-ui`, `/v3/api-docs`).
- **Fix:** default both to `false`; enable by profile/env.

### CFG-09 – Actuator details and bind address — Medium
- **Evidence:** `application.properties:134` `management.endpoint.health.show-details=always`; `:135` exposure limited to `health,info` (good); `:22-23` `server.address=0.0.0.0`, port 30000 plain HTTP (no `server.ssl.*` configured; TLS termination assumed external – needs confirmation).
- **Why it matters:** health details disclose DB/JMS/disk component status to anonymous callers (if the endpoint is unauthenticated – needs confirmation).
- **Fix:** `show-details=when-authorized` and document TLS termination requirement.

### CFG-10 – Risky defaults — Medium
- **Evidence:** `application.properties:34` `spring.jpa.hibernate.ddl-auto=update`; `:215` `connector.message-processing.inbound-verification-mode=RELAXED` (outbound is `STRICT`, `:212`); `:100-104` multipart max 200 MB/210 MB and `server.tomcat.max-swallow-size=-1`.
- **Why it matters:** schema auto-migration under the app's DB user in production; relaxed verification weakens inbound trust checks (semantics need confirmation); large uploads + unlimited swallow aid resource exhaustion.
- **Fix:** use `validate`/migration scripts, default inbound to `STRICT`, set a finite swallow size.

### CFG-11 – CORS and SOAP logging — Low
- **Evidence:** `application.properties:116` `connector.cors.allowed-origins=http://localhost:3000`; `:368` backend link `logging-enabled=true`; `:391` push-address `http://localhost:port/…` (plain HTTP, placeholder).
- **Why it matters:** CORS is restrictive (good) but dev-specific; SOAP payload logging may write message content/PII to logs (needs confirmation).
- **Fix:** set origin via env; default `logging-enabled=false`; use HTTPS for push addresses.

### CFG-12 – Outdated MySQL driver — Medium
- **Evidence:** `gradle/libs.versions.toml:28,81` `mysql:mysql-connector-java:8.0.33` (relocated artifact, no longer published as of 8.0.31+; affected by known CVEs e.g. CVE-2023-22102 fixed in 8.2.0 – needs confirmation against current advisories).
- **Why it matters:** unmaintained coordinates, known vulnerability, no future fixes.
- **Fix:** move to `com.mysql:mysql-connector-j` (current).

### CFG-13 – Dockerfile hardening gaps — Medium
- **Evidence:** `Dockerfile:1` `gradle:9-jdk21-alpine` and `:6` `eclipse-temurin:21-jre-jammy` (tags, no digest); `:3` `COPY . .` (limited by `.dockerignore`, content not reviewed here); no `HEALTHCHECK` (file ends `:35`); `:33` `EXPOSE 30000`; `:4` build skips tests; `:31` non-root user (good).
- **Why it matters:** non-reproducible images; orchestrators cannot detect an unhealthy container; actuator `health` is available (`application.properties:135`) so a check is cheap. The sample keystores/config are baked into the image (`:26`, ties to CFG-02/03).
- **Fix:** pin base images by digest, add `HEALTHCHECK` on `/actuator/health`, and read-only rootfs/dropped caps in compose.

### CFG-14 – Mutable/unhardened images in compose — Low
- **Evidence:** `docker-compose.yml:54,102` `mysql:8`; `:146` `postgres:17`; `:164` `gvenzl/oracle-free:23-slim`; `:79` `…gateway:edge`; `:190` `connector-ng:activemq`. No `user:`, `read_only`, `cap_drop`, `security_opt`; no `restart`/resource limits on several services; healthchecks absent for MariaDB/Postgres/gateway/connector (`:127-182`).
- **Why it matters:** unpredictable updates and containers running with default privileges.
- **Fix:** pin versions/digests and add `cap_drop: [ALL]`, `no-new-privileges`.

### CFG-15 – Workflow action pinning — Medium
- **Evidence:** unpinned tag refs: `ci.yaml:56,120` `actions/upload-artifact@v7`; `dependency-submission.yaml:16,18,23` `actions/checkout@v7.0.1`, `setup-java@v6.0.1`, `gradle/actions/dependency-submission@v5`. Mutable-branch reusable workflows: `ci.yaml:101` (`…/install-graphviz@main`), `commitlint.yaml:12`, `sast.yaml:10,15`, `publish-release.yaml:36`, `publish-snapshot.yaml:17` – all `e-CODEX/workflows@main`. Good: `ci.yaml:25,28,49,114`, `linting.yaml:11`, `publish-release.yaml:16-25` and `publish-snapshot.yaml:10` are SHA-pinned.
- **Why it matters:** a compromised tag or `main` of the external repo runs with this repo's token/secrets, including release and docker-push jobs.
- **Fix:** pin all actions and reusable workflows to full commit SHAs (Dependabot can bump them).

### CFG-16 – Over-broad token in CI — Medium
- **Evidence:** `ci.yaml:14-17` `unit-test` has `contents: write` and `pull-requests: write`, triggered by `push`, `pull_request`, `merge_group` (`:3-6`), and runs build code plus a third-party action (`:49`, SHA-pinned).
- **Why it matters:** build scripts executed from a PR branch could use a write token (for same-repo PRs; fork PRs receive a read-only token). Write is presumably for the badge commit (needs confirmation).
- **Fix:** run tests with `contents: read`; do badge/PR comments in a separate job restricted to `develop`.

### CFG-17 – `secrets: inherit` — Medium
- **Evidence:** `sast.yaml:14` `secrets: inherit` into `e-CODEX/workflows/.github/workflows/sonar-java.yaml@main` (unpinned, CFG-15), triggered on `pull_request` (`:3-4`).
- **Why it matters:** all repo secrets are passed to an externally controlled, mutable workflow; no `permissions:` block narrows the token.
- **Fix:** pass only `SONAR_TOKEN` explicitly and pin the workflow to a SHA.

### CFG-18 – Missing default permissions — Low
- **Evidence:** no top-level `permissions:` in `ci.yaml`, `commitlint.yaml`, `linting.yaml`, `sast.yaml`, `publish-release.yaml`, `publish-snapshot.yaml` (only `dependency-submission.yaml:8-9`, with `contents: write`). `publish-release.yaml:1-12` is `workflow_dispatch` with an `environment` input (good: single `production` option) but no visible approval gate in this repo (needs confirmation in environment settings).
- **Why it matters:** token scope falls back to repo defaults (may be read-write).
- **Fix:** add `permissions: contents: read` at workflow level and escalate per job.
- **Positive:** no `pull_request_target` or `workflow_run` triggers; no `${{ github.event.* }}` interpolated into `run:` steps.

### CFG-19 – Repository and build integrity — Low
- **Evidence:** `build.gradle.kts:15-18` extra repo `https://build.shibboleth.net/maven/releases/` applied to all projects without `content {}` filtering; `:113-118` publishing credentials from `MAVEN_REPO_USERNAME/PASSWORD` env (good); no `gradle/verification-metadata.xml` or dependency locks in tree; `gradle-wrapper.properties` has no `distributionSha256Sum`.
- **Why it matters:** dependency-confusion / tampered artifact risk is not mitigated by checksum verification.
- **Fix:** restrict the Shibboleth repo with `content { includeGroup… }` and enable Gradle dependency verification + wrapper checksum.

### CFG-20 – Positives and dependency posture — Info
- Env-var placeholders for DB/JMS/S3/JWT secrets (`application.properties:28-32,63-67,423`); actuator exposure limited to `health,info` (`:135`); container runs as non-root (`Dockerfile:31`); `*.env` ignored; SBOM generated (`ci.yaml:110-117`); Dependabot configured for gradle, docker and actions (`.github/dependabot.yml`). Library versions in `gradle/libs.versions.toml` (Spring Boot 4.1.1, CXF 4.1.0, jjwt 0.13.0, postgresql 42.7.13, etc.) were not checked against a live CVE database from this environment (needs confirmation, e.g. via the SBOM and `dependency-check`). `wss4j`/`xmlsec` are deliberately held at 3.0.5 (`libs.versions.toml:8-9`), and `micrometer` at `1.7.1` (`:39`) looks inconsistent with the Boot 4 line (needs confirmation).
- Note: `JWT_SECRET_KEY` strength/length is not validated in config (`:423`); the comment at `:416` relies on operators.
