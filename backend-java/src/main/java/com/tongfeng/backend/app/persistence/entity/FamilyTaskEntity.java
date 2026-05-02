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
@Table(name = "family_task", indexes = {
		@Index(name = "idx_family_task_code", columnList = "taskCode", unique = true),
		@Index(name = "idx_family_task_patient_created", columnList = "patientUserCode,createdAt"),
		@Index(name = "idx_family_task_caregiver_created", columnList = "caregiverUserCode,createdAt"),
		@Index(name = "idx_family_task_binding_status", columnList = "bindingCode,status")
})
public class FamilyTaskEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String taskCode;

	@Column(nullable = false, length = 64)
	private String bindingCode;

	@Column(nullable = false, length = 64)
	private String patientUserCode;

	@Column(nullable = false, length = 64)
	private String caregiverUserCode;

	@Column(nullable = false, length = 32)
	private String relationType;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(nullable = false, length = 128)
	private String title;

	@Column(length = 500)
	private String description;

	@Column(length = 64)
	private String createdByUserCode;

	@Column
	private Instant dueAt;

	@Column
	private Instant completedAt;

	@Column(length = 500)
	private String completionNote;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getTaskCode() {
		return taskCode;
	}

	public void setTaskCode(String taskCode) {
		this.taskCode = taskCode;
	}

	public String getBindingCode() {
		return bindingCode;
	}

	public void setBindingCode(String bindingCode) {
		this.bindingCode = bindingCode;
	}

	public String getPatientUserCode() {
		return patientUserCode;
	}

	public void setPatientUserCode(String patientUserCode) {
		this.patientUserCode = patientUserCode;
	}

	public String getCaregiverUserCode() {
		return caregiverUserCode;
	}

	public void setCaregiverUserCode(String caregiverUserCode) {
		this.caregiverUserCode = caregiverUserCode;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreatedByUserCode() {
		return createdByUserCode;
	}

	public void setCreatedByUserCode(String createdByUserCode) {
		this.createdByUserCode = createdByUserCode;
	}

	public Instant getDueAt() {
		return dueAt;
	}

	public void setDueAt(Instant dueAt) {
		this.dueAt = dueAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public String getCompletionNote() {
		return completionNote;
	}

	public void setCompletionNote(String completionNote) {
		this.completionNote = completionNote;
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
