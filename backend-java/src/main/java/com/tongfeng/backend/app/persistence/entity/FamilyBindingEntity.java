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
@Table(name = "family_binding", indexes = {
		@Index(name = "idx_family_binding_code", columnList = "bindingCode", unique = true),
		@Index(name = "idx_family_binding_patient_status", columnList = "patientUserCode,status"),
		@Index(name = "idx_family_binding_caregiver_status", columnList = "caregiverUserCode,status")
})
public class FamilyBindingEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String bindingCode;

	@Column(nullable = false, length = 64)
	private String patientUserCode;

	@Column(nullable = false, length = 64)
	private String caregiverUserCode;

	@Column(nullable = false, length = 32)
	private String relationType;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(length = 64)
	private String sourceInviteCode;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
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

	public String getSourceInviteCode() {
		return sourceInviteCode;
	}

	public void setSourceInviteCode(String sourceInviteCode) {
		this.sourceInviteCode = sourceInviteCode;
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
