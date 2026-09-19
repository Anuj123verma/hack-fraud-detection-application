-- Case management audit trail (append-only, mirrors alert_audit_log).
CREATE TABLE case_audit_log (
    id           UUID PRIMARY KEY,
    case_id      UUID NOT NULL REFERENCES aml_case(id),
    actor        VARCHAR(100) NOT NULL,
    action       VARCHAR(50) NOT NULL,
    old_status   VARCHAR(20),
    new_status   VARCHAR(20),
    reason       VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
