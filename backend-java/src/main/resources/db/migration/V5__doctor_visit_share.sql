ALTER TABLE privacy_consent_record ADD COLUMN change_reason VARCHAR(200) NULL;
ALTER TABLE clinical_decision_audit ADD COLUMN decision_payload_json VARCHAR(4000) NULL;

CREATE TABLE doctor_visit_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    lookback_days INT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    KEY idx_doctor_visit_share_user (user_code, created_at)
);
