package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.UserAccountEntity;
import com.tongfeng.backend.app.persistence.repo.UserAccountRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class HealthAutomationScheduler {

	private static final Logger log = LoggerFactory.getLogger(HealthAutomationScheduler.class);

	private final UserAccountRepository userAccountRepository;
	private final HealthRuleEngineService healthRuleEngineService;
	private final ProactiveCareService proactiveCareService;

	public HealthAutomationScheduler(
			UserAccountRepository userAccountRepository,
			HealthRuleEngineService healthRuleEngineService,
			ProactiveCareService proactiveCareService
	) {
		this.userAccountRepository = userAccountRepository;
		this.healthRuleEngineService = healthRuleEngineService;
		this.proactiveCareService = proactiveCareService;
	}

	@Scheduled(cron = "${app.reminder-refresh-cron:0 0/30 * * * *}")
	public void refreshReminders() {
		for (UserAccountEntity user : userAccountRepository.findAll()) {
			try {
				proactiveCareService.refreshTodayWeatherIfConfigured(user.getUserCode());
				healthRuleEngineService.rebuildActiveReminders(user.getUserCode());
			} catch (Exception ex) {
				log.warn("refresh reminders failed for user {}", user.getUserCode(), ex);
			}
		}
	}

	@Scheduled(cron = "${app.summary-refresh-cron:0 5 0 * * *}")
	public void refreshDailySummaries() {
		LocalDate today = LocalDate.now();
		for (UserAccountEntity user : userAccountRepository.findAll()) {
			try {
				healthRuleEngineService.refreshDailySummary(user.getUserCode(), today);
			} catch (Exception ex) {
				log.warn("refresh daily summary failed for user {}", user.getUserCode(), ex);
			}
		}
	}
}
