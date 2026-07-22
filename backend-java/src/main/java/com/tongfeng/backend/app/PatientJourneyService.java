package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.ClinicalDecisionAuditEntity;
import com.tongfeng.backend.app.persistence.repo.ClinicalDecisionAuditRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientJourneyService {

	private static final String TRIAGE_TYPE = "GOUT_FLARE_TRIAGE";

	private final HealthAssistantService healthAssistantService;
	private final ClinicalDecisionAuditRepository auditRepository;
	private final JsonCodec jsonCodec;

	public PatientJourneyService(
			HealthAssistantService healthAssistantService,
			ClinicalDecisionAuditRepository auditRepository,
			JsonCodec jsonCodec
	) {
		this.healthAssistantService = healthAssistantService;
		this.auditRepository = auditRepository;
		this.jsonCodec = jsonCodec;
	}

	@Transactional(readOnly = true)
	public AppContracts.TodayActionPlanResponse getTodayActionPlan(String userId) {
		AppContracts.TodayActionPlanResponse base = healthAssistantService.getTodayActionPlan(userId);
		return auditRepository.findFirstByUserCodeAndDecisionTypeOrderByGeneratedAtDesc(userId, TRIAGE_TYPE)
				.map(audit -> mergeTodayPlan(base, readTriage(audit)))
				.orElse(base);
	}

	@Transactional(readOnly = true)
	public AppContracts.TimelineResponse getTimeline(String userId) {
		List<AppContracts.TimelineEvent> events = new ArrayList<>(healthAssistantService.getTimeline(userId).events());
		for (ClinicalDecisionAuditEntity audit : auditRepository.findByUserCodeAndDecisionTypeOrderByGeneratedAtDesc(userId, TRIAGE_TYPE)) {
			AppContracts.GoutFlareTriageResponse triage = readTriage(audit);
			events.add(new AppContracts.TimelineEvent(
					audit.getDecisionCode(),
					"GOUT_TRIAGE",
					"痛风发作结构化分诊",
					triage.summary(),
					audit.getGeneratedAt(),
					triage.triageLevel(),
					triage.triageCode(),
					triage.ruleVersion(),
					triage.decisionCode(),
					triage.verificationStatus(),
					triage.redFlags(),
					triage.nextActions()
			));
		}
		events.sort(Comparator.comparing(AppContracts.TimelineEvent::occurredAt).reversed());
		return new AppContracts.TimelineResponse(events);
	}

	private AppContracts.TodayActionPlanResponse mergeTodayPlan(
			AppContracts.TodayActionPlanResponse base,
			AppContracts.GoutFlareTriageResponse triage
	) {
		List<String> reasons = new ArrayList<>(base.reasons());
		reasons.addAll(triage.reasons());
		List<AppContracts.TodayActionItemResponse> actions = new ArrayList<>();
		for (int i = 0; i < triage.nextActions().size(); i++) {
			actions.add(new AppContracts.TodayActionItemResponse(
					"GOUT_TRIAGE_" + i,
					"GOUT_TRIAGE",
					"痛风分诊下一步行动",
					triage.nextActions().get(i),
					triage.triageLevel() == AppContracts.RiskLevel.RED ? "HIGH" : "MEDIUM",
					"ACTIONABLE"
			));
		}
		actions.addAll(base.actions());
		return new AppContracts.TodayActionPlanResponse(
				base.userId(),
				maxRisk(base.overallRiskLevel(), triage.triageLevel()),
				triage.triageCode(),
				triage.decisionCode(),
				triage.ruleVersion(),
				triage.verificationStatus(),
				triage.redFlags(),
				base.triageTitle(),
				triage.summary(),
				triage.nextActions().isEmpty() ? base.nextStep() : triage.nextActions().getFirst(),
				List.copyOf(reasons),
				List.copyOf(actions),
				base.trustNotes(),
				base.generatedAt()
		);
	}

	private AppContracts.GoutFlareTriageResponse readTriage(ClinicalDecisionAuditEntity audit) {
		if (audit.getDecisionPayloadJson() == null || audit.getDecisionPayloadJson().isBlank()) {
			throw new BusinessException("CLINICAL_DECISION_PAYLOAD_MISSING", "分诊审计结果缺少可信载荷");
		}
		return jsonCodec.fromJson(audit.getDecisionPayloadJson(), AppContracts.GoutFlareTriageResponse.class);
	}

	private AppContracts.RiskLevel maxRisk(AppContracts.RiskLevel left, AppContracts.RiskLevel right) {
		return rank(right) > rank(left) ? right : left;
	}

	private int rank(AppContracts.RiskLevel riskLevel) {
		return switch (riskLevel) {
			case GREEN -> 0;
			case YELLOW -> 1;
			case RED -> 2;
		};
	}
}
