package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HealthRecordAuditEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.WeightRecordEntity;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HealthRecordAuditRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.WeightRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class RecordCenterService {

	private static final TypeReference<List<AppContracts.HealthRecordAuditFieldResponse>> AUDIT_FIELD_LIST_TYPE =
			new TypeReference<>() {
			};

	private static final String TYPE_URIC_ACID = "URIC_ACID";
	private static final String TYPE_WEIGHT = "WEIGHT";
	private static final String TYPE_HYDRATION = "HYDRATION";
	private static final String TYPE_FLARE = "FLARE";
	private static final String ACTION_UPDATE = "UPDATE";
	private static final String ACTION_DELETE = "DELETE";
	private static final String ACTION_RESTORE = "RESTORE";

	private static final List<String> DEFAULT_RECORD_TYPES = List.of(
			TYPE_URIC_ACID,
			TYPE_WEIGHT,
			TYPE_HYDRATION,
			TYPE_FLARE
	);

	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final WeightRecordRepository weightRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final FlareRecordRepository flareRecordRepository;
	private final HealthRecordAuditRepository healthRecordAuditRepository;
	private final HealthRuleEngineService healthRuleEngineService;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;

	public RecordCenterService(
			UricAcidRecordRepository uricAcidRecordRepository,
			WeightRecordRepository weightRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			FlareRecordRepository flareRecordRepository,
			HealthRecordAuditRepository healthRecordAuditRepository,
			HealthRuleEngineService healthRuleEngineService,
			IdGenerator idGenerator,
			JsonCodec jsonCodec
	) {
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.weightRecordRepository = weightRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.flareRecordRepository = flareRecordRepository;
		this.healthRecordAuditRepository = healthRecordAuditRepository;
		this.healthRuleEngineService = healthRuleEngineService;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
	}

	public List<AppContracts.UricAcidRecordResponse> listUricAcidRecords(String userId) {
		return uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.map(record -> new AppContracts.UricAcidRecordResponse(
						record.getRecordCode(),
						record.getUaValue(),
						record.getUaUnit(),
						record.getMeasuredAt(),
						record.getSourceName(),
						record.getNoteText(),
						uricAcidRisk(record.getUaValue())
				))
				.toList();
	}

	public List<AppContracts.WeightRecordResponse> listWeightRecords(String userId) {
		return weightRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId).stream()
				.map(record -> new AppContracts.WeightRecordResponse(
						record.getRecordCode(),
						record.getWeightValue(),
						"kg",
						record.getMeasuredAt(),
						record.getSourceName(),
						record.getNoteText(),
						AppContracts.RiskLevel.GREEN
				))
				.toList();
	}

	public List<AppContracts.HydrationRecordResponse> listHydrationRecords(String userId) {
		return hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId).stream()
				.map(record -> new AppContracts.HydrationRecordResponse(
						record.getRecordCode(),
						record.getWaterIntakeMl(),
						record.getUrineColorLevel(),
						record.getCheckedAt(),
						record.getNoteText(),
						hydrationRisk(record.getUrineColorLevel())
				))
				.toList();
	}

	public List<AppContracts.FlareRecordResponse> listFlareRecords(String userId) {
		return flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId).stream()
				.map(record -> new AppContracts.FlareRecordResponse(
						record.getRecordCode(),
						record.getJointName(),
						record.getPainLevel(),
						record.getStartedAt(),
						record.getDurationNote(),
						record.getNoteText(),
						flareRisk(record.getPainLevel())
				))
				.toList();
	}

	public AppContracts.HealthRecordCenterResponse getRecordCenter(
			String userId,
			List<String> requestedTypes,
			int limit,
			String cursor
	) {
		List<String> types = normalizeRecordTypes(requestedTypes);
		Instant cursorInstant = parseCursor(cursor);
		List<AppContracts.HealthRecordCenterItemResponse> items = loadCenterItems(userId, types).stream()
				.filter(item -> cursorInstant == null || item.occurredAt().isBefore(cursorInstant))
				.sorted(Comparator.comparing(AppContracts.HealthRecordCenterItemResponse::occurredAt).reversed())
				.toList();
		boolean hasMore = items.size() > limit;
		List<AppContracts.HealthRecordCenterItemResponse> finalItems = items.stream()
				.limit(limit)
				.toList();
		String nextCursor = finalItems.isEmpty() || !hasMore
				? null
				: finalItems.getLast().occurredAt().toString();
		return new AppContracts.HealthRecordCenterResponse(
				types,
				items.size(),
				finalItems.size(),
				limit,
				finalItems,
				nextCursor,
				hasMore
		);
	}

	public AppContracts.HealthRecordDetailResponse getRecordDetail(String userId, String type, String recordId) {
		String normalizedType = normalizeRecordType(type);
		return switch (normalizedType) {
			case TYPE_URIC_ACID -> toUricAcidDetail(requireOwnedUricAcid(userId, recordId));
			case TYPE_WEIGHT -> toWeightDetail(requireOwnedWeight(userId, recordId));
			case TYPE_HYDRATION -> toHydrationDetail(requireOwnedHydration(userId, recordId));
			case TYPE_FLARE -> toFlareDetail(requireOwnedFlare(userId, recordId));
			default -> throw unsupportedType(normalizedType);
		};
	}

	@Transactional
	public AppContracts.HealthRecordDetailResponse updateRecord(
			String userId,
			String type,
			String recordId,
			AppContracts.HealthRecordUpdateRequest request
	) {
		String normalizedType = normalizeRecordType(type);
		String changeReason = requireChangeReason(request.changeReason());
		AppContracts.HealthRecordDetailResponse beforeDetail = getRecordDetail(userId, normalizedType, recordId);

		switch (normalizedType) {
			case TYPE_URIC_ACID -> updateUricAcidRecord(requireOwnedUricAcid(userId, recordId), request);
			case TYPE_WEIGHT -> updateWeightRecord(requireOwnedWeight(userId, recordId), request);
			case TYPE_HYDRATION -> updateHydrationRecord(requireOwnedHydration(userId, recordId), request);
			case TYPE_FLARE -> updateFlareRecord(requireOwnedFlare(userId, recordId), request);
			default -> throw unsupportedType(normalizedType);
		}

		AppContracts.HealthRecordDetailResponse afterDetail = getRecordDetail(userId, normalizedType, recordId);
		List<AppContracts.HealthRecordAuditFieldResponse> diffFields = buildAuditDiff(beforeDetail, afterDetail);
		if (diffFields.isEmpty()) {
			throw new BusinessException("RECORD_UPDATE_NO_CHANGES", "记录内容没有实际变化");
		}

		saveAudit(
				userId,
				recordId,
				normalizedType,
				ACTION_UPDATE,
				changeReason,
				"已更正 " + normalizedType + " 记录",
				diffFields,
				beforeDetail,
				afterDetail
		);
		refreshInsightState(userId);
		return afterDetail;
	}

	public List<AppContracts.HealthRecordAuditResponse> listRecordAudits(
			String userId,
			String type,
			String recordId,
			int limit
	) {
		String normalizedType = normalizeRecordType(type);
		int finalLimit = Math.min(Math.max(limit, 1), 50);
		return healthRecordAuditRepository.findByUserCodeAndRecordTypeAndRecordIdOrderByOperatedAtDesc(
						userId,
						normalizedType,
						recordId
				).stream()
				.limit(finalLimit)
				.map(this::toAuditResponse)
				.toList();
	}

	@Transactional
	public AppContracts.HealthRecordRestoreResponse restoreRecord(
			String userId,
			String type,
			String recordId,
			String auditId,
			AppContracts.HealthRecordRestoreRequest request
	) {
		String normalizedType = normalizeRecordType(type);
		String changeReason = requireRestoreReason(request.changeReason());
		HealthRecordAuditEntity auditEntity = requireOwnedAudit(userId, normalizedType, recordId, auditId);
		AppContracts.HealthRecordDetailResponse snapshot = readRestoreSnapshot(auditEntity);
		AppContracts.HealthRecordDetailResponse beforeDetail = findRecordDetailIfExists(userId, normalizedType, recordId).orElse(null);

		applySnapshot(userId, normalizedType, recordId, snapshot);

		AppContracts.HealthRecordDetailResponse afterDetail = getRecordDetail(userId, normalizedType, recordId);
		List<AppContracts.HealthRecordAuditFieldResponse> diffFields = beforeDetail == null
				? buildRestoreCreateDiff(afterDetail)
				: buildAuditDiff(beforeDetail, afterDetail);
		if (diffFields.isEmpty()) {
			throw new BusinessException("RECORD_RESTORE_NO_CHANGES", "恢复后记录没有变化");
		}

		saveAudit(
				userId,
				recordId,
				normalizedType,
				ACTION_RESTORE,
				changeReason,
				"已恢复 " + normalizedType + " 记录",
				diffFields,
				beforeDetail,
				afterDetail
		);
		refreshInsightState(userId);
		return new AppContracts.HealthRecordRestoreResponse(
				recordId,
				normalizedType,
				auditId,
				"RESTORED",
				Instant.now(),
				"记录已恢复",
				afterDetail
		);
	}

	@Transactional
	public AppContracts.HealthRecordDeleteResponse deleteRecord(String userId, String type, String recordId) {
		String normalizedType = normalizeRecordType(type);
		AppContracts.HealthRecordDetailResponse beforeDetail = getRecordDetail(userId, normalizedType, recordId);
		List<AppContracts.HealthRecordAuditFieldResponse> diffFields = buildDeleteAuditDiff(beforeDetail);

		switch (normalizedType) {
			case TYPE_URIC_ACID -> uricAcidRecordRepository.delete(requireOwnedUricAcid(userId, recordId));
			case TYPE_WEIGHT -> weightRecordRepository.delete(requireOwnedWeight(userId, recordId));
			case TYPE_HYDRATION -> hydrationRecordRepository.delete(requireOwnedHydration(userId, recordId));
			case TYPE_FLARE -> flareRecordRepository.delete(requireOwnedFlare(userId, recordId));
			default -> throw unsupportedType(normalizedType);
		}

		saveAudit(
				userId,
				recordId,
				normalizedType,
				ACTION_DELETE,
				"用户删除记录",
				"已删除 " + normalizedType + " 记录",
				diffFields,
				beforeDetail,
				null
		);
		refreshInsightState(userId);
		return new AppContracts.HealthRecordDeleteResponse(
				recordId,
				normalizedType,
				"DELETED",
				Instant.now(),
				"记录已删除"
		);
	}

	private List<AppContracts.HealthRecordCenterItemResponse> loadCenterItems(String userId, List<String> types) {
		List<AppContracts.HealthRecordCenterItemResponse> items = new ArrayList<>();
		if (types.contains(TYPE_URIC_ACID)) {
			uricAcidRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId)
					.forEach(record -> items.add(toUricAcidCenterItem(record)));
		}
		if (types.contains(TYPE_WEIGHT)) {
			weightRecordRepository.findByUserCodeOrderByMeasuredAtDesc(userId)
					.forEach(record -> items.add(toWeightCenterItem(record)));
		}
		if (types.contains(TYPE_HYDRATION)) {
			hydrationRecordRepository.findByUserCodeOrderByCheckedAtDesc(userId)
					.forEach(record -> items.add(toHydrationCenterItem(record)));
		}
		if (types.contains(TYPE_FLARE)) {
			flareRecordRepository.findByUserCodeOrderByStartedAtDesc(userId)
					.forEach(record -> items.add(toFlareCenterItem(record)));
		}
		return items;
	}

	private List<String> normalizeRecordTypes(List<String> requestedTypes) {
		if (requestedTypes == null || requestedTypes.isEmpty()) {
			return DEFAULT_RECORD_TYPES;
		}
		List<String> normalizedTypes = requestedTypes.stream()
				.map(this::normalizeRecordType)
				.distinct()
				.toList();
		return normalizedTypes.isEmpty() ? DEFAULT_RECORD_TYPES : normalizedTypes;
	}

	private String normalizeRecordType(String type) {
		String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
		if (!DEFAULT_RECORD_TYPES.contains(normalizedType)) {
			throw unsupportedType(normalizedType);
		}
		return normalizedType;
	}

	private Instant parseCursor(String cursor) {
		if (!StringUtils.hasText(cursor)) {
			return null;
		}
		try {
			return Instant.parse(cursor.trim());
		} catch (Exception ex) {
			throw new BusinessException("RECORD_CURSOR_INVALID", "记录中心游标格式不正确");
		}
	}

	private BusinessException unsupportedType(String type) {
		return new BusinessException("RECORD_TYPE_UNSUPPORTED", "统一记录中心暂不支持该记录类型: " + type);
	}

	private AppContracts.HealthRecordCenterItemResponse toUricAcidCenterItem(UricAcidRecordEntity record) {
		return new AppContracts.HealthRecordCenterItemResponse(
				record.getRecordCode(),
				TYPE_URIC_ACID,
				"尿酸记录",
				record.getUaValue() + " " + record.getUaUnit(),
				record.getMeasuredAt(),
				uricAcidRisk(record.getUaValue()),
				record.getSourceName(),
				List.of(record.getUaUnit())
		);
	}

	private AppContracts.HealthRecordCenterItemResponse toWeightCenterItem(WeightRecordEntity record) {
		List<String> tags = new ArrayList<>();
		tags.add("kg");
		if (StringUtils.hasText(record.getSourceName())) {
			tags.add("来源已记录");
		}
		return new AppContracts.HealthRecordCenterItemResponse(
				record.getRecordCode(),
				TYPE_WEIGHT,
				"体重记录",
				formatDecimal(record.getWeightValue()) + " kg",
				record.getMeasuredAt(),
				AppContracts.RiskLevel.GREEN,
				record.getSourceName(),
				tags
		);
	}

	private AppContracts.HealthRecordCenterItemResponse toHydrationCenterItem(HydrationRecordEntity record) {
		return new AppContracts.HealthRecordCenterItemResponse(
				record.getRecordCode(),
				TYPE_HYDRATION,
				"补水记录",
				record.getWaterIntakeMl() + "ml / 尿液颜色等级 " + record.getUrineColorLevel(),
				record.getCheckedAt(),
				hydrationRisk(record.getUrineColorLevel()),
				null,
				List.of("补水", "尿液颜色")
		);
	}

	private AppContracts.HealthRecordCenterItemResponse toFlareCenterItem(FlareRecordEntity record) {
		return new AppContracts.HealthRecordCenterItemResponse(
				record.getRecordCode(),
				TYPE_FLARE,
				"发作记录",
				record.getJointName() + " 疼痛等级 " + record.getPainLevel(),
				record.getStartedAt(),
				flareRisk(record.getPainLevel()),
				null,
				List.of(record.getJointName())
		);
	}

	private AppContracts.HealthRecordDetailResponse toUricAcidDetail(UricAcidRecordEntity record) {
		return new AppContracts.HealthRecordDetailResponse(
				record.getRecordCode(),
				TYPE_URIC_ACID,
				"尿酸记录详情",
				record.getUaValue() + " " + record.getUaUnit(),
				record.getMeasuredAt(),
				uricAcidRisk(record.getUaValue()),
				record.getSourceName(),
				record.getNoteText(),
				List.of(record.getUaUnit()),
				List.of(
						new AppContracts.HealthRecordDetailFieldResponse("value", "尿酸值", String.valueOf(record.getUaValue())),
						new AppContracts.HealthRecordDetailFieldResponse("unit", "单位", record.getUaUnit()),
						new AppContracts.HealthRecordDetailFieldResponse("measuredAt", "记录时间", record.getMeasuredAt().toString())
				)
		);
	}

	private AppContracts.HealthRecordDetailResponse toWeightDetail(WeightRecordEntity record) {
		return new AppContracts.HealthRecordDetailResponse(
				record.getRecordCode(),
				TYPE_WEIGHT,
				"体重记录详情",
				formatDecimal(record.getWeightValue()) + " kg",
				record.getMeasuredAt(),
				AppContracts.RiskLevel.GREEN,
				record.getSourceName(),
				record.getNoteText(),
				List.of("kg"),
				List.of(
						new AppContracts.HealthRecordDetailFieldResponse("value", "体重", formatDecimal(record.getWeightValue())),
						new AppContracts.HealthRecordDetailFieldResponse("unit", "单位", "kg"),
						new AppContracts.HealthRecordDetailFieldResponse("measuredAt", "记录时间", record.getMeasuredAt().toString())
				)
		);
	}

	private AppContracts.HealthRecordDetailResponse toHydrationDetail(HydrationRecordEntity record) {
		return new AppContracts.HealthRecordDetailResponse(
				record.getRecordCode(),
				TYPE_HYDRATION,
				"补水记录详情",
				record.getWaterIntakeMl() + "ml / 尿液颜色等级 " + record.getUrineColorLevel(),
				record.getCheckedAt(),
				hydrationRisk(record.getUrineColorLevel()),
				null,
				record.getNoteText(),
				List.of("补水", "尿液颜色"),
				List.of(
						new AppContracts.HealthRecordDetailFieldResponse("waterIntakeMl", "饮水量", record.getWaterIntakeMl() + "ml"),
						new AppContracts.HealthRecordDetailFieldResponse("urineColorLevel", "尿液颜色等级", String.valueOf(record.getUrineColorLevel())),
						new AppContracts.HealthRecordDetailFieldResponse("checkedAt", "记录时间", record.getCheckedAt().toString())
				)
		);
	}

	private AppContracts.HealthRecordDetailResponse toFlareDetail(FlareRecordEntity record) {
		List<AppContracts.HealthRecordDetailFieldResponse> fields = new ArrayList<>();
		fields.add(new AppContracts.HealthRecordDetailFieldResponse("joint", "发作部位", record.getJointName()));
		fields.add(new AppContracts.HealthRecordDetailFieldResponse("painLevel", "疼痛等级", String.valueOf(record.getPainLevel())));
		fields.add(new AppContracts.HealthRecordDetailFieldResponse("startedAt", "开始时间", record.getStartedAt().toString()));
		if (StringUtils.hasText(record.getDurationNote())) {
			fields.add(new AppContracts.HealthRecordDetailFieldResponse("durationNote", "持续时长", record.getDurationNote()));
		}
		return new AppContracts.HealthRecordDetailResponse(
				record.getRecordCode(),
				TYPE_FLARE,
				"发作记录详情",
				record.getJointName() + " 疼痛等级 " + record.getPainLevel(),
				record.getStartedAt(),
				flareRisk(record.getPainLevel()),
				null,
				record.getNoteText(),
				List.of(record.getJointName()),
				fields
		);
	}

	private void updateUricAcidRecord(UricAcidRecordEntity entity, AppContracts.HealthRecordUpdateRequest request) {
		if (request.value() != null) {
			validatePositiveInteger(request.value(), "尿酸值必须大于0");
			entity.setUaValue(request.value());
		}
		if (request.unit() != null) {
			entity.setUaUnit(normalizeRequiredText(request.unit(), "尿酸单位不能为空"));
		}
		if (request.measuredAt() != null) {
			entity.setMeasuredAt(request.measuredAt());
		}
		if (request.source() != null) {
			entity.setSourceName(normalizeNullableText(request.source()));
		}
		if (request.note() != null) {
			entity.setNoteText(normalizeNullableText(request.note()));
		}
		uricAcidRecordRepository.save(entity);
	}

	private void updateWeightRecord(WeightRecordEntity entity, AppContracts.HealthRecordUpdateRequest request) {
		if (request.decimalValue() != null) {
			validatePositiveDecimal(request.decimalValue(), "体重必须大于0");
			entity.setWeightValue(request.decimalValue());
		}
		if (request.measuredAt() != null) {
			entity.setMeasuredAt(request.measuredAt());
		}
		if (request.source() != null) {
			entity.setSourceName(normalizeNullableText(request.source()));
		}
		if (request.note() != null) {
			entity.setNoteText(normalizeNullableText(request.note()));
		}
		weightRecordRepository.save(entity);
	}

	private void updateHydrationRecord(HydrationRecordEntity entity, AppContracts.HealthRecordUpdateRequest request) {
		if (request.waterIntakeMl() != null) {
			if (request.waterIntakeMl() < 0) {
				throw new BusinessException("RECORD_UPDATE_INVALID", "饮水量不能小于0");
			}
			entity.setWaterIntakeMl(request.waterIntakeMl());
		}
		if (request.urineColorLevel() != null) {
			validateRange(request.urineColorLevel(), 1, 5, "尿液颜色等级必须在 1-5 之间");
			entity.setUrineColorLevel(request.urineColorLevel());
		}
		if (request.checkedAt() != null) {
			entity.setCheckedAt(request.checkedAt());
		}
		if (request.note() != null) {
			entity.setNoteText(normalizeNullableText(request.note()));
		}
		hydrationRecordRepository.save(entity);
	}

	private void updateFlareRecord(FlareRecordEntity entity, AppContracts.HealthRecordUpdateRequest request) {
		if (request.joint() != null) {
			entity.setJointName(normalizeRequiredText(request.joint(), "发作部位不能为空"));
		}
		if (request.painLevel() != null) {
			validateRange(request.painLevel(), 1, 10, "疼痛等级必须在 1-10 之间");
			entity.setPainLevel(request.painLevel());
		}
		if (request.startedAt() != null) {
			entity.setStartedAt(request.startedAt());
		}
		if (request.durationNote() != null) {
			entity.setDurationNote(normalizeNullableText(request.durationNote()));
		}
		if (request.note() != null) {
			entity.setNoteText(normalizeNullableText(request.note()));
		}
		flareRecordRepository.save(entity);
	}

	private void applySnapshot(
			String userId,
			String type,
			String recordId,
			AppContracts.HealthRecordDetailResponse snapshot
	) {
		switch (type) {
			case TYPE_URIC_ACID -> applyUricAcidSnapshot(userId, recordId, snapshot);
			case TYPE_WEIGHT -> applyWeightSnapshot(userId, recordId, snapshot);
			case TYPE_HYDRATION -> applyHydrationSnapshot(userId, recordId, snapshot);
			case TYPE_FLARE -> applyFlareSnapshot(userId, recordId, snapshot);
			default -> throw unsupportedType(type);
		}
	}

	private void applyUricAcidSnapshot(String userId, String recordId, AppContracts.HealthRecordDetailResponse snapshot) {
		UricAcidRecordEntity entity = uricAcidRecordRepository.findByRecordCode(recordId)
				.orElseGet(UricAcidRecordEntity::new);
		entity.setRecordCode(recordId);
		entity.setUserCode(userId);
		entity.setUaValue(parseRequiredIntegerField(snapshot, "value", "尿酸值快照缺失"));
		entity.setUaUnit(parseRequiredField(snapshot, "unit", "尿酸单位快照缺失"));
		entity.setMeasuredAt(snapshot.occurredAt());
		entity.setSourceName(snapshot.source());
		entity.setNoteText(snapshot.note());
		uricAcidRecordRepository.save(entity);
	}

	private void applyWeightSnapshot(String userId, String recordId, AppContracts.HealthRecordDetailResponse snapshot) {
		WeightRecordEntity entity = weightRecordRepository.findByRecordCode(recordId)
				.orElseGet(WeightRecordEntity::new);
		entity.setRecordCode(recordId);
		entity.setUserCode(userId);
		entity.setWeightValue(parseRequiredDecimalField(snapshot, "value", "体重快照缺失"));
		entity.setMeasuredAt(snapshot.occurredAt());
		entity.setSourceName(snapshot.source());
		entity.setNoteText(snapshot.note());
		weightRecordRepository.save(entity);
	}

	private void applyHydrationSnapshot(String userId, String recordId, AppContracts.HealthRecordDetailResponse snapshot) {
		HydrationRecordEntity entity = hydrationRecordRepository.findByRecordCode(recordId)
				.orElseGet(HydrationRecordEntity::new);
		entity.setRecordCode(recordId);
		entity.setUserCode(userId);
		entity.setWaterIntakeMl(parseIntegerWithSuffix(snapshot, "waterIntakeMl", "ml", "饮水量快照缺失"));
		entity.setUrineColorLevel(parseRequiredIntegerField(snapshot, "urineColorLevel", "尿液颜色等级快照缺失"));
		entity.setCheckedAt(snapshot.occurredAt());
		entity.setNoteText(snapshot.note());
		hydrationRecordRepository.save(entity);
	}

	private void applyFlareSnapshot(String userId, String recordId, AppContracts.HealthRecordDetailResponse snapshot) {
		FlareRecordEntity entity = flareRecordRepository.findByRecordCode(recordId)
				.orElseGet(FlareRecordEntity::new);
		entity.setRecordCode(recordId);
		entity.setUserCode(userId);
		entity.setJointName(parseRequiredField(snapshot, "joint", "发作部位快照缺失"));
		entity.setPainLevel(parseRequiredIntegerField(snapshot, "painLevel", "疼痛等级快照缺失"));
		entity.setStartedAt(snapshot.occurredAt());
		entity.setDurationNote(parseOptionalField(snapshot, "durationNote"));
		entity.setNoteText(snapshot.note());
		flareRecordRepository.save(entity);
	}

	private UricAcidRecordEntity requireOwnedUricAcid(String userId, String recordId) {
		return requireOwned(
				uricAcidRecordRepository.findByRecordCode(recordId),
				userId,
				recordId,
				UricAcidRecordEntity::getUserCode
		);
	}

	private WeightRecordEntity requireOwnedWeight(String userId, String recordId) {
		return requireOwned(
				weightRecordRepository.findByRecordCode(recordId),
				userId,
				recordId,
				WeightRecordEntity::getUserCode
		);
	}

	private HydrationRecordEntity requireOwnedHydration(String userId, String recordId) {
		return requireOwned(
				hydrationRecordRepository.findByRecordCode(recordId),
				userId,
				recordId,
				HydrationRecordEntity::getUserCode
		);
	}

	private FlareRecordEntity requireOwnedFlare(String userId, String recordId) {
		return requireOwned(
				flareRecordRepository.findByRecordCode(recordId),
				userId,
				recordId,
				FlareRecordEntity::getUserCode
		);
	}

	private <T> T requireOwned(Optional<T> record, String userId, String recordId, UserCodeAccessor<T> accessor) {
		T entity = record.orElseThrow(() -> new BusinessException("RECORD_NOT_FOUND", "记录不存在: " + recordId));
		if (!Objects.equals(accessor.userCode(entity), userId)) {
			throw new BusinessException("FORBIDDEN", "无权访问该记录");
		}
		return entity;
	}

	private HealthRecordAuditEntity requireOwnedAudit(String userId, String type, String recordId, String auditId) {
		HealthRecordAuditEntity entity = healthRecordAuditRepository.findByAuditCode(auditId)
				.orElseThrow(() -> new BusinessException("RECORD_AUDIT_NOT_FOUND", "审计记录不存在: " + auditId));
		if (!Objects.equals(entity.getUserCode(), userId)
				|| !Objects.equals(entity.getRecordType(), type)
				|| !Objects.equals(entity.getRecordId(), recordId)) {
			throw new BusinessException("FORBIDDEN", "无权访问该审计记录");
		}
		return entity;
	}

	private AppContracts.HealthRecordDetailResponse readRestoreSnapshot(HealthRecordAuditEntity auditEntity) {
		if (!StringUtils.hasText(auditEntity.getBeforeSnapshotJson())) {
			throw new BusinessException("RECORD_RESTORE_UNSUPPORTED", "该审计记录不支持恢复");
		}
		AppContracts.HealthRecordDetailResponse snapshot = jsonCodec.fromJson(
				auditEntity.getBeforeSnapshotJson(),
				AppContracts.HealthRecordDetailResponse.class
		);
		if (!Objects.equals(snapshot.recordId(), auditEntity.getRecordId())
				|| !Objects.equals(snapshot.type(), auditEntity.getRecordType())) {
			throw new BusinessException("RECORD_RESTORE_UNSUPPORTED", "审计快照与目标记录不匹配");
		}
		return snapshot;
	}

	private Optional<AppContracts.HealthRecordDetailResponse> findRecordDetailIfExists(String userId, String type, String recordId) {
		try {
			return Optional.of(getRecordDetail(userId, type, recordId));
		} catch (BusinessException ex) {
			if ("RECORD_NOT_FOUND".equals(ex.getCode())) {
				return Optional.empty();
			}
			throw ex;
		}
	}

	private List<AppContracts.HealthRecordAuditFieldResponse> buildAuditDiff(
			AppContracts.HealthRecordDetailResponse before,
			AppContracts.HealthRecordDetailResponse after
	) {
		Map<String, AuditField> beforeFields = toAuditFieldMap(before);
		Map<String, AuditField> afterFields = toAuditFieldMap(after);
		LinkedHashSet<String> keys = new LinkedHashSet<>();
		keys.addAll(beforeFields.keySet());
		keys.addAll(afterFields.keySet());

		List<AppContracts.HealthRecordAuditFieldResponse> diffFields = new ArrayList<>();
		for (String key : keys) {
			AuditField beforeField = beforeFields.get(key);
			AuditField afterField = afterFields.get(key);
			String beforeValue = beforeField == null ? null : beforeField.value();
			String afterValue = afterField == null ? null : afterField.value();
			if (!Objects.equals(valueOrNull(beforeValue), valueOrNull(afterValue))) {
				String label = beforeField != null ? beforeField.label() : afterField.label();
				diffFields.add(new AppContracts.HealthRecordAuditFieldResponse(key, label, beforeValue, afterValue));
			}
		}
		return diffFields;
	}

	private List<AppContracts.HealthRecordAuditFieldResponse> buildDeleteAuditDiff(
			AppContracts.HealthRecordDetailResponse beforeDetail
	) {
		Map<String, AuditField> beforeFields = toAuditFieldMap(beforeDetail);
		List<AppContracts.HealthRecordAuditFieldResponse> diffFields = new ArrayList<>();
		for (Map.Entry<String, AuditField> entry : beforeFields.entrySet()) {
			diffFields.add(new AppContracts.HealthRecordAuditFieldResponse(
					entry.getKey(),
					entry.getValue().label(),
					entry.getValue().value(),
					null
			));
		}
		return diffFields;
	}

	private Map<String, AuditField> toAuditFieldMap(AppContracts.HealthRecordDetailResponse detail) {
		Map<String, AuditField> fields = new LinkedHashMap<>();
		for (AppContracts.HealthRecordDetailFieldResponse field : detail.fields()) {
			fields.put(field.key(), new AuditField(field.label(), field.value()));
		}
		fields.put("source", new AuditField("来源", detail.source()));
		fields.put("note", new AuditField("备注", detail.note()));
		return fields;
	}

	private void saveAudit(
			String userId,
			String recordId,
			String type,
			String action,
			String changeReason,
			String summary,
			List<AppContracts.HealthRecordAuditFieldResponse> diffFields,
			AppContracts.HealthRecordDetailResponse beforeDetail,
			AppContracts.HealthRecordDetailResponse afterDetail
	) {
		HealthRecordAuditEntity entity = new HealthRecordAuditEntity();
		entity.setAuditCode(idGenerator.next("audit"));
		entity.setUserCode(userId);
		entity.setRecordId(recordId);
		entity.setRecordType(type);
		entity.setActionType(action);
		entity.setChangeReason(changeReason);
		entity.setSummaryText(summary);
		entity.setFieldDiffsJson(jsonCodec.toJson(diffFields));
		entity.setBeforeSnapshotJson(beforeDetail == null ? null : jsonCodec.toJson(beforeDetail));
		entity.setAfterSnapshotJson(afterDetail == null ? null : jsonCodec.toJson(afterDetail));
		entity.setOperatedAt(Instant.now());
		healthRecordAuditRepository.save(entity);
	}

	private AppContracts.HealthRecordAuditResponse toAuditResponse(HealthRecordAuditEntity entity) {
		List<AppContracts.HealthRecordAuditFieldResponse> fields = StringUtils.hasText(entity.getFieldDiffsJson())
				? jsonCodec.fromJson(entity.getFieldDiffsJson(), AUDIT_FIELD_LIST_TYPE)
				: List.of();
		return new AppContracts.HealthRecordAuditResponse(
				entity.getAuditCode(),
				entity.getRecordId(),
				entity.getRecordType(),
				entity.getActionType(),
				entity.getChangeReason(),
				entity.getSummaryText(),
				entity.getOperatedAt(),
				fields
		);
	}

	private List<AppContracts.HealthRecordAuditFieldResponse> buildRestoreCreateDiff(
			AppContracts.HealthRecordDetailResponse afterDetail
	) {
		List<AppContracts.HealthRecordAuditFieldResponse> diffFields = new ArrayList<>();
		for (Map.Entry<String, AuditField> entry : toAuditFieldMap(afterDetail).entrySet()) {
			diffFields.add(new AppContracts.HealthRecordAuditFieldResponse(
					entry.getKey(),
					entry.getValue().label(),
					null,
					entry.getValue().value()
			));
		}
		return diffFields;
	}

	private String parseRequiredField(
			AppContracts.HealthRecordDetailResponse snapshot,
			String key,
			String message
	) {
		return snapshot.fields().stream()
				.filter(field -> Objects.equals(field.key(), key))
				.map(AppContracts.HealthRecordDetailFieldResponse::value)
				.findFirst()
				.filter(StringUtils::hasText)
				.orElseThrow(() -> new BusinessException("RECORD_RESTORE_UNSUPPORTED", message));
	}

	private String parseOptionalField(AppContracts.HealthRecordDetailResponse snapshot, String key) {
		return snapshot.fields().stream()
				.filter(field -> Objects.equals(field.key(), key))
				.map(AppContracts.HealthRecordDetailFieldResponse::value)
				.findFirst()
				.orElse(null);
	}

	private Integer parseRequiredIntegerField(
			AppContracts.HealthRecordDetailResponse snapshot,
			String key,
			String message
	) {
		String value = parseRequiredField(snapshot, key, message);
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			throw new BusinessException("RECORD_RESTORE_UNSUPPORTED", message);
		}
	}

	private Integer parseIntegerWithSuffix(
			AppContracts.HealthRecordDetailResponse snapshot,
			String key,
			String suffix,
			String message
	) {
		String value = parseRequiredField(snapshot, key, message);
		String normalized = value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
		try {
			return Integer.parseInt(normalized.trim());
		} catch (NumberFormatException ex) {
			throw new BusinessException("RECORD_RESTORE_UNSUPPORTED", message);
		}
	}

	private BigDecimal parseRequiredDecimalField(
			AppContracts.HealthRecordDetailResponse snapshot,
			String key,
			String message
	) {
		String value = parseRequiredField(snapshot, key, message);
		try {
			return new BigDecimal(value.trim());
		} catch (NumberFormatException ex) {
			throw new BusinessException("RECORD_RESTORE_UNSUPPORTED", message);
		}
	}

	private String requireChangeReason(String changeReason) {
		if (!StringUtils.hasText(changeReason)) {
			throw new BusinessException("RECORD_UPDATE_INVALID", "更正原因不能为空");
		}
		return changeReason.trim();
	}

	private String requireRestoreReason(String changeReason) {
		if (!StringUtils.hasText(changeReason)) {
			throw new BusinessException("RECORD_RESTORE_INVALID", "恢复原因不能为空");
		}
		return changeReason.trim();
	}

	private String normalizeRequiredText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException("RECORD_UPDATE_INVALID", message);
		}
		return value.trim();
	}

	private String normalizeNullableText(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String valueOrNull(String value) {
		return StringUtils.hasText(value) ? value : null;
	}

	private void validatePositiveInteger(Integer value, String message) {
		if (value == null || value <= 0) {
			throw new BusinessException("RECORD_UPDATE_INVALID", message);
		}
	}

	private void validatePositiveDecimal(BigDecimal value, String message) {
		if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("RECORD_UPDATE_INVALID", message);
		}
	}

	private void validateRange(Integer value, int min, int max, String message) {
		if (value == null || value < min || value > max) {
			throw new BusinessException("RECORD_UPDATE_INVALID", message);
		}
	}

	private void refreshInsightState(String userId) {
		healthRuleEngineService.refreshDailySummary(userId, LocalDate.now());
		healthRuleEngineService.rebuildActiveReminders(userId);
	}

	private String formatDecimal(BigDecimal value) {
		return value == null ? "0" : value.stripTrailingZeros().toPlainString();
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

	private AppContracts.RiskLevel hydrationRisk(Integer urineColorLevel) {
		if (urineColorLevel == null) {
			return AppContracts.RiskLevel.GREEN;
		}
		return urineColorLevel >= 4 ? AppContracts.RiskLevel.YELLOW : AppContracts.RiskLevel.GREEN;
	}

	private AppContracts.RiskLevel flareRisk(Integer painLevel) {
		if (painLevel == null) {
			return AppContracts.RiskLevel.GREEN;
		}
		return painLevel >= 8 ? AppContracts.RiskLevel.RED : AppContracts.RiskLevel.YELLOW;
	}

	@FunctionalInterface
	private interface UserCodeAccessor<T> {
		String userCode(T value);
	}

	private record AuditField(String label, String value) {
	}

}
