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
@Table(name = "blood_pressure_record", indexes = {
		@Index(name = "idx_blood_pressure_record_code", columnList = "recordCode", unique = true),
		@Index(name = "idx_blood_pressure_user_measured", columnList = "userCode,measuredAt")
})
public class BloodPressureRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String recordCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false)
	private Integer systolicPressure;

	@Column(nullable = false)
	private Integer diastolicPressure;

	@Column
	private Integer pulseRate;

	@Column(nullable = false, length = 32)
	private String unit;

	@Column(nullable = false)
	private Instant measuredAt;

	@Column(length = 64)
	private String sourceName;

	@Column(length = 500)
	private String noteText;

	public Long getId() {
		return id;
	}

	public String getRecordCode() {
		return recordCode;
	}

	public void setRecordCode(String recordCode) {
		this.recordCode = recordCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public Integer getSystolicPressure() {
		return systolicPressure;
	}

	public void setSystolicPressure(Integer systolicPressure) {
		this.systolicPressure = systolicPressure;
	}

	public Integer getDiastolicPressure() {
		return diastolicPressure;
	}

	public void setDiastolicPressure(Integer diastolicPressure) {
		this.diastolicPressure = diastolicPressure;
	}

	public Integer getPulseRate() {
		return pulseRate;
	}

	public void setPulseRate(Integer pulseRate) {
		this.pulseRate = pulseRate;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Instant getMeasuredAt() {
		return measuredAt;
	}

	public void setMeasuredAt(Instant measuredAt) {
		this.measuredAt = measuredAt;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getNoteText() {
		return noteText;
	}

	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}
}
