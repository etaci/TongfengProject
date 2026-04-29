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
@Table(name = "weather_daily_snapshot", indexes = {
		@Index(name = "idx_weather_daily_snapshot_code", columnList = "snapshotCode", unique = true),
		@Index(name = "idx_weather_daily_snapshot_user_date", columnList = "userCode,summaryDate", unique = true)
})
public class WeatherDailySnapshotEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String snapshotCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false)
	private LocalDate summaryDate;

	@Column(nullable = false, length = 128)
	private String cityName;

	@Column(length = 8)
	private String countryCode;

	private Double latitude;

	private Double longitude;

	@Column(length = 64)
	private String timezoneId;

	@Column(precision = 6, scale = 2)
	private BigDecimal temperatureC;

	@Column(precision = 6, scale = 2)
	private BigDecimal apparentTemperatureC;

	private Integer relativeHumidity;

	private Integer precipitationProbability;

	private Integer weatherCode;

	@Column(nullable = false, length = 32)
	private String riskLevel;

	@Column(nullable = false, length = 32)
	private String sourceType;

	@Column(nullable = false, length = 128)
	private String weatherText;

	@Column(nullable = false, length = 500)
	private String summaryText;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getSnapshotCode() {
		return snapshotCode;
	}

	public void setSnapshotCode(String snapshotCode) {
		this.snapshotCode = snapshotCode;
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

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
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

	public BigDecimal getTemperatureC() {
		return temperatureC;
	}

	public void setTemperatureC(BigDecimal temperatureC) {
		this.temperatureC = temperatureC;
	}

	public BigDecimal getApparentTemperatureC() {
		return apparentTemperatureC;
	}

	public void setApparentTemperatureC(BigDecimal apparentTemperatureC) {
		this.apparentTemperatureC = apparentTemperatureC;
	}

	public Integer getRelativeHumidity() {
		return relativeHumidity;
	}

	public void setRelativeHumidity(Integer relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}

	public Integer getPrecipitationProbability() {
		return precipitationProbability;
	}

	public void setPrecipitationProbability(Integer precipitationProbability) {
		this.precipitationProbability = precipitationProbability;
	}

	public Integer getWeatherCode() {
		return weatherCode;
	}

	public void setWeatherCode(Integer weatherCode) {
		this.weatherCode = weatherCode;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(String riskLevel) {
		this.riskLevel = riskLevel;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getWeatherText() {
		return weatherText;
	}

	public void setWeatherText(String weatherText) {
		this.weatherText = weatherText;
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
