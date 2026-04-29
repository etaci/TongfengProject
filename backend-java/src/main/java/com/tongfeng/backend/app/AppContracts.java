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

	public record AuthTokenResponse(
			String userId,
			String nickname,
			String token,
			String tokenType,
			Instant expiresAt
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

	public record BloodPressureCreateRequest(
			@NotNull(message = "收缩压不能为空")
			@Min(value = 60, message = "收缩压不能低于60")
			@Max(value = 260, message = "收缩压不能高于260")
			Integer systolicPressure,
			@NotNull(message = "舒张压不能为空")
			@Min(value = 40, message = "舒张压不能低于40")
			@Max(value = 180, message = "舒张压不能高于180")
			Integer diastolicPressure,
			@Min(value = 30, message = "脉搏不能低于30")
			@Max(value = 240, message = "脉搏不能高于240")
			Integer pulseRate,
			String unit,
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

	public record BloodPressureRecordResponse(
			String recordId,
			Integer systolicPressure,
			Integer diastolicPressure,
			Integer pulseRate,
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
			Integer systolicPressure,
			Integer diastolicPressure,
			Integer pulseRate,
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
			Integer expiresInDays
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

	public record FamilyPatientSummaryResponse(
			String patientUserId,
			String patientNickname,
			String relationType,
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

	public record DeviceBindingCreateRequest(
			@NotBlank(message = "设备类型不能为空")
			String deviceType,
			@NotBlank(message = "厂商不能为空")
			String vendorName,
			String deviceModel,
			@NotBlank(message = "序列号不能为空")
			String serialNumber,
			String aliasName
	) {
	}

	public record DeviceBindingResponse(
			String deviceCode,
			String deviceType,
			String vendorName,
			String deviceModel,
			String serialNumber,
			String aliasName,
			String vendorProfileCode,
			List<String> supportedMetricTypes,
			String status,
			Instant lastSyncedAt,
			Instant createdAt
	) {
	}

	public record DeviceCatalogItemResponse(
			String profileCode,
			String deviceType,
			String deviceTypeName,
			String vendorName,
			String deviceModel,
			List<String> supportedMetricTypes,
			String bindingHint
	) {
	}

	public record DeviceOverviewItemResponse(
			String deviceCode,
			String aliasName,
			String deviceType,
			String deviceTypeName,
			String vendorName,
			String deviceModel,
			String status,
			List<String> supportedMetricTypes,
			int totalSyncCount,
			String latestMetricType,
			String latestSummary,
			String syncHealthStatus,
			Instant lastSyncedAt
	) {
	}

	public record DeviceOverviewResponse(
			int totalDevices,
			int activeDevices,
			int recentlySyncedDevices,
			int attentionDevices,
			List<DeviceOverviewItemResponse> devices
	) {
	}

	public record DeviceSyncItemRequest(
			@NotBlank(message = "指标类型不能为空")
			String metricType,
			@NotBlank(message = "外部事件ID不能为空")
			String externalEventId,
			Instant measuredAt,
			BigDecimal value,
			String unit,
			Integer waterIntakeMl,
			Integer urineColorLevel,
			Integer systolicPressure,
			Integer diastolicPressure,
			Integer pulseRate,
			String note
	) {
	}

	public record DeviceSyncBatchRequest(
			@NotEmpty(message = "同步数据不能为空")
			List<@Valid DeviceSyncItemRequest> items
	) {
	}

	public record DeviceSyncResultResponse(
			String syncCode,
			String metricType,
			String syncStatus,
			String resultRecordId,
			String summary,
			Instant measuredAt
	) {
	}

	public record DeviceSyncBatchResponse(
			String deviceCode,
			int syncedCount,
			List<DeviceSyncResultResponse> results,
			Instant lastSyncedAt
	) {
	}

	public record GrowthOverviewResponse(
			String userId,
			int level,
			String levelTitle,
			int totalPoints,
			int redeemablePoints,
			int currentLevelMinPoints,
			Integer nextLevelPoints,
			int currentStreakDays,
			int longestStreakDays,
			int todayPoints,
			int badgesCount,
			List<String> highlights
	) {
	}

	public record GrowthTaskResponse(
			String taskCode,
			String title,
			String description,
			int rewardPoints,
			int completedCount,
			int targetCount,
			boolean completed
	) {
	}

	public record GrowthBadgeResponse(
			String badgeKey,
			String badgeName,
			String badgeDescription,
			Instant awardedAt
	) {
	}

	public record GrowthPointLogResponse(
			String pointId,
			String actionType,
			int points,
			String summary,
			String awardedDate,
			Instant createdAt
	) {
	}

	public record GrowthChallengeResponse(
			String challengeCode,
			String category,
			String title,
			String description,
			int rewardPoints,
			int completedCount,
			int targetCount,
			String priority,
			boolean completed,
			List<String> hints
	) {
	}

	public record GrowthWeeklyPlanResponse(
			String weekStartDate,
			String weekEndDate,
			int weeklyEarnedPoints,
			int targetPoints,
			int progressPercent,
			List<GrowthChallengeResponse> challenges
	) {
	}

	public record GrowthRewardResponse(
			String rewardKey,
			String rewardName,
			String rewardDescription,
			String rewardType,
			int pointsCost,
			int remainingClaims,
			boolean claimable,
			String claimHint
	) {
	}

	public record GrowthRewardClaimResponse(
			String claimCode,
			String rewardKey,
			String rewardName,
			String rewardType,
			int pointsCost,
			int remainingPoints,
			String status,
			String claimNote,
			Instant claimedAt
	) {
	}

	public record MedicationItem(
			@NotBlank(message = "药物名称不能为空")
			String name,
			@NotBlank(message = "剂量不能为空")
			String dosage,
			@NotBlank(message = "频次不能为空")
			String frequency,
			String remark
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
}
