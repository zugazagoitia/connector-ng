# Security audit 03 — SOAP backend, WS-Security, keystores and crypto

Scope: read-only review of the SOAP backend and its crypto material. Only issues present in the current code are listed. Secrets are redacted (the committed store password is shown as `12***`, 5 chars).

Paths below are relative to the repo root. `INF` = `connector-infrastructure/src/main/java/eu/ecodex/connector/infrastructure`, `APP` = `connector-distribution/src/main/resources/config/application.properties`.

## Security model

- **Inbound backend SOAP endpoint.** `BackendSoapWebServiceConfig` publishes one Apache CXF JAX-WS endpoint per `connector.link.backend[*]` entry (`INF/inbound/web/soap/BackendSoapWebServiceConfig.java:44-48`, `BackendWebServiceFactory.java:60-102`). The WSDL is `wsdl/v1/DomibusConnectorBackendWebService.wsdl`. The default address is `/backend` (`APP:366`), served over the Spring Boot HTTP listener on `server.port=30000` (`APP:22`). The properties file configures no `server.ssl.*` setting.
- **WS-Security policy** (`connector-soap-api/src/main/resources/policy/backend.policy.xml`). It is a WS-SecurityPolicy 1.2 `AsymmetricBinding`. It uses X.509 V3 tokens (initiator token included in each message, recipient token never included), the `Basic256` algorithm suite, `Strict` layout, a timestamp, and signed and encrypted body only. `Wss11` options include `RequireSignatureConfirmation`. The policy is loaded with `ConnectorWsPolicyLoader` (`INF/outbound/soap/ConnectorWsPolicyLoader.java:46-73`) and attached as a `WSPolicyFeature`.
- **Crypto configuration.** WSS4J `Merlin` crypto is built from keystore and truststore properties (`INF/outbound/soap/ConnectorMerlinPropertiesFactory.java:45-110`). The signing keystore and the encryption truststore are separate. The inbound side sets `ENCRYPT_USERNAME=useReqSigCert`, so responses are encrypted to the certificate the client signed with (`BackendWebServiceFactory.java:80`).
- **Authentication and authorisation of backends.** The WSS4J-validated signing certificate yields a `Principal`. `getBackendClientName()` (`INF/inbound/web/soap/controller/ConnectorBackendWebServiceController.java:408-436`) takes the principal name (certificate subject DN) and maps it, case-insensitively, to a configured link partner (`ConnectorBackendClientVerifier`, `connector-application/.../ConnectorFindLinkPartnerService.java:34-45`). All operations use that link-partner name as the caller identity. There are no roles.
- **Outbound push to backends.** `ConnectorBackendDeliveryServiceClient.createClient` (`INF/outbound/soap/ConnectorBackendDeliveryServiceClient.java:62-122`) builds a CXF client using the same policy and Merlin properties. The push address comes from the link partner (`pushAddress()`).
- **Gateway (Domibus) link.** The connector talks to Domibus over JMS (queues `connector.queues.gateway-*`, `APP:182-184`) via ActiveMQ credentials from environment variables (`APP:64-66`). No SOAP/TLS client to Domibus exists in the connector code reviewed. The Domibus test configuration lives under `docker/gateway/`.
- **XML parsing.** CXF/WSS4J parses SOAP. The PMode parser uses `SecureXmlParserUtil.parseSecurely` (`INF/util/SecureXmlParserUtil.java:48-87`).
- **Keystores.** JKS keystores and truststores are committed in two places with identical content: `connector-infrastructure/src/main/resources/keystores/` and `connector-distribution/src/main/resources/config/keystores/` (identical MD5 for each of the 4 files). All have store type JKS, RSA-2048, SHA256withRSA, validity 2025-05-27 to 2125-05-27. The aliases are `alice`, `bob`, `connector_blue`, `connector_red`. The distribution `application.properties` references them with store, key and truststore password `12***` (`APP:165-166,239,322,348,370-376`). The domain model `KeystoreType` supports only `JKS` and `PKCS12` (`connector-domain/.../model/security/KeystoreType.java:33-34`).

## Findings table

