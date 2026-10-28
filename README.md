# e-CODEX Connector New Generation

The e-CODEX Connector links national backend applications to the e-CODEX gateway (Domibus).
It receives business messages from backends, packages and signs them (ASiC-S container, trust-ok token, ETSI REM evidences) and submits them to the gateway, and in the other direction validates messages received from the gateway and delivers them to the right backend.

## Code coverage
### Global
![Coverage](.github/badges/jacoco.svg)

### Branches
![Branches](.github/badges/branches.svg)

## Documentation

The full documentation is written in AsciiDoc under [`connector-documentation/src/docs/asciidoc`](connector-documentation/src/docs/asciidoc):

| Guide | Content |
|-------|---------|
| [**Complete documentation**](connector-documentation/src/docs/asciidoc/index.adoc) | Single entry point: all guides below in one document, with a reading guide |
| [Installation](connector-documentation/src/docs/asciidoc/installation/index.adoc) | Prerequisites, database, Docker and JAR installation, TLS, security guidance |
| [Configuration](connector-documentation/src/docs/asciidoc/configuration/index.adoc) | Reference of all configuration properties, migration from 6.x |
| [Architecture](connector-documentation/src/docs/asciidoc/architecture/index.adoc) | Modules and message/evidence processing |
| [Compatibility](connector-documentation/src/docs/asciidoc/compatibility/index.adoc) | Tested databases, Java, gateway and broker versions |
| [Changelog](CHANGELOG.md) | Release notes |

HTML and PDF versions are built with `./gradlew :connector-documentation:build` (output in `connector-documentation/build/docs/asciidoc`) and are included in the distribution package. The complete documentation is `index.html` / `index.pdf`; each guide is also available on its own.

## REST API
The OpenAPI documentation is available at `http://localhost:30000/swagger-ui/index.html` once the connector is running.
Most endpoints require a JWT bearer token obtained from `POST /api/v1/auth/login`.

## Build from source

Requirements: JDK 21 and internet access (Docker is additionally needed for the integration tests).

```shell
# compile, run the unit tests and build the distribution
./gradlew build -x integrationTest

# run the integration tests (Testcontainers, requires Docker)
./gradlew integrationTest
```

The distribution is generated in `connector-distribution/build/` (`connector-distribution-<version>.zip` and the unpacked `connector-distribution/standalone/` directory).

## Run a local evaluation stack with Docker Compose

> [!IMPORTANT]
> This stack is intended for development and evaluation only. It uses default credentials, sample keystores and unauthenticated JMX.
> For production installations, follow the installation guide and its security checklist.

The `docker-compose.yml` file starts the connector together with the services it needs: MySQL (connector and gateway databases), Apache ActiveMQ Classic, MinIO (S3) and the Domibus gateway.
It also defines optional MariaDB, PostgreSQL and Oracle services that are not needed for the default (MySQL) setup.

Prerequisites: Docker Engine 24+ and Docker Compose 2.x, and read access to the e-CODEX registry for the gateway image (`docker login scm.ecodex.eu`).

### 1. Build the connector image

```shell
docker build -t connector-ng:activemq .
```

The tag must match the `image` of the `connector-ng` service in `docker-compose.yml`.
Alternatively, uncomment the `build` section of that service.

### 2. Set the environment variables

Create a file named `docker.env` in the root directory of the project.
Host names are those of the Compose services, and ports are the ports *inside* the Compose network:

```env
# MySQL
DB_USERNAME=connector
DB_PASSWORD=connector
DB_URL=jdbc:mysql://connector-ng-db:3306/connector
DB_DRIVER_CLASSNAME=com.mysql.cj.jdbc.MysqlXADataSource
DB_DIALECT=org.hibernate.dialect.MySQLDialect

# JWT authentication (at least 32 characters, e.g. the output of `openssl rand -base64 32`)
JWT_SECRET_KEY=<jwt_key>

# S3 (MinIO)
S3_ACCESS_KEY=admin
S3_SECRET_KEY=admin123
S3_REGION=us-east-1
S3_BUCKET=attachments
S3_ENDPOINT=http://minio-s3:9000

# Temporary directory
TEMP_DIR_LOCATION=/app/temp

# ActiveMQ
BROKER_URL=tcp://activemq:61616
BROKER_USER=admin
BROKER_PASSWORD=admin
```

> [!NOTE]
> These values can also be set directly in `docker/connector/config/application.properties`.

### 3. Prepare the connector configuration

The connector requires its configuration files to be present before the container starts.
Copy the sample configuration of the distribution into the directory mounted by Compose:

```shell
cp -r connector-distribution/src/main/resources/config/. docker/connector/config/
```

`application.properties` is a template: its values are placeholders and test values (test backend `backend_alice` / `cn=alice`, sample keystores, `pmode-blue.xml`).
For a local evaluation you only need the adjustments below; for any other use, replace every value as described in the chapter "Preparing application.properties" of the configuration guide.

Edit `docker/connector/config/application.properties`:

- the template declares a P-Mode file `./config/pmode/pmode-blue.xml`: place your gateway P-Mode there, or remove the `connector.business-domains.defaults[0].pmode.*` lines and upload it later from the administration portal — the connector does not start if the file is missing;
- create the `attachments` bucket in MinIO (console at `http://localhost:9001`) before the first message is sent.

> [!WARNING]
> Pay particular attention to the database connection settings, the JMS broker credentials, the S3 storage parameters, and the keystore/truststore configuration (paths, passwords and aliases). They are required for TLS and WS-Security communication, ASiC-S signing, business document verification and other features.

### 4. Start the stack

```shell
docker compose up -d
```

Once the containers are running, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| Connector REST API | `http://localhost:30000` | values of `connector.admin.*` (template: `admin` / `123456`, to be changed at first login) |
| Connector Swagger UI | `http://localhost:30000/swagger-ui/index.html` | |
| Connector health | `http://localhost:30000/actuator/health` | |
| Connector database (MySQL) | `localhost:30006` | `connector` / `connector` |
| MinIO console | `http://localhost:9001` | `admin` / `admin123` |
| ActiveMQ web console | `http://localhost:8161` | `admin` / `admin` |
| Domibus gateway console | `http://localhost:30010` | `admin` / `123456` (see also [gw-container](https://github.com/e-CODEX/gw-container/)) |

## Security

See the security guidance of the installation guide before deploying the connector outside a development environment.
Report suspected vulnerabilities privately to the e-CODEX team rather than in a public issue.

## License

Licensed under the [EUPL 1.2](LICENSE).
