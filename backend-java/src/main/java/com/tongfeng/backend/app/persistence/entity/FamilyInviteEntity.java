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
@Table(name = "family_invite", indexes = {
		@Index(name = "idx_family_invite_code", columnList = "inviteCode", unique = true),
		@Index(name = "idx_family_invite_creator_status", columnList = "creatorUserCode,status"),
		@Index(name = "idx_family_invite_patient_status", columnList = "patientUserCode,status")
})
public class FamilyInviteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String inviteCode;

	@Column(nullable = false, length = 64)
	private String creatorUserCode;

	@Column(nullable = false, length = 64)
	private String patientUserCode;

	@Column(nullable = false, length = 32)
	private String relationType;

	@Column(length = 200)
	private String inviteMessage;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(length = 64)
	private String acceptedByUserCode;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getInviteCode() {
		return inviteCode;
	}

	public void setInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}

	public String getCreatorUserCode() {
		return creatorUserCode;
	}

	public void setCreatorUserCode(String creatorUserCode) {
		this.creatorUserCode = creatorUserCode;
	}

	public String getPatientUserCode() {
		return patientUserCode;
	}

	public void setPatientUserCode(String patientUserCode) {
		this.patientUserCode = patientUserCode;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}

	public String getInviteMessage() {
		return inviteMessage;
	}

	public void setInviteMessage(String inviteMessage) {
		this.inviteMessage = inviteMessage;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAcceptedByUserCode() {
		return acceptedByUserCode;
	}

	public void setAcceptedByUserCode(String acceptedByUserCode) {
		this.acceptedByUserCode = acceptedByUserCode;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
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
