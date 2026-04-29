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
@Table(name = "growth_reward_claim", indexes = {
		@Index(name = "idx_growth_reward_claim_claim_code", columnList = "claimCode", unique = true),
		@Index(name = "idx_growth_reward_claim_user_claimed", columnList = "userCode,claimedAt"),
		@Index(name = "idx_growth_reward_claim_user_reward", columnList = "userCode,rewardKey")
})
public class GrowthRewardClaimEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String claimCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String rewardKey;

	@Column(nullable = false, length = 64)
	private String rewardName;

	@Column(nullable = false, length = 32)
	private String rewardType;

	@Column(nullable = false)
	private Integer pointsCost;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(nullable = false, length = 200)
	private String claimNote;

	@Column(nullable = false)
	private Instant claimedAt;

	public Long getId() {
		return id;
	}

	public String getClaimCode() {
		return claimCode;
	}

	public void setClaimCode(String claimCode) {
		this.claimCode = claimCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getRewardKey() {
		return rewardKey;
	}

	public void setRewardKey(String rewardKey) {
		this.rewardKey = rewardKey;
	}

	public String getRewardName() {
		return rewardName;
	}

	public void setRewardName(String rewardName) {
		this.rewardName = rewardName;
	}

	public String getRewardType() {
		return rewardType;
	}

	public void setRewardType(String rewardType) {
		this.rewardType = rewardType;
	}

	public Integer getPointsCost() {
		return pointsCost;
	}

	public void setPointsCost(Integer pointsCost) {
		this.pointsCost = pointsCost;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getClaimNote() {
		return claimNote;
	}

	public void setClaimNote(String claimNote) {
		this.claimNote = claimNote;
	}

	public Instant getClaimedAt() {
		return claimedAt;
	}

	public void setClaimedAt(Instant claimedAt) {
		this.claimedAt = claimedAt;
	}
}
