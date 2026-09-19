# Sentinel AML — Low-Level Design (LLD)

> Every detail below is taken directly from the actual source in `sentinel-aml/src`,
> not from the pre-build plan. Where a design decision changed during implementation
> (and it did, three times — see §9), that's called out explicitly.

## 1. Package Structure (actual)

```
com.meridiantrust.sentinel
├── SentinelAmlApplication.java        (Spring Boot entrypoint)
├── DetectionBootstrapRunner.java      (replays seed data through detection at startup)
├── config/
│   ├── AmlProperties.java             (typed @ConfigurationProperties for aml.* in yml)
│   ├── OpenApiConfig.java             (Swagger metadata + HTTP Basic security scheme)
│   └── SecurityConfig.java            (in-memory users, filter chain, @EnableMethodSecurity)
├── controller/                        (6 REST controllers — see §6 for full endpoint table)
├── domain/                            (7 JPA entities + 2 enums — see §2 for ERD)
├── dto/                               (13 request/response records)
├── exception/                         (GlobalExceptionHandler + ApiError)
├── repository/                        (7 Spring Data JPA repositories)
├── rules/                             (AmlRule strategy interface + 5 rules + RiskScorer)
├── security/                          (PiiMasker)
└── service/                           (6 services — orchestration, no controller has business logic)
```

## 2. Entity-Relationship Diagram

```
customer 1───* account 1───* transaction
   │                              
   │ 1                            
   ▼ *                            
 alert *───1 aml_case             
   │                              │
   ▼ 1..*                         ▼ 1..*
alert_audit_log             case_audit_log
```

Exact tables (from `V1__init_schema.sql` + `V3__case_audit_log.sql`):

| Table | Key columns |
|---|---|
| `customer` | `id (UUID PK)`, `customer_ref (unique)`, `full_name`, `id_number`, `customer_type`, `base_risk_rating` |
| `account` | `id`, `customer_id (FK)`, `account_number (unique)`, `currency`, `risk_rating`, `status` |
| `transaction` | `id`, `external_txn_id (unique)`, `account_id (FK)`, `direction`, `amount`, `currency`, `amount_base`, `counterparty_name`, `counterparty_jurisdiction`, `channel`, `txn_timestamp` — indexed on `(account_id, txn_timestamp)` for fast history scans |
| `alert` | `id`, `customer_id (FK)`, `account_id (FK, nullable)`, `case_id (FK, nullable)`, `risk_score`, `status`, `dedup_key (unique)`, `rule_codes`, `evidence_txn_ids`, `explanation`, `disposition_reason`, `disposed_by`, `disposed_at` |
| `aml_case` | `id`, `case_number (unique)`, `customer_id (FK)`, `status`, `priority`, `closed_at` |
| `alert_audit_log` | `id`, `alert_id (FK)`, `actor`, `action`, `old_status`, `new_status`, `reason`, `created_at` — **append-only**, no UPDATE/DELETE ever issued by the app |
| `case_audit_log` | mirrors `alert_audit_log`, scoped to `case_id` |

