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
@Table(name = "proactive_care_setting", indexes = {
		@Index(name = "idx_proactive_care_setting_user_code", columnList = "userCode", unique = true),
		@Index(name = "idx_proactive_care_setting_city", columnList = "monitoringCity")
})
public class ProactiveCareSettingEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String userCode;

	@Column(nullable = false, length = 128)
	private String monitoringCity;

	@Column(length = 8)
	private String countryCode;

	@Column(length = 128)
	private String resolvedName;

	private Double latitude;

	private Double longitude;

	@Column(length = 64)
	private String timezoneId;

	@Column(nullable = false)
	private boolean weatherAlertsEnabled;

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

	public String getMonitoringCity() {
		return monitoringCity;
	}

	public void setMonitoringCity(String monitoringCity) {
		this.monitoringCity = monitoringCity;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getResolvedName() {
		return resolvedName;
	}

	public void setResolvedName(String resolvedName) {
		this.resolvedName = resolvedName;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getTimezoneId() {
		return timezoneId;
	}

	public void setTimezoneId(String timezoneId) {
		this.timezoneId = timezoneId;
	}

	public boolean isWeatherAlertsEnabled() {
		return weatherAlertsEnabled;
	}

	public void setWeatherAlertsEnabled(boolean weatherAlertsEnabled) {
		this.weatherAlertsEnabled = weatherAlertsEnabled;
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
