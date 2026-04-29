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
@Table(name = "hydration_record", indexes = {
		@Index(name = "idx_hydration_record_code", columnList = "recordCode", unique = true),
		@Index(name = "idx_hydration_user_checked", columnList = "userCode,checkedAt")
})
public class HydrationRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String recordCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false)
	private Integer waterIntakeMl;

	@Column(nullable = false)
	private Integer urineColorLevel;

	@Column(nullable = false)
	private Instant checkedAt;

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

	public Integer getWaterIntakeMl() {
		return waterIntakeMl;
	}

	public void setWaterIntakeMl(Integer waterIntakeMl) {
		this.waterIntakeMl = waterIntakeMl;
	}

	public Integer getUrineColorLevel() {
		return urineColorLevel;
	}

	public void setUrineColorLevel(Integer urineColorLevel) {
		this.urineColorLevel = urineColorLevel;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt) {
		this.checkedAt = checkedAt;
	}

	public String getNoteText() {
		return noteText;
	}

	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}
}
