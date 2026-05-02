CREATE TABLE user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    birthday DATE NULL,
    height_cm INT NULL,
    target_uric_acid INT NULL,
    allergies_json JSON NULL,
    comorbidities_json JSON NULL,
    emergency_contact VARCHAR(128) NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_profile_user_code (user_code)
);

CREATE TABLE auth_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    session_code VARCHAR(64) NOT NULL UNIQUE,
    auth_mode VARCHAR(32) NOT NULL,
    account_type VARCHAR(32) NULL,
    account_identifier VARCHAR(128) NULL,
    privacy_consent_completed BIT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    KEY idx_auth_session_code (session_code),
    KEY idx_auth_session_user_code (user_code),
    KEY idx_auth_session_token (token)
);

CREATE TABLE auth_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    identity_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    principal_value VARCHAR(128) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    password_salt VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_auth_identity_user_code (user_code),
    KEY idx_auth_identity_principal_status (principal_value, status)
);

CREATE TABLE privacy_consent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consent_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    consent_version VARCHAR(32) NOT NULL,
    privacy_policy_version VARCHAR(32) NOT NULL,
    privacy_accepted BIT NOT NULL,
    terms_accepted BIT NOT NULL,
    medical_data_authorized BIT NOT NULL,
    family_collaboration_authorized BIT NOT NULL,
    notification_authorized BIT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    effective_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_privacy_consent_user_effective (user_code, effective_at)
);

CREATE TABLE stored_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    relative_path VARCHAR(512) NOT NULL,
    uploaded_at DATETIME NOT NULL,
    KEY idx_file_user_code (user_code)
);

CREATE TABLE meal_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    meal_type VARCHAR(32) NOT NULL,
    taken_at DATETIME NOT NULL,
    note_text VARCHAR(500) NULL,
    file_code VARCHAR(64) NULL,
    risk_level VARCHAR(16) NOT NULL,
    purine_estimate_mg INT NULL,
    items_json JSON NOT NULL,
    suggestions_json JSON NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    KEY idx_meal_user_code (user_code),
    KEY idx_meal_taken_at (taken_at)
);

CREATE TABLE uric_acid_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    ua_value INT NOT NULL,
    ua_unit VARCHAR(32) NOT NULL,
    measured_at DATETIME NOT NULL,
    source_name VARCHAR(64) NULL,
    note_text VARCHAR(500) NULL,
    KEY idx_ua_user_code (user_code),
    KEY idx_ua_measured_at (measured_at)
);

CREATE TABLE weight_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    weight_value DECIMAL(6,2) NOT NULL,
    measured_at DATETIME NOT NULL,
    source_name VARCHAR(64) NULL,
    note_text VARCHAR(500) NULL,
    KEY idx_weight_user_code (user_code)
);

CREATE TABLE blood_pressure_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    systolic_pressure INT NOT NULL,
    diastolic_pressure INT NOT NULL,
    pulse_rate INT NULL,
    unit VARCHAR(32) NOT NULL,
    measured_at DATETIME NOT NULL,
    source_name VARCHAR(64) NULL,
    note_text VARCHAR(500) NULL,
    KEY idx_blood_pressure_user_measured (user_code, measured_at)
);

CREATE TABLE flare_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    joint_name VARCHAR(64) NOT NULL,
    pain_level INT NOT NULL,
    started_at DATETIME NOT NULL,
    duration_note VARCHAR(128) NULL,
    note_text VARCHAR(500) NULL,
    KEY idx_flare_user_code (user_code)
);

CREATE TABLE hydration_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    water_intake_ml INT NOT NULL,
    urine_color_level INT NOT NULL,
    checked_at DATETIME NOT NULL,
    note_text VARCHAR(500) NULL,
    KEY idx_hydration_user_code (user_code)
);

CREATE TABLE lab_report_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    file_code VARCHAR(64) NULL,
    report_date DATE NOT NULL,
    indicators_json JSON NOT NULL,
    overall_risk_level VARCHAR(16) NOT NULL,
    suggestions_json JSON NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    KEY idx_lab_user_code (user_code)
);

CREATE TABLE medication_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    current_medications_json JSON NOT NULL,
    follow_up_note VARCHAR(500) NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE medication_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    checkin_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    medication_name VARCHAR(128) NOT NULL,
    scheduled_period VARCHAR(32) NOT NULL,
    checkin_status VARCHAR(32) NOT NULL,
    note_text VARCHAR(500) NULL,
    checkin_date DATE NOT NULL,
    checkin_at DATETIME NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    KEY idx_medication_checkin_user_date (user_code, checkin_date),
    UNIQUE KEY uk_medication_checkin_daily_slot (user_code, checkin_date, medication_name, scheduled_period)
);

CREATE TABLE reminder_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reminder_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(500) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    trigger_at DATETIME NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    dedup_key VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_reminder_event_user_status (user_code, status),
    KEY idx_reminder_event_user_trigger (user_code, trigger_at)
);

CREATE TABLE daily_health_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    summary_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    summary_date DATE NOT NULL,
    latest_uric_acid_value INT NULL,
    latest_uric_acid_unit VARCHAR(32) NULL,
    latest_weight_value DECIMAL(6,2) NULL,
    total_water_intake_ml INT NULL,
    high_risk_meal_count INT NULL,
    flare_count INT NULL,
    overall_risk_level VARCHAR(16) NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_daily_health_summary_user_date (user_code, summary_date)
);

CREATE TABLE proactive_care_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    monitoring_city VARCHAR(128) NOT NULL,
    country_code VARCHAR(8) NULL,
    resolved_name VARCHAR(128) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    timezone_id VARCHAR(64) NULL,
    weather_alerts_enabled BIT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_proactive_care_setting_city (monitoring_city)
);

