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
@Table(name = "flare_record", indexes = {
		@Index(name = "idx_flare_record_code", columnList = "recordCode", unique = true),
		@Index(name = "idx_flare_user_started", columnList = "userCode,startedAt")
})
public class FlareRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String recordCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String jointName;

	@Column(nullable = false)
	private Integer painLevel;

	@Column(nullable = false)
	private Instant startedAt;

	@Column(length = 128)
	private String durationNote;

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

	public String getJointName() {
		return jointName;
	}

	public void setJointName(String jointName) {
		this.jointName = jointName;
	}

	public Integer getPainLevel() {
		return painLevel;
	}

	public void setPainLevel(Integer painLevel) {
		this.painLevel = painLevel;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public String getDurationNote() {
		return durationNote;
	}

	public void setDurationNote(String durationNote) {
		this.durationNote = durationNote;
	}

	public String getNoteText() {
		return noteText;
	}

	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}
}
