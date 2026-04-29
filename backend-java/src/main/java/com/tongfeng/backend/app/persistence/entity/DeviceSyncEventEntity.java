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
@Table(name = "device_sync_event", indexes = {
		@Index(name = "idx_device_sync_event_code", columnList = "syncCode", unique = true),
		@Index(name = "idx_device_sync_event_user_measured", columnList = "userCode,measuredAt"),
		@Index(name = "idx_device_sync_event_device_external", columnList = "deviceCode,externalEventId", unique = true)
})
public class DeviceSyncEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String syncCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String deviceCode;

	@Column(nullable = false, length = 32)
	private String deviceType;

	@Column(nullable = false, length = 32)
	private String metricType;

	@Column(nullable = false, length = 128)
	private String externalEventId;

	@Column(nullable = false)
	private Instant measuredAt;

	@Column(nullable = false, length = 2000)
	private String payloadJson;

	@Column(length = 64)
	private String resultRecordCode;

	@Column(nullable = false, length = 32)
	private String syncStatus;

	@Column(nullable = false, length = 500)
	private String summaryText;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getSyncCode() {
		return syncCode;
	}

	public void setSyncCode(String syncCode) {
		this.syncCode = syncCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getDeviceCode() {
		return deviceCode;
	}

	public void setDeviceCode(String deviceCode) {
		this.deviceCode = deviceCode;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getMetricType() {
		return metricType;
	}

	public void setMetricType(String metricType) {
		this.metricType = metricType;
	}

	public String getExternalEventId() {
		return externalEventId;
	}

	public void setExternalEventId(String externalEventId) {
		this.externalEventId = externalEventId;
	}

	public Instant getMeasuredAt() {
		return measuredAt;
	}

	public void setMeasuredAt(Instant measuredAt) {
		this.measuredAt = measuredAt;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	public String getResultRecordCode() {
		return resultRecordCode;
	}

	public void setResultRecordCode(String resultRecordCode) {
		this.resultRecordCode = resultRecordCode;
	}

	public String getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
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