CREATE TABLE weather_daily_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    snapshot_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    summary_date DATE NOT NULL,
    city_name VARCHAR(128) NOT NULL,
    country_code VARCHAR(8) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    timezone_id VARCHAR(64) NULL,
    temperature_c DECIMAL(6,2) NULL,
    apparent_temperature_c DECIMAL(6,2) NULL,
    relative_humidity INT NULL,
    precipitation_probability INT NULL,
    weather_code INT NULL,
    risk_level VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    weather_text VARCHAR(128) NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_weather_daily_snapshot_user_date (user_code, summary_date)
);

CREATE TABLE family_invite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invite_code VARCHAR(64) NOT NULL UNIQUE,
    creator_user_code VARCHAR(64) NOT NULL,
    patient_user_code VARCHAR(64) NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    invite_message VARCHAR(200) NULL,
    status VARCHAR(32) NOT NULL,
    caregiver_permission VARCHAR(32) NOT NULL,
    weekly_report_enabled BIT NOT NULL,
    notify_on_high_risk BIT NOT NULL,
    accepted_by_user_code VARCHAR(64) NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_family_invite_creator_status (creator_user_code, status),
    KEY idx_family_invite_patient_status (patient_user_code, status)
);

CREATE TABLE family_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    binding_code VARCHAR(64) NOT NULL UNIQUE,
    patient_user_code VARCHAR(64) NOT NULL,
    caregiver_user_code VARCHAR(64) NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    caregiver_permission VARCHAR(32) NOT NULL,
    weekly_report_enabled BIT NOT NULL,
    notify_on_high_risk BIT NOT NULL,
    source_invite_code VARCHAR(64) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_family_binding_patient_status (patient_user_code, status),
    KEY idx_family_binding_caregiver_status (caregiver_user_code, status)
);

CREATE TABLE family_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_code VARCHAR(64) NOT NULL UNIQUE,
    binding_code VARCHAR(64) NOT NULL,
    patient_user_code VARCHAR(64) NOT NULL,
    caregiver_user_code VARCHAR(64) NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,
    created_by_user_code VARCHAR(64) NULL,
    due_at DATETIME NULL,
    completed_at DATETIME NULL,
    completion_note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_family_task_patient_created (patient_user_code, created_at),
    KEY idx_family_task_caregiver_created (caregiver_user_code, created_at),
    KEY idx_family_task_binding_status (binding_code, status)
);

CREATE TABLE device_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    device_type VARCHAR(32) NOT NULL,
    vendor_name VARCHAR(64) NOT NULL,
    device_model VARCHAR(64) NULL,
    serial_number VARCHAR(128) NOT NULL,
    alias_name VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    last_synced_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_device_binding_user_status (user_code, status),
    KEY idx_device_binding_serial (serial_number)
);

CREATE TABLE device_sync_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    device_code VARCHAR(64) NOT NULL,
    device_type VARCHAR(32) NOT NULL,
    metric_type VARCHAR(32) NOT NULL,
    external_event_id VARCHAR(128) NOT NULL,
    measured_at DATETIME NOT NULL,
    payload_json VARCHAR(2000) NOT NULL,
    result_record_code VARCHAR(64) NULL,
    sync_status VARCHAR(32) NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_device_sync_event_user_measured (user_code, measured_at),
    UNIQUE KEY uk_device_sync_event_device_external (device_code, external_event_id)
);

CREATE TABLE growth_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    level_no INT NOT NULL,
    total_points INT NOT NULL,
    redeemed_points INT NOT NULL,
    current_streak_days INT NOT NULL,
    longest_streak_days INT NOT NULL,
    last_active_date DATE NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_growth_profile_user_code (user_code)
);

CREATE TABLE growth_point_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    point_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    dedup_key VARCHAR(128) NOT NULL,
    business_code VARCHAR(64) NULL,
    points INT NOT NULL,
    awarded_date DATE NOT NULL,
    summary_text VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_growth_point_log_user_date (user_code, awarded_date),
    UNIQUE KEY uk_growth_point_log_user_dedup (user_code, dedup_key)
);

CREATE TABLE growth_badge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    award_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    badge_key VARCHAR(64) NOT NULL,
    badge_name VARCHAR(64) NOT NULL,
    badge_description VARCHAR(200) NOT NULL,
    awarded_at DATETIME NOT NULL,
    UNIQUE KEY uk_growth_badge_user_badge (user_code, badge_key)
);

CREATE TABLE growth_reward_claim (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    claim_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    reward_key VARCHAR(64) NOT NULL,
    reward_name VARCHAR(64) NOT NULL,
    reward_type VARCHAR(32) NOT NULL,
    points_cost INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    claim_note VARCHAR(200) NOT NULL,
    claimed_at DATETIME NOT NULL,
    KEY idx_growth_reward_claim_user_claimed (user_code, claimed_at),
    KEY idx_growth_reward_claim_user_reward (user_code, reward_key)
);

CREATE TABLE health_record_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    audit_code VARCHAR(64) NOT NULL UNIQUE,
    user_code VARCHAR(64) NOT NULL,
    record_id VARCHAR(64) NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    change_reason VARCHAR(200) NOT NULL,
    summary_text VARCHAR(500) NOT NULL,
    field_diffs_json VARCHAR(4000) NOT NULL,
    before_snapshot_json VARCHAR(4000) NULL,
    after_snapshot_json VARCHAR(4000) NULL,
    operated_at DATETIME NOT NULL,
    KEY idx_health_record_audit_record (user_code, record_type, record_id, operated_at)
);
