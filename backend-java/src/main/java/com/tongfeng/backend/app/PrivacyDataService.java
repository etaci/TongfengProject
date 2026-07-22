package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongfeng.backend.app.persistence.entity.AuthSessionEntity;
import com.tongfeng.backend.app.persistence.entity.PrivacyConsentRecordEntity;
import com.tongfeng.backend.app.persistence.entity.StoredFileEntity;
import com.tongfeng.backend.app.persistence.repo.AuthSessionRepository;
import com.tongfeng.backend.app.persistence.repo.PrivacyConsentRecordRepository;
import com.tongfeng.backend.app.persistence.repo.StoredFileRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PrivacyDataService {

	private static final Set<String> WITHDRAWABLE_SCOPES = Set.of(
			"MEDICAL_DATA",
			"FAMILY_COLLABORATION",
			"NOTIFICATION"
	);
	private static final List<String> USER_TABLES = List.of(
			"doctor_visit_share", "clinical_decision_audit", "mvp_usage_event", "access_audit", "health_record_audit",
			"growth_reward_claim", "growth_badge", "growth_point_log", "growth_profile",
			"device_sync_event", "device_binding", "weather_daily_snapshot", "proactive_care_setting",
			"daily_health_summary", "reminder_event", "medication_checkin", "medication_plan",
			"lab_report_record", "hydration_record", "flare_record", "blood_pressure_record",
			"weight_record", "uric_acid_record", "meal_record", "stored_file",
			"privacy_consent_record", "auth_verification_challenge", "auth_session", "auth_identity",
			"user_profile"
	);

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final PrivacyConsentRecordRepository consentRepository;
	private final StoredFileRepository storedFileRepository;
	private final AuthSessionRepository authSessionRepository;
	private final LocalFileStorageService fileStorageService;
	private final SessionCacheService sessionCacheService;
	private final IdGenerator idGenerator;

	public PrivacyDataService(
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper,
			PrivacyConsentRecordRepository consentRepository,
			StoredFileRepository storedFileRepository,
			AuthSessionRepository authSessionRepository,
			LocalFileStorageService fileStorageService,
			SessionCacheService sessionCacheService,
			IdGenerator idGenerator
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.consentRepository = consentRepository;
		this.storedFileRepository = storedFileRepository;
		this.authSessionRepository = authSessionRepository;
		this.fileStorageService = fileStorageService;
		this.sessionCacheService = sessionCacheService;
		this.idGenerator = idGenerator;
	}

	@Transactional(readOnly = true)
	public byte[] exportOwnedData(String userId) {
		Map<String, Object> export = new LinkedHashMap<>();
		export.put("exportVersion", "PRIVACY_EXPORT_V1");
		export.put("generatedAt", Instant.now());
		export.put("userId", userId);
		export.put("account", jdbcTemplate.queryForList(
				"SELECT user_code, nickname, created_at, updated_at FROM user_account WHERE user_code = ?",
				userId
		));
		for (String table : exportTables()) {
			if (tableExists(table)) {
				export.put(table, jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE user_code = ?", userId));
			}
		}
		export.put("family_bindings", jdbcTemplate.queryForList(
				"SELECT * FROM family_binding WHERE patient_user_code = ? OR caregiver_user_code = ?",
				userId,
				userId
		));
		export.put("family_tasks", jdbcTemplate.queryForList(
				"SELECT * FROM family_task WHERE patient_user_code = ? OR caregiver_user_code = ? OR created_by_user_code = ?",
				userId,
				userId,
				userId
		));
		export.put("family_invitations", jdbcTemplate.queryForList(
				"SELECT * FROM family_invite WHERE creator_user_code = ? OR patient_user_code = ? OR accepted_by_user_code = ?",
				userId,
				userId,
				userId
		));
		export.put("access_audits", jdbcTemplate.queryForList(
				"SELECT * FROM access_audit WHERE actor_user_code = ? OR patient_user_code = ?",
				userId,
				userId
		));
		if (tableExists("doctor_visit_share")) {
			export.put("doctor_visit_shares", jdbcTemplate.queryForList(
					"SELECT share_code, lookback_days, expires_at, revoked, revoked_at, created_at FROM doctor_visit_share WHERE user_code = ?",
					userId
			));
		}
		try {
			byte[] manifest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export);
			return buildExportArchive(userId, manifest);
		} catch (JsonProcessingException ex) {
			throw new BusinessException("DATA_EXPORT_FAILED", "生成个人数据导出文件失败");
		}
	}

	@Transactional
	public AppContracts.PrivacyConsentResponse withdrawAuthorizations(
			String userId,
			AppContracts.PrivacyAuthorizationWithdrawalRequest request
	) {
		Set<String> scopes = request.scopes().stream().map(String::trim).map(String::toUpperCase).collect(java.util.stream.Collectors.toSet());
		if (scopes.isEmpty() || !WITHDRAWABLE_SCOPES.containsAll(scopes)) {
			throw new BusinessException("VALIDATION_ERROR", "仅支持撤回 MEDICAL_DATA、FAMILY_COLLABORATION、NOTIFICATION 授权");
		}
		PrivacyConsentRecordEntity current = consentRepository.findFirstByUserCodeOrderByEffectiveAtDesc(userId)
				.orElseThrow(() -> new BusinessException("PRIVACY_CONSENT_NOT_FOUND", "当前没有可撤回的授权记录"));
		Instant now = Instant.now();
		PrivacyConsentRecordEntity entity = new PrivacyConsentRecordEntity();
		entity.setConsentCode(idGenerator.next("consent"));
		entity.setUserCode(userId);
		entity.setConsentVersion(current.getConsentVersion());
		entity.setPrivacyPolicyVersion(current.getPrivacyPolicyVersion());
		entity.setPrivacyAccepted(current.isPrivacyAccepted());
		entity.setTermsAccepted(current.isTermsAccepted());
		entity.setMedicalDataAuthorized(scopes.contains("MEDICAL_DATA") ? false : current.isMedicalDataAuthorized());
		entity.setFamilyCollaborationAuthorized(scopes.contains("FAMILY_COLLABORATION") ? false : current.isFamilyCollaborationAuthorized());
		entity.setNotificationAuthorized(scopes.contains("NOTIFICATION") ? false : current.isNotificationAuthorized());
		entity.setSourceType("WITHDRAWAL");
		entity.setChangeReason(request.reason().trim());
		entity.setEffectiveAt(now);
		entity.setCreatedAt(now);
		consentRepository.save(entity);
		return toConsentResponse(entity);
	}

	@Transactional
	public AppContracts.AccountDeletionResponse deleteAccount(
			String userId,
			AppContracts.AccountDeletionRequest request
	) {
		if (!"DELETE_MY_ACCOUNT".equals(request.confirmation())) {
			throw new BusinessException("ACCOUNT_DELETE_CONFIRMATION_INVALID", "请输入 DELETE_MY_ACCOUNT 确认永久删除");
		}
		List<String> relativePaths = storedFileRepository.findByUserCodeOrderByUploadedAtDesc(
				userId,
				org.springframework.data.domain.Pageable.unpaged()
		).getContent().stream().map(StoredFileEntity::getRelativePath).toList();
		List<String> sessionTokens = authSessionRepository.findByUserCodeOrderByLastSeenAtDescCreatedAtDesc(userId).stream()
				.map(AuthSessionEntity::getToken)
				.toList();
		int deletedRows = 0;
		deletedRows += jdbcTemplate.update(
				"DELETE FROM family_task WHERE patient_user_code = ? OR caregiver_user_code = ? OR created_by_user_code = ?",
				userId, userId, userId
		);
		deletedRows += jdbcTemplate.update(
				"DELETE FROM family_binding WHERE patient_user_code = ? OR caregiver_user_code = ?",
				userId, userId
		);
		deletedRows += jdbcTemplate.update(
				"DELETE FROM family_invite WHERE creator_user_code = ? OR patient_user_code = ? OR accepted_by_user_code = ?",
				userId, userId, userId
		);
		deletedRows += jdbcTemplate.update(
				"DELETE FROM access_audit WHERE actor_user_code = ? OR patient_user_code = ?",
				userId, userId
		);
		for (String table : USER_TABLES) {
			if (!"access_audit".equals(table) && tableExists(table)) {
				deletedRows += jdbcTemplate.update("DELETE FROM " + table + " WHERE user_code = ?", userId);
			}
		}
		deletedRows += jdbcTemplate.update("DELETE FROM user_account WHERE user_code = ?", userId);
		String receipt = "deletion-" + hash(userId + ":" + Instant.now()).substring(0, 24);
		registerPostCommitCleanup(relativePaths, sessionTokens);
		return new AppContracts.AccountDeletionResponse(
				receipt,
				"COMPLETED",
				deletedRows,
				relativePaths.size(),
				Instant.now(),
				"账号、健康数据、授权记录和关联文件已安排永久删除，所有会话将失效"
		);
	}

	private List<String> exportTables() {
		return List.of(
				"user_profile", "privacy_consent_record", "stored_file", "meal_record", "uric_acid_record",
				"weight_record", "blood_pressure_record", "flare_record", "hydration_record", "lab_report_record",
				"medication_plan", "medication_checkin", "reminder_event", "daily_health_summary",
				"proactive_care_setting", "weather_daily_snapshot", "health_record_audit", "mvp_usage_event",
				"clinical_decision_audit"
		);
	}

	private void registerPostCommitCleanup(List<String> relativePaths, List<String> sessionTokens) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				relativePaths.forEach(fileStorageService::deleteQuietly);
				sessionTokens.forEach(sessionCacheService::evict);
			}
		});
	}

	private byte[] buildExportArchive(String userId, byte[] manifest) {
		try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
			zip.putNextEntry(new ZipEntry("data.json"));
			zip.write(manifest);
			zip.closeEntry();
			for (StoredFileEntity file : storedFileRepository.findByUserCodeOrderByUploadedAtDesc(
					userId,
					org.springframework.data.domain.Pageable.unpaged()
			).getContent()) {
				try (java.io.InputStream input = fileStorageService.loadAsResource(file.getRelativePath()).getInputStream()) {
					String safeName = file.getFileName().replace("\\", "_").replace("/", "_");
					zip.putNextEntry(new ZipEntry("files/" + file.getFileCode() + "_" + safeName));
					input.transferTo(zip);
					zip.closeEntry();
				} catch (java.io.IOException | BusinessException ex) {
					// The manifest still records missing files so the export remains auditable.
				}
			}
			zip.finish();
			return bytes.toByteArray();
		} catch (java.io.IOException ex) {
			throw new BusinessException("DATA_EXPORT_FAILED", "打包个人数据和上传文件失败");
		}
	}

	private AppContracts.PrivacyConsentResponse toConsentResponse(PrivacyConsentRecordEntity entity) {
		return new AppContracts.PrivacyConsentResponse(
				entity.getConsentCode(), entity.getUserCode(), entity.getConsentVersion(), entity.getPrivacyPolicyVersion(),
				entity.isPrivacyAccepted(), entity.isTermsAccepted(), entity.isMedicalDataAuthorized(),
				entity.isFamilyCollaborationAuthorized(), entity.isNotificationAuthorized(), entity.getSourceType(), entity.getChangeReason(),
				entity.getEffectiveAt(), entity.getCreatedAt()
		);
	}

	private String hash(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private boolean tableExists(String tableName) {
		Boolean exists = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection -> {
			String[] candidates = {tableName, tableName.toUpperCase(), tableName.toLowerCase()};
			for (String candidate : candidates) {
				try (java.sql.ResultSet tables = connection.getMetaData().getTables(
						connection.getCatalog(), null, candidate, new String[] {"TABLE"})) {
					if (tables.next()) {
						return true;
					}
				}
			}
			return false;
		});
		return Boolean.TRUE.equals(exists);
	}
}
