# MiddleTier-Money

A Spring Boot REST API service for managing personal finances. It tracks accounts, transactions, statements, and regular payments; generates PDF reports; processes bank reconciliation files; and provides a REST API for configuration and operational control.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.5 (Undertow) |
| Database | MySQL (production), H2 (test/debug) |
| Migrations | Liquibase |
| Build | Maven |
| Coverage | JaCoCo |
| API Docs | SpringDoc OpenAPI / Swagger UI |

## Features

- **Account management** — Create and manage bank accounts with account-specific SVG logos
- **Transaction management** — Record, update, delete, and query financial transactions with filtering
- **Category management** — Organise transactions into categories with configurable sort order
- **Regular payment scheduling** — Define recurring payments that auto-generate transactions on a configurable cron schedule, with weekend date adjustment support
- **Statement management** — Create monthly statements; lock statements to prevent further modification; delete with validation
- **Reconciliation** — Load and process bank reconciliation files; match transactions against statements; real-time file update notifications via Server-Sent Events
- **Reports** — Generate monthly and annual financial reports as PDFs; automatic scheduled report generation
- **Archive** — Archive historical transactions on a configurable schedule
- **Email reporting** — Generate and email transaction reports via SMTP
- **Health & metrics** — Spring Boot Actuator endpoints exposed for monitoring

## Building

```bash
# Full build including integration tests
mvn verify

# Skip integration tests
mvn package -Dskip.surefire.tests=true
```

The build produces an executable JAR and a deployment zip assembly.

## Running

The service uses Spring profiles to select the configuration:

| Profile | Purpose | Port |
|---|---|---|
| *(default)* | Local/debug using H2 in-memory database | 13017 |
| `dev` | Development environment (MySQL) | 10017 |
| `pdn` | Production (MySQL) | 12017 |

```bash
# Run locally (H2, debug)
java -jar target/MiddleTier-Money-*.jar

# Run with a specific profile
java -jar target/MiddleTier-Money-*.jar --spring.profiles.active=pdn
```

In production the service runs as a systemd unit (`middletier-money.service`) on port **12017**.

## Configuration

All custom properties are under the `money.*` namespace. Key properties:

| Property | Description | Default |
|---|---|---|
| `money.regular-enabled` | Enable/disable scheduled regular payment generation | `false` |
| `money.regular-schedule` | Cron expression for regular payment generation | `0 30 2 * * ?` |
| `money.report-enabled` | Enable/disable scheduled report generation | `false` |
| `money.report-schedule` | Cron expression for report generation | `0 0 4 1-10 * ?` |
| `money.report-working` | Scratch directory for report generation | — |
| `money.report-share` | Directory where generated reports are saved | — |
| `money.archive-enabled` | Enable/disable scheduled archiving | `false` |
| `money.archive-schedule` | Cron expression for archiving | `0 0 4 1-10 2 ?` |
| `money.reconcile-file-location` | Directory to watch for reconciliation files | — |
| `money.smtp-host` | SMTP server hostname | `smtp.jbrmmg.me.uk` |
| `money.smtp-port` | SMTP server port | `1025` |
| `money.smtp-from` | Sender email address | `system@jbrmmg.me.uk` |

Production database credentials are supplied via environment variables:
- `db.pdn.money.server`
- `db.pdn.money.user`
- `db.pdn.money.password`

## Docker

The Dockerfile is at `src/main/resources/docker/Dockerfile`. It packages a pre-built JAR into a lightweight JRE 17 Alpine image and runs it with the `pdn` profile by default.

### Building the image

Download the JAR from Nexus first, then build from the project root:

```bash
curl -O http://nexus.jbrmmg.me.uk:8081/repository/maven-releases/com/jbr/middle-money/MiddleTier-Money/26.3.1/MiddleTier-Money-26.3.1.jar
mv MiddleTier-Money-26.3.1.jar target/

docker build -f src/main/resources/docker/Dockerfile -t money-app:26.3.1 .
```

### Host directories

Create the following directories on the host before running the container:

```bash
sudo mkdir -p /var/log/money /var/data/money/reconcile /var/data/money/reports
```

> **TODO:** These paths are temporary. They will be revisited when a proper CI/CD pipeline is set up.

### Running the container

```bash
docker run -d -p 12017:12017 \
           -v /var/log/money:/app/logs \
           -v /var/data/money/reconcile:/app/reconcile \
           -v /var/data/money/reports:/app/reports \
           money-app:26.3.1
```

The `-d` flag runs the container in detached (background) mode. To view logs:

```bash
docker logs <container-id>       # view logs on demand
docker logs -f <container-id>    # follow logs (like tail -f)
```

To use a different profile:

```bash
docker run -d -p 12017:12017 money-app:26.3.1 --spring.profiles.active=dev
```

### Volume mounts

