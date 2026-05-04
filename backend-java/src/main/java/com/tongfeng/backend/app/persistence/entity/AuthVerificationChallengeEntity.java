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
@Table(name = "auth_verification_challenge", indexes = {
		@Index(name = "idx_auth_verification_challenge_code", columnList = "challengeCode", unique = true),
		@Index(name = "idx_auth_verification_principal_purpose", columnList = "principalValue,purpose,status")
})
public class AuthVerificationChallengeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String challengeCode;

	@Column(length = 64)
	private String userCode;

	@Column(nullable = false, length = 32)
	private String purpose;

	@Column(nullable = false, length = 32)
	private String accountType;

	@Column(nullable = false, length = 128)
	private String principalValue;

	@Column(nullable = false, length = 128)
	private String maskedTarget;

	@Column(nullable = false, length = 128)
	private String verificationCodeHash;

	@Column(nullable = false, length = 128)
	private String verificationCodeSalt;

	@Column(nullable = false, length = 32)
	private String deliveryChannel;

	@Column(nullable = false, length = 32)
	private String deliveryProvider;

	@Column(nullable = false, length = 32)
	private String deliveryStatus;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column
	private Instant usedAt;

	@Column(nullable = false)
	private int attemptCount;

	@Column
	private Instant lastAttemptAt;

	@Column(nullable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public String getChallengeCode() {
		return challengeCode;
	}

	public void setChallengeCode(String challengeCode) {
		this.challengeCode = challengeCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getPrincipalValue() {
		return principalValue;
	}

	public void setPrincipalValue(String principalValue) {
		this.principalValue = principalValue;
	}

	public String getMaskedTarget() {
		return maskedTarget;
	}

	public void setMaskedTarget(String maskedTarget) {
		this.maskedTarget = maskedTarget;
	}

	public String getVerificationCodeHash() {
		return verificationCodeHash;
	}

	public void setVerificationCodeHash(String verificationCodeHash) {
		this.verificationCodeHash = verificationCodeHash;
	}

	public String getVerificationCodeSalt() {
		return verificationCodeSalt;
	}

	public void setVerificationCodeSalt(String verificationCodeSalt) {
		this.verificationCodeSalt = verificationCodeSalt;
	}

	public String getDeliveryChannel() {
		return deliveryChannel;
	}

	public void setDeliveryChannel(String deliveryChannel) {
		this.deliveryChannel = deliveryChannel;
	}

	public String getDeliveryStatus() {
		return deliveryStatus;
	}

	public void setDeliveryStatus(String deliveryStatus) {
		this.deliveryStatus = deliveryStatus;
	}

	public String getDeliveryProvider() {
		return deliveryProvider;
	}

	public void setDeliveryProvider(String deliveryProvider) {
		this.deliveryProvider = deliveryProvider;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public void setUsedAt(Instant usedAt) {
		this.usedAt = usedAt;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}

	public Instant getLastAttemptAt() {
		return lastAttemptAt;
	}

	public void setLastAttemptAt(Instant lastAttemptAt) {
		this.lastAttemptAt = lastAttemptAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
