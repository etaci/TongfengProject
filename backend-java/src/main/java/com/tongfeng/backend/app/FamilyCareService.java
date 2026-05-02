package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.FamilyBindingEntity;
import com.tongfeng.backend.app.persistence.entity.FamilyInviteEntity;
import com.tongfeng.backend.app.persistence.entity.FamilyTaskEntity;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UserAccountEntity;
import com.tongfeng.backend.app.persistence.repo.FamilyBindingRepository;
import com.tongfeng.backend.app.persistence.repo.FamilyInviteRepository;
import com.tongfeng.backend.app.persistence.repo.FamilyTaskRepository;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UserAccountRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class FamilyCareService {

	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_ACCEPTED = "ACCEPTED";
	private static final String STATUS_CANCELLED = "CANCELLED";
	private static final String STATUS_EXPIRED = "EXPIRED";
	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final String STATUS_UNBOUND = "UNBOUND";
	private static final String STATUS_OPEN = "OPEN";
	private static final String STATUS_COMPLETED = "COMPLETED";
	private static final String PERMISSION_READ_ONLY = "READ_ONLY";
	private static final String PERMISSION_REMINDER = "REMINDER";
	private static final String PERMISSION_TASK = "TASK";

	private final FamilyInviteRepository familyInviteRepository;
	private final FamilyBindingRepository familyBindingRepository;
	private final FamilyTaskRepository familyTaskRepository;
	private final UserAccountRepository userAccountRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final HealthRuleEngineService healthRuleEngineService;
	private final ProactiveCareService proactiveCareService;
	private final IdGenerator idGenerator;

	public FamilyCareService(
			FamilyInviteRepository familyInviteRepository,
			FamilyBindingRepository familyBindingRepository,
			FamilyTaskRepository familyTaskRepository,
			UserAccountRepository userAccountRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			FlareRecordRepository flareRecordRepository,
			HealthRuleEngineService healthRuleEngineService,
			ProactiveCareService proactiveCareService,
			IdGenerator idGenerator
	) {
		this.familyInviteRepository = familyInviteRepository;
		this.familyBindingRepository = familyBindingRepository;
		this.familyTaskRepository = familyTaskRepository;
		this.userAccountRepository = userAccountRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.healthRuleEngineService = healthRuleEngineService;
		this.proactiveCareService = proactiveCareService;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public AppContracts.FamilyInviteResponse createInvite(String patientUserId, AppContracts.FamilyInviteCreateRequest request) {
		UserAccountEntity patient = requireAccount(patientUserId);
		Instant now = Instant.now();
		FamilyInviteEntity entity = new FamilyInviteEntity();
		entity.setInviteCode(idGenerator.next("invite"));
		entity.setCreatorUserCode(patientUserId);
		entity.setPatientUserCode(patientUserId);
		entity.setRelationType(request.relationType().trim());
		entity.setInviteMessage(StringUtils.hasText(request.inviteMessage()) ? request.inviteMessage().trim() : "邀请家属一起关注近期痛风风险。");
		entity.setStatus(STATUS_PENDING);
		entity.setCaregiverPermission(normalizeCaregiverPermission(request.caregiverPermission()));
		entity.setWeeklyReportEnabled(defaultBoolean(request.weeklyReportEnabled(), true));
		entity.setNotifyOnHighRisk(defaultBoolean(request.notifyOnHighRisk(), true));
		entity.setExpiresAt(now.plus(defaultDays(request.expiresInDays()), ChronoUnit.DAYS));
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		familyInviteRepository.save(entity);
		return toInviteResponse(entity, patient, null);
	}

	public List<AppContracts.FamilyInviteResponse> listInvites(String patientUserId) {
		UserAccountEntity patient = requireAccount(patientUserId);
		refreshExpiredInvites(patientUserId);
		return familyInviteRepository.findByPatientUserCodeOrderByCreatedAtDesc(patientUserId).stream()
				.map(item -> toInviteResponse(
						item,
						patient,
						StringUtils.hasText(item.getAcceptedByUserCode()) ? findAccount(item.getAcceptedByUserCode()).orElse(null) : null
				))
				.toList();
	}

	@Transactional
	public AppContracts.FamilyInviteResponse acceptInvite(String caregiverUserId, String inviteCode) {
		if (!StringUtils.hasText(inviteCode)) {
			throw new BusinessException("INVITE_CODE_REQUIRED", "邀请编码不能为空");
		}
		UserAccountEntity caregiver = requireAccount(caregiverUserId);
		FamilyInviteEntity invite = familyInviteRepository.findByInviteCode(inviteCode)
				.orElseThrow(() -> new BusinessException("INVITE_NOT_FOUND", "邀请不存在或已失效"));
		UserAccountEntity patient = requireAccount(invite.getPatientUserCode());
		if (Objects.equals(invite.getPatientUserCode(), caregiverUserId)) {
			throw new BusinessException("INVITE_SELF_ACCEPT", "不能接受自己发起的家属邀请");
		}
		if (invite.getExpiresAt().isBefore(Instant.now())) {
			invite.setStatus(STATUS_EXPIRED);
			invite.setUpdatedAt(Instant.now());
			familyInviteRepository.save(invite);
			throw new BusinessException("INVITE_EXPIRED", "该邀请已过期，请让患者重新发起绑定");
		}
		if (!STATUS_PENDING.equals(invite.getStatus())) {
			throw new BusinessException("INVITE_UNAVAILABLE", "该邀请当前不可接受");
		}
		if (familyBindingRepository.findByPatientUserCodeAndCaregiverUserCodeAndStatus(invite.getPatientUserCode(), caregiverUserId, STATUS_ACTIVE).isPresent()) {
			throw new BusinessException("BINDING_EXISTS", "该家属关系已经存在，无需重复接受");
		}
		Instant now = Instant.now();
		FamilyBindingEntity binding = new FamilyBindingEntity();
		binding.setBindingCode(idGenerator.next("binding"));
		binding.setPatientUserCode(invite.getPatientUserCode());
		binding.setCaregiverUserCode(caregiverUserId);
		binding.setRelationType(invite.getRelationType());
		binding.setStatus(STATUS_ACTIVE);
		binding.setCaregiverPermission(invite.getCaregiverPermission());
		binding.setWeeklyReportEnabled(invite.isWeeklyReportEnabled());
		binding.setNotifyOnHighRisk(invite.isNotifyOnHighRisk());
		binding.setSourceInviteCode(invite.getInviteCode());
		binding.setCreatedAt(now);
		binding.setUpdatedAt(now);
		familyBindingRepository.save(binding);

		invite.setAcceptedByUserCode(caregiverUserId);
		invite.setStatus(STATUS_ACCEPTED);
		invite.setUpdatedAt(now);
		familyInviteRepository.save(invite);
		return toInviteResponse(invite, patient, caregiver);
	}

	@Transactional
	public AppContracts.FamilyInviteResponse cancelInvite(String currentUserId, String inviteCode) {
		FamilyInviteEntity invite = familyInviteRepository.findByInviteCode(inviteCode)
				.orElseThrow(() -> new BusinessException("INVITE_NOT_FOUND", "邀请不存在或已失效"));
		if (!Objects.equals(invite.getCreatorUserCode(), currentUserId) && !Objects.equals(invite.getPatientUserCode(), currentUserId)) {
			throw new BusinessException("FORBIDDEN", "无权取消该邀请");
		}
		if (!STATUS_PENDING.equals(invite.getStatus())) {
			throw new BusinessException("INVITE_UNAVAILABLE", "当前邀请状态不可取消");
		}
		invite.setStatus(STATUS_CANCELLED);
		invite.setUpdatedAt(Instant.now());
		familyInviteRepository.save(invite);
		UserAccountEntity patient = requireAccount(invite.getPatientUserCode());
		return toInviteResponse(invite, patient, null);
	}

	public AppContracts.FamilyMembersResponse getMembers(String userId) {
		List<AppContracts.FamilyBindingMemberResponse> asPatient = familyBindingRepository.findByPatientUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
				.map(this::toBindingResponse)
				.toList();
		List<AppContracts.FamilyBindingMemberResponse> asCaregiver = familyBindingRepository.findByCaregiverUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(item -> STATUS_ACTIVE.equals(item.getStatus()))
				.map(this::toBindingResponse)
				.toList();
		return new AppContracts.FamilyMembersResponse(asPatient, asCaregiver);
	}

	@Transactional
	public AppContracts.FamilyBindingMemberResponse updateBindingPermissions(
			String currentUserId,
			String bindingCode,
			AppContracts.FamilyBindingPermissionUpdateRequest request
	) {
		FamilyBindingEntity binding = familyBindingRepository.findByBindingCode(bindingCode)
				.orElseThrow(() -> new BusinessException("BINDING_NOT_FOUND", "绑定关系不存在"));
		if (!Objects.equals(binding.getPatientUserCode(), currentUserId)) {
			throw new BusinessException("FORBIDDEN", "只有患者本人可以调整家属权限");
		}
		if (!STATUS_ACTIVE.equals(binding.getStatus())) {
			throw new BusinessException("BINDING_INACTIVE", "当前绑定关系不可调整权限");
		}
		binding.setCaregiverPermission(normalizeCaregiverPermission(request.caregiverPermission()));
		binding.setWeeklyReportEnabled(defaultBoolean(request.weeklyReportEnabled(), binding.isWeeklyReportEnabled()));
		binding.setNotifyOnHighRisk(defaultBoolean(request.notifyOnHighRisk(), binding.isNotifyOnHighRisk()));
		binding.setUpdatedAt(Instant.now());
		familyBindingRepository.save(binding);
		return toBindingResponse(binding);
	}

	@Transactional
	public AppContracts.FamilyBindingMemberResponse removeBinding(String currentUserId, String bindingCode) {
		FamilyBindingEntity binding = familyBindingRepository.findByBindingCode(bindingCode)
				.orElseThrow(() -> new BusinessException("BINDING_NOT_FOUND", "绑定关系不存在"));
		if (!Objects.equals(binding.getPatientUserCode(), currentUserId) && !Objects.equals(binding.getCaregiverUserCode(), currentUserId)) {
			throw new BusinessException("FORBIDDEN", "无权解绑该关系");
		}
		binding.setStatus(STATUS_UNBOUND);
		binding.setUpdatedAt(Instant.now());
		familyBindingRepository.save(binding);
		return toBindingResponse(binding);
	}

	public List<AppContracts.FamilyAlertResponse> getAlerts(String caregiverUserId) {
		List<AppContracts.FamilyAlertResponse> alerts = new ArrayList<>();
		for (FamilyBindingEntity binding : familyBindingRepository.findByCaregiverUserCodeOrderByCreatedAtDesc(caregiverUserId)) {
			if (!STATUS_ACTIVE.equals(binding.getStatus())) {
				continue;
			}
			if (!binding.isNotifyOnHighRisk() || !canReceiveAlerts(binding)) {
				continue;
			}
			UserAccountEntity patient = requireAccount(binding.getPatientUserCode());
			List<AppContracts.ReminderResponse> reminders = healthRuleEngineService.getActiveReminders(binding.getPatientUserCode()).stream()
					.filter(item -> item.riskLevel() != AppContracts.RiskLevel.GREEN)
					.limit(2)
					.toList();
			for (AppContracts.ReminderResponse reminder : reminders) {
				alerts.add(new AppContracts.FamilyAlertResponse(
						idGenerator.next("family-alert"),
						patient.getUserCode(),
						patient.getNickname(),
						binding.getRelationType(),
						reminder.riskLevel(),
						reminder.title(),
						reminder.content(),
						"REMINDER",
						reminder.triggerAt()
				));
			}
			AppContracts.ProactiveCareBriefResponse brief = proactiveCareService.getProactiveCareBrief(binding.getPatientUserCode());
			if (brief.overallRiskLevel() != AppContracts.RiskLevel.GREEN) {
				alerts.add(new AppContracts.FamilyAlertResponse(
						idGenerator.next("family-alert"),
						patient.getUserCode(),
						patient.getNickname(),
						binding.getRelationType(),
						brief.overallRiskLevel(),
						"主动管理风险提示",
						brief.summary(),
						"PROACTIVE_CARE",
						brief.generatedAt()
				));
			}
		}
		return alerts.stream()
				.sorted(Comparator
						.comparing((AppContracts.FamilyAlertResponse item) -> riskRank(item.riskLevel()))
						.reversed()
						.thenComparing(AppContracts.FamilyAlertResponse::generatedAt, Comparator.reverseOrder()))
				.toList();
	}

	public AppContracts.FamilyPatientSummaryResponse getPatientSummary(String caregiverUserId, String patientUserId) {
		FamilyBindingEntity binding = requireActiveBinding(patientUserId, caregiverUserId);
		UserAccountEntity patient = requireAccount(patientUserId);
		AppContracts.ProactiveCareBriefResponse brief = proactiveCareService.getProactiveCareBrief(patientUserId);
		List<AppContracts.ReminderResponse> reminders = healthRuleEngineService.getActiveReminders(patientUserId);
		Optional<UricAcidRecordEntity> latestUa = uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(patientUserId).stream().findFirst();
		Optional<FlareRecordEntity> latestFlare = flareRecordRepository.findByUserCodeOrderByStartedAtDesc(patientUserId).stream().findFirst();
		return new AppContracts.FamilyPatientSummaryResponse(
				patient.getUserCode(),
				patient.getNickname(),
				binding.getRelationType(),
				binding.getCaregiverPermission(),
				binding.isWeeklyReportEnabled(),
				brief.overallRiskLevel(),
				brief.summary(),
				brief.suggestions(),
				reminders,
				brief.weather(),
				latestFlare.map(FlareRecordEntity::getStartedAt).orElse(null),
				latestUa.map(UricAcidRecordEntity::getUaValue).orElse(null),
				latestUa.map(UricAcidRecordEntity::getUaUnit).orElse(null),
				brief.suggestions(),
				brief.generatedAt()
		);
	}

	public AppContracts.FamilyBindingMemberResponse requireSharedWeeklyReportAccess(String caregiverUserId, String patientUserId) {
		FamilyBindingEntity binding = requireActiveBinding(patientUserId, caregiverUserId);
		if (!binding.isWeeklyReportEnabled()) {
			throw new BusinessException("WEEKLY_REPORT_DISABLED", "当前患者尚未向该家属开放周报共享。");
		}
		return toBindingResponse(binding);
	}

	public AppContracts.FamilyTasksResponse getTasks(String userId) {
		List<AppContracts.FamilyTaskResponse> asPatient = familyTaskRepository.findByPatientUserCodeOrderByCreatedAtDesc(userId).stream()
				.map(this::toTaskResponse)
				.toList();
		List<AppContracts.FamilyTaskResponse> asCaregiver = familyTaskRepository.findByCaregiverUserCodeOrderByCreatedAtDesc(userId).stream()
				.filter(this::hasActiveBinding)
				.map(this::toTaskResponse)
				.toList();
		return new AppContracts.FamilyTasksResponse(asPatient, asCaregiver);
	}

	@Transactional
	public AppContracts.FamilyTaskResponse createTask(
			String currentUserId,
			String bindingCode,
			AppContracts.FamilyTaskCreateRequest request
	) {
		FamilyBindingEntity binding = familyBindingRepository.findByBindingCode(bindingCode)
				.orElseThrow(() -> new BusinessException("BINDING_NOT_FOUND", "绑定关系不存在"));
		if (!Objects.equals(binding.getPatientUserCode(), currentUserId)) {
			throw new BusinessException("FORBIDDEN", "只有患者本人可以发起家属代办");
		}
		requireActiveTaskBinding(binding);
		Instant now = Instant.now();
		FamilyTaskEntity task = new FamilyTaskEntity();
		task.setTaskCode(idGenerator.next("family-task"));
		task.setBindingCode(binding.getBindingCode());
		task.setPatientUserCode(binding.getPatientUserCode());
		task.setCaregiverUserCode(binding.getCaregiverUserCode());
		task.setRelationType(binding.getRelationType());
		task.setStatus(STATUS_OPEN);
		task.setTitle(request.title().trim());
		task.setDescription(trimToNull(request.description()));
		task.setCreatedByUserCode(currentUserId);
		task.setDueAt(request.dueAt());
		task.setCreatedAt(now);
		task.setUpdatedAt(now);
		familyTaskRepository.save(task);
		return toTaskResponse(task);
	}

	@Transactional
	public AppContracts.FamilyTaskResponse completeTask(
			String currentUserId,
			String taskCode,
			AppContracts.FamilyTaskCompleteRequest request
	) {
		FamilyTaskEntity task = familyTaskRepository.findByTaskCode(taskCode)
				.orElseThrow(() -> new BusinessException("FAMILY_TASK_NOT_FOUND", "家属代办不存在"));
		if (!Objects.equals(task.getCaregiverUserCode(), currentUserId)) {
			throw new BusinessException("FORBIDDEN", "只有被指派的家属可以确认完成");
		}
		FamilyBindingEntity binding = familyBindingRepository.findByBindingCode(task.getBindingCode())
				.orElseThrow(() -> new BusinessException("BINDING_NOT_FOUND", "绑定关系不存在"));
		requireActiveTaskBinding(binding);
		if (!STATUS_OPEN.equals(task.getStatus())) {
			throw new BusinessException("FAMILY_TASK_COMPLETED", "当前代办已处理，无需重复确认");
		}
		task.setStatus(STATUS_COMPLETED);
		task.setCompletedAt(Instant.now());
		task.setCompletionNote(trimToNull(request.completionNote()));
		task.setUpdatedAt(task.getCompletedAt());
		familyTaskRepository.save(task);
		return toTaskResponse(task);
	}

	private void refreshExpiredInvites(String patientUserId) {
		Instant now = Instant.now();
		List<FamilyInviteEntity> invites = familyInviteRepository.findByPatientUserCodeOrderByCreatedAtDesc(patientUserId);
		boolean changed = false;
		for (FamilyInviteEntity invite : invites) {
			if (STATUS_PENDING.equals(invite.getStatus()) && invite.getExpiresAt().isBefore(now)) {
				invite.setStatus(STATUS_EXPIRED);
				invite.setUpdatedAt(now);
				changed = true;
			}
		}
		if (changed) {
			familyInviteRepository.saveAll(invites);
		}
	}

	private AppContracts.FamilyInviteResponse toInviteResponse(
			FamilyInviteEntity invite,
			UserAccountEntity patient,
			UserAccountEntity acceptedBy
	) {
		return new AppContracts.FamilyInviteResponse(
				invite.getInviteCode(),
				patient.getUserCode(),
				patient.getNickname(),
				invite.getCreatorUserCode(),
				invite.getRelationType(),
				invite.getInviteMessage(),
				invite.getStatus(),
				invite.getCaregiverPermission(),
				invite.isWeeklyReportEnabled(),
				invite.isNotifyOnHighRisk(),
				invite.getAcceptedByUserCode(),
				acceptedBy == null ? null : acceptedBy.getNickname(),
				invite.getExpiresAt(),
				invite.getCreatedAt()
		);
	}

	private AppContracts.FamilyBindingMemberResponse toBindingResponse(FamilyBindingEntity binding) {
		UserAccountEntity patient = requireAccount(binding.getPatientUserCode());
		UserAccountEntity caregiver = requireAccount(binding.getCaregiverUserCode());
		return new AppContracts.FamilyBindingMemberResponse(
				binding.getBindingCode(),
				patient.getUserCode(),
				patient.getNickname(),
				caregiver.getUserCode(),
				caregiver.getNickname(),
				binding.getRelationType(),
				binding.getStatus(),
				binding.getCaregiverPermission(),
				binding.isWeeklyReportEnabled(),
				binding.isNotifyOnHighRisk(),
				binding.getCreatedAt()
		);
	}

	private AppContracts.FamilyTaskResponse toTaskResponse(FamilyTaskEntity task) {
		UserAccountEntity patient = requireAccount(task.getPatientUserCode());
		UserAccountEntity caregiver = requireAccount(task.getCaregiverUserCode());
		return new AppContracts.FamilyTaskResponse(
				task.getTaskCode(),
				task.getBindingCode(),
				patient.getUserCode(),
				patient.getNickname(),
				caregiver.getUserCode(),
				caregiver.getNickname(),
				task.getRelationType(),
				task.getStatus(),
				task.getTitle(),
				task.getDescription(),
				task.getDueAt(),
				task.getCreatedAt(),
				task.getCompletedAt(),
				task.getCompletionNote()
		);
	}

	private FamilyBindingEntity requireActiveBinding(String patientUserId, String caregiverUserId) {
		return familyBindingRepository.findByPatientUserCodeAndCaregiverUserCodeAndStatus(patientUserId, caregiverUserId, STATUS_ACTIVE)
				.orElseThrow(() -> new BusinessException("FORBIDDEN", "当前没有该患者的有效家属授权"));
	}

	private boolean hasActiveBinding(FamilyTaskEntity task) {
		return familyBindingRepository.findByBindingCode(task.getBindingCode())
				.map(item -> STATUS_ACTIVE.equals(item.getStatus()) && Objects.equals(item.getCaregiverUserCode(), task.getCaregiverUserCode()))
				.orElse(false);
	}

	private UserAccountEntity requireAccount(String userId) {
		return userAccountRepository.findByUserCode(userId)
				.orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
	}

	private Optional<UserAccountEntity> findAccount(String userId) {
		return userAccountRepository.findByUserCode(userId);
	}

	private int defaultDays(Integer expiresInDays) {
		return expiresInDays == null ? 7 : expiresInDays;
	}

	private boolean defaultBoolean(Boolean value, boolean defaultValue) {
		return value == null ? defaultValue : value;
	}

	private boolean canReceiveAlerts(FamilyBindingEntity binding) {
		return PERMISSION_REMINDER.equals(binding.getCaregiverPermission())
				|| PERMISSION_TASK.equals(binding.getCaregiverPermission());
	}

	private void requireActiveTaskBinding(FamilyBindingEntity binding) {
		if (!STATUS_ACTIVE.equals(binding.getStatus())) {
			throw new BusinessException("BINDING_INACTIVE", "当前绑定关系不可操作家属代办");
		}
		if (!PERMISSION_TASK.equals(binding.getCaregiverPermission())) {
			throw new BusinessException("FAMILY_TASK_PERMISSION_DENIED", "当前绑定关系尚未开放共同照护权限");
		}
	}

	private String normalizeCaregiverPermission(String value) {
		if (!StringUtils.hasText(value)) {
			return PERMISSION_REMINDER;
		}
		String normalized = value.trim().toUpperCase();
		if (PERMISSION_READ_ONLY.equals(normalized) || PERMISSION_REMINDER.equals(normalized) || PERMISSION_TASK.equals(normalized)) {
			return normalized;
		}
		throw new BusinessException("FAMILY_PERMISSION_INVALID", "不支持的家属权限，请使用 READ_ONLY / REMINDER / TASK。");
	}

	private int riskRank(AppContracts.RiskLevel riskLevel) {
		return switch (riskLevel) {
			case RED -> 3;
			case YELLOW -> 2;
			case GREEN -> 1;
		};
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
