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

@Entity
@Table(name = "meal_record", indexes = {
		@Index(name = "idx_meal_record_code", columnList = "recordCode", unique = true),
		@Index(name = "idx_meal_record_user_taken", columnList = "userCode,takenAt")
})
public class MealRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String recordCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(nullable = false, length = 32)
	private String mealType;

	@Column(nullable = false)
	private Instant takenAt;

	@Column(length = 500)
	private String noteText;

	@Column(length = 64)
	private String fileCode;

	@Column(nullable = false, length = 16)
	private String riskLevel;

	private Integer purineEstimateMg;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String itemsJson;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String suggestionsJson;

	@Column(nullable = false, length = 500)
	private String summaryText;

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

	public String getMealType() {
		return mealType;
	}

	public void setMealType(String mealType) {
		this.mealType = mealType;
	}

	public Instant getTakenAt() {
		return takenAt;
	}

	public void setTakenAt(Instant takenAt) {
		this.takenAt = takenAt;
	}

	public String getNoteText() {
		return noteText;
	}

	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}

	public String getFileCode() {
		return fileCode;
	}

	public void setFileCode(String fileCode) {
		this.fileCode = fileCode;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(String riskLevel) {
		this.riskLevel = riskLevel;
	}

	public Integer getPurineEstimateMg() {
		return purineEstimateMg;
	}

	public void setPurineEstimateMg(Integer purineEstimateMg) {
		this.purineEstimateMg = purineEstimateMg;
	}

	public String getItemsJson() {
		return itemsJson;
	}

	public void setItemsJson(String itemsJson) {
		this.itemsJson = itemsJson;
	}

	public String getSuggestionsJson() {
		return suggestionsJson;
	}

	public void setSuggestionsJson(String suggestionsJson) {
		this.suggestionsJson = suggestionsJson;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
}
