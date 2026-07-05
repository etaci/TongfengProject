package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import com.tongfeng.backend.app.persistence.entity.StoredFileEntity;
import com.tongfeng.backend.app.persistence.entity.UserProfileEntity;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.StoredFileRepository;
import com.tongfeng.backend.app.persistence.repo.UserProfileRepository;
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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class LabReportService {

	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};
	private static final TypeReference<List<AppContracts.LabIndicator>> LAB_INDICATOR_LIST_TYPE = new TypeReference<>() {
	};

	private final UserProfileRepository userProfileRepository;
	private final LabReportRecordRepository labReportRecordRepository;
	private final StoredFileRepository storedFileRepository;
	private final LocalFileStorageService localFileStorageService;
	private final AiServiceClient aiServiceClient;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;
	private final MvpMetricsService mvpMetricsService;
	private final HealthRuleEngineService healthRuleEngineService;

	public LabReportService(
			UserProfileRepository userProfileRepository,
			LabReportRecordRepository labReportRecordRepository,
			StoredFileRepository storedFileRepository,
			LocalFileStorageService localFileStorageService,
			AiServiceClient aiServiceClient,
			IdGenerator idGenerator,
			JsonCodec jsonCodec,
			MvpMetricsService mvpMetricsService,
			HealthRuleEngineService healthRuleEngineService
	) {
		this.userProfileRepository = userProfileRepository;
		this.labReportRecordRepository = labReportRecordRepository;
		this.storedFileRepository = storedFileRepository;
		this.localFileStorageService = localFileStorageService;
		this.aiServiceClient = aiServiceClient;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
		this.mvpMetricsService = mvpMetricsService;
		this.healthRuleEngineService = healthRuleEngineService;
	}

	@Transactional
	public AppContracts.LabReportAnalyzeResponse analyzeLabReport(String userId, String reportDate, MultipartFile file) {
		ensureProfile(userId);
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
		entity.setManualConfirmed(false);
		entity.setManualConfirmedAt(null);
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
		ensureProfile(userId);
		return labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId).stream()
				.map(this::toLabReportResponse)
				.toList();
	}

	@Transactional
	public AppContracts.LabReportReviewResponse confirmLabReportManually(
			String userId,
			String reportId,
			AppContracts.LabReportManualConfirmRequest request
	) {
		ensureProfile(userId);
		LabReportRecordEntity report = requireOwnedReport(userId, reportId, "无权修改该化验单");

		List<AppContracts.LabIndicator> existingIndicators = readIndicators(report.getIndicatorsJson());
		if (isLabReviewReady(existingIndicators) && !report.isManualConfirmed()) {
			throw new BusinessException("LAB_REPORT_ALREADY_READY", "当前化验单已完成 OCR 解析，无需人工补录");
		}

		List<AppContracts.LabIndicator> indicators = request.indicators().stream()
				.map(this::toManualLabIndicator)
				.toList();
		report.setIndicatorsJson(jsonCodec.toJson(indicators));
		report.setOverallRiskLevel(resolveLabOverallRiskLevel(indicators).name());
		report.setSuggestionsJson(jsonCodec.toJson(buildManualLabSuggestions(indicators, request.summaryNote())));
		report.setManualConfirmed(true);
		report.setManualConfirmedAt(Instant.now());
		report.setSummaryText(buildManualLabSummary(indicators, request.summaryNote()));
		labReportRecordRepository.save(report);
		refreshInsightState(userId);
		return getLabReportReview(userId, reportId);
	}

	public AppContracts.LabReportReviewResponse getLabReportReview(String userId, String reportId) {
		UserProfileEntity profile = ensureProfile(userId);
		LabReportRecordEntity report = requireOwnedReport(userId, reportId, "无权查看该化验单复盘");

		List<LabReportRecordEntity> reports = labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId);
		LabReportRecordEntity previousReport = reports.stream()
				.filter(item -> !Objects.equals(item.getReportCode(), report.getReportCode()))
				.filter(item -> !item.getReportDate().isAfter(report.getReportDate()))
				.max(Comparator.comparing(LabReportRecordEntity::getReportDate))
				.orElse(null);

		List<AppContracts.LabIndicator> currentIndicators = readIndicators(report.getIndicatorsJson());
		if (!isLabReviewReady(currentIndicators)) {
			List<String> trustNotes = buildLabTrustNotes(previousReport, currentIndicators);
			List<AppContracts.TodayActionItemResponse> manualTasks = buildLabManualConfirmationTasks(report);
			List<String> blockedOutputs = buildLabManualBlockedOutputs();
			List<String> nextActions = List.of(
					"重新上传更清晰的化验单图片或 PDF，优先确保指标名称、数值和单位完整可见。",
					"如果 OCR 仍不稳定，请手动核对关键指标后再继续复盘。",
					"在人工确认前，不要把当前结果当作正式化验结论或趋势依据。"
			);
			String blockedSummary = "当前报告未能稳定识别出可复盘的关键指标，系统已阻断自动目标判断和趋势复盘，请先人工确认。";
			AppContracts.LabReportTrustMetaResponse trustMeta = buildLabReviewTrustMeta(report, currentIndicators, false, blockedOutputs);
			AppContracts.LabDoctorSummaryResponse doctorSummary = buildLabDoctorSummary(
					report,
					blockedSummary,
					List.of(),
					nextActions,
					trustNotes,
					false
			);
			return new AppContracts.LabReportReviewResponse(
					report.getReportCode(),
					report.getReportDate(),
					toRiskLevel(report.getOverallRiskLevel()),
					true,
					false,
					"MANUAL_CONFIRMATION_REQUIRED",
					blockedSummary,
					"LAB_MANUAL_CONFIRMATION_PENDING",
					previousReport == null ? null : previousReport.getReportCode(),
					previousReport == null ? null : previousReport.getReportDate(),
					previousReport == null ? null : (int) ChronoUnit.DAYS.between(previousReport.getReportDate(), report.getReportDate()),
					profile.getTargetUricAcid(),
					null,
					null,
					false,
					"当前不输出目标值结论，需先人工确认指标后再生成正式复盘。",
					List.of(),
					List.of(),
					"请先完成人工确认，再决定是否需要重拍、补录或尽快咨询医生。",
					manualTasks,
					blockedOutputs,
					nextActions,
					trustNotes,
					trustMeta,
					doctorSummary,
					Instant.now()
			);
		}

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
		String reviewSummary = buildLabReviewSummary(report, currentUricAcid, previousUricAcid, targetUricAcidValue, previousReport);
		AppContracts.LabReportTrustMetaResponse trustMeta = buildLabReviewTrustMeta(report, currentIndicators, true, List.of());
		AppContracts.LabDoctorSummaryResponse doctorSummary = buildLabDoctorSummary(
				report,
				reviewSummary,
				keyChanges,
				appendFollowUpRecommendation(nextActions, followUpRecommendation),
				trustNotes,
				true
		);

		return new AppContracts.LabReportReviewResponse(
				report.getReportCode(),
				report.getReportDate(),
				toRiskLevel(report.getOverallRiskLevel()),
				false,
				true,
				"READY",
				reviewSummary,
				"LAB_REVIEW_READY",
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
				List.of(),
				List.of(),
				nextActions,
				trustNotes,
				trustMeta,
				doctorSummary,
				Instant.now()
		);
	}

	private UserProfileEntity ensureProfile(String userId) {
		return userProfileRepository.findByUserCode(userId)
				.orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "用户档案不存在，请先登录"));
	}

	private LabReportRecordEntity requireOwnedReport(String userId, String reportId, String forbiddenMessage) {
		LabReportRecordEntity report = labReportRecordRepository.findByReportCode(reportId)
				.orElseThrow(() -> new BusinessException("LAB_REPORT_NOT_FOUND", "化验单不存在"));
		if (!Objects.equals(report.getUserCode(), userId)) {
			throw new BusinessException("FORBIDDEN", forbiddenMessage);
		}
		return report;
	}

	private void refreshInsightState(String userId) {
		healthRuleEngineService.refreshDailySummary(userId, LocalDate.now());
		healthRuleEngineService.rebuildActiveReminders(userId);
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

	private AppContracts.LabReportAnalyzeResponse toLabReportResponse(LabReportRecordEntity record) {
		List<AppContracts.LabIndicator> indicators = readIndicators(record.getIndicatorsJson());
		boolean reviewReady = isLabReviewReady(indicators);
		return new AppContracts.LabReportAnalyzeResponse(
				record.getReportCode(),
				record.getReportDate(),
				indicators,
				toRiskLevel(record.getOverallRiskLevel()),
				!reviewReady,
				reviewReady,
				record.isManualConfirmed() ? "MANUAL_CONFIRMED" : (reviewReady ? "OCR_EXTRACTED" : "MANUAL_CONFIRMATION_REQUIRED"),
				readStringList(record.getSuggestionsJson()),
				buildLabAnalyzeTrustNotes(record, indicators),
				record.getSummaryText(),
				resolveLabAnalysisMode(record, indicators)
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

	private AppContracts.LabIndicator toManualLabIndicator(AppContracts.LabManualIndicatorRequest request) {
		return new AppContracts.LabIndicator(
				request.code().trim(),
				request.name().trim(),
				request.value(),
				request.unit().trim(),
				StringUtils.hasText(request.referenceRange()) ? request.referenceRange().trim() : null,
				request.riskLevel()
		);
	}

	private AppContracts.RiskLevel resolveLabOverallRiskLevel(List<AppContracts.LabIndicator> indicators) {
		return safeIndicators(indicators).stream()
				.map(AppContracts.LabIndicator::riskLevel)
				.filter(Objects::nonNull)
				.max(Comparator.comparingInt(this::riskRank))
				.orElse(AppContracts.RiskLevel.GREEN);
	}

	private List<String> buildManualLabSuggestions(List<AppContracts.LabIndicator> indicators, String summaryNote) {
		List<String> suggestions = new ArrayList<>();
		if (StringUtils.hasText(summaryNote)) {
			suggestions.add(summaryNote.trim());
		}
		boolean hasRedRisk = safeIndicators(indicators).stream()
				.anyMatch(item -> item.riskLevel() == AppContracts.RiskLevel.RED);
		if (hasRedRisk) {
			suggestions.add("人工确认后尽快带上原始化验单与医生沟通高风险指标。");
		} else {
			suggestions.add("人工确认后继续结合原始化验单和既往结果完成复盘。");
		}
		suggestions.add("保留原始报告图片或 PDF，复诊时可直接出示给医生。");
		return suggestions.stream()
				.filter(StringUtils::hasText)
				.distinct()
				.limit(3)
				.toList();
	}

	private String buildManualLabSummary(List<AppContracts.LabIndicator> indicators, String summaryNote) {
		if (StringUtils.hasText(summaryNote)) {
			return summaryNote.trim();
		}
		AppContracts.LabIndicator uricAcid = findUricAcidIndicator(safeIndicators(indicators)).orElse(null);
		if (uricAcid != null && uricAcid.value() != null) {
			return "已根据原始化验单完成关键指标人工确认，当前尿酸为 "
					+ formatDelta(uricAcid.value()) + " " + safeUnit(uricAcid.unit()) + "。";
		}
		return "已根据原始化验单完成关键指标人工确认，请继续查看正式复盘结论。";
	}

	private List<AppContracts.TodayActionItemResponse> buildLabManualConfirmationTasks(LabReportRecordEntity report) {
		return List.of(
				new AppContracts.TodayActionItemResponse(
						"lab-confirm-clarity",
						"LAB_REPORT",
						"核对原始报告清晰度",
						"确认报告中的指标名称、数值、单位和参考范围完整可见，再继续复盘。",
						"HIGH",
						"DO_NOW"
				),
				new AppContracts.TodayActionItemResponse(
						"lab-confirm-key-values",
						"LAB_REPORT",
						"补录关键指标",
						"优先补录尿酸及本次异常指标，避免错误结果进入正式结论。",
						"HIGH",
						"DO_NOW"
				),
				new AppContracts.TodayActionItemResponse(
						"lab-confirm-source",
						"LAB_REPORT",
						"保留报告来源",
						"保留本次原始图片或 PDF，方便后续复诊与医生核对。",
						"MEDIUM",
						StringUtils.hasText(report.getFileCode()) ? "READY" : "PENDING"
				)
		);
	}

	private List<String> buildLabManualBlockedOutputs() {
		return List.of(
				"个人目标值是否达标",
				"与上一份报告的正式趋势对比",
				"可直接分享给医生的正式复盘结论"
		);
	}

	private AppContracts.LabReportTrustMetaResponse buildLabReviewTrustMeta(
			LabReportRecordEntity report,
			List<AppContracts.LabIndicator> indicators,
			boolean reviewReady,
			List<String> lockedSections
	) {
		StoredFileEntity storedFile = findStoredFile(report);
		String verificationStage;
		if (!reviewReady) {
			verificationStage = "MANUAL_CONFIRMATION_REQUIRED";
		} else if (report.isManualConfirmed()) {
			verificationStage = "MANUAL_CONFIRMED";
		} else {
			verificationStage = "OCR_EXTRACTED";
		}
		return new AppContracts.LabReportTrustMetaResponse(
				"USER_UPLOAD",
				report.isManualConfirmed() ? "用户上传并人工确认" : "用户上传报告",
				storedFile != null,
				storedFile == null ? null : storedFile.getFileName(),
				storedFile == null ? null : storedFile.getUploadedAt(),
				storedFile == null ? "未标记机构来源" : "来源待人工核对",
				false,
				verificationStage,
				safeList(lockedSections),
				report.getManualConfirmedAt(),
				buildLabTrustTimeline(report, storedFile, verificationStage),
				buildLabIndicatorTrustItems(report, indicators, reviewReady)
		);
	}

	private List<AppContracts.LabReportTrustTimelineItemResponse> buildLabTrustTimeline(
			LabReportRecordEntity report,
			StoredFileEntity storedFile,
			String verificationStage
	) {
		List<AppContracts.LabReportTrustTimelineItemResponse> items = new ArrayList<>();
		if (storedFile != null) {
			items.add(new AppContracts.LabReportTrustTimelineItemResponse(
					"UPLOAD",
					"上传原始报告",
					"已保存原始文件，可用于后续人工核对与医生复诊。",
					"DONE",
					storedFile.getUploadedAt()
			));
		}
		items.add(new AppContracts.LabReportTrustTimelineItemResponse(
				verificationStage,
				"生成当前校验阶段",
				report.isManualConfirmed() ? "当前结果来自人工确认后的正式复盘。" : "当前结果仍需结合原始报告继续核对。",
				report.isManualConfirmed() ? "DONE" : "IN_PROGRESS",
				report.isManualConfirmed() && report.getManualConfirmedAt() != null ? report.getManualConfirmedAt()
						: report.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
		));
		if (report.isManualConfirmed() && report.getManualConfirmedAt() != null) {
			items.add(new AppContracts.LabReportTrustTimelineItemResponse(
					"MANUAL_CONFIRMATION",
					"完成人工确认",
					"关键指标已由用户根据原始化验单补录确认。",
					"DONE",
					report.getManualConfirmedAt()
			));
		}
		return items;
	}

	private List<AppContracts.LabIndicatorTrustItemResponse> buildLabIndicatorTrustItems(
			LabReportRecordEntity report,
			List<AppContracts.LabIndicator> indicators,
			boolean reviewReady
	) {
		if (!reviewReady || safeIndicators(indicators).isEmpty()) {
			return List.of();
		}
		return safeIndicators(indicators).stream()
				.map(indicator -> buildLabIndicatorTrustItem(report, indicator))
				.toList();
	}

	private AppContracts.LabIndicatorTrustItemResponse buildLabIndicatorTrustItem(
			LabReportRecordEntity report,
			AppContracts.LabIndicator indicator
	) {
		if (report.isManualConfirmed()) {
			return new AppContracts.LabIndicatorTrustItemResponse(
					indicator.code(),
					indicator.name(),
					"MANUAL_CONFIRMATION",
					"VERIFIED",
					100,
					"人工确认",
					"该字段已根据原始化验单人工补录或复核，不再依赖 OCR 原始提取。"
			);
		}

		int confidenceScore = estimateLabIndicatorConfidence(indicator);
		String confidenceLabel = confidenceScore >= 90 ? "较高"
				: confidenceScore >= 80 ? "中等"
				: "待复核";
		return new AppContracts.LabIndicatorTrustItemResponse(
				indicator.code(),
				indicator.name(),
				"OCR_EXTRACTED",
				confidenceScore >= 80 ? "OCR_READY" : "REVIEW_RECOMMENDED",
				confidenceScore,
				confidenceLabel,
				buildLabIndicatorConfidenceNote(indicator, confidenceScore)
		);
	}

	private int estimateLabIndicatorConfidence(AppContracts.LabIndicator indicator) {
		int score = 62;
		if (StringUtils.hasText(indicator.code())) {
			score += 10;
		}
		if (StringUtils.hasText(indicator.name())) {
			score += 10;
		}
		if (indicator.value() != null) {
			score += 10;
		}
		if (StringUtils.hasText(indicator.unit())) {
			score += 6;
		}
		if (StringUtils.hasText(indicator.referenceRange())) {
			score += 4;
		}
		return Math.min(score, 92);
	}

	private String buildLabIndicatorConfidenceNote(AppContracts.LabIndicator indicator, int confidenceScore) {
		List<String> reasons = new ArrayList<>();
		if (!StringUtils.hasText(indicator.referenceRange())) {
			reasons.add("缺少参考范围");
		}
		if (!StringUtils.hasText(indicator.unit())) {
			reasons.add("缺少单位");
		}
		if (!StringUtils.hasText(indicator.name())) {
			reasons.add("缺少指标名称");
		}
		if (reasons.isEmpty()) {
			return "当前为字段完整性规则分，后续接入 OCR 原始置信度后可进一步替换。 当前分值 " + confidenceScore + "。";
		}
		return "当前为字段完整性规则分，发现 " + String.join("、", reasons) + "，建议优先人工复核。";
	}

	private AppContracts.LabDoctorSummaryResponse buildLabDoctorSummary(
			LabReportRecordEntity report,
			String reviewSummary,
			List<String> keyFindings,
			List<String> careRequests,
			List<String> trustNotes,
			boolean readyToShare
	) {
		List<String> finalTrustNotes = new ArrayList<>(safeList(trustNotes));
		if (report.isManualConfirmed()) {
			finalTrustNotes.add("本摘要包含人工确认结果，建议同时向医生出示原始化验单。");
		} else if (readyToShare) {
			finalTrustNotes.add("本摘要基于 OCR 提取结果生成，复诊时仍建议医生核对原始报告。");
		} else {
			finalTrustNotes.add("当前仍处于人工确认前阶段，不建议把未核验结论当作正式医疗依据。");
		}
		return new AppContracts.LabDoctorSummaryResponse(
				readyToShare,
				"LAB_DOCTOR_SUMMARY",
				reviewSummary,
				safeList(keyFindings),
				safeList(careRequests),
				finalTrustNotes.stream().distinct().limit(4).toList()
		);
	}

	private List<String> appendFollowUpRecommendation(List<String> nextActions, String followUpRecommendation) {
		List<String> actions = new ArrayList<>(safeList(nextActions));
		if (StringUtils.hasText(followUpRecommendation)) {
			actions.add(followUpRecommendation);
		}
		return actions.stream().filter(StringUtils::hasText).distinct().limit(4).toList();
	}

	private StoredFileEntity findStoredFile(LabReportRecordEntity report) {
		if (report == null || !StringUtils.hasText(report.getFileCode())) {
			return null;
		}
		return storedFileRepository.findByFileCode(report.getFileCode()).orElse(null);
	}

	private List<String> buildLabAnalyzeTrustNotes(
			LabReportRecordEntity record,
			List<AppContracts.LabIndicator> indicators
	) {
		if (!isLabReviewReady(indicators)) {
			return List.of(
					"当前报告处于人工确认模式，系统不会用估算值替代真实化验结果。",
					"在人工确认前，不会输出目标值判断、趋势对比或正式风险推演。"
			);
		}
		if (record.isManualConfirmed()) {
			return List.of(
					"当前结果已根据原始化验单完成关键指标人工确认。",
					"复诊时建议同时向医生出示原始报告和本次结构化复盘摘要。"
			);
		}
		return List.of(
				"当前结果基于 OCR 提取到的指标生成，仍需结合原始化验单和医生意见确认。",
				"如果关键指标缺失或图像不清晰，请重新上传后再复盘。"
		);
	}

	private String resolveLabAnalysisMode(LabReportRecordEntity record, List<AppContracts.LabIndicator> indicators) {
		if (record.isManualConfirmed()) {
			return "MANUAL_CONFIRMED";
		}
		if (isLabSafeFallback(record)) {
			return AiServiceClient.LAB_ANALYSIS_MODE_SAFE_FALLBACK;
		}
		return isLabReviewReady(indicators) ? AiServiceClient.LAB_ANALYSIS_MODE_AI_OCR : "MANUAL_CONFIRMATION_REQUIRED";
	}

	private boolean isLabSafeFallback(LabReportRecordEntity record) {
		return readStringList(record.getSuggestionsJson()).stream().anyMatch(item -> item != null && item.contains("AI 子服务当前不可用"));
	}

	private boolean isLabReviewReady(List<AppContracts.LabIndicator> indicators) {
		return indicators != null && !indicators.isEmpty();
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

	private List<AppContracts.LabIndicator> readIndicators(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		return jsonCodec.fromJson(json, LAB_INDICATOR_LIST_TYPE);
	}

	private LocalDate parseDateOrToday(String value) {
		if (!StringUtils.hasText(value)) {
			return LocalDate.now();
		}
		return LocalDate.parse(value);
	}

	private AppContracts.RiskLevel toRiskLevel(String value) {
		return AppContracts.RiskLevel.valueOf(value);
	}
}
