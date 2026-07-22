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
@Table(name = "clinical_decision_audit", indexes = {
		@Index(name = "idx_clinical_decision_user", columnList = "userCode,generatedAt"),
		@Index(name = "idx_clinical_decision_type", columnList = "decisionType,generatedAt")
})
public class ClinicalDecisionAuditEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String decisionCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String decisionType;

	@Column(nullable = false, length = 32)
	private String decisionResult;

	@Column(nullable = false, length = 64)
	private String ruleVersion;

	@Column(nullable = false, length = 4000)
	private String sourceReferencesJson;

	@Column(nullable = false, length = 4000)
	private String inputSnapshotJson;

	@Column(nullable = false, length = 1000)
	private String outputSummary;

	@Column(length = 4000)
	private String decisionPayloadJson;

	@Column(nullable = false)
	private Instant generatedAt;

	@Column(nullable = false)
	private boolean manualReviewed;

	private Instant manualReviewedAt;

	public Long getId() {
		return id;
	}

	public String getDecisionCode() {
		return decisionCode;
	}

	public void setDecisionCode(String decisionCode) {
		this.decisionCode = decisionCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getDecisionType() {
		return decisionType;
	}

	public void setDecisionType(String decisionType) {
		this.decisionType = decisionType;
	}

	public String getDecisionResult() {
		return decisionResult;
	}

	public void setDecisionResult(String decisionResult) {
		this.decisionResult = decisionResult;
	}

	public String getRuleVersion() {
		return ruleVersion;
	}

	public void setRuleVersion(String ruleVersion) {
		this.ruleVersion = ruleVersion;
	}

	public String getSourceReferencesJson() {
		return sourceReferencesJson;
	}

	public void setSourceReferencesJson(String sourceReferencesJson) {
		this.sourceReferencesJson = sourceReferencesJson;
	}

	public String getInputSnapshotJson() {
		return inputSnapshotJson;
	}

	public void setInputSnapshotJson(String inputSnapshotJson) {
		this.inputSnapshotJson = inputSnapshotJson;
	}

	public String getOutputSummary() {
		return outputSummary;
	}

	public void setOutputSummary(String outputSummary) {
		this.outputSummary = outputSummary;
	}

	public String getDecisionPayloadJson() {
		return decisionPayloadJson;
	}

	public void setDecisionPayloadJson(String decisionPayloadJson) {
		this.decisionPayloadJson = decisionPayloadJson;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
	}

	public boolean isManualReviewed() {
		return manualReviewed;
	}

	public void setManualReviewed(boolean manualReviewed) {
		this.manualReviewed = manualReviewed;
	}

	public Instant getManualReviewedAt() {
		return manualReviewedAt;
	}

	public void setManualReviewedAt(Instant manualReviewedAt) {
		this.manualReviewedAt = manualReviewedAt;
	}
}
