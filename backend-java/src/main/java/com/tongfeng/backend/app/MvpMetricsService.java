package com.tongfeng.backend.app;

import com.tongfeng.backend.app.persistence.entity.MvpUsageEventEntity;
import com.tongfeng.backend.app.persistence.repo.MvpUsageEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MvpMetricsService {

	public static final String EVENT_MEAL_ANALYZED = "MEAL_ANALYZED";
	public static final String EVENT_URIC_ACID_RECORDED = "URIC_ACID_RECORDED";
	public static final String EVENT_LAB_REPORT_ANALYZED = "LAB_REPORT_ANALYZED";
	public static final String EVENT_FAMILY_INVITE_CREATED = "FAMILY_INVITE_CREATED";
	public static final String EVENT_FAMILY_INVITE_ACCEPTED = "FAMILY_INVITE_ACCEPTED";
	public static final String EVENT_FAMILY_PATIENT_SUMMARY_VIEWED = "FAMILY_PATIENT_SUMMARY_VIEWED";

	private static final Map<String, String> EVENT_LABELS = Map.of(
			EVENT_MEAL_ANALYZED, "饮食识别",
			EVENT_URIC_ACID_RECORDED, "尿酸记录",
			EVENT_LAB_REPORT_ANALYZED, "化验单解析",
			EVENT_FAMILY_INVITE_CREATED, "家庭邀请创建",
			EVENT_FAMILY_INVITE_ACCEPTED, "家庭邀请接受",
			EVENT_FAMILY_PATIENT_SUMMARY_VIEWED, "家属查看患者摘要"
	);

	private static final List<String> EVENT_ORDER = List.of(
			EVENT_MEAL_ANALYZED,
			EVENT_URIC_ACID_RECORDED,
			EVENT_LAB_REPORT_ANALYZED,
			EVENT_FAMILY_INVITE_CREATED,
			EVENT_FAMILY_INVITE_ACCEPTED,
			EVENT_FAMILY_PATIENT_SUMMARY_VIEWED
	);

	private final MvpUsageEventRepository mvpUsageEventRepository;
	private final IdGenerator idGenerator;
	private final JsonCodec jsonCodec;

	public MvpMetricsService(
			MvpUsageEventRepository mvpUsageEventRepository,
			IdGenerator idGenerator,
			JsonCodec jsonCodec
	) {
		this.mvpUsageEventRepository = mvpUsageEventRepository;
		this.idGenerator = idGenerator;
		this.jsonCodec = jsonCodec;
	}

	@Transactional
	public void recordEvent(
			String userId,
			String eventType,
			String sourcePage,
			Instant eventAt,
			Map<String, Object> payload
	) {
		Instant now = Instant.now();
		Instant effectiveEventAt = eventAt == null ? now : eventAt;
		MvpUsageEventEntity entity = new MvpUsageEventEntity();
		entity.setEventCode(idGenerator.next("mvp"));
		entity.setUserCode(userId);
		entity.setEventType(eventType);
		entity.setSourcePage(StringUtils.hasText(sourcePage) ? sourcePage.trim() : "unknown");
		entity.setEventDate(effectiveEventAt.atZone(ZoneId.systemDefault()).toLocalDate());
		entity.setCreatedAt(now);
		entity.setPayloadJson(jsonCodec.toJson(sanitizePayload(payload)));
		mvpUsageEventRepository.save(entity);
	}

	public AppContracts.MvpMetricsSummaryResponse getSummary(int days) {
		int safeDays = Math.max(days, 1);
		LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
		List<MvpUsageEventEntity> events = mvpUsageEventRepository.findByEventDateGreaterThanEqualOrderByCreatedAtDesc(startDate);
		return new AppContracts.MvpMetricsSummaryResponse(
				safeDays,
				events.size(),
				distinctUsers(events),
				countUsers(events, EVENT_MEAL_ANALYZED),
				countUsers(events, EVENT_URIC_ACID_RECORDED),
				countUsers(events, EVENT_LAB_REPORT_ANALYZED),
				countUsers(events, EVENT_FAMILY_INVITE_CREATED),
				countUsers(events, EVENT_FAMILY_INVITE_ACCEPTED),
				countUsers(events, EVENT_FAMILY_PATIENT_SUMMARY_VIEWED),
				buildBreakdown(events),
				Instant.now()
		);
	}

	private List<AppContracts.MvpMetricBreakdownItemResponse> buildBreakdown(List<MvpUsageEventEntity> events) {
		Map<String, List<MvpUsageEventEntity>> grouped = events.stream()
				.collect(Collectors.groupingBy(MvpUsageEventEntity::getEventType));
		return EVENT_ORDER.stream()
				.map(eventType -> {
					List<MvpUsageEventEntity> group = grouped.getOrDefault(eventType, List.of());
					Instant latestEventAt = group.stream()
							.map(MvpUsageEventEntity::getCreatedAt)
							.filter(Objects::nonNull)
							.max(Instant::compareTo)
							.orElse(null);
					return new AppContracts.MvpMetricBreakdownItemResponse(
							eventType,
							EVENT_LABELS.getOrDefault(eventType, eventType),
							group.size(),
							distinctUsers(group),
							latestEventAt
					);
				})
				.toList();
	}

	private long countUsers(List<MvpUsageEventEntity> events, String eventType) {
		return events.stream()
				.filter(item -> eventType.equals(item.getEventType()))
				.map(MvpUsageEventEntity::getUserCode)
				.filter(StringUtils::hasText)
				.distinct()
				.count();
	}

	private long distinctUsers(List<MvpUsageEventEntity> events) {
		return events.stream()
				.map(MvpUsageEventEntity::getUserCode)
				.filter(StringUtils::hasText)
				.distinct()
				.count();
	}

	private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return Map.of();
		}
		Map<String, Object> sanitized = new LinkedHashMap<>();
		payload.forEach((key, value) -> {
			if (StringUtils.hasText(key) && value != null) {
				sanitized.put(key, value);
			}
		});
		return sanitized;
	}
}
