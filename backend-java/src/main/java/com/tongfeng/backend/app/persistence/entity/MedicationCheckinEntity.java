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
@Table(name = "medication_checkin", indexes = {
		@Index(name = "idx_medication_checkin_code", columnList = "checkinCode", unique = true),
		@Index(name = "idx_medication_checkin_user_date", columnList = "userCode,checkinDate"),
		@Index(name = "uk_medication_checkin_daily_slot", columnList = "userCode,checkinDate,medicationName,scheduledPeriod", unique = true)
})
public class MedicationCheckinEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String checkinCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 128)
	private String medicationName;

	@Column(nullable = false, length = 32)
	private String scheduledPeriod;

	@Column(nullable = false, length = 32)
	private String checkinStatus;

	@Column(length = 500)
	private String noteText;

	@Column(nullable = false)
	private LocalDate checkinDate;

	@Column(nullable = false)
	private Instant checkinAt;

	@Column(nullable = false, length = 32)
	private String sourceType;

	public Long getId() {
		return id;
	}

	public String getCheckinCode() {
		return checkinCode;
	}

	public void setCheckinCode(String checkinCode) {
		this.checkinCode = checkinCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getMedicationName() {
		return medicationName;
	}

	public void setMedicationName(String medicationName) {
		this.medicationName = medicationName;
	}

	public String getScheduledPeriod() {
		return scheduledPeriod;
	}

	public void setScheduledPeriod(String scheduledPeriod) {
		this.scheduledPeriod = scheduledPeriod;
	}

	public String getCheckinStatus() {
		return checkinStatus;
	}

	public void setCheckinStatus(String checkinStatus) {
		this.checkinStatus = checkinStatus;
	}

	public String getNoteText() {
		return noteText;
	}

	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}

	public LocalDate getCheckinDate() {
		return checkinDate;
	}

	public void setCheckinDate(LocalDate checkinDate) {
		this.checkinDate = checkinDate;
	}

	public Instant getCheckinAt() {
		return checkinAt;
	}

	public void setCheckinAt(Instant checkinAt) {
		this.checkinAt = checkinAt;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}
}
