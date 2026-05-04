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
@Table(name = "auth_identity", indexes = {
		@Index(name = "idx_auth_identity_code", columnList = "identityCode", unique = true),
		@Index(name = "idx_auth_identity_user_code", columnList = "userCode"),
		@Index(name = "idx_auth_identity_principal_status", columnList = "principalValue,status")
})
public class AuthIdentityEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String identityCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 32)
	private String accountType;

	@Column(nullable = false, length = 128)
	private String principalValue;

	@Column(nullable = false, length = 256)
	private String passwordHash;

	@Column(nullable = false, length = 128)
	private String passwordSalt;

	@Column(nullable = false, length = 32)
	private String status;

	@Column
	private Instant lastLoginAt;

	@Column
	private Instant verifiedAt;

	@Column(nullable = false)
	private int failedLoginCount;

	@Column
	private Instant lastFailedLoginAt;

	@Column
	private Instant lockedUntil;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getIdentityCode() {
		return identityCode;
	}

	public void setIdentityCode(String identityCode) {
		this.identityCode = identityCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
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

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPasswordSalt() {
		return passwordSalt;
	}

	public void setPasswordSalt(String passwordSalt) {
		this.passwordSalt = passwordSalt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public void setVerifiedAt(Instant verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	public int getFailedLoginCount() {
		return failedLoginCount;
	}

	public void setFailedLoginCount(int failedLoginCount) {
		this.failedLoginCount = failedLoginCount;
	}

	public Instant getLastFailedLoginAt() {
		return lastFailedLoginAt;
	}

	public void setLastFailedLoginAt(Instant lastFailedLoginAt) {
		this.lastFailedLoginAt = lastFailedLoginAt;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public void setLockedUntil(Instant lockedUntil) {
		this.lockedUntil = lockedUntil;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
