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
@Table(name = "lab_report_record", indexes = {
		@Index(name = "idx_lab_report_code", columnList = "reportCode", unique = true),
		@Index(name = "idx_lab_report_user_date", columnList = "userCode,reportDate")
})
public class LabReportRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String reportCode;

	@Column(nullable = false, length = 64)
	private String userCode;

	@Column(length = 64)
	private String fileCode;

	@Column(nullable = false)
	private LocalDate reportDate;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String indicatorsJson;

	@Column(nullable = false, length = 16)
	private String overallRiskLevel;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String suggestionsJson;

	@Column(nullable = false)
	private boolean manualConfirmed;

	@Column
	private Instant manualConfirmedAt;

	@Column(nullable = false, length = 500)
	private String summaryText;

	public Long getId() {
		return id;
	}

	public String getReportCode() {
		return reportCode;
	}

	public void setReportCode(String reportCode) {
		this.reportCode = reportCode;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getFileCode() {
		return fileCode;
	}

	public void setFileCode(String fileCode) {
		this.fileCode = fileCode;
	}

	public LocalDate getReportDate() {
		return reportDate;
	}

	public void setReportDate(LocalDate reportDate) {
		this.reportDate = reportDate;
	}

	public String getIndicatorsJson() {
		return indicatorsJson;
	}

	public void setIndicatorsJson(String indicatorsJson) {
		this.indicatorsJson = indicatorsJson;
	}

	public String getOverallRiskLevel() {
		return overallRiskLevel;
	}

	public void setOverallRiskLevel(String overallRiskLevel) {
		this.overallRiskLevel = overallRiskLevel;
	}

	public String getSuggestionsJson() {
		return suggestionsJson;
	}

	public void setSuggestionsJson(String suggestionsJson) {
		this.suggestionsJson = suggestionsJson;
	}

	public boolean isManualConfirmed() {
		return manualConfirmed;
	}

	public void setManualConfirmed(boolean manualConfirmed) {
		this.manualConfirmed = manualConfirmed;
	}

	public Instant getManualConfirmedAt() {
		return manualConfirmedAt;
	}

	public void setManualConfirmedAt(Instant manualConfirmedAt) {
		this.manualConfirmedAt = manualConfirmedAt;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
}
