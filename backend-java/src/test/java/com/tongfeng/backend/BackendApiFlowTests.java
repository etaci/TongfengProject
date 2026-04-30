package com.tongfeng.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongfeng.backend.app.AppContracts;
import com.tongfeng.backend.app.MvpMetricsService;
import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import com.tongfeng.backend.app.persistence.entity.MvpUsageEventEntity;
import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import com.tongfeng.backend.app.persistence.repo.FlareRecordRepository;
import com.tongfeng.backend.app.persistence.repo.HydrationRecordRepository;
import com.tongfeng.backend.app.persistence.repo.LabReportRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MealRecordRepository;
import com.tongfeng.backend.app.persistence.repo.MvpUsageEventRepository;
import com.tongfeng.backend.app.persistence.repo.UricAcidRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApiFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MealRecordRepository mealRecordRepository;

	@Autowired
	private HydrationRecordRepository hydrationRecordRepository;

	@Autowired
	private FlareRecordRepository flareRecordRepository;

	@Autowired
	private LabReportRecordRepository labReportRecordRepository;

	@Autowired
	private UricAcidRecordRepository uricAcidRecordRepository;

	@Autowired
	private MvpUsageEventRepository mvpUsageEventRepository;

	@Test
	void shouldCompleteMvpClosedLoop() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/mock-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nickname": "demo-user"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();

		JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String token = loginBody.path("data").path("token").asText();
		String patientUserId = loginBody.path("data").path("userId").asText();

		mockMvc.perform(get("/api/v1/app/capabilities")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.features[0].featureKey").value("family-care"));

		mockMvc.perform(get("/api/v1/profile")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("demo-user"));

		mockMvc.perform(put("/api/v1/profile")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "demo-user",
								  "gender": "MALE",
								  "birthday": "1990-01-01",
								  "heightCm": 178,
								  "targetUricAcid": 360,
								  "allergies": ["seafood"],
								  "comorbidities": ["hyperuricemia"],
								  "emergencyContact": "family-a"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.gender").value("MALE"));

		mockMvc.perform(post("/api/v1/records/uric-acid")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 468,
								  "unit": "umol/L",
								  "source": "home-device",
								  "note": "fasting"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordId").isNotEmpty());

		mockMvc.perform(post("/api/v1/records/weight")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 71.8,
								  "source": "manual-scale",
								  "note": "morning"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordId").isNotEmpty());

		mockMvc.perform(post("/api/v1/records/hydration")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "waterIntakeMl": 900,
								  "urineColorLevel": 4,
								  "note": "afternoon"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordId").isNotEmpty());

		mockMvc.perform(post("/api/v1/records/flares")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "joint": "left-ankle",
								  "painLevel": 8,
								  "note": "night flare"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordId").isNotEmpty());

		mockMvc.perform(put("/api/v1/proactive-care/settings")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "monitoringCity": "Shanghai",
								  "countryCode": "CN",
								  "weatherAlertsEnabled": true
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.monitoringCity").value("Shanghai"));

		mockMvc.perform(get("/api/v1/proactive-care/brief")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.overallRiskLevel").isNotEmpty())
				.andExpect(jsonPath("$.data.factors").isArray());

		mockMvc.perform(get("/api/v1/flares/reports/latest")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.reportId").isNotEmpty())
				.andExpect(jsonPath("$.data.joint").value("left-ankle"));

		mockMvc.perform(get("/api/v1/records/uric-acid")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].value").value(468))
				.andExpect(jsonPath("$.data[0].riskLevel").value("YELLOW"));

		mockMvc.perform(get("/api/v1/records/hydration")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].waterIntakeMl").value(900))
				.andExpect(jsonPath("$.data[0].riskLevel").value("YELLOW"));

		mockMvc.perform(post("/api/v1/knowledge/ask")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "question": "Should I drink more water when uric acid is high?",
								  "scene": "mvp-test"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.answer").isNotEmpty());

		mockMvc.perform(put("/api/v1/medications")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentMedications": [
								    {
								      "name": "allopurinol",
								      "dosage": "100mg",
								      "frequency": "once-daily",
								      "remark": "after dinner"
								    }
								  ],
								  "followUpNote": "recheck uric acid after two weeks"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentMedications[0].name").value("allopurinol"));

		mockMvc.perform(get("/api/v1/dashboard/overview")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.stage").isNotEmpty())
				.andExpect(jsonPath("$.data.uricAcidCount").value(1));

		mockMvc.perform(get("/api/v1/dashboard/daily-summaries")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].latestUricAcidValue").value(468));

		MvcResult centerResult = mockMvc.perform(get("/api/v1/records/center?types=WEIGHT&types=URIC_ACID&limit=2")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").value(2))
				.andExpect(jsonPath("$.data.returnedCount").value(2))
				.andReturn();

		MvcResult weightListResult = mockMvc.perform(get("/api/v1/records/weight")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

		String weightRecordId = objectMapper.readTree(weightListResult.getResponse().getContentAsString())
				.path("data")
				.get(0)
				.path("recordId")
				.asText();

		mockMvc.perform(get("/api/v1/records/detail?type=WEIGHT&recordId=" + weightRecordId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.type").value("WEIGHT"));

		mockMvc.perform(put("/api/v1/records/detail?type=WEIGHT&recordId=" + weightRecordId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "decimalValue": 70.6,
								  "source": "manual-adjustment",
								  "note": "after calibration",
								  "changeReason": "sync source normalized"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fields[0].value").value("70.6"));

		mockMvc.perform(get("/api/v1/records/audits?type=WEIGHT&recordId=" + weightRecordId + "&limit=20")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].action").value("UPDATE"));

		mockMvc.perform(delete("/api/v1/records/detail?type=WEIGHT&recordId=" + weightRecordId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DELETED"));

		MvcResult deletedAuditResult = mockMvc.perform(get("/api/v1/records/audits?type=WEIGHT&recordId=" + weightRecordId + "&limit=20")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode deletedAuditBody = objectMapper.readTree(deletedAuditResult.getResponse().getContentAsString());
		String deleteAuditId = deletedAuditBody.path("data").get(0).path("auditId").asText();

		mockMvc.perform(post("/api/v1/records/restore?type=WEIGHT&recordId=" + weightRecordId + "&auditId=" + deleteAuditId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "changeReason": "undo delete"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RESTORED"));

		mockMvc.perform(get("/api/v1/records/timeline")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.events[0].type").isNotEmpty());

		mockMvc.perform(get("/api/v1/reminders")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].title").isNotEmpty());

		MvcResult familyLoginResult = mockMvc.perform(post("/api/v1/auth/mock-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nickname": "family-user"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		String familyToken = objectMapper.readTree(familyLoginResult.getResponse().getContentAsString())
				.path("data")
				.path("token")
				.asText();
		String familyUserId = objectMapper.readTree(familyLoginResult.getResponse().getContentAsString())
				.path("data")
				.path("userId")
				.asText();

		MvcResult inviteResult = mockMvc.perform(post("/api/v1/family/invitations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "relationType": "SPOUSE",
								  "inviteMessage": "Please watch recent risks together",
								  "expiresInDays": 7
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.inviteCode").isNotEmpty())
				.andReturn();

		String inviteCode = objectMapper.readTree(inviteResult.getResponse().getContentAsString())
				.path("data")
				.path("inviteCode")
				.asText();

		mockMvc.perform(post("/api/v1/family/invitations/{inviteCode}/accept", inviteCode)
						.header("Authorization", "Bearer " + familyToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ACCEPTED"));

		MvcResult memberResult = mockMvc.perform(get("/api/v1/family/members")
						.header("Authorization", "Bearer " + familyToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.asCaregiver[0].patientNickname").value("demo-user"))
				.andReturn();

		String bindingCode = objectMapper.readTree(memberResult.getResponse().getContentAsString())
				.path("data")
				.path("asCaregiver")
				.get(0)
				.path("bindingCode")
				.asText();

		mockMvc.perform(get("/api/v1/family/alerts")
						.header("Authorization", "Bearer " + familyToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].patientNickname").value("demo-user"));

		mockMvc.perform(get("/api/v1/family/patients/{patientUserId}/summary", patientUserId)
						.header("Authorization", "Bearer " + familyToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.patientNickname").value("demo-user"))
				.andExpect(jsonPath("$.data.reminders").isArray());

		MvcResult metricsResult = mockMvc.perform(get("/api/v1/mvp/metrics/summary?days=7")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode metricsBody = objectMapper.readTree(metricsResult.getResponse().getContentAsString());
		assertTrue(metricsBody.path("data").path("totalEvents").asInt() >= 4);
		assertTrue(metricsBody.path("data").path("uricAcidRecordUsers").asInt() >= 1);
		assertTrue(metricsBody.path("data").path("familyInviteUsers").asInt() >= 1);
		assertTrue(metricsBody.path("data").path("familyAcceptUsers").asInt() >= 1);
		assertTrue(metricsBody.path("data").path("familySummaryUsers").asInt() >= 1);

		List<MvpUsageEventEntity> recentUsageEvents = mvpUsageEventRepository.findByEventDateGreaterThanEqualOrderByCreatedAtDesc(LocalDate.now().minusDays(6));
		assertTrue(recentUsageEvents.stream().anyMatch(item ->
				patientUserId.equals(item.getUserCode())
						&& MvpMetricsService.EVENT_URIC_ACID_RECORDED.equals(item.getEventType())));
		assertTrue(recentUsageEvents.stream().anyMatch(item ->
				patientUserId.equals(item.getUserCode())
						&& MvpMetricsService.EVENT_FAMILY_INVITE_CREATED.equals(item.getEventType())));
		assertTrue(recentUsageEvents.stream().anyMatch(item ->
				familyUserId.equals(item.getUserCode())
						&& MvpMetricsService.EVENT_FAMILY_INVITE_ACCEPTED.equals(item.getEventType())));
		assertTrue(recentUsageEvents.stream().anyMatch(item ->
				familyUserId.equals(item.getUserCode())
						&& MvpMetricsService.EVENT_FAMILY_PATIENT_SUMMARY_VIEWED.equals(item.getEventType())));

		mockMvc.perform(delete("/api/v1/family/members/{bindingCode}", bindingCode)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("UNBOUND"));
	}

	@Test
	void shouldBuildUricAcidCauseAnalysisFromRecentSignals() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/mock-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nickname": "analysis-user"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String token = loginBody.path("data").path("token").asText();
		String userId = loginBody.path("data").path("userId").asText();
		String suffix = String.valueOf(System.nanoTime());
		Instant now = Instant.now();

		UricAcidRecordEntity previousUa = new UricAcidRecordEntity();
		previousUa.setRecordCode("ua-prev-" + suffix);
		previousUa.setUserCode(userId);
		previousUa.setUaValue(430);
		previousUa.setUaUnit("umol/L");
		previousUa.setMeasuredAt(now.minus(5, ChronoUnit.DAYS));
		previousUa.setSourceName("manual");
		previousUa.setNoteText("baseline");
		uricAcidRecordRepository.save(previousUa);

		UricAcidRecordEntity latestUa = new UricAcidRecordEntity();
		latestUa.setRecordCode("ua-latest-" + suffix);
		latestUa.setUserCode(userId);
		latestUa.setUaValue(510);
		latestUa.setUaUnit("umol/L");
		latestUa.setMeasuredAt(now.minus(2, ChronoUnit.HOURS));
		latestUa.setSourceName("manual");
		latestUa.setNoteText("follow-up");
		uricAcidRecordRepository.save(latestUa);

		MealRecordEntity mealRecord = new MealRecordEntity();
		mealRecord.setRecordCode("meal-" + suffix);
		mealRecord.setUserCode(userId);
		mealRecord.setMealType("DINNER");
		mealRecord.setTakenAt(now.minus(1, ChronoUnit.DAYS));
		mealRecord.setNoteText("夜宵喝了啤酒");
		mealRecord.setRiskLevel("RED");
		mealRecord.setPurineEstimateMg(320);
		mealRecord.setItemsJson(objectMapper.writeValueAsString(List.of(
				new AppContracts.MealItem("啤酒", AppContracts.RiskLevel.RED, "识别到饮酒", 120),
				new AppContracts.MealItem("小龙虾", AppContracts.RiskLevel.RED, "识别到高嘌呤食材", 200)
		)));
		mealRecord.setSuggestionsJson(objectMapper.writeValueAsString(List.of("避免夜间饮酒", "减少高嘌呤聚餐")));
		mealRecord.setSummaryText("夜宵饮酒并摄入高嘌呤食材，风险较高。");
		mealRecordRepository.save(mealRecord);

		HydrationRecordEntity hydrationRecord = new HydrationRecordEntity();
		hydrationRecord.setRecordCode("hydration-" + suffix);
		hydrationRecord.setUserCode(userId);
		hydrationRecord.setWaterIntakeMl(900);
		hydrationRecord.setUrineColorLevel(4);
		hydrationRecord.setCheckedAt(now.minus(6, ChronoUnit.HOURS));
		hydrationRecord.setNoteText("补水不足");
		hydrationRecordRepository.save(hydrationRecord);

		FlareRecordEntity flareRecord = new FlareRecordEntity();
		flareRecord.setRecordCode("flare-" + suffix);
		flareRecord.setUserCode(userId);
		flareRecord.setJointName("left-ankle");
		flareRecord.setPainLevel(8);
		flareRecord.setStartedAt(now.minus(2, ChronoUnit.DAYS));
		flareRecord.setDurationNote("8h");
		flareRecord.setNoteText("持续疼痛");
		flareRecordRepository.save(flareRecord);

		LabReportRecordEntity labReport = new LabReportRecordEntity();
		labReport.setReportCode("lab-" + suffix);
		labReport.setUserCode(userId);
		labReport.setReportDate(LocalDate.now().minusDays(3));
		labReport.setIndicatorsJson(objectMapper.writeValueAsString(List.of(
				new AppContracts.LabIndicator("UA", "尿酸", BigDecimal.valueOf(510), "umol/L", "208-428", AppContracts.RiskLevel.RED),
				new AppContracts.LabIndicator("CREA", "肌酐", BigDecimal.valueOf(122), "umol/L", "57-111", AppContracts.RiskLevel.YELLOW)
		)));
		labReport.setOverallRiskLevel("RED");
		labReport.setSuggestionsJson(objectMapper.writeValueAsString(List.of("建议尽快复查尿酸", "注意肾功能随访")));
		labReport.setSummaryText("尿酸显著偏高，并伴随代谢指标异常，建议持续复查。");
		labReportRecordRepository.save(labReport);

		mockMvc.perform(get("/api/v1/analysis/uric-acid-causes?lookbackDays=7")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.latestUricAcidValue").value(510))
				.andExpect(jsonPath("$.data.targetUricAcidValue").value(360))
				.andExpect(jsonPath("$.data.overallRiskLevel").value("RED"))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("CURRENT_URIC_ACID_HIGH")))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("URIC_ACID_TREND_UP")))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("DIET_RISK_EXPOSURE")))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("ALCOHOL_EXPOSURE")))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("HYDRATION_INSUFFICIENT")))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("RECENT_FLARE")))
				.andExpect(jsonPath("$.data.factors[*].code", hasItem("LAB_SIGNAL")))
				.andExpect(jsonPath("$.data.nextActions").isArray());
	}
}
