package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tongfeng.backend.app.persistence.entity.AuthIdentityEntity;
import com.tongfeng.backend.app.persistence.entity.AuthSessionEntity;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MedicationCheckinEntity;
import com.tongfeng.backend.app.persistence.entity.MedicationPlanEntity;
import com.tongfeng.backend.app.persistence.entity.PrivacyConsentRecordEntity;
import com.tongfeng.backend.app.persistence.entity.StoredFileEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UserAccountEntity;
import com.tongfeng.backend.app.persistence.entity.UserProfileEntity;
import com.tongfeng.backend.app.persistence.entity.WeightRecordEntity;
import com.tongfeng.backend.app.persistence.repo.AuthIdentityRepository;
import com.tongfeng.backend.app.persistence.repo.AuthSessionRepository;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MedicationCheckinRepository;
import com.tongfeng.backend.app.persistence.repo.MedicationPlanRepository;
import com.tongfeng.backend.app.persistence.repo.PrivacyConsentRecordRepository;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

	private record DailyMedicationStats(
			int plannedDoseCount,
			int takenDoseCount,
			int missedDoseCount,
			int skippedDoseCount,
			int overdueCount
	) {
	}

	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;
	private final AuthIdentityRepository authIdentityRepository;
	private final AuthSessionRepository authSessionRepository;
	private final PrivacyConsentRecordRepository privacyConsentRecordRepository;
	private final StoredFileRepository storedFileRepository;
	private final MealRecordRepository mealRecordRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final WeightRecordRepository weightRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final LabReportRecordRepository labReportRecordRepository;
	private final MedicationCheckinRepository medicationCheckinRepository;
	private final MedicationPlanRepository medicationPlanRepository;
	private final LocalFileStorageService localFileStorageService;
	private final AiServiceClient aiServiceClient;
	private final AppProperties appProperties;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;
	private final PasswordHashService passwordHashService;
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
			AuthIdentityRepository authIdentityRepository,
			AuthSessionRepository authSessionRepository,
			PrivacyConsentRecordRepository privacyConsentRecordRepository,
			StoredFileRepository storedFileRepository,
			MealRecordRepository mealRecordRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			WeightRecordRepository weightRecordRepository,
			FlareRecordRepository flareRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			LabReportRecordRepository labReportRecordRepository,
			MedicationCheckinRepository medicationCheckinRepository,
			MedicationPlanRepository medicationPlanRepository,
			LocalFileStorageService localFileStorageService,
			AiServiceClient aiServiceClient,
			AppProperties appProperties,
			IdGenerator idGenerator,
			JsonCodec jsonCodec,
			PasswordHashService passwordHashService,
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
		this.authIdentityRepository = authIdentityRepository;
		this.authSessionRepository = authSessionRepository;
		this.privacyConsentRecordRepository = privacyConsentRecordRepository;
		this.storedFileRepository = storedFileRepository;
		this.mealRecordRepository = mealRecordRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.weightRecordRepository = weightRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.labReportRecordRepository = labReportRecordRepository;
		this.medicationCheckinRepository = medicationCheckinRepository;
		this.medicationPlanRepository = medicationPlanRepository;
		this.localFileStorageService = localFileStorageService;
		this.aiServiceClient = aiServiceClient;
		this.appProperties = appProperties;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
		this.passwordHashService = passwordHashService;
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

		AppContracts.AuthTokenResponse tokenResponse = createAuthSessionResponse(
				userId,
				request.nickname(),
				"MOCK",
				"DEMO",
				request.nickname(),
				false
		);
		return tokenResponse;
	}

	@Transactional
	public AppContracts.AuthTokenResponse register(AppContracts.RegisterRequest request) {
		String accountType = normalizeAccountType(request.accountType());
		String principal = normalizePrincipal(accountType, request.account());
		validatePassword(request.password(), request.confirmPassword());
		validateRequiredPrivacyConsent(request.consent());
		if (authIdentityRepository.findByPrincipalValueAndStatus(principal, "ACTIVE").isPresent()) {
			throw new BusinessException("ACCOUNT_EXISTS", "该账号已经注册，请直接登录");
		}

		Instant now = Instant.now();
		String userId = idGenerator.next("user");
		UserAccountEntity accountEntity = new UserAccountEntity();
		accountEntity.setUserCode(userId);
		accountEntity.setNickname(request.nickname().trim());
		accountEntity.setCreatedAt(now);
		accountEntity.setUpdatedAt(now);
		userAccountRepository.save(accountEntity);

		UserProfileEntity profileEntity = new UserProfileEntity();
		profileEntity.setUserCode(userId);
		profileEntity.setName(request.nickname().trim());
		profileEntity.setGender("UNKNOWN");
		profileEntity.setTargetUricAcid(360);
		profileEntity.setAllergiesJson(jsonCodec.toJson(List.of()));
		profileEntity.setComorbiditiesJson(jsonCodec.toJson(List.of()));
		profileEntity.setUpdatedAt(now);
		userProfileRepository.save(profileEntity);

		PasswordHashService.HashedPassword hashedPassword = passwordHashService.hash(request.password());
		AuthIdentityEntity identityEntity = new AuthIdentityEntity();
		identityEntity.setIdentityCode(idGenerator.next("identity"));
		identityEntity.setUserCode(userId);
		identityEntity.setAccountType(accountType);
		identityEntity.setPrincipalValue(principal);
		identityEntity.setPasswordHash(hashedPassword.hash());
		identityEntity.setPasswordSalt(hashedPassword.salt());
		identityEntity.setStatus("ACTIVE");
		identityEntity.setCreatedAt(now);
		identityEntity.setUpdatedAt(now);
		authIdentityRepository.save(identityEntity);

		savePrivacyConsent(userId, request.consent(), "REGISTER");
		return createAuthSessionResponse(
				userId,
				accountEntity.getNickname(),
				"PASSWORD",
				accountType,
				principal,
				true
		);
	}

	@Transactional
	public AppContracts.AuthTokenResponse login(AppContracts.LoginRequest request) {
		String accountType = normalizeAccountType(request.accountType());
		String principal = normalizePrincipal(accountType, request.account());
		AuthIdentityEntity identityEntity = authIdentityRepository.findByPrincipalValueAndStatus(principal, "ACTIVE")
				.orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "账号不存在或已停用"));
		if (!Objects.equals(identityEntity.getAccountType(), accountType)) {
			throw new BusinessException("ACCOUNT_TYPE_MISMATCH", "账号类型不匹配，请确认使用邮箱还是手机号登录");
		}
		if (!passwordHashService.matches(request.password(), identityEntity.getPasswordSalt(), identityEntity.getPasswordHash())) {
			throw new BusinessException("INVALID_CREDENTIALS", "账号或密码不正确");
		}
		UserAccountEntity accountEntity = ensureAccount(identityEntity.getUserCode());
		boolean privacyConsentCompleted = hasCompletedPrivacyConsent(identityEntity.getUserCode());
		if (!privacyConsentCompleted) {
			throw new BusinessException("PRIVACY_CONSENT_REQUIRED", "请先完成隐私政策和服务条款授权后再登录");
		}
		identityEntity.setLastLoginAt(Instant.now());
		identityEntity.setUpdatedAt(Instant.now());
		authIdentityRepository.save(identityEntity);
		return createAuthSessionResponse(
				accountEntity.getUserCode(),
				accountEntity.getNickname(),
				"PASSWORD",
				accountType,
				principal,
				true
		);
	}

	@Transactional
	public AppContracts.AuthLogoutResponse logout(String token) {
		if (!StringUtils.hasText(token)) {
			throw new BusinessException("UNAUTHORIZED", "当前没有可注销的登录会话");
		}
		authSessionRepository.deleteByToken(token);
		sessionCacheService.evict(token);
		return new AppContracts.AuthLogoutResponse(Instant.now(), "已安全退出登录");
	}

	@Transactional
	public UserSession requireSession(String token) {
		if (!StringUtils.hasText(token)) {
			throw new BusinessException("UNAUTHORIZED", "缺少登录凭证");
		}
		AuthSessionEntity sessionEntity = authSessionRepository.findByToken(token)
				.orElseThrow(() -> new BusinessException("UNAUTHORIZED", "登录已失效，请重新登录"));
		Instant now = Instant.now();
		if (sessionEntity.getExpiresAt().isBefore(now)) {
			authSessionRepository.delete(sessionEntity);
			sessionCacheService.evict(token);
			throw new BusinessException("UNAUTHORIZED", "登录已失效，请重新登录");
		}
		sessionEntity.setLastSeenAt(now);
		authSessionRepository.save(sessionEntity);
		UserSession session = toUserSession(sessionEntity);
		sessionCacheService.put(session);
		return session;
	}

	public AppContracts.AuthSessionInfoResponse getCurrentSessionInfo(String userId, String token) {
		UserSession session = requireOwnedSession(userId, token);
		return new AppContracts.AuthSessionInfoResponse(
				session.sessionCode(),
				session.userId(),
				session.nickname(),
				session.authMode(),
				session.accountType(),
				session.accountIdentifier(),
				session.privacyConsentCompleted(),
				session.createdAt(),
				session.lastSeenAt(),
				session.expiresAt()
		);
	}

	public List<AppContracts.AuthActiveSessionResponse> getActiveSessions(String userId, String token) {
		UserSession currentSession = requireOwnedSession(userId, token);
		Instant now = Instant.now();
		return authSessionRepository.findByUserCodeOrderByLastSeenAtDescCreatedAtDesc(userId).stream()
				.filter(item -> item.getExpiresAt().isAfter(now))
				.map(item -> toActiveSessionResponse(item, Objects.equals(item.getSessionCode(), currentSession.sessionCode())))
				.toList();
	}

	@Transactional
	public AppContracts.PasswordChangeResponse changePassword(
			String userId,
			String token,
			AppContracts.ChangePasswordRequest request
	) {
		UserSession currentSession = requireOwnedSession(userId, token);
		if (!"PASSWORD".equals(currentSession.authMode())) {
			throw new BusinessException("PASSWORD_CHANGE_NOT_SUPPORTED", "当前登录方式不支持修改密码");
		}
		AuthIdentityEntity identityEntity = authIdentityRepository.findByUserCodeAndAccountTypeAndPrincipalValueAndStatus(
				userId,
				currentSession.accountType(),
				currentSession.accountIdentifier(),
				"ACTIVE"
		).orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "当前账号不存在或已停用"));
		if (!passwordHashService.matches(request.currentPassword(), identityEntity.getPasswordSalt(), identityEntity.getPasswordHash())) {
			throw new BusinessException("INVALID_CURRENT_PASSWORD", "当前密码不正确");
		}
		if (Objects.equals(request.currentPassword(), request.newPassword())) {
			throw new BusinessException("PASSWORD_UNCHANGED", "新密码不能与当前密码相同");
		}
		validatePassword(request.newPassword(), request.confirmPassword());

		PasswordHashService.HashedPassword hashedPassword = passwordHashService.hash(request.newPassword());
		Instant now = Instant.now();
		identityEntity.setPasswordHash(hashedPassword.hash());
		identityEntity.setPasswordSalt(hashedPassword.salt());
		identityEntity.setUpdatedAt(now);
		authIdentityRepository.save(identityEntity);

		int loggedOutOtherSessions = 0;
		if (defaultBoolean(request.logoutOtherSessions(), true)) {
			List<AuthSessionEntity> otherSessions = authSessionRepository.findByUserCodeOrderByLastSeenAtDescCreatedAtDesc(userId).stream()
					.filter(item -> !Objects.equals(item.getSessionCode(), currentSession.sessionCode()))
					.toList();
			loggedOutOtherSessions = otherSessions.size();
			authSessionRepository.deleteAll(otherSessions);
			otherSessions.forEach(item -> sessionCacheService.evict(item.getToken()));
		}

		return new AppContracts.PasswordChangeResponse(
				now,
				loggedOutOtherSessions,
				loggedOutOtherSessions > 0 ? "密码已更新，并已退出其他设备会话" : "密码已更新"
		);
	}

	@Transactional
	public AppContracts.AuthSessionRevokeResponse revokeSession(String userId, String token, String sessionCode) {
		UserSession currentSession = requireOwnedSession(userId, token);
		if (Objects.equals(currentSession.sessionCode(), sessionCode)) {
			throw new BusinessException("USE_LOGOUT_FOR_CURRENT_SESSION", "当前设备请直接使用退出登录");
		}
		AuthSessionEntity sessionEntity = authSessionRepository.findBySessionCodeAndUserCode(sessionCode, userId)
				.orElseThrow(() -> new BusinessException("SESSION_NOT_FOUND", "目标会话不存在或已失效"));
		authSessionRepository.delete(sessionEntity);
		sessionCacheService.evict(sessionEntity.getToken());
		return new AppContracts.AuthSessionRevokeResponse(
				sessionCode,
				Instant.now(),
				"该设备会话已移除"
		);
	}

	public AppContracts.PrivacyConsentResponse getCurrentPrivacyConsent(String userId) {
		ensureProfile(userId);
		PrivacyConsentRecordEntity entity = privacyConsentRecordRepository.findFirstByUserCodeOrderByEffectiveAtDesc(userId)
				.orElseThrow(() -> new BusinessException("PRIVACY_CONSENT_NOT_FOUND", "当前还没有隐私授权记录"));
		return toPrivacyConsentResponse(entity);
	}

	public List<AppContracts.PrivacyConsentResponse> getPrivacyConsentHistory(String userId) {
		ensureProfile(userId);
		return privacyConsentRecordRepository.findByUserCodeOrderByEffectiveAtDesc(userId).stream()
				.map(this::toPrivacyConsentResponse)
				.toList();
	}

	@Transactional
	public AppContracts.PrivacyConsentResponse updatePrivacyConsent(String userId, AppContracts.PrivacyConsentSubmitRequest request) {
		ensureProfile(userId);
		validateRequiredPrivacyConsent(request);
		return toPrivacyConsentResponse(savePrivacyConsent(userId, request, "SETTINGS"));
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

	public AppContracts.TodayActionPlanResponse getTodayActionPlan(String userId) {
		ensureProfile(userId);
		List<AppContracts.ReminderResponse> reminders = getReminders(userId);
		List<AppContracts.DailyHealthSummaryResponse> summaries = getDailySummaries(userId, 1);
		AppContracts.DailyHealthSummaryResponse todaySummary = summaries.isEmpty() ? null : summaries.getFirst();
		Optional<UricAcidRecordEntity> latestUricAcid = latestUricAcid(userId);
		Optional<HydrationRecordEntity> latestHydration = latestHydration(userId);
		Optional<FlareRecordEntity> latestFlare = latestFlare(userId);
		List<LabReportRecordEntity> labReports = labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId);
		AppContracts.MedicationPlanResponse medicationPlan = getMedicationPlan(userId);
		AppContracts.MedicationAdherenceSummaryResponse medicationAdherence = buildMedicationAdherenceSummary(userId, medicationPlan, 7);

		LocalDate today = LocalDate.now();
		List<String> reasons = new ArrayList<>();
		AppContracts.RiskLevel overallRisk = todaySummary == null
				? AppContracts.RiskLevel.GREEN
				: todaySummary.overallRiskLevel();
		String triageCode = "SELF_MANAGEMENT";
		String triageTitle = "今天适合继续居家主动管理";
		String triageSummary = "当前更适合按计划记录、补水、规律饮食和按时用药。";
		String nextStep = "优先完成今天的 1 到 3 个行动项，并持续观察症状变化。";

		if (todaySummary != null && defaultInt(todaySummary.highRiskMealCount()) >= 2) {
			reasons.add("今天已经出现多次高风险饮食暴露");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.YELLOW);
		}
		if (todaySummary != null && defaultInt(todaySummary.flareCount()) > 0) {
			reasons.add("今天记录到了发作事件");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.YELLOW);
		}
		if (latestUricAcid.isPresent() && defaultInt(latestUricAcid.get().getUaValue()) >= 500) {
			reasons.add("最近一次尿酸已经达到高风险区间");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.RED);
			triageCode = "CONTACT_DOCTOR_SOON";
			triageTitle = "建议尽快联系医生复核当前方案";
			triageSummary = "尿酸或近期症状提示复发风险偏高，建议尽快和医生确认用药、复查与饮食策略。";
			nextStep = "今天先完成补水、避免高嘌呤饮食，并准备最近的记录和化验结果。";
		}
		if (latestHydration.isPresent() && defaultInt(latestHydration.get().getUrineColorLevel()) >= 4) {
			reasons.add("最近一次尿液颜色偏深，提示补水不足");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.YELLOW);
			if ("SELF_MANAGEMENT".equals(triageCode)) {
				triageCode = "CONTACT_DOCTOR_SOON";
				triageTitle = "建议尽快调整补水和复盘诱因";
				triageSummary = "当前最需要先把补水和近期诱因记录补齐，必要时联系医生复核。";
				nextStep = "今天先完成饮水记录与症状观察，如颜色持续加深请尽快就医。";
			}
		}
		if (latestFlare.isPresent() && defaultInt(latestFlare.get().getPainLevel()) >= 9) {
			reasons.add("最近一次发作疼痛等级非常高");
			overallRisk = AppContracts.RiskLevel.RED;
			triageCode = "URGENT_OFFLINE";
			triageTitle = "剧烈发作需要尽快线下就医";
			triageSummary = "如果疼痛持续、无法负重，或伴随发热红肿明显加重，请不要只做居家处理。";
			nextStep = "优先线下评估，并带上最近的尿酸、用药和发作记录。";
		} else if (latestFlare.isPresent() && defaultInt(latestFlare.get().getPainLevel()) >= 7) {
			reasons.add("最近一次发作疼痛已经明显影响日常活动");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.RED);
			if (!"URGENT_OFFLINE".equals(triageCode)) {
				triageCode = "CONTACT_DOCTOR_SOON";
				triageTitle = "建议尽快联系医生，避免发作继续升级";
				triageSummary = "当前需要尽快处理发作诱因、止痛方案和复查安排。";
				nextStep = "今天先减少活动负荷，并尽快复盘最近饮食、补水和药物执行情况。";
			}
		}

		if (reasons.isEmpty()) {
			reasons.add("当前没有检测到必须立刻线下就医的高危信号");
		}

		List<AppContracts.TodayActionItemResponse> actions = buildTodayActions(
				today,
				reminders,
				todaySummary,
				latestUricAcid,
				latestHydration,
				latestFlare,
				labReports,
				medicationPlan,
				medicationAdherence
		);
		List<String> trustNotes = List.of(
				"AI 建议只基于你已经录入的记录生成，不能替代医生诊断和处方。",
				"如果化验单识别或指标提取与你手里的原始报告不一致，请以原始报告为准。",
				"一旦出现剧烈疼痛、无法负重、胸痛、呼吸困难或持续高热，请立即线下就医。"
		);

		return new AppContracts.TodayActionPlanResponse(
				userId,
				overallRisk,
				triageCode,
				triageTitle,
				triageSummary,
				nextStep,
				reasons,
				actions,
				trustNotes,
				Instant.now()
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

	public AppContracts.LabReportReviewResponse getLabReportReview(String userId, String reportId) {
		ensureProfile(userId);
		LabReportRecordEntity report = labReportRecordRepository.findByReportCode(reportId)
				.orElseThrow(() -> new BusinessException("LAB_REPORT_NOT_FOUND", "化验单不存在"));
		if (!Objects.equals(report.getUserCode(), userId)) {
			throw new BusinessException("FORBIDDEN", "无权查看该化验单复盘");
		}

		UserProfileEntity profile = ensureProfile(userId);
		List<LabReportRecordEntity> reports = labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId);
		LabReportRecordEntity previousReport = reports.stream()
				.filter(item -> !Objects.equals(item.getReportCode(), report.getReportCode()))
				.filter(item -> !item.getReportDate().isAfter(report.getReportDate()))
				.max(Comparator.comparing(LabReportRecordEntity::getReportDate))
				.orElse(null);

		List<AppContracts.LabIndicator> currentIndicators = readIndicators(report.getIndicatorsJson());
		List<AppContracts.LabIndicator> previousIndicators = previousReport == null
				? List.of()
				: readIndicators(previousReport.getIndicatorsJson());
		Map<String, AppContracts.LabIndicator> previousByKey = previousIndicators.stream()
				.collect(Collectors.toMap(
						this::indicatorKey,
						item -> item,
						(left, right) -> left,
						LinkedHashMap::new
				));

		List<AppContracts.LabReportReviewComparisonResponse> comparisons = currentIndicators.stream()
				.map(item -> toLabComparison(item, previousByKey.get(indicatorKey(item))))
				.sorted(Comparator
						.comparing((AppContracts.LabReportReviewComparisonResponse item) -> riskRank(item.currentRiskLevel()))
						.reversed()
						.thenComparing(item -> absoluteValue(item.deltaValue()), Comparator.reverseOrder()))
				.toList();

		AppContracts.LabIndicator currentUricAcid = findUricAcidIndicator(currentIndicators).orElse(null);
		AppContracts.LabIndicator previousUricAcid = findUricAcidIndicator(previousIndicators).orElse(null);
		Integer targetUricAcidValue = profile.getTargetUricAcid();
		boolean withinTarget = currentUricAcid != null
				&& targetUricAcidValue != null
				&& currentUricAcid.value() != null
				&& currentUricAcid.value().compareTo(BigDecimal.valueOf(targetUricAcidValue)) <= 0;
		String targetConclusion = buildLabTargetConclusion(currentUricAcid, previousUricAcid, targetUricAcidValue, withinTarget);
		List<String> keyChanges = buildLabKeyChanges(currentUricAcid, previousUricAcid, comparisons, previousReport);
		String followUpRecommendation = buildLabFollowUpRecommendation(report, currentUricAcid, targetUricAcidValue);
		List<String> nextActions = buildLabNextActions(report, currentUricAcid, targetUricAcidValue, comparisons);
		List<String> trustNotes = buildLabTrustNotes(previousReport, currentIndicators);

		return new AppContracts.LabReportReviewResponse(
				report.getReportCode(),
				report.getReportDate(),
				toRiskLevel(report.getOverallRiskLevel()),
				buildLabReviewSummary(report, currentUricAcid, previousUricAcid, targetUricAcidValue, previousReport),
				previousReport == null ? null : previousReport.getReportCode(),
				previousReport == null ? null : previousReport.getReportDate(),
				previousReport == null ? null : (int) ChronoUnit.DAYS.between(previousReport.getReportDate(), report.getReportDate()),
				targetUricAcidValue,
				currentUricAcid == null ? null : currentUricAcid.value(),
				currentUricAcid == null ? null : currentUricAcid.unit(),
				withinTarget,
				targetConclusion,
				comparisons,
				keyChanges,
				followUpRecommendation,
				nextActions,
				trustNotes,
				Instant.now()
		);
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

	@Transactional
	public AppContracts.FamilyBindingMemberResponse updateFamilyBindingPermissions(
			String userId,
			String bindingCode,
			AppContracts.FamilyBindingPermissionUpdateRequest request
	) {
		ensureProfile(userId);
		return familyCareService.updateBindingPermissions(userId, bindingCode, request);
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

	public AppContracts.FamilySharedMedicationWeeklyReportResponse getFamilySharedMedicationWeeklyReport(
			String userId,
			String patientUserId,
			int days
	) {
		ensureProfile(userId);
		AppContracts.FamilyBindingMemberResponse binding = familyCareService.requireSharedWeeklyReportAccess(userId, patientUserId);
		AppContracts.MedicationWeeklyReportResponse weeklyReport = getMedicationWeeklyReport(patientUserId, days);
		return new AppContracts.FamilySharedMedicationWeeklyReportResponse(
				binding.patientUserId(),
				binding.patientNickname(),
				binding.relationType(),
				binding.caregiverPermission(),
				binding.weeklyReportEnabled(),
				weeklyReport,
				Instant.now()
		);
	}

	public AppContracts.FamilyTasksResponse getFamilyTasks(String userId) {
		ensureProfile(userId);
		return familyCareService.getTasks(userId);
	}

	@Transactional
	public AppContracts.FamilyTaskResponse createFamilyTask(
			String userId,
			String bindingCode,
			AppContracts.FamilyTaskCreateRequest request
	) {
		ensureProfile(userId);
		AppContracts.FamilyTaskResponse response = familyCareService.createTask(userId, bindingCode, request);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_FAMILY_TASK_CREATED,
				"family",
				response.createdAt(),
				eventPayload(
						"taskCode", response.taskCode(),
						"bindingCode", response.bindingCode(),
						"caregiverUserId", response.caregiverUserId()
				)
		);
		return response;
	}

	@Transactional
	public AppContracts.FamilyTaskResponse completeFamilyTask(
			String userId,
			String taskCode,
			AppContracts.FamilyTaskCompleteRequest request
	) {
		ensureProfile(userId);
		AppContracts.FamilyTaskResponse response = familyCareService.completeTask(userId, taskCode, request);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_FAMILY_TASK_COMPLETED,
				"family",
				Instant.now(),
				eventPayload(
						"taskCode", response.taskCode(),
						"patientUserId", response.patientUserId(),
						"bindingCode", response.bindingCode()
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
		refreshInsightState(userId);
		return new AppContracts.MedicationPlanResponse(
				request.currentMedications(),
				request.followUpNote(),
				entity.getUpdatedAt()
		);
	}

	public AppContracts.MedicationAdherenceSummaryResponse getMedicationAdherence(String userId, int days) {
		ensureProfile(userId);
		return buildMedicationAdherenceSummary(userId, getMedicationPlan(userId), days);
	}

	public AppContracts.MedicationWeeklyReportResponse getMedicationWeeklyReport(String userId, int days) {
		ensureProfile(userId);
		return buildMedicationWeeklyReport(userId, getMedicationPlan(userId), days);
	}

	@Transactional
	public AppContracts.MedicationCheckinResponse submitMedicationCheckin(
			String userId,
			AppContracts.MedicationCheckinRequest request
	) {
		ensureProfile(userId);
		AppContracts.MedicationPlanResponse medicationPlan = getMedicationPlan(userId);
		List<AppContracts.MedicationItem> medications = medicationPlan.currentMedications();
		if (medications == null || medications.isEmpty()) {
			throw new BusinessException("MEDICATION_PLAN_REQUIRED", "请先维护当前用药计划后再打卡。");
		}

		AppContracts.MedicationItem medicationItem = medications.stream()
				.filter(item -> normalizeMedicationName(item.name()).equals(normalizeMedicationName(request.medicationName())))
				.findFirst()
				.orElseThrow(() -> new BusinessException("MEDICATION_NOT_FOUND", "当前用药计划中未找到该药物，请先核对药名。"));

		String scheduledPeriod = normalizeScheduledPeriod(request.scheduledPeriod());
		List<String> plannedPeriods = resolveMedicationPeriods(medicationItem.frequency());
		if (!plannedPeriods.contains(scheduledPeriod)) {
			throw new BusinessException("MEDICATION_PERIOD_INVALID", "该药物当前不在这个服药时段内，请按计划核对后再提交。");
		}

		String status = normalizeMedicationStatus(request.status());
		LocalDate today = LocalDate.now();
		MedicationCheckinEntity entity = medicationCheckinRepository
				.findByUserCodeAndCheckinDateAndMedicationNameIgnoreCaseAndScheduledPeriod(
						userId,
						today,
						medicationItem.name(),
						scheduledPeriod
				)
				.orElseGet(() -> {
					MedicationCheckinEntity checkinEntity = new MedicationCheckinEntity();
					checkinEntity.setCheckinCode(idGenerator.next("med-check"));
					checkinEntity.setUserCode(userId);
					checkinEntity.setMedicationName(medicationItem.name());
					checkinEntity.setScheduledPeriod(scheduledPeriod);
					checkinEntity.setCheckinDate(today);
					return checkinEntity;
				});

		entity.setCheckinStatus(status);
		entity.setNoteText(trimToNull(request.note()));
		entity.setCheckinAt(Instant.now());
		entity.setSourceType("MANUAL");
		medicationCheckinRepository.save(entity);
		mvpMetricsService.recordEvent(
				userId,
				MvpMetricsService.EVENT_MEDICATION_CHECKIN,
				"assistant",
				entity.getCheckinAt(),
				eventPayload(
						"checkinId", entity.getCheckinCode(),
						"medicationName", entity.getMedicationName(),
						"scheduledPeriod", entity.getScheduledPeriod(),
						"status", entity.getCheckinStatus()
				)
		);
		refreshInsightState(userId);
		return toMedicationCheckinResponse(entity);
	}

	public List<AppContracts.DailyHealthSummaryResponse> getDailySummaries(String userId, int days) {
		List<AppContracts.DailyHealthSummaryResponse> summaries = healthRuleEngineService.getRecentSummaries(userId, days);
		if (!summaries.isEmpty()) {
			return summaries;
		}
		healthRuleEngineService.refreshDailySummary(userId, LocalDate.now());
		return healthRuleEngineService.getRecentSummaries(userId, days);
	}

	private List<AppContracts.TodayActionItemResponse> buildTodayActions(
			LocalDate today,
			List<AppContracts.ReminderResponse> reminders,
			AppContracts.DailyHealthSummaryResponse todaySummary,
			Optional<UricAcidRecordEntity> latestUricAcid,
			Optional<HydrationRecordEntity> latestHydration,
			Optional<FlareRecordEntity> latestFlare,
			List<LabReportRecordEntity> labReports,
			AppContracts.MedicationPlanResponse medicationPlan,
			AppContracts.MedicationAdherenceSummaryResponse medicationAdherence
	) {
		List<AppContracts.TodayActionItemResponse> actions = new ArrayList<>();

		actions.add(new AppContracts.TodayActionItemResponse(
				"risk-review",
				"RISK",
				"先看清今天的风险和边界",
				reminders.isEmpty()
						? "今天先完成一项关键记录，再回来查看系统是否生成新的提醒。"
						: "优先阅读今天最重要的提醒，确认是否需要联系医生或尽快线下就医。",
				"HIGH",
				reminders.isEmpty() ? "PENDING" : "READY"
		));

		boolean hasMedicationPlan = medicationPlan != null && medicationPlan.currentMedications() != null
				&& !medicationPlan.currentMedications().isEmpty();
		boolean medicationDone = hasMedicationPlan
				&& medicationAdherence != null
				&& medicationAdherence.plannedDoseCount() > 0
				&& medicationAdherence.takenDoseCount() == medicationAdherence.plannedDoseCount()
				&& medicationAdherence.missedDoseCount() == 0
				&& medicationAdherence.skippedDoseCount() == 0
				&& medicationAdherence.overdueItems().isEmpty();
		boolean hasMedicationOverdue = hasMedicationPlan
				&& medicationAdherence != null
				&& !medicationAdherence.overdueItems().isEmpty();
		boolean hasMedicationException = hasMedicationPlan
				&& medicationAdherence != null
				&& (medicationAdherence.missedDoseCount() > 0 || medicationAdherence.skippedDoseCount() > 0);
		actions.add(new AppContracts.TodayActionItemResponse(
				"medication-check",
				"MEDICATION",
				!hasMedicationPlan
						? "补齐你的用药计划"
						: medicationDone
								? "今天的用药已确认完成"
								: hasMedicationOverdue
										? "补完今天未确认的用药"
										: hasMedicationException
												? "复盘今天的用药执行情况"
												: "确认今天的用药计划",
				!hasMedicationPlan
						? "先把医生已经开立或你正在执行的药物方案录入，后续提醒和复盘才会更可靠。"
						: medicationDone
								? "今天计划剂次已全部确认，当前连续完成 " + medicationAdherence.currentStreakDays() + " 天。"
								: hasMedicationOverdue
										? "还有待确认的时段：" + summarizeMedicationOverdueItems(medicationAdherence.overdueItems())
										: hasMedicationException
												? "今天已记录漏服或跳过，请不要自行加倍补服。"
												: "当前用药包括：" + summarizeMedicationNames(medicationPlan.currentMedications()),
				"HIGH",
				!hasMedicationPlan || hasMedicationOverdue
						? "PENDING"
						: medicationDone
								? "DONE"
								: "READY"
		));

		boolean hydrationDone = latestHydration.isPresent() && isSameDay(latestHydration.get().getCheckedAt(), today);
		actions.add(new AppContracts.TodayActionItemResponse(
				"hydration-log",
				"HYDRATION",
				hydrationDone ? "继续补足今天的饮水量" : "记录今天的饮水和尿液颜色",
				hydrationDone
						? "你今天已经有补水记录，接下来继续观察尿液颜色是否仍偏深。"
						: "今天至少补一条饮水与尿液颜色记录，这是判断发作风险最重要的基础数据之一。",
				"HIGH",
				hydrationDone ? "DONE" : "PENDING"
		));

		boolean hasRecentLab = !labReports.isEmpty() && labReports.getFirst().getReportDate() != null
				&& !labReports.getFirst().getReportDate().isBefore(today.minusDays(7));
		boolean needsRiskReview = (todaySummary != null && defaultInt(todaySummary.highRiskMealCount()) > 0)
				|| latestFlare.isPresent()
				|| hasRecentLab
				|| latestUricAcid.map(item -> defaultInt(item.getUaValue()) >= 420).orElse(false);
		actions.add(new AppContracts.TodayActionItemResponse(
				"review-signal",
				"REVIEW",
				needsRiskReview ? "复盘这次异常信号" : "保持今天的低风险节奏",
				needsRiskReview
						? "把最近一次异常饮食、发作、尿酸或化验结果串起来看，明确下一步是补水、复测还是联系医生。"
						: "如果今天没有明显异常，就维持当前节奏，优先保持低嘌呤饮食和规律记录。",
				"MEDIUM",
				needsRiskReview ? "READY" : "DONE"
		));

		return actions.stream().limit(4).toList();
	}

	private AppContracts.MedicationAdherenceSummaryResponse buildMedicationAdherenceSummary(
			String userId,
			AppContracts.MedicationPlanResponse medicationPlan,
			int days
	) {
		int safeDays = Math.max(days, 1);
		LocalDate today = LocalDate.now();
		LocalDate startDate = today.minusDays(safeDays - 1L);
		List<MedicationCheckinEntity> checkins = medicationCheckinRepository
				.findByUserCodeAndCheckinDateGreaterThanEqualOrderByCheckinDateDescCheckinAtDesc(userId, startDate);
		List<AppContracts.MedicationItem> medications = medicationPlan == null || medicationPlan.currentMedications() == null
				? List.of()
				: medicationPlan.currentMedications();

		if (medications.isEmpty()) {
			return new AppContracts.MedicationAdherenceSummaryResponse(
					today.toString(),
					0,
					0,
					0,
					0,
					0,
					0,
					List.of(),
					List.of("先补齐当前用药计划，后续才能进行每日依从打卡和漏服复盘。"),
					checkins.stream().limit(8).map(this::toMedicationCheckinResponse).toList()
			);
		}

		Map<String, List<String>> medicationPeriods = buildMedicationPeriods(medications);
		Map<LocalDate, Map<String, MedicationCheckinEntity>> checkinsByDate = groupMedicationCheckinsByDate(checkins);
		Map<String, MedicationCheckinEntity> todayCheckins = checkinsByDate.getOrDefault(today, Map.of());
		DailyMedicationStats todayStats = calculateMedicationStats(medicationPeriods, todayCheckins);
		List<String> overdueItems = collectOverdueItems(medicationPeriods, todayCheckins);
		int adherenceRate = todayStats.plannedDoseCount() == 0
				? 0
				: (int) Math.round(todayStats.takenDoseCount() * 100.0 / todayStats.plannedDoseCount());
		int currentStreakDays = calculateMedicationStreak(today, safeDays, medicationPeriods, checkinsByDate);
		List<String> nextActions = buildMedicationNextActions(todayStats, overdueItems, adherenceRate, currentStreakDays);

		return new AppContracts.MedicationAdherenceSummaryResponse(
				today.toString(),
				todayStats.plannedDoseCount(),
				todayStats.takenDoseCount(),
				todayStats.missedDoseCount(),
				todayStats.skippedDoseCount(),
				adherenceRate,
				currentStreakDays,
				overdueItems,
				nextActions,
				checkins.stream().limit(8).map(this::toMedicationCheckinResponse).toList()
		);
	}

	private AppContracts.MedicationWeeklyReportResponse buildMedicationWeeklyReport(
			String userId,
			AppContracts.MedicationPlanResponse medicationPlan,
			int days
	) {
		int safeDays = Math.max(days, 1);
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusDays(safeDays - 1L);
		List<MedicationCheckinEntity> checkins = medicationCheckinRepository
				.findByUserCodeAndCheckinDateGreaterThanEqualOrderByCheckinDateDescCheckinAtDesc(userId, startDate);
		List<AppContracts.MedicationItem> medications = medicationPlan == null || medicationPlan.currentMedications() == null
				? List.of()
				: medicationPlan.currentMedications();
		Map<String, List<String>> medicationPeriods = buildMedicationPeriods(medications);
		Map<LocalDate, Map<String, MedicationCheckinEntity>> checkinsByDate = groupMedicationCheckinsByDate(checkins);
		List<AppContracts.MedicationAdherenceDayResponse> dailyBreakdown = new ArrayList<>();
		int plannedDoseCount = 0;
		int takenDoseCount = 0;
		int missedDoseCount = 0;
		int skippedDoseCount = 0;
		int overdueDoseCount = 0;

		for (int offset = safeDays - 1; offset >= 0; offset--) {
			LocalDate currentDate = endDate.minusDays(offset);
			DailyMedicationStats stats = calculateMedicationStats(
					medicationPeriods,
					checkinsByDate.getOrDefault(currentDate, Map.of())
			);
			plannedDoseCount += stats.plannedDoseCount();
			takenDoseCount += stats.takenDoseCount();
			missedDoseCount += stats.missedDoseCount();
			skippedDoseCount += stats.skippedDoseCount();
			overdueDoseCount += stats.overdueCount();
			int adherenceRate = stats.plannedDoseCount() == 0
					? 0
					: (int) Math.round(stats.takenDoseCount() * 100.0 / stats.plannedDoseCount());
			dailyBreakdown.add(new AppContracts.MedicationAdherenceDayResponse(
					currentDate.toString(),
					stats.plannedDoseCount(),
					stats.takenDoseCount(),
					stats.missedDoseCount(),
					stats.skippedDoseCount(),
					adherenceRate
			));
		}

		int adherenceRate = plannedDoseCount == 0 ? 0 : (int) Math.round(takenDoseCount * 100.0 / plannedDoseCount);
		int currentStreakDays = calculateMedicationStreak(endDate, safeDays, medicationPeriods, checkinsByDate);
		int longestStreakDays = calculateLongestMedicationStreak(startDate, endDate, medicationPeriods, checkinsByDate);
		List<AppContracts.MedicationRefillAlertResponse> refillAlerts = buildMedicationRefillAlerts(medications);
		List<String> focusMedications = buildMedicationFocusMedications(startDate, endDate, medicationPeriods, checkinsByDate);
		List<String> highlights = buildMedicationWeeklyHighlights(
				adherenceRate,
				takenDoseCount,
				plannedDoseCount,
				missedDoseCount,
				refillAlerts,
				longestStreakDays
		);
		List<String> nextActions = buildMedicationWeeklyNextActions(
				adherenceRate,
				missedDoseCount,
				overdueDoseCount,
				refillAlerts
		);

		return new AppContracts.MedicationWeeklyReportResponse(
				startDate.toString(),
				endDate.toString(),
				plannedDoseCount,
				takenDoseCount,
				missedDoseCount,
				skippedDoseCount,
				overdueDoseCount,
				adherenceRate,
				currentStreakDays,
				longestStreakDays,
				dailyBreakdown,
				focusMedications,
				refillAlerts,
				highlights,
				nextActions,
				Instant.now()
		);
	}

	private Map<String, List<String>> buildMedicationPeriods(List<AppContracts.MedicationItem> medications) {
		Map<String, List<String>> medicationPeriods = new LinkedHashMap<>();
		medications.forEach(item -> medicationPeriods.put(item.name(), resolveMedicationPeriods(item.frequency())));
		return medicationPeriods;
	}

	private Map<LocalDate, Map<String, MedicationCheckinEntity>> groupMedicationCheckinsByDate(List<MedicationCheckinEntity> checkins) {
		Map<LocalDate, Map<String, MedicationCheckinEntity>> grouped = new LinkedHashMap<>();
		checkins.forEach(entity -> grouped
				.computeIfAbsent(entity.getCheckinDate(), ignored -> new LinkedHashMap<>())
				.merge(
						medicationDoseKey(entity.getMedicationName(), entity.getScheduledPeriod()),
						entity,
						this::pickLatestCheckin
				));
		return grouped;
	}

	private DailyMedicationStats calculateMedicationStats(
			Map<String, List<String>> medicationPeriods,
			Map<String, MedicationCheckinEntity> dailyCheckins
	) {
		int plannedDoseCount = 0;
		int takenDoseCount = 0;
		int missedDoseCount = 0;
		int skippedDoseCount = 0;
		int overdueCount = 0;

		for (Map.Entry<String, List<String>> entry : medicationPeriods.entrySet()) {
			for (String scheduledPeriod : entry.getValue()) {
				plannedDoseCount++;
				MedicationCheckinEntity entity = dailyCheckins.get(medicationDoseKey(entry.getKey(), scheduledPeriod));
				if (entity == null) {
					overdueCount++;
					continue;
				}
				switch (normalizeMedicationStatus(entity.getCheckinStatus())) {
					case "TAKEN" -> takenDoseCount++;
					case "MISSED" -> missedDoseCount++;
					case "SKIPPED" -> skippedDoseCount++;
					default -> overdueCount++;
				}
			}
		}

		return new DailyMedicationStats(
				plannedDoseCount,
				takenDoseCount,
				missedDoseCount,
				skippedDoseCount,
				overdueCount
		);
	}

	private List<String> collectOverdueItems(
			Map<String, List<String>> medicationPeriods,
			Map<String, MedicationCheckinEntity> dailyCheckins
	) {
		List<String> overdueItems = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : medicationPeriods.entrySet()) {
			for (String scheduledPeriod : entry.getValue()) {
				if (!dailyCheckins.containsKey(medicationDoseKey(entry.getKey(), scheduledPeriod))) {
					overdueItems.add(entry.getKey() + " - " + displayScheduledPeriod(scheduledPeriod));
				}
			}
		}
		return overdueItems;
	}

	private int calculateMedicationStreak(
			LocalDate today,
			int safeDays,
			Map<String, List<String>> medicationPeriods,
			Map<LocalDate, Map<String, MedicationCheckinEntity>> checkinsByDate
	) {
		int streak = 0;
		for (int offset = 0; offset < safeDays; offset++) {
			LocalDate currentDate = today.minusDays(offset);
			DailyMedicationStats stats = calculateMedicationStats(
					medicationPeriods,
					checkinsByDate.getOrDefault(currentDate, Map.of())
			);
			if (stats.plannedDoseCount() == 0
					|| stats.takenDoseCount() != stats.plannedDoseCount()
					|| stats.missedDoseCount() > 0
					|| stats.skippedDoseCount() > 0
					|| stats.overdueCount() > 0) {
				break;
			}
			streak++;
		}
		return streak;
	}

	private int calculateLongestMedicationStreak(
			LocalDate startDate,
			LocalDate endDate,
			Map<String, List<String>> medicationPeriods,
			Map<LocalDate, Map<String, MedicationCheckinEntity>> checkinsByDate
	) {
		int longest = 0;
		int current = 0;
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			DailyMedicationStats stats = calculateMedicationStats(
					medicationPeriods,
					checkinsByDate.getOrDefault(date, Map.of())
			);
			boolean completed = stats.plannedDoseCount() > 0
					&& stats.takenDoseCount() == stats.plannedDoseCount()
					&& stats.missedDoseCount() == 0
					&& stats.skippedDoseCount() == 0
					&& stats.overdueCount() == 0;
			if (completed) {
				current++;
				longest = Math.max(longest, current);
			} else {
				current = 0;
			}
		}
		return longest;
	}

	private List<AppContracts.MedicationRefillAlertResponse> buildMedicationRefillAlerts(List<AppContracts.MedicationItem> medications) {
		return medications.stream()
				.filter(item -> item.remainingDays() != null)
				.filter(item -> item.remainingDays() <= resolveRefillThresholdDays(item))
				.sorted(Comparator.comparing(item -> defaultInt(item.remainingDays())))
				.map(item -> new AppContracts.MedicationRefillAlertResponse(
						item.name(),
						item.dosage(),
						item.remainingDays(),
						resolveRefillThresholdDays(item),
						medicationRefillRisk(item),
						buildMedicationRefillSuggestion(item)
				))
				.toList();
	}

	private List<String> buildMedicationFocusMedications(
			LocalDate startDate,
			LocalDate endDate,
			Map<String, List<String>> medicationPeriods,
			Map<LocalDate, Map<String, MedicationCheckinEntity>> checkinsByDate
	) {
		Map<String, Integer> issueCounts = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : medicationPeriods.entrySet()) {
			int count = 0;
			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				Map<String, MedicationCheckinEntity> dailyCheckins = checkinsByDate.getOrDefault(date, Map.of());
				for (String scheduledPeriod : entry.getValue()) {
					MedicationCheckinEntity entity = dailyCheckins.get(medicationDoseKey(entry.getKey(), scheduledPeriod));
					if (entity == null || !"TAKEN".equals(normalizeMedicationStatus(entity.getCheckinStatus()))) {
						count++;
					}
				}
			}
			if (count > 0) {
				issueCounts.put(entry.getKey(), count);
			}
		}

		return issueCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(3)
				.map(entry -> entry.getKey() + "：近 " + ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)) + " 天有 " + entry.getValue() + " 次未完成剂次")
				.toList();
	}

	private List<String> buildMedicationWeeklyHighlights(
			int adherenceRate,
			int takenDoseCount,
			int plannedDoseCount,
			int missedDoseCount,
			List<AppContracts.MedicationRefillAlertResponse> refillAlerts,
			int longestStreakDays
	) {
		List<String> highlights = new ArrayList<>();
		highlights.add("本周已确认 " + takenDoseCount + " / " + plannedDoseCount + " 个计划剂次。");
		if (adherenceRate >= 90) {
			highlights.add("本周依从率达到 " + adherenceRate + "%，执行比较稳定。");
		} else if (adherenceRate >= 70) {
			highlights.add("本周依从率为 " + adherenceRate + "%，还有进一步提升空间。");
		} else {
			highlights.add("本周依从率仅 " + adherenceRate + "%，需要优先收紧提醒和复盘。");
		}
		if (missedDoseCount > 0) {
			highlights.add("本周共记录 " + missedDoseCount + " 次漏服，需要重点看触发场景。");
		}
		if (longestStreakDays >= 2) {
			highlights.add("最长连续完成 " + longestStreakDays + " 天，可作为后续维持节奏的参考。");
		}
		if (!refillAlerts.isEmpty()) {
			highlights.add("当前有 " + refillAlerts.size() + " 种药物接近补药时点。");
		}
		return highlights;
	}

	private List<String> buildMedicationWeeklyNextActions(
			int adherenceRate,
			int missedDoseCount,
			int overdueDoseCount,
			List<AppContracts.MedicationRefillAlertResponse> refillAlerts
	) {
		List<String> actions = new ArrayList<>();
		if (overdueDoseCount > 0) {
			actions.add("把未确认剂次补齐，避免周报里出现大量“未完成但未说明”的空档。");
		}
		if (missedDoseCount > 0) {
			actions.add("把漏服发生的时间、场景和原因补进备注，下次复诊更容易解释。");
		}
		if (adherenceRate < 80) {
			actions.add("建议把高风险药物放进固定时段提醒，先把依从率拉回到 80% 以上。");
		}
		if (!refillAlerts.isEmpty()) {
			actions.add("本周优先安排补药，避免因为药量不足打断连续服药。");
		}
		if (actions.isEmpty()) {
			actions.add("本周执行整体稳定，继续保持当前服药节奏并按时补药即可。");
		}
		return actions;
	}

	private List<String> buildMedicationNextActions(
			DailyMedicationStats todayStats,
			List<String> overdueItems,
			int adherenceRate,
			int currentStreakDays
	) {
		List<String> actions = new ArrayList<>();
		if (todayStats.plannedDoseCount() == 0) {
			actions.add("当前还没有可执行的剂次计划，请先补齐用药方案。");
			return actions;
		}
		if (!overdueItems.isEmpty()) {
			actions.add("今天还有 " + overdueItems.size() + " 个剂次待确认，请优先完成打卡。");
		}
		if (todayStats.missedDoseCount() > 0) {
			actions.add("如果已经漏服，请不要自行加倍补服；不确定如何处理时，请联系医生或药师。");
		}
		if (todayStats.skippedDoseCount() > 0) {
			actions.add("若因不适或检查跳过用药，请把原因写进备注，方便后续复盘。");
		}
		if (adherenceRate < 80) {
			actions.add("今天的依从率偏低，建议固定提醒时段，并在下次复诊时带上用药记录。");
		}
		if (todayStats.takenDoseCount() == todayStats.plannedDoseCount() && overdueItems.isEmpty()) {
			actions.add("今天的计划剂次已全部确认完成，继续保持。");
		}
		if (currentStreakDays >= 3) {
			actions.add("你已经连续完成 " + currentStreakDays + " 天用药确认，可以继续维持这个节奏。");
		}
		if (actions.isEmpty()) {
			actions.add("今天先核对药名、剂量和时段，再按计划完成打卡。");
		}
		return actions;
	}

	private AppContracts.MedicationCheckinResponse toMedicationCheckinResponse(MedicationCheckinEntity entity) {
		return new AppContracts.MedicationCheckinResponse(
				entity.getCheckinCode(),
				entity.getMedicationName(),
				entity.getScheduledPeriod(),
				entity.getCheckinStatus(),
				buildMedicationGuidance(entity.getCheckinStatus()),
				entity.getNoteText(),
				entity.getCheckinDate().toString(),
				entity.getCheckinAt()
		);
	}

	private MedicationCheckinEntity pickLatestCheckin(MedicationCheckinEntity left, MedicationCheckinEntity right) {
		if (left.getCheckinAt() == null) {
			return right;
		}
		if (right.getCheckinAt() == null) {
			return left;
		}
		return left.getCheckinAt().isAfter(right.getCheckinAt()) ? left : right;
	}

	private List<String> resolveMedicationPeriods(String frequency) {
		String normalized = normalizeSimpleText(frequency);
		if (Set.of("twice-daily", "bid", "q12h", "每日两次", "一天两次").contains(normalized)) {
			return List.of("MORNING", "EVENING");
		}
		if (Set.of("three-times-daily", "tid", "q8h", "每日三次", "一天三次").contains(normalized)) {
			return List.of("MORNING", "NOON", "EVENING");
		}
		if (Set.of("bedtime", "睡前", "每晚睡前").contains(normalized)) {
			return List.of("BEDTIME");
		}
		return List.of("MORNING");
	}

	private String normalizeScheduledPeriod(String value) {
		String normalized = normalizeSimpleText(value);
		return switch (normalized) {
			case "morning", "am", "早餐后", "早上", "早晨", "上午" -> "MORNING";
			case "noon", "midday", "午间", "中午", "午后" -> "NOON";
			case "evening", "pm", "晚间", "晚上", "晚饭后" -> "EVENING";
			case "bedtime", "睡前", "夜间" -> "BEDTIME";
			default -> throw new BusinessException("MEDICATION_PERIOD_INVALID", "不支持的服药时段，请使用 MORNING / NOON / EVENING / BEDTIME。");
		};
	}

	private String normalizeMedicationStatus(String value) {
		String normalized = normalizeSimpleText(value);
		return switch (normalized) {
			case "taken", "done", "已服用", "已吃药" -> "TAKEN";
			case "missed", "漏服", "忘记服用" -> "MISSED";
			case "skipped", "skip", "跳过", "暂不服用" -> "SKIPPED";
			default -> throw new BusinessException("MEDICATION_STATUS_INVALID", "不支持的用药状态，请使用 TAKEN / MISSED / SKIPPED。");
		};
	}

	private String buildMedicationGuidance(String status) {
		return switch (normalizeMedicationStatus(status)) {
			case "TAKEN" -> "已记录本次服药，请继续按计划观察症状和不适反应。";
			case "MISSED" -> "如果已经漏服，请不要自行加倍补服；不确定如何处理时，请联系医生或药师。";
			case "SKIPPED" -> "已记录本次跳过，请补充原因，并在复诊时同步给医生。";
			default -> "请按医生方案执行。";
		};
	}

	private String summarizeMedicationOverdueItems(List<String> overdueItems) {
		if (overdueItems == null || overdueItems.isEmpty()) {
			return "暂无";
		}
		String summary = overdueItems.stream().limit(3).collect(Collectors.joining("、"));
		return overdueItems.size() > 3 ? summary + " 等" : summary;
	}

	private String displayScheduledPeriod(String scheduledPeriod) {
		return switch (normalizeScheduledPeriod(scheduledPeriod)) {
			case "MORNING" -> "早晨";
			case "NOON" -> "中午";
			case "EVENING" -> "晚上";
			case "BEDTIME" -> "睡前";
			default -> scheduledPeriod;
		};
	}

	private int resolveRefillThresholdDays(AppContracts.MedicationItem item) {
		return item.refillThresholdDays() == null ? 3 : item.refillThresholdDays();
	}

	private AppContracts.RiskLevel medicationRefillRisk(AppContracts.MedicationItem item) {
		return defaultInt(item.remainingDays()) <= 1
				? AppContracts.RiskLevel.RED
				: AppContracts.RiskLevel.YELLOW;
	}

	private String buildMedicationRefillSuggestion(AppContracts.MedicationItem item) {
		int remainingDays = defaultInt(item.remainingDays());
		return remainingDays <= 1
				? item.name() + " 预计只够 " + remainingDays + " 天，建议今天就安排补药。"
				: item.name() + " 预计还能用 " + remainingDays + " 天，建议提前开药或购药。";
	}

	private String medicationDoseKey(String medicationName, String scheduledPeriod) {
		return normalizeMedicationName(medicationName) + "|" + normalizeScheduledPeriod(scheduledPeriod);
	}

	private String normalizeMedicationName(String value) {
		String normalized = trimToNull(value);
		return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
	}

	private String normalizeSimpleText(String value) {
		String normalized = trimToNull(value);
		return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private UserProfileEntity ensureProfile(String userId) {
		return userProfileRepository.findByUserCode(userId)
				.orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "用户档案不存在，请先登录"));
	}

	private UserAccountEntity ensureAccount(String userId) {
		return userAccountRepository.findByUserCode(userId)
				.orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
	}

	private AppContracts.AuthTokenResponse createAuthSessionResponse(
			String userId,
			String nickname,
			String authMode,
			String accountType,
			String accountIdentifier,
			boolean privacyConsentCompleted
	) {
		Instant now = Instant.now();
		String sessionCode = idGenerator.next("session");
		String token = UUID.randomUUID().toString().replace("-", "");
		Instant expiresAt = now.plus(appProperties.getAuthTokenDays(), ChronoUnit.DAYS);
		AuthSessionEntity sessionEntity = new AuthSessionEntity();
		sessionEntity.setUserCode(userId);
		sessionEntity.setNickname(nickname);
		sessionEntity.setSessionCode(sessionCode);
		sessionEntity.setAuthMode(authMode);
		sessionEntity.setAccountType(accountType);
		sessionEntity.setAccountIdentifier(accountIdentifier);
		sessionEntity.setPrivacyConsentCompleted(privacyConsentCompleted);
		sessionEntity.setToken(token);
		sessionEntity.setExpiresAt(expiresAt);
		sessionEntity.setCreatedAt(now);
		sessionEntity.setLastSeenAt(now);
		authSessionRepository.save(sessionEntity);

		UserSession session = toUserSession(sessionEntity);
		sessionCacheService.put(session);
		return new AppContracts.AuthTokenResponse(
				session.sessionCode(),
				userId,
				nickname,
				authMode,
				accountType,
				accountIdentifier,
				privacyConsentCompleted,
				session.createdAt(),
				session.lastSeenAt(),
				token,
				"Bearer",
				expiresAt
		);
	}

	private UserSession toUserSession(AuthSessionEntity sessionEntity) {
		return new UserSession(
				sessionEntity.getSessionCode(),
				sessionEntity.getUserCode(),
				sessionEntity.getNickname(),
				sessionEntity.getAuthMode(),
				sessionEntity.getAccountType(),
				sessionEntity.getAccountIdentifier(),
				sessionEntity.isPrivacyConsentCompleted(),
				sessionEntity.getCreatedAt(),
				sessionEntity.getLastSeenAt(),
				sessionEntity.getToken(),
				sessionEntity.getExpiresAt()
		);
	}

	private UserSession requireOwnedSession(String userId, String token) {
		UserSession session = requireSession(token);
		if (!Objects.equals(session.userId(), userId)) {
			throw new BusinessException("FORBIDDEN", "当前会话与用户身份不匹配");
		}
		return session;
	}

	private AppContracts.AuthActiveSessionResponse toActiveSessionResponse(AuthSessionEntity entity, boolean currentSession) {
		return new AppContracts.AuthActiveSessionResponse(
				entity.getSessionCode(),
				entity.getAuthMode(),
				entity.getAccountType(),
				entity.getAccountIdentifier(),
				currentSession,
				entity.getCreatedAt(),
				entity.getLastSeenAt(),
				entity.getExpiresAt()
		);
	}

	private PrivacyConsentRecordEntity savePrivacyConsent(
			String userId,
			AppContracts.PrivacyConsentSubmitRequest request,
			String sourceType
	) {
		Instant now = Instant.now();
		PrivacyConsentRecordEntity entity = new PrivacyConsentRecordEntity();
		entity.setConsentCode(idGenerator.next("consent"));
		entity.setUserCode(userId);
		entity.setConsentVersion(request.consentVersion().trim());
		entity.setPrivacyPolicyVersion(request.privacyPolicyVersion().trim());
		entity.setPrivacyAccepted(Boolean.TRUE.equals(request.privacyAccepted()));
		entity.setTermsAccepted(Boolean.TRUE.equals(request.termsAccepted()));
		entity.setMedicalDataAuthorized(defaultBoolean(request.medicalDataAuthorized(), true));
		entity.setFamilyCollaborationAuthorized(defaultBoolean(request.familyCollaborationAuthorized(), true));
		entity.setNotificationAuthorized(defaultBoolean(request.notificationAuthorized(), true));
		entity.setSourceType(sourceType);
		entity.setEffectiveAt(now);
		entity.setCreatedAt(now);
		return privacyConsentRecordRepository.save(entity);
	}

	private AppContracts.PrivacyConsentResponse toPrivacyConsentResponse(PrivacyConsentRecordEntity entity) {
		return new AppContracts.PrivacyConsentResponse(
				entity.getConsentCode(),
				entity.getUserCode(),
				entity.getConsentVersion(),
				entity.getPrivacyPolicyVersion(),
				entity.isPrivacyAccepted(),
				entity.isTermsAccepted(),
				entity.isMedicalDataAuthorized(),
				entity.isFamilyCollaborationAuthorized(),
				entity.isNotificationAuthorized(),
				entity.getSourceType(),
				entity.getEffectiveAt(),
				entity.getCreatedAt()
		);
	}

	private void validateRequiredPrivacyConsent(AppContracts.PrivacyConsentSubmitRequest consent) {
		if (consent == null) {
			throw new BusinessException("PRIVACY_CONSENT_REQUIRED", "请先完成隐私政策和服务条款授权");
		}
		if (!Boolean.TRUE.equals(consent.privacyAccepted()) || !Boolean.TRUE.equals(consent.termsAccepted())) {
			throw new BusinessException("PRIVACY_CONSENT_REQUIRED", "必须同意隐私政策和服务条款后才能继续");
		}
	}

	private void validatePassword(String password, String confirmPassword) {
		if (!StringUtils.hasText(password) || password.trim().length() < 8) {
			throw new BusinessException("PASSWORD_TOO_SHORT", "密码至少需要 8 位");
		}
		if (!Objects.equals(password, confirmPassword)) {
			throw new BusinessException("PASSWORD_CONFIRM_MISMATCH", "两次输入的密码不一致");
		}
	}

	private String normalizeAccountType(String value) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException("ACCOUNT_TYPE_REQUIRED", "账号类型不能为空");
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (Set.of("EMAIL", "PHONE").contains(normalized)) {
			return normalized;
		}
		throw new BusinessException("ACCOUNT_TYPE_INVALID", "账号类型仅支持 EMAIL 或 PHONE");
	}

	private String normalizePrincipal(String accountType, String value) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException("ACCOUNT_REQUIRED", "账号不能为空");
		}
		String normalized = value.trim();
		if ("EMAIL".equals(accountType)) {
			normalized = normalized.toLowerCase(Locale.ROOT);
			if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
				throw new BusinessException("EMAIL_INVALID", "请输入有效的邮箱账号");
			}
			return normalized;
		}
		String digits = normalized.replaceAll("[^0-9+]", "");
		if (digits.length() < 11) {
			throw new BusinessException("PHONE_INVALID", "请输入有效的手机号");
		}
		return digits;
	}

	private boolean hasCompletedPrivacyConsent(String userId) {
		return privacyConsentRecordRepository.findFirstByUserCodeOrderByEffectiveAtDesc(userId)
				.map(item -> item.isPrivacyAccepted() && item.isTermsAccepted())
				.orElse(false);
	}

	private boolean defaultBoolean(Boolean value, boolean fallback) {
		return value == null ? fallback : value;
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

	private AppContracts.LabReportReviewComparisonResponse toLabComparison(
			AppContracts.LabIndicator current,
			AppContracts.LabIndicator previous
	) {
		BigDecimal previousValue = previous == null ? null : previous.value();
		BigDecimal deltaValue = current.value() == null || previousValue == null
				? null
				: current.value().subtract(previousValue);
		String trend;
		String interpretation;
		if (deltaValue == null) {
			trend = "NO_BASELINE";
			interpretation = "当前没有可直接对比的上一份同类指标。";
		} else if (deltaValue.compareTo(BigDecimal.ZERO) > 0) {
			trend = "UP";
			interpretation = "比上一次上升 " + formatDelta(deltaValue) + " " + safeUnit(current.unit()) + "。";
		} else if (deltaValue.compareTo(BigDecimal.ZERO) < 0) {
			trend = "DOWN";
			interpretation = "比上一次下降 " + formatDelta(deltaValue.abs()) + " " + safeUnit(current.unit()) + "。";
		} else {
			trend = "STABLE";
			interpretation = "与上一次基本持平。";
		}
		return new AppContracts.LabReportReviewComparisonResponse(
				current.code(),
				current.name(),
				current.value(),
				previousValue,
				deltaValue,
				current.unit(),
				current.referenceRange(),
				current.riskLevel(),
				trend,
				interpretation
		);
	}

	private String buildLabReviewSummary(
			LabReportRecordEntity report,
			AppContracts.LabIndicator currentUricAcid,
			AppContracts.LabIndicator previousUricAcid,
			Integer targetUricAcidValue,
			LabReportRecordEntity previousReport
	) {
		if (currentUricAcid == null) {
			return previousReport == null
					? "本次化验单已完成解析，但未识别到明确的尿酸指标，建议手动核对关键结果。"
					: "本次化验单未识别到明确的尿酸指标，已保留与上一份报告的参考对比。";
		}
		String base = "本次尿酸为 " + formatDelta(currentUricAcid.value()) + " " + safeUnit(currentUricAcid.unit());
		if (targetUricAcidValue != null) {
			base += currentUricAcid.value().compareTo(BigDecimal.valueOf(targetUricAcidValue)) <= 0
					? "，已经达到个人目标范围。"
					: "，仍高于个人目标 " + targetUricAcidValue + " " + safeUnit(currentUricAcid.unit()) + "。";
		} else {
			base += "，建议结合个人目标值一起判断。";
		}
		if (previousUricAcid == null) {
			return base + " 当前还缺少可直接对比的上一份尿酸结果。";
		}
		BigDecimal delta = currentUricAcid.value().subtract(previousUricAcid.value());
		if (delta.compareTo(BigDecimal.ZERO) > 0) {
			return base + " 与上一份相比上升 " + formatDelta(delta) + " " + safeUnit(currentUricAcid.unit()) + "。";
		}
		if (delta.compareTo(BigDecimal.ZERO) < 0) {
			return base + " 与上一份相比下降 " + formatDelta(delta.abs()) + " " + safeUnit(currentUricAcid.unit()) + "。";
		}
		return base + " 与上一份尿酸结果基本持平。";
	}

	private String buildLabTargetConclusion(
			AppContracts.LabIndicator currentUricAcid,
			AppContracts.LabIndicator previousUricAcid,
			Integer targetUricAcidValue,
			boolean withinTarget
	) {
		if (currentUricAcid == null) {
			return "本次未识别到尿酸指标，暂时无法判断是否达到个人目标。";
		}
		if (targetUricAcidValue == null) {
			return "你的个人目标尿酸尚未设置，建议先补充档案中的目标值。";
		}
		if (withinTarget) {
			return previousUricAcid != null && previousUricAcid.value() != null
					&& previousUricAcid.value().compareTo(BigDecimal.valueOf(targetUricAcidValue)) > 0
					? "这次已经回到个人目标以内，比上一份更接近稳定控制。"
					: "这次处于个人目标范围内，继续保持当前复查和管理节奏。";
		}
		BigDecimal gap = currentUricAcid.value().subtract(BigDecimal.valueOf(targetUricAcidValue)).max(BigDecimal.ZERO);
		return "这次仍高于个人目标约 " + formatDelta(gap) + " " + safeUnit(currentUricAcid.unit()) + "，还需要继续收紧日常管理。";
	}

	private List<String> buildLabKeyChanges(
			AppContracts.LabIndicator currentUricAcid,
			AppContracts.LabIndicator previousUricAcid,
			List<AppContracts.LabReportReviewComparisonResponse> comparisons,
			LabReportRecordEntity previousReport
	) {
		List<String> changes = new ArrayList<>();
		if (previousReport == null) {
			changes.add("这是当前工作台里的第一份化验单，后续报告会自动生成趋势对比。");
		}
		if (currentUricAcid != null) {
			changes.add("本次尿酸结果为 " + formatDelta(currentUricAcid.value()) + " " + safeUnit(currentUricAcid.unit()) + "。");
		}
		if (currentUricAcid != null && previousUricAcid != null && previousUricAcid.value() != null) {
			BigDecimal delta = currentUricAcid.value().subtract(previousUricAcid.value());
			if (delta.compareTo(BigDecimal.ZERO) > 0) {
				changes.add("尿酸比上一份上升了 " + formatDelta(delta) + " " + safeUnit(currentUricAcid.unit()) + "。");
			} else if (delta.compareTo(BigDecimal.ZERO) < 0) {
				changes.add("尿酸比上一份下降了 " + formatDelta(delta.abs()) + " " + safeUnit(currentUricAcid.unit()) + "。");
			}
		}
		comparisons.stream()
				.filter(item -> !"UA".equalsIgnoreCase(defaultString(item.code())))
				.filter(item -> item.currentRiskLevel() != AppContracts.RiskLevel.GREEN || item.deltaValue() != null)
				.limit(2)
				.forEach(item -> changes.add((StringUtils.hasText(item.name()) ? item.name() : item.code()) + "：" + item.interpretation()));
		return changes.stream().limit(4).toList();
	}

	private String buildLabFollowUpRecommendation(
			LabReportRecordEntity report,
			AppContracts.LabIndicator currentUricAcid,
			Integer targetUricAcidValue
	) {
		if (toRiskLevel(report.getOverallRiskLevel()) == AppContracts.RiskLevel.RED) {
			return "建议 1 到 2 周内完成复查，并尽快带上本次结果与近期症状咨询医生。";
		}
		if (currentUricAcid != null && targetUricAcidValue != null
				&& currentUricAcid.value() != null
				&& currentUricAcid.value().compareTo(BigDecimal.valueOf(targetUricAcidValue)) > 0) {
			return "建议 2 到 4 周内复查尿酸，并复盘近期饮食、饮酒、补水和用药执行。";
		}
		return "建议按当前随访节奏在 4 到 8 周内复查，继续观察是否稳定维持在目标范围。";
	}

	private List<String> buildLabNextActions(
			LabReportRecordEntity report,
			AppContracts.LabIndicator currentUricAcid,
			Integer targetUricAcidValue,
			List<AppContracts.LabReportReviewComparisonResponse> comparisons
	) {
		List<String> actions = new ArrayList<>();
		if (currentUricAcid == null) {
			actions.add("先手动核对化验单上的尿酸结果，必要时重新上传更清晰的图片或 PDF。");
		} else if (targetUricAcidValue != null && currentUricAcid.value().compareTo(BigDecimal.valueOf(targetUricAcidValue)) > 0) {
			actions.add("把这次尿酸结果和最近 7 天的饮食、饮酒、补水、用药记录一起复盘。");
		} else {
			actions.add("继续保持当前用药和饮食节奏，把目标值维持在稳定区间。");
		}
		if (comparisons.stream().anyMatch(item -> item.currentRiskLevel() == AppContracts.RiskLevel.RED)) {
			actions.add("把红色风险指标单独整理出来，下次复诊时优先和医生确认。");
		} else {
			actions.add("把这次关键指标和上一次结果放在一起看，确认变化方向是否持续。");
		}
		actions.add(buildLabFollowUpRecommendation(report, currentUricAcid, targetUricAcidValue));
		return actions.stream().limit(3).toList();
	}

	private List<String> buildLabTrustNotes(
			LabReportRecordEntity previousReport,
			List<AppContracts.LabIndicator> currentIndicators
	) {
		List<String> notes = new ArrayList<>();
		notes.add("复盘结论只基于当前已识别的指标与档案目标值生成，不能替代医生诊断。");
		if (previousReport == null) {
			notes.add("当前缺少上一份报告基线，趋势判断会比连续复查时更弱。");
		}
		if (currentIndicators.isEmpty()) {
			notes.add("如果 OCR 漏掉了关键指标，请重新上传更清晰的报告或手动补录。");
		}
		return notes;
	}

	private Optional<AppContracts.LabIndicator> findUricAcidIndicator(List<AppContracts.LabIndicator> indicators) {
		return indicators.stream()
				.filter(item -> isUricAcidIndicator(item.code(), item.name()))
				.findFirst();
	}

	private boolean isUricAcidIndicator(String code, String name) {
		String merged = (defaultString(code) + "|" + defaultString(name)).toLowerCase(Locale.ROOT);
		return merged.contains("ua") || merged.contains("uric") || merged.contains("尿酸");
	}

	private String indicatorKey(AppContracts.LabIndicator indicator) {
		String code = defaultString(indicator.code()).trim().toUpperCase(Locale.ROOT);
		if (StringUtils.hasText(code)) {
			return code;
		}
		return defaultString(indicator.name()).trim().toLowerCase(Locale.ROOT);
	}

	private int riskRank(AppContracts.RiskLevel riskLevel) {
		if (riskLevel == null) {
			return 0;
		}
		return switch (riskLevel) {
			case RED -> 3;
			case YELLOW -> 2;
			case GREEN -> 1;
		};
	}

	private BigDecimal absoluteValue(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.abs();
	}

	private String formatDelta(BigDecimal value) {
		if (value == null) {
			return "-";
		}
		return value.stripTrailingZeros().toPlainString();
	}

	private String safeUnit(String unit) {
		return StringUtils.hasText(unit) ? unit : "";
	}

	private String defaultString(String value) {
		return value == null ? "" : value;
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

	private boolean isSameDay(Instant instant, LocalDate targetDate) {
		return instant != null && instant.atZone(ZoneId.systemDefault()).toLocalDate().isEqual(targetDate);
	}

	private String summarizeMedicationNames(List<AppContracts.MedicationItem> items) {
		return items.stream()
				.limit(2)
				.map(AppContracts.MedicationItem::name)
				.collect(Collectors.joining("、"))
				+ (items.size() > 2 ? " 等" : "");
	}

	private AppContracts.RiskLevel maxRisk(AppContracts.RiskLevel left, AppContracts.RiskLevel right) {
		return riskRank(left) >= riskRank(right) ? left : right;
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
}
