CREATE TABLE IF NOT EXISTS mvp_usage_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    source_page VARCHAR(32) NOT NULL,
    event_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    payload_json VARCHAR(4000) NOT NULL,
    KEY idx_mvp_usage_event_date (event_date),
    KEY idx_mvp_usage_event_type_date (event_type, event_date)
);

ALTER TABLE mvp_usage_event MODIFY COLUMN payload_json VARCHAR(4000) NOT NULL;

CREATE TABLE clinical_decision_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decision_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    decision_type VARCHAR(64) NOT NULL,
    decision_result VARCHAR(32) NOT NULL,
    rule_version VARCHAR(64) NOT NULL,
    source_references_json VARCHAR(4000) NOT NULL,
    input_snapshot_json VARCHAR(4000) NOT NULL,
    output_summary VARCHAR(1000) NOT NULL,
    generated_at DATETIME NOT NULL,
    manual_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    manual_reviewed_at DATETIME NULL,
    KEY idx_clinical_decision_user (user_code, generated_at),
    KEY idx_clinical_decision_type (decision_type, generated_at)
);
