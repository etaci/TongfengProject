package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.DailyHealthSummaryEntity;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.ProactiveCareSettingEntity;
import com.tongfeng.backend.app.persistence.entity.ReminderEventEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.WeatherDailySnapshotEntity;
import com.tongfeng.backend.app.persistence.entity.WeightRecordEntity;
import com.tongfeng.backend.app.persistence.repo.DailyHealthSummaryRepository;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.ProactiveCareSettingRepository;
import com.tongfeng.backend.app.persistence.repo.ReminderEventRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.WeatherDailySnapshotRepository;
import com.tongfeng.backend.app.persistence.repo.WeightRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthRuleEngineService {

	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final String SOURCE_RULE_ENGINE = "RULE_ENGINE";

	private final ReminderEventRepository reminderEventRepository;
	private final DailyHealthSummaryRepository dailyHealthSummaryRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final WeightRecordRepository weightRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final MealRecordRepository mealRecordRepository;
	private final LabReportRecordRepository labReportRecordRepository;
	private final ProactiveCareSettingRepository proactiveCareSettingRepository;
	private final WeatherDailySnapshotRepository weatherDailySnapshotRepository;
	private final IdGenerator idGenerator;

	public HealthRuleEngineService(
			ReminderEventRepository reminderEventRepository,
			DailyHealthSummaryRepository dailyHealthSummaryRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			WeightRecordRepository weightRecordRepository,
			FlareRecordRepository flareRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			MealRecordRepository mealRecordRepository,
			LabReportRecordRepository labReportRecordRepository,
			ProactiveCareSettingRepository proactiveCareSettingRepository,
			WeatherDailySnapshotRepository weatherDailySnapshotRepository,
			IdGenerator idGenerator
	) {
		this.reminderEventRepository = reminderEventRepository;
		this.dailyHealthSummaryRepository = dailyHealthSummaryRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.weightRecordRepository = weightRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.mealRecordRepository = mealRecordRepository;
		this.labReportRecordRepository = labReportRecordRepository;
		this.proactiveCareSettingRepository = proactiveCareSettingRepository;
		this.weatherDailySnapshotRepository = weatherDailySnapshotRepository;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public void rebuildActiveReminders(String userId) {
		Instant now = Instant.now();
		List<AppContracts.ReminderResponse> computed = buildReminderResponses(userId, now);
		reminderEventRepository.deleteByUserCodeAndStatus(userId, STATUS_ACTIVE);
		for (AppContracts.ReminderResponse item : computed) {
			ReminderEventEntity entity = new ReminderEventEntity();
			entity.setReminderCode(item.reminderId());
			entity.setUserCode(userId);
			entity.setType(item.type());
			entity.setTitle(item.title());
			entity.setContent(item.content());
			entity.setRiskLevel(item.riskLevel().name());
			entity.setTriggerAt(item.triggerAt());
			entity.setSourceType(SOURCE_RULE_ENGINE);
			entity.setStatus(STATUS_ACTIVE);
			entity.setDedupKey(item.type() + ":" + item.title());
			entity.setCreatedAt(now);
			entity.setUpdatedAt(now);
			reminderEventRepository.save(entity);
		}
	}

	public List<AppContracts.ReminderResponse> getActiveReminders(String userId) {
		List<ReminderEventEntity> stored = reminderEventRepository.findByUserCodeAndStatusOrderByTriggerAtDesc(userId, STATUS_ACTIVE);
		if (stored.isEmpty()) {
			return buildReminderResponses(userId, Instant.now());
		}
		return stored.stream()
				.map(item -> new AppContracts.ReminderResponse(
						item.getReminderCode(),
						item.getType(),
						item.getTitle(),
						item.getContent(),
						AppContracts.RiskLevel.valueOf(item.getRiskLevel()),
						item.getTriggerAt()
				))
				.toList();
	}

	@Transactional
	public DailyHealthSummaryEntity refreshDailySummary(String userId, LocalDate summaryDate) {
		Instant now = Instant.now();
		DailyHealthSummaryEntity entity = dailyHealthSummaryRepository.findByUserCodeAndSummaryDate(userId, summaryDate)
				.orElseGet(() -> {
					DailyHealthSummaryEntity created = new DailyHealthSummaryEntity();
					created.setSummaryCode(idGenerator.next("summary"));
					created.setUserCode(userId);
					created.setSummaryDate(summaryDate);
					created.setCreatedAt(now);
					return created;
				});

		List<UricAcidRecordEntity> uaRecords = uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId);
		List<WeightRecordEntity> weightRecords = weightRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId);
		List<MealRecordEntity> mealRecords = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId);
		List<HydrationRecordEntity> hydrationRecords = hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId);
		List<FlareRecordEntity> flareRecords = flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId);

		Optional<UricAcidRecordEntity> latestUa = uaRecords.stream()
				.filter(item -> !item.getMeasuredAt().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(summaryDate))
				.findFirst();
		Optional<WeightRecordEntity> latestWeight = weightRecords.stream()
				.filter(item -> !item.getMeasuredAt().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(summaryDate))
				.findFirst();

		int totalWater = hydrationRecords.stream()
				.filter(item -> item.getCheckedAt().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(summaryDate))
				.mapToInt(HydrationRecordEntity::getWaterIntakeMl)
				.sum();
		int highRiskMealCount = (int) mealRecords.stream()
				.filter(item -> item.getTakenAt().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(summaryDate))
				.filter(item -> AppContracts.RiskLevel.valueOf(item.getRiskLevel()) != AppContracts.RiskLevel.GREEN)
				.count();
		int flareCount = (int) flareRecords.stream()
				.filter(item -> item.getStartedAt().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(summaryDate))
				.count();

		AppContracts.RiskLevel riskLevel = AppContracts.RiskLevel.GREEN;
		if (latestUa.isPresent()) {
			riskLevel = maxRisk(riskLevel, uricAcidRisk(latestUa.get().getUaValue()));
		}
		if (highRiskMealCount >= 2) {
			riskLevel = maxRisk(riskLevel, AppContracts.RiskLevel.RED);
		} else if (highRiskMealCount == 1) {
			riskLevel = maxRisk(riskLevel, AppContracts.RiskLevel.YELLOW);
		}
		if (totalWater > 0 && totalWater < 1800) {
			riskLevel = maxRisk(riskLevel, AppContracts.RiskLevel.YELLOW);
		}
		if (flareCount > 0) {
			riskLevel = maxRisk(riskLevel, AppContracts.RiskLevel.RED);
		}

		entity.setLatestUricAcidValue(latestUa.map(UricAcidRecordEntity::getUaValue).orElse(null));
		entity.setLatestUricAcidUnit(latestUa.map(UricAcidRecordEntity::getUaUnit).orElse(null));
		entity.setLatestWeightValue(latestWeight.map(WeightRecordEntity::getWeightValue).orElse(null));
		entity.setTotalWaterIntakeMl(totalWater);
		entity.setHighRiskMealCount(highRiskMealCount);
		entity.setFlareCount(flareCount);
		entity.setOverallRiskLevel(riskLevel.name());
		entity.setSummaryText(buildDailySummaryText(summaryDate, latestUa.orElse(null), totalWater, highRiskMealCount, flareCount, riskLevel));
		entity.setUpdatedAt(now);
		return dailyHealthSummaryRepository.save(entity);
	}

	public List<AppContracts.DailyHealthSummaryResponse> getRecentSummaries(String userId, int days) {
		LocalDate cutoff = LocalDate.now().minusDays(Math.max(days - 1L, 0L));
		return dailyHealthSummaryRepository.findByUserCodeOrderBySummaryDateDesc(userId).stream()
				.filter(item -> !item.getSummaryDate().isBefore(cutoff))
				.map(item -> new AppContracts.DailyHealthSummaryResponse(
						item.getSummaryDate().toString(),
						item.getLatestUricAcidValue(),
						item.getLatestUricAcidUnit(),
						item.getLatestWeightValue(),
						item.getTotalWaterIntakeMl(),
						item.getHighRiskMealCount(),
						item.getFlareCount(),
						AppContracts.RiskLevel.valueOf(item.getOverallRiskLevel()),
						item.getSummaryText()
				))
				.toList();
	}

	private List<AppContracts.ReminderResponse> buildReminderResponses(String userId, Instant now) {
		List<AppContracts.ReminderResponse> reminders = new ArrayList<>();
		List<UricAcidRecordEntity> uaRecords = uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId);
		List<MealRecordEntity> mealRecords = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId);
		List<HydrationRecordEntity> hydrationRecords = hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId);
		List<FlareRecordEntity> flareRecords = flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId);
		List<LabReportRecordEntity> labRecords = labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId);

		Optional<UricAcidRecordEntity> latestUa = uaRecords.stream().findFirst();
		if (latestUa.isEmpty()) {
			reminders.add(reminder("FOLLOW_UP", "补录最近一次尿酸", "当前还没有尿酸基线数据，建议尽快录入最近一次检测值，方便后续趋势判断。", AppContracts.RiskLevel.YELLOW, now));
		} else {
			AppContracts.RiskLevel uaRisk = uricAcidRisk(latestUa.get().getUaValue());
			if (uaRisk != AppContracts.RiskLevel.GREEN) {
				reminders.add(reminder(
						"FOLLOW_UP",
						"尿酸值需要关注",
						"最近一次尿酸为 " + latestUa.get().getUaValue() + " " + latestUa.get().getUaUnit() + "，建议结合饮食与复查节奏持续观察。",
						uaRisk,
						latestUa.get().getMeasuredAt()
				));
			}

			long daysSinceUa = ChronoUnit.DAYS.between(latestUa.get().getMeasuredAt(), now);
			if (daysSinceUa >= 30) {
				reminders.add(reminder("FOLLOW_UP", "建议安排复查", "距离上次尿酸记录已超过 30 天，建议尽快补做复查并录入新结果。", AppContracts.RiskLevel.YELLOW, now));
			}

			if (uaRecords.size() >= 2) {
				UricAcidRecordEntity earliest = uaRecords.get(Math.min(uaRecords.size() - 1, 2));
				int delta = latestUa.get().getUaValue() - earliest.getUaValue();
				if (delta >= 60) {
					reminders.add(reminder("TREND", "尿酸趋势上升", "最近几次尿酸记录呈上升趋势，建议尽快复盘近期饮食和饮水情况。", AppContracts.RiskLevel.RED, now));
				} else if (delta >= 30) {
					reminders.add(reminder("TREND", "尿酸趋势波动偏大", "最近几次尿酸记录有上升趋势，建议加强连续记录并关注诱因。", AppContracts.RiskLevel.YELLOW, now));
				}
			}
		}

		Optional<HydrationRecordEntity> latestHydration = hydrationRecords.stream().findFirst();
		if (latestHydration.isPresent()) {
			HydrationRecordEntity hydration = latestHydration.get();
			if (hydration.getUrineColorLevel() >= 4 || hydration.getWaterIntakeMl() < 1200) {
				reminders.add(reminder("HYDRATION", "今天的补水还不够", "最近一次记录显示补水不足或尿液颜色偏深，建议继续补水并观察颜色变化。", AppContracts.RiskLevel.RED, hydration.getCheckedAt()));
			} else if (hydration.getWaterIntakeMl() < 1800) {
				reminders.add(reminder("HYDRATION", "补水量偏少", "当前饮水量仍偏低，建议继续补水，尽量让全天总量更稳定。", AppContracts.RiskLevel.YELLOW, hydration.getCheckedAt()));
			}
		}

		long redMealsIn3Days = mealRecords.stream()
				.filter(item -> !item.getTakenAt().isBefore(now.minus(3, ChronoUnit.DAYS)))
				.filter(item -> AppContracts.RiskLevel.valueOf(item.getRiskLevel()) == AppContracts.RiskLevel.RED)
				.count();
		if (redMealsIn3Days >= 2) {
			reminders.add(reminder("DIET", "高风险饮食连续出现", "最近 3 天内出现多次高风险饮食，建议优先回避海鲜、酒精、浓肉汤等典型诱因。", AppContracts.RiskLevel.RED, now));
		} else {
			Optional<MealRecordEntity> latestMeal = mealRecords.stream().findFirst();
			if (latestMeal.isPresent() && AppContracts.RiskLevel.valueOf(latestMeal.get().getRiskLevel()) == AppContracts.RiskLevel.RED) {
				reminders.add(reminder("DIET", "最近一餐风险偏高", latestMeal.get().getSummaryText(), AppContracts.RiskLevel.RED, latestMeal.get().getTakenAt()));
			}
		}

		Optional<FlareRecordEntity> latestFlare = flareRecords.stream().findFirst();
		if (latestFlare.isPresent() && latestFlare.get().getPainLevel() >= 8 && !latestFlare.get().getStartedAt().isBefore(now.minus(7, ChronoUnit.DAYS))) {
			reminders.add(reminder("ALERT", "疼痛等级较高", "最近一次发作疼痛等级较高，如出现持续加重、发热或行动受限，请及时线下就医。", AppContracts.RiskLevel.RED, latestFlare.get().getStartedAt()));
		}

		Optional<LabReportRecordEntity> latestLab = labRecords.stream().findFirst();
		if (latestLab.isPresent() && AppContracts.RiskLevel.valueOf(latestLab.get().getOverallRiskLevel()) == AppContracts.RiskLevel.RED) {
			reminders.add(reminder("LAB", "化验单结果需要跟进", latestLab.get().getSummaryText(), AppContracts.RiskLevel.RED, latestLab.get().getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
		}

		Optional<ProactiveCareSettingEntity> careSetting = proactiveCareSettingRepository.findByUserCode(userId);
		Optional<WeatherDailySnapshotEntity> weatherSnapshot = weatherDailySnapshotRepository.findByUserCodeAndSummaryDate(userId, LocalDate.now());
		if (careSetting.isPresent() && careSetting.get().isWeatherAlertsEnabled() && weatherSnapshot.isPresent()) {
			AppContracts.RiskLevel weatherRisk = AppContracts.RiskLevel.valueOf(weatherSnapshot.get().getRiskLevel());
			if (weatherRisk != AppContracts.RiskLevel.GREEN) {
				reminders.add(reminder(
						"WEATHER",
						"天气联合风险偏高",
						weatherSnapshot.get().getSummaryText(),
						weatherRisk,
						weatherSnapshot.get().getUpdatedAt()
				));
			}
		}

		if (reminders.isEmpty()) {
			reminders.add(reminder("CARE", "今日状态平稳", "当前没有命中高优先级风险规则，建议继续保持连续记录。", AppContracts.RiskLevel.GREEN, now));
		}

		return reminders.stream()
				.sorted(Comparator
						.comparing((AppContracts.ReminderResponse item) -> riskRank(item.riskLevel()))
						.reversed()
						.thenComparing(AppContracts.ReminderResponse::triggerAt, Comparator.reverseOrder()))
				.toList();
	}

	private AppContracts.ReminderResponse reminder(
			String type,
			String title,
			String content,
			AppContracts.RiskLevel riskLevel,
			Instant triggerAt
	) {
		return new AppContracts.ReminderResponse(
				idGenerator.next("reminder"),
				type,
				title,
				content,
				riskLevel,
				triggerAt
		);
	}

	private String buildDailySummaryText(
			LocalDate summaryDate,
			UricAcidRecordEntity latestUa,
			int totalWater,
			int highRiskMealCount,
			int flareCount,
			AppContracts.RiskLevel riskLevel
	) {
		String uaText = latestUa == null ? "暂无尿酸数据" : ("尿酸 " + latestUa.getUaValue() + " " + latestUa.getUaUnit());
		return summaryDate + " 汇总：" + uaText + "，高风险饮食 " + highRiskMealCount + " 次，饮水 " + totalWater + "ml，发作 " + flareCount + " 次，整体风险 " + riskLevel + "。";
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

	private AppContracts.RiskLevel maxRisk(AppContracts.RiskLevel left, AppContracts.RiskLevel right) {
		return riskRank(left) >= riskRank(right) ? left : right;
	}

	private int riskRank(AppContracts.RiskLevel riskLevel) {
		return switch (riskLevel) {
			case RED -> 3;
			case YELLOW -> 2;
			case GREEN -> 1;
		};
	}
}
