# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Full build including integration tests
mvn verify

# Build without integration tests (faster)
mvn package -Dskip.surefire.tests=true

# Run all unit tests
mvn test

# Run a single unit test class
mvn test -Dtest=AccountTest

# Run a single integration test class
mvn verify -Dit.test=MoneyIT -DskipTests

# Run integration tests only
mvn failsafe:integration-test failsafe:verify
```

Unit tests (`*Test.java`) run against H2 in-memory; integration tests (`*IT.java`) use Testcontainers with a real MySQL container.

## Architecture Overview

### Dual-Datasource Design

The application maintains **two independent JPA datasources**, each with its own entity manager, transaction manager, and Liquibase changelog:

- **Primary** (`ConfigDbPrimary`): Entities in `data/primary/`, repositories in `data/primary/repository/`. This is the main financial database (accounts, transactions, statements, categories, etc.). Config prefix: `spring.datasource`.
- **Internal** (`ConfigDbInternal`): Entities in `data.internal/`, repositories in `data/internal/repository/`. Used for `TransactionReport`. Config prefix: `spring.datasource.internal` / `internal.liquibase`.

When adding new entities, they must go into one of these two packages so the correct entity manager picks them up. Never mix entities across datasource boundaries.

### Request Flow

Controllers (`control/`) → Managers (`manager/`) → Repositories (`data/primary/repository/`)

Controllers handle HTTP mapping only; all business logic lives in managers. DTOs (`dto/`) are the API boundary — ModelMapper converters in `dto/mapper/converter/` handle the entity↔DTO conversions. Never expose JPA entities directly from controllers.

### AbstractManager Cache

`AccountManager` and `CategoryManager` extend `AbstractManager`, which maintains an **in-memory HashMap cache** loaded lazily on first access. This cache is never invalidated during runtime — changes go through the manager so the cache stays consistent. Do not bypass the manager and write to the repository directly for account/category data.

### FinancialAmount

All monetary values use `FinancialAmount` (wrapping `BigDecimal`) rather than primitive doubles or raw `BigDecimal`. It carries a `CR`/`DB` type derived from sign. Custom Jackson serializer/deserializer handle JSON. Use `FinancialAmount` for any new monetary field.

### Spring Events

Managers publish Spring `ApplicationEvent`s for significant state changes (`UpdateTransactionEvent`, `DeleteTransactionEvent`, `ReconcileTransactionEvent`, `StatementLockEvent`, `ReconciliationFileLoadEvent`). Downstream managers listen to these. Prefer events over direct manager-to-manager calls when the coupling is one-way.

### Reconciliation File Monitoring

`ReconciliationFileMonitor` extends Spring DevTools `FileSystemWatcher` and watches the directory configured at `money.reconcile-file-location`. On startup it replays all existing files, then monitors for changes. Results are pushed to clients via Server-Sent Events at `/api/v1/reconciliation/file-updates`.

### Scheduled Tasks

Three independently controlled cron jobs, each guarded by a `money.*-enabled` flag (all default `false`):
- Regular payment generation (`RegularCtrl`) — creates transactions from `Regular` definitions
- Report generation (`ReportGenerator`) — produces PDFs via iTextPDF/XMLWorker pipeline
- Archive (`ArchiveManager`) — moves old transactions

### API URL Structure

All endpoints are served under the `/api/v1` base path. Key routes include `/api/v1/transaction/list`, `/api/v1/accounts`, `/api/v1/categories`, `/api/v1/reconciliation/*`, etc.

## Spring Profiles

| Profile | Database | Port |
|---|---|---|
| *(default)* | H2 in-memory (H2 console at `/h2`) | 13017 |
| `dev` | MySQL | 10017 |
| `pdn` | MySQL (production) | 12017 |

The `dbg` profile variant (e.g., `dbg-dev`) loads additional seed data via a separate Liquibase changelog (`db/changelog/dbg/`).

## Database Migrations

Liquibase changelogs live in `src/main/resources/db/changelog/`. The master file (`db.changelog-master.yaml`) applies to the primary datasource; `internal/db.changelog-master.yaml` applies to the internal datasource. H2-incompatible SQL has a parallel version under `db/changelog/dbg/`. Integration test data is in `db/changelog/dbg-it/`.

## Testing Conventions

- Unit tests extend nothing special; they use H2 and `@SpringBootTest`.
- Integration tests extend `Support` (which provides a configured `MockMvc`) and use `@ActiveProfiles("it")` + Testcontainers MySQL. The `Support.java` base class wires up MockMvc via `webAppContextSetup`.
- Integration test classes are named `*IT` and live under `integration/` so Failsafe picks them up; unit test classes are named `*Test` and are picked up by Surefire.

## Production Environment Variables

```
db.pdn.money.server
db.pdn.money.user
db.pdn.money.password
```
