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
@Table(name = "growth_badge", indexes = {
		@Index(name = "idx_growth_badge_award_code", columnList = "awardCode", unique = true),
		@Index(name = "idx_growth_badge_user_badge", columnList = "userCode,badgeKey", unique = true)
})
public class GrowthBadgeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String awardCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String badgeKey;

	@Column(nullable = false, length = 64)
	private String badgeName;

	@Column(nullable = false, length = 200)
	private String badgeDescription;

	@Column(nullable = false)
	private Instant awardedAt;

	public Long getId() {
		return id;
	}

	public String getAwardCode() {
		return awardCode;
	}

	public void setAwardCode(String awardCode) {
		this.awardCode = awardCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getBadgeKey() {
		return badgeKey;
	}

	public void setBadgeKey(String badgeKey) {
		this.badgeKey = badgeKey;
	}

	public String getBadgeName() {
		return badgeName;
	}

	public void setBadgeName(String badgeName) {
		this.badgeName = badgeName;
	}

	public String getBadgeDescription() {
		return badgeDescription;
	}

	public void setBadgeDescription(String badgeDescription) {
		this.badgeDescription = badgeDescription;
	}

	public Instant getAwardedAt() {
		return awardedAt;
	}

	public void setAwardedAt(Instant awardedAt) {
		this.awardedAt = awardedAt;
	}
}
