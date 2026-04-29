package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.ProactiveCareSettingEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.WeatherDailySnapshotEntity;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.ProactiveCareSettingRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.WeatherDailySnapshotRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProactiveCareService {

	private final ProactiveCareSettingRepository proactiveCareSettingRepository;
	private final WeatherDailySnapshotRepository weatherDailySnapshotRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final MealRecordRepository mealRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final LabReportRecordRepository labReportRecordRepository;
	private final WeatherGatewayService weatherGatewayService;
	private final IdGenerator idGenerator;

	public ProactiveCareService(
			ProactiveCareSettingRepository proactiveCareSettingRepository,
			WeatherDailySnapshotRepository weatherDailySnapshotRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			MealRecordRepository mealRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			FlareRecordRepository flareRecordRepository,
			LabReportRecordRepository labReportRecordRepository,
			WeatherGatewayService weatherGatewayService,
			IdGenerator idGenerator
	) {
		this.proactiveCareSettingRepository = proactiveCareSettingRepository;
		this.weatherDailySnapshotRepository = weatherDailySnapshotRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.mealRecordRepository = mealRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.labReportRecordRepository = labReportRecordRepository;
		this.weatherGatewayService = weatherGatewayService;
		this.idGenerator = idGenerator;
	}

	public AppContracts.ProactiveCareSettingsResponse getSettings(String userId) {
		Optional<WeatherDailySnapshotEntity> latestSnapshot = weatherDailySnapshotRepository.findFirstByUserCodeOrderBySummaryDateDesc(userId);
		return proactiveCareSettingRepository.findByUserCode(userId)
				.map(item -> toSettingsResponse(item, latestSnapshot.orElse(null)))
				.orElseGet(() -> new AppContracts.ProactiveCareSettingsResponse(
						null,
						null,
						null,
						null,
						null,
						false,
						null,
						latestSnapshot.map(WeatherDailySnapshotEntity::getUpdatedAt).orElse(null)
				));
	}

	@Transactional
	public AppContracts.ProactiveCareSettingsResponse updateSettings(String userId, AppContracts.ProactiveCareSettingsRequest request) {
		Instant now = Instant.now();
		ProactiveCareSettingEntity entity = proactiveCareSettingRepository.findByUserCode(userId)
				.orElseGet(() -> {
					ProactiveCareSettingEntity created = new ProactiveCareSettingEntity();
					created.setUserCode(userId);
					created.setCreatedAt(now);
					return created;
				});
		entity.setMonitoringCity(request.monitoringCity().trim());
		entity.setCountryCode(normalizeCountryCode(request.countryCode()));
		entity.setWeatherAlertsEnabled(request.weatherAlertsEnabled() == null || request.weatherAlertsEnabled());

		WeatherGatewayService.ResolvedLocation location = weatherGatewayService.resolveLocation(entity.getMonitoringCity(), entity.getCountryCode());
		if (location != null) {
			entity.setResolvedName(location.resolvedName());
			entity.setLatitude(location.latitude());
			entity.setLongitude(location.longitude());
			entity.setTimezoneId(location.timezoneId());
		} else {
			entity.setResolvedName(entity.getMonitoringCity());
			entity.setLatitude(null);
			entity.setLongitude(null);
			entity.setTimezoneId("Asia/Shanghai");
		}
		entity.setUpdatedAt(now);
		proactiveCareSettingRepository.save(entity);
		WeatherDailySnapshotEntity snapshot = refreshTodayWeather(entity);
		return toSettingsResponse(entity, snapshot);
	}

	public Optional<WeatherDailySnapshotEntity> getTodayWeatherSnapshotIfPresent(String userId) {
		return weatherDailySnapshotRepository.findByUserCodeAndSummaryDate(userId, LocalDate.now());
	}

	@Transactional
	public Optional<WeatherDailySnapshotEntity> refreshTodayWeatherIfConfigured(String userId) {
		return proactiveCareSettingRepository.findByUserCode(userId)
				.filter(ProactiveCareSettingEntity::isWeatherAlertsEnabled)
				.filter(item -> StringUtils.hasText(item.getMonitoringCity()))
				.map(this::refreshTodayWeather);
	}

	public AppContracts.ProactiveCareBriefResponse getProactiveCareBrief(String userId) {
		Optional<WeatherDailySnapshotEntity> weatherSnapshot = getOrRefreshTodayWeather(userId);
		List<AppContracts.RiskFactorResponse> factors = new ArrayList<>();

		weatherSnapshot.map(this::buildWeatherFactor).ifPresent(factors::add);
		latestUricAcid(userId).map(this::buildUricAcidFactor).ifPresent(factors::add);
		latestHydration(userId).map(this::buildHydrationFactor).ifPresent(factors::add);
		buildMealFactor(userId).ifPresent(factors::add);
		buildRecentFlareFactor(userId).ifPresent(factors::add);
		buildLabFactor(userId).ifPresent(factors::add);

		List<AppContracts.RiskFactorResponse> sortedFactors = factors.stream()
				.sorted(Comparator.comparingInt((AppContracts.RiskFactorResponse item) -> riskRank(item.riskLevel())).reversed())
				.toList();

		int riskScore = sortedFactors.stream().mapToInt(item -> switch (item.riskLevel()) {
			case RED -> 30;
			case YELLOW -> 15;
			case GREEN -> 5;
		}).sum();
		AppContracts.RiskLevel overallRisk = computeOverallRisk(sortedFactors, riskScore);
		List<String> suggestions = buildProactiveSuggestions(sortedFactors);
		if (suggestions.isEmpty()) {
			suggestions = List.of("当前没有明显的主动干预信号，建议继续保持记录节奏。");
		}
		String summary = buildProactiveSummary(overallRisk, sortedFactors, weatherSnapshot.orElse(null));
		return new AppContracts.ProactiveCareBriefResponse(
				overallRisk,
				Math.min(riskScore, 100),
				summary,
				weatherSnapshot.map(this::toWeatherSnapshotResponse).orElse(null),
				sortedFactors,
				suggestions,
				Instant.now()
		);
	}

	public AppContracts.FlareReviewReportResponse getLatestFlareReviewReport(String userId, int lookbackDays) {
		FlareRecordEntity flare = flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).stream()
				.findFirst()
				.orElseThrow(() -> new BusinessException("FLARE_NOT_FOUND", "暂无发作记录，暂时无法生成复盘报告"));
		Instant begin = flare.getStartedAt().minus(Math.max(lookbackDays, 1), ChronoUnit.DAYS);
		List<String> suspectedTriggers = new ArrayList<>();
		List<AppContracts.TimelineEvent> relatedEvents = new ArrayList<>();
		List<String> actionSuggestions = new ArrayList<>();

		List<MealRecordEntity> meals = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream()
				.filter(item -> !item.getTakenAt().isBefore(begin) && item.getTakenAt().isBefore(flare.getStartedAt()))
				.limit(5)
				.toList();
		long redMeals = meals.stream().filter(item -> toRiskLevel(item.getRiskLevel()) == AppContracts.RiskLevel.RED).count();
		if (redMeals > 0) {
			suspectedTriggers.add("发作前 " + lookbackDays + " 天内存在 " + redMeals + " 次高风险饮食暴露");
			actionSuggestions.add("下次如遇聚餐或外食，建议提前规避海鲜、酒精、浓汤和动物内脏。");
		}
		meals.forEach(item -> relatedEvents.add(new AppContracts.TimelineEvent(
				item.getRecordCode(),
				"MEAL",
				"饮食记录",
				item.getSummaryText(),
				item.getTakenAt(),
				toRiskLevel(item.getRiskLevel())
		)));

		List<HydrationRecordEntity> hydrations = hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream()
				.filter(item -> !item.getCheckedAt().isBefore(begin) && item.getCheckedAt().isBefore(flare.getStartedAt()))
				.limit(5)
				.toList();
		boolean lowHydration = hydrations.stream().anyMatch(item -> item.getWaterIntakeMl() < 1800 || item.getUrineColorLevel() >= 4);
		if (lowHydration) {
			suspectedTriggers.add("发作前补水偏少或尿液颜色偏深，可能影响尿酸排泄");
			actionSuggestions.add("建议把全天补水拆分到多个时段完成，并继续记录尿液颜色变化。");
		}
		hydrations.forEach(item -> relatedEvents.add(new AppContracts.TimelineEvent(
				item.getRecordCode(),
				"HYDRATION",
				"饮水/尿液打卡",
				item.getWaterIntakeMl() + "ml / 尿液颜色 " + item.getUrineColorLevel() + " 级",
				item.getCheckedAt(),
				item.getUrineColorLevel() >= 4 ? AppContracts.RiskLevel.YELLOW : AppContracts.RiskLevel.GREEN
		)));

		latestUricAcidBefore(userId, flare.getStartedAt()).ifPresent(item -> {
			if (item.getUaValue() > 420) {
				suspectedTriggers.add("发作前最近一次尿酸为 " + item.getUaValue() + " " + item.getUaUnit() + "，处于偏高区间");
				actionSuggestions.add("建议补录复查计划，并结合用药与饮食记录观察尿酸回落节奏。");
			}
			relatedEvents.add(new AppContracts.TimelineEvent(
					item.getRecordCode(),
					"URIC_ACID",
					"尿酸记录",
					item.getUaValue() + " " + item.getUaUnit(),
					item.getMeasuredAt(),
					uricAcidRisk(item.getUaValue())
			));
		});

		labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId).stream()
				.filter(item -> {
					Instant occurredAt = item.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
					return !occurredAt.isBefore(begin) && occurredAt.isBefore(flare.getStartedAt());
				})
				.findFirst()
				.ifPresent(item -> {
					if (toRiskLevel(item.getOverallRiskLevel()) == AppContracts.RiskLevel.RED) {
						suspectedTriggers.add("近期化验单曾出现高风险结果，建议结合发作前后的实验室指标复查");
					}
					relatedEvents.add(new AppContracts.TimelineEvent(
							item.getReportCode(),
							"LAB_REPORT",
							"化验单解析",
							item.getSummaryText(),
							item.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant(),
							toRiskLevel(item.getOverallRiskLevel())
					));
				});

		weatherDailySnapshotRepository.findByUserCodeAndSummaryDate(
				userId,
				flare.getStartedAt().atZone(ZoneId.systemDefault()).toLocalDate()
		).ifPresent(item -> {
			if (toRiskLevel(item.getRiskLevel()) != AppContracts.RiskLevel.GREEN) {
				suspectedTriggers.add("发作当天监测城市天气偏冷、偏湿或有明显降水，可能加重不适感");
			}
			relatedEvents.add(new AppContracts.TimelineEvent(
					item.getSnapshotCode(),
					"WEATHER",
					"天气快照",
					item.getSummaryText(),
					item.getUpdatedAt(),
					toRiskLevel(item.getRiskLevel())
			));
		});

		if (suspectedTriggers.isEmpty()) {
			suspectedTriggers.add("当前没有识别到强关联诱因，建议继续补全饮食、补水和化验数据。");
		}
		if (actionSuggestions.isEmpty()) {
			actionSuggestions.add("建议继续记录发作前 72 小时的饮食、补水、疼痛变化，方便后续形成更稳定的个人诱因模型。");
		}

		relatedEvents.sort(Comparator.comparing(AppContracts.TimelineEvent::occurredAt).reversed());
		AppContracts.RiskLevel overallRisk = flare.getPainLevel() >= 8 || suspectedTriggers.size() >= 3
				? AppContracts.RiskLevel.RED
				: AppContracts.RiskLevel.YELLOW;
		String summary = flare.getJointName() + " 于 " + flare.getStartedAt() + " 出现发作，系统结合发作前 "
				+ lookbackDays + " 天记录，识别出 " + suspectedTriggers.size() + " 条需要关注的触发线索。";
		return new AppContracts.FlareReviewReportResponse(
				idGenerator.next("flare-report"),
				flare.getRecordCode(),
				flare.getStartedAt(),
				flare.getJointName(),
				flare.getPainLevel(),
				overallRisk,
				suspectedTriggers,
				relatedEvents,
				actionSuggestions,
				summary,
				Instant.now()
		);
	}

	private Optional<WeatherDailySnapshotEntity> getOrRefreshTodayWeather(String userId) {
		Optional<WeatherDailySnapshotEntity> existing = weatherDailySnapshotRepository.findByUserCodeAndSummaryDate(userId, LocalDate.now());
		if (existing.isPresent()) {
			return existing;
		}
		return refreshTodayWeatherIfConfigured(userId);
	}

	@Transactional
	WeatherDailySnapshotEntity refreshTodayWeather(ProactiveCareSettingEntity setting) {
		Instant now = Instant.now();
		LocalDate today = LocalDate.now();
		WeatherDailySnapshotEntity entity = weatherDailySnapshotRepository.findByUserCodeAndSummaryDate(setting.getUserCode(), today)
				.orElseGet(() -> {
					WeatherDailySnapshotEntity created = new WeatherDailySnapshotEntity();
					created.setSnapshotCode(idGenerator.next("weather"));
					created.setUserCode(setting.getUserCode());
					created.setSummaryDate(today);
					created.setCreatedAt(now);
					return created;
				});
		WeatherGatewayService.WeatherObservation observation = weatherGatewayService.queryTodayWeather(
				setting.getMonitoringCity(),
				setting.getCountryCode(),
				today
		);
		AppContracts.RiskLevel weatherRisk = weatherRisk(observation);
		entity.setCityName(observation.cityName());
		entity.setCountryCode(observation.countryCode());
		entity.setLatitude(observation.latitude());
		entity.setLongitude(observation.longitude());
		entity.setTimezoneId(observation.timezoneId());
		entity.setTemperatureC(observation.temperatureC());
		entity.setApparentTemperatureC(observation.apparentTemperatureC());
		entity.setRelativeHumidity(observation.relativeHumidity());
		entity.setPrecipitationProbability(observation.precipitationProbability());
		entity.setWeatherCode(observation.weatherCode());
		entity.setRiskLevel(weatherRisk.name());
		entity.setSourceType(observation.sourceType());
		entity.setWeatherText(observation.weatherText());
		entity.setSummaryText(buildWeatherSummary(observation, weatherRisk));
		entity.setUpdatedAt(now);
		return weatherDailySnapshotRepository.save(entity);
	}

	private AppContracts.ProactiveCareSettingsResponse toSettingsResponse(
			ProactiveCareSettingEntity entity,
			WeatherDailySnapshotEntity latestSnapshot
	) {
		return new AppContracts.ProactiveCareSettingsResponse(
				entity.getMonitoringCity(),
				entity.getCountryCode(),
				entity.getLatitude(),
				entity.getLongitude(),
				entity.getTimezoneId(),
				entity.isWeatherAlertsEnabled(),
				entity.getUpdatedAt(),
				latestSnapshot == null ? null : latestSnapshot.getUpdatedAt()
		);
	}

	private AppContracts.WeatherSnapshotResponse toWeatherSnapshotResponse(WeatherDailySnapshotEntity snapshot) {
		return new AppContracts.WeatherSnapshotResponse(
				snapshot.getCityName(),
				snapshot.getCountryCode(),
				snapshot.getSummaryDate().toString(),
				snapshot.getTemperatureC(),
				snapshot.getApparentTemperatureC(),
				snapshot.getRelativeHumidity(),
				snapshot.getPrecipitationProbability(),
				snapshot.getWeatherCode(),
				toRiskLevel(snapshot.getRiskLevel()),
				snapshot.getSourceType(),
				snapshot.getSummaryText()
		);
	}

	private AppContracts.RiskFactorResponse buildWeatherFactor(WeatherDailySnapshotEntity snapshot) {
		return new AppContracts.RiskFactorResponse(
				"WEATHER",
				"天气联合风险",
				toRiskLevel(snapshot.getRiskLevel()),
				snapshot.getSummaryText(),
				snapshot.getSourceType() + " / " + snapshot.getWeatherText()
		);
	}

	private AppContracts.RiskFactorResponse buildUricAcidFactor(UricAcidRecordEntity record) {
		AppContracts.RiskLevel risk = uricAcidRisk(record.getUaValue());
		return new AppContracts.RiskFactorResponse(
				"URIC_ACID",
				"尿酸水平",
				risk,
				"最近一次尿酸为 " + record.getUaValue() + " " + record.getUaUnit() + "。",
				record.getMeasuredAt().toString()
		);
	}

	private AppContracts.RiskFactorResponse buildHydrationFactor(HydrationRecordEntity record) {
		AppContracts.RiskLevel risk = record.getUrineColorLevel() >= 4 || record.getWaterIntakeMl() < 1200
				? AppContracts.RiskLevel.RED
				: (record.getWaterIntakeMl() < 1800 ? AppContracts.RiskLevel.YELLOW : AppContracts.RiskLevel.GREEN);
		return new AppContracts.RiskFactorResponse(
				"HYDRATION",
				"补水状态",
				risk,
				"最近一次补水记录为 " + record.getWaterIntakeMl() + "ml，尿液颜色 " + record.getUrineColorLevel() + " 级。",
				record.getCheckedAt().toString()
		);
	}

	private Optional<AppContracts.RiskFactorResponse> buildMealFactor(String userId) {
		Instant since = Instant.now().minus(3, ChronoUnit.DAYS);
		List<MealRecordEntity> meals = mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream()
				.filter(item -> !item.getTakenAt().isBefore(since))
				.toList();
		if (meals.isEmpty()) {
			return Optional.empty();
		}
		long redMeals = meals.stream().filter(item -> toRiskLevel(item.getRiskLevel()) == AppContracts.RiskLevel.RED).count();
		if (redMeals >= 2) {
			return Optional.of(new AppContracts.RiskFactorResponse(
					"MEAL",
					"近期饮食暴露",
					AppContracts.RiskLevel.RED,
					"最近 3 天出现 " + redMeals + " 次高风险饮食记录。",
					"3-day-window"
			));
		}
		if (redMeals == 1) {
			return Optional.of(new AppContracts.RiskFactorResponse(
					"MEAL",
					"近期饮食暴露",
					AppContracts.RiskLevel.YELLOW,
					"最近 3 天出现 1 次高风险饮食，建议后续 24 小时保持清淡。",
					"3-day-window"
			));
		}
		return Optional.of(new AppContracts.RiskFactorResponse(
				"MEAL",
				"近期饮食暴露",
				AppContracts.RiskLevel.GREEN,
				"最近 3 天未出现明显高风险饮食暴露。",
				"3-day-window"
		));
	}

	private Optional<AppContracts.RiskFactorResponse> buildRecentFlareFactor(String userId) {
		return flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).stream()
				.findFirst()
				.map(item -> {
					boolean in14Days = !item.getStartedAt().isBefore(Instant.now().minus(14, ChronoUnit.DAYS));
					AppContracts.RiskLevel risk = in14Days
							? (item.getPainLevel() >= 8 ? AppContracts.RiskLevel.RED : AppContracts.RiskLevel.YELLOW)
							: AppContracts.RiskLevel.GREEN;
					return new AppContracts.RiskFactorResponse(
							"FLARE",
							"近期发作史",
							risk,
							in14Days
									? "近 14 天内有发作记录，部位 " + item.getJointName() + "，疼痛等级 " + item.getPainLevel() + "。"
									: "最近一次发作已超过 14 天。",
							item.getStartedAt().toString()
					);
				});
	}

	private Optional<AppContracts.RiskFactorResponse> buildLabFactor(String userId) {
		return labReportRecordRepository.findByUserCodeOrderByReportDateDesc(userId).stream()
				.findFirst()
				.map(item -> new AppContracts.RiskFactorResponse(
						"LAB",
						"化验单信号",
						toRiskLevel(item.getOverallRiskLevel()),
						item.getSummaryText(),
						item.getReportDate().toString()
				));
	}

	private List<String> buildProactiveSuggestions(List<AppContracts.RiskFactorResponse> factors) {
		List<String> suggestions = new ArrayList<>();
		for (AppContracts.RiskFactorResponse factor : factors) {
			if (factor.code().equals("WEATHER") && factor.riskLevel() != AppContracts.RiskLevel.GREEN) {
				suggestions.add("天气波动较大时，建议优先保暖、补水，并减少高嘌呤外食。");
			}
			if (factor.code().equals("HYDRATION") && factor.riskLevel() != AppContracts.RiskLevel.GREEN) {
				suggestions.add("今天建议尽快补足饮水量，分时段完成会比一次性大量饮水更稳定。");
			}
			if (factor.code().equals("URIC_ACID") && factor.riskLevel() != AppContracts.RiskLevel.GREEN) {
				suggestions.add("建议把近期尿酸、饮食和用药记录放在一起复盘，必要时安排复查。");
			}
			if (factor.code().equals("MEAL") && factor.riskLevel() != AppContracts.RiskLevel.GREEN) {
				suggestions.add("接下来 1 到 2 餐建议降低海鲜、浓汤、酒精等典型诱因暴露。");
			}
			if (factor.code().equals("FLARE") && factor.riskLevel() == AppContracts.RiskLevel.RED) {
				suggestions.add("若疼痛持续加重或伴发热、行动受限，请及时线下就医。");
			}
		}
		return suggestions.stream().distinct().toList();
	}

	private String buildProactiveSummary(
			AppContracts.RiskLevel overallRisk,
			List<AppContracts.RiskFactorResponse> factors,
			WeatherDailySnapshotEntity weatherSnapshot
	) {
		String weatherText = weatherSnapshot == null ? "未设置天气监测城市" : weatherSnapshot.getCityName() + " 天气已纳入评估";
		return "当前主动管理风险等级为 " + overallRisk + "，共识别 " + factors.size() + " 个核心信号；" + weatherText + "。";
	}

	private String buildWeatherSummary(WeatherGatewayService.WeatherObservation observation, AppContracts.RiskLevel riskLevel) {
		return observation.cityName() + " 今日体感温度 " + observation.apparentTemperatureC()
				+ "℃，湿度 " + observation.relativeHumidity() + "%，降水概率 "
				+ observation.precipitationProbability() + "%，天气状态 " + observation.weatherText()
				+ "，天气联合风险 " + riskLevel + "。";
	}

	private Optional<WeatherDailySnapshotEntity> latestWeather(String userId) {
		return weatherDailySnapshotRepository.findFirstByUserCodeOrderBySummaryDateDesc(userId);
	}

	private Optional<UricAcidRecordEntity> latestUricAcid(String userId) {
		return uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream().findFirst();
	}

	private Optional<UricAcidRecordEntity> latestUricAcidBefore(String userId, Instant before) {
		return uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.filter(item -> item.getMeasuredAt().isBefore(before))
				.findFirst();
	}

	private Optional<HydrationRecordEntity> latestHydration(String userId) {
		return hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream().findFirst();
	}

	private AppContracts.RiskLevel computeOverallRisk(List<AppContracts.RiskFactorResponse> factors, int score) {
		if (factors.stream().anyMatch(item -> item.riskLevel() == AppContracts.RiskLevel.RED) || score >= 60) {
			return AppContracts.RiskLevel.RED;
		}
		if (factors.stream().anyMatch(item -> item.riskLevel() == AppContracts.RiskLevel.YELLOW) || score >= 25) {
			return AppContracts.RiskLevel.YELLOW;
		}
		return AppContracts.RiskLevel.GREEN;
	}

	private AppContracts.RiskLevel weatherRisk(WeatherGatewayService.WeatherObservation observation) {
		if (observation.apparentTemperatureC() == null) {
			return AppContracts.RiskLevel.GREEN;
		}
		int humidity = observation.relativeHumidity() == null ? 0 : observation.relativeHumidity();
		int precipitation = observation.precipitationProbability() == null ? 0 : observation.precipitationProbability();
		if (observation.apparentTemperatureC().doubleValue() <= 5 && humidity >= 75) {
			return AppContracts.RiskLevel.RED;
		}
		if (observation.apparentTemperatureC().doubleValue() >= 32 || precipitation >= 70) {
			return AppContracts.RiskLevel.RED;
		}
		if (observation.apparentTemperatureC().doubleValue() <= 10 || humidity >= 80 || precipitation >= 45) {
			return AppContracts.RiskLevel.YELLOW;
		}
		return AppContracts.RiskLevel.GREEN;
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

	private String normalizeCountryCode(String countryCode) {
		return StringUtils.hasText(countryCode) ? countryCode.trim().toUpperCase(Locale.ROOT) : null;
	}
}
