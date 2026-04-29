package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.FamilyBindingEntity;
import com.tongfeng.backend.app.persistence.entity.GrowthBadgeEntity;
import com.tongfeng.backend.app.persistence.entity.GrowthPointLogEntity;
import com.tongfeng.backend.app.persistence.entity.GrowthProfileEntity;
import com.tongfeng.backend.app.persistence.entity.GrowthRewardClaimEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MedicationPlanEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.repo.DeviceBindingRepository;
import com.tongfeng.backend.app.persistence.repo.FamilyBindingRepository;
import com.tongfeng.backend.app.persistence.repo.GrowthBadgeRepository;
import com.tongfeng.backend.app.persistence.repo.GrowthPointLogRepository;
import com.tongfeng.backend.app.persistence.repo.GrowthProfileRepository;
import com.tongfeng.backend.app.persistence.repo.GrowthRewardClaimRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MedicationPlanRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GrowthIncentiveService {

	public static final String ACTION_MEAL_ANALYZE = "MEAL_ANALYZE";
	public static final String ACTION_URIC_ACID_RECORD = "URIC_ACID_RECORD";
	public static final String ACTION_WEIGHT_RECORD = "WEIGHT_RECORD";
	public static final String ACTION_BLOOD_PRESSURE_RECORD = "BLOOD_PRESSURE_RECORD";
	public static final String ACTION_FLARE_RECORD = "FLARE_RECORD";
	public static final String ACTION_HYDRATION_RECORD = "HYDRATION_RECORD";
	public static final String ACTION_LAB_REPORT = "LAB_REPORT";
	public static final String ACTION_KNOWLEDGE_ASK = "KNOWLEDGE_ASK";
	public static final String ACTION_PROACTIVE_SETTINGS = "PROACTIVE_SETTINGS";
	public static final String ACTION_FAMILY_BIND = "FAMILY_BIND";
	public static final String ACTION_DEVICE_BIND = "DEVICE_BIND";
	public static final String ACTION_MEDICATION_PLAN = "MEDICATION_PLAN";

	private static final ZoneId APP_ZONE = ZoneId.systemDefault();
	private static final int[] LEVEL_THRESHOLDS = {0, 80, 180, 320, 520, 780, 1100};
	private static final String[] LEVEL_TITLES = {
			"健康新手",
			"记录起步者",
			"稳定执行者",
			"风险观察者",
			"闭环管理者",
			"家庭守护者",
			"长期共创者"
	};
	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final String STATUS_CLAIMED = "CLAIMED";

	private static final List<RewardCatalog> REWARD_CATALOGS = List.of(
			new RewardCatalog(
					"DIET_GUIDE_PACK",
					"低嘌呤饮食清单包",
					"兑换一份结构化饮食提醒包，适合前端在成长页展示为已解锁权益。",
					"DIGITAL",
					60,
					1,
					"已解锁低嘌呤饮食清单包，可在前端展示为已领取权益。"
			),
			new RewardCatalog(
					"RECHECK_PLAN_TEMPLATE",
					"复查计划模板",
					"兑换一份复查计划模板，适合与主动管理页面联动展示。",
					"DIGITAL",
					90,
					1,
					"已解锁复查计划模板，可在前端展示为已领取模板权益。"
			),
			new RewardCatalog(
					"FAMILY_SUPPORT_CARD",
					"家属协作关怀卡",
					"适合家属协同场景的成长奖励，可作为亲友支持权益展示。",
					"PRIVILEGE",
					120,
					1,
					"已领取家属协作关怀卡，建议前端在家属协同页展示激励状态。"
			)
	);

	private final GrowthProfileRepository growthProfileRepository;
	private final GrowthPointLogRepository growthPointLogRepository;
	private final GrowthBadgeRepository growthBadgeRepository;
	private final GrowthRewardClaimRepository growthRewardClaimRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final MealRecordRepository mealRecordRepository;
	private final MedicationPlanRepository medicationPlanRepository;
	private final DeviceBindingRepository deviceBindingRepository;
	private final FamilyBindingRepository familyBindingRepository;
	private final HealthRuleEngineService healthRuleEngineService;
	private final IdGenerator idGenerator;

	public GrowthIncentiveService(
			GrowthProfileRepository growthProfileRepository,
			GrowthPointLogRepository growthPointLogRepository,
			GrowthBadgeRepository growthBadgeRepository,
			GrowthRewardClaimRepository growthRewardClaimRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			MealRecordRepository mealRecordRepository,
			MedicationPlanRepository medicationPlanRepository,
			DeviceBindingRepository deviceBindingRepository,
			FamilyBindingRepository familyBindingRepository,
			HealthRuleEngineService healthRuleEngineService,
			IdGenerator idGenerator
	) {
		this.growthProfileRepository = growthProfileRepository;
		this.growthPointLogRepository = growthPointLogRepository;
		this.growthBadgeRepository = growthBadgeRepository;
		this.growthRewardClaimRepository = growthRewardClaimRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.mealRecordRepository = mealRecordRepository;
		this.medicationPlanRepository = medicationPlanRepository;
		this.deviceBindingRepository = deviceBindingRepository;
		this.familyBindingRepository = familyBindingRepository;
		this.healthRuleEngineService = healthRuleEngineService;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public void awardRecordAction(String userId, String actionType, String businessCode, String summary) {
		awardPoints(
				userId,
				actionType,
				actionType + ":" + businessCode,
				businessCode,
				resolveActionPoints(actionType),
				summary,
				LocalDate.now(APP_ZONE)
		);
	}

	@Transactional
	public void awardDailyAction(String userId, String actionType, String summary) {
		LocalDate today = LocalDate.now(APP_ZONE);
		awardPoints(
				userId,
				actionType,
				actionType + ":" + today,
				today.toString(),
				resolveActionPoints(actionType),
				summary,
				today
		);
	}

	public AppContracts.GrowthOverviewResponse getGrowthOverview(String userId) {
		GrowthProfileEntity profile = growthProfileRepository.findByUserCode(userId)
				.orElseGet(() -> buildDefaultProfile(userId, Instant.now()));
		LocalDate today = LocalDate.now(APP_ZONE);
		int level = profile.getLevelNo();
		int totalPoints = defaultInt(profile.getTotalPoints());
		int redeemablePoints = availablePoints(profile);
		Integer nextLevelPoints = resolveNextLevelPoints(level);
		int todayPoints = growthPointLogRepository.findByUserCodeAndAwardedDateOrderByCreatedAtDesc(userId, today).stream()
				.mapToInt(item -> defaultInt(item.getPoints()))
				.sum();
		List<String> highlights = new ArrayList<>();
		highlights.add("当前等级 " + level + " - " + resolveLevelTitle(level));
		highlights.add("已连续活跃 " + defaultInt(profile.getCurrentStreakDays()) + " 天");
		highlights.add("当前可兑换积分 " + redeemablePoints + " 分");
		if (nextLevelPoints != null) {
			highlights.add("距离下一等级还差 " + Math.max(nextLevelPoints - totalPoints, 0) + " 积分");
		} else {
			highlights.add("已达到当前成长体系最高等级");
		}
		return new AppContracts.GrowthOverviewResponse(
				userId,
				level,
				resolveLevelTitle(level),
				totalPoints,
				redeemablePoints,
				resolveCurrentLevelMinPoints(level),
				nextLevelPoints,
				defaultInt(profile.getCurrentStreakDays()),
				defaultInt(profile.getLongestStreakDays()),
				todayPoints,
				growthBadgeRepository.findByUserCodeOrderByAwardedAtDesc(userId).size(),
				highlights
		);
	}

	public List<AppContracts.GrowthTaskResponse> listGrowthTasks(String userId) {
		LocalDate today = LocalDate.now(APP_ZONE);
		int mealCount = countTodayMeals(userId, today);
		int uricAcidCount = countTodayUricAcid(userId, today);
		int hydrationCount = countTodayHydration(userId, today);
		int knowledgeCount = countDailyAction(userId, today, ACTION_KNOWLEDGE_ASK);
		int medicationCount = medicationPlanRepository.findByUserCode(userId)
				.map(MedicationPlanEntity::getUpdatedAt)
				.filter(updatedAt -> sameDate(updatedAt, today))
				.map(item -> 1)
				.orElse(0);
		return List.of(
				buildTask("DAILY_MEAL", "记录一餐", "完成一次餐盘识别或饮食记录", resolveActionPoints(ACTION_MEAL_ANALYZE), mealCount, 1),
				buildTask("DAILY_URIC_ACID", "尿酸记录", "补充一次今日尿酸数据", resolveActionPoints(ACTION_URIC_ACID_RECORD), uricAcidCount, 1),
				buildTask("DAILY_HYDRATION", "补水打卡", "完成一次饮水/尿液颜色记录", resolveActionPoints(ACTION_HYDRATION_RECORD), hydrationCount, 1),
				buildTask("DAILY_KNOWLEDGE", "知识学习", "完成一次痛风知识问答", resolveActionPoints(ACTION_KNOWLEDGE_ASK), knowledgeCount, 1),
				buildTask("DAILY_MEDICATION", "用药确认", "更新一次用药计划或随访备注", resolveActionPoints(ACTION_MEDICATION_PLAN), medicationCount, 1)
		);
	}

	public AppContracts.GrowthWeeklyPlanResponse getWeeklyPlan(String userId) {
		LocalDate today = LocalDate.now(APP_ZONE);
		LocalDate weekStart = today.with(DayOfWeek.MONDAY);
		LocalDate weekEnd = weekStart.plusDays(6);
		int weeklyEarnedPoints = growthPointLogRepository.findByUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(item -> !item.getAwardedDate().isBefore(weekStart) && !item.getAwardedDate().isAfter(weekEnd))
				.mapToInt(item -> defaultInt(item.getPoints()))
				.sum();
		int targetPoints = resolveWeeklyTargetPoints(userId);
		List<AppContracts.GrowthChallengeResponse> challenges = new ArrayList<>();
		challenges.add(buildWeeklyMealChallenge(userId, weekStart, weekEnd));
		challenges.add(buildWeeklyHydrationChallenge(userId, weekStart, weekEnd));
		challenges.add(buildWeeklyUricAcidChallenge(userId, weekStart, weekEnd));
		challenges.add(buildWeeklyKnowledgeChallenge(userId, weekStart, weekEnd));
		AppContracts.GrowthChallengeResponse riskChallenge = buildRiskChallenge(userId, weekStart, weekEnd);
		if (riskChallenge != null) {
			challenges.add(riskChallenge);
		}
		challenges.add(buildFamilyChallenge(userId));
		int progressPercent = targetPoints <= 0 ? 100 : Math.min(100, weeklyEarnedPoints * 100 / targetPoints);
		return new AppContracts.GrowthWeeklyPlanResponse(
				weekStart.toString(),
				weekEnd.toString(),
				weeklyEarnedPoints,
				targetPoints,
				progressPercent,
				challenges
		);
	}

	public List<AppContracts.GrowthRewardResponse> listRewards(String userId) {
		GrowthProfileEntity profile = growthProfileRepository.findByUserCode(userId)
				.orElseGet(() -> buildDefaultProfile(userId, Instant.now()));
		int redeemablePoints = availablePoints(profile);
		return REWARD_CATALOGS.stream()
				.map(catalog -> {
					long claimedCount = growthRewardClaimRepository.countByUserCodeAndRewardKey(userId, catalog.rewardKey());
					int remainingClaims = Math.max(catalog.perUserLimit() - (int) claimedCount, 0);
					boolean claimable = remainingClaims > 0 && redeemablePoints >= catalog.pointsCost();
					String claimHint;
					if (remainingClaims <= 0) {
						claimHint = "当前奖励已达到个人领取上限";
					} else if (redeemablePoints < catalog.pointsCost()) {
						claimHint = "积分不足，还差 " + (catalog.pointsCost() - redeemablePoints) + " 分";
					} else {
						claimHint = "可立即兑换";
					}
					return new AppContracts.GrowthRewardResponse(
							catalog.rewardKey(),
							catalog.rewardName(),
							catalog.rewardDescription(),
							catalog.rewardType(),
							catalog.pointsCost(),
							remainingClaims,
							claimable,
							claimHint
					);
				})
				.toList();
	}

	@Transactional
	public AppContracts.GrowthRewardClaimResponse claimReward(String userId, String rewardKey) {
		RewardCatalog catalog = findRewardCatalog(rewardKey);
		long claimedCount = growthRewardClaimRepository.countByUserCodeAndRewardKey(userId, catalog.rewardKey());
		if (claimedCount >= catalog.perUserLimit()) {
			throw new BusinessException("GROWTH_REWARD_LIMIT", "当前奖励已达到领取上限");
		}
		GrowthProfileEntity profile = getOrCreateProfile(userId);
		int redeemablePoints = availablePoints(profile);
		if (redeemablePoints < catalog.pointsCost()) {
			throw new BusinessException("GROWTH_POINTS_NOT_ENOUGH", "当前可兑换积分不足，无法领取该奖励");
		}
		Instant now = Instant.now();
		profile.setRedeemedPoints(defaultInt(profile.getRedeemedPoints()) + catalog.pointsCost());
		profile.setUpdatedAt(now);
		growthProfileRepository.save(profile);
		refreshBadges(userId, profile);

		GrowthRewardClaimEntity entity = new GrowthRewardClaimEntity();
		entity.setClaimCode(idGenerator.next("claim"));
		entity.setUserCode(userId);
		entity.setRewardKey(catalog.rewardKey());
		entity.setRewardName(catalog.rewardName());
		entity.setRewardType(catalog.rewardType());
		entity.setPointsCost(catalog.pointsCost());
		entity.setStatus(STATUS_CLAIMED);
		entity.setClaimNote(catalog.claimNote());
		entity.setClaimedAt(now);
		growthRewardClaimRepository.save(entity);
		return toRewardClaimResponse(entity, availablePoints(profile));
	}

	public List<AppContracts.GrowthRewardClaimResponse> listRewardClaims(String userId) {
		GrowthProfileEntity profile = growthProfileRepository.findByUserCode(userId)
				.orElseGet(() -> buildDefaultProfile(userId, Instant.now()));
		int remainingPoints = availablePoints(profile);
		return growthRewardClaimRepository.findByUserCodeOrderByClaimedAtDesc(userId).stream()
				.map(item -> toRewardClaimResponse(item, remainingPoints))
				.toList();
	}

	public List<AppContracts.GrowthPointLogResponse> listPointLogs(String userId, int limit) {
		return growthPointLogRepository.findTop50ByUserCodeOrderByCreatedAtDesc(userId).stream()
				.limit(Math.max(limit, 1))
				.map(item -> new AppContracts.GrowthPointLogResponse(
						item.getPointCode(),
						item.getActionType(),
						defaultInt(item.getPoints()),
						item.getSummaryText(),
						item.getAwardedDate().toString(),
						item.getCreatedAt()
				))
				.toList();
	}

	public List<AppContracts.GrowthBadgeResponse> listBadges(String userId) {
		return growthBadgeRepository.findByUserCodeOrderByAwardedAtDesc(userId).stream()
				.map(item -> new AppContracts.GrowthBadgeResponse(
						item.getBadgeKey(),
						item.getBadgeName(),
						item.getBadgeDescription(),
						item.getAwardedAt()
				))
				.toList();
	}

	@Transactional
	protected void awardPoints(
			String userId,
			String actionType,
			String dedupKey,
			String businessCode,
			int points,
			String summary,
			LocalDate activeDate
	) {
		if (points <= 0) {
			return;
		}
		if (growthPointLogRepository.findByUserCodeAndDedupKey(userId, dedupKey).isPresent()) {
			return;
		}
		Instant now = Instant.now();
		GrowthPointLogEntity logEntity = new GrowthPointLogEntity();
		logEntity.setPointCode(idGenerator.next("point"));
		logEntity.setUserCode(userId);
		logEntity.setActionType(actionType);
		logEntity.setDedupKey(dedupKey);
		logEntity.setBusinessCode(businessCode);
		logEntity.setPoints(points);
		logEntity.setAwardedDate(activeDate);
		logEntity.setSummaryText(summary);
		logEntity.setCreatedAt(now);
		growthPointLogRepository.save(logEntity);

		GrowthProfileEntity profile = getOrCreateProfile(userId);
		profile.setTotalPoints(defaultInt(profile.getTotalPoints()) + points);
		updateStreak(profile, activeDate);
		profile.setLevelNo(resolveLevel(profile.getTotalPoints()));
		profile.setUpdatedAt(now);
		growthProfileRepository.save(profile);
		refreshBadges(userId, profile);
	}

	private AppContracts.GrowthChallengeResponse buildWeeklyMealChallenge(String userId, LocalDate weekStart, LocalDate weekEnd) {
		int progress = countMealsInRange(userId, weekStart, weekEnd);
		return buildChallenge(
				"WEEKLY_MEAL",
				"WEEKLY",
				"本周饮食记录计划",
				"本周至少完成 4 次饮食记录，帮助系统识别稳定的饮食风险模式。",
				20,
				progress,
				4,
				"MEDIUM",
				List.of("建议分散到不同天完成", "高风险餐后更适合补一条记录")
		);
	}

	private AppContracts.GrowthChallengeResponse buildWeeklyHydrationChallenge(String userId, LocalDate weekStart, LocalDate weekEnd) {
		int progress = countHydrationInRange(userId, weekStart, weekEnd);
		return buildChallenge(
				"WEEKLY_HYDRATION",
				"WEEKLY",
				"本周补水计划",
				"本周至少完成 5 次补水打卡，帮助降低脱水相关风险。",
				18,
				progress,
				5,
				"MEDIUM",
				List.of("可在上午、午后、晚间分次记录", "尿液颜色偏深时优先完成")
		);
	}

	private AppContracts.GrowthChallengeResponse buildWeeklyUricAcidChallenge(String userId, LocalDate weekStart, LocalDate weekEnd) {
		int progress = countUricAcidInRange(userId, weekStart, weekEnd);
		return buildChallenge(
				"WEEKLY_URIC_ACID",
				"WEEKLY",
				"本周尿酸观察计划",
				"本周至少补充 1 次尿酸记录，保持趋势观察不断档。",
				25,
				progress,
				1,
				"HIGH",
				List.of("晨起空腹或固定时段记录更利于横向比较")
		);
	}

	private AppContracts.GrowthChallengeResponse buildWeeklyKnowledgeChallenge(String userId, LocalDate weekStart, LocalDate weekEnd) {
		int progress = countActionInRange(userId, weekStart, weekEnd, ACTION_KNOWLEDGE_ASK);
		return buildChallenge(
				"WEEKLY_KNOWLEDGE",
				"WEEKLY",
				"本周知识学习计划",
				"本周至少完成 2 次知识问答，把系统建议转成可执行行动。",
				10,
				progress,
				2,
				"LOW",
				List.of("可以围绕饮食、复查、发作预警分别提问")
		);
	}

	private AppContracts.GrowthChallengeResponse buildRiskChallenge(String userId, LocalDate weekStart, LocalDate weekEnd) {
		List<AppContracts.ReminderResponse> reminders = healthRuleEngineService.getActiveReminders(userId).stream()
				.filter(item -> item.riskLevel() != AppContracts.RiskLevel.GREEN)
				.toList();
		if (reminders.isEmpty()) {
			return null;
		}
		AppContracts.ReminderResponse topReminder = reminders.getFirst();
		String content = (topReminder.title() + " " + topReminder.content()).toUpperCase();
		if (content.contains("尿酸")) {
			int progress = Math.min(countUricAcidInRange(userId, weekStart, weekEnd), 1)
					+ Math.min(countHydrationInRange(userId, weekStart, weekEnd), 2);
			return buildChallenge(
					"RISK_URIC_ACID",
					"RISK",
					"尿酸风险消减挑战",
					"根据当前提醒，建议本周完成 1 次尿酸记录和 2 次补水打卡。",
					30,
					progress,
					3,
					riskPriority(topReminder.riskLevel()),
					List.of(topReminder.content(), "完成后更适合回看主动风险简报变化")
			);
		}
		if (content.contains("疼痛") || content.contains("发作")) {
			int medicationCount = countMedicationUpdatedInRange(userId, weekStart, weekEnd);
			int knowledgeCount = countActionInRange(userId, weekStart, weekEnd, ACTION_KNOWLEDGE_ASK);
			int progress = Math.min(medicationCount, 1) + Math.min(knowledgeCount, 1);
			return buildChallenge(
					"RISK_FLARE",
					"RISK",
					"发作管理挑战",
					"当前存在发作相关风险，建议本周补 1 次用药/复查计划并完成 1 次知识问答。",
					28,
					progress,
					2,
					riskPriority(topReminder.riskLevel()),
					List.of(topReminder.content(), "如症状持续加重，应优先线下就医")
			);
		}
		int progress = Math.min(countHydrationInRange(userId, weekStart, weekEnd), 1)
				+ Math.min(countMealsInRange(userId, weekStart, weekEnd), 1);
		return buildChallenge(
				"RISK_GENERAL",
				"RISK",
				"本周风险应对挑战",
				"根据当前提醒，建议本周补 1 次补水打卡和 1 次饮食记录。",
				20,
				progress,
				2,
				riskPriority(topReminder.riskLevel()),
				List.of(topReminder.content())
		);
	}

	private AppContracts.GrowthChallengeResponse buildFamilyChallenge(String userId) {
		int activeBindings = countActiveFamilyBindings(userId);
		if (activeBindings > 0) {
			return buildChallenge(
					"FAMILY_KEEP",
					"FAMILY",
					"保持家属协作",
					"你当前已建立家属协作关系，继续保持至少 1 条有效绑定。",
					18,
					1,
					1,
					"LOW",
					List.of("可引导家属查看风险摘要与提醒", "适合搭配主动管理简报一起使用")
			);
		}
		return buildChallenge(
				"FAMILY_ONBOARD",
				"FAMILY",
				"开启家属协作",
				"邀请至少 1 位家属加入协作，帮助风险提醒形成双人闭环。",
				18,
				0,
				1,
				"MEDIUM",
				List.of("适合与高风险提醒场景一起引导", "完成绑定后可解锁家属协作相关成长奖励")
		);
	}

	private AppContracts.GrowthChallengeResponse buildChallenge(
			String challengeCode,
			String category,
			String title,
			String description,
			int rewardPoints,
			int completedCount,
			int targetCount,
			String priority,
			List<String> hints
	) {
		return new AppContracts.GrowthChallengeResponse(
				challengeCode,
				category,
				title,
				description,
				rewardPoints,
				completedCount,
				targetCount,
				priority,
				completedCount >= targetCount,
				hints
		);
	}

	private GrowthProfileEntity getOrCreateProfile(String userId) {
		return growthProfileRepository.findByUserCode(userId)
				.orElseGet(() -> {
					Instant now = Instant.now();
					GrowthProfileEntity entity = buildDefaultProfile(userId, now);
					return growthProfileRepository.save(entity);
				});
	}

	private GrowthProfileEntity buildDefaultProfile(String userId, Instant now) {
		GrowthProfileEntity entity = new GrowthProfileEntity();
		entity.setUserCode(userId);
		entity.setLevelNo(1);
		entity.setTotalPoints(0);
		entity.setRedeemedPoints(0);
		entity.setCurrentStreakDays(0);
		entity.setLongestStreakDays(0);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		return entity;
	}

	private void updateStreak(GrowthProfileEntity profile, LocalDate activeDate) {
		LocalDate lastActiveDate = profile.getLastActiveDate();
		int currentStreak = defaultInt(profile.getCurrentStreakDays());
		if (lastActiveDate == null) {
			currentStreak = 1;
		} else if (lastActiveDate.isEqual(activeDate)) {
			currentStreak = Math.max(currentStreak, 1);
		} else if (lastActiveDate.plus(1, ChronoUnit.DAYS).isEqual(activeDate)) {
			currentStreak = Math.max(currentStreak, 0) + 1;
		} else if (activeDate.isAfter(lastActiveDate)) {
			currentStreak = 1;
		}
		profile.setLastActiveDate(activeDate);
		profile.setCurrentStreakDays(currentStreak);
		profile.setLongestStreakDays(Math.max(defaultInt(profile.getLongestStreakDays()), currentStreak));
	}

	private void refreshBadges(String userId, GrowthProfileEntity profile) {
		if (!growthPointLogRepository.findByUserCodeOrderByCreatedAtDesc(userId).isEmpty()) {
			unlockBadge(userId, "FIRST_STEP", "迈出第一步", "首次完成健康管理动作并获得积分");
		}
		if (!uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).isEmpty()) {
			unlockBadge(userId, "URIC_ACID_STARTER", "尿酸观察员", "已建立首条尿酸跟踪记录");
		}
		if (growthPointLogRepository.findByUserCodeOrderByCreatedAtDesc(userId).stream()
				.anyMatch(item -> ACTION_BLOOD_PRESSURE_RECORD.equals(item.getActionType()))) {
			unlockBadge(userId, "BP_STARTER", "血压记录员", "已建立首条血压跟踪记录");
		}
		if (hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).size() >= 3) {
			unlockBadge(userId, "HYDRATION_KEEPER", "补水坚持者", "累计完成 3 次补水打卡");
		}
		if (deviceBindingRepository.findByUserCodeOrderByCreatedAtDesc(userId).stream().anyMatch(item -> STATUS_ACTIVE.equals(item.getStatus()))) {
			unlockBadge(userId, "DEVICE_PIONEER", "设备先锋", "已成功接入至少 1 台健康设备");
		}
		if (hasAnyActiveFamilyBinding(userId)) {
			unlockBadge(userId, "FAMILY_ALLY", "家属同盟", "已建立家属协同关系");
		}
		if (defaultInt(profile.getLongestStreakDays()) >= 3) {
			unlockBadge(userId, "STREAK_THREE", "连续行动者", "连续活跃达到 3 天");
		}
		if (defaultInt(profile.getRedeemedPoints()) >= 60) {
			unlockBadge(userId, "REWARD_OPENER", "奖励解锁者", "已完成首次成长奖励兑换");
		}
	}

	private boolean hasAnyActiveFamilyBinding(String userId) {
		return familyBindingRepository.findByPatientUserCodeOrderByCreatedAtDesc(userId).stream()
				.map(FamilyBindingEntity::getStatus)
				.anyMatch(STATUS_ACTIVE::equals)
				|| familyBindingRepository.findByCaregiverUserCodeOrderByCreatedAtDesc(userId).stream()
				.map(FamilyBindingEntity::getStatus)
				.anyMatch(STATUS_ACTIVE::equals);
	}

	@Transactional
	protected void unlockBadge(String userId, String badgeKey, String badgeName, String badgeDescription) {
		if (growthBadgeRepository.findByUserCodeAndBadgeKey(userId, badgeKey).isPresent()) {
			return;
		}
		GrowthBadgeEntity entity = new GrowthBadgeEntity();
		entity.setAwardCode(idGenerator.next("badge"));
		entity.setUserCode(userId);
		entity.setBadgeKey(badgeKey);
		entity.setBadgeName(badgeName);
		entity.setBadgeDescription(badgeDescription);
		entity.setAwardedAt(Instant.now());
		growthBadgeRepository.save(entity);
	}

	private AppContracts.GrowthTaskResponse buildTask(
			String taskCode,
			String title,
			String description,
			int rewardPoints,
			int completedCount,
			int targetCount
	) {
		return new AppContracts.GrowthTaskResponse(
				taskCode,
				title,
				description,
				rewardPoints,
				completedCount,
				targetCount,
				completedCount >= targetCount
		);
	}

	private AppContracts.GrowthRewardClaimResponse toRewardClaimResponse(GrowthRewardClaimEntity entity, int remainingPoints) {
		return new AppContracts.GrowthRewardClaimResponse(
				entity.getClaimCode(),
				entity.getRewardKey(),
				entity.getRewardName(),
				entity.getRewardType(),
				defaultInt(entity.getPointsCost()),
				remainingPoints,
				entity.getStatus(),
				entity.getClaimNote(),
				entity.getClaimedAt()
		);
	}

	private RewardCatalog findRewardCatalog(String rewardKey) {
		return REWARD_CATALOGS.stream()
				.filter(item -> item.rewardKey().equalsIgnoreCase(rewardKey))
				.findFirst()
				.orElseThrow(() -> new BusinessException("GROWTH_REWARD_NOT_FOUND", "奖励不存在"));
	}

	private int countTodayMeals(String userId, LocalDate today) {
		return (int) mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream()
				.filter(item -> sameDate(item.getTakenAt(), today))
				.count();
	}

	private int countTodayUricAcid(String userId, LocalDate today) {
		return (int) uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.filter(item -> sameDate(item.getMeasuredAt(), today))
				.count();
	}

	private int countTodayHydration(String userId, LocalDate today) {
		return (int) hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream()
				.filter(item -> sameDate(item.getCheckedAt(), today))
				.count();
	}

	private int countDailyAction(String userId, LocalDate date, String actionType) {
		return (int) growthPointLogRepository.findByUserCodeAndAwardedDateOrderByCreatedAtDesc(userId, date).stream()
				.filter(item -> actionType.equals(item.getActionType()))
				.count();
	}

	private int countMealsInRange(String userId, LocalDate start, LocalDate end) {
		return (int) mealRecordRepository.findByUserCodeOrderByTakenAtDesc(userId).stream()
				.map(MealRecordEntity::getTakenAt)
				.filter(item -> betweenDates(item, start, end))
				.count();
	}

	private int countHydrationInRange(String userId, LocalDate start, LocalDate end) {
		return (int) hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream()
				.map(HydrationRecordEntity::getCheckedAt)
				.filter(item -> betweenDates(item, start, end))
				.count();
	}

	private int countUricAcidInRange(String userId, LocalDate start, LocalDate end) {
		return (int) uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.map(UricAcidRecordEntity::getMeasuredAt)
				.filter(item -> betweenDates(item, start, end))
				.count();
	}

	private int countActionInRange(String userId, LocalDate start, LocalDate end, String actionType) {
		return (int) growthPointLogRepository.findByUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(item -> actionType.equals(item.getActionType()))
				.filter(item -> !item.getAwardedDate().isBefore(start) && !item.getAwardedDate().isAfter(end))
				.count();
	}

	private int countMedicationUpdatedInRange(String userId, LocalDate start, LocalDate end) {
		return medicationPlanRepository.findByUserCode(userId)
				.map(MedicationPlanEntity::getUpdatedAt)
				.filter(item -> betweenDates(item, start, end))
				.map(item -> 1)
				.orElse(0);
	}

	private int countActiveFamilyBindings(String userId) {
		int asPatient = (int) familyBindingRepository.findByPatientUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
				.count();
		int asCaregiver = (int) familyBindingRepository.findByCaregiverUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
				.count();
		return asPatient + asCaregiver;
	}

	private int resolveActionPoints(String actionType) {
		return switch (actionType) {
			case ACTION_MEAL_ANALYZE -> 12;
			case ACTION_URIC_ACID_RECORD -> 20;
			case ACTION_WEIGHT_RECORD -> 8;
			case ACTION_BLOOD_PRESSURE_RECORD -> 12;
			case ACTION_FLARE_RECORD -> 15;
			case ACTION_HYDRATION_RECORD -> 10;
			case ACTION_LAB_REPORT -> 25;
			case ACTION_KNOWLEDGE_ASK -> 5;
			case ACTION_PROACTIVE_SETTINGS -> 8;
			case ACTION_FAMILY_BIND -> 18;
			case ACTION_DEVICE_BIND -> 18;
			case ACTION_MEDICATION_PLAN -> 10;
			default -> 0;
		};
	}

	private int resolveLevel(int totalPoints) {
		int level = 1;
		for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
			if (totalPoints >= LEVEL_THRESHOLDS[i]) {
				level = i + 1;
			}
		}
		return level;
	}

	private String resolveLevelTitle(int level) {
		int index = Math.min(Math.max(level, 1), LEVEL_TITLES.length) - 1;
		return LEVEL_TITLES[index];
	}

	private int resolveCurrentLevelMinPoints(int level) {
		int index = Math.min(Math.max(level, 1), LEVEL_THRESHOLDS.length) - 1;
		return LEVEL_THRESHOLDS[index];
	}

	private Integer resolveNextLevelPoints(int level) {
		if (level >= LEVEL_THRESHOLDS.length) {
			return null;
		}
		return LEVEL_THRESHOLDS[level];
	}

	private int resolveWeeklyTargetPoints(String userId) {
		GrowthProfileEntity profile = growthProfileRepository.findByUserCode(userId)
				.orElseGet(() -> buildDefaultProfile(userId, Instant.now()));
		return 60 + Math.max(profile.getLevelNo() - 1, 0) * 20;
	}

	private String riskPriority(AppContracts.RiskLevel riskLevel) {
		return switch (riskLevel) {
			case RED -> "HIGH";
			case YELLOW -> "MEDIUM";
			case GREEN -> "LOW";
		};
	}

	private int availablePoints(GrowthProfileEntity profile) {
		return Math.max(defaultInt(profile.getTotalPoints()) - defaultInt(profile.getRedeemedPoints()), 0);
	}

	private boolean sameDate(Instant time, LocalDate date) {
		return time != null && time.atZone(APP_ZONE).toLocalDate().isEqual(date);
	}

	private boolean betweenDates(Instant time, LocalDate start, LocalDate end) {
		if (time == null) {
			return false;
		}
		LocalDate date = time.atZone(APP_ZONE).toLocalDate();
		return !date.isBefore(start) && !date.isAfter(end);
	}

	private int defaultInt(Integer value) {
		return value == null ? 0 : value;
	}

	private record RewardCatalog(
			String rewardKey,
			String rewardName,
			String rewardDescription,
			String rewardType,
			int pointsCost,
			int perUserLimit,
			String claimNote
	) {
	}
}
