package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.BloodPressureRecordEntity;
import com.tongfeng.backend.app.persistence.entity.DeviceBindingEntity;
import com.tongfeng.backend.app.persistence.entity.DeviceSyncEventEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.entity.WeightRecordEntity;
import com.tongfeng.backend.app.persistence.repo.BloodPressureRecordRepository;
import com.tongfeng.backend.app.persistence.repo.DeviceBindingRepository;
import com.tongfeng.backend.app.persistence.repo.DeviceSyncEventRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import com.tongfeng.backend.app.persistence.repo.WeightRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class DeviceIntegrationService {

	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final String STATUS_UNBOUND = "UNBOUND";
	private static final String SYNC_SUCCESS = "SUCCESS";
	private static final String SYNC_DUPLICATE = "DUPLICATE";
	private static final String DEVICE_HEALTH_ACTIVE = "ACTIVE";
	private static final String DEVICE_HEALTH_NEVER_SYNCED = "NEVER_SYNCED";
	private static final String DEVICE_HEALTH_STALE = "STALE";
	private static final String DEVICE_HEALTH_UNBOUND = "UNBOUND";
	private static final List<String> ALL_METRIC_TYPES = List.of("URIC_ACID", "HYDRATION", "WEIGHT", "BLOOD_PRESSURE");
	private static final List<DeviceProfile> DEVICE_CATALOG = List.of(
			new DeviceProfile(
					"TONGFENG_UA_HOME",
					"URIC_ACID_METER",
					"尿酸仪",
					"Tongfeng",
					"UA-1",
					List.of("URIC_ACID"),
					"适合家庭日常尿酸跟踪"
			),
			new DeviceProfile(
					"TONGFENG_SMART_CUP",
					"SMART_WATER_CUP",
					"智能饮水杯",
					"Tongfeng",
					"CUP-1",
					List.of("HYDRATION"),
					"适合自动记录饮水与尿液颜色打卡"
			),
			new DeviceProfile(
					"TONGFENG_SCALE_HOME",
					"WEIGHT_SCALE",
					"体重秤",
					"Tongfeng",
					"SCALE-1",
					List.of("WEIGHT"),
					"适合晨起体重管理"
			),
			new DeviceProfile(
					"TONGFENG_BP_HOME",
					"BLOOD_PRESSURE_MONITOR",
					"血压计",
					"Tongfeng",
					"BP-1",
					List.of("BLOOD_PRESSURE"),
					"适合家庭血压与脉搏监测"
			),
			new DeviceProfile(
					"TONGFENG_COMBO_STATION",
					"HEALTH_COMBO_STATION",
					"多指标健康站",
					"Tongfeng",
					"STATION-1",
					List.of("URIC_ACID", "HYDRATION", "WEIGHT", "BLOOD_PRESSURE"),
					"适合家庭多指标集中采集"
			)
	);

	private final DeviceBindingRepository deviceBindingRepository;
	private final DeviceSyncEventRepository deviceSyncEventRepository;
	private final UricAcidRecordRepository uricAcidRecordRepository;
	private final HydrationRecordRepository hydrationRecordRepository;
	private final WeightRecordRepository weightRecordRepository;
	private final BloodPressureRecordRepository bloodPressureRecordRepository;
	private final HealthRuleEngineService healthRuleEngineService;
	private final GrowthIncentiveService growthIncentiveService;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;

	public DeviceIntegrationService(
			DeviceBindingRepository deviceBindingRepository,
			DeviceSyncEventRepository deviceSyncEventRepository,
			UricAcidRecordRepository uricAcidRecordRepository,
			HydrationRecordRepository hydrationRecordRepository,
			WeightRecordRepository weightRecordRepository,
			BloodPressureRecordRepository bloodPressureRecordRepository,
			HealthRuleEngineService healthRuleEngineService,
			GrowthIncentiveService growthIncentiveService,
			IdGenerator idGenerator,
			JsonCodec jsonCodec
	) {
		this.deviceBindingRepository = deviceBindingRepository;
		this.deviceSyncEventRepository = deviceSyncEventRepository;
		this.uricAcidRecordRepository = uricAcidRecordRepository;
		this.hydrationRecordRepository = hydrationRecordRepository;
		this.weightRecordRepository = weightRecordRepository;
		this.bloodPressureRecordRepository = bloodPressureRecordRepository;
		this.healthRuleEngineService = healthRuleEngineService;
		this.growthIncentiveService = growthIncentiveService;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
	}

	public List<AppContracts.DeviceCatalogItemResponse> listDeviceCatalog() {
		return DEVICE_CATALOG.stream()
				.map(profile -> new AppContracts.DeviceCatalogItemResponse(
						profile.profileCode(),
						profile.deviceType(),
						profile.deviceTypeName(),
						profile.vendorName(),
						profile.deviceModel(),
						profile.supportedMetricTypes(),
						profile.bindingHint()
				))
				.toList();
	}

	@Transactional
	public AppContracts.DeviceBindingResponse bindDevice(String userId, AppContracts.DeviceBindingCreateRequest request) {
		String serialNumber = request.serialNumber().trim();
		deviceBindingRepository.findByUserCodeAndSerialNumberAndStatus(userId, serialNumber, STATUS_ACTIVE)
				.ifPresent(item -> {
					throw new BusinessException("DEVICE_EXISTS", "该设备已绑定，无需重复添加");
				});
		DeviceProfile profile = requireSupportedProfile(
				request.deviceType(),
				request.vendorName(),
				request.deviceModel()
		);
		Instant now = Instant.now();
		DeviceBindingEntity entity = new DeviceBindingEntity();
		entity.setDeviceCode(idGenerator.next("device"));
		entity.setUserCode(userId);
		entity.setDeviceType(profile.deviceType());
		entity.setVendorName(profile.vendorName());
		entity.setDeviceModel(StringUtils.hasText(request.deviceModel()) ? request.deviceModel().trim() : profile.deviceModel());
		entity.setSerialNumber(serialNumber);
		entity.setAliasName(trimToNull(request.aliasName()));
		entity.setStatus(STATUS_ACTIVE);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		deviceBindingRepository.save(entity);
		return toBindingResponse(entity);
	}

	public List<AppContracts.DeviceBindingResponse> listDevices(String userId) {
		return deviceBindingRepository.findByUserCodeOrderByCreatedAtDesc(userId).stream()
				.map(this::toBindingResponse)
				.toList();
	}

	public AppContracts.DeviceOverviewResponse getDeviceOverview(String userId) {
		List<AppContracts.DeviceOverviewItemResponse> devices = deviceBindingRepository.findByUserCodeOrderByCreatedAtDesc(userId)
				.stream()
				.map(this::toOverviewItemResponse)
				.toList();
		int activeDevices = (int) devices.stream()
				.filter(item -> STATUS_ACTIVE.equals(item.status()))
				.count();
		int recentlySyncedDevices = (int) devices.stream()
				.filter(item -> DEVICE_HEALTH_ACTIVE.equals(item.syncHealthStatus()))
				.count();
		int attentionDevices = (int) devices.stream()
				.filter(item -> !DEVICE_HEALTH_ACTIVE.equals(item.syncHealthStatus()))
				.count();
		return new AppContracts.DeviceOverviewResponse(
				devices.size(),
				activeDevices,
				recentlySyncedDevices,
				attentionDevices,
				devices
		);
	}

	@Transactional
	public AppContracts.DeviceBindingResponse unbindDevice(String userId, String deviceCode) {
		DeviceBindingEntity entity = requireOwnedDevice(userId, deviceCode);
		entity.setStatus(STATUS_UNBOUND);
		entity.setUpdatedAt(Instant.now());
		deviceBindingRepository.save(entity);
		return toBindingResponse(entity);
	}

	public List<AppContracts.DeviceSyncResultResponse> listSyncEvents(String userId, String deviceCode) {
		DeviceBindingEntity device = requireOwnedDevice(userId, deviceCode);
		return deviceSyncEventRepository.findByDeviceCodeOrderByMeasuredAtDesc(device.getDeviceCode()).stream()
				.map(this::toSyncResultResponse)
				.toList();
	}

	@Transactional
	public AppContracts.DeviceSyncBatchResponse syncDeviceData(
			String userId,
			String deviceCode,
			AppContracts.DeviceSyncBatchRequest request
	) {
		DeviceBindingEntity device = requireOwnedDevice(userId, deviceCode);
		DeviceProfile profile = resolveProfile(device);
		List<AppContracts.DeviceSyncResultResponse> results = new ArrayList<>();
		Instant lastSyncedAt = device.getLastSyncedAt();

		for (AppContracts.DeviceSyncItemRequest item : request.items()) {
			AppContracts.DeviceSyncResultResponse result = syncSingleItem(userId, device, profile, item);
			results.add(result);
			if (lastSyncedAt == null || result.measuredAt().isAfter(lastSyncedAt)) {
				lastSyncedAt = result.measuredAt();
			}
		}

		device.setLastSyncedAt(lastSyncedAt == null ? Instant.now() : lastSyncedAt);
		device.setUpdatedAt(Instant.now());
		deviceBindingRepository.save(device);
		healthRuleEngineService.refreshDailySummary(userId, LocalDate.now());
		healthRuleEngineService.rebuildActiveReminders(userId);
		return new AppContracts.DeviceSyncBatchResponse(
				device.getDeviceCode(),
				(int) results.stream().filter(item -> SYNC_SUCCESS.equals(item.syncStatus())).count(),
				results,
				device.getLastSyncedAt()
		);
	}

	private AppContracts.DeviceSyncResultResponse syncSingleItem(
			String userId,
			DeviceBindingEntity device,
			DeviceProfile profile,
			AppContracts.DeviceSyncItemRequest item
	) {
		String metricType = normalizeUpper(item.metricType());
		validateMetricAllowed(profile, metricType);
		Instant measuredAt = item.measuredAt() == null ? Instant.now() : item.measuredAt();
		DeviceSyncEventEntity existing = deviceSyncEventRepository
				.findByDeviceCodeAndExternalEventId(device.getDeviceCode(), item.externalEventId())
				.orElse(null);
		if (existing != null) {
			return new AppContracts.DeviceSyncResultResponse(
					existing.getSyncCode(),
					existing.getMetricType(),
					SYNC_DUPLICATE,
					existing.getResultRecordCode(),
					existing.getSummaryText(),
					existing.getMeasuredAt()
			);
		}

		Instant now = Instant.now();
		DeviceSyncEventEntity event = new DeviceSyncEventEntity();
		event.setSyncCode(idGenerator.next("sync"));
		event.setUserCode(userId);
		event.setDeviceCode(device.getDeviceCode());
		event.setDeviceType(device.getDeviceType());
		event.setMetricType(metricType);
		event.setExternalEventId(item.externalEventId().trim());
		event.setMeasuredAt(measuredAt);
		event.setPayloadJson(jsonCodec.toJson(item));
		event.setCreatedAt(now);
		event.setUpdatedAt(now);

		String resultRecordId;
		String summary;
		switch (metricType) {
			case "URIC_ACID" -> {
				validateUricAcidItem(item);
				UricAcidRecordEntity record = new UricAcidRecordEntity();
				record.setRecordCode(idGenerator.next("ua"));
				record.setUserCode(userId);
				record.setUaValue(item.value().intValue());
				record.setUaUnit(defaultUnit(item.unit(), "umol/L"));
				record.setMeasuredAt(measuredAt);
				record.setSourceName(buildSourceName(device));
				record.setNoteText(trimToNull(item.note()));
				uricAcidRecordRepository.save(record);
				growthIncentiveService.awardRecordAction(
						userId,
						GrowthIncentiveService.ACTION_URIC_ACID_RECORD,
						record.getRecordCode(),
						"设备同步尿酸记录"
				);
				resultRecordId = record.getRecordCode();
				summary = "设备同步尿酸 " + record.getUaValue() + " " + record.getUaUnit();
			}
			case "HYDRATION" -> {
				validateHydrationItem(item);
				HydrationRecordEntity record = new HydrationRecordEntity();
				record.setRecordCode(idGenerator.next("hydration"));
				record.setUserCode(userId);
				record.setWaterIntakeMl(item.waterIntakeMl());
				record.setUrineColorLevel(item.urineColorLevel());
				record.setCheckedAt(measuredAt);
				record.setNoteText(trimToNull(item.note()));
				hydrationRecordRepository.save(record);
				growthIncentiveService.awardRecordAction(
						userId,
						GrowthIncentiveService.ACTION_HYDRATION_RECORD,
						record.getRecordCode(),
						"设备同步补水记录"
				);
				resultRecordId = record.getRecordCode();
				summary = "设备同步补水 " + record.getWaterIntakeMl() + "ml，尿液颜色等级 " + record.getUrineColorLevel();
			}
			case "WEIGHT" -> {
				validateWeightItem(item);
				WeightRecordEntity record = new WeightRecordEntity();
				record.setRecordCode(idGenerator.next("weight"));
				record.setUserCode(userId);
				record.setWeightValue(item.value());
				record.setMeasuredAt(measuredAt);
				record.setSourceName(buildSourceName(device));
				record.setNoteText(trimToNull(item.note()));
				weightRecordRepository.save(record);
				growthIncentiveService.awardRecordAction(
						userId,
						GrowthIncentiveService.ACTION_WEIGHT_RECORD,
						record.getRecordCode(),
						"设备同步体重记录"
				);
				resultRecordId = record.getRecordCode();
				summary = "设备同步体重 " + record.getWeightValue() + " " + defaultUnit(item.unit(), "kg");
			}
			case "BLOOD_PRESSURE" -> {
				validateBloodPressureItem(item);
				BloodPressureRecordEntity record = new BloodPressureRecordEntity();
				record.setRecordCode(idGenerator.next("bp"));
				record.setUserCode(userId);
				record.setSystolicPressure(item.systolicPressure());
				record.setDiastolicPressure(item.diastolicPressure());
				record.setPulseRate(item.pulseRate());
				record.setUnit(defaultUnit(item.unit(), "mmHg"));
				record.setMeasuredAt(measuredAt);
				record.setSourceName(buildSourceName(device));
				record.setNoteText(trimToNull(item.note()));
				bloodPressureRecordRepository.save(record);
				growthIncentiveService.awardRecordAction(
						userId,
						GrowthIncentiveService.ACTION_BLOOD_PRESSURE_RECORD,
						record.getRecordCode(),
						"设备同步血压记录"
				);
				resultRecordId = record.getRecordCode();
				summary = "设备同步血压 " + record.getSystolicPressure() + "/" + record.getDiastolicPressure()
						+ " " + record.getUnit()
						+ (record.getPulseRate() == null ? "" : "，脉搏 " + record.getPulseRate());
			}
			default -> throw new BusinessException("DEVICE_METRIC_UNSUPPORTED", "当前仅支持同步尿酸、饮水、体重和血压数据");
		}

		event.setResultRecordCode(resultRecordId);
		event.setSyncStatus(SYNC_SUCCESS);
		event.setSummaryText(summary);
		deviceSyncEventRepository.save(event);
		return new AppContracts.DeviceSyncResultResponse(
				event.getSyncCode(),
				event.getMetricType(),
				event.getSyncStatus(),
				event.getResultRecordCode(),
				event.getSummaryText(),
				event.getMeasuredAt()
		);
	}

	private DeviceBindingEntity requireOwnedDevice(String userId, String deviceCode) {
		DeviceBindingEntity entity = deviceBindingRepository.findByDeviceCode(deviceCode)
				.orElseThrow(() -> new BusinessException("DEVICE_NOT_FOUND", "设备不存在"));
		if (!Objects.equals(entity.getUserCode(), userId)) {
			throw new BusinessException("FORBIDDEN", "无权访问该设备");
		}
		if (!STATUS_ACTIVE.equals(entity.getStatus())) {
			throw new BusinessException("DEVICE_INACTIVE", "该设备已解绑，不能继续同步数据");
		}
		return entity;
	}

	private AppContracts.DeviceBindingResponse toBindingResponse(DeviceBindingEntity entity) {
		DeviceProfile profile = resolveProfile(entity);
		return new AppContracts.DeviceBindingResponse(
				entity.getDeviceCode(),
				entity.getDeviceType(),
				entity.getVendorName(),
				entity.getDeviceModel(),
				entity.getSerialNumber(),
				entity.getAliasName(),
				profile.profileCode(),
				profile.supportedMetricTypes(),
				entity.getStatus(),
				entity.getLastSyncedAt(),
				entity.getCreatedAt()
		);
	}

	private AppContracts.DeviceOverviewItemResponse toOverviewItemResponse(DeviceBindingEntity entity) {
		DeviceProfile profile = resolveProfile(entity);
		List<DeviceSyncEventEntity> syncEvents = deviceSyncEventRepository.findByDeviceCodeOrderByMeasuredAtDesc(entity.getDeviceCode());
		DeviceSyncEventEntity latestEvent = syncEvents.isEmpty() ? null : syncEvents.getFirst();
		return new AppContracts.DeviceOverviewItemResponse(
				entity.getDeviceCode(),
				entity.getAliasName(),
				entity.getDeviceType(),
				profile.deviceTypeName(),
				entity.getVendorName(),
				entity.getDeviceModel(),
				entity.getStatus(),
				profile.supportedMetricTypes(),
				syncEvents.size(),
				latestEvent == null ? null : latestEvent.getMetricType(),
				latestEvent == null ? null : latestEvent.getSummaryText(),
				determineDeviceHealthStatus(entity),
				entity.getLastSyncedAt()
		);
	}

	private AppContracts.DeviceSyncResultResponse toSyncResultResponse(DeviceSyncEventEntity entity) {
		return new AppContracts.DeviceSyncResultResponse(
				entity.getSyncCode(),
				entity.getMetricType(),
				entity.getSyncStatus(),
				entity.getResultRecordCode(),
				entity.getSummaryText(),
				entity.getMeasuredAt()
		);
	}

	private DeviceProfile requireSupportedProfile(String deviceType, String vendorName, String deviceModel) {
		String normalizedType = normalizeUpper(deviceType);
		String normalizedVendor = normalizeUpper(vendorName);
		String normalizedModel = normalizeNullable(deviceModel);
		List<DeviceProfile> sameTypeProfiles = DEVICE_CATALOG.stream()
				.filter(profile -> profile.deviceType().equals(normalizedType))
				.toList();
		if (sameTypeProfiles.isEmpty()) {
			throw new BusinessException("DEVICE_TYPE_UNSUPPORTED", "当前设备类型未开放接入，请先从设备目录中选择");
		}
		List<DeviceProfile> sameVendorProfiles = sameTypeProfiles.stream()
				.filter(profile -> normalizeUpper(profile.vendorName()).equals(normalizedVendor))
				.toList();
		if (sameVendorProfiles.isEmpty()) {
			throw new BusinessException("DEVICE_VENDOR_UNSUPPORTED", "当前厂商尚未完成该设备类型接入");
		}
		if (!StringUtils.hasText(deviceModel)) {
			return sameVendorProfiles.getFirst();
		}
		return sameVendorProfiles.stream()
				.filter(profile -> normalizeUpper(profile.deviceModel()).equals(normalizedModel))
				.findFirst()
				.orElseThrow(() -> new BusinessException("DEVICE_MODEL_UNSUPPORTED", "当前设备型号未在接入白名单中"));
	}

	private DeviceProfile resolveProfile(DeviceBindingEntity device) {
		String normalizedType = normalizeUpper(device.getDeviceType());
		String normalizedVendor = normalizeUpper(device.getVendorName());
		String normalizedModel = normalizeNullable(device.getDeviceModel());
		return DEVICE_CATALOG.stream()
				.filter(profile -> profile.deviceType().equals(normalizedType))
				.filter(profile -> normalizeUpper(profile.vendorName()).equals(normalizedVendor))
				.filter(profile -> !StringUtils.hasText(device.getDeviceModel())
						|| normalizeUpper(profile.deviceModel()).equals(normalizedModel))
				.findFirst()
				.orElseGet(() -> new DeviceProfile(
						"LEGACY_" + normalizedType,
						normalizedType,
						deviceTypeName(normalizedType),
						device.getVendorName(),
						device.getDeviceModel(),
						inferSupportedMetrics(normalizedType),
						"历史设备，按设备类型补齐能力映射"
				));
	}

	private void validateMetricAllowed(DeviceProfile profile, String metricType) {
		if (!ALL_METRIC_TYPES.contains(metricType)) {
			throw new BusinessException("DEVICE_METRIC_UNSUPPORTED", "当前仅支持同步尿酸、饮水、体重和血压数据");
		}
		if (!profile.supportedMetricTypes().contains(metricType)) {
			throw new BusinessException(
					"DEVICE_METRIC_NOT_ALLOWED",
					"当前设备类型仅支持同步 " + String.join(" / ", profile.supportedMetricTypes())
			);
		}
	}

	private String determineDeviceHealthStatus(DeviceBindingEntity entity) {
		if (!STATUS_ACTIVE.equals(entity.getStatus())) {
			return DEVICE_HEALTH_UNBOUND;
		}
		if (entity.getLastSyncedAt() == null) {
			return DEVICE_HEALTH_NEVER_SYNCED;
		}
		if (entity.getLastSyncedAt().isBefore(Instant.now().minus(7, ChronoUnit.DAYS))) {
			return DEVICE_HEALTH_STALE;
		}
		return DEVICE_HEALTH_ACTIVE;
	}

	private List<String> inferSupportedMetrics(String deviceType) {
		return switch (deviceType) {
			case "URIC_ACID_METER" -> List.of("URIC_ACID");
			case "SMART_WATER_CUP" -> List.of("HYDRATION");
			case "WEIGHT_SCALE" -> List.of("WEIGHT");
			case "BLOOD_PRESSURE_MONITOR" -> List.of("BLOOD_PRESSURE");
			case "HEALTH_COMBO_STATION" -> List.of("URIC_ACID", "HYDRATION", "WEIGHT", "BLOOD_PRESSURE");
			default -> ALL_METRIC_TYPES;
		};
	}

	private String deviceTypeName(String deviceType) {
		return switch (deviceType) {
			case "URIC_ACID_METER" -> "尿酸仪";
			case "SMART_WATER_CUP" -> "智能饮水杯";
			case "WEIGHT_SCALE" -> "体重秤";
			case "BLOOD_PRESSURE_MONITOR" -> "血压计";
			case "HEALTH_COMBO_STATION" -> "多指标健康站";
			default -> "健康设备";
		};
	}

	private void validateUricAcidItem(AppContracts.DeviceSyncItemRequest item) {
		if (item.value() == null || item.value().doubleValue() <= 0) {
			throw new BusinessException("DEVICE_URIC_ACID_INVALID", "尿酸同步值必须大于0");
		}
	}

	private void validateHydrationItem(AppContracts.DeviceSyncItemRequest item) {
		if (item.waterIntakeMl() == null || item.waterIntakeMl() < 0) {
			throw new BusinessException("DEVICE_HYDRATION_INVALID", "饮水量不能为空且不能为负数");
		}
		if (item.urineColorLevel() == null || item.urineColorLevel() < 1 || item.urineColorLevel() > 5) {
			throw new BusinessException("DEVICE_HYDRATION_INVALID", "尿液颜色等级必须在1到5之间");
		}
	}

	private void validateWeightItem(AppContracts.DeviceSyncItemRequest item) {
		if (item.value() == null || item.value().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("DEVICE_WEIGHT_INVALID", "体重同步值必须大于0");
		}
	}

	private void validateBloodPressureItem(AppContracts.DeviceSyncItemRequest item) {
		if (item.systolicPressure() == null || item.systolicPressure() < 60 || item.systolicPressure() > 260) {
			throw new BusinessException("DEVICE_BP_INVALID", "收缩压必须在60到260之间");
		}
		if (item.diastolicPressure() == null || item.diastolicPressure() < 40 || item.diastolicPressure() > 180) {
			throw new BusinessException("DEVICE_BP_INVALID", "舒张压必须在40到180之间");
		}
		if (item.pulseRate() != null && (item.pulseRate() < 30 || item.pulseRate() > 240)) {
			throw new BusinessException("DEVICE_BP_INVALID", "脉搏必须在30到240之间");
		}
	}

	private String buildSourceName(DeviceBindingEntity device) {
		String alias = StringUtils.hasText(device.getAliasName()) ? device.getAliasName().trim() : device.getVendorName();
		return alias + "设备";
	}

	private String normalizeUpper(String value) {
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeNullable(String value) {
		return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String defaultUnit(String value, String fallback) {
		return StringUtils.hasText(value) ? value.trim() : fallback;
	}

	private record DeviceProfile(
			String profileCode,
			String deviceType,
			String deviceTypeName,
			String vendorName,
			String deviceModel,
			List<String> supportedMetricTypes,
			String bindingHint
	) {
	}
}
