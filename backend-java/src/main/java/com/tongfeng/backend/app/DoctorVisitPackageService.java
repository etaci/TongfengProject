package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.ClinicalDecisionAuditEntity;
import com.tongfeng.backend.app.persistence.entity.DoctorVisitShareEntity;
import com.tongfeng.backend.app.persistence.repo.ClinicalDecisionAuditRepository;
import com.tongfeng.backend.app.persistence.repo.DoctorVisitShareRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorVisitPackageService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private final HealthAssistantService healthAssistantService;
	private final ClinicalDecisionAuditRepository auditRepository;
	private final DoctorVisitShareRepository shareRepository;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;

	public DoctorVisitPackageService(
			HealthAssistantService healthAssistantService,
			ClinicalDecisionAuditRepository auditRepository,
			DoctorVisitShareRepository shareRepository,
			IdGenerator idGenerator,
			JsonCodec jsonCodec
	) {
		this.healthAssistantService = healthAssistantService;
		this.auditRepository = auditRepository;
		this.shareRepository = shareRepository;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
	}

	@Transactional(readOnly = true)
	public AppContracts.DoctorVisitPackageResponse buildPackage(String userId, int lookbackDays) {
		int days = normalizeDays(lookbackDays);
		AppContracts.UserProfileResponse profile = healthAssistantService.getProfile(userId);
		AppContracts.TrendResponse trends = healthAssistantService.getTrends(userId, days);
		List<AppContracts.FlareRecordResponse> flares = healthAssistantService.listFlareRecords(userId).stream()
				.filter(item -> item.startedAt().isAfter(Instant.now().minus(days, ChronoUnit.DAYS)))
				.toList();
		List<AppContracts.LabReportAnalyzeResponse> labs = healthAssistantService.listLabReports(userId).stream()
				.filter(item -> !item.reportDate().isBefore(java.time.LocalDate.now().minusDays(days - 1L)))
				.toList();
		AppContracts.GoutFlareTriageResponse latestTriage = auditRepository
				.findFirstByUserCodeAndDecisionTypeOrderByGeneratedAtDesc(userId, "GOUT_FLARE_TRIAGE")
				.map(this::readTriage)
				.orElse(null);
		List<AppContracts.DoctorVisitLabSummary> labSummaries = labs.stream()
				.map(item -> new AppContracts.DoctorVisitLabSummary(
						item.reportId(),
						item.reportDate(),
						item.overallRiskLevel(),
						item.manualConfirmationRequired() ? "MANUAL_CONFIRMATION_REQUIRED" : "RULE_EVALUATED",
						!item.manualConfirmationRequired(),
						item.analysisMode()
				))
				.toList();
		return new AppContracts.DoctorVisitPackageResponse(
				idGenerator.next("doctor-package"),
				days,
				profile.name(),
				profile.targetUricAcid(),
				trends.uricAcid(),
				flares,
				healthAssistantService.getMedicationWeeklyReport(userId, days),
				labSummaries,
				latestTriage,
				buildDoctorQuestions(latestTriage, labSummaries),
				List.of("用户手工记录", "用户上传化验单", "规则引擎版本化输出"),
				List.of(
						"数据包用于就诊准备，不构成诊断或处方。",
						"化验单只有通过人工确认或可信校验后，才适合用于正式复盘；原始报告应由医生核对。",
						"V25 分诊结果为规则计算，尚未经过医生人工复核。"
				),
				Instant.now()
		);
	}

	@Transactional
	public AppContracts.DoctorVisitShareResponse createShare(String userId, AppContracts.DoctorVisitShareCreateRequest request) {
		int days = normalizeDays(request.lookbackDays() == null ? 30 : request.lookbackDays());
		int hours = request.expiresInHours() == null ? 24 : request.expiresInHours();
		String token = randomToken();
		Instant now = Instant.now();
		DoctorVisitShareEntity entity = new DoctorVisitShareEntity();
		entity.setShareCode(idGenerator.next("doctor-share"));
		entity.setUserCode(userId);
		entity.setTokenHash(hash(token));
		entity.setLookbackDays(days);
		entity.setExpiresAt(now.plus(hours, ChronoUnit.HOURS));
		entity.setRevoked(false);
		entity.setCreatedAt(now);
		shareRepository.save(entity);
		return toShareResponse(entity, token);
	}

	@Transactional(readOnly = true)
	public AppContracts.DoctorVisitPackageResponse readSharedPackage(String token) {
		DoctorVisitShareEntity entity = shareRepository.findByTokenHash(hash(token))
				.orElseThrow(() -> new BusinessException("DOCTOR_SHARE_NOT_FOUND", "就诊包分享链接不存在"));
		if (entity.isRevoked() || entity.getExpiresAt().isBefore(Instant.now())) {
			throw new BusinessException("DOCTOR_SHARE_EXPIRED", "就诊包分享链接已过期或已撤销");
		}
		return buildPackage(entity.getUserCode(), entity.getLookbackDays());
	}

	@Transactional
	public AppContracts.DoctorVisitShareResponse revokeShare(String userId, String shareCode) {
		DoctorVisitShareEntity entity = shareRepository.findByShareCodeAndUserCode(shareCode, userId)
				.orElseThrow(() -> new BusinessException("DOCTOR_SHARE_NOT_FOUND", "就诊包分享链接不存在"));
		entity.setRevoked(true);
		entity.setRevokedAt(Instant.now());
		shareRepository.save(entity);
		return toShareResponse(entity, null);
	}

	public String renderPrintHtml(AppContracts.DoctorVisitPackageResponse data) {
		String rows = data.uricAcidTrend().stream()
				.map(item -> "<tr><td>" + escape(item.date()) + "</td><td>" + item.value() + " " + escape(item.unit()) + "</td></tr>")
				.collect(Collectors.joining());
		String labs = data.labReports().stream()
				.map(item -> "<li>" + escape(item.reportDate().toString()) + " / " + escape(item.riskLevel().name())
						+ " / " + escape(item.verificationStatus()) + " / 来源：" + escape(item.sourceType()) + "</li>")
				.collect(Collectors.joining());
		return "<!doctype html><html lang=\"zh-CN\"><meta charset=\"utf-8\"><title>医生就诊包</title>"
				+ "<style>body{font-family:sans-serif;line-height:1.7;max-width:900px;margin:32px auto;color:#222}h1{font-size:26px}h2{border-bottom:1px solid #ccc;padding-bottom:4px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:6px;text-align:left}.trust{background:#fff8df;padding:12px}</style>"
				+ "<h1>痛风/高尿酸医生就诊包</h1><p>患者：" + escape(data.patientName()) + "；统计周期：近 " + data.lookbackDays() + " 天</p>"
				+ "<h2>尿酸趋势</h2><table><tr><th>日期</th><th>结果</th></tr>" + rows + "</table>"
				+ "<h2>发作记录</h2><ul>" + data.flareRecords().stream().map(item -> "<li>" + escape(item.startedAt().toString()) + " / " + escape(item.joint()) + " / 疼痛 " + item.painLevel() + "</li>").collect(Collectors.joining()) + "</ul>"
				+ "<h2>化验可信状态</h2><ul>" + labs + "</ul>"
				+ "<h2>待问医生的问题</h2><ul>" + data.questionsForDoctor().stream().map(item -> "<li>" + escape(item) + "</li>").collect(Collectors.joining()) + "</ul>"
				+ "<div class=\"trust\"><strong>数据来源与可信边界</strong><ul>" + data.trustNotes().stream().map(item -> "<li>" + escape(item) + "</li>").collect(Collectors.joining()) + "</ul></div>"
				+ "<p>生成时间：" + data.generatedAt() + "</p></html>";
	}

	private List<String> buildDoctorQuestions(AppContracts.GoutFlareTriageResponse triage, List<AppContracts.DoctorVisitLabSummary> labs) {
		List<String> questions = new java.util.ArrayList<>();
		if (triage != null && !"SELF_MANAGEMENT".equals(triage.triageCode())) {
			questions.add("本次发作是否需要进一步检查感染、损伤或调整治疗方案？");
		}
		if (labs.stream().anyMatch(item -> !item.readyForClinicalReview())) {
			questions.add("哪些化验指标需要根据原始报告重新确认？");
		}
		questions.add("下一次尿酸复查和就诊时间应如何安排？");
		return questions;
	}

	private int normalizeDays(int days) {
		if (days != 30 && days != 90) {
			throw new BusinessException("VALIDATION_ERROR", "就诊包统计周期只能是 30 或 90 天");
		}
		return days;
	}

	private AppContracts.GoutFlareTriageResponse readTriage(ClinicalDecisionAuditEntity entity) {
		return jsonCodec.fromJson(entity.getDecisionPayloadJson(), AppContracts.GoutFlareTriageResponse.class);
	}

	private AppContracts.DoctorVisitShareResponse toShareResponse(DoctorVisitShareEntity entity, String token) {
		return new AppContracts.DoctorVisitShareResponse(
				entity.getShareCode(), token, token == null ? null : "/api/public/doctor-visit-shares/" + token,
				entity.getLookbackDays(), entity.getExpiresAt(), entity.isRevoked(), entity.getCreatedAt()
		);
	}

	private String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
