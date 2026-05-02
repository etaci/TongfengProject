package com.tongfeng.backend.app;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AppContracts {

	private AppContracts() {
	}

	public enum RiskLevel {
		GREEN,
		YELLOW,
		RED
	}

	public record MockLoginRequest(
			@NotBlank(message = "昵称不能为空")
			String nickname
	) {
	}

	public record PrivacyConsentSubmitRequest(
			@NotBlank(message = "授权版本不能为空")
			String consentVersion,
			@NotBlank(message = "隐私政策版本不能为空")
			String privacyPolicyVersion,
			@NotNull(message = "请确认是否同意隐私政策")
			Boolean privacyAccepted,
			@NotNull(message = "请确认是否同意服务条款")
			Boolean termsAccepted,
			Boolean medicalDataAuthorized,
			Boolean familyCollaborationAuthorized,
			Boolean notificationAuthorized
	) {
	}

	public record RegisterRequest(
			@NotBlank(message = "昵称不能为空")
			String nickname,
			@NotBlank(message = "账号类型不能为空")
			String accountType,
			@NotBlank(message = "账号不能为空")
			String account,
			@NotBlank(message = "密码不能为空")
			String password,
			@NotBlank(message = "确认密码不能为空")
			String confirmPassword,
			@NotNull(message = "请先完成隐私授权")
			@Valid
			PrivacyConsentSubmitRequest consent
	) {
	}

	public record LoginRequest(
			@NotBlank(message = "账号类型不能为空")
			String accountType,
			@NotBlank(message = "账号不能为空")
			String account,
			@NotBlank(message = "密码不能为空")
			String password
	) {
	}

	public record ChangePasswordRequest(
			@NotBlank(message = "当前密码不能为空")
			String currentPassword,
			@NotBlank(message = "新密码不能为空")
			String newPassword,
			@NotBlank(message = "确认密码不能为空")
			String confirmPassword,
			Boolean logoutOtherSessions
	) {
	}

	public record AuthTokenResponse(
			String sessionCode,
			String userId,
			String nickname,
			String authMode,
			String accountType,
			String accountIdentifier,
			boolean privacyConsentCompleted,
			Instant createdAt,
			Instant lastSeenAt,
			String token,
			String tokenType,
			Instant expiresAt
	) {
	}

	public record AuthSessionInfoResponse(
			String sessionCode,
			String userId,
			String nickname,
			String authMode,
			String accountType,
			String accountIdentifier,
			boolean privacyConsentCompleted,
			Instant createdAt,
			Instant lastSeenAt,
			Instant expiresAt
	) {
	}

	public record AuthLogoutResponse(
			Instant loggedOutAt,
			String message
	) {
	}

	public record AuthActiveSessionResponse(
			String sessionCode,
			String authMode,
			String accountType,
			String accountIdentifier,
			boolean currentSession,
			Instant createdAt,
			Instant lastSeenAt,
			Instant expiresAt
	) {
	}

	public record PasswordChangeResponse(
			Instant changedAt,
			int loggedOutOtherSessions,
			String message
	) {
	}

	public record AuthSessionRevokeResponse(
			String sessionCode,
			Instant revokedAt,
			String message
	) {
	}

	public record PrivacyConsentResponse(
			String consentCode,
			String userId,
			String consentVersion,
			String privacyPolicyVersion,
			boolean privacyAccepted,
			boolean termsAccepted,
			boolean medicalDataAuthorized,
			boolean familyCollaborationAuthorized,
			boolean notificationAuthorized,
			String sourceType,
			Instant effectiveAt,
			Instant createdAt
	) {
	}

	public record UserProfileRequest(
			@NotBlank(message = "姓名不能为空")
			String name,
			@NotBlank(message = "性别不能为空")
			String gender,
			LocalDate birthday,
			@Min(value = 100, message = "身高不能低于100cm")
			@Max(value = 240, message = "身高不能高于240cm")
			Integer heightCm,
			@Min(value = 180, message = "目标尿酸值过低")
			@Max(value = 600, message = "目标尿酸值过高")
			Integer targetUricAcid,
			List<String> allergies,
			List<String> comorbidities,
			String emergencyContact
	) {
	}

	public record UserProfileResponse(
			String userId,
			String name,
			String gender,
			LocalDate birthday,
			Integer heightCm,
			Integer targetUricAcid,
			List<String> allergies,
			List<String> comorbidities,
			String emergencyContact,
			Instant updatedAt
	) {
	}

	public record FeatureStatusResponse(
			String featureKey,
			String displayName,
			boolean enabled,
			String note
	) {
	}

	public record AppCapabilitiesResponse(
			List<FeatureStatusResponse> features
	) {
	}

	public record FileUploadResponse(
			String fileId,
			String fileName,
			String accessUrl,
			long size,
			String contentType
	) {
	}

	public record MealItem(
			String name,
			RiskLevel riskLevel,
			String evidence,
			Integer purineEstimateMg
	) {
	}

	public record MealAnalyzeResponse(
			String recordId,
			String imageUrl,
			String mealType,
			Instant takenAt,
			RiskLevel riskLevel,
			Integer purineEstimateMg,
			List<MealItem> items,
			List<String> suggestions,
			String summary
	) {
	}

	public record MealRecordResponse(
			String recordId,
			String mealType,
			Instant takenAt,
			RiskLevel riskLevel,
			Integer purineEstimateMg,
			String imageUrl,
			String summary
	) {
	}

	public record UricAcidCreateRequest(
			@NotNull(message = "尿酸值不能为空")
			@Min(value = 1, message = "尿酸值必须大于0")
			Integer value,
			@NotBlank(message = "单位不能为空")
			String unit,
			Instant measuredAt,
			String source,
			String note
	) {
	}

	public record WeightCreateRequest(
			@NotNull(message = "体重不能为空")
			@Positive(message = "体重必须大于0")
			BigDecimal value,
			Instant measuredAt,
			String source,
			String note
	) {
	}

	public record FlareCreateRequest(
			@NotBlank(message = "发作部位不能为空")
			String joint,
			@NotNull(message = "疼痛等级不能为空")
			@Min(value = 1, message = "疼痛等级不能小于1")
			@Max(value = 10, message = "疼痛等级不能大于10")
			Integer painLevel,
			Instant startedAt,
			String durationNote,
			String note
	) {
	}

	public record HydrationCreateRequest(
			@NotNull(message = "饮水量不能为空")
			@Min(value = 0, message = "饮水量不能为负数")
			Integer waterIntakeMl,
			@NotNull(message = "尿液颜色等级不能为空")
			@Min(value = 1, message = "尿液颜色等级不能小于1")
			@Max(value = 5, message = "尿液颜色等级不能大于5")
			Integer urineColorLevel,
			Instant checkedAt,
			String note
	) {
	}

	public record RecordSimpleResponse(
			String recordId,
			Instant createdAt,
			String message
	) {
	}

	public record UricAcidRecordResponse(
			String recordId,
			Integer value,
			String unit,
			Instant measuredAt,
			String source,
			String note,
			RiskLevel riskLevel
	) {
	}

	public record WeightRecordResponse(
			String recordId,
			BigDecimal value,
			String unit,
			Instant measuredAt,
			String source,
			String note,
			RiskLevel riskLevel
	) {
	}

	public record HydrationRecordResponse(
			String recordId,
			Integer waterIntakeMl,
			Integer urineColorLevel,
			Instant checkedAt,
			String note,
			RiskLevel riskLevel
	) {
	}

	public record FlareRecordResponse(
			String recordId,
			String joint,
			Integer painLevel,
			Instant startedAt,
			String durationNote,
			String note,
			RiskLevel riskLevel
	) {
	}

	public record HealthRecordCenterItemResponse(
			String recordId,
			String type,
			String title,
			String summary,
			Instant occurredAt,
			RiskLevel riskLevel,
			String source,
			List<String> tags
	) {
	}

	public record HealthRecordCenterResponse(
			List<String> types,
			int totalCount,
			int returnedCount,
			int limit,
			List<HealthRecordCenterItemResponse> items,
			String nextCursor,
			boolean hasMore
	) {
	}

	public record HealthRecordDetailFieldResponse(
			String key,
			String label,
			String value
	) {
	}

	public record HealthRecordDetailResponse(
			String recordId,
			String type,
			String title,
			String summary,
			Instant occurredAt,
			RiskLevel riskLevel,
			String source,
			String note,
			List<String> tags,
			List<HealthRecordDetailFieldResponse> fields
	) {
	}

	public record HealthRecordDeleteResponse(
			String recordId,
			String type,
			String status,
			Instant deletedAt,
			String message
	) {
	}

	public record HealthRecordUpdateRequest(
			Integer value,
			BigDecimal decimalValue,
			String unit,
			Instant measuredAt,
			String source,
			Integer waterIntakeMl,
			Integer urineColorLevel,
			Instant checkedAt,
			String joint,
			Integer painLevel,
			Instant startedAt,
			String durationNote,
			String note,
			@NotBlank(message = "更正原因不能为空")
			String changeReason
	) {
	}

	public record HealthRecordAuditFieldResponse(
			String key,
			String label,
			String beforeValue,
			String afterValue
	) {
	}

	public record HealthRecordAuditResponse(
			String auditId,
			String recordId,
			String type,
			String action,
			String changeReason,
			String summary,
			Instant operatedAt,
			List<HealthRecordAuditFieldResponse> fields
	) {
	}

	public record HealthRecordRestoreRequest(
			@NotBlank(message = "恢复原因不能为空")
			String changeReason
	) {
	}

	public record HealthRecordRestoreResponse(
			String recordId,
			String type,
			String restoredFromAuditId,
			String status,
			Instant restoredAt,
			String message,
			HealthRecordDetailResponse detail
	) {
	}

	public record ReminderResponse(
			String reminderId,
			String type,
			String title,
			String content,
			RiskLevel riskLevel,
			Instant triggerAt
	) {
	}

	public record DashboardOverviewResponse(
			String userId,
			String stage,
			int mealsCount,
			int highRiskMealsCount,
			int uricAcidCount,
			int flareCount,
			String latestRiskSummary,
			List<String> todayFocus,
			List<ReminderResponse> reminders
	) {
	}

	public record TodayActionItemResponse(
			String actionKey,
			String category,
			String title,
			String description,
			String priority,
			String status
	) {
	}

	public record TodayActionPlanResponse(
			String userId,
			RiskLevel overallRiskLevel,
			String triageCode,
			String triageTitle,
			String triageSummary,
			String nextStep,
			List<String> reasons,
			List<TodayActionItemResponse> actions,
			List<String> trustNotes,
			Instant generatedAt
	) {
	}

	public record TrendPoint(
			String date,
			BigDecimal value,
			String unit
	) {
	}

	public record TrendResponse(
			List<TrendPoint> uricAcid,
			List<TrendPoint> weight,
			List<TrendPoint> hydration
	) {
	}

	public record UricAcidCauseAnalysisResponse(
			int lookbackDays,
			Integer latestUricAcidValue,
			String latestUricAcidUnit,
			Instant measuredAt,
			Integer targetUricAcidValue,
			RiskLevel overallRiskLevel,
			String summary,
			List<RiskFactorResponse> factors,
			List<String> nextActions,
			Instant generatedAt
	) {
	}

	public record MvpMetricBreakdownItemResponse(
			String eventType,
			String label,
			long totalEvents,
			long uniqueUsers,
			Instant latestEventAt
	) {
	}

	public record MvpMetricsSummaryResponse(
			int days,
			long totalEvents,
			long activeUsers,
			long mealAnalyzeUsers,
			long uricAcidRecordUsers,
			long labReportUsers,
			long familyInviteUsers,
			long familyAcceptUsers,
			long familySummaryUsers,
			List<MvpMetricBreakdownItemResponse> eventBreakdown,
			Instant generatedAt
	) {
	}

	public record DailyHealthSummaryResponse(
			String summaryDate,
			Integer latestUricAcidValue,
			String latestUricAcidUnit,
			BigDecimal latestWeightValue,
			Integer totalWaterIntakeMl,
			Integer highRiskMealCount,
			Integer flareCount,
			RiskLevel overallRiskLevel,
			String summaryText
	) {
	}

	public record TimelineEvent(
			String eventId,
			String type,
			String title,
			String detail,
			Instant occurredAt,
			RiskLevel riskLevel
	) {
	}

	public record TimelineResponse(List<TimelineEvent> events) {
	}

	public record LabIndicator(
			String code,
			String name,
			BigDecimal value,
			String unit,
			String referenceRange,
			RiskLevel riskLevel
	) {
	}

	public record LabReportAnalyzeResponse(
			String reportId,
			LocalDate reportDate,
			List<LabIndicator> indicators,
			RiskLevel overallRiskLevel,
			List<String> suggestions,
			String summary
	) {
	}

	public record LabReportReviewComparisonResponse(
			String code,
			String name,
			BigDecimal currentValue,
			BigDecimal previousValue,
			BigDecimal deltaValue,
			String unit,
			String referenceRange,
			RiskLevel currentRiskLevel,
			String trend,
			String interpretation
	) {
	}

	public record LabReportReviewResponse(
			String reportId,
			LocalDate reportDate,
			RiskLevel overallRiskLevel,
			String reviewSummary,
			String comparedReportId,
			LocalDate comparedReportDate,
			Integer daysBetweenReports,
			Integer targetUricAcidValue,
			BigDecimal currentUricAcidValue,
			String currentUricAcidUnit,
			boolean uricAcidWithinTarget,
			String targetConclusion,
			List<LabReportReviewComparisonResponse> comparisons,
			List<String> keyChanges,
			String followUpRecommendation,
			List<String> nextActions,
			List<String> trustNotes,
			Instant generatedAt
	) {
	}

	public record AskKnowledgeRequest(
			@NotBlank(message = "问题不能为空")
			String question,
			String scene
	) {
	}

	public record KnowledgeAnswerResponse(
			String answer,
			List<String> references,
			boolean escalateToDoctor,
			String disclaimer
	) {
	}

	public record PersonaSummaryResponse(
			List<String> tags,
			List<String> triggers,
			String narrative
	) {
	}

	public record ProactiveCareSettingsRequest(
			@NotBlank(message = "监测城市不能为空")
			String monitoringCity,
			String countryCode,
			Boolean weatherAlertsEnabled
	) {
	}

	public record ProactiveCareSettingsResponse(
			String monitoringCity,
			String countryCode,
			Double latitude,
			Double longitude,
			String timezoneId,
			boolean weatherAlertsEnabled,
			Instant updatedAt,
			Instant lastWeatherSyncAt
	) {
	}

	public record WeatherSnapshotResponse(
			String city,
			String countryCode,
			String summaryDate,
			BigDecimal temperatureC,
			BigDecimal apparentTemperatureC,
			Integer relativeHumidity,
			Integer precipitationProbability,
			Integer weatherCode,
			RiskLevel riskLevel,
			String sourceType,
			String summary
	) {
	}

	public record RiskFactorResponse(
			String code,
			String title,
			RiskLevel riskLevel,
			String detail,
			String evidence
	) {
	}

	public record ProactiveCareBriefResponse(
			RiskLevel overallRiskLevel,
			int riskScore,
			String summary,
			WeatherSnapshotResponse weather,
			List<RiskFactorResponse> factors,
			List<String> suggestions,
			Instant generatedAt
	) {
	}

	public record FlareReviewReportResponse(
			String reportId,
			String flareRecordId,
			Instant flareStartedAt,
			String joint,
			Integer painLevel,
			RiskLevel overallRiskLevel,
			List<String> suspectedTriggers,
			List<TimelineEvent> relatedEvents,
			List<String> actionSuggestions,
			String summary,
			Instant generatedAt
	) {
	}

	public record FamilyInviteCreateRequest(
			@NotBlank(message = "关系类型不能为空")
			String relationType,
			String inviteMessage,
			@Min(value = 1, message = "有效天数不能小于1")
			@Max(value = 30, message = "有效天数不能大于30")
			Integer expiresInDays,
			String caregiverPermission,
			Boolean weeklyReportEnabled,
			Boolean notifyOnHighRisk
	) {
	}

	public record FamilyInviteResponse(
			String inviteCode,
			String patientUserId,
			String patientNickname,
			String creatorUserId,
			String relationType,
			String inviteMessage,
			String status,
			String caregiverPermission,
			boolean weeklyReportEnabled,
			boolean notifyOnHighRisk,
			String acceptedByUserId,
			String acceptedByNickname,
			Instant expiresAt,
			Instant createdAt
	) {
	}

	public record FamilyBindingMemberResponse(
			String bindingCode,
			String patientUserId,
			String patientNickname,
			String caregiverUserId,
			String caregiverNickname,
			String relationType,
			String status,
			String caregiverPermission,
			boolean weeklyReportEnabled,
			boolean notifyOnHighRisk,
			Instant createdAt
	) {
	}

	public record FamilyMembersResponse(
			List<FamilyBindingMemberResponse> asPatient,
			List<FamilyBindingMemberResponse> asCaregiver
	) {
	}

	public record FamilyAlertResponse(
			String alertId,
			String patientUserId,
			String patientNickname,
			String relationType,
			RiskLevel riskLevel,
			String title,
			String content,
			String sourceType,
			Instant generatedAt
		) {
	}

	public record FamilyBindingPermissionUpdateRequest(
			@NotBlank(message = "家属权限不能为空")
			String caregiverPermission,
			Boolean weeklyReportEnabled,
			Boolean notifyOnHighRisk
	) {
	}

	public record FamilyPatientSummaryResponse(
			String patientUserId,
			String patientNickname,
			String relationType,
			String caregiverPermission,
			boolean weeklyReportEnabled,
			RiskLevel overallRiskLevel,
			String latestRiskSummary,
			List<String> todayFocus,
			List<ReminderResponse> reminders,
			WeatherSnapshotResponse weather,
			Instant lastFlareAt,
			Integer lastUricAcidValue,
			String lastUricAcidUnit,
			List<String> nextActions,
			Instant generatedAt
	) {
	}

	public record FamilySharedMedicationWeeklyReportResponse(
			String patientUserId,
			String patientNickname,
			String relationType,
			String caregiverPermission,
			boolean weeklyReportEnabled,
			MedicationWeeklyReportResponse weeklyReport,
			Instant generatedAt
	) {
	}

	public record FamilyTaskCreateRequest(
			@NotBlank(message = "代办标题不能为空")
			String title,
			String description,
			Instant dueAt
	) {
	}

	public record FamilyTaskCompleteRequest(
			String completionNote
	) {
	}

	public record FamilyTaskResponse(
			String taskCode,
			String bindingCode,
			String patientUserId,
			String patientNickname,
			String caregiverUserId,
			String caregiverNickname,
			String relationType,
			String status,
			String title,
			String description,
			Instant dueAt,
			Instant createdAt,
			Instant completedAt,
			String completionNote
	) {
	}

	public record FamilyTasksResponse(
			List<FamilyTaskResponse> asPatient,
			List<FamilyTaskResponse> asCaregiver
	) {
	}

	public record MedicationItem(
			@NotBlank(message = "药物名称不能为空")
			String name,
			@NotBlank(message = "剂量不能为空")
			String dosage,
			@NotBlank(message = "频次不能为空")
			String frequency,
			String remark,
			@Min(value = 0, message = "剩余药量天数不能为负数")
			Integer remainingDays,
			@Min(value = 1, message = "补药提醒阈值不能小于1天")
			@Max(value = 30, message = "补药提醒阈值不能大于30天")
			Integer refillThresholdDays
	) {
	}

	public record MedicationPlanRequest(
			@NotEmpty(message = "当前用药不能为空")
			List<@Valid MedicationItem> currentMedications,
			String followUpNote
	) {
	}

	public record MedicationPlanResponse(
			List<MedicationItem> currentMedications,
			String followUpNote,
			Instant updatedAt
	) {
	}

	public record MedicationCheckinRequest(
			@NotBlank(message = "药物名称不能为空")
			String medicationName,
			@NotBlank(message = "服药时段不能为空")
			String scheduledPeriod,
			@NotBlank(message = "服药状态不能为空")
			String status,
			String note
	) {
	}

	public record MedicationCheckinResponse(
			String checkinId,
			String medicationName,
			String scheduledPeriod,
			String status,
			String guidance,
			String note,
			String checkinDate,
			Instant checkinAt
	) {
	}

	public record MedicationAdherenceSummaryResponse(
			String summaryDate,
			int plannedDoseCount,
			int takenDoseCount,
			int missedDoseCount,
			int skippedDoseCount,
			int adherenceRate,
			int currentStreakDays,
			List<String> overdueItems,
			List<String> nextActions,
			List<MedicationCheckinResponse> recentCheckins
	) {
	}

	public record MedicationAdherenceDayResponse(
			String summaryDate,
			int plannedDoseCount,
			int takenDoseCount,
			int missedDoseCount,
			int skippedDoseCount,
			int adherenceRate
	) {
	}

	public record MedicationRefillAlertResponse(
			String medicationName,
			String dosage,
			Integer remainingDays,
			Integer refillThresholdDays,
			RiskLevel riskLevel,
			String suggestion
	) {
	}

	public record MedicationWeeklyReportResponse(
			String startDate,
			String endDate,
			int plannedDoseCount,
			int takenDoseCount,
			int missedDoseCount,
			int skippedDoseCount,
			int overdueDoseCount,
			int adherenceRate,
			int currentStreakDays,
			int longestStreakDays,
			List<MedicationAdherenceDayResponse> dailyBreakdown,
			List<String> focusMedications,
			List<MedicationRefillAlertResponse> refillAlerts,
			List<String> highlights,
			List<String> nextActions,
			Instant generatedAt
	) {
	}
}
