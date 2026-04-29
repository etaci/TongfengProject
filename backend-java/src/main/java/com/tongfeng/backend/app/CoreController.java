package com.tongfeng.backend.app;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.tongfeng.backend.app.persistence.entity.StoredFileEntity;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
public class CoreController {

	private final HealthAssistantService healthAssistantService;

	public CoreController(HealthAssistantService healthAssistantService) {
		this.healthAssistantService = healthAssistantService;
	}

	@GetMapping("/api/v1/profile")
	public ApiResponse<AppContracts.UserProfileResponse> getProfile(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getProfile(userId));
	}

	@PutMapping("/api/v1/profile")
	public ApiResponse<AppContracts.UserProfileResponse> updateProfile(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.UserProfileRequest request
	) {
		return ApiResponse.success(healthAssistantService.updateProfile(userId, request));
	}

	@PostMapping(value = "/api/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<AppContracts.FileUploadResponse> uploadFile(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestPart("file") MultipartFile file
	) {
		return ApiResponse.success(healthAssistantService.uploadFile(userId, file));
	}

	@GetMapping("/api/v1/files/{fileId}")
	public ResponseEntity<Resource> getFile(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String fileId
	) {
		StoredFileEntity storedFile = healthAssistantService.getOwnedFile(userId, fileId);
		Resource resource = healthAssistantService.loadOwnedFile(userId, fileId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(storedFile.getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedFile.getFileName() + "\"")
				.body(resource);
	}

	@PostMapping(value = "/api/v1/meals/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<AppContracts.MealAnalyzeResponse> analyzeMeal(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestPart("file") MultipartFile file,
			@RequestParam String mealType,
			@RequestParam(required = false) String takenAt,
			@RequestParam(required = false) String note
	) {
		return ApiResponse.success(healthAssistantService.analyzeMeal(userId, mealType, takenAt, note, file));
	}

	@GetMapping("/api/v1/meals")
	public ApiResponse<List<AppContracts.MealRecordResponse>> listMeals(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listMeals(userId));
	}

	@PostMapping("/api/v1/records/uric-acid")
	public ApiResponse<AppContracts.RecordSimpleResponse> addUricAcid(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.UricAcidCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.addUricAcid(userId, request));
	}

	@PostMapping("/api/v1/records/weight")
	public ApiResponse<AppContracts.RecordSimpleResponse> addWeight(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.WeightCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.addWeight(userId, request));
	}

	@PostMapping("/api/v1/records/blood-pressure")
	public ApiResponse<AppContracts.RecordSimpleResponse> addBloodPressure(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.BloodPressureCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.addBloodPressure(userId, request));
	}

	@PostMapping("/api/v1/records/flares")
	public ApiResponse<AppContracts.RecordSimpleResponse> addFlare(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.FlareCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.addFlare(userId, request));
	}

	@PostMapping("/api/v1/records/hydration")
	public ApiResponse<AppContracts.RecordSimpleResponse> addHydration(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.HydrationCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.addHydration(userId, request));
	}

	@GetMapping("/api/v1/records/uric-acid")
	public ApiResponse<List<AppContracts.UricAcidRecordResponse>> listUricAcidRecords(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listUricAcidRecords(userId));
	}

	@GetMapping("/api/v1/records/weight")
	public ApiResponse<List<AppContracts.WeightRecordResponse>> listWeightRecords(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listWeightRecords(userId));
	}

	@GetMapping("/api/v1/records/hydration")
	public ApiResponse<List<AppContracts.HydrationRecordResponse>> listHydrationRecords(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listHydrationRecords(userId));
	}

	@GetMapping("/api/v1/records/flares")
	public ApiResponse<List<AppContracts.FlareRecordResponse>> listFlareRecords(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listFlareRecords(userId));
	}

	@GetMapping("/api/v1/records/center")
	public ApiResponse<AppContracts.HealthRecordCenterResponse> getRecordCenter(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(required = false) List<String> types,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "limit 不能小于1")
			@Max(value = 100, message = "limit 不能大于100")
			int limit
	) {
		return ApiResponse.success(healthAssistantService.getRecordCenter(userId, types, limit, cursor));
	}

	@GetMapping("/api/v1/records/detail")
	public ApiResponse<AppContracts.HealthRecordDetailResponse> getRecordDetail(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam String type,
			@RequestParam String recordId
	) {
		return ApiResponse.success(healthAssistantService.getRecordDetail(userId, type, recordId));
	}

	@PutMapping("/api/v1/records/detail")
	public ApiResponse<AppContracts.HealthRecordDetailResponse> updateRecord(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam String type,
			@RequestParam String recordId,
			@Valid @RequestBody AppContracts.HealthRecordUpdateRequest request
	) {
		return ApiResponse.success(healthAssistantService.updateRecord(userId, type, recordId, request));
	}

	@GetMapping("/api/v1/records/audits")
	public ApiResponse<List<AppContracts.HealthRecordAuditResponse>> listRecordAudits(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam String type,
			@RequestParam String recordId,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "limit 涓嶈兘灏忎簬1")
			@Max(value = 50, message = "limit 涓嶈兘澶т簬50")
			int limit
	) {
		return ApiResponse.success(healthAssistantService.listRecordAudits(userId, type, recordId, limit));
	}

	@PostMapping("/api/v1/records/restore")
	public ApiResponse<AppContracts.HealthRecordRestoreResponse> restoreRecord(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam String type,
			@RequestParam String recordId,
			@RequestParam String auditId,
			@Valid @RequestBody AppContracts.HealthRecordRestoreRequest request
	) {
		return ApiResponse.success(healthAssistantService.restoreRecord(userId, type, recordId, auditId, request));
	}

	@DeleteMapping("/api/v1/records/detail")
	public ApiResponse<AppContracts.HealthRecordDeleteResponse> deleteRecord(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam String type,
			@RequestParam String recordId
	) {
		return ApiResponse.success(healthAssistantService.deleteRecord(userId, type, recordId));
	}

	@GetMapping("/api/v1/records/timeline")
	public ApiResponse<AppContracts.TimelineResponse> getTimeline(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getTimeline(userId));
	}

	@GetMapping("/api/v1/records/blood-pressure")
	public ApiResponse<List<AppContracts.BloodPressureRecordResponse>> listBloodPressureRecords(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listBloodPressureRecords(userId));
	}

	@GetMapping("/api/v1/dashboard/overview")
	public ApiResponse<AppContracts.DashboardOverviewResponse> getOverview(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getOverview(userId));
	}

	@GetMapping("/api/v1/dashboard/trends")
	public ApiResponse<AppContracts.TrendResponse> getTrends(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "7")
			@Min(value = 1, message = "days 不能小于1")
			@Max(value = 90, message = "days 不能大于90")
			int days
	) {
		return ApiResponse.success(healthAssistantService.getTrends(userId, days));
	}

	@GetMapping("/api/v1/dashboard/daily-summaries")
	public ApiResponse<List<AppContracts.DailyHealthSummaryResponse>> getDailySummaries(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "7")
			@Min(value = 1, message = "days 不能小于1")
			@Max(value = 90, message = "days 不能大于90")
			int days
	) {
		return ApiResponse.success(healthAssistantService.getDailySummaries(userId, days));
	}

	@GetMapping("/api/v1/reminders")
	public ApiResponse<List<AppContracts.ReminderResponse>> getReminders(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getReminders(userId));
	}

	@PostMapping(value = "/api/v1/lab-reports/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<AppContracts.LabReportAnalyzeResponse> analyzeLabReport(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestPart("file") MultipartFile file,
			@RequestParam(required = false) String reportDate
	) {
		return ApiResponse.success(healthAssistantService.analyzeLabReport(userId, reportDate, file));
	}

	@GetMapping("/api/v1/lab-reports")
	public ApiResponse<List<AppContracts.LabReportAnalyzeResponse>> listLabReports(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listLabReports(userId));
	}

	@PostMapping("/api/v1/knowledge/ask")
	public ApiResponse<AppContracts.KnowledgeAnswerResponse> askKnowledge(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.AskKnowledgeRequest request
	) {
		return ApiResponse.success(healthAssistantService.askKnowledge(userId, request));
	}

	@GetMapping("/api/v1/persona/summary")
	public ApiResponse<AppContracts.PersonaSummaryResponse> getPersonaSummary(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getPersonaSummary(userId));
	}

	@GetMapping("/api/v1/proactive-care/settings")
	public ApiResponse<AppContracts.ProactiveCareSettingsResponse> getProactiveCareSettings(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getProactiveCareSettings(userId));
	}

	@PutMapping("/api/v1/proactive-care/settings")
	public ApiResponse<AppContracts.ProactiveCareSettingsResponse> updateProactiveCareSettings(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.ProactiveCareSettingsRequest request
	) {
		return ApiResponse.success(healthAssistantService.updateProactiveCareSettings(userId, request));
	}

	@GetMapping("/api/v1/proactive-care/brief")
	public ApiResponse<AppContracts.ProactiveCareBriefResponse> getProactiveCareBrief(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getProactiveCareBrief(userId));
	}

	@GetMapping("/api/v1/flares/reports/latest")
	public ApiResponse<AppContracts.FlareReviewReportResponse> getLatestFlareReviewReport(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "7")
			@Min(value = 1, message = "lookbackDays 不能小于1")
			@Max(value = 30, message = "lookbackDays 不能大于30")
			int lookbackDays
	) {
		return ApiResponse.success(healthAssistantService.getLatestFlareReviewReport(userId, lookbackDays));
	}

	@PostMapping("/api/v1/family/invitations")
	public ApiResponse<AppContracts.FamilyInviteResponse> createFamilyInvite(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.FamilyInviteCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.createFamilyInvite(userId, request));
	}

	@GetMapping("/api/v1/family/invitations")
	public ApiResponse<List<AppContracts.FamilyInviteResponse>> listFamilyInvites(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listFamilyInvites(userId));
	}

	@PostMapping("/api/v1/family/invitations/{inviteCode}/accept")
	public ApiResponse<AppContracts.FamilyInviteResponse> acceptFamilyInvite(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String inviteCode
	) {
		return ApiResponse.success(healthAssistantService.acceptFamilyInvite(userId, inviteCode));
	}

	@PostMapping("/api/v1/family/invitations/{inviteCode}/cancel")
	public ApiResponse<AppContracts.FamilyInviteResponse> cancelFamilyInvite(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String inviteCode
	) {
		return ApiResponse.success(healthAssistantService.cancelFamilyInvite(userId, inviteCode));
	}

	@GetMapping("/api/v1/family/members")
	public ApiResponse<AppContracts.FamilyMembersResponse> getFamilyMembers(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getFamilyMembers(userId));
	}

	@DeleteMapping("/api/v1/family/members/{bindingCode}")
	public ApiResponse<AppContracts.FamilyBindingMemberResponse> removeFamilyBinding(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String bindingCode
	) {
		return ApiResponse.success(healthAssistantService.removeFamilyBinding(userId, bindingCode));
	}

	@GetMapping("/api/v1/family/alerts")
	public ApiResponse<List<AppContracts.FamilyAlertResponse>> getFamilyAlerts(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getFamilyAlerts(userId));
	}

	@GetMapping("/api/v1/family/patients/{patientUserId}/summary")
	public ApiResponse<AppContracts.FamilyPatientSummaryResponse> getFamilyPatientSummary(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String patientUserId
	) {
		return ApiResponse.success(healthAssistantService.getFamilyPatientSummary(userId, patientUserId));
	}

	@PostMapping("/api/v1/devices")
	public ApiResponse<AppContracts.DeviceBindingResponse> bindDevice(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.DeviceBindingCreateRequest request
	) {
		return ApiResponse.success(healthAssistantService.bindDevice(userId, request));
	}

	@GetMapping("/api/v1/devices")
	public ApiResponse<List<AppContracts.DeviceBindingResponse>> listDevices(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listDevices(userId));
	}

	@GetMapping("/api/v1/devices/catalog")
	public ApiResponse<List<AppContracts.DeviceCatalogItemResponse>> listDeviceCatalog(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.listDeviceCatalog(userId));
	}

	@GetMapping("/api/v1/devices/overview")
	public ApiResponse<AppContracts.DeviceOverviewResponse> getDeviceOverview(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getDeviceOverview(userId));
	}

	@DeleteMapping("/api/v1/devices/{deviceCode}")
	public ApiResponse<AppContracts.DeviceBindingResponse> unbindDevice(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String deviceCode
	) {
		return ApiResponse.success(healthAssistantService.unbindDevice(userId, deviceCode));
	}

	@PostMapping("/api/v1/devices/{deviceCode}/sync")
	public ApiResponse<AppContracts.DeviceSyncBatchResponse> syncDeviceData(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String deviceCode,
			@Valid @RequestBody AppContracts.DeviceSyncBatchRequest request
	) {
		return ApiResponse.success(healthAssistantService.syncDeviceData(userId, deviceCode, request));
	}

	@GetMapping("/api/v1/devices/{deviceCode}/sync-events")
	public ApiResponse<List<AppContracts.DeviceSyncResultResponse>> listDeviceSyncEvents(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String deviceCode
	) {
		return ApiResponse.success(healthAssistantService.listDeviceSyncEvents(userId, deviceCode));
	}

	@GetMapping("/api/v1/growth/overview")
	public ApiResponse<AppContracts.GrowthOverviewResponse> getGrowthOverview(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getGrowthOverview(userId));
	}

	@GetMapping("/api/v1/growth/tasks")
	public ApiResponse<List<AppContracts.GrowthTaskResponse>> getGrowthTasks(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getGrowthTasks(userId));
	}

	@GetMapping("/api/v1/growth/weekly-plan")
	public ApiResponse<AppContracts.GrowthWeeklyPlanResponse> getGrowthWeeklyPlan(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getGrowthWeeklyPlan(userId));
	}

	@GetMapping("/api/v1/growth/rewards")
	public ApiResponse<List<AppContracts.GrowthRewardResponse>> getGrowthRewards(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getGrowthRewards(userId));
	}

	@PostMapping("/api/v1/growth/rewards/{rewardKey}/claim")
	public ApiResponse<AppContracts.GrowthRewardClaimResponse> claimGrowthReward(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String rewardKey
	) {
		return ApiResponse.success(healthAssistantService.claimGrowthReward(userId, rewardKey));
	}

	@GetMapping("/api/v1/growth/reward-claims")
	public ApiResponse<List<AppContracts.GrowthRewardClaimResponse>> getGrowthRewardClaims(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getGrowthRewardClaims(userId));
	}

	@GetMapping("/api/v1/growth/points")
	public ApiResponse<List<AppContracts.GrowthPointLogResponse>> getGrowthPointLogs(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "20")
			@Min(value = 1, message = "limit 不能小于1")
			@Max(value = 50, message = "limit 不能大于50")
			int limit
	) {
		return ApiResponse.success(healthAssistantService.getGrowthPointLogs(userId, limit));
	}

	@GetMapping("/api/v1/growth/badges")
	public ApiResponse<List<AppContracts.GrowthBadgeResponse>> getGrowthBadges(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getGrowthBadges(userId));
	}

	@GetMapping("/api/v1/medications")
	public ApiResponse<AppContracts.MedicationPlanResponse> getMedicationPlan(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getMedicationPlan(userId));
	}

	@PutMapping("/api/v1/medications")
	public ApiResponse<AppContracts.MedicationPlanResponse> updateMedicationPlan(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.MedicationPlanRequest request
	) {
		return ApiResponse.success(healthAssistantService.updateMedicationPlan(userId, request));
	}
}
