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
@Table(name = "access_audit", indexes = {
		@Index(name = "idx_access_audit_actor", columnList = "actorUserCode,operatedAt"),
		@Index(name = "idx_access_audit_patient", columnList = "patientUserCode,operatedAt"),
		@Index(name = "idx_access_audit_resource", columnList = "resourceType,resourceId,operatedAt")
})
public class AccessAuditEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String auditCode;

	@Column(nullable = false, length = 64)
	private String actorUserCode;

	@Column(nullable = false, length = 32)
	private String actorRole;

	@Column(nullable = false, length = 64)
	private String patientUserCode;

	@Column(nullable = false, length = 64)
	private String resourceType;

	@Column(nullable = false, length = 128)
	private String resourceId;

	@Column(nullable = false, length = 64)
	private String actionType;

	@Column(nullable = false, length = 32)
	private String decision;

	@Column(length = 64)
	private String bindingCode;

	@Column(length = 64)
	private String sessionCode;

	@Column(nullable = false, length = 500)
	private String reasonText;

	@Column(nullable = false)
	private Instant operatedAt;

	public Long getId() {
		return id;
	}

	public String getAuditCode() {
		return auditCode;
	}

	public void setAuditCode(String auditCode) {
		this.auditCode = auditCode;
	}

	public String getActorUserCode() {
		return actorUserCode;
	}

	public void setActorUserCode(String actorUserCode) {
		this.actorUserCode = actorUserCode;
	}

	public String getActorRole() {
		return actorRole;
	}

	public void setActorRole(String actorRole) {
		this.actorRole = actorRole;
	}

	public String getPatientUserCode() {
		return patientUserCode;
	}

	public void setPatientUserCode(String patientUserCode) {
		this.patientUserCode = patientUserCode;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public String getBindingCode() {
		return bindingCode;
	}

	public void setBindingCode(String bindingCode) {
		this.bindingCode = bindingCode;
	}

	public String getSessionCode() {
		return sessionCode;
	}

	public void setSessionCode(String sessionCode) {
		this.sessionCode = sessionCode;
	}

	public String getReasonText() {
		return reasonText;
	}

	public void setReasonText(String reasonText) {
		this.reasonText = reasonText;
	}

	public Instant getOperatedAt() {
		return operatedAt;
	}

	public void setOperatedAt(Instant operatedAt) {
		this.operatedAt = operatedAt;
	}
}
