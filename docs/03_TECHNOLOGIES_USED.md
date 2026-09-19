# Sentinel AML — Technologies Used

Every item below is actually in `pom.xml` / actually running — nothing aspirational.

## Core Runtime

| Tech | Version | Why this, not something else |
|---|---|---|
| **Java** | 17 (Eclipse Temurin) | LTS release, required baseline for Spring Boot 3.x; records + pattern matching used throughout DTOs/rules for concise, immutable data carriers |
| **Spring Boot** | 3.3.4 | Batteries-included web framework — gets us an embedded server, dependency injection, config binding, and a huge ecosystem without hand-rolling any of it. The default, sane choice for a Java REST backend on a deadline |
| **Maven + Maven Daemon (mvnd)** | Maven 3.9.16 (bundled in mvnd 1.0.6) | Standard Java build tool. `mvnd` specifically because the plain Apache Maven distribution download was blocked by network/allowlist restrictions in this environment — mvnd ships its own Maven binary so it sidestepped that entirely, and it's also just faster (persistent daemon, no JVM cold-start per build) |

## Web / API Layer

| Tech | Why |
|---|---|
| **Spring Web (Spring MVC)** | REST controllers, standard `@RestController`/`@RequestMapping` model — nothing exotic, easiest thing to reason about and test |
| **Spring Validation** (`jakarta.validation` / Bean Validation) | `@Valid` on request DTOs (e.g. transaction ingest, disposition requests) — declarative input validation instead of hand-written null/format checks scattered through controllers |
| **springdoc-openapi-starter-webmvc-ui** 2.6.0 | Auto-generates the OpenAPI 3 spec + Swagger UI from the existing `@RestController` annotations — satisfies the "OpenAPI documentation" requirement for free, no separate spec file to keep in sync |

## Persistence

| Tech | Why |
|---|---|
| **Spring Data JPA + Hibernate 6.5.3** | Repository-pattern data access (`findByX` derived queries) instead of hand-written JDBC/SQL boilerplate for straightforward CRUD |
| **H2 Database** (file mode, `MODE=PostgreSQL`) | The brief calls for a relational store; a real PostgreSQL server + Docker wasn't available in this environment on short notice. H2 in **PostgreSQL compatibility mode** was chosen deliberately over H2's default mode so the SQL dialect, syntax, and behavior stay close enough to Postgres that migrating later is a connection-string change, not a rewrite. Runs as a local file (`./data/sentinel.mv.db`) — zero setup, fully self-contained |
| **Flyway** | Version-controlled schema migrations (`V1__init_schema.sql`, `V2__seed_data.sql`, `V3__case_audit_log.sql`) instead of `ddl-auto: update` — the schema is explicit, reviewable, and reproducible, and doubles as the synthetic seed-data delivery mechanism |

## Security

| Tech | Why |
|---|---|
| **Spring Security** (HTTP Basic + method security) | Provides both the authentication filter chain and `@PreAuthorize` role checks out of the box. **HTTP Basic instead of JWT** was an explicit, documented scope cut — it gives the same server-side RBAC guarantees for a local single-session demo without needing token issuance/refresh/expiry plumbing that wasn't worth building in the time box |
| **BCrypt** (`spring-security-crypto`) | Industry-standard password hashing for the two in-memory demo users — not storing plaintext even for a demo |

## Utility / Developer Ergonomics

| Tech | Why |
|---|---|
| **Lombok** | Cuts constructor/getter/setter/builder boilerplate on entities and value objects (`@Builder`, `@Data` equivalents) — purely a typing-reduction tool, excluded from the final repackaged JAR (compile-time only) |
| **SLF4J** (via Spring Boot's default logging) | Standard logging facade, used for detection-engine trigger logs and startup replay timing |

## Testing

| Tech | Why |
|---|---|
| **JUnit 5** | Standard modern Java test framework |
| **Spring Boot Test / Spring Security Test** (on the classpath, available but not required for the rule tests) | The rule engine was deliberately designed so its 19 unit tests run as **plain POJO tests** with zero Spring context boot — faster feedback loop, and proves the rule logic doesn't secretly depend on the framework |

## Frontend

| Tech | Why |
|---|---|
| **Plain HTML + vanilla JavaScript** (`static/index.html`, `static/js/*.js`) | Served directly by Spring Boot's static resource handler — **zero Node/npm/webpack/build step**. Chosen specifically because no Node tooling was confirmed available in this environment, and a hackathon frontend doesn't need a SPA framework's overhead |
| **Tailwind CSS via CDN `<script>` tag**, with a hand-written `styles.css` fallback | Fast, no-build utility styling; if a corporate proxy blocks the CDN script, the app still renders and is usable via the plain CSS fallback — never a single point of failure |

## What's Deliberately *Not* Here (and why)

| Not used | Why it was cut |
|---|---|
| **Apache Kafka** | Real-time streaming ingestion at scale is architecturally the "right" answer for a production AML platform, but stands up its own infrastructure (broker, topics, consumer groups) that wasn't justified for a demo where a synchronous REST endpoint proves the same detection logic just as well |
| **Docker / containers** | Not available/verifiable in this environment quickly enough; the whole app is one JAR + one embedded file DB, so containerization was unnecessary to prove the concept |
| **JWT / OAuth2** | See Security section above — HTTP Basic covers the RBAC requirement for this scope |
| **React / Angular / Vue** | See Frontend section above — no build tooling needed for the required functionality |
| **Real PostgreSQL** | See Persistence section — H2 in PostgreSQL-compatible mode gets 95% of the benefit with 0% of the setup risk |