The container expects three directories to be bind-mounted for persistent data:

| Mount point | Purpose | Env var override |
|---|---|---|
| `/app/logs` | Application log files | `LOGGING_FILE_NAME` |
| `/app/reconcile` | Watched directory for reconciliation files | `MONEY_RECONCILE_FILE_LOCATION` |
| `/app/reports` | Report working and output directories | `MONEY_REPORT_WORKING`, `MONEY_REPORT_SHARE` |

### Container user and host directory permissions

> **TODO:** The container currently runs as `root` (the default for the `eclipse-temurin:17-jre-alpine` base image). This means the bind-mounted host directories require no special permissions, but running as root inside a container is not best practice.
>
> A future improvement is to add a dedicated non-root user to the Dockerfile:
> ```dockerfile
> RUN addgroup -S money && adduser -S money -G money
> USER money
> ```
> Once that is done, the host directories will need to be owned by that user's uid (typically 1000):
> ```bash
> chown -R 1000:1000 /host/path/logs /host/path/reconcile /host/path/reports
> ```

## REST API

The API is self-documented via Swagger UI at `/swagger-ui.html` when the service is running.

### Base paths

| Base path | Purpose |
|---|---|
| `/jbr/ext/money` | Configuration — accounts, categories, statements, regular payments |
| `/jbr/int/money` | Operations — transactions, reports, archive, email |

### Key endpoints

#### Accounts (`/jbr/[ext|int]/money`)

| Method | Path | Description |
|---|---|---|
| GET | `/accounts` | List all accounts |
| POST | `/accounts` | Create account |
| PUT | `/accounts` | Update account |
| DELETE | `/accounts` | Delete account |
| GET | `/account/logo` | Get account SVG logo |

#### Categories (`/jbr/[ext|int]/money`)

| Method | Path | Description |
|---|---|---|
| GET | `/categories` | List all categories |
| POST | `/categories` | Create category |
| PUT | `/categories` | Update category |
| DELETE | `/categories` | Delete category |

#### Transactions (`/jbr/[ext|int]/money`)

| Method | Path | Description |
|---|---|---|
| POST | `/transaction` | Create transaction(s) |
| PUT | `/transaction` | Update transaction(s) |
| DELETE | `/transaction` | Delete transaction(s) |
| POST | `/transaction/list` | Query transactions with filter |

#### Regular Payments (`/jbr/[ext|int]/money`)

| Method | Path | Description |
|---|---|---|
| GET | `/transaction/regulars` | List all regular payments |
| POST | `/transaction/regulars` | Create regular payment |
| PUT | `/transaction/regulars` | Update regular payment |
| DELETE | `/transaction/regulars` | Delete regular payment |

#### Statements (`/jbr/[ext|int]/money`)

| Method | Path | Description |
|---|---|---|
| GET | `/statement` | List statements (filterable by account and lock status) |
| POST | `/statement` | Create statement |
| POST | `/statement/lock` | Lock a statement |
| DELETE | `/statement` | Delete statement |

#### Reconciliation (`/jbr/[ext|int]/money`)

| Method | Path | Description |
|---|---|---|
| PUT | `/reconcile` | Mark transactions as reconciled |
| POST | `/reconciliation/load` | Load a reconciliation file |
| PUT | `/reconciliation/updatefileaccount` | Update account mapping on a file |
| GET | `/reconciliation/files` | List loaded reconciliation files |
| GET | `/reconciliation/file-updates` | Server-Sent Events stream of file updates |
| DELETE | `/reconciliation/clear` | Clear reconciliation data |

#### Reports & Archive (`/jbr/int/money`)

| Method | Path | Description |
|---|---|---|
| POST | `/transaction/report` | Generate a monthly PDF report |
| POST | `/transaction/annualreport` | Generate an annual PDF report |
| POST | `/transaction/archive` | Archive transactions for a period |
| POST | `/email` | Send a transaction report email |

### Actuator

Spring Boot Actuator is fully exposed. The health endpoint includes service-specific details:

```
GET /actuator/health
```

## Database Migrations

Schema is managed by Liquibase. Changelogs are in `src/main/resources/db/changelog/`. Separate changelogs exist for the internal datasource (`db/changelog/internal/`) and H2-compatible debug usage (`db/changelog/dbg/`).

## Project Structure

```
src/main/java/com/jbr/middletier/money/
  config/         Application configuration and properties
  control/        REST controllers
  data/           JPA entities (primary and internal datasources)
  dto/            Data transfer objects and mappers
  exceptions/     Custom exceptions and REST error handler
  manager/        Business logic managers
  reconciliation/ Reconciliation file processing
  reporting/      PDF and email report generation
  schedule/       Scheduled tasks (regular payments)
  util/           Utility classes
  xml/            SVG logo and HTML processing
```
