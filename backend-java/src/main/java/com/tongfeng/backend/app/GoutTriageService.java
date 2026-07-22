package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.ClinicalDecisionAuditEntity;
import com.tongfeng.backend.app.persistence.entity.ReminderEventEntity;
import com.tongfeng.backend.app.persistence.repo.ClinicalDecisionAuditRepository;
import com.tongfeng.backend.app.persistence.repo.ReminderEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoutTriageService {

	static final String RULE_VERSION = "GOUT_FLARE_TRIAGE_V1.0.0";
	private static final String DECISION_TYPE = "GOUT_FLARE_TRIAGE";
	private static final String NOT_REVIEWED = "RULE_EVALUATED_NOT_CLINICIAN_REVIEWED";
	private static final String DISCLAIMER = "本结果仅用于健康管理和就医分流，不构成诊断，不能排除关节感染、骨折或其他急症；症状加重或不确定时请及时线下就医。";
	private static final List<AppContracts.ClinicalSourceReference> SOURCES = List.of(
			new AppContracts.ClinicalSourceReference(
					"NICE_NG219",
					"NICE NG219: Gout diagnosis and management",
					"https://www.nice.org.uk/guidance/ng219"
			),
			new AppContracts.ClinicalSourceReference(
					"NHS_SEPTIC_ARTHRITIS",
					"NHS: Septic arthritis",
					"https://www.nhs.uk/conditions/septic-arthritis/"
			),
			new AppContracts.ClinicalSourceReference(
					"ACR_GOUT_GUIDELINE_2020",
					"2020 American College of Rheumatology Guideline for the Management of Gout",
					"https://rheumatology.org/gout-guideline"
			)
	);

	private final ClinicalDecisionAuditRepository auditRepository;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;
	private final ReminderEventRepository reminderEventRepository;

	public GoutTriageService(
			ClinicalDecisionAuditRepository auditRepository,
			IdGenerator idGenerator,
			JsonCodec jsonCodec,
			ReminderEventRepository reminderEventRepository
	) {
		this.auditRepository = auditRepository;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
		this.reminderEventRepository = reminderEventRepository;
	}

	@Transactional
	public AppContracts.GoutFlareTriageResponse evaluate(
			String userId,
			AppContracts.GoutFlareTriageRequest request
	) {
		Instant generatedAt = Instant.now();
		String decisionCode = idGenerator.next("clinical-decision");
		List<String> reasons = new ArrayList<>();
		List<String> redFlags = new ArrayList<>();
		List<String> nextActions = new ArrayList<>();

		boolean possibleJointInfection = request.fever() && request.rednessOrSwelling();
		boolean seriousMobilityConcern = !request.canBearWeight()
				&& (request.traumaHistory() || request.rednessOrSwelling());
		boolean urgent = possibleJointInfection || request.systemicSymptoms() || seriousMobilityConcern;

		String triageCode;
		AppContracts.RiskLevel triageLevel;
		String summary;
		if (urgent) {
			triageCode = "URGENT_OFFLINE";
			triageLevel = AppContracts.RiskLevel.RED;
			summary = "当前回答包含需要尽快线下排查的红旗信号，请不要仅按普通痛风发作自行处理。";
			if (possibleJointInfection) {
				redFlags.add("关节红肿同时伴有发热，需要排查关节感染等急症");
			}
			if (request.systemicSymptoms()) {
				redFlags.add("存在全身不适症状");
			}
			if (seriousMobilityConcern) {
				redFlags.add("无法负重并伴随外伤或关节红肿");
			}
			reasons.addAll(redFlags);
			nextActions.add("尽快前往急诊或可处理急性关节问题的线下医疗机构");
			nextActions.add("携带当前用药、过敏史、既往检查结果和本次起病时间记录");
			nextActions.add("不要因本分流结果自行新增、停用或加量处方药");
		} else if (requiresDoctorContact(request)) {
			triageCode = "CONTACT_DOCTOR_SOON";
			triageLevel = AppContracts.RiskLevel.YELLOW;
			summary = "当前没有命中紧急红旗，但建议尽快联系医生确认诊断、用药或复查安排。";
			appendDoctorContactReasons(request, reasons);
			nextActions.add("尽快联系医生或门诊，说明起病时间、关节位置和疼痛变化");
			nextActions.add("继续记录体温、红肿范围、负重能力和已使用药物");
			nextActions.add("若出现发热伴红肿、全身不适或无法负重，立即转为线下紧急评估");
		} else {
			triageCode = "SELF_MANAGEMENT";
			triageLevel = AppContracts.RiskLevel.GREEN;
			summary = "当前回答未发现紧急红旗，可先按既有医生方案居家观察，并持续记录症状变化。";
			reasons.add("疼痛程度较低，且未报告发热、全身不适、外伤或负重障碍");
			nextActions.add("按医生既有方案处理，不自行调整处方药剂量");
			nextActions.add("记录疼痛、红肿、体温和活动能力的变化");
			nextActions.add("症状加重、持续不缓解或出现红旗信号时及时联系医生");
		}

		AppContracts.GoutFlareTriageResponse response = new AppContracts.GoutFlareTriageResponse(
				decisionCode,
				triageCode,
				triageLevel,
				summary,
				List.copyOf(reasons),
				List.copyOf(redFlags),
				List.copyOf(nextActions),
				RULE_VERSION,
				SOURCES,
				NOT_REVIEWED,
				generatedAt,
				DISCLAIMER
		);
		saveAudit(userId, request, response);
		publishReminder(userId, response);
		return response;
	}

	private boolean requiresDoctorContact(AppContracts.GoutFlareTriageRequest request) {
		return request.firstEpisode()
				|| request.painLevel() >= 7
				|| request.recentMedicationChange()
				|| request.traumaHistory()
				|| request.fever()
				|| !request.canBearWeight();
	}

	private void appendDoctorContactReasons(
			AppContracts.GoutFlareTriageRequest request,
			List<String> reasons
	) {
		if (request.firstEpisode()) {
			reasons.add("这是首次出现类似发作，需要由医生确认原因");
		}
		if (request.painLevel() >= 7) {
			reasons.add("疼痛等级较高，已经可能影响日常活动");
		}
		if (request.recentMedicationChange()) {
			reasons.add("近期用药发生变化，需要核对药物执行和不良反应");
		}
		if (request.traumaHistory()) {
			reasons.add("存在近期外伤史，需要排除损伤相关原因");
		}
		if (request.fever()) {
			reasons.add("报告了发热，即使没有同时红肿也建议尽快复核");
		}
		if (!request.canBearWeight()) {
			reasons.add("当前无法正常负重");
		}
	}

	private void saveAudit(
			String userId,
			AppContracts.GoutFlareTriageRequest request,
			AppContracts.GoutFlareTriageResponse response
	) {
		ClinicalDecisionAuditEntity entity = new ClinicalDecisionAuditEntity();
		entity.setDecisionCode(response.decisionCode());
		entity.setUserCode(userId);
		entity.setDecisionType(DECISION_TYPE);
		entity.setDecisionResult(response.triageCode());
		entity.setRuleVersion(response.ruleVersion());
		entity.setSourceReferencesJson(jsonCodec.toJson(response.sourceReferences()));
		entity.setInputSnapshotJson(jsonCodec.toJson(request));
		entity.setOutputSummary(jsonCodec.toJson(response.reasons()));
		entity.setDecisionPayloadJson(jsonCodec.toJson(response));
		entity.setGeneratedAt(response.generatedAt());
		entity.setManualReviewed(false);
		auditRepository.save(entity);
	}

	private void publishReminder(String userId, AppContracts.GoutFlareTriageResponse response) {
		if ("SELF_MANAGEMENT".equals(response.triageCode())) {
			return;
		}
		Instant now = response.generatedAt();
		ReminderEventEntity entity = new ReminderEventEntity();
		entity.setReminderCode(idGenerator.next("reminder"));
		entity.setUserCode(userId);
		entity.setType("GOUT_TRIAGE");
		entity.setTitle("URGENT_OFFLINE".equals(response.triageCode())
				? "痛风分诊提示尽快线下就医"
				: "痛风分诊提示尽快联系医生");
		entity.setContent(response.summary() + " 下一步：" + String.join("；", response.nextActions()));
		entity.setRiskLevel(response.triageLevel().name());
		entity.setTriggerAt(now);
		entity.setSourceType("GOUT_TRIAGE");
		entity.setStatus("ACTIVE");
		entity.setDedupKey("gout-triage:" + response.decisionCode());
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		reminderEventRepository.save(entity);
	}
}
