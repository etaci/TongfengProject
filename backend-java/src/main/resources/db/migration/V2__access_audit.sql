CREATE TABLE access_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    audit_code VARCHAR(64) NOT NULL UNIQUE,
    actor_user_code VARCHAR(64) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    patient_user_code VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    binding_code VARCHAR(64) NULL,
    session_code VARCHAR(64) NULL,
    reason_text VARCHAR(500) NOT NULL,
    operated_at DATETIME NOT NULL,
    KEY idx_access_audit_actor (actor_user_code, operated_at),
    KEY idx_access_audit_patient (patient_user_code, operated_at),
    KEY idx_access_audit_resource (resource_type, resource_id, operated_at)
);
