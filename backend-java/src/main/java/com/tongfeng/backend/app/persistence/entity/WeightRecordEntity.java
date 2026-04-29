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

@Entity
@Table(name = "weight_record", indexes = {
		@Index(name = "idx_weight_record_code", columnList = "recordCode", unique = true),
		@Index(name = "idx_weight_user_measured", columnList = "userCode,measuredAt")
})
public class WeightRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String recordCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal weightValue;

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

	public BigDecimal getWeightValue() {
		return weightValue;
	}

	public void setWeightValue(BigDecimal weightValue) {
		this.weightValue = weightValue;
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
