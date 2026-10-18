# Security audit – consolidated summary

## Scope and method

- Read-only static review of the connector repository at commit `f8619535` (branch `zugazagoitia/orcaducho-security-model-audit` merge state), performed 2026-10-18. No code was run, no source/config/build/test file was changed, and no live deployment was tested.
- Four area reports: [01 Authentication](01-authentication.md), [02 Authorization / web / data](02-authorization-web-data.md), [03 SOAP / crypto / keystores](03-soap-crypto-keystores.md), [04 Config / deployment / supply chain](04-config-deployment-supply-chain.md). This file only consolidates them; the detailed evidence (file:line), impact and one-line fix for each finding are in the linked sections.
- IDs keep their original prefixes (AUTH, F, SC, CFG). Some issues are reported from two angles (e.g. default admin: F-06 / CFG-01; committed keys: SC-01 / CFG-02 / CFG-03; Swagger: F-07 / CFG-08; actuator: F-08 / CFG-09; default store passwords: F-16 / CFG-02); each ID is listed once below.

## Security model (summary)

The connector is a Spring Boot application with four trust boundaries. (1) **REST clients** (backend UIs/admins) use `/api/v1/**`: stateless JWT bearer auth (HS-signed access token, 30-day opaque refresh token, BCrypt-12 passwords), but a hand-written `permitAll` list leaves message submission, attachment upload and evidence download open, and everything under `/api/v1/admin/**` is "any authenticated user" except users and two POST endpoints. (2) **SOAP backends** call `/services/backend` (CXF, WS-SecurityPolicy asymmetric binding, X.509 sign+encrypt body); the caller is identified only by the certificate subject DN mapped to a configured link partner, over plain HTTP at the transport layer. (3) The **gateway (Domibus)** is reached over JMS/ActiveMQ with credentials from the environment and a trust-all deserialization setting. (4) **Storage** is a SQL database (JPA, parameterised queries) plus S3/MinIO for attachments, with secrets for DB/JMS/S3/JWT read from environment placeholders. **Admins** are DB users with an ADMIN role; a default `admin/123456` account and sample JKS keystores with `12345` passwords ship in the distribution config and are assumed (needs confirmation) to be overridden by operators; nothing in the repo enforces that.

## Spot-check of the highest-severity findings

| Finding | Check against code | Result |
|---|---|---|
| F-01 (Critical) | `WebSecurityConfiguration.java:82-84` lists `/api/v1/messages/outbound` and `/evidence-trigger` under `permitAll`; `ConnectorMessageController.java:76-77` and `:95-96` call `getBackendClient("cn=alice")` under a `// TODO current cn is fake` comment. | **Confirmed**, severity unchanged (Critical). |
| F-04 (High) | `WebSecurityConfiguration.java:70-80`: only `/admin/users/**` and two POST routes need ADMIN; `/api/v1/admin/**` is otherwise `.authenticated()`. | **Confirmed**, severity unchanged (High); which non-ADMIN roles exist stays needs confirmation. |
| F-06 / CFG-01 (High) | `connector-distribution/.../config/application.properties:428-431`: `connector.admin.username=admin`, `password=123456`, `enabled=true`. | **Confirmed**, severity unchanged (High). |

No severity was adjusted. (AUTH-01 was also read: the secret comes from `${JWT_SECRET_KEY}` with no default, so the risk depends on operator key choice; High is kept as reported but is the weakest of the High group.)

## Top 5 risks

1. **F-01 – Anonymous message/evidence submission as `cn=alice`** (Critical): no credential needed, and the ownership model is bypassed on the REST path.
2. **F-04 – Admin API open to any authenticated user** (High): vertical privilege escalation, including truststore modification via PATCH.
3. **F-06 / CFG-01 – Default admin `admin/123456` enabled** (High): public repo knowledge gives full admin on any deployment that did not override it; chains with F-04.
4. **SC-01 / CFG-03 / CFG-02 – Committed private keys with `12345` passwords wired as defaults** (High): publicly known keys sign/decrypt message traffic if left in use, defeating WS-Security.
5. **F-02 / F-03 – Unauthenticated attachment upload and evidence download** (High): anonymous writes to S3/DB (200 MB each) and reads of delivery evidence; resource exhaustion and data exposure.