| ID | Title | Severity |
|----|-------|----------|
| SC-01 | Private keys and default store/key password committed to the repo and shipped in the distribution | High |
| SC-02 | Same demo keys shared by connector and backend stores (no separation of identities) | Medium |
| SC-03 | Backend authorisation depends on subject DN only, with a TODO and no issuer or certificate pinning | Medium |
| SC-04 | WS policy lacks replay protection (no nonce, no timestamp lifetime, no expiry enforcement config) | Medium |
| SC-05 | Weak/legacy algorithm suite `Basic256` (SHA-1, RSA-OAEP/1.5 legacy) | Medium |
| SC-06 | Plain HTTP for the backend endpoint; no TLS configured; credentials-free transport | Medium |
| SC-07 | Truststore loads JRE `cacerts` for WS-Security (`load.cacerts=true`) | Medium |
| SC-08 | Passwords in plaintext properties, passed to WSS4J properties | Medium |
| SC-09 | Inbound callback handler is a no-op; password handling is implicit | Low |
| SC-10 | Entity-expansion limit set to `0` in parser util; limit semantics need confirmation | Low |
| SC-11 | `LoggingFeature` enabled by default logs full SOAP payloads | Low |
| SC-12 | Gateway/test config uses weak defaults (store password, ActiveMQ admin, `useSSL=false`, JMX SSL off, trust-all packages) | Low |
| SC-13 | JKS keystore format used by default | Info |
| SC-14 | No hostname verification setup / TLS client config for outbound push | Info (needs confirmation) |

## Detailed findings

### SC-01 Committed private keys and default password — High
- **Evidence:** `connector-infrastructure/src/main/resources/keystores/backend-keystore.jks` and `connector-keystore.jks` hold `PrivateKeyEntry` for `alice`, `bob`, `connector_blue`, `connector_red` (opened with the default password `12***`). Identical copies sit in `connector-distribution/src/main/resources/config/keystores/*.jks`. Properties: `APP:239,322,371,373,376`. Gateway side: `docker/gateway/config/conf/domibus/keystores/gateway-keystore.jks` with `docker/gateway/config/conf/domibus/domibus.properties:64,71,81`.
- **Why it matters:** Anyone with repo or distribution access can impersonate any backend (`alice`/`bob`) or the connector. The password is the trivial `12***`. Anyone who deploys the distribution without replacing the stores trusts the public demo keys, and the distribution ships them in the artifact.
- **Fix:** Remove the private keys from the repo and distribution (generate at deploy time) and take store passwords from environment or secrets.

### SC-02 Shared identities across stores — Medium
- **Evidence:** `backend-keystore.jks` also contains the connector's own keys (`connector_blue`, `connector_red`) and the backend client keys (`alice`, `bob`) in one store. `backend-truststore.jks` and `connector-truststore.jks` are separate. `APP:370-376`.
- **Why it matters:** The one keystore holds both client and server private keys. Compromise of one key exposes all.
- **Fix:** One keystore per identity, containing only its own private key.

### SC-03 Authorisation by subject DN only — Medium
- **Evidence:** `INF/inbound/web/ConnectorBackendClientVerifier.java:50` (`TODO to be completed, certificateDn should be retrieved from the certificate`) and `:52`. DN is lowercased before matching (`ConnectorFindLinkPartnerService.java:36-37`). Config: `APP:393`-area `certificate-dn=cn=alice`.
- **Why it matters:** The identity is a name string, not a pinned certificate or issuer. Any certificate in the truststore with a matching CN is accepted, and case-insensitive matching widens the match. The truststore trust decision (WSS4J) and the mapping are loosely coupled. Failed lookups surface as `IllegalStateException` / `ConnectorLinkPartnerException`.
- **Fix:** Match on full DN plus certificate thumbprint/serial or issuer stored per link partner.

### SC-04 No replay protection configured — Medium
- **Evidence:** `backend.policy.xml:52` has `<sp:IncludeTimestamp/>` only; there is no `UsernameToken` nonce, and the code sets no WSS4J `TIMESTAMP_TTL`, `ENABLE_NONCE_CACHE` or `ENABLE_TIMESTAMP_CACHE` property (`BackendWebServiceFactory.java:71-89`). `RequireSignatureConfirmation` is at line 62.
- **Why it matters:** WSS4J defaults apply (timestamp TTL 300 s, replay caches by default in current CXF versions). Behaviour relies on library defaults and needs confirmation. Because the body is signed and the timestamp is signed, a replay is possible only within the TTL if the cache is not active.
- **Fix:** Set explicit `security.timestamp.timeToLive`, `security.enable.timestamp.cache` and `security.enable.nonce.cache`.

### SC-05 Legacy algorithm suite — Medium
- **Evidence:** `backend.policy.xml:43` `<sp:Basic256/>` with comment `TODO: define more secure algorithm !Backward compatible?`.
- **Why it matters:** `Basic256` mandates SHA-1 digests and signature, RSA-OAEP (SHA-1) key transport and AES-256-CBC. SHA-1 is deprecated, and AES-CBC for body encryption has no authenticated mode.
- **Fix:** Move to `Basic256Sha256` or a custom suite with SHA-256+ and AES-GCM (coordinate with backend clients).

### SC-06 No TLS on the backend endpoint — Medium
- **Evidence:** `APP:22` (`server.port=30000`); no `server.ssl.*` in `APP`; `APP:367` address `/backend`; push address example is `http://localhost:port/...` (`APP:~393`).
- **Why it matters:** Message-level security protects only the body. Headers, message ids and metadata are unencrypted, and deployments can run the endpoint without transport security. Needs confirmation whether a TLS-terminating proxy is mandated in deployment docs.
- **Fix:** Enable `server.ssl.*` with client auth or document and enforce a TLS terminating proxy.

