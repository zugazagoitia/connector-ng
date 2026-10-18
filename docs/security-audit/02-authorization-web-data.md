# Security Audit 02 — Authorization, Web Surface and Data Access

Scope: `WebSecurityConfiguration`, REST controllers (`inbound/web/rest/**`), SOAP backend controller ownership checks (commit `37d9a4fa`), `ConnectorS3FileStorageProvider`, JPA repositories, application services, `connector-distribution/.../config/application.properties`. Read-only static review; no code was run. Paths are relative to `connector-infrastructure/src/main/java/eu/ecodex/connector/infrastructure/` (abbrev. `INF/`) unless stated. `PROPS` = `connector-distribution/src/main/resources/config/application.properties`.

## 1. Security model (as implemented today)

- **Authentication**: stateless JWT bearer (`ConnectorJwtAuthenticationFilter`, added before `UsernamePasswordAuthenticationFilter`, `WebSecurityConfiguration.java:106`). Invalid/missing token simply leaves the context anonymous (`ConnectorJwtAuthenticationFilter.java:74-77,107-110`); the request then lives or dies by the URL matchers. HTTP Basic disabled (`:68`). Sessions stateless (`:107`). Passwords BCrypt cost 12 (`:135`). Access token 15 min, refresh 30 d (`PROPS:424,426`).
- **CSRF disabled** (`WebSecurityConfiguration.java:67`) — acceptable for a bearer-header-only API with no cookies (no cookie auth found).
- **CORS**: enabled from properties, registered for `/**` (`:119-131`); default origin `http://localhost:3000` (`PROPS:116`); `allowCredentials` and methods/headers come from `ConnectorCorsProperties` (default values not verified — needs confirmation).
- **Authorization** is URL-matcher based only. `@EnableMethodSecurity` is on (`:49`) but **no `@PreAuthorize/@Secured/@RolesAllowed` exists anywhere** in main code. Only one role rule: `ADMIN` for `/api/v1/admin/users/**`, `POST /api/v1/admin/business-domains`, `POST /api/v1/admin/processing-modes` (`:72-77`). All other `/api/v1/admin/**` and `/api/v1/auth/me` need any authenticated user (`:79-80`). A `permitAll` list covers backend-facing REST APIs, login/refresh, SOAP, actuator and Swagger (`:81-102`). Everything else: authenticated (`:103-104`).
- **Backend-client identity**: SOAP resolves the link partner from the client certificate principal (`ConnectorBackendWebServiceController.java:408-436`) and, since `37d9a4fa`, ack/retrieve verify the transport step belongs to that link partner (`ConnectorAckMessageTransportStepService.java:~91`, `ConnectorRetrieveMessageByTransportIdService.java:~56`). The REST equivalents hard-code `cn=alice` (see F-01).
- **Errors**: `GlobalExceptionHandler` maps domain exceptions to `ErrorResponse(status, e.getMessage())`; `sendError` for 401/403 (`WebSecurityConfiguration.java:109-113`).
- **Storage**: attachments in DB metadata + S3 (single bucket, key = attachment identifier `"<uuid>-<original filename>"`).
- **Data access**: Spring Data JPA derived queries + Specifications; 5 native queries, all with bind parameters.

## 2. Endpoint → authorization matrix

Effective access derived from matcher order in `WebSecurityConfiguration.java:70-105`. "Any auth" = any valid JWT, any role.

