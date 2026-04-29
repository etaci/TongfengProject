package com.tongfeng.backend.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "growth_profile", indexes = {
		@Index(name = "idx_growth_profile_user_code", columnList = "userCode", unique = true)
})
public class GrowthProfileEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String userCode;

	@Column(nullable = false)
	private Integer levelNo;

	@Column(nullable = false)
	private Integer totalPoints;

	@Column(nullable = false)
	private Integer redeemedPoints;

	@Column(nullable = false)
	private Integer currentStreakDays;

	@Column(nullable = false)
	private Integer longestStreakDays;

	private LocalDate lastActiveDate;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public Integer getLevelNo() {
		return levelNo;
	}

	public void setLevelNo(Integer levelNo) {
		this.levelNo = levelNo;
	}

	public Integer getTotalPoints() {
		return totalPoints;
	}

	public void setTotalPoints(Integer totalPoints) {
		this.totalPoints = totalPoints;
	}

	public Integer getRedeemedPoints() {
		return redeemedPoints;
	}

	public void setRedeemedPoints(Integer redeemedPoints) {
		this.redeemedPoints = redeemedPoints;
	}

	public Integer getCurrentStreakDays() {
		return currentStreakDays;
	}

	public void setCurrentStreakDays(Integer currentStreakDays) {
		this.currentStreakDays = currentStreakDays;
	}

	public Integer getLongestStreakDays() {
		return longestStreakDays;
	}

	public void setLongestStreakDays(Integer longestStreakDays) {
		this.longestStreakDays = longestStreakDays;
	}

	public LocalDate getLastActiveDate() {
		return lastActiveDate;
	}

	public void setLastActiveDate(LocalDate lastActiveDate) {
		this.lastActiveDate = lastActiveDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
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