### SC-07 JRE cacerts loaded into WS truststore — Medium
- **Evidence:** `INF/outbound/soap/ConnectorMerlinPropertiesFactory.java:107` (`LOAD_CACERTS=true`).
- **Why it matters:** In addition to the configured truststore, all public CAs in the JRE become trust anchors for WS-Security signature validation, so any certificate chained to a public CA can pass chain validation (the DN mapping in SC-03 then becomes the only gate).
- **Fix:** Set `load.cacerts=false` so only the configured truststore is trusted.

### SC-08 Plaintext secrets in config — Medium
- **Evidence:** `APP:166,239,322,348,371,373,376`; the factory copies passwords into WSS4J properties (`ConnectorMerlinPropertiesFactory.java:68,70,106`) and into `KeystorePasswordCallback` (`INF/outbound/soap/ConnectorBackendDeliveryServiceClient.java:113-116`). The DB, broker and S3 credentials are already externalised through env placeholders (`APP:32,64-66`).
- **Why it matters:** Passwords sit in clear text in a shipped file and in process memory as strings.
- **Fix:** Use `${ENV}` placeholders for all store passwords like the DB password.

### SC-09 No-op inbound callback handler — Low
- **Evidence:** `INF/inbound/web/soap/DefaultWsCallbackHandler.java:30-32` (only logs at trace; set at `BackendWebServiceFactory.java:86-89`).
- **Why it matters:** It never sets passwords; the private-key password comes from Merlin properties instead. Trace logging prints callback objects. Low risk, but the intent is unclear.
- **Fix:** Remove it, or implement a handler that supplies the key password explicitly.

### SC-10 Entity expansion limit — Low
- **Evidence:** `INF/util/SecureXmlParserUtil.java:49-73`. DOCTYPE is disallowed (line 54), external entities and DTD loading disabled (55-58), XInclude off (63), `entityExpansionLimit` is `"0"` (71), but `XMLConstants.FEATURE_SECURE_PROCESSING` and `ACCESS_EXTERNAL_DTD/SCHEMA` are not set.
- **Why it matters:** Because DOCTYPE is rejected, XXE and billion-laughs are blocked in practice. The `"0"` limit means "no limit" in some JAXP implementations (needs confirmation), though it is moot given the DOCTYPE ban. The only caller is the PMode parser (`ConnectorDomProcessingModeParser.java:143`). SOAP parsing relies on CXF/WoodStox defaults (no explicit hardening in the connector code).
- **Fix:** Set `FEATURE_SECURE_PROCESSING` true and a sane non-zero limit.

### SC-11 SOAP payload logging — Low
- **Evidence:** `APP:368` `logging-enabled=true`; `BackendWebServiceFactory.java:91-93`; `ConnectorBackendDeliveryServiceClient.java:88-90`.
- **Why it matters:** CXF `LoggingFeature` logs message content to the log, including decrypted business payloads and attachments metadata, by default in the shipped config.
- **Fix:** Default `logging-enabled=false`.

### SC-12 Weak defaults in gateway and docker config — Low
- **Evidence:** `docker/gateway/config/conf/domibus/domibus.properties:64,71,81` (store passwords `12***`), `:698` (`activeMQ.password=ad***`), `:103` (`useSSL=false`); `docker-compose.yml:39` (`jmxremote.ssl=false`); `APP:69` `spring.activemq.packages.trust-all=true`.
- **Why it matters:** The development gateway stack and the JMS link use default credentials and unencrypted transport. `trust-all` allows deserialization of arbitrary classes from JMS object messages (needs confirmation whether object messages are used).
- **Fix:** Use restricted `trusted-packages`, enable TLS for broker and JMX, and replace default passwords outside dev.

### SC-13 JKS keystore format — Info
- **Evidence:** all four stores are type JKS (`APP:242,325,351`); `KeystoreType.java:33`.
- **Why it matters:** JKS is a legacy format. PKCS12 is supported by the enum but unused.
- **Fix:** Prefer PKCS12 for new stores.

### SC-14 Hostname verification / outbound TLS — Info (needs confirmation)
- **Evidence:** `ConnectorBackendDeliveryServiceClient.java:83-121` sets no `TLSClientParameters`, `HostnameVerifier` or `TrustManager`; no custom TLS code exists in the audited tree.
- **Why it matters:** For https push addresses, the JVM default trust and host name checks apply, and no truststore other than the JRE one is used for TLS. Nothing disables verification, so no vulnerability is found. Mutual TLS to backends cannot be configured.
- **Fix:** Expose per-link TLS client parameters (truststore, hostname verification on).
