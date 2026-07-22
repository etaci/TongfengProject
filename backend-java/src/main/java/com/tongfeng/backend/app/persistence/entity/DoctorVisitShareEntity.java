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
@Table(name = "doctor_visit_share", indexes = {
		@Index(name = "idx_doctor_visit_share_code", columnList = "shareCode", unique = true),
		@Index(name = "idx_doctor_visit_share_token", columnList = "tokenHash", unique = true),
		@Index(name = "idx_doctor_visit_share_user", columnList = "userCode,createdAt")
})
public class DoctorVisitShareEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false, length = 64, unique = true)
	private String shareCode;
	@Column(nullable = false, length = 64)
	private String userCode;
	@Column(nullable = false, length = 64, unique = true)
	private String tokenHash;
	@Column(nullable = false)
	private int lookbackDays;
	@Column(nullable = false)
	private Instant expiresAt;
	@Column(nullable = false)
	private boolean revoked;
	private Instant revokedAt;
	@Column(nullable = false)
	private Instant createdAt;

	public String getShareCode() { return shareCode; }
	public void setShareCode(String shareCode) { this.shareCode = shareCode; }
	public String getUserCode() { return userCode; }
	public void setUserCode(String userCode) { this.userCode = userCode; }
	public String getTokenHash() { return tokenHash; }
	public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
	public int getLookbackDays() { return lookbackDays; }
	public void setLookbackDays(int lookbackDays) { this.lookbackDays = lookbackDays; }
	public Instant getExpiresAt() { return expiresAt; }
	public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
	public boolean isRevoked() { return revoked; }
	public void setRevoked(boolean revoked) { this.revoked = revoked; }
	public Instant getRevokedAt() { return revokedAt; }
	public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
