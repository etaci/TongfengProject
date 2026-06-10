package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.AccessAuditEntity;
import com.tongfeng.backend.app.persistence.entity.FamilyBindingEntity;
import com.tongfeng.backend.app.persistence.repo.AccessAuditRepository;
import com.tongfeng.backend.app.persistence.repo.FamilyBindingRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessControlService {

	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final String ROLE_PATIENT = "PATIENT";
	private static final String ROLE_CAREGIVER_READ_ONLY = "CAREGIVER_READ_ONLY";
	private static final String ROLE_CAREGIVER_REMINDER = "CAREGIVER_REMINDER";
	private static final String ROLE_CAREGIVER_TASK = "CAREGIVER_TASK";
	private static final String DECISION_ALLOWED = "ALLOWED";

	private final AccessAuditRepository accessAuditRepository;
	private final FamilyBindingRepository familyBindingRepository;
	private final IdGenerator idGenerator;

	public AccessControlService(
			AccessAuditRepository accessAuditRepository,
			FamilyBindingRepository familyBindingRepository,
			IdGenerator idGenerator
	) {
		this.accessAuditRepository = accessAuditRepository;
		this.familyBindingRepository = familyBindingRepository;
		this.idGenerator = idGenerator;
	}

	public AppContracts.AccessPolicyResponse getPolicy(String userId) {
		return new AppContracts.AccessPolicyResponse(
				"SESSION_TOKEN_RBAC_V1",
				"Bearer Session Token + business RBAC",
				"当前版本继续使用服务端会话 Token；JWT 尚未启用，权限边界由角色、资源、动作和家属绑定共同决定。",
				List.of(
						new AppContracts.AccessRoleResponse(
								ROLE_PATIENT,
								"患者本人",
								List.of("管理个人健康档案", "上传和人工确认化验单", "管理家属授权", "查看本人访问审计")
						),
						new AppContracts.AccessRoleResponse(
								ROLE_CAREGIVER_READ_ONLY,
								"只读家属",
								List.of("查看患者授权范围内的摘要", "不能接收高风险提醒", "不能处理共同照护代办")
						),
						new AppContracts.AccessRoleResponse(
								ROLE_CAREGIVER_REMINDER,
								"提醒家属",
								List.of("查看患者授权摘要", "接收高风险提醒", "查看患者开放的周报")
						),
						new AppContracts.AccessRoleResponse(
								ROLE_CAREGIVER_TASK,
								"共同照护家属",
								List.of("查看患者授权摘要", "接收高风险提醒", "处理患者指派的共同照护代办")
						)
				),
				List.of(
						new AppContracts.AccessRuleResponse("PATIENT", "HEALTH_RECORD", "READ_WRITE", "本人数据"),
						new AppContracts.AccessRuleResponse("PATIENT", "FAMILY_PERMISSION", "MANAGE", "本人创建的家属绑定"),
						new AppContracts.AccessRuleResponse("CAREGIVER_*", "FAMILY_PATIENT_SUMMARY", "READ", "有效家属绑定"),
						new AppContracts.AccessRuleResponse("CAREGIVER_REMINDER_OR_TASK", "FAMILY_ALERT", "READ", "开启高风险通知"),
						new AppContracts.AccessRuleResponse("CAREGIVER_TASK", "FAMILY_TASK", "COMPLETE", "患者指派的有效代办")
				),
				List.of(
						new AppContracts.AccessBoundaryResponse("患者-家属边界", "家属只能通过 ACTIVE 绑定访问指定患者数据，不能横向枚举其他患者。"),
						new AppContracts.AccessBoundaryResponse("隐私边界", "权限调整、共享摘要、共享周报会进入访问审计，便于患者追踪。"),
						new AppContracts.AccessBoundaryResponse("管理员边界", "当前版本未开放管理员业务角色；后续如增加后台，需要独立 ADMIN 角色和审计。")
				),
				Instant.now()
		);
	}

	@Transactional
	public void auditPatientAction(
			String actorUserId,
			String sessionCode,
			String resourceType,
			String resourceId,
			String actionType,
			String reason
	) {
		saveAudit(actorUserId, ROLE_PATIENT, actorUserId, resourceType, resourceId, actionType, null, sessionCode, reason);
	}

	@Transactional
	public void auditCaregiverAction(
			String caregiverUserId,
			String sessionCode,
			String patientUserId,
			String resourceType,
			String resourceId,
			String actionType,
			String reason
	) {
		FamilyBindingEntity binding = familyBindingRepository
				.findByPatientUserCodeAndCaregiverUserCodeAndStatus(patientUserId, caregiverUserId, STATUS_ACTIVE)
				.orElseThrow(() -> new BusinessException("FORBIDDEN", "当前没有该患者的有效家属授权"));
		saveAudit(
				caregiverUserId,
				toCaregiverRole(binding.getCaregiverPermission()),
				patientUserId,
				resourceType,
				resourceId,
				actionType,
				binding.getBindingCode(),
				sessionCode,
				reason
		);
	}

	public List<AppContracts.AccessAuditResponse> listOwnAccessAudits(String userId, int limit) {
		return accessAuditRepository.findByActorUserCodeOrderByOperatedAtDesc(userId).stream()
				.limit(limit)
				.map(this::toResponse)
				.toList();
	}

	public List<AppContracts.AccessAuditResponse> listPatientAccessAudits(String userId, int limit) {
		return accessAuditRepository.findByPatientUserCodeOrderByOperatedAtDesc(userId).stream()
				.limit(limit)
				.map(this::toResponse)
				.toList();
	}

	private void saveAudit(
			String actorUserId,
			String actorRole,
			String patientUserId,
			String resourceType,
			String resourceId,
			String actionType,
			String bindingCode,
			String sessionCode,
			String reason
	) {
		AccessAuditEntity entity = new AccessAuditEntity();
		entity.setAuditCode(idGenerator.next("access-audit"));
		entity.setActorUserCode(actorUserId);
		entity.setActorRole(actorRole);
		entity.setPatientUserCode(patientUserId);
		entity.setResourceType(resourceType);
		entity.setResourceId(resourceId);
		entity.setActionType(actionType);
		entity.setDecision(DECISION_ALLOWED);
		entity.setBindingCode(bindingCode);
		entity.setSessionCode(sessionCode);
		entity.setReasonText(reason);
		entity.setOperatedAt(Instant.now());
		accessAuditRepository.save(entity);
	}

	private String toCaregiverRole(String caregiverPermission) {
		return switch (caregiverPermission) {
			case "TASK" -> ROLE_CAREGIVER_TASK;
			case "REMINDER" -> ROLE_CAREGIVER_REMINDER;
			default -> ROLE_CAREGIVER_READ_ONLY;
		};
	}

	private AppContracts.AccessAuditResponse toResponse(AccessAuditEntity entity) {
		return new AppContracts.AccessAuditResponse(
				entity.getAuditCode(),
				entity.getActorUserCode(),
				entity.getActorRole(),
				entity.getPatientUserCode(),
				entity.getResourceType(),
				entity.getResourceId(),
				entity.getActionType(),
				entity.getDecision(),
				entity.getBindingCode(),
				entity.getSessionCode(),
				entity.getReasonText(),
				entity.getOperatedAt()
		);
	}
}
