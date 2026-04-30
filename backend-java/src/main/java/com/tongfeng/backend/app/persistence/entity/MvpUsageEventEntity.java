package com.tongfeng.backend.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "mvp_usage_event", indexes = {
		@Index(name = "idx_mvp_usage_event_code", columnList = "eventCode", unique = true),
		@Index(name = "idx_mvp_usage_event_date", columnList = "eventDate"),
		@Index(name = "idx_mvp_usage_event_type_date", columnList = "eventType,eventDate")
})
public class MvpUsageEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String eventCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String eventType;

	@Column(nullable = false, length = 32)
	private String sourcePage;

	@Column(nullable = false)
	private LocalDate eventDate;

	@Column(nullable = false)
	private Instant createdAt;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String payloadJson;

	public Long getId() {
		return id;
	}

	public String getEventCode() {
		return eventCode;
	}

	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getSourcePage() {
		return sourcePage;
	}

	public void setSourcePage(String sourcePage) {
		this.sourcePage = sourcePage;
	}

	public LocalDate getEventDate() {
		return eventDate;
	}

	public void setEventDate(LocalDate eventDate) {
		this.eventDate = eventDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}
}
