package com.tongfeng.backend.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "health_record_audit", indexes = {
		@Index(name = "idx_health_record_audit_code", columnList = "auditCode", unique = true),
		@Index(name = "idx_health_record_audit_record", columnList = "userCode,recordType,recordId,operatedAt")
})
public class HealthRecordAuditEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String auditCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String recordId;

	@Column(nullable = false, length = 32)
	private String recordType;

	@Column(nullable = false, length = 32)
	private String actionType;

	@Column(nullable = false, length = 200)
	private String changeReason;

	@Column(nullable = false, length = 500)
	private String summaryText;

	@Column(nullable = false, length = 4000)
	private String fieldDiffsJson;

	@Column(length = 4000)
	private String beforeSnapshotJson;

	@Column(length = 4000)
	private String afterSnapshotJson;

	@Column(nullable = false)
	private Instant operatedAt;

	public Long getId() {
		return id;
	}

	public String getAuditCode() {
		return auditCode;
	}

	public void setAuditCode(String auditCode) {
		this.auditCode = auditCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public String getRecordType() {
		return recordType;
	}

	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public void setChangeReason(String changeReason) {
		this.changeReason = changeReason;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	public String getFieldDiffsJson() {
		return fieldDiffsJson;
	}

	public void setFieldDiffsJson(String fieldDiffsJson) {
		this.fieldDiffsJson = fieldDiffsJson;
	}

	public String getBeforeSnapshotJson() {
		return beforeSnapshotJson;
	}

	public void setBeforeSnapshotJson(String beforeSnapshotJson) {
		this.beforeSnapshotJson = beforeSnapshotJson;
	}

	public String getAfterSnapshotJson() {
		return afterSnapshotJson;
	}

	public void setAfterSnapshotJson(String afterSnapshotJson) {
		this.afterSnapshotJson = afterSnapshotJson;
	}

	public Instant getOperatedAt() {
		return operatedAt;
	}

	public void setOperatedAt(Instant operatedAt) {
		this.operatedAt = operatedAt;
	}
}