### JPA fetch-type note (a real bug fix, kept here as design record)
`Account.customer`, `Transaction.account`, `Alert.customer/account/amlCase`, and
`AmlCase.customer` are declared **`EAGER`**, not the more "correct-looking" `LAZY`.
This was a deliberate fix: `spring.jpa.open-in-view=false` is set (good practice —
no session-per-request hack), which means any `LAZY` association touched *after*
a repository call returns (e.g., inside a controller's DTO-mapping method) throws
`LazyInitializationException`. Given how small and shallow this entity graph is,
`EAGER` was the pragmatic choice over retrofitting `@Transactional` onto every
read path. Found by actually running the app, not by unit tests.

## 3. Rule Engine Internals

### 3.1 Strategy interface
```java
public interface AmlRule {
    RuleResult evaluate(RuleContext context);
}
```
`RuleContext` carries: the current transaction, its account, its customer, the
full account transaction history (ascending by time), and the full customer
transaction history across all their accounts (ascending by time). This lets
every rule reason about "this transaction plus everything before it" without
each rule re-querying the database.

`RuleResult` carries: `triggered` (bool), `ruleCode`, `contributionScore` (int),
`explanation` (human-readable string), `evidenceTxnIds` (list of UUIDs).

### 3.2 Orchestration — `DetectionEngineService.evaluate(Transaction)`
1. Load `account` + `customer` off the transaction.
2. Fetch `accountHistory` and `customerHistory` (ascending by timestamp).
3. Build one `RuleContext`.
4. Run **all 5 rules** against it, keep only the ones that fired.
5. If any fired: log it, call `AlertService.recordDetectionResult(...)`.

This single method is called from three places — one code path, per DRY:
- `TransactionIngestService.ingest()` (streaming, single transaction)
- `IngestionService.ingestTransactions()` (bulk CSV, looped in chronological order)
- `DetectionBootstrapRunner.run()` (startup replay of seed data — wrapped in one
  `@Transactional` spanning the whole loop so lazy-loaded associations on each
  transaction stay attached to a live Hibernate session for the entire replay;
  this was also a real bug fix — see §9.2)

### 3.3 The five rules (exact config from `application.yml`)

| # | Rule code | Weight | Config | Logic |
|---|---|---|---|---|
| 1 | `LARGE_SINGLE_TXN` | 60 | `threshold-base: 10000` | Single transaction's base-currency amount ≥ threshold → always flags (CTR-style, no exceptions) |
| 2 | `STRUCTURING` | 60 (+10/extra txn, capped 100) | `min-amount:9000, max-amount:9999, min-count:3, window-hours:24` | The *triggering* transaction itself must land in the smurfing band [9000,9999]. Then count all transactions on the same account in that band within a rolling 24h window ending at the trigger; ≥3 → fires. Score bumps +10 per transaction beyond the minimum count. |
| 3 | `RAPID_MOVEMENT` | 70 | `min-percent-out:0.8, window-hours:48` | Only evaluated when the current txn is a `DEBIT`. Walks backward through account history for every prior `CREDIT`; for each, sums all debits between that credit and `windowHours` later; if outbound sum ≥ 80% of the credit amount → fires (classic layering pattern) |
| 4 | `HIGH_RISK_JURISDICTION` | 80 | `jurisdictions: IR,KP,SY,CU,MM` | Any transaction whose `counterparty_jurisdiction` is on the list → always flags, amount-independent |
| 5 | `BEHAVIORAL_DEVIATION` | 50 | `multiplier:3.0, lookback-days:90` | Compares today's total transaction value against the customer's historical average **per active day** (see §9.1 for why "active day" and not "calendar day"); fires if today ≥ 3× that average. Returns `notTriggered` if there's no baseline history at all (protects new customers from false positives). |

### 3.4 Risk scoring
```java
RiskScorer.combine(triggeredResults) = min(100, sum(each result's contributionScore))
```
Pure function, no side effects, unit-tested directly (`RiskScorerTest`). This is
why one transaction hitting 2 rules (e.g. `HIGH_RISK_JURISDICTION` (80) +
`LARGE_SINGLE_TXN` (60)) caps at 100, not 140 — verified live (several seeded
alerts sit exactly at risk score 100).

## 4. Alert Deduplication & Aggregation Algorithm

```
dedupKey = customerId + "|" + accountId + "|" + sortedDistinctRuleCodes + "|" + calendarDate(txn)
```
- **Same key already exists** → merge: `riskScore = max(old, new)`, evidence
  transaction IDs unioned (de-duplicated via `LinkedHashSet`), explanations
  concatenated with `" || "` so an analyst can see the full history of why
  this alert kept growing.
- **New key** → new `Alert` row created, `AlertAuditLog` entry `ALERT_CREATED`.
- Different rule-code combinations on the same account/day → **separate**
  alerts by design (e.g., a plain `LARGE_SINGLE_TXN` hit and a
  `LARGE_SINGLE_TXN,RAPID_MOVEMENT` combo hit are treated as distinct
  patterns worth separate review), confirmed against live seed data.

## 5. Case Management State Machine

- **Valid statuses**: `OPEN, IN_PROGRESS, ESCALATED, SAR_FILED, CLOSED`
- **Priority bands** (auto-computed from the max risk score across all linked alerts):

  | Risk score | Priority |
  |---|---|
  | ≥ 90 | CRITICAL |
  | ≥ 70 | HIGH |
  | ≥ 40 | MEDIUM |
  | < 40 | LOW |

- **Reason required** to transition into `CLOSED` or `SAR_FILED` — enforced in
  `CaseService.updateStatus()`, rejected with `IllegalArgumentException` →
  HTTP 400 (verified live).
- **Same-customer constraint**: `openCase()` and `linkAlert()` both reject
  alerts belonging to a different customer than the case.
- Case numbers: `CASE-<year>-<zero-padded-sequence>`, e.g. `CASE-2026-000001`.

## 6. REST API Surface (actual, from controllers)

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/v1/alerts` | ANALYST | Alert queue, sorted risk-score desc then recency desc, PII masked |
| GET | `/api/v1/alerts/{id}` | ANALYST | Alert detail; unmasked only if caller has `COMPLIANCE_ADMIN` |
| PATCH | `/api/v1/alerts/{id}/disposition` | ANALYST | Change status; reason required for `CLEARED`/`CLOSED` |
| POST | `/api/v1/cases` | ANALYST | Open a case from one or more alerts (same customer) |
| POST | `/api/v1/cases/{id}/alerts/{alertId}` | ANALYST | Link another alert into an existing case |
| PATCH | `/api/v1/cases/{id}/status` | ANALYST | Transition case status; reason required for `CLOSED`/`SAR_FILED` |
| GET | `/api/v1/cases` | ANALYST | Case queue, sorted priority desc then recency desc |
| GET | `/api/v1/cases/{id}` | ANALYST | Case detail with linked alert IDs |
| POST | `/api/v1/transactions` | ANALYST | Streaming single-transaction ingest + inline detection |
| POST | `/api/v1/ingestion/customers` | COMPLIANCE_ADMIN | Bulk CSV customer load |
| POST | `/api/v1/ingestion/accounts` | COMPLIANCE_ADMIN | Bulk CSV account load |
| POST | `/api/v1/ingestion/transactions` | COMPLIANCE_ADMIN | Bulk CSV transaction load + chronological detection replay |
| GET | `/api/v1/customers` | ANALYST | Customer list, PII masked |
| GET | `/api/v1/customers/{id}/transactions` | ANALYST | Transaction timeline across all of a customer's accounts |
| GET | `/api/v1/admin/rule-config` | COMPLIANCE_ADMIN | Live view of rule thresholds/weights |
| GET | `/api/v1/admin/currency-config` | COMPLIANCE_ADMIN | Live view of base currency + rate table |

All routes except `/`, `/index.html`, `/js/**`, `/css/**`, `/swagger-ui/**`,
`/v3/api-docs/**` require authentication (`SecurityConfig`), verified live via
401 for anonymous and 403 for wrong-role calls.

## 7. PII Masking Algorithm (`PiiMasker`)

- **Name masking**: split on spaces, keep first letter of each word, replace
  the rest with `*` (e.g. `"Sunil Mehta"` → `"S**** M****"`).
- **ID masking**: keep only the last 4 characters, prefix with `****`
  (e.g. `"IN1234569002"` → `"****9002"`); anything under 4 chars → `"****"`.
- Computed server-side in controller DTO-mapping methods, gated on
  `ROLE_COMPLIANCE_ADMIN` presence in `Authentication.getAuthorities()` — the
  frontend never decides whether to mask.

## 8. Currency Normalization

`CurrencyService.toBase(amount, currencyCode)`:
```
amount_base = amount * rates[currencyCode], rounded HALF_UP to 2 decimals
```
Base currency and rate table both live in `application.yml` under `aml.currency`
(default base `INR`; `USD:83.0, EUR:90.0, GBP:105.0`). Every rule reads
`amountBase`, never the raw `amount`/`currency` pair, so thresholds are
currency-agnostic.

## 9. Bugs Found & Fixed During Build/Run (design-relevant, kept for the record)

1. **Behavioral deviation averaging.** Originally divided the baseline total by
   the full 90-*calendar*-day lookback window (including zero-activity days),
   which artificially deflates the average for any customer who doesn't
   transact literally every day — a false-positive generator. Fixed to divide
   by the count of *distinct active days* in the baseline instead. Caught by
   `BehavioralDeviationRuleTest`.
2. **`LazyInitializationException` on startup replay.** See §2's fetch-type
   note — fixed via `EAGER` associations + wrapping `DetectionBootstrapRunner.run()`
   in one `@Transactional`. Only surfaced by actually running the app.
3. **Seed data self-contradiction.** The "clean, zero-alert" demo customers had
   payroll credits (45k/50k INR) that exceeded the 10k `LARGE_SINGLE_TXN`
   threshold, so they tripped their own control group. Lowered those amounts
   in `V2__seed_data.sql` to sit under every threshold.

## 10. Testing Strategy

19 JUnit 5 tests (`src/test/java/.../rules/`), all rule logic tested with plain
POJOs — **no Spring context boot required**, because `RuleContext`/`RuleResult`
are pure data + the rules only depend on `AmlProperties` (a plain object built
directly in tests via `TestFixtures`). Coverage per rule: true-positive,
true-negative, and boundary cases (exactly-at-threshold, just-below-threshold,
outside-time-window, no-baseline-history). `RiskScorerTest` covers the
capped-sum combinator directly.
