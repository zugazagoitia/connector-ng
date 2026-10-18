# 01 – Authentication (JWT, refresh token, login, password hashing)

Read-only audit of the current code. Paths are relative to the repo root; abbreviations:
`INF` = `connector-infrastructure/src/main/java/eu/ecodex/connector/infrastructure`,
`APP` = `connector-application/src/main/java/eu/ecodex/connector/application`.

## Security model

- **Login** (`POST /api/v1/auth/login`, public – `INF/inbound/web/WebSecurityConfiguration.java:91`): `ConnectorUserAuthenticationProviderImpl.login` (`INF/outbound/auth/login/ConnectorUserAuthenticationProviderImpl.java:84-123`) delegates to Spring's `AuthenticationManager` with username/password, then issues an access JWT and a refresh token. Any `AuthenticationException` becomes a generic "Invalid username or password"; `DisabledException` becomes a distinct "account inactive" error.
- **Access token**: HS256-class JWT (`Keys.hmacShaKeyFor`, algorithm chosen by key length) signed with a symmetric key taken from `connector.auth.security.jwt.secret=${JWT_SECRET_KEY}` (`connector-distribution/src/main/resources/config/application.properties:423`). Claims: `sub`=username, `iat`, `exp`, `roles`, `userId` (`ConnectorJwtGenerator.java:80-94`). Configured lifetime 15m (`application.properties:424`; code default 1h, `ConnectorJwtProperties.java:35`). No `iss`/`aud`/`jti`.
- **Request validation**: `ConnectorJwtAuthenticationFilter` (`INF/outbound/auth/accesstoken/`) reads `Authorization: Bearer`, verifies signature+`exp` via `ConnectorJwtParser.parse` (`ConnectorJwtParser.java:183-189`), loads the user from DB by `sub`, checks `sub` equals username, and builds the authentication from the **DB** authorities (not the token's `roles`). Stateless sessions, CSRF disabled (`WebSecurityConfiguration.java:67,108`).
- **Refresh token**: 128 random bytes (`SecureRandom`, URL-safe Base64) returned to client; only the SHA-256 hex is stored (`APP/service/auth/refreshtoken/ConnectorRefreshTokenGenerator.java:24-27,56-60`). Lifetime 30d, cleanup cron daily 03:00 (`application.properties:426-427`). Login deletes all of the user's refresh tokens then issues one; `/refresh` (public) verifies, then revokes all active tokens of the user and issues a new pair; logout revokes by user.
- **Password hashing**: `BCryptPasswordEncoder(12)` (`WebSecurityConfiguration.java:135`), wrapped by `ConnectorUserPasswordEncoderImpl`.

## Findings

| ID | Title | Severity |
|----|-------|----------|
| AUTH-01 | No JWT secret strength/length validation; secret is raw string bytes | High |
| AUTH-02 | No login throttling / lockout (online brute force) | High |
| AUTH-03 | Refresh-token reuse not detected; revoked-token replay is a silent failure only | Medium |
| AUTH-04 | Refresh rotation is non-atomic (race allows parallel valid tokens) | Medium |
| AUTH-05 | Refresh endpoint does not re-check user `enabled`; disabled users keep refreshing | High |
| AUTH-06 | Logout/revocation does not invalidate the access token | Medium |
| AUTH-07 | User enumeration via distinct disabled-account response and timing | Medium |
| AUTH-08 | `iss`/`aud` configured/absent but never set or validated | Low |
| AUTH-09 | Refresh service mixes `Instant.now()` with injected `Clock`; no clock skew allowance | Low |
| AUTH-10 | Failed-login log lines record attacker-supplied username unsanitised (log injection) | Low |
| AUTH-11 | Expired-token helper `isExpired` returns false for invalid tokens (fail-open naming) | Info |
| AUTH-12 | Password hashing: BCrypt cost 12, 72-byte limit, no upgrade path | Info |
| AUTH-13 | Logout refresh-token ownership error message leaks user identifier | Low |

## Detailed findings

### AUTH-01 – JWT secret strength not enforced (High)
- **Evidence**: `INF/outbound/auth/accesstoken/ConnectorJwtParser.java:65`, `ConnectorJwtGenerator.java:63` (`Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`); `ConnectorJwtProperties.java:34` (`@NonNull String secret`, no length/entropy constraint); source is env var `JWT_SECRET_KEY` (`application.properties:423`; README.md:37 shows only the placeholder `<jwt_key>`).
- **Why it matters**: `hmacShaKeyFor` only rejects keys < 32 bytes; a 32-char dictionary/low-entropy string passes and HMAC tokens can be brute-forced offline, enabling forging of any user/role (`roles` claim is ignored by the filter, but `sub` alone suffices). A key shorter than 32 bytes fails at startup with a library error, which is acceptable, but no guidance on random generation or Base64 decoding exists. Key is not rotatable (single key, no `kid`).
- **Fix**: Require ≥ 256 bits of random key (Base64-decoded), fail fast with a clear message, and document generation (`openssl rand -base64 48`).

### AUTH-02 – No brute-force protection on login (High)
- **Evidence**: `ConnectorUserAuthenticationProviderImpl.java:84-123` (no attempt counter, delay, lockout); `/api/v1/auth/login` permit-all (`WebSecurityConfiguration.java:91`); no lockout/attempt fields found in the auth code or `application.properties` (needs confirmation that no gateway/WAF rate-limits upstream).
- **Why it matters**: Unlimited password guessing against any known username; BCrypt-12 slows each guess (~250 ms) but not parallel attempts.
- **Fix**: Add per-username and per-IP rate limiting with temporary lockout/backoff on repeated failures.

### AUTH-03 – No refresh-token reuse detection (Medium)
- **Evidence**: `ConnectorVerifyUserRefreshTokenService.java:65-67` throws "Refresh token revoked" and nothing else; `ConnectorRefreshUserRefreshTokenService.java:117-122` rotation revokes the user's tokens but a later replay of a stolen old token is merely rejected.
- **Why it matters**: A stolen refresh token that is replayed after the legitimate client rotated is not treated as compromise, so the attacker/victim race is invisible and the live session is not killed or alerted.
- **Fix**: On presentation of a revoked token, revoke all of that user's tokens and log a security event.

### AUTH-04 – Non-atomic rotation (Medium)
- **Evidence**: `ConnectorRefreshUserRefreshTokenService` class has no `@Transactional` (compare `ConnectorRegisterUserRefreshTokenService.java:35`); verify (line 87), revoke (118-120) and issue (122) are separate operations; `ConnectorUserRefreshTokenJpaRepository` modifying queries are individually `@Transactional`.
- **Why it matters**: Two concurrent `/refresh` calls with the same token can both pass verification before either revokes, yielding two valid new refresh tokens; a failure after revoke and before issue leaves the user with no token. Also `revokeByUserUuid(user.uuid(), Instant.now())` (line 120) bypasses the injected `Clock` (see AUTH-09). Needs confirmation of DB-level unique constraints.
- **Fix**: Make rotation one transaction using an atomic conditional update (`UPDATE … SET revoked=true WHERE hash=? AND revoked=false`) and check the row count.

### AUTH-05 – Refresh ignores account state (High)
- **Evidence**: `ConnectorVerifyUserRefreshTokenService.java:53-72` checks only hash, `user.uuid()!=null`, revoked, expiry; `ConnectorRefreshUserRefreshTokenService.java:86-100` then mints tokens for `verifiedRefreshToken.user()` with no `enabled()` check, and roles come from the stored user at that moment.
- **Why it matters**: Disabling a user or removing roles does not stop refresh, so access continues up to 30 days unless the admin path also revokes tokens (needs confirmation in `ConnectorEditUserService`/`ConnectorRemoveUserService`). The JWT filter does reload the user per request but the filter does not check `isEnabled()` either (`ConnectorJwtAuthenticationFilter.java:85-86`; no explicit `isEnabled` test, relies on nothing).
- **Fix**: Reject refresh and filter authentication when `user.enabled()` is false, and revoke tokens on disable/delete.

### AUTH-06 – Access token not revocable (Medium)
- **Evidence**: `ConnectorUserAuthenticationProviderImpl.java:125-128` logout only calls `revokeUserRefreshToken`; the filter validates only signature, `exp` and `sub` (`ConnectorJwtParser.java:115-122`).
- **Why it matters**: A stolen access token stays valid for its remaining lifetime (≤15m configured, 1h if property omitted) after logout or password change.
- **Fix**: Keep lifetime short; optionally add a `jti`/token-version check against the user record.

### AUTH-07 – User enumeration (Medium)
- **Evidence**: `ConnectorUserAuthenticationProviderImpl.java:112-115` returns a distinct "Your account is inactive" for `DisabledException` (only raised after the password is verified by Spring's `DaoAuthenticationProvider`, so it is only revealed to a correct-password holder – low exposure); `ConnectorRetrieveUserByUsernameService.java:49-51` message "User not found by username %s" used by `loadUserByUsername` (`ConnectorUserDetailsServiceImpl.java:44`). Which `AuthenticationProvider` actually runs and whether `hideUserNotFoundExceptions` stays at the Spring default (true) needs confirmation; if a missing user throws before hashing, response time differs from a wrong password for an existing user (BCrypt-12 ≈ hundreds of ms vs. a DB miss), enabling timing enumeration.
- **Why it matters**: Valid usernames can be harvested by timing, helping AUTH-02 attacks.
- **Fix**: Compare against a dummy BCrypt hash when the user does not exist (Spring's default does this only if the standard provider is used) and verify with a timing test.

### AUTH-08 – Issuer/audience not used (Low)
- **Evidence**: property `connector.auth.security.jwt.issuer=connector.ecodex.eu` (`application.properties:425`) is not a field of `ConnectorJwtProperties` (lines 34-36) and neither `ConnectorJwtGenerator.java:87-93` sets `.issuer()` nor `ConnectorJwtParser.java:184-188` calls `requireIssuer`.
- **Why it matters**: Tokens signed with the same key by another service/environment are accepted; the setting gives a false impression of validation.
- **Fix**: Set `iss` on issue and `requireIssuer` on parse (and add `aud`), or delete the dead property.

### AUTH-09 – Clock handling / skew (Low)
- **Evidence**: parser uses injected clock (`ConnectorJwtParser.java:186`) but no `clockSkewSeconds`; `ConnectorRefreshUserRefreshTokenService.java:120` uses `Instant.now()`.
- **Why it matters**: Default skew is 0 (acceptable for a single-issuer/verifier deployment; multi-node deployments with drift see spurious 401s); inconsistent clocks make time-based tests and revocation timestamps unreliable.
- **Fix**: Use the injected `Clock` consistently; set small explicit skew only if multi-node.

### AUTH-10 – Log injection / credential-adjacent logging (Low)
- **Evidence**: `ConnectorUserAuthenticationProviderImpl.java:113,117,120` log the raw submitted `username` at ERROR; `ConnectorJwtAuthenticationFilter.java:109` logs exception messages from token parsing (JJWT messages do not include the token; verified not to log the token itself); `ConnectorJwtGenerator.java:86` logs username at DEBUG.
- **Why it matters**: CR/LF in usernames can forge log lines; users mistyping a password in the username field leak it to logs.
- **Fix**: Sanitise/escape or truncate usernames in log statements.

### AUTH-11 – `isExpired` semantics (Info)
- **Evidence**: `ConnectorJwtParser.java:131-140` returns `false` for tampered/malformed tokens.
- **Why it matters**: Callers must not treat `false` as "valid"; current caller (`ConnectorJwtAuthenticationProvider.java:80-82`) is not used in the filter path, so no exploitable issue found today.
- **Fix**: Rename or return a tri-state (valid/expired/invalid).

### AUTH-12 – Password hashing parameters (Info)
- **Evidence**: `WebSecurityConfiguration.java:135` `new BCryptPasswordEncoder(12)`; `ConnectorUserPasswordEncoderImpl.java:34-39` returns the input unchanged when it has no text.
- **Why it matters**: BCrypt cost 12 is acceptable today (random per-hash salt, adaptive). BCrypt silently ignores bytes beyond 72, and there is no `DelegatingPasswordEncoder`/`upgradeEncoding` path to move to Argon2id later. `encodePassword` returning a blank password as-is must be guarded by callers (password policy validation in `ConnectorUpdateUserPasswordService` / `ConnectorRegisterUserService` needs confirmation).
- **Fix**: Enforce a password policy with max length ≤ 72 bytes and reject blanks before encoding; consider `DelegatingPasswordEncoder` for future migration.

### AUTH-13 – Identifier disclosure in revoke error (Low)
- **Evidence**: `ConnectorRevokeUserRefreshTokenService.java:41-44` (`"Invalid refresh token for user " + uuid`) and distinct "Invalid refresh token" at line 40.
- **Why it matters**: Lets a caller distinguish "token exists but belongs to someone else" from "unknown token", which confirms token validity to an attacker holding a guessed/leaked token.
- **Fix**: Return the same generic error in both cases.

## Positive observations
- Refresh tokens: 1024-bit CSPRNG value, stored hashed (SHA-256 is appropriate for high-entropy tokens), expiry enforced, daily purge of expired/old revoked tokens (`ConnectorCleanupUserRefreshTokenService.java:44-45`).
- JWT parsing uses the typed `parseSignedClaims` with a pinned key, so `alg=none` and unsigned tokens are rejected.
- Authorities come from the DB on each request, not from token claims.
