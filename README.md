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

The build produces an executable JAR. The Maven `<version>` is `${revision}`, which defaults to `local-SNAPSHOT` for local builds; CI overrides it with `-Drevision=...` (see [CI/CD](#cicd)).

## Running

The default Spring profile uses an H2 in-memory database and is the only profile used in Docker — dev and production no longer have dedicated `dev`/`pdn` profiles. Instead, deployment-specific configuration (datasource, feature flags) is supplied entirely through environment variables that override the default profile, set via `docker-compose.yml` (production) / `docker-compose-dev.yml` (development). See [Docker](#docker) and [CI/CD](#cicd).

A `mac` profile and several `dbg` profile variants still exist under `src/main/resources/config/` for local development convenience.

```bash
# Run locally (H2, debug)
java -jar target/MiddleTier-Money-*.jar
```

In production the service runs as a Docker container, deployed automatically by GitHub Actions on port **12017**.

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

In the Docker deployment, database credentials are supplied as `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` environment variables in `docker-compose.yml` (production) / `docker-compose-dev.yml` (development). The CI workflows write these into a `.env` file from GitHub Actions secrets (`DB_PDN_MONEY_*` for production, `DB_DEV_MONEY_*` for development) before deploying — see [CI/CD](#cicd).

## Docker

The Dockerfile is at `src/main/resources/docker/Dockerfile`. It packages a pre-built JAR into a lightweight JRE 17 Alpine image and runs it with the default Spring profile; all environment-specific config is supplied at container runtime via environment variables.

Images are built and pushed automatically by CI (see [CI/CD](#cicd)) to the Nexus Docker registry as `money` (production) and `money-dev` (development). You normally don't need to build the image manually.

### Running

Deployment is via Docker Compose, using the checked-in compose files:

```bash
# Production
docker compose -f docker-compose.yml pull
docker compose -f docker-compose.yml up -d

# Development
docker compose -f docker-compose-dev.yml pull
docker compose -f docker-compose-dev.yml up -d
```

Both require a `.env` file alongside the compose file providing `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — CI writes this automatically as part of deployment; for a manual run, create it yourself.

### Volume mounts

| Mount point | Purpose | Production source | Development source |
|---|---|---|---|
| `/app/logs` | Application log files | named volume `money-logs` | named volume `money-dev-logs` |
| `/app/reports/working` | Report scratch directory (internal only) | named volume `money-working` | named volume `money-dev-working` |
| `/app/reports/share` | Generated report output (host-visible) | bind mount `/media/Shared/Documents/PDF/MoneyReports` | bind mount `/var/money/report` |
| `/app/reconcile` | Watched directory for reconciliation files | bind mount `/home/jason/Downloads` | bind mount `/var/money/reconcile` |

`/app/reports/share` and `/app/reconcile` are bind mounts (not named volumes) because they need to be visible to the host — the actual mounted share and the browser's default download directory, respectively. The development bind-mount paths are placeholders pending a real dev host layout.

### Container user

> **TODO:** The container currently runs as `root` (the default for the `eclipse-temurin:17-jre-alpine` base image). Running as root inside a container is not best practice; a future improvement is to add a dedicated non-root user to the Dockerfile and adjust bind-mount ownership accordingly.

## CI/CD

Two GitHub Actions workflows, each running on a dedicated self-hosted runner (config template at `src/main/resources/github/docker-compose.yml`, using the `myoung34/github-runner` image):

| Workflow | Trigger | Runner label | Deploys to |
|---|---|---|---|
| `.github/workflows/dev.yml` | push to any branch except `Release` | `money-dev` | development (`docker-compose-dev.yml`) |
| `.github/workflows/build.yml` | push to `Release` | `money-prod` | production (`docker-compose.yml`) |

Both workflows: build and run the full test suite (`mvn verify`, including Testcontainers-based integration tests), run a SonarCloud analysis (`mvn sonar:sonar`, organization `jbrmmg`), build a Docker image, push it to the Nexus Docker registry, then deploy via `docker compose pull && up -d`.

**SonarCloud note:** the free plan only supports viewing analysis of the main branch (`Release`). Feature-branch and pull-request analysis still runs (for any in-build warnings/log output) but isn't viewable on SonarCloud without a paid plan, so Sonar only runs in `build.yml`, not `dev.yml`.

**Versioning:** `pom.xml` declares `<version>${revision}</version>`. CI passes `-Drevision=...` to set it per build:
- `build.yml` (Release): `yyyy.mm.<github.run_number>`, e.g. `2026.06.42`
- `dev.yml`: `yyyy.mm.dd-SNAPSHOT`
- Local builds without `-Drevision` default to `local-SNAPSHOT`.

**Testcontainers on the runner:** the runner containers mount `/var/run/docker.sock` and set `DOCKER_HOST`/`TESTCONTAINERS_RYUK_DISABLED` so integration tests can launch MySQL via Testcontainers. A `src/test/resources/docker-java.properties` file pins `api.version=1.44`, which is required for compatibility with Docker Engine 29+ (which raised its minimum supported API version, breaking Testcontainers 1.x's auto-negotiation).

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