| Endpoint | Source | Effective access | Ownership / object check | Note |
|---|---|---|---|---|
| POST `/api/v1/auth/login` | AuthApi:47 | permitAll | n/a | no throttling seen (needs confirmation) |
| POST `/api/v1/auth/refresh` | AuthApi:56 | permitAll | refresh token | |
| POST `/api/v1/auth/change-password` | AuthApi:67 | **anyRequest → authenticated** | own user | |
| POST `/api/v1/auth/logout` | AuthApi:79 | authenticated | own token | |
| GET/PATCH `/api/v1/auth/me`, POST `/auth/me/change-password` | UserApi:41-63 | authenticated | principal | |
| `/api/v1/admin/users/**` (users, roles, role-assignment) | Admin*Api | ADMIN | n/a | only ADMIN-protected group |
| POST `/api/v1/admin/business-domains` | BusinessDomainAdminApi:42 | ADMIN | n/a | |
| GET `/api/v1/admin/business-domains` | :54 | **any auth** | none | |
| POST `/api/v1/admin/processing-modes` | PModeAdminApi:49 | ADMIN | n/a | |
| GET `/admin/processing-modes`, `/{uuid}`, `/{uuid}/download` | :61-79 | **any auth** | none | downloads pmode XML |
| **PATCH `/admin/processing-modes/{uuid}/truststore`** | :88 | **any auth** (write!) | none | only POST is ADMIN-gated |
| POST `/admin/messages/outbound/test` | MessageAdminApi:55 | **any auth** (sends test message) | none | |
| GET `/admin/messages`, `/{id}`, `/{id}/transport-steps`, `/stats`, `/reports`, `/reports/export` | :67-129 | **any auth** | none (all messages, all link partners) | |
| GET `/admin/attachments`, `/{id}/download` | AttachmentAdminApi:42,59 | **any auth** | none | any attachment content |
| GET `/admin/transport-steps`, `/{id}/message` | TransportAdminApi:40,58 | **any auth** | none | |
| GET `/admin/configurations/*` (8 endpoints) | ConfigAdminApi:39-88 | **any auth** | none | exposes full connector config DTOs |
| GET `/admin/jms/queues/stats` | JmsAdminApi:32 | **any auth** | none | |
| POST `/api/v1/attachments/upload` | AttachmentApi:34 | **permitAll** | none | unauthenticated file upload to S3 |
| POST `/api/v1/messages/outbound` | MessageApi:38 | **permitAll** | fake `cn=alice` | |
| POST `/api/v1/messages/evidence-trigger` | MessageApi:48 | **permitAll** | fake `cn=alice` | |
| GET `/api/v1/evidences/{uuid}/download` | EvidenceApi:32 | **permitAll** | none | |
| GET `/api/v1/services`, `/link-partners` | LinkPartnerApi:27 | permitAll | none | |
| GET `/api/v1/processing-modes/{id}/services\|actions\|parties` | PModeApi:33-45 | permitAll | none | |
| `/services/backend` (SOAP) | CXF | permitAll at Spring layer | link-partner via cert principal; ack/retrieve ownership-checked | transport auth (mTLS) not in this scope — needs confirmation |
| `/actuator/**` | | permitAll | | exposure limited by property to `health,info` |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-resources/**` | | permitAll | | enabled by default |

## 3. Findings

| ID | Title | Severity | Evidence |
|---|---|---|---|
| F-01 | REST outbound/evidence submission is unauthenticated and impersonates hard-coded link partner `cn=alice` | **Critical** | WebSecurityConfiguration.java:82-84; ConnectorMessageController.java:76-77,95-96 |
| F-02 | Unauthenticated attachment upload to S3 (200 MB files) | **High** | WebSecurityConfiguration.java:82; ConnectorAttachmentController.java:40-79; PROPS:102-104 |
| F-03 | Unauthenticated evidence download by UUID | **High** | WebSecurityConfiguration.java:85; ConnectorEvidenceController.java:31-43 |
| F-04 | `/api/v1/admin/**` is "any authenticated user" except users/POST create endpoints — vertical privilege escalation (incl. truststore PATCH) | **High** | WebSecurityConfiguration.java:72-80 |
| F-05 | No object-level (IDOR) ownership on admin message/attachment/transport reads | **Medium** | AttachmentAdminController.java:87-100; AdminApis (matrix) |
| F-06 | Default admin credentials `admin/123456` shipped in distribution config | **High** | PROPS:428-431 |
| F-07 | Swagger UI / OpenAPI docs open and enabled by default | **Medium** | WebSecurityConfiguration.java:98-100; PROPS:123,125 |
| F-08 | Health details shown to anonymous callers (`show-details=always`) and `/actuator/**` fully permitAll | **Medium** | WebSecurityConfiguration.java:96; PROPS:134-135 |
| F-09 | Exception messages returned verbatim to clients; ownership exception unmapped | **Medium** | GlobalExceptionHandler.java:61-132; ConnectorAttachmentController.java:~69 |
| F-10 | Content-Disposition built from unsanitised attachment name; stored content-type served as-is | **Medium** | ConnectorAttachmentAdminController.java:92-97; ConnectorUploadAttachmentsService.java:53 |
| F-11 | S3 object keys are user-controlled filename suffix; no bucket/key access scoping or SSE config | **Low** | ConnectorUploadAttachmentsService.java:53-57; ConnectorS3FileStorageProvider.java:72-75,92-95 |
| F-12 | S3 download swallows errors and returns `null` | **Low** | ConnectorS3FileStorageProvider.java:80-83 |
| F-13 | Weak password policy (min 6) | **Low** | ConnectorUpdateUserPasswordRequest.java:37 |
| F-14 | No security headers / TLS configured explicitly; no rate limiting | **Low** | WebSecurityConfiguration.java:66-115 (no `.headers()`); needs confirmation |
| F-15 | Unbounded `size` page parameter on admin lists | **Low** | ConnectorMessageAdminApi.java:73-74 (needs confirmation of server-side cap) |
| F-16 | Default keystore/truststore passwords `1234…` in distribution config | **Medium** | PROPS:166,239,246,322,330,348,371-376 |
| F-17 | Temp file created with client-supplied filename as suffix | **Low** | ConnectorAttachmentController.java:~46-52 |
| F-18 | Ownership checks are string case-insensitive name comparison; not applied on REST path | **Info** | ConnectorAckMessageTransportStepService.java:~91; ConnectorRetrieveMessageByTransportIdService.java:~56 |
| F-19 | SQL/JPQL injection: no issue found | **Info** | ConnectorMessageTransportStepJpaRepository.java:35-82 |
| F-20 | Sensitive data in logs: no secrets found; DN and exception details logged | **Info** | ConnectorBackendClientVerifier.java:48; S3Provider:81 |

## 4. Detailed findings

### F-01 REST message submission unauthenticated, fixed identity `cn=alice` — Critical
- **Evidence**: `/api/v1/messages/outbound` and `/evidence-trigger` are `permitAll` (`WebSecurityConfiguration.java:82-84,102`). Controller ignores the caller and resolves the backend client with the literal DN: `ConnectorMessageController.java:76-77` and `:95-96` (`// TODO current cn is fake`); `ConnectorBackendClientVerifier.java:50` repeats the TODO.
- **Why it matters**: any network client can submit business messages and evidence triggers as link partner `alice` with no credential; also lets anyone reference other partners' `backendMessageIdentifier`s in evidence triggers. The ownership model added in `37d9a4fa` is bypassed on this path.
- **Fix**: require authentication on these routes and derive the link partner from the authenticated principal/certificate instead of a constant.

### F-02 Unauthenticated attachment upload — High
- **Evidence**: `WebSecurityConfiguration.java:82`; uploads written to temp dir then S3 (`ConnectorAttachmentController.java:40-79`, `ConnectorUploadAttachmentsService.java:53-65`); limits 200 MB/file, 210 MB/request, `max-swallow-size=-1` (`PROPS:102-107`).
- **Why**: storage/disk exhaustion and anonymous content injection into the bucket and DB.
- **Fix**: remove from `permitAll`; require authentication and add quotas.

### F-03 Unauthenticated evidence download — High
- **Evidence**: `WebSecurityConfiguration.java:85`; `ConnectorEvidenceController.java:31-43` returns evidence XML by UUID with no caller check.
- **Why**: evidence is signed message-delivery proof; UUIDs are unguessable, which reduces but does not remove risk (leak via logs/URLs). No ownership check against the caller.
- **Fix**: require authentication and verify the evidence belongs to the caller's link partner.

### F-04 Admin API reachable by any authenticated user — High
- **Evidence**: only `/api/v1/admin/users/**`, `POST …/business-domains`, `POST …/processing-modes` require ADMIN (`WebSecurityConfiguration.java:72-77`); everything else under `/api/v1/admin/**` is `.authenticated()` (`:79-80`). No method security annotations exist. `PATCH /admin/processing-modes/{uuid}/truststore` (`ConnectorProcessingModeAdminApi.java:88`) mutates trust material, `POST /admin/messages/outbound/test` sends messages, and config/JMS/message reads are exposed.
- **Why**: a low-privilege account (or any non-ADMIN role) can change the trust store and read all data.
- **Fix**: use `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` as the default and relax only intentionally.
- Note: which non-ADMIN roles exist/are intended to use admin reads — needs confirmation.

### F-05 No object-level ownership on admin reads — Medium
- **Evidence**: admin download `ConnectorAttachmentAdminController.java:87-100` looks up by identifier only; message/transport-step/attachment lists take no principal and filter nothing by link partner.
- **Why**: any (non-admin) authenticated user can read every partner's messages and attachments (compounded by F-04).
- **Fix**: restrict to ADMIN (F-04) or filter by caller's partner scope.

### F-06 Default admin credentials — High
- **Evidence**: `PROPS:428-431` (`admin` / `123****`, enabled, `admin@test.com`); comment at `:417` acknowledges. JWT secret is env-injected (`:423`) — no default.
- **Fix**: remove default password; require env-provided value and fail startup if unset.

### F-07 Swagger/OpenAPI exposed — Medium
- **Evidence**: `permitAll` `WebSecurityConfiguration.java:98-100`; enabled `PROPS:123,125` (comment says disable in production).
- **Fix**: default to disabled, or require ADMIN.

### F-08 Actuator — Medium
- **Evidence**: `/actuator/**` permitAll (`:96`); exposure `health,info` only (`PROPS:135`) but `health.show-details=always` (`:134`) discloses DB/JMS/S3 component status to anonymous callers.
- **Fix**: `show-details=when-authorized` and restrict `/actuator/**` (keep only liveness probe public).

### F-09 Information leakage in errors — Medium
- **Evidence**: handlers return `e.getMessage()` (`GlobalExceptionHandler.java:61-132`); internal I/O messages wrapped into `ConnectorInternalServerException(e.getMessage())` (`ConnectorAttachmentController.java:~69`); `ConnectorUploadAttachmentsService` wraps any exception message. 404 messages distinguish user/role/message existence (user enumeration via `ConnectorUserNotFound` vs `BadCredentials`, handlers `:84-108`). `ConnectorLinkPartnerNotOwnerException` has no handler (grep: only thrown), so REST/default path yields a generic 500 and message text at the SOAP fault level (needs confirmation).
- **Fix**: return generic messages for 5xx, map ownership exception to 403/404, keep details in logs.

### F-10 Header injection / content-type trust — Medium
- **Evidence**: `"attachment; filename=%s".formatted(attachment.name())` unquoted/unescaped (`ConnectorAttachmentAdminController.java:96-97`); `parseMediaType(attachment.contentType())` uses the uploader-supplied type (`:92-94`); evidence download similar (`ConnectorEvidenceController.java:38`).
- **Why**: filenames with quotes/semicolons alter the header; an HTML/SVG content-type served inline-able on the API origin. Servlet containers usually reject CR/LF, so splitting is unlikely.
- **Fix**: use `ContentDisposition.attachment().filename(...)` and add `X-Content-Type-Options: nosniff`.

### F-11 S3 key naming and access control — Low
- **Evidence**: key = `"<uuid>-<filename>"` where filename is `StringUtils.cleanPath(originalFilename)` (`ConnectorUploadAttachmentsService.java:53-55`, controller `:~44`); put/get use single configured bucket (`S3Provider:72-75,92-95`). UUID prefix prevents collisions/overwrites; `..` in keys is not meaningful in S3. No ACL, SSE or prefix-per-tenant set in code; relies on bucket policy and static access/secret keys from env (`PROPS:220-231`).
- **Fix**: use only the UUID as key and keep the filename in metadata; enable bucket SSE and block-public-access (infra, needs confirmation).

### F-12 Silent null on S3 failure — Low
- **Evidence**: `S3Provider:80-83` logs and returns `null`; callers (`ConnectorDownloadAttachment`) may NPE/204 (needs confirmation). Error log includes identifier (contains user filename).
- **Fix**: throw a typed not-found/storage exception.

### F-13 Password policy — Low
- `ConnectorUpdateUserPasswordRequest.java:37` minimum 6 chars, no complexity/breach check. **Fix**: raise to ≥12.

### F-14 Headers, TLS, throttling — Low (needs confirmation)
- No `headers()` customisation; Spring Security defaults add `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control`; HSTS only if request is HTTPS. No TLS config in `PROPS` (`server.port=30000`, `server.address=0.0.0.0`, `:22-23`), so TLS is presumably terminated elsewhere. No login/refresh rate limiting found. **Fix**: add a reverse-proxy or filter-level throttle on `/auth/*`.

### F-15 Unbounded page size — Low (needs confirmation)
- `ConnectorMessageAdminApi.java:73-74` `size` int with no `@Max`; `ConnectorPageRequest` in domain not reviewed for a cap. **Fix**: `@Min(1) @Max(100)`.

### F-16 Default store passwords — Medium
- `PROPS:166,239,246,322,330,348,371-376` ship `1234…` for keystores/truststores/private keys (values redacted). Private signing keys protected by trivial passwords. **Fix**: inject via env/secret store without defaults.

### F-17 Temp file suffix from client — Low
- `Files.createTempFile("upload-attachment-<uuid>-", filename)` (`ConnectorAttachmentController.java:~46-52`); path separators in suffix will most likely throw (→ F-09 500) rather than traverse; missing/null filename throws NPE via `requireNonNull` (400/500 behavior needs confirmation). **Fix**: use a fixed suffix (`.tmp`).

### F-18 Ownership check quality — Info
- `37d9a4fa` checks `existingStep.linkPartnerName().equalsIgnoreCase(command.linkPartnerName())` in ack/retrieve; correct for SOAP. Not applied to REST (F-01) or to the "list pending" queries which instead filter by `link_partner_name = :backendName` (`ConnectorMessageTransportStepJpaRepository.java:47-66`). The ack lookup `findByTransportedMessageIdentifierOrRemoteSystemIdentifier` (`:35-45`) returns a single row for an OR condition — a collision between one partner's `remote_system_identifier` and another's transported id could match the wrong step (then rejected as not-owner; availability only; needs confirmation).

### F-19 SQL/JPQL injection — Info (no finding)
All 5 native queries use `:named` binds (`ConnectorMessageTransportStepJpaRepository.java:35-82`, `…StatusJpaRepository.java:31-40`, `ConnectorUserRefreshTokenJpaRepository.java:77`); filtering uses Criteria Specifications (`TransportStepSpecification`, `AttachmentSpecification`, `DateRangeSpecifications`). Sort field is a fixed `"createdAt"` in the admin attachment controller (`:~60`); other sort inputs not confirmed — needs confirmation. `TestDatabaseCleanup` JdbcTemplate is test fixtures only.

### F-20 Logging — Info
- No password/token values logged. JWT filter logs principal and authorities at DEBUG (`ConnectorJwtAuthenticationFilter.java:100-104`) and exception message at ERROR (`:109`); verifier logs DN at DEBUG (`ConnectorBackendClientVerifier.java:48`); SOAP logs userPrincipal string on failure (`ConnectorBackendWebServiceController.java:412-419`). `spring.jpa.show-sql=false` (`PROPS:35`). Link-partner `logging-enabled=true` (`PROPS:368`) may log message payloads — needs confirmation.

## 5. Coverage notes / not verified
- SOAP transport authentication (mTLS config) and `ConnectorCorsProperties` defaults were not traced to ground truth.
- Role semantics beyond `ADMIN` (`ConnectorRoleName`) not enumerated.
- No dynamic testing performed.
