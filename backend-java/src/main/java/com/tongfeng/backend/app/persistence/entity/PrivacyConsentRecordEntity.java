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
@Table(name = "privacy_consent_record", indexes = {
		@Index(name = "idx_privacy_consent_code", columnList = "consentCode", unique = true),
		@Index(name = "idx_privacy_consent_user_effective", columnList = "userCode,effectiveAt")
})
public class PrivacyConsentRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String consentCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 32)
	private String consentVersion;

	@Column(nullable = false, length = 32)
	private String privacyPolicyVersion;

	@Column(nullable = false)
	private boolean privacyAccepted;

	@Column(nullable = false)
	private boolean termsAccepted;

	@Column(nullable = false)
	private boolean medicalDataAuthorized;

	@Column(nullable = false)
	private boolean familyCollaborationAuthorized;

	@Column(nullable = false)
	private boolean notificationAuthorized;

	@Column(nullable = false, length = 32)
	private String sourceType;

	@Column(nullable = false)
	private Instant effectiveAt;

	@Column(nullable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public String getConsentCode() {
		return consentCode;
	}

	public void setConsentCode(String consentCode) {
		this.consentCode = consentCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getConsentVersion() {
		return consentVersion;
	}

	public void setConsentVersion(String consentVersion) {
		this.consentVersion = consentVersion;
	}

	public String getPrivacyPolicyVersion() {
		return privacyPolicyVersion;
	}

	public void setPrivacyPolicyVersion(String privacyPolicyVersion) {
		this.privacyPolicyVersion = privacyPolicyVersion;
	}

	public boolean isPrivacyAccepted() {
		return privacyAccepted;
	}

	public void setPrivacyAccepted(boolean privacyAccepted) {
		this.privacyAccepted = privacyAccepted;
	}

	public boolean isTermsAccepted() {
		return termsAccepted;
	}

	public void setTermsAccepted(boolean termsAccepted) {
		this.termsAccepted = termsAccepted;
	}

	public boolean isMedicalDataAuthorized() {
		return medicalDataAuthorized;
	}

	public void setMedicalDataAuthorized(boolean medicalDataAuthorized) {
		this.medicalDataAuthorized = medicalDataAuthorized;
	}

	public boolean isFamilyCollaborationAuthorized() {
		return familyCollaborationAuthorized;
	}

	public void setFamilyCollaborationAuthorized(boolean familyCollaborationAuthorized) {
		this.familyCollaborationAuthorized = familyCollaborationAuthorized;
	}

	public boolean isNotificationAuthorized() {
		return notificationAuthorized;
	}

	public void setNotificationAuthorized(boolean notificationAuthorized) {
		this.notificationAuthorized = notificationAuthorized;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public Instant getEffectiveAt() {
		return effectiveAt;
	}

	public void setEffectiveAt(Instant effectiveAt) {
		this.effectiveAt = effectiveAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
