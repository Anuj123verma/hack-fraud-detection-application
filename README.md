# Sentinel AML — Real-Time Money Laundering Detection Platform

Hackathon MVP for MeridianTrust's "Walmart TMS" brief. Built under a **1-hour**
timebox, so scope was ruthlessly triaged (see "What was cut" below).

## What's here

- Java 17 + Spring Boot 3 backend, layered architecture (`controller -> service -> repository`)
- Detection engine implementing all 5 required AML typologies as independent, unit-tested rule classes
- H2 file-based database in **PostgreSQL compatibility mode** (see "Why H2 instead of Postgres")
- Flyway-migrated schema + synthetic seed data covering 4 typologies (structuring, rapid movement, high-risk jurisdiction, behavioral deviation) plus clean "noise" customers
- Alert generation with risk scoring, DB-level de-duplication, and an immutable audit log
- RBAC via Spring Security (HTTP Basic + method-level `@PreAuthorize`) with PII masking
- OpenAPI/Swagger UI for interactive API docs
- JUnit5 unit tests for every rule, covering boundary conditions

## Why H2 instead of Postgres

The brief prefers Postgres, but this box had **no Java, Maven, Postgres, or
Docker installed**, and only a 1-hour budget. Installing/configuring a real
Postgres server would have eaten the entire timebox. H2 in `MODE=PostgreSQL`
gives the same SQL surface (JPA/Flyway code is identical either way) with
zero install time. **To switch to real Postgres:** change 3 lines in
`application.yml` (`spring.datasource.url/driver-class-name`, swap the H2
Maven dependency for `postgresql`) — no other code changes needed.

## Architecture

```
Client (curl / Swagger UI)
        |  HTTP Basic auth
        v
Controller layer  (TransactionController, AlertController)
        v
Service layer     (TransactionIngestService, DetectionEngineService, AlertService, CurrencyService)
        v
Rules package      (5x AmlRule implementations, strategy pattern, pure/stateless)
        v
Repository layer  (Spring Data JPA)
        v
H2 (Postgres-compatible) — Flyway-migrated schema + seed data
```

Every transaction — whether it arrives via the streaming REST endpoint or
the startup bulk-replay runner — flows through the **same**
`DetectionEngineService.evaluate()` method. One code path, two callers: DRY.

## Rule engine

Rules live in `com.meridiantrust.sentinel.rules`. Each one implements
`AmlRule.evaluate(RuleContext) -> RuleResult` and is completely stateless —
no Spring context needed to unit test it (see `src/test/.../rules/`).

| Rule class | Business rule | Config (application.yml `aml.rules.*`) |
|---|---|---|
| `LargeSingleTransactionRule` | CTR-style: single txn >= threshold | `large-single-txn.threshold-base` |
| `StructuringRule` | 3+ txns of $9,000-$9,999 within 24h | `structuring.min-amount/max-amount/min-count/window-hours` |
| `RapidMovementRule` | >=80% of a credit moved out within 48h | `rapid-movement.min-percent-out/window-hours` |
| `HighRiskJurisdictionRule` | counterparty jurisdiction on watchlist | `high-risk-jurisdiction.jurisdictions` |
| `BehavioralDeviationRule` | daily value > 3x the 90-day rolling average | `behavioral-deviation.multiplier/lookback-days` |

**Tuning a rule requires no code change or redeploy** — edit the relevant
value in `application.yml` (or override via environment variable, e.g.
`AML_RULES_STRUCTURING_MINCOUNT=4`) and restart.

Risk scores are the sum of each triggered rule's configured `weight`,
capped at 100 (`RiskScorer.combine`). Alerts are de-duplicated by
`customerId|accountId|ruleCodes|calendarDay` at the database level (unique
constraint + upsert), so repeated triggers of the same pattern on the same
day aggregate into one alert instead of spamming the queue.

## Case management

