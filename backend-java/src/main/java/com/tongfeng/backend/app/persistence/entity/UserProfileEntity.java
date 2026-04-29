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
@Table(name = "user_profile", indexes = {
		@Index(name = "idx_user_profile_user_code", columnList = "userCode", unique = true)
})
public class UserProfileEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String userCode;

	@Column(nullable = false, length = 64)
	private String name;

	@Column(nullable = false, length = 16)
	private String gender;

	private LocalDate birthday;

	private Integer heightCm;

	private Integer targetUricAcid;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String allergiesJson;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String comorbiditiesJson;

	@Column(length = 128)
	private String emergencyContact;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public LocalDate getBirthday() {
		return birthday;
	}

	public void setBirthday(LocalDate birthday) {
		this.birthday = birthday;
	}

	public Integer getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(Integer heightCm) {
		this.heightCm = heightCm;
	}

	public Integer getTargetUricAcid() {
		return targetUricAcid;
	}

	public void setTargetUricAcid(Integer targetUricAcid) {
		this.targetUricAcid = targetUricAcid;
	}

	public String getAllergiesJson() {
		return allergiesJson;
	}

	public void setAllergiesJson(String allergiesJson) {
		this.allergiesJson = allergiesJson;
	}

	public String getComorbiditiesJson() {
		return comorbiditiesJson;
	}

	public void setComorbiditiesJson(String comorbiditiesJson) {
		this.comorbiditiesJson = comorbiditiesJson;
	}

	public String getEmergencyContact() {
		return emergencyContact;
	}

	public void setEmergencyContact(String emergencyContact) {
		this.emergencyContact = emergencyContact;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
