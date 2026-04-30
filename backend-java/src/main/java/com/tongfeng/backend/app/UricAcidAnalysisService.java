package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UserProfileEntity;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UserProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class UricAcidAnalysisService {

	private static final TypeReference<List<AppContracts.MealItem>> MEAL_ITEM_LIST_TYPE = new TypeReference<>() {
	};
	private static final TypeReference<List<AppContracts.LabIndicator>> LAB_INDICATOR_LIST_TYPE = new TypeReference<>() {
	};
	private static final List<String> ALCOHOL_KEYWORDS = List.of(
			"酒", "啤酒", "白酒", "黄酒", "红酒", "葡萄酒", "威士忌", "清酒", "烧酒", "cocktail", "beer", "wine", "whisky"
	);

	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final UserProfileRepository userProfileRepository;
	private final MealRecordRepository mealRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final LabReportRecordRepository labReportRecordRepository;
	private final JsonCodec jsonCodec;

	public UricAcidAnalysisService(
			UricAcidRecordRepository uricAcidRecordRepository,
			UserProfileRepository userProfileRepository,
			MealRecordRepository mealRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			FlareRecordRepository flareRecordRepository,
			LabReportRecordRepository labReportRecordRepository,
			JsonCodec jsonCodec
	) {
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.userProfileRepository = userProfileRepository;
		this.mealRecordRepository = mealRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.labReportRecordRepository = labReportRecordRepository;
		this.jsonCodec = jsonCodec;
	}

	public AppContracts.UricAcidCauseAnalysisResponse analyzeLatest(String userId, int lookbackDays) {
		int finalLookbackDays = Math.max(1, lookbackDays);
		Instant now = Instant.now();
		Instant lookbackStart = now.minus(finalLookbackDays, ChronoUnit.DAYS);
		int targetUricAcid = userProfileRepository.findByUserCode(userId)
				.map(UserProfileEntity::getTargetUricAcid)
				.orElse(360);

		List<UricAcidRecordEntity> uricAcidRecords = uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId);
		Optional<UricAcidRecordEntity> latestOptional = uricAcidRecords.stream().findFirst();
		if (latestOptional.isEmpty()) {
			return buildMissingBaselineResponse(finalLookbackDays, targetUricAcid, now);
		}

		UricAcidRecordEntity latest = latestOptional.get();
		List<AppContracts.RiskFactorResponse> factors = new ArrayList<>();
		Set<String> nextActions = new LinkedHashSet<>();
		AppContracts.RiskLevel overallRisk = uricAcidRisk(latest.getUaValue(), targetUricAcid);

		if (latest.getUaValue() > targetUricAcid) {
			AppContracts.RiskLevel currentRisk = latest.getUaValue() >= 500
					? AppContracts.RiskLevel.RED
					: AppContracts.RiskLevel.YELLOW;
			factors.add(new AppContracts.RiskFactorResponse(
					"CURRENT_URIC_ACID_HIGH",
					"当前尿酸仍高于目标",
					currentRisk,
					"最近一次尿酸已经高于目标范围，说明近期风险控制仍需加强。",
					"最近一次尿酸 " + latest.getUaValue() + " " + latest.getUaUnit() + "，目标值 " + targetUricAcid + " " + latest.getUaUnit()
			));
			nextActions.add("优先回顾最近 3 至 7 天的饮食、补水和作息变化，避免继续叠加高风险因素。");
			overallRisk = maxRisk(overallRisk, currentRisk);
		}

		Optional<UricAcidRecordEntity> previousOptional = uricAcidRecords.stream()
				.skip(1)
				.filter(record -> !record.getMeasuredAt().isBefore(lookbackStart))
				.findFirst();
		if (previousOptional.isPresent()) {
			int delta = latest.getUaValue() - previousOptional.get().getUaValue();
			if (delta >= 60) {
				factors.add(new AppContracts.RiskFactorResponse(
						"URIC_ACID_TREND_UP",
						"尿酸在短期内明显上升",
						AppContracts.RiskLevel.RED,
						"近期尿酸有持续走高迹象，说明单次异常并非孤立事件。",
						"较上一条记录上升 " + delta + " " + latest.getUaUnit()
				));
				nextActions.add("建议尽快安排下一次复查，确认升高趋势是否持续。");
				overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.RED);
			} else if (delta >= 30) {
				factors.add(new AppContracts.RiskFactorResponse(
						"URIC_ACID_TREND_UP",
						"尿酸趋势有上升波动",
						AppContracts.RiskLevel.YELLOW,
						"近期尿酸较上一条记录明显抬高，需要关注连续变化而不只是单次结果。",
						"较上一条记录上升 " + delta + " " + latest.getUaUnit()
				));
				nextActions.add("未来 1 周建议保持更连续的尿酸记录，确认是否存在稳定上升。");
				overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.YELLOW);
			}
		}

		long daysSinceLatest = ChronoUnit.DAYS.between(latest.getMeasuredAt(), now);
		if (daysSinceLatest >= 30) {
			factors.add(new AppContracts.RiskFactorResponse(
					"FOLLOW_UP_OVERDUE",
					"复查节奏偏慢",
					AppContracts.RiskLevel.YELLOW,
					"距离上次尿酸记录时间较久，当前判断可能滞后于真实状态。",
					"最近一次尿酸记录距今 " + daysSinceLatest + " 天"
			));
			nextActions.add("建议补录最近一次化验或尽快安排复查，避免长期只依赖旧数据判断。");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.YELLOW);
		}

		List<MealRecordEntity> recentMeals = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream()
				.filter(record -> !record.getTakenAt().isBefore(lookbackStart))
				.toList();
		long redMealCount = recentMeals.stream()
				.filter(record -> toRiskLevel(record.getRiskLevel()) == AppContracts.RiskLevel.RED)
				.count();
		long yellowOrRedMealCount = recentMeals.stream()
				.filter(record -> toRiskLevel(record.getRiskLevel()) != AppContracts.RiskLevel.GREEN)
				.count();
		if (redMealCount >= 1 || yellowOrRedMealCount >= 2) {
			AppContracts.RiskLevel mealRisk = redMealCount >= 2 ? AppContracts.RiskLevel.RED : AppContracts.RiskLevel.YELLOW;
			String evidence = "近 " + finalLookbackDays + " 天共出现 " + redMealCount + " 次高风险饮食、"
					+ yellowOrRedMealCount + " 次非绿色饮食";
			List<String> mealHighlights = recentMeals.stream()
					.filter(record -> toRiskLevel(record.getRiskLevel()) != AppContracts.RiskLevel.GREEN)
					.flatMap(record -> readMealItems(record.getItemsJson()).stream())
					.map(AppContracts.MealItem::name)
					.filter(StringUtils::hasText)
					.distinct()
					.limit(3)
					.toList();
			if (!mealHighlights.isEmpty()) {
				evidence += "，重点食材包括 " + String.join("、", mealHighlights);
			}
			factors.add(new AppContracts.RiskFactorResponse(
					"DIET_RISK_EXPOSURE",
					"近期饮食暴露偏高",
					mealRisk,
					"最近一段时间内高风险或中高风险饮食较多，是最需要优先复盘的诱因。",
					evidence
			));
			nextActions.add("接下来几餐优先回避海鲜、浓肉汤、动物内脏和重口聚餐场景。");
			overallRisk = maxRisk(overallRisk, mealRisk);
		}

		long alcoholExposureCount = recentMeals.stream()
				.filter(this::containsAlcoholSignal)
				.count();
		if (alcoholExposureCount > 0) {
			AppContracts.RiskLevel alcoholRisk = alcoholExposureCount >= 2 ? AppContracts.RiskLevel.RED : AppContracts.RiskLevel.YELLOW;
			factors.add(new AppContracts.RiskFactorResponse(
					"ALCOHOL_EXPOSURE",
					"近期存在饮酒信号",
					alcoholRisk,
					"饮酒尤其是啤酒、白酒等场景，常与尿酸波动和发作风险上升有关。",
					"近 " + finalLookbackDays + " 天识别到 " + alcoholExposureCount + " 次疑似饮酒相关记录"
			));
			nextActions.add("近期尽量避免饮酒，尤其不要与高嘌呤饮食叠加出现。");
			overallRisk = maxRisk(overallRisk, alcoholRisk);
		}

		long lateNightMealCount = recentMeals.stream()
				.filter(this::isLateNightMeal)
				.count();
		if (lateNightMealCount >= 2) {
			factors.add(new AppContracts.RiskFactorResponse(
					"LATE_NIGHT_EATING",
					"夜间进食偏频繁",
					AppContracts.RiskLevel.YELLOW,
					"夜间进食和不规律作息会降低近期复盘的可控性，容易与饮食风险叠加。",
					"近 " + finalLookbackDays + " 天出现 " + lateNightMealCount + " 次晚间或夜宵进食"
			));
			nextActions.add("尽量把高风险聚餐和夜宵场景拆开，优先恢复规律作息。");
			overallRisk = maxRisk(overallRisk, AppContracts.RiskLevel.YELLOW);
		}

		List<HydrationRecordEntity> recentHydration = hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream()
				.filter(record -> !record.getCheckedAt().isBefore(lookbackStart))
				.toList();
		Optional<HydrationRecordEntity> latestHydrationOptional = recentHydration.stream().findFirst();
		if (latestHydrationOptional.isPresent()) {
			HydrationRecordEntity latestHydration = latestHydrationOptional.get();
			int averageWater = recentHydration.isEmpty()
					? 0
					: (int) recentHydration.stream().mapToInt(HydrationRecordEntity::getWaterIntakeMl).average().orElse(0);
			if (latestHydration.getUrineColorLevel() >= 4 || averageWater < 1500) {
				AppContracts.RiskLevel hydrationRisk = latestHydration.getUrineColorLevel() >= 4 || averageWater < 1200
						? AppContracts.RiskLevel.RED
						: AppContracts.RiskLevel.YELLOW;
				factors.add(new AppContracts.RiskFactorResponse(
						"HYDRATION_INSUFFICIENT",
						"近期补水不足",
						hydrationRisk,
						"补水不足和尿液颜色偏深提示近期代谢环境不够稳定，容易放大尿酸控制压力。",
						"最近饮水均值约 " + averageWater + " ml，最近一次尿液颜色等级 " + latestHydration.getUrineColorLevel()
				));
				nextActions.add("今天优先把补水节奏拉稳，尽量分次补足全天饮水量。");
				overallRisk = maxRisk(overallRisk, hydrationRisk);
			}
		}

		List<FlareRecordEntity> recentFlares = flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).stream()
				.filter(record -> !record.getStartedAt().isBefore(lookbackStart))
				.toList();
		Optional<FlareRecordEntity> latestFlareOptional = recentFlares.stream().findFirst();
		if (latestFlareOptional.isPresent()) {
			FlareRecordEntity latestFlare = latestFlareOptional.get();
			AppContracts.RiskLevel flareRisk = latestFlare.getPainLevel() >= 8
					? AppContracts.RiskLevel.RED
					: AppContracts.RiskLevel.YELLOW;
			factors.add(new AppContracts.RiskFactorResponse(
					"RECENT_FLARE",
					"近期有发作或疼痛升级",
					flareRisk,
					"近期发作说明当前风险并非停留在纸面数据层面，需要更保守地管理诱因。",
					latestFlare.getJointName() + " 疼痛等级 " + latestFlare.getPainLevel()
			));
			nextActions.add("若疼痛持续加重、伴随发热或活动受限，请及时线下就医。");
			overallRisk = maxRisk(overallRisk, flareRisk);
		}

		Optional<LabReportRecordEntity> latestLabOptional = labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId).stream()
				.filter(record -> !record.getReportDate().isBefore(LocalDate.now().minusDays(30)))
				.findFirst();
		if (latestLabOptional.isPresent()) {
			LabReportRecordEntity latestLab = latestLabOptional.get();
			AppContracts.RiskLevel labRisk = toRiskLevel(latestLab.getOverallRiskLevel());
			if (labRisk != AppContracts.RiskLevel.GREEN) {
				String evidence = latestLab.getSummaryText();
				List<String> indicatorNames = readIndicators(latestLab.getIndicatorsJson()).stream()
						.filter(indicator -> indicator.riskLevel() != AppContracts.RiskLevel.GREEN)
						.map(indicator -> StringUtils.hasText(indicator.name()) ? indicator.name() : indicator.code())
						.distinct()
						.limit(3)
						.toList();
				if (!indicatorNames.isEmpty()) {
					evidence = "近 30 天异常指标包括 " + String.join("、", indicatorNames) + "；" + evidence;
				}
				factors.add(new AppContracts.RiskFactorResponse(
						"LAB_SIGNAL",
						"化验单提示代谢风险信号",
						labRisk,
						"化验单中的异常结果说明需要把饮食、补水和复查节奏结合起来看，而不是只盯单一尿酸值。",
						evidence
				));
				nextActions.add("结合最近化验单结果安排复查节奏，必要时按既往医嘱与医生沟通。");
				overallRisk = maxRisk(overallRisk, labRisk);
			}
		}

		if (factors.isEmpty()) {
			nextActions.add("当前没有识别到强诱因，建议继续连续记录饮食、饮水和尿酸变化。");
		}
		nextActions.add("保持连续记录至少 3 至 7 天，系统才能更稳定地定位主要诱因。");

		List<AppContracts.RiskFactorResponse> sortedFactors = factors.stream()
				.sorted(Comparator
						.comparing((AppContracts.RiskFactorResponse factor) -> riskRank(factor.riskLevel()))
						.reversed()
						.thenComparing(AppContracts.RiskFactorResponse::code))
				.toList();

		return new AppContracts.UricAcidCauseAnalysisResponse(
				finalLookbackDays,
				latest.getUaValue(),
				latest.getUaUnit(),
				latest.getMeasuredAt(),
				targetUricAcid,
				overallRisk,
				buildSummary(latest, targetUricAcid, sortedFactors, finalLookbackDays),
				sortedFactors,
				List.copyOf(nextActions),
				now
		);
	}

	private AppContracts.UricAcidCauseAnalysisResponse buildMissingBaselineResponse(
			int lookbackDays,
			int targetUricAcid,
			Instant now
	) {
		List<AppContracts.RiskFactorResponse> factors = List.of(new AppContracts.RiskFactorResponse(
				"MISSING_BASELINE",
				"缺少尿酸基线数据",
				AppContracts.RiskLevel.YELLOW,
				"当前没有可用于归因的尿酸结果，系统还无法判断近期波动是趋势问题还是偶发问题。",
				"请先录入最近一次尿酸检测结果"
		));
		List<String> nextActions = List.of(
				"先补录最近一次尿酸结果，建立可分析的基线。",
				"随后连续记录 3 至 7 天饮食和饮水，系统会更容易识别主要诱因。"
		);
		return new AppContracts.UricAcidCauseAnalysisResponse(
				lookbackDays,
				null,
				"umol/L",
				null,
				targetUricAcid,
				AppContracts.RiskLevel.YELLOW,
				"当前还没有尿酸基线数据，建议先完成录入，再结合近几天饮食和补水记录做原因分析。",
				factors,
				nextActions,
				now
		);
	}

	private String buildSummary(
			UricAcidRecordEntity latest,
			int targetUricAcid,
			List<AppContracts.RiskFactorResponse> factors,
			int lookbackDays
	) {
		if (factors.isEmpty()) {
			return "最近一次尿酸为 " + latest.getUaValue() + " " + latest.getUaUnit()
					+ "，近 " + lookbackDays + " 天暂未识别到强诱因，建议继续补齐饮食和补水记录以提高分析稳定性。";
		}
		String factorTitles = factors.stream()
				.limit(3)
				.map(AppContracts.RiskFactorResponse::title)
				.reduce((left, right) -> left + "、" + right)
				.orElse("近期风险因素");
		return "最近一次尿酸为 " + latest.getUaValue() + " " + latest.getUaUnit()
				+ "，目标值 " + targetUricAcid + " " + latest.getUaUnit()
				+ "。结合近 " + lookbackDays + " 天记录，系统优先怀疑 " + factorTitles + " 与本次波动相关。";
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

	private boolean containsAlcoholSignal(MealRecordEntity record) {
		String recordText = ((record.getSummaryText() == null ? "" : record.getSummaryText()) + " "
				+ (record.getNoteText() == null ? "" : record.getNoteText())).toLowerCase(Locale.ROOT);
		boolean textHit = ALCOHOL_KEYWORDS.stream()
				.map(keyword -> keyword.toLowerCase(Locale.ROOT))
				.anyMatch(recordText::contains);
		if (textHit) {
			return true;
		}
		return readMealItems(record.getItemsJson()).stream()
				.map(AppContracts.MealItem::name)
				.filter(StringUtils::hasText)
				.map(name -> name.toLowerCase(Locale.ROOT))
				.anyMatch(itemName -> ALCOHOL_KEYWORDS.stream()
						.map(keyword -> keyword.toLowerCase(Locale.ROOT))
						.anyMatch(itemName::contains));
	}

	private boolean isLateNightMeal(MealRecordEntity record) {
		int hour = record.getTakenAt().atZone(ZoneId.systemDefault()).getHour();
		return hour >= 21 || hour <= 5;
	}

	private AppContracts.RiskLevel uricAcidRisk(Integer value, int targetUricAcid) {
		if (value == null) {
			return AppContracts.RiskLevel.GREEN;
		}
		if (value >= 500) {
			return AppContracts.RiskLevel.RED;
		}
		if (value > Math.max(targetUricAcid, 420)) {
			return AppContracts.RiskLevel.YELLOW;
		}
		if (value > targetUricAcid) {
			return AppContracts.RiskLevel.YELLOW;
		}
		return AppContracts.RiskLevel.GREEN;
	}

	private AppContracts.RiskLevel maxRisk(AppContracts.RiskLevel left, AppContracts.RiskLevel right) {
		return riskRank(left) >= riskRank(right) ? left : right;
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
