# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).
The same content is published in the documentation (`connector-documentation/src/docs/asciidoc/changelog`); keep both in sync.

## [7.1.0] - Unreleased

### Upgrade notes

- **Breaking — REST API**: the multipart part of the outbound message business content was renamed from `businessContent.contentFile` to `businessContent.xmlContent`.
- **Breaking — REST API**: `POST /api/v1/auth/refresh` no longer takes the access token; only the refresh token is sent.
- **Breaking — Authentication**: the REST API now requires a JWT bearer token (see *Security* below). The configuration must contain `connector.auth.security.jwt.*` and `connector.admin.*`.
- **Configuration**: the message processing properties can now be overridden. The verification modes are bound as `connector.message-processing.outbound-message-verification-mode` and `connector.message-processing.inbound-message-verification-mode`.
- **Database**: new tables (users, roles, refresh tokens), columns and indexes were added. Only initial scripts are provided (`dbscripts/initial`); compare them with your schema before upgrading.
- **Transactions**: Narayana was replaced by Atomikos; the JTA transaction log is written to `spring.jta.log-file` (default `./data/jtalog`), which must be on persistent storage.

### Added

- **Delivery evidence** — Automatic `DELIVERY` evidence submission once a message is delivered to the backend (`connector.message-processing.auto-trigger-delivery-evidences`).
- **Pull mode** — Backend link partners with `sender-mode=pull` retrieve inbound messages through the API ("ready for download" status, bulk download and acknowledgement).
- **Statistics and reporting** — Message statistics and reports, exportable as `csv`, `json` and `xlsx`, filterable by business domain.
- **Search** — Filter messages by business domain, direction, service, action and date range; filter attachments and transport steps by status, identifier and date range.
- **Message security** — Validity check of incoming business message containers.
- **Administration** — REST API endpoints supporting the administration portal: users and roles, business domains, processing modes (upload, download, truststore update and certificate details), parties, services, actions, link partners, JMS queue statistics, configuration overview, evidence and attachment download.
- **Authentication** — REST API endpoints require a valid JWT bearer token, except the explicitly listed public endpoints.
  - An admin account is created at startup from `connector.admin.*`; its password must be changed at first login.
  - `/api/v1/admin/users/**` requires the `ADMIN` role in addition to authentication.
  - Public endpoints are listed in the security guidance of the installation guide.
- **Connector-to-connector test** — Support for C2C test messages (`connector.c2ctests.*`, enabled by default) and test message submission from the administration API.
- **Backend ownership** — A backend client can only acknowledge or retrieve its own messages.

### Fixed

- **ebMS** — Set the correct `ebms:description` for outbound messages.
- **P-Mode** — Party roles are resolved from the definition's `<roles>` and `<process>` declarations instead of a fixed value; truststore content size; existence check when exchanging messages; correct error when a business domain already has a P-Mode.
- **Stream management** — Fixed improper stream closure that could lead to resource leaks.
- **Message** — Inbound message payload parsing; validation of AS4 properties of REST outbound messages; handling of business document data and LOB columns.
- **Compatibility** — Restored backward compatibility of messages between the 7.x and 4.x/6.x series.
- **REST API** — Invalid enum values now return HTTP 400 with the allowed values instead of HTTP 403.
- **Statistics** — Statistics computation and PostgreSQL compatibility of the statistics query.
- **Evidence trigger** — The evidence trigger process is now transactional.

### Technical

- **Database** — Support for Oracle; indexes added on entities.
- **Transaction management** — Replaced Narayana with Atomikos for JTA transaction management.
- **Spring Boot** — Upgraded to Spring Boot 4.1.0; Spring Boot Actuator enabled (`health`, `info`).
- **OpenTelemetry** — Support for OpenTelemetry export (metrics, traces, logs).
- **Web** — Support for Cross-Origin Resource Sharing (CORS, `connector.cors.*`).

### Security

- The configuration template contains test credentials and partners (`admin`/`123456`, keystores with password `12345`, test backend `cn=alice`). Replace them before going to production; see the security guidance.

## [7.0.0] - 2026-06-30

### Added

- **Business message sending** — Added support for sending business messages.
- **Reception acknowledgment** — Added support for backend confirmation of business message reception.
- **ASiC-S container compatibility** — Generated ASiC-S containers remain fully backward-compatible with the existing connector.
- **Message processing performance** — Improved overall message processing performance.
- **S3 storage support** — Added support for S3-based file storage.
- **REST/JMS backend integration** — Laid the foundation for future REST and JMS backend integration.

### Technical

- **Docker-friendly deployment** — The new connector is designed to run smoothly in containerized environments.
- **Shared JMS broker** — The connector now shares the same JMS broker (ActiveMQ Classic) as the gateway.
