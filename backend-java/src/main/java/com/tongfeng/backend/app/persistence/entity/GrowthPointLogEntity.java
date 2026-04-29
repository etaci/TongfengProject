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
@Table(name = "growth_point_log", indexes = {
		@Index(name = "idx_growth_point_log_point_code", columnList = "pointCode", unique = true),
		@Index(name = "idx_growth_point_log_user_date", columnList = "userCode,awardedDate"),
		@Index(name = "idx_growth_point_log_user_dedup", columnList = "userCode,dedupKey", unique = true)
})
public class GrowthPointLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String pointCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String actionType;

	@Column(nullable = false, length = 128)
	private String dedupKey;

	@Column(length = 64)
	private String businessCode;

	@Column(nullable = false)
	private Integer points;

	@Column(nullable = false)
	private LocalDate awardedDate;

	@Column(nullable = false, length = 200)
	private String summaryText;

	@Column(nullable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public String getPointCode() {
		return pointCode;
	}

	public void setPointCode(String pointCode) {
		this.pointCode = pointCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getDedupKey() {
		return dedupKey;
	}

	public void setDedupKey(String dedupKey) {
		this.dedupKey = dedupKey;
	}

	public String getBusinessCode() {
		return businessCode;
	}

	public void setBusinessCode(String businessCode) {
		this.businessCode = businessCode;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public LocalDate getAwardedDate() {
		return awardedDate;
	}

	public void setAwardedDate(LocalDate awardedDate) {
		this.awardedDate = awardedDate;
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
}
