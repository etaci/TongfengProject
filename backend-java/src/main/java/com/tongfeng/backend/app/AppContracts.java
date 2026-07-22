package com.tongfeng.backend.app;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

	public record VerificationCodeRequest(
			@NotBlank(message = "用途不能为空")
			String purpose,
			@NotBlank(message = "账号类型不能为空")
			String accountType,
			@NotBlank(message = "账号不能为空")
			String account
	) {
	}

	public record VerificationCodeConfirmRequest(
			@NotBlank(message = "验证码不能为空")
			String verificationCode
	) {
	}

	public record PasswordResetConfirmRequest(
			@NotBlank(message = "账号类型不能为空")
			String accountType,
			@NotBlank(message = "账号不能为空")
			String account,
			@NotBlank(message = "验证码不能为空")
			String verificationCode,
			@NotBlank(message = "新密码不能为空")
			String newPassword,
			@NotBlank(message = "确认密码不能为空")
			String confirmPassword
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
			String deviceLabel,
			String clientIpMasked,
			String loginRiskLevel,
			List<String> securityNotices,
			boolean accountVerified,
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
			String deviceLabel,
			String clientIpMasked,
			String loginRiskLevel,
			List<String> securityNotices,
			boolean accountVerified,
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

	public record ErrorCodeItemResponse(
			String code,
			int httpStatus,
			String httpStatusText,
			String category,
			boolean retryable,
			String defaultMessage
	) {
	}

	public record ErrorCodeCatalogResponse(
			String version,
			Instant generatedAt,
			List<ErrorCodeItemResponse> items
	) {
	}

	public record AuthActiveSessionResponse(
			String sessionCode,
			String authMode,
			String accountType,
			String accountIdentifier,
			String deviceLabel,
			String clientIpMasked,
			String loginRiskLevel,
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

	public record VerificationChallengeResponse(
			String challengeCode,
			String purpose,
			String accountType,
			String maskedTarget,
			String deliveryChannel,
			String deliveryProvider,
			String deliveryStatus,
			Instant expiresAt,
			String simulatedCode,
			String message
	) {
	}

	public record AccountVerificationStatusResponse(
			String accountType,
			String accountIdentifier,
			boolean verified,
			Instant verifiedAt,
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
			String changeReason,
			Instant effectiveAt,
			Instant createdAt
	) {
	}

	public record PrivacyAuthorizationWithdrawalRequest(
			@NotEmpty(message = "请至少选择一项要撤回的授权")
			List<@NotBlank String> scopes,
			@NotBlank(message = "请填写授权撤回原因")
			@Size(max = 200, message = "授权撤回原因不能超过 200 个字符")
			String reason
	) {
	}

	public record AccountDeletionRequest(
			@NotBlank(message = "请输入删除确认文本")
			String confirmation,
			@Size(max = 200, message = "注销原因不能超过 200 个字符")
			String reason
	) {
	}

	public record AccountDeletionResponse(
			String deletionReceipt,
			String status,
			int deletedDatabaseRows,
			int scheduledPhysicalFiles,
			Instant completedAt,
			String message
	) {
	}

	public record PrivacyNoticeResponse(
			String version,
			Instant effectiveAt,
			List<String> collectedData,
			List<String> purposes,
			List<String> userRights,
			List<String> thirdPartyProcessors,
			String retentionPolicy,
			String medicalBoundary
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
			List<FeatureStatusResponse> features,
			String capabilityVersion,
			Instant generatedAt
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
			String summary,
			String analysisMode,
			List<String> trustNotes
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
			String triageDecisionCode,
			String triageRuleVersion,
			String triageVerificationStatus,
			List<String> triageRedFlags,
			String triageTitle,
			String triageSummary,
			String nextStep,
			List<String> reasons,
			List<TodayActionItemResponse> actions,
			List<String> trustNotes,
			Instant generatedAt
	) {
	}

	public record GoutFlareTriageRequest(
			@NotNull(message = "起病时间不能为空")
			@PastOrPresent(message = "起病时间不能晚于当前时间")
			Instant onsetAt,
			@NotBlank(message = "关节位置不能为空")
			@Size(max = 128, message = "关节位置不能超过 128 个字符")
			String jointLocation,
			@Min(value = 0, message = "疼痛等级不能小于 0")
			@Max(value = 10, message = "疼痛等级不能大于 10")
			int painLevel,
			@NotNull(message = "请确认是否红肿")
			Boolean rednessOrSwelling,
			@NotNull(message = "请确认是否发热")
			Boolean fever,
			@NotNull(message = "请确认是否可以负重")
			Boolean canBearWeight,
			@NotNull(message = "请确认近期是否调整用药")
			Boolean recentMedicationChange,
			@NotNull(message = "请确认是否有外伤史")
			Boolean traumaHistory,
			@NotNull(message = "请确认是否首次发作")
			Boolean firstEpisode,
			@NotNull(message = "请确认是否有全身不适症状")
			Boolean systemicSymptoms
	) {
	}

	public record ClinicalSourceReference(
			String sourceCode,
			String title,
			String url
	) {
	}

	public record GoutFlareTriageResponse(
			String decisionCode,
			String triageCode,
			RiskLevel triageLevel,
			String summary,
			List<String> reasons,
			List<String> redFlags,
			List<String> nextActions,
			String ruleVersion,
			List<ClinicalSourceReference> sourceReferences,
			String verificationStatus,
			Instant generatedAt,
			String disclaimer
	) {
	}

	public record DoctorVisitLabSummary(
			String reportId,
			LocalDate reportDate,
			RiskLevel riskLevel,
			String verificationStatus,
			boolean readyForClinicalReview,
			String sourceType
	) {
	}

	public record DoctorVisitPackageResponse(
			String packageCode,
			int lookbackDays,
			String patientName,
			Integer targetUricAcid,
			List<TrendPoint> uricAcidTrend,
			List<FlareRecordResponse> flareRecords,
			MedicationWeeklyReportResponse medicationAdherence,
			List<DoctorVisitLabSummary> labReports,
			GoutFlareTriageResponse latestTriage,
			List<String> questionsForDoctor,
			List<String> dataSources,
			List<String> trustNotes,
			Instant generatedAt
	) {
	}

	public record DoctorVisitShareCreateRequest(
			@Min(value = 30, message = "就诊包统计周期至少为 30 天")
			@Max(value = 90, message = "就诊包统计周期最多为 90 天")
			Integer lookbackDays,
			@Min(value = 1, message = "分享有效期至少为 1 小时")
			@Max(value = 168, message = "分享有效期最多为 168 小时")
			Integer expiresInHours
	) {
	}

	public record DoctorVisitShareResponse(
			String shareCode,
			String shareToken,
			String sharePath,
			int lookbackDays,
			Instant expiresAt,
			boolean revoked,
			Instant createdAt
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
			RiskLevel riskLevel,
			String triageCode,
			String ruleVersion,
			String decisionCode,
			String verificationStatus,
			List<String> redFlags,
			List<String> nextActions
	) {
		public TimelineEvent(
				String eventId,
				String type,
				String title,
				String detail,
				Instant occurredAt,
				RiskLevel riskLevel
		) {
			this(eventId, type, title, detail, occurredAt, riskLevel, null, null, null, null, List.of(), List.of());
		}
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
			boolean manualConfirmationRequired,
			boolean reviewReady,
			String extractionStatus,
			List<String> suggestions,
			List<String> trustNotes,
			String summary,
			String analysisMode
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

	public record LabReportTrustTimelineItemResponse(
			String eventKey,
			String title,
			String detail,
			String status,
			Instant occurredAt
	) {
	}

	public record LabIndicatorTrustItemResponse(
			String code,
			String name,
			String sourceType,
			String verificationStatus,
			Integer confidenceScore,
			String confidenceLabel,
			String note
	) {
	}

	public record LabReportTrustMetaResponse(
			String documentSourceType,
			String documentSourceLabel,
			boolean originalFileAttached,
			String originalFileName,
			Instant uploadedAt,
			String institutionSourceLabel,
			boolean institutionVerified,
			String verificationStage,
			List<String> lockedSections,
			Instant manualConfirmedAt,
			List<LabReportTrustTimelineItemResponse> confirmationHistory,
			List<LabIndicatorTrustItemResponse> fieldConfidenceItems
	) {
	}

	public record LabDoctorSummaryResponse(
			boolean readyToShare,
			String shareTitle,
			String shareSummary,
			List<String> keyFindings,
			List<String> careRequests,
			List<String> trustNotes
	) {
	}

	public record LabReportReviewResponse(
			String reportId,
			LocalDate reportDate,
			RiskLevel overallRiskLevel,
			boolean manualConfirmationRequired,
			boolean reviewReady,
			String reviewStatus,
			String reviewSummary,
			String workflowTitle,
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
			List<TodayActionItemResponse> manualConfirmationTasks,
			List<String> blockedOutputs,
			List<String> nextActions,
			List<String> trustNotes,
			LabReportTrustMetaResponse trustMeta,
			LabDoctorSummaryResponse doctorSummary,
			Instant generatedAt
	) {
	}

	public record LabManualIndicatorRequest(
			@NotBlank(message = "指标编码不能为空")
			String code,
			@NotBlank(message = "指标名称不能为空")
			String name,
			@NotNull(message = "指标数值不能为空")
			@Positive(message = "指标数值必须大于 0")
			BigDecimal value,
			@NotBlank(message = "指标单位不能为空")
			String unit,
			String referenceRange,
			@NotNull(message = "请确认指标风险等级")
			RiskLevel riskLevel
	) {
	}

	public record LabReportManualConfirmRequest(
			@NotEmpty(message = "请至少补录一个关键指标")
			List<@Valid LabManualIndicatorRequest> indicators,
			String summaryNote
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

	public record AccessRoleResponse(
			String roleKey,
			String displayName,
			List<String> capabilities
	) {
	}

	public record AccessRuleResponse(
			String roleKey,
			String resourceType,
			String actionType,
			String condition
	) {
	}

	public record AccessBoundaryResponse(
			String boundaryName,
			String description
	) {
	}

	public record AccessPolicyResponse(
			String policyVersion,
			String tokenStrategy,
			String tokenStrategyNote,
			List<AccessRoleResponse> roles,
			List<AccessRuleResponse> rules,
			List<AccessBoundaryResponse> boundaries,
			Instant generatedAt
	) {
	}

	public record AccessAuditResponse(
			String auditCode,
			String actorUserId,
			String actorRole,
			String patientUserId,
			String resourceType,
			String resourceId,
			String actionType,
			String decision,
			String bindingCode,
			String sessionCode,
			String reason,
			Instant operatedAt
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