`AmlCase` bundles one or more related Alerts under a single investigation.
Implemented in `CaseService`/`CaseController`, mirroring `AlertService`'s
audit pattern exactly (own `case_audit_log` table, append-only, reason
required to close/file-SAR):

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/cases` `{"alertIds":[...]}` | Open a case from one or more alerts (must share one customer) |
| `POST /api/v1/cases/{id}/alerts/{alertId}` | Link another alert into an existing case; priority auto-recomputed from the max linked risk score |
| `PATCH /api/v1/cases/{id}/status` `{"status":"CLOSED","reason":"..."}` | Move the case through OPEN -> IN_PROGRESS -> ESCALATED -> SAR_FILED -> CLOSED |
| `GET /api/v1/cases` | Case queue, sorted by priority then recency, PII masked |
| `GET /api/v1/cases/{id}` | Case detail with linked alert IDs; PII unmasked for `compliance_admin` only |

## Bulk CSV ingestion

Complements the streaming endpoint from before - same validate/persist path
under the hood, plus a full ingestion error report (row number + reason for
every rejected row; nothing is ever silently dropped). Requires
`compliance_admin` (bulk loads are an admin operation, not an analyst one).
Sample files are in `sample-data/`.

```bash
curl -u compliance_admin:admin123 -F "file=@sample-data/customers.csv" \
  http://localhost:8080/api/v1/ingestion/customers

curl -u compliance_admin:admin123 -F "file=@sample-data/accounts.csv" \
  http://localhost:8080/api/v1/ingestion/accounts

curl -u compliance_admin:admin123 -F "file=@sample-data/transactions.csv" \
  http://localhost:8080/api/v1/ingestion/transactions
```

Transaction rows are replayed through the detection engine in chronological
order after the whole batch persists, so window-based rules (structuring,
rapid movement, behavioral deviation) see correct history regardless of the
order rows appear in the file. The sample `transactions.csv` above trips
both the Structuring rule (3 txns in the $9k-9.999k band on `ACC-101`) and
the Large-Single-Transaction rule (`ACC-102`, $12,000).

## Admin visibility

`GET /api/v1/admin/rule-config` and `GET /api/v1/admin/currency-config`
(compliance_admin only) show the currently-effective thresholds/rates
sourced from `application.yml` - handy for confirming what's live without
grepping config files. Tuning is still config-file + restart based (see
"Rule engine" section above), not a live-editable admin API - documented
tradeoff, not an oversight.

## Frontend

Plain HTML/CSS/JS (native ES modules, no bundler, no npm/Node needed at
all) served directly by Spring Boot from `src/main/resources/static/` -
it's just there the moment the app starts, at `http://localhost:8080/`.
This sidesteps the exact tooling problem we hit with Maven: zero build
step means zero new install dependencies.

- `index.html` - shell + tab nav (Alert Queue / Cases / Customers-Timeline)
- `js/api.js` - fetch wrapper; stores Basic Auth creds in `sessionStorage` only (never persisted to disk), attaches them to every API call
- `js/alerts.js` - alert queue table + detail panel + disposition form + "Open Case from this Alert"
- `js/cases.js` - case queue table + detail panel + link-alert / status-update forms
- `js/customers.js` - customer list + click-through transaction timeline
- `js/util.js` - shared HTML-escaping / date / risk-badge helpers (XSS-safe rendering - every user-controlled string goes through `escapeHtml`)
- `css/styles.css` - fallback styling if the Tailwind CDN `<script>` tag gets blocked by a proxy; the app stays fully usable either way

Sign in with either demo user (see Auth table below) directly in the top-right
bar - no separate login page, credentials are just attached to each API call.

## Running it

**Verified working end-to-end** (Windows, JDK 17 Temurin + Maven Daemon 1.0.6,
no system Maven/network access to Apache required):

```bat
run-app.bat     REM builds + starts the app on http://localhost:8080
run-tests.bat   REM mvnd -B test  -> 19/19 passing
package.bat     REM mvnd -B clean package -> runnable target\sentinel-aml-0.1.0.jar
```

Or with a standard `mvn`/`mvnd` on your PATH:

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`. On startup, `DetectionBootstrapRunner`
automatically replays every seeded transaction through the detection engine
(logs timing to prove the "bulk load" performance path), so the alert queue
is already populated — no manual steps needed to see results. Confirmed via
curl/PowerShell: alert queue + detail + disposition, case open/link/status
(reason-required close enforced with a 400), CSV bulk ingestion, customer
timeline, RBAC (401 unauthenticated / 403 wrong role), and Swagger UI all
work against the live app.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console (dev only): not exposed by default; enable `spring.h2.console.enabled=true` if needed.

### Auth

Two in-memory users (HTTP Basic). Passwords come from env vars, with demo
fallbacks — **override these in any non-local environment**:

| User | Password (env var) | Roles |
|---|---|---|
| `analyst` | `ANALYST_PASSWORD` (default `analyst123`) | ANALYST |
| `compliance_admin` | `COMPLIANCE_ADMIN_PASSWORD` (default `admin123`) | ANALYST, COMPLIANCE_ADMIN |

### Demo walkthrough (curl)

```bash
# 1. See the alert queue the bootstrap runner already populated (masked PII)
curl -u analyst:analyst123 http://localhost:8080/api/v1/alerts

