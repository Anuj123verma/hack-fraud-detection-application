-- Sentinel AML core schema (H2, PostgreSQL compatibility mode)

CREATE TABLE customer (
    id                UUID PRIMARY KEY,
    customer_ref      VARCHAR(50) NOT NULL UNIQUE,
    full_name         VARCHAR(200) NOT NULL,
    id_number         VARCHAR(50) NOT NULL,
    customer_type     VARCHAR(20) NOT NULL,
    base_risk_rating  VARCHAR(10) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account (
    id              UUID PRIMARY KEY,
    customer_id     UUID NOT NULL REFERENCES customer(id),
    account_number  VARCHAR(50) NOT NULL UNIQUE,
    currency        VARCHAR(3) NOT NULL,
    risk_rating     VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transaction (
    id                          UUID PRIMARY KEY,
    external_txn_id             VARCHAR(60) NOT NULL UNIQUE,
    account_id                  UUID NOT NULL REFERENCES account(id),
    direction                   VARCHAR(10) NOT NULL,
    amount                      NUMERIC(18,2) NOT NULL,
    currency                    VARCHAR(3) NOT NULL,
    amount_base                 NUMERIC(18,2) NOT NULL,
    counterparty_name           VARCHAR(200),
    counterparty_jurisdiction   VARCHAR(2),
    channel                     VARCHAR(20) NOT NULL,
    txn_timestamp               TIMESTAMP NOT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_txn_account_time ON transaction(account_id, txn_timestamp);

CREATE TABLE aml_case (
    id            UUID PRIMARY KEY,
    case_number   VARCHAR(30) NOT NULL UNIQUE,
    customer_id   UUID NOT NULL REFERENCES customer(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority      VARCHAR(10) NOT NULL DEFAULT 'LOW',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at     TIMESTAMP
);

CREATE TABLE alert (
    id                   UUID PRIMARY KEY,
    customer_id          UUID NOT NULL REFERENCES customer(id),
    account_id           UUID REFERENCES account(id),
    case_id              UUID REFERENCES aml_case(id),
    risk_score           INT NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'NEW',
    dedup_key            VARCHAR(150) NOT NULL UNIQUE,
    rule_codes           VARCHAR(300) NOT NULL,
    evidence_txn_ids     VARCHAR(2000) NOT NULL,
    explanation          VARCHAR(2000) NOT NULL,
    disposition_reason   VARCHAR(500),
    disposed_by          VARCHAR(100),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disposed_at          TIMESTAMP
);

-- Append-only audit trail. Application layer never issues UPDATE/DELETE against this table.
CREATE TABLE alert_audit_log (
    id           UUID PRIMARY KEY,
    alert_id     UUID NOT NULL REFERENCES alert(id),
    actor        VARCHAR(100) NOT NULL,
    action       VARCHAR(50) NOT NULL,
    old_status   VARCHAR(20),
    new_status   VARCHAR(20),
    reason       VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
