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
@Table(name = "medication_plan", indexes = {
		@Index(name = "idx_medication_plan_user_code", columnList = "userCode", unique = true)
})
public class MedicationPlanEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String userCode;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String currentMedicationsJson;

	@Column(length = 500)
	private String followUpNote;

	@Column(nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getCurrentMedicationsJson() {
		return currentMedicationsJson;
	}

	public void setCurrentMedicationsJson(String currentMedicationsJson) {
		this.currentMedicationsJson = currentMedicationsJson;
	}

	public String getFollowUpNote() {
		return followUpNote;
	}

	public void setFollowUpNote(String followUpNote) {
		this.followUpNote = followUpNote;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