## All findings (sorted by severity)

| ID | Severity | Area | Title |
|----|----------|------|-------|
| [F-01](02-authorization-web-data.md#f-01-rest-message-submission-unauthenticated-fixed-identity-cnalice--critical) | Critical | Authorization / web / data | REST outbound/evidence submission is unauthenticated and impersonates hard-coded link partner `cn=alice` |
| [AUTH-01](01-authentication.md#auth-01--jwt-secret-strength-not-enforced-high) | High | Authentication | No JWT secret strength/length validation; secret is raw string bytes |
| [AUTH-02](01-authentication.md#auth-02--no-brute-force-protection-on-login-high) | High | Authentication | No login throttling / lockout (online brute force) |
| [AUTH-05](01-authentication.md#auth-05--refresh-ignores-account-state-high) | High | Authentication | Refresh endpoint does not re-check user `enabled`; disabled users keep refreshing |
| [F-02](02-authorization-web-data.md#f-02-unauthenticated-attachment-upload--high) | High | Authorization / web / data | Unauthenticated attachment upload to S3 (200 MB files) |
| [F-03](02-authorization-web-data.md#f-03-unauthenticated-evidence-download--high) | High | Authorization / web / data | Unauthenticated evidence download by UUID |
| [F-04](02-authorization-web-data.md#f-04-admin-api-reachable-by-any-authenticated-user--high) | High | Authorization / web / data | `/api/v1/admin/**` is "any authenticated user" except users/POST create endpoints — vertical privilege escalation (incl. truststore PATCH) |
| [F-06](02-authorization-web-data.md#f-06-default-admin-credentials--high) | High | Authorization / web / data | Default admin credentials `admin/123456` shipped in distribution config |
| [SC-01](03-soap-crypto-keystores.md#sc-01-committed-private-keys-and-default-password--high) | High | SOAP / crypto / keystores | Private keys and default store/key password committed to the repo and shipped in the distribution |
| [CFG-01](04-config-deployment-supply-chain.md#cfg-01--default-admin-credentials-enabled--high) | High | Config / deployment / supply chain | Default admin account `admin`/`123456` enabled in shipped config |
| [CFG-02](04-config-deployment-supply-chain.md#cfg-02--hardcoded-keystore-passwords--high) | High | Config / deployment / supply chain | Hardcoded keystore/key/truststore passwords (`12345`) in shipped config |
| [CFG-03](04-config-deployment-supply-chain.md#cfg-03--sample-private-keys-in-repo-and-wired-as-defaults--high) | High | Config / deployment / supply chain | Sample private keys committed and used by default config |
| [CFG-04](04-config-deployment-supply-chain.md#cfg-04--jms-trust-all-packages--high) | High | Config / deployment / supply chain | `spring.activemq.packages.trust-all=true` (JMS deserialization) |
| [CFG-05](04-config-deployment-supply-chain.md#cfg-05--compose-publishes-backing-services-with-default-credentials--high) | High | Config / deployment / supply chain | Docker-compose publishes DBs/broker/MinIO/gateway to host with default creds |
| [CFG-06](04-config-deployment-supply-chain.md#cfg-06--activemq-jmx-unauthenticated-and-extra-admin-account--high) | High | Config / deployment / supply chain | ActiveMQ JMX exposed without auth or TLS; extra hardcoded broker admin |
| [AUTH-03](01-authentication.md#auth-03--no-refresh-token-reuse-detection-medium) | Medium | Authentication | Refresh-token reuse not detected; revoked-token replay is a silent failure only |
| [AUTH-04](01-authentication.md#auth-04--non-atomic-rotation-medium) | Medium | Authentication | Refresh rotation is non-atomic (race allows parallel valid tokens) |
| [AUTH-06](01-authentication.md#auth-06--access-token-not-revocable-medium) | Medium | Authentication | Logout/revocation does not invalidate the access token |
| [AUTH-07](01-authentication.md#auth-07--user-enumeration-medium) | Medium | Authentication | User enumeration via distinct disabled-account response and timing |
| [F-05](02-authorization-web-data.md#f-05-no-object-level-ownership-on-admin-reads--medium) | Medium | Authorization / web / data | No object-level (IDOR) ownership on admin message/attachment/transport reads |
| [F-07](02-authorization-web-data.md#f-07-swaggeropenapi-exposed--medium) | Medium | Authorization / web / data | Swagger UI / OpenAPI docs open and enabled by default |
| [F-08](02-authorization-web-data.md#f-08-actuator--medium) | Medium | Authorization / web / data | Health details shown to anonymous callers (`show-details=always`) and `/actuator/**` fully permitAll |
| [F-09](02-authorization-web-data.md#f-09-information-leakage-in-errors--medium) | Medium | Authorization / web / data | Exception messages returned verbatim to clients; ownership exception unmapped |
| [F-10](02-authorization-web-data.md#f-10-header-injection--content-type-trust--medium) | Medium | Authorization / web / data | Content-Disposition built from unsanitised attachment name; stored content-type served as-is |
| [F-16](02-authorization-web-data.md#f-16-default-store-passwords--medium) | Medium | Authorization / web / data | Default keystore/truststore passwords `1234…` in distribution config |
| [SC-02](03-soap-crypto-keystores.md#sc-02-shared-identities-across-stores--medium) | Medium | SOAP / crypto / keystores | Same demo keys shared by connector and backend stores (no separation of identities) |
| [SC-03](03-soap-crypto-keystores.md#sc-03-authorisation-by-subject-dn-only--medium) | Medium | SOAP / crypto / keystores | Backend authorisation depends on subject DN only, with a TODO and no issuer or certificate pinning |
| [SC-04](03-soap-crypto-keystores.md#sc-04-no-replay-protection-configured--medium) | Medium | SOAP / crypto / keystores | WS policy lacks replay protection (no nonce, no timestamp lifetime, no expiry enforcement config) |
| [SC-05](03-soap-crypto-keystores.md#sc-05-legacy-algorithm-suite--medium) | Medium | SOAP / crypto / keystores | Weak/legacy algorithm suite `Basic256` (SHA-1, RSA-OAEP/1.5 legacy) |
| [SC-06](03-soap-crypto-keystores.md#sc-06-no-tls-on-the-backend-endpoint--medium) | Medium | SOAP / crypto / keystores | Plain HTTP for the backend endpoint; no TLS configured; credentials-free transport |
| [SC-07](03-soap-crypto-keystores.md#sc-07-jre-cacerts-loaded-into-ws-truststore--medium) | Medium | SOAP / crypto / keystores | Truststore loads JRE `cacerts` for WS-Security (`load.cacerts=true`) |
| [SC-08](03-soap-crypto-keystores.md#sc-08-plaintext-secrets-in-config--medium) | Medium | SOAP / crypto / keystores | Passwords in plaintext properties, passed to WSS4J properties |
| [CFG-07](04-config-deployment-supply-chain.md#cfg-07--domibus-gateway-config--medium) | Medium | Config / deployment / supply chain | Domibus gateway config: default passwords, MySQL without TLS |
| [CFG-08](04-config-deployment-supply-chain.md#cfg-08--swagger-ui--api-docs-enabled--medium) | Medium | Config / deployment / supply chain | Swagger UI / OpenAPI docs enabled by default |
| [CFG-09](04-config-deployment-supply-chain.md#cfg-09--actuator-details-and-bind-address--medium) | Medium | Config / deployment / supply chain | Actuator health shows full details; server binds 0.0.0.0 |
| [CFG-10](04-config-deployment-supply-chain.md#cfg-10--risky-defaults--medium) | Medium | Config / deployment / supply chain | `ddl-auto=update` and relaxed inbound verification as defaults |
| [CFG-12](04-config-deployment-supply-chain.md#cfg-12--outdated-mysql-driver--medium) | Medium | Config / deployment / supply chain | Outdated MySQL JDBC driver `mysql-connector-java 8.0.33` |
| [CFG-13](04-config-deployment-supply-chain.md#cfg-13--dockerfile-hardening-gaps--medium) | Medium | Config / deployment / supply chain | Dockerfile: unpinned base images, no HEALTHCHECK, JRE image with full build context copy |
| [CFG-15](04-config-deployment-supply-chain.md#cfg-15--workflow-action-pinning--medium) | Medium | Config / deployment / supply chain | GitHub Actions: unpinned third-party actions and `@main` reusable workflows |
| [CFG-16](04-config-deployment-supply-chain.md#cfg-16--over-broad-token-in-ci--medium) | Medium | Config / deployment / supply chain | CI `unit-test` job grants `contents: write` on pull_request/push |
| [CFG-17](04-config-deployment-supply-chain.md#cfg-17--secrets-inherit--medium) | Medium | Config / deployment / supply chain | `secrets: inherit` into reusable Sonar workflow on `pull_request` |
| [AUTH-08](01-authentication.md#auth-08--issueraudience-not-used-low) | Low | Authentication | `iss`/`aud` configured/absent but never set or validated |
| [AUTH-09](01-authentication.md#auth-09--clock-handling--skew-low) | Low | Authentication | Refresh service mixes `Instant.now()` with injected `Clock`; no clock skew allowance |
| [AUTH-10](01-authentication.md#auth-10--log-injection--credential-adjacent-logging-low) | Low | Authentication | Failed-login log lines record attacker-supplied username unsanitised (log injection) |
| [AUTH-13](01-authentication.md#auth-13--identifier-disclosure-in-revoke-error-low) | Low | Authentication | Logout refresh-token ownership error message leaks user identifier |
| [F-11](02-authorization-web-data.md#f-11-s3-key-naming-and-access-control--low) | Low | Authorization / web / data | S3 object keys are user-controlled filename suffix; no bucket/key access scoping or SSE config |
| [F-12](02-authorization-web-data.md#f-12-silent-null-on-s3-failure--low) | Low | Authorization / web / data | S3 download swallows errors and returns `null` |
| [F-13](02-authorization-web-data.md#f-13-password-policy--low) | Low | Authorization / web / data | Weak password policy (min 6) |
| [F-14](02-authorization-web-data.md#f-14-headers-tls-throttling--low-needs-confirmation) | Low | Authorization / web / data | No security headers / TLS configured explicitly; no rate limiting *(needs confirmation)* |
| [F-15](02-authorization-web-data.md#f-15-unbounded-page-size--low-needs-confirmation) | Low | Authorization / web / data | Unbounded `size` page parameter on admin lists *(needs confirmation)* |
| [F-17](02-authorization-web-data.md#f-17-temp-file-suffix-from-client--low) | Low | Authorization / web / data | Temp file created with client-supplied filename as suffix |
| [SC-09](03-soap-crypto-keystores.md#sc-09-no-op-inbound-callback-handler--low) | Low | SOAP / crypto / keystores | Inbound callback handler is a no-op; password handling is implicit |
| [SC-10](03-soap-crypto-keystores.md#sc-10-entity-expansion-limit--low) | Low | SOAP / crypto / keystores | Entity-expansion limit set to `0` in parser util; limit semantics need confirmation |
| [SC-11](03-soap-crypto-keystores.md#sc-11-soap-payload-logging--low) | Low | SOAP / crypto / keystores | `LoggingFeature` enabled by default logs full SOAP payloads |
| [SC-12](03-soap-crypto-keystores.md#sc-12-weak-defaults-in-gateway-and-docker-config--low) | Low | SOAP / crypto / keystores | Gateway/test config uses weak defaults (store password, ActiveMQ admin, `useSSL=false`, JMX SSL off, trust-all packages) |
| [CFG-11](04-config-deployment-supply-chain.md#cfg-11--cors-and-soap-logging--low) | Low | Config / deployment / supply chain | CORS fixed to `http://localhost:3000`; backend SOAP logging enabled |
| [CFG-14](04-config-deployment-supply-chain.md#cfg-14--mutableunhardened-images-in-compose--low) | Low | Config / deployment / supply chain | Floating `latest`-style/mutable image tags in compose |
| [CFG-18](04-config-deployment-supply-chain.md#cfg-18--missing-default-permissions--low) | Low | Config / deployment / supply chain | Missing top-level `permissions:` in most workflows |
| [CFG-19](04-config-deployment-supply-chain.md#cfg-19--repository-and-build-integrity--low) | Low | Config / deployment / supply chain | Extra Maven repo (Shibboleth) over HTTPS without content restriction; no dependency verification/lock |
| [AUTH-11](01-authentication.md#auth-11--isexpired-semantics-info) | Info | Authentication | Expired-token helper `isExpired` returns false for invalid tokens (fail-open naming) |
| [AUTH-12](01-authentication.md#auth-12--password-hashing-parameters-info) | Info | Authentication | Password hashing: BCrypt cost 12, 72-byte limit, no upgrade path |
| [F-18](02-authorization-web-data.md#f-18-ownership-check-quality--info) | Info | Authorization / web / data | Ownership checks are string case-insensitive name comparison; not applied on REST path |
| [F-19](02-authorization-web-data.md#f-19-sqljpql-injection--info-no-finding) | Info | Authorization / web / data | SQL/JPQL injection: no issue found |
| [F-20](02-authorization-web-data.md#f-20-logging--info) | Info | Authorization / web / data | Sensitive data in logs: no secrets found; DN and exception details logged |
| [SC-13](03-soap-crypto-keystores.md#sc-13-jks-keystore-format--info) | Info | SOAP / crypto / keystores | JKS keystore format used by default |
| [SC-14](03-soap-crypto-keystores.md#sc-14-hostname-verification--outbound-tls--info-needs-confirmation) | Info | SOAP / crypto / keystores | No hostname verification setup / TLS client config for outbound push *(needs confirmation)* |
| [CFG-20](04-config-deployment-supply-chain.md#cfg-20--positives-and-dependency-posture--info) | Info | Config / deployment / supply chain | Dependabot noted as sound; Spring Boot / library versions current |

## Items marked "needs confirmation"

- [F-04](02-authorization-web-data.md#f-04-admin-api-reachable-by-any-authenticated-user--high): which non-ADMIN roles exist and are meant to use admin reads.
- [F-11](02-authorization-web-data.md#f-11-s3-key-naming-and-access-control--low): bucket SSE / block-public-access (infrastructure).
- [F-12](02-authorization-web-data.md#f-12-silent-null-on-s3-failure--low): caller behaviour on `null` download (NPE vs 204).
- [F-14](02-authorization-web-data.md#f-14-headers-tls-throttling--low-needs-confirmation): security headers, TLS termination, rate limiting (also login throttling in the endpoint matrix).
- [F-15](02-authorization-web-data.md#f-15-unbounded-page-size--low-needs-confirmation): server-side cap on `size`.
- [SC-04](03-soap-crypto-keystores.md#sc-04-no-replay-protection-configured--medium): replay behaviour relies on library defaults.
- [SC-10](03-soap-crypto-keystores.md#sc-10-entity-expansion-limit--low): meaning of the `0` limit in the JAXP implementation.
- [SC-14](03-soap-crypto-keystores.md#sc-14-hostname-verification--outbound-tls--info-needs-confirmation): hostname verification / outbound TLS client config.
- [CFG-01..CFG-03 trust assumption](04-config-deployment-supply-chain.md#security-model): shipped sample credentials/keys are assumed to be overridden by operators.
- [CFG-08](04-config-deployment-supply-chain.md#cfg-08--swagger-ui--api-docs-enabled--medium), [CFG-09](04-config-deployment-supply-chain.md#cfg-09--actuator-details-and-bind-address--medium): whether Swagger/actuator are reachable unauthenticated (code in report 02 indicates `permitAll`).
- [CFG-11](04-config-deployment-supply-chain.md#cfg-11--cors-and-soap-logging--low): whether SOAP logging captures PII in practice.
- [CFG-12](04-config-deployment-supply-chain.md#cfg-12--outdated-mysql-driver--medium), [CFG-16](04-config-deployment-supply-chain.md#cfg-16--over-broad-token-in-ci--medium), [CFG-18](04-config-deployment-supply-chain.md#cfg-18--missing-default-permissions--low): exact exposure depends on external/org-level settings (see detailed text).
- [AUTH-02](01-authentication.md#auth-02--no-brute-force-protection-on-login-high), [AUTH-04](01-authentication.md#auth-04--non-atomic-rotation-medium), [AUTH-05](01-authentication.md#auth-05--refresh-ignores-account-state-high), [AUTH-07](01-authentication.md#auth-07--user-enumeration-medium), [AUTH-12](01-authentication.md#auth-12--password-hashing-parameters-info): contain caveats in their detailed text (e.g. throttling possibly at a proxy, admin-path token revocation).