# 2. Open one alert's full detail (still masked - analyst role)
curl -u analyst:analyst123 http://localhost:8080/api/v1/alerts/<id>

# 2b. Same alert, unmasked (compliance_admin role)
curl -u compliance_admin:admin123 http://localhost:8080/api/v1/alerts/<id>

# 3. Ingest a brand-new streaming transaction that trips the CTR rule
curl -u analyst:analyst123 -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"ACC-001","externalTxnId":"TXN-DEMO-1","direction":"DEBIT",
       "amount":15000,"currency":"INR","counterpartyName":"Demo Corp",
       "channel":"WIRE"}'

# 4. Disposition an alert (reason required to clear/close - enforced server-side)
curl -u analyst:analyst123 -X PATCH http://localhost:8080/api/v1/alerts/<id>/disposition \
  -H "Content-Type: application/json" \
  -d '{"status":"CLEARED","reason":"Confirmed legitimate payroll batch with customer."}'
```



## What was cut (and why)

| Cut | Reason | Mitigation |
|---|---|---|
| Kafka streaming | Needs infra not installed/available | Inline synchronous detection on the REST ingest endpoint gives the same sub-second latency for the demo |
| React/Angular frontend | Time | Swagger UI + curl walkthrough serves as the UI for the demo |
| Full admin rule-config CRUD API (live edit) | Time; keeps rule-version audit trail simpler | GET /api/v1/admin/rule-config gives read visibility; tuning is config-file + restart based |
| AOP-based audit aspect | Time | Audit rows are written directly inside AlertService/CaseService - same immutability guarantee, less ceremony |
| JWT auth | Time | HTTP Basic + Spring Security roles gives the same server-side RBAC enforcement for the demo |
| CSV quoting/escaping (embedded commas) | Time | Plain comma-split parser; fine for the synthetic seed data provided |

Case management and bulk CSV ingestion were originally on this cut list but
have since been implemented in full (see sections above).

See `SENTINEL_AML_DESIGN.md` (one level up) for the full original design.

## Tests

```bash
mvn test
```

Unit tests cover every rule's true-positive, true-negative, and boundary
cases (e.g., exactly-at-threshold, just-below-threshold, outside-time-window,
no-baseline-history) without needing a Spring context. All 19 pass.

## Bugs found & fixed during first real build/run

Building and actually running this (rather than just reading the code) turned
up three real issues, kept here for transparency:

1. **Behavioral deviation over-sensitivity.** The rule averaged a customer's
   baseline spend across the *entire* lookback window (90 calendar days),
   including days with zero activity. That silently drags the average way
   down for anyone who doesn't transact literally every day (almost
   everyone), so a totally normal transaction would look like a huge
   multiple of a tiny, artificial baseline — a false-positive generator
   hiding behind a passing-looking implementation. Fixed to average over the
   customer's actual *active* days instead. Caught by
   `BehavioralDeviationRuleTest`.
2. **`LazyInitializationException` on the bootstrap replay and every
   read-only DTO mapping path.** `spring.jpa.open-in-view` is intentionally
   off (good practice), but several `@ManyToOne` associations (`Account.
   customer`, `Transaction.account`, `Alert.customer/account/amlCase`,
   `AmlCase.customer`) were `LAZY`, and were being accessed *after* the
   owning repository call's transaction had already closed - the classic
   detached-entity trap. Given how small and shallow this entity graph is,
   switched those associations to `EAGER` rather than retrofitting
   `@Transactional` onto every controller/read path. Only found by actually
   starting the app, not by unit tests.
3. **Seed data crossing its own thresholds.** The "clean noise" customers
   (meant to prove zero false positives) had payroll credits of 45k/50k INR
   against a 10k CTR threshold, so they tripped `LARGE_SINGLE_TXN` on their
   own seed data — undermining the exact claim that data was there to prove.
   Lowered those amounts to comfortably sit under every threshold.
