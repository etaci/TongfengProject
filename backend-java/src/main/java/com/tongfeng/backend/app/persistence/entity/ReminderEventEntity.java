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
@Table(name = "reminder_event", indexes = {
		@Index(name = "idx_reminder_event_code", columnList = "reminderCode", unique = true),
		@Index(name = "idx_reminder_event_user_status", columnList = "userCode,status"),
		@Index(name = "idx_reminder_event_user_trigger", columnList = "userCode,triggerAt")
})
public class ReminderEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String reminderCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 32)
	private String type;

	@Column(nullable = false, length = 128)
	private String title;

	@Column(nullable = false, length = 500)
	private String content;

	@Column(nullable = false, length = 16)
	private String riskLevel;

	@Column(nullable = false)
	private Instant triggerAt;

	@Column(nullable = false, length = 64)
	private String sourceType;

	@Column(nullable = false, length = 64)
	private String status;

	@Column(nullable = false, length = 128)
	private String dedupKey;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getReminderCode() {
		return reminderCode;
	}

	public void setReminderCode(String reminderCode) {
		this.reminderCode = reminderCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(String riskLevel) {
		this.riskLevel = riskLevel;
	}

	public Instant getTriggerAt() {
		return triggerAt;
	}

	public void setTriggerAt(Instant triggerAt) {
		this.triggerAt = triggerAt;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDedupKey() {
		return dedupKey;
	}

	public void setDedupKey(String dedupKey) {
		this.dedupKey = dedupKey;
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
