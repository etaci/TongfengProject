package com.tongfeng.backend.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_health_summary", indexes = {
		@Index(name = "idx_daily_health_summary_code", columnList = "summaryCode", unique = true),
		@Index(name = "idx_daily_health_summary_user_date", columnList = "userCode,summaryDate", unique = true)
})
public class DailyHealthSummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String summaryCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false)
	private LocalDate summaryDate;

	private Integer latestUricAcidValue;

	@Column(length = 32)
	private String latestUricAcidUnit;

	@Column(precision = 6, scale = 2)
	private BigDecimal latestWeightValue;

	private Integer totalWaterIntakeMl;

	private Integer highRiskMealCount;

	private Integer flareCount;

	@Column(nullable = false, length = 16)
	private String overallRiskLevel;

	@Column(nullable = false, length = 500)
	private String summaryText;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getSummaryCode() {
		return summaryCode;
	}

	public void setSummaryCode(String summaryCode) {
		this.summaryCode = summaryCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public LocalDate getSummaryDate() {
		return summaryDate;
	}

	public void setSummaryDate(LocalDate summaryDate) {
		this.summaryDate = summaryDate;
	}

	public Integer getLatestUricAcidValue() {
		return latestUricAcidValue;
	}

	public void setLatestUricAcidValue(Integer latestUricAcidValue) {
		this.latestUricAcidValue = latestUricAcidValue;
	}

	public String getLatestUricAcidUnit() {
		return latestUricAcidUnit;
	}

	public void setLatestUricAcidUnit(String latestUricAcidUnit) {
		this.latestUricAcidUnit = latestUricAcidUnit;
	}

	public BigDecimal getLatestWeightValue() {
		return latestWeightValue;
	}

	public void setLatestWeightValue(BigDecimal latestWeightValue) {
		this.latestWeightValue = latestWeightValue;
	}

	public Integer getTotalWaterIntakeMl() {
		return totalWaterIntakeMl;
	}

	public void setTotalWaterIntakeMl(Integer totalWaterIntakeMl) {
		this.totalWaterIntakeMl = totalWaterIntakeMl;
	}

	public Integer getHighRiskMealCount() {
		return highRiskMealCount;
	}

	public void setHighRiskMealCount(Integer highRiskMealCount) {
		this.highRiskMealCount = highRiskMealCount;
	}

	public Integer getFlareCount() {
		return flareCount;
	}

	public void setFlareCount(Integer flareCount) {
		this.flareCount = flareCount;
	}

	public String getOverallRiskLevel() {
		return overallRiskLevel;
	}

	public void setOverallRiskLevel(String overallRiskLevel) {
		this.overallRiskLevel = overallRiskLevel;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
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
