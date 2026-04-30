package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tongfeng.backend.app.persistence.entity.AuthSessionEntity;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MedicationPlanEntity;
import com.tongfeng.backend.app.persistence.entity.StoredFileEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UserAccountEntity;
import com.tongfeng.backend.app.persistence.entity.UserProfileEntity;
import com.tongfeng.backend.app.persistence.entity.WeightRecordEntity;
import com.tongfeng.backend.app.persistence.repo.AuthSessionRepository;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MedicationPlanRepository;
import com.tongfeng.backend.app.persistence.repo.StoredFileRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UserAccountRepository;
import com.tongfeng.backend.app.persistence.repo.UserProfileRepository;
import com.tongfeng.backend.app.persistence.repo.WeightRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class HealthAssistantService {

	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};
	private static final TypeReference<List<AppContracts.MealItem>> MEAL_ITEM_LIST_TYPE = new TypeReference<>() {
	};
	private static final TypeReference<List<AppContracts.LabIndicator>> LAB_INDICATOR_LIST_TYPE = new TypeReference<>() {
	};
	private static final TypeReference<List<AppContracts.MedicationItem>> MEDICATION_ITEM_LIST_TYPE = new TypeReference<>() {
	};

	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;
	private final AuthSessionRepository authSessionRepository;
	private final StoredFileRepository storedFileRepository;
	private final MealRecordRepository mealRecordRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final WeightRecordRepository weightRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final LabReportRecordRepository labReportRecordRepository;
	private final MedicationPlanRepository medicationPlanRepository;
	private final LocalFileStorageService localFileStorageService;
	private final AiServiceClient aiServiceClient;
	private final AppProperties appProperties;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;
	private final SessionCacheService sessionCacheService;
	private final HealthRuleEngineService healthRuleEngineService;
	private final ProactiveCareService proactiveCareService;
	private final FamilyCareService familyCareService;
	private final RecordCenterService recordCenterService;
	private final UricAcidAnalysisService uricAcidAnalysisService;
	private final MvpMetricsService mvpMetricsService;

	public HealthAssistantService(
			UserAccountRepository userAccountRepository,
			UserProfileRepository userProfileRepository,
			AuthSessionRepository authSessionRepository,
			StoredFileRepository storedFileRepository,
			MealRecordRepository mealRecordRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			WeightRecordRepository weightRecordRepository,
			FlareRecordRepository flareRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			LabReportRecordRepository labReportRecordRepository,
			MedicationPlanRepository medicationPlanRepository,
			LocalFileStorageService localFileStorageService,
			AiServiceClient aiServiceClient,
			AppProperties appProperties,
			IdGenerator idGenerator,
			JsonCodec jsonCodec,
			SessionCacheService sessionCacheService,
			HealthRuleEngineService healthRuleEngineService,
			ProactiveCareService proactiveCareService,
			FamilyCareService familyCareService,
			RecordCenterService recordCenterService,
			UricAcidAnalysisService uricAcidAnalysisService,
			MvpMetricsService mvpMetricsService
	) {
		this.userAccountRepository = userAccountRepository;
		this.userProfileRepository = userProfileRepository;
		this.authSessionRepository = authSessionRepository;
		this.storedFileRepository = storedFileRepository;
		this.mealRecordRepository = mealRecordRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.weightRecordRepository = weightRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.labReportRecordRepository = labReportRecordRepository;
		this.medicationPlanRepository = medicationPlanRepository;
		this.localFileStorageService = localFileStorageService;
		this.aiServiceClient = aiServiceClient;
		this.appProperties = appProperties;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
		this.sessionCacheService = sessionCacheService;
		this.healthRuleEngineService = healthRuleEngineService;
		this.proactiveCareService = proactiveCareService;
		this.familyCareService = familyCareService;
		this.recordCenterService = recordCenterService;
		this.uricAcidAnalysisService = uricAcidAnalysisService;
		this.mvpMetricsService = mvpMetricsService;
	}

	@Transactional
	public AppContracts.AuthTokenResponse mockLogin(AppContracts.MockLoginRequest request) {
		String userId = idGenerator.next("user");
		Instant now = Instant.now();
		UserAccountEntity accountEntity = new UserAccountEntity();
		accountEntity.setUserCode(userId);
		accountEntity.setNickname(request.nickname());
		accountEntity.setCreatedAt(now);
		accountEntity.setUpdatedAt(now);
		userAccountRepository.save(accountEntity);

		UserProfileEntity profileEntity = new UserProfileEntity();
		profileEntity.setUserCode(userId);
		profileEntity.setName(request.nickname());
		profileEntity.setGender("UNKNOWN");
		profileEntity.setTargetUricAcid(360);
		profileEntity.setAllergiesJson(jsonCodec.toJson(List.of()));
		profileEntity.setComorbiditiesJson(jsonCodec.toJson(List.of()));
		profileEntity.setUpdatedAt(now);
		userProfileRepository.save(profileEntity);

		String token = UUID.randomUUID().toString().replace("-", "");
		Instant expiresAt = now.plus(appProperties.getAuthTokenDays(), ChronoUnit.DAYS);
		AuthSessionEntity sessionEntity = new AuthSessionEntity();
		sessionEntity.setUserCode(userId);
		sessionEntity.setNickname(request.nickname());
		sessionEntity.setToken(token);
		sessionEntity.setExpiresAt(expiresAt);
		sessionEntity.setCreatedAt(now);
		authSessionRepository.save(sessionEntity);
		sessionCacheService.put(new UserSession(userId, request.nickname(), token, expiresAt));
		return new AppContracts.AuthTokenResponse(userId, request.nickname(), token, "Bearer", expiresAt);
	}

	public UserSession requireSession(String token) {
		if (!StringUtils.hasText(token)) {
			throw new BusinessException("UNAUTHORIZED", "缺少登录凭证");
		}
		Optional<UserSession> cachedSession = sessionCacheService.get(token);
		if (cachedSession.isPresent()) {
			UserSession session = cachedSession.get();
			if (session.expiresAt().isAfter(Instant.now())) {
				return session;
			}
			sessionCacheService.evict(token);
		}

		AuthSessionEntity sessionEntity = authSessionRepository.findByToken(token)
				.orElseThrow(() -> new BusinessException("UNAUTHORIZED", "登录已失效，请重新登录"));
		if (sessionEntity.getExpiresAt().isBefore(Instant.now())) {
			sessionCacheService.evict(token);
			throw new BusinessException("UNAUTHORIZED", "登录已失效，请重新登录");
		}
		UserSession session = new UserSession(
				sessionEntity.getUserCode(),
				sessionEntity.getNickname(),
				sessionEntity.getToken(),
				sessionEntity.getExpiresAt()
		);
		sessionCacheService.put(session);
		return session;
	}

	public AppContracts.UserProfileResponse getProfile(String userId) {
		return toProfileResponse(ensureProfile(userId));
	}

	@Transactional
	public AppContracts.UserProfileResponse updateProfile(String userId, AppContracts.UserProfileRequest request) {
		UserProfileEntity profileEntity = ensureProfile(userId);
		profileEntity.setName(request.name());
		profileEntity.setGender(request.gender());
		profileEntity.setBirthday(request.birthday());
		profileEntity.setHeightCm(request.heightCm());
		if (request.targetUricAcid() != null) {
			profileEntity.setTargetUricAcid(request.targetUricAcid());
		}
		profileEntity.setAllergiesJson(jsonCodec.toJson(safeList(request.allergies())));
		profileEntity.setComorbiditiesJson(jsonCodec.toJson(safeList(request.comorbidities())));
		profileEntity.setEmergencyContact(request.emergencyContact());
		profileEntity.setUpdatedAt(Instant.now());
		userProfileRepository.save(profileEntity);
		return toProfileResponse(profileEntity);
	}

	@Transactional
	public AppContracts.FileUploadResponse uploadFile(String userId, MultipartFile file) {
		StoredFileEntity storedFileEntity = persistStoredFile(userId, file);
		return toFileUploadResponse(storedFileEntity);
	}

	public StoredFileEntity getOwnedFile(String userId, String fileId) {
		StoredFileEntity storedFileEntity = storedFileRepository.findByFileCode(fileId)
				.orElseThrow(() -> new BusinessException("FILE_NOT_FOUND", "文件不存在"));
		if (!Objects.equals(storedFileEntity.getUserCode(), userId)) {
			throw new BusinessException("FORBIDDEN", "无权访问该文件");
		}
		return storedFileEntity;
	}

	public org.springframework.core.io.Resource loadOwnedFile(String userId, String fileId) {
		StoredFileEntity storedFileEntity = getOwnedFile(userId, fileId);
		return localFileStorageService.loadAsResource(storedFileEntity.getRelativePath());
	}

	@Transactional
	public AppContracts.MealAnalyzeResponse analyzeMeal(
			String userId,
			String mealType,
			String takenAt,
			String note,
			MultipartFile file
	) {
		StoredFileEntity storedFileEntity = persistStoredFile(userId, file);
		AiServiceClient.MealAiResult aiResult = aiServiceClient.analyzeMeal(userId, mealType, note, file);
		MealRecordEntity recordEntity = new MealRecordEntity();
		recordEntity.setRecordCode(idGenerator.next("meal"));
		recordEntity.setUserCode(userId);
		recordEntity.setMealType(StringUtils.hasText(mealType) ? mealType : "MEAL");
		recordEntity.setTakenAt(parseInstantOrNow(takenAt));
		recordEntity.setNoteText(note);
		recordEntity.setFileCode(storedFileEntity.getFileCode());
		recordEntity.setRiskLevel(aiResult.overallRiskLevel().name());
		recordEntity.setPurineEstimateMg(aiResult.purineEstimateMg());
		recordEntity.setItemsJson(jsonCodec.toJson(safeMealItems(aiResult.items())));
		recordEntity.setSuggestionsJson(jsonCodec.toJson(safeList(aiResult.suggestions())));
		recordEntity.setSummaryText(aiResult.summary());
		mealRecordRepository.save(recordEntity);
		refreshInsightState(userId);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_MEAL_ANALYZED,
				"analysis",
				recordEntity.getTakenAt(),
				eventPayload(
						"recordId", recordEntity.getRecordCode(),
						"mealType", recordEntity.getMealType(),
						"riskLevel", recordEntity.getRiskLevel()
				)
		);
		return new AppContracts.MealAnalyzeResponse(
				recordEntity.getRecordCode(),
				buildFileAccessUrl(storedFileEntity.getFileCode()),
				recordEntity.getMealType(),
				recordEntity.getTakenAt(),
				toRiskLevel(recordEntity.getRiskLevel()),
				recordEntity.getPurineEstimateMg(),
				readMealItems(recordEntity.getItemsJson()),
				readStringList(recordEntity.getSuggestionsJson()),
				recordEntity.getSummaryText()
		);
	}

	public List<AppContracts.MealRecordResponse> listMeals(String userId) {
		return mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream()
				.map(record -> new AppContracts.MealRecordResponse(
						record.getRecordCode(),
						record.getMealType(),
						record.getTakenAt(),
						toRiskLevel(record.getRiskLevel()),
						record.getPurineEstimateMg(),
						buildFileAccessUrl(record.getFileCode()),
						record.getSummaryText()
				))
				.toList();
	}

	public List<AppContracts.UricAcidRecordResponse> listUricAcidRecords(String userId) {
		return recordCenterService.listUricAcidRecords(userId);
	}

	public List<AppContracts.WeightRecordResponse> listWeightRecords(String userId) {
		return recordCenterService.listWeightRecords(userId);
	}

	public List<AppContracts.HydrationRecordResponse> listHydrationRecords(String userId) {
		return recordCenterService.listHydrationRecords(userId);
	}

	public List<AppContracts.FlareRecordResponse> listFlareRecords(String userId) {
		return recordCenterService.listFlareRecords(userId);
	}

	@Transactional
	public AppContracts.RecordSimpleResponse addUricAcid(String userId, AppContracts.UricAcidCreateRequest request) {
		UricAcidRecordEntity entity = new UricAcidRecordEntity();
		entity.setRecordCode(idGenerator.next("ua"));
		entity.setUserCode(userId);
		entity.setUaValue(request.value());
		entity.setUaUnit(request.unit());
		entity.setMeasuredAt(request.measuredAt() == null ? Instant.now() : request.measuredAt());
		entity.setSourceName(request.source());
		entity.setNoteText(request.note());
		uricAcidRecordRepository.save(entity);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_URIC_ACID_RECORDED,
				"records",
				entity.getMeasuredAt(),
				eventPayload(
						"recordId", entity.getRecordCode(),
						"value", entity.getUaValue(),
						"unit", entity.getUaUnit(),
						"source", entity.getSourceName()
				)
		);
		refreshInsightState(userId);
		return new AppContracts.RecordSimpleResponse(entity.getRecordCode(), entity.getMeasuredAt(), "尿酸记录已保存");
	}

	@Transactional
	public AppContracts.RecordSimpleResponse addWeight(String userId, AppContracts.WeightCreateRequest request) {
		WeightRecordEntity entity = new WeightRecordEntity();
		entity.setRecordCode(idGenerator.next("weight"));
		entity.setUserCode(userId);
		entity.setWeightValue(request.value());
		entity.setMeasuredAt(request.measuredAt() == null ? Instant.now() : request.measuredAt());
		entity.setSourceName(request.source());
		entity.setNoteText(request.note());
		weightRecordRepository.save(entity);
		refreshInsightState(userId);
		return new AppContracts.RecordSimpleResponse(entity.getRecordCode(), entity.getMeasuredAt(), "体重记录已保存");
	}

	@Transactional
	public AppContracts.RecordSimpleResponse addFlare(String userId, AppContracts.FlareCreateRequest request) {
		return addFlareInternal(userId, request);
	}

	private AppContracts.RecordSimpleResponse addFlareInternal(String userId, AppContracts.FlareCreateRequest request) {
		FlareRecordEntity entity = new FlareRecordEntity();
		entity.setRecordCode(idGenerator.next("flare"));
		entity.setUserCode(userId);
		entity.setJointName(request.joint());
		entity.setPainLevel(request.painLevel());
		entity.setStartedAt(request.startedAt() == null ? Instant.now() : request.startedAt());
		entity.setDurationNote(request.durationNote());
		entity.setNoteText(request.note());
		flareRecordRepository.save(entity);
		refreshInsightState(userId);
		return new AppContracts.RecordSimpleResponse(entity.getRecordCode(), entity.getStartedAt(), "发作记录已保存");
	}

	@Transactional
	public AppContracts.RecordSimpleResponse addHydration(String userId, AppContracts.HydrationCreateRequest request) {
		HydrationRecordEntity entity = new HydrationRecordEntity();
		entity.setRecordCode(idGenerator.next("hydration"));
		entity.setUserCode(userId);
		entity.setWaterIntakeMl(request.waterIntakeMl());
		entity.setUrineColorLevel(request.urineColorLevel());
		entity.setCheckedAt(request.checkedAt() == null ? Instant.now() : request.checkedAt());
		entity.setNoteText(request.note());
		hydrationRecordRepository.save(entity);
		refreshInsightState(userId);
		return new AppContracts.RecordSimpleResponse(entity.getRecordCode(), entity.getCheckedAt(), "饮水/尿液打卡已保存");
	}

	public AppContracts.HealthRecordCenterResponse getRecordCenter(String userId, List<String> types, int limit, String cursor) {
		return recordCenterService.getRecordCenter(userId, types, limit, cursor);
	}

	public AppContracts.HealthRecordDetailResponse getRecordDetail(String userId, String type, String recordId) {
		return recordCenterService.getRecordDetail(userId, type, recordId);
	}

	@Transactional
	public AppContracts.HealthRecordDetailResponse updateRecord(
			String userId,
			String type,
			String recordId,
			AppContracts.HealthRecordUpdateRequest request
	) {
		return recordCenterService.updateRecord(userId, type, recordId, request);
	}

	public List<AppContracts.HealthRecordAuditResponse> listRecordAudits(
			String userId,
			String type,
			String recordId,
			int limit
	) {
		return recordCenterService.listRecordAudits(userId, type, recordId, limit);
	}

	@Transactional
	public AppContracts.HealthRecordRestoreResponse restoreRecord(
			String userId,
			String type,
			String recordId,
			String auditId,
			AppContracts.HealthRecordRestoreRequest request
	) {
		return recordCenterService.restoreRecord(userId, type, recordId, auditId, request);
	}

	@Transactional
	public AppContracts.HealthRecordDeleteResponse deleteRecord(String userId, String type, String recordId) {
		return recordCenterService.deleteRecord(userId, type, recordId);
	}

	public AppContracts.DashboardOverviewResponse getOverview(String userId) {
		List<AppContracts.ReminderResponse> reminders = getReminders(userId);
		List<AppContracts.DailyHealthSummaryResponse> summaries = getDailySummaries(userId, 1);
		AppContracts.DailyHealthSummaryResponse todaySummary = summaries.isEmpty()
				? null
				: summaries.getFirst();
		List<MealRecordEntity> meals = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId);
		String latestRiskSummary = todaySummary != null
				? todaySummary.summaryText()
				: meals.stream()
						.findFirst()
						.map(MealRecordEntity::getSummaryText)
						.orElse("暂无风险分析，建议先完成饮食拍照或关键指标录入");
		int highRiskMeals = todaySummary != null
				? defaultInt(todaySummary.highRiskMealCount())
				: (int) meals.stream()
						.filter(record -> toRiskLevel(record.getRiskLevel()) == AppContracts.RiskLevel.RED)
						.count();
		List<String> focus = reminders.stream()
				.limit(3)
				.map(AppContracts.ReminderResponse::content)
				.toList();
		return new AppContracts.DashboardOverviewResponse(
				userId,
				"主动管理增强版",
				meals.size(),
				highRiskMeals,
				uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).size(),
				flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).size(),
				latestRiskSummary,
				focus,
				reminders
		);
	}

	public AppContracts.MvpMetricsSummaryResponse getMvpMetricsSummary(String userId, int days) {
		ensureProfile(userId);
		return mvpMetricsService.getSummary(days);
	}

	public AppContracts.TrendResponse getTrends(String userId, int days) {
		Instant begin = Instant.now().minus(Math.max(days, 1), ChronoUnit.DAYS);
		List<AppContracts.TrendPoint> uricAcid = uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.filter(record -> !record.getMeasuredAt().isBefore(begin))
				.sorted(Comparator.comparing(UricAcidRecordEntity::getMeasuredAt))
				.map(record -> new AppContracts.TrendPoint(
						record.getMeasuredAt().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
						BigDecimal.valueOf(record.getUaValue()),
						record.getUaUnit()
				))
				.toList();
		List<AppContracts.TrendPoint> weight = weightRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.filter(record -> !record.getMeasuredAt().isBefore(begin))
				.sorted(Comparator.comparing(WeightRecordEntity::getMeasuredAt))
				.map(record -> new AppContracts.TrendPoint(
						record.getMeasuredAt().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
						record.getWeightValue(),
						"kg"
				))
				.toList();
		List<AppContracts.TrendPoint> hydration = hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream()
				.filter(record -> !record.getCheckedAt().isBefore(begin))
				.sorted(Comparator.comparing(HydrationRecordEntity::getCheckedAt))
				.map(record -> new AppContracts.TrendPoint(
						record.getCheckedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
						BigDecimal.valueOf(record.getWaterIntakeMl()),
						"ml"
				))
				.toList();
		return new AppContracts.TrendResponse(uricAcid, weight, hydration);
	}

	public AppContracts.TimelineResponse getTimeline(String userId) {
		List<AppContracts.TimelineEvent> events = new ArrayList<>();
		mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).forEach(record -> events.add(
				new AppContracts.TimelineEvent(
						record.getRecordCode(),
						"MEAL",
						"饮食拍照识别",
						record.getSummaryText(),
						record.getTakenAt(),
						toRiskLevel(record.getRiskLevel())
				)
		));
		uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).forEach(record -> events.add(
				new AppContracts.TimelineEvent(
						record.getRecordCode(),
						"URIC_ACID",
						"尿酸记录",
						record.getUaValue() + " " + record.getUaUnit(),
						record.getMeasuredAt(),
						uricAcidRisk(record.getUaValue())
				)
		));
		weightRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).forEach(record -> events.add(
				new AppContracts.TimelineEvent(
						record.getRecordCode(),
						"WEIGHT",
						"体重记录",
						record.getWeightValue() + " kg",
						record.getMeasuredAt(),
						AppContracts.RiskLevel.GREEN
				)
		));
		flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).forEach(record -> events.add(
				new AppContracts.TimelineEvent(
						record.getRecordCode(),
						"FLARE",
						"发作记录",
						record.getJointName() + " 疼痛等级 " + record.getPainLevel(),
						record.getStartedAt(),
						record.getPainLevel() >= 8 ? AppContracts.RiskLevel.RED : AppContracts.RiskLevel.YELLOW
				)
		));
		hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).forEach(record -> events.add(
				new AppContracts.TimelineEvent(
						record.getRecordCode(),
						"HYDRATION",
						"饮水/尿液打卡",
						record.getWaterIntakeMl() + "ml / 尿液颜色 " + record.getUrineColorLevel() + " 级",
						record.getCheckedAt(),
						record.getUrineColorLevel() >= 4 ? AppContracts.RiskLevel.YELLOW : AppContracts.RiskLevel.GREEN
				)
		));
		labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId).forEach(record -> events.add(
				new AppContracts.TimelineEvent(
						record.getReportCode(),
						"LAB_REPORT",
						"化验单解析",
						record.getSummaryText(),
						record.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant(),
						toRiskLevel(record.getOverallRiskLevel())
				)
		));
		events.sort(Comparator.comparing(AppContracts.TimelineEvent::occurredAt).reversed());
		return new AppContracts.TimelineResponse(events);
	}

	public AppContracts.UricAcidCauseAnalysisResponse getLatestUricAcidCauseAnalysis(String userId, int lookbackDays) {
		ensureProfile(userId);
		return uricAcidAnalysisService.analyzeLatest(userId, lookbackDays);
	}

	public List<AppContracts.ReminderResponse> getReminders(String userId) {
		return healthRuleEngineService.getActiveReminders(userId);
	}

	@Transactional
	public AppContracts.LabReportAnalyzeResponse analyzeLabReport(String userId, String reportDate, MultipartFile file) {
		LocalDate finalReportDate = parseDateOrToday(reportDate);
		StoredFileEntity storedFileEntity = persistStoredFile(userId, file);
		AiServiceClient.LabAiResult aiResult = aiServiceClient.analyzeLabReport(userId, finalReportDate, file);
		LabReportRecordEntity entity = new LabReportRecordEntity();
		entity.setReportCode(idGenerator.next("lab"));
		entity.setUserCode(userId);
		entity.setFileCode(storedFileEntity.getFileCode());
		entity.setReportDate(finalReportDate);
		entity.setIndicatorsJson(jsonCodec.toJson(safeIndicators(aiResult.indicators())));
		entity.setOverallRiskLevel(aiResult.overallRiskLevel().name());
		entity.setSuggestionsJson(jsonCodec.toJson(safeList(aiResult.suggestions())));
		entity.setSummaryText(aiResult.summary());
		labReportRecordRepository.save(entity);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_LAB_REPORT_ANALYZED,
				"assistant",
				entity.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant(),
				eventPayload(
						"reportCode", entity.getReportCode(),
						"riskLevel", entity.getOverallRiskLevel(),
						"indicatorCount", safeIndicators(aiResult.indicators()).size()
				)
		);
		refreshInsightState(userId);
		return toLabReportResponse(entity);
	}

	public List<AppContracts.LabReportAnalyzeResponse> listLabReports(String userId) {
		return labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId).stream()
				.map(this::toLabReportResponse)
				.toList();
	}

	@Transactional
	public AppContracts.KnowledgeAnswerResponse askKnowledge(String userId, AppContracts.AskKnowledgeRequest request) {
		ensureProfile(userId);
		return aiServiceClient.askKnowledge(request.question(), request.scene());
	}

	public AppContracts.PersonaSummaryResponse getPersonaSummary(String userId) {
		List<String> tags = new ArrayList<>();
		List<String> triggers = new ArrayList<>();

		latestUricAcid(userId).ifPresent(record -> {
			if (record.getUaValue() > 480) {
				tags.add("近期尿酸明显偏高");
			} else if (record.getUaValue() > 420) {
				tags.add("近期尿酸轻度偏高");
			}
		});

		latestHydration(userId).ifPresent(record -> {
			if (record.getWaterIntakeMl() < 1800 || record.getUrineColorLevel() >= 4) {
				tags.add("饮水不足倾向");
			}
		});

		List<MealRecordEntity> meals = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId);
		long redMeals = meals.stream()
				.filter(record -> toRiskLevel(record.getRiskLevel()) == AppContracts.RiskLevel.RED)
				.count();
		if (redMeals >= 2) {
			tags.add("高风险饮食暴露频繁");
		}

		Map<String, Long> itemFrequency = meals.stream()
				.flatMap(record -> readMealItems(record.getItemsJson()).stream())
				.filter(item -> item.riskLevel() != AppContracts.RiskLevel.GREEN)
				.collect(Collectors.groupingBy(AppContracts.MealItem::name, Collectors.counting()));
		triggers.addAll(itemFrequency.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.limit(3)
				.map(Map.Entry::getKey)
				.toList());

		if (tags.isEmpty()) {
			tags.add("数据积累中");
		}
		if (triggers.isEmpty()) {
			triggers.add("暂无明显诱因，建议继续记录");
		}

		String narrative = "当前画像基于首版闭环数据生成，重点关注 " + String.join("、", tags)
				+ "；系统推测需要优先留意的诱因包括 " + String.join("、", triggers) + "。";
		return new AppContracts.PersonaSummaryResponse(tags, triggers, narrative);
	}

	public AppContracts.ProactiveCareSettingsResponse getProactiveCareSettings(String userId) {
		ensureProfile(userId);
		return proactiveCareService.getSettings(userId);
	}

	@Transactional
	public AppContracts.ProactiveCareSettingsResponse updateProactiveCareSettings(
			String userId,
			AppContracts.ProactiveCareSettingsRequest request
	) {
		ensureProfile(userId);
		AppContracts.ProactiveCareSettingsResponse response = proactiveCareService.updateSettings(userId, request);
		refreshInsightState(userId);
		return response;
	}

	public AppContracts.ProactiveCareBriefResponse getProactiveCareBrief(String userId) {
		ensureProfile(userId);
		return proactiveCareService.getProactiveCareBrief(userId);
	}

	public AppContracts.FlareReviewReportResponse getLatestFlareReviewReport(String userId, int lookbackDays) {
		ensureProfile(userId);
		return proactiveCareService.getLatestFlareReviewReport(userId, lookbackDays);
	}

	@Transactional
	public AppContracts.FamilyInviteResponse createFamilyInvite(String userId, AppContracts.FamilyInviteCreateRequest request) {
		ensureProfile(userId);
		AppContracts.FamilyInviteResponse response = familyCareService.createInvite(userId, request);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_FAMILY_INVITE_CREATED,
				"family",
				response.createdAt(),
				eventPayload(
						"inviteCode", response.inviteCode(),
						"relationType", response.relationType(),
						"patientUserId", response.patientUserId()
				)
		);
		return response;
	}

	public List<AppContracts.FamilyInviteResponse> listFamilyInvites(String userId) {
		ensureProfile(userId);
		return familyCareService.listInvites(userId);
	}

	@Transactional
	public AppContracts.FamilyInviteResponse acceptFamilyInvite(String userId, String inviteCode) {
		ensureProfile(userId);
		AppContracts.FamilyInviteResponse response = familyCareService.acceptInvite(userId, inviteCode);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_FAMILY_INVITE_ACCEPTED,
				"family",
				Instant.now(),
				eventPayload(
						"inviteCode", response.inviteCode(),
						"patientUserId", response.patientUserId(),
						"acceptedByUserId", userId
				)
		);
		return response;
	}

	@Transactional
	public AppContracts.FamilyInviteResponse cancelFamilyInvite(String userId, String inviteCode) {
		ensureProfile(userId);
		return familyCareService.cancelInvite(userId, inviteCode);
	}

	public AppContracts.FamilyMembersResponse getFamilyMembers(String userId) {
		ensureProfile(userId);
		return familyCareService.getMembers(userId);
	}

	@Transactional
	public AppContracts.FamilyBindingMemberResponse removeFamilyBinding(String userId, String bindingCode) {
		ensureProfile(userId);
		return familyCareService.removeBinding(userId, bindingCode);
	}

	public List<AppContracts.FamilyAlertResponse> getFamilyAlerts(String userId) {
		ensureProfile(userId);
		return familyCareService.getAlerts(userId);
	}

	public AppContracts.FamilyPatientSummaryResponse getFamilyPatientSummary(String userId, String patientUserId) {
		ensureProfile(userId);
		AppContracts.FamilyPatientSummaryResponse response = familyCareService.getPatientSummary(userId, patientUserId);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_FAMILY_PATIENT_SUMMARY_VIEWED,
				"family",
				Instant.now(),
				eventPayload(
						"patientUserId", patientUserId,
						"patientNickname", response.patientNickname(),
						"riskLevel", response.overallRiskLevel().name()
				)
		);
		return response;
	}

	public AppContracts.MedicationPlanResponse getMedicationPlan(String userId) {
		return medicationPlanRepository.findByUserCode(userId)
				.map(entity -> new AppContracts.MedicationPlanResponse(
						readMedicationItems(entity.getCurrentMedicationsJson()),
						entity.getFollowUpNote(),
						entity.getUpdatedAt()
				))
				.orElseGet(() -> new AppContracts.MedicationPlanResponse(List.of(), "", Instant.EPOCH));
	}

	@Transactional
	public AppContracts.MedicationPlanResponse updateMedicationPlan(String userId, AppContracts.MedicationPlanRequest request) {
		MedicationPlanEntity entity = medicationPlanRepository.findByUserCode(userId)
				.orElseGet(() -> {
					MedicationPlanEntity planEntity = new MedicationPlanEntity();
					planEntity.setUserCode(userId);
					return planEntity;
		});
		entity.setCurrentMedicationsJson(jsonCodec.toJson(request.currentMedications()));
		entity.setFollowUpNote(request.followUpNote());
		entity.setUpdatedAt(Instant.now());
		medicationPlanRepository.save(entity);
		return new AppContracts.MedicationPlanResponse(
				request.currentMedications(),
				request.followUpNote(),
				entity.getUpdatedAt()
		);
	}

	public List<AppContracts.DailyHealthSummaryResponse> getDailySummaries(String userId, int days) {
		List<AppContracts.DailyHealthSummaryResponse> summaries = healthRuleEngineService.getRecentSummaries(userId, days);
		if (!summaries.isEmpty()) {
			return summaries;
		}
		healthRuleEngineService.refreshDailySummary(userId, LocalDate.now());
		return healthRuleEngineService.getRecentSummaries(userId, days);
	}

	private UserProfileEntity ensureProfile(String userId) {
		return userProfileRepository.findByUserCode(userId)
				.orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "用户档案不存在，请先登录"));
	}

	private Map<String, Object> eventPayload(Object... keyValues) {
		Map<String, Object> payload = new LinkedHashMap<>();
		for (int index = 0; index + 1 < keyValues.length; index += 2) {
			Object key = keyValues[index];
			Object value = keyValues[index + 1];
			if (key instanceof String stringKey && StringUtils.hasText(stringKey) && value != null) {
				payload.put(stringKey, value);
			}
		}
		return payload;
	}

	private StoredFileEntity persistStoredFile(String userId, MultipartFile file) {
		String fileCode = idGenerator.next("file");
		LocalFileStorageService.StoredPhysicalFile storedPhysicalFile = localFileStorageService.save(fileCode, file);
		StoredFileEntity entity = new StoredFileEntity();
		entity.setFileCode(fileCode);
		entity.setUserCode(userId);
		entity.setFileName(storedPhysicalFile.fileName());
		entity.setContentType(storedPhysicalFile.contentType());
		entity.setFileSize(storedPhysicalFile.size());
		entity.setRelativePath(storedPhysicalFile.relativePath());
		entity.setUploadedAt(Instant.now());
		return storedFileRepository.save(entity);
	}

	private AppContracts.UserProfileResponse toProfileResponse(UserProfileEntity profile) {
		return new AppContracts.UserProfileResponse(
				profile.getUserCode(),
				profile.getName(),
				profile.getGender(),
				profile.getBirthday(),
				profile.getHeightCm(),
				profile.getTargetUricAcid(),
				readStringList(profile.getAllergiesJson()),
				readStringList(profile.getComorbiditiesJson()),
				profile.getEmergencyContact(),
				profile.getUpdatedAt()
		);
	}

	private AppContracts.FileUploadResponse toFileUploadResponse(StoredFileEntity storedFile) {
		return new AppContracts.FileUploadResponse(
				storedFile.getFileCode(),
				storedFile.getFileName(),
				buildFileAccessUrl(storedFile.getFileCode()),
				storedFile.getFileSize(),
				storedFile.getContentType()
		);
	}

	private AppContracts.LabReportAnalyzeResponse toLabReportResponse(LabReportRecordEntity record) {
		return new AppContracts.LabReportAnalyzeResponse(
				record.getReportCode(),
				record.getReportDate(),
				readIndicators(record.getIndicatorsJson()),
				toRiskLevel(record.getOverallRiskLevel()),
				readStringList(record.getSuggestionsJson()),
				record.getSummaryText()
		);
	}

	private List<AppContracts.MealItem> safeMealItems(List<AppContracts.MealItem> items) {
		return items == null ? List.of() : items;
	}

	private List<AppContracts.LabIndicator> safeIndicators(List<AppContracts.LabIndicator> indicators) {
		return indicators == null ? List.of() : indicators;
	}

	private <T> List<T> safeList(List<T> items) {
		return items == null ? List.of() : items;
	}

	private List<String> readStringList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		return jsonCodec.fromJson(json, STRING_LIST_TYPE);
	}

	private List<AppContracts.MealItem> readMealItems(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		return jsonCodec.fromJson(json, MEAL_ITEM_LIST_TYPE);
	}

	private List<AppContracts.LabIndicator> readIndicators(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		return jsonCodec.fromJson(json, LAB_INDICATOR_LIST_TYPE);
	}

	private List<AppContracts.MedicationItem> readMedicationItems(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		return jsonCodec.fromJson(json, MEDICATION_ITEM_LIST_TYPE);
	}

	private Instant parseInstantOrNow(String value) {
		if (!StringUtils.hasText(value)) {
			return Instant.now();
		}
		return Instant.parse(value);
	}

	private LocalDate parseDateOrToday(String value) {
		if (!StringUtils.hasText(value)) {
			return LocalDate.now();
		}
		return LocalDate.parse(value);
	}

	private String buildFileAccessUrl(String fileCode) {
		return "/api/v1/files/" + fileCode;
	}

	private Optional<UricAcidRecordEntity> latestUricAcid(String userId) {
		return uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream().findFirst();
	}

	private Optional<HydrationRecordEntity> latestHydration(String userId) {
		return hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream().findFirst();
	}

	private Optional<MealRecordEntity> latestMeal(String userId) {
		return mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream().findFirst();
	}

	private Optional<FlareRecordEntity> latestFlare(String userId) {
		return flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).stream().findFirst();
	}

	private void refreshInsightState(String userId) {
		healthRuleEngineService.refreshDailySummary(userId, LocalDate.now());
		healthRuleEngineService.rebuildActiveReminders(userId);
	}

	private int defaultInt(Integer value) {
		return value == null ? 0 : value;
	}

	private AppContracts.RiskLevel uricAcidRisk(Integer value) {
		if (value == null) {
			return AppContracts.RiskLevel.GREEN;
		}
		if (value >= 500) {
			return AppContracts.RiskLevel.RED;
		}
		if (value > 420) {
			return AppContracts.RiskLevel.YELLOW;
		}
		return AppContracts.RiskLevel.GREEN;
	}

	private AppContracts.RiskLevel toRiskLevel(String value) {
		return AppContracts.RiskLevel.valueOf(value);
	}

	private int riskRank(AppContracts.RiskLevel riskLevel) {
		return switch (riskLevel) {
			case RED -> 3;
			case YELLOW -> 2;
			case GREEN -> 1;
		};
	}
}
