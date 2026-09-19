# Sentinel AML — High-Level Design (HLD)

> Reflects the **actual built and verified** system, not the original pre-build plan.
> Location: `sentinel-aml/` · Verified: build passes, 19/19 tests green, app runs and was
> exercised end-to-end via curl/PowerShell against the live server.

## 1. Problem Statement

A bank needs a platform that watches customer transactions in (near) real time and
flags patterns associated with money laundering — without drowning analysts in false
positives, and while being able to justify *why* every alert fired (regulators and
auditors will ask).

## 2. Actors

| Actor | Role in the system |
|---|---|
| **Analyst** | Works the alert queue, dispositions alerts, opens/manages cases |
| **Compliance Admin** | Everything an Analyst can do, plus sees unmasked PII and read-only rule/currency config |
| **Detection Engine (system actor)** | Evaluates every transaction against 5 rules and raises alerts automatically |
| **Regulator / Auditor (indirect)** | Consumes the audit trail and SAR-filed cases after the fact — never touches the system directly in this build |

## 3. System Context

```
                 ┌───────────────────────────┐
   Analyst /     │                           │
   Compliance ──▶│   Sentinel AML Platform   │──▶ (future) SAR filing / regulator export
   Admin         │   (single Spring Boot     │
   (browser)     │    application)           │
                 │                           │◀── CSV bulk files (customers/accounts/txns)
                 └───────────┬───────────────┘
                             │
                             ▼
                    H2 file-mode database
                 (PostgreSQL-compatible SQL)
```

Everything — API, detection engine, static frontend, and database driver — runs inside
**one JVM process**, one runnable JAR. No external services required to demo it.

## 4. Layered Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│  Presentation:  static/ (HTML/CSS/vanilla JS, Tailwind via CDN)    │
├───────────────────────────────────────────────────────────────────┤
│  API layer:     REST controllers (Spring MVC)                     │
│                 - AlertController, CaseController,                │
│                 - TransactionController, IngestionController,     │
│                 - CustomerController, AdminController             │
│                 RBAC enforced here via @PreAuthorize (server-side,│
│                 never trusts the frontend)                        │
├───────────────────────────────────────────────────────────────────┤
│  Service layer: DetectionEngineService, AlertService, CaseService,│
│                 CurrencyService, IngestionService,                │
│                 TransactionIngestService                          │
├───────────────────────────────────────────────────────────────────┤
│  Rule engine:   AmlRule (strategy interface) +                    │
│                 5 concrete rules + RiskScorer                     │
├───────────────────────────────────────────────────────────────────┤
│  Persistence:   Spring Data JPA repositories + Hibernate entities │
├───────────────────────────────────────────────────────────────────┤
│  Database:      H2 (file-mode, PostgreSQL compatibility mode),    │
│                 schema/seed data versioned via Flyway              │
└───────────────────────────────────────────────────────────────────┘
```

## 5. Key Subsystems

### 5.1 Ingestion (two paths, one shared engine)
- **Streaming**: `POST /api/v1/transactions` — one transaction in, validated,
  currency-normalized, persisted, and run through detection inline (sub-second).
- **Bulk/batch**: CSV upload endpoints for customers, accounts, and transactions
  (`/api/v1/ingestion/*`) — row-level validation with per-row error reporting,
  and transactions are replayed through detection **in chronological order** so
  rules like structuring/rapid-movement see history in the right sequence.
- Both paths funnel into the same `DetectionEngineService.evaluate()` — one
  code path, two callers (DRY).

### 5.2 Detection Engine (Strategy Pattern)
Five independently-configurable rules, each implementing a common `AmlRule`
interface. Every transaction is evaluated against **all five** rules; any that
fire contribute to one combined, capped risk score (0–100). See the LLD for
exact thresholds and algorithms.

### 5.3 Alerting, Deduplication & Explainability
- Alerts are **not** raised per rule-hit; they're deduplicated by
  `customer + account + rule-set + calendar day`, so 4 structuring-band
  transactions on the same day produce **one** alert, not four.
- Every alert carries a human-readable explanation string and the exact
  evidence transaction IDs that caused it — nothing is a black box.
- Disposition changes (clear/close) require a reason and are written to an
  append-only audit log.

### 5.4 Case Management
Bundles one or more alerts (same customer only) into an investigation case,
with a priority auto-derived from the highest linked alert's risk score, a
status workflow (`OPEN → IN_PROGRESS → ESCALATED → SAR_FILED/CLOSED`), and the
same reason-required-audit-logged discipline as alerts.

### 5.5 Security & PII Protection
- HTTP Basic auth, two roles (`ANALYST`, `COMPLIANCE_ADMIN`), enforced with
  `@PreAuthorize` on every controller method — checked server-side, confirmed
  live (401 unauthenticated, 403 wrong role).
- Names and ID numbers are masked (`S**** M****`, `****9002`) for everyone
  except `COMPLIANCE_ADMIN`, computed server-side so the frontend can't leak
  it by accident.

### 5.6 Currency Normalization
Every transaction amount is converted to one base currency (INR by default)
via a configurable rate table, so all rule thresholds compare apples to
apples regardless of the transaction's original currency.

### 5.7 Auditability
Two append-only tables (`alert_audit_log`, `case_audit_log`) capture every
state transition with actor, old/new status, reason, and timestamp. The
application layer never issues `UPDATE`/`DELETE` against them.

## 6. Non-Functional Requirements — How They're Met

| Requirement | How |
|---|---|
| Explainability | Every alert stores a plain-English explanation + evidence transaction IDs |
| Auditability | Append-only audit tables for both alerts and cases |
| Configurability | All rule thresholds/weights + currency rates live in `application.yml`, viewable live via `/api/v1/admin/*` |
| RBAC | Spring Security method security, verified with live 401/403 checks |
| Deduplication | Composite dedup key prevents alert-flooding from repeated triggers |
| Testability | Rule logic is pure and Spring-context-free — all 19 unit tests run without booting Spring |

## 7. Deployment View

Single runnable Spring Boot fat JAR (`target/sentinel-aml-0.1.0.jar`), embedded
Tomcat on port 8080, embedded H2 database file under `./data/`. No Docker,
Kafka, or external Postgres required to run the whole thing — this was a
deliberate, documented scope decision to keep the hackathon build fully
self-contained (see README "What was cut and why").

## 8. Explicitly Out of Scope (for this build)

- Kafka / event-streaming ingestion (single-instance HTTP ingestion instead)
- Real PostgreSQL (H2 in PostgreSQL-compatibility mode instead)
- JWT auth (HTTP Basic instead — simpler for a local demo, same RBAC guarantees)
- React/Node frontend (plain static HTML/JS served by Spring Boot — zero extra tooling)
- Full CRUD admin API for rule tuning (config file + restart instead — kept the
  audit story simple: one version of the rules, not many)
