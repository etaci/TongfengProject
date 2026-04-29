package com.tongfeng.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApiFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void shouldCompleteBasicClosedLoop() throws Exception {
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

		mockMvc.perform(get("/api/v1/flares/reports/latest")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.reportId").isNotEmpty())
				.andExpect(jsonPath("$.data.joint").value("left-ankle"));

		mockMvc.perform(get("/api/v1/records/flares")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].joint").value("left-ankle"))
				.andExpect(jsonPath("$.data[0].riskLevel").value("RED"));

		MvcResult familyLoginResult = mockMvc.perform(post("/api/v1/auth/mock-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nickname": "family-user"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();

		JsonNode familyLoginBody = objectMapper.readTree(familyLoginResult.getResponse().getContentAsString());
		String familyToken = familyLoginBody.path("data").path("token").asText();

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

		mockMvc.perform(get("/api/v1/family/invitations")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].status").value("ACCEPTED"));

		mockMvc.perform(delete("/api/v1/family/members/{bindingCode}", bindingCode)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("UNBOUND"));

		mockMvc.perform(get("/api/v1/devices/catalog")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].profileCode").isNotEmpty())
				.andExpect(jsonPath("$.data[4].deviceType").value("HEALTH_COMBO_STATION"));

		MvcResult bindDeviceResult = mockMvc.perform(post("/api/v1/devices")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "deviceType": "HEALTH_COMBO_STATION",
								  "vendorName": "Tongfeng",
								  "deviceModel": "STATION-1",
								  "serialNumber": "STATION-SN-001",
								  "aliasName": "family-health-station"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deviceCode").isNotEmpty())
				.andExpect(jsonPath("$.data.vendorProfileCode").value("TONGFENG_COMBO_STATION"))
				.andReturn();

		String deviceCode = objectMapper.readTree(bindDeviceResult.getResponse().getContentAsString())
				.path("data")
				.path("deviceCode")
				.asText();

		mockMvc.perform(get("/api/v1/devices")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].supportedMetricTypes.length()").value(4));

		mockMvc.perform(post("/api/v1/devices/{deviceCode}/sync", deviceCode)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "items": [
								    {
								      "metricType": "URIC_ACID",
								      "externalEventId": "evt-ua-1",
								      "measuredAt": "2026-04-29T08:00:00Z",
								      "value": 502,
								      "unit": "umol/L",
								      "note": "device sync"
								    },
								    {
								      "metricType": "HYDRATION",
								      "externalEventId": "evt-hyd-1",
								      "measuredAt": "2026-04-29T09:00:00Z",
								      "waterIntakeMl": 900,
								      "urineColorLevel": 4,
								      "note": "smart cup sync"
								    }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.syncedCount").value(2))
				.andExpect(jsonPath("$.data.results[0].syncStatus").value("SUCCESS"));

		mockMvc.perform(get("/api/v1/devices/{deviceCode}/sync-events", deviceCode)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].syncCode").isNotEmpty())
				.andExpect(jsonPath("$.data[0].metricType").isNotEmpty());

		mockMvc.perform(get("/api/v1/records/uric-acid")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].value").value(468))
				.andExpect(jsonPath("$.data[0].riskLevel").value("YELLOW"))
				.andExpect(jsonPath("$.data[1].value").value(502))
				.andExpect(jsonPath("$.data[1].riskLevel").value("RED"));

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
								  "scene": "growth-task-test"
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
				.andExpect(jsonPath("$.data.uricAcidCount").value(2));

		mockMvc.perform(get("/api/v1/growth/overview")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.level").value(2))
				.andExpect(jsonPath("$.data.totalPoints").value(124))
				.andExpect(jsonPath("$.data.badgesCount").value(4));

		mockMvc.perform(get("/api/v1/growth/tasks")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].taskCode").isNotEmpty())
				.andExpect(jsonPath("$.data[1].completed").value(true));

		mockMvc.perform(get("/api/v1/growth/weekly-plan")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.weekStartDate").isNotEmpty())
				.andExpect(jsonPath("$.data.challenges[0].challengeCode").isNotEmpty())
				.andExpect(jsonPath("$.data.challenges[0].category").isNotEmpty());

		mockMvc.perform(get("/api/v1/growth/rewards")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].rewardKey").value("DIET_GUIDE_PACK"))
				.andExpect(jsonPath("$.data[0].claimable").value(true));

		mockMvc.perform(get("/api/v1/growth/points?limit=10")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].pointId").isNotEmpty())
				.andExpect(jsonPath("$.data[0].points").isNumber());

		mockMvc.perform(get("/api/v1/growth/badges")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].badgeKey").isNotEmpty())
				.andExpect(jsonPath("$.data[0].badgeName").isNotEmpty());

		mockMvc.perform(post("/api/v1/growth/rewards/{rewardKey}/claim", "DIET_GUIDE_PACK")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.rewardKey").value("DIET_GUIDE_PACK"))
				.andExpect(jsonPath("$.data.remainingPoints").value(64))
				.andExpect(jsonPath("$.data.status").value("CLAIMED"));

		mockMvc.perform(get("/api/v1/growth/reward-claims")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].rewardKey").value("DIET_GUIDE_PACK"))
				.andExpect(jsonPath("$.data[0].pointsCost").value(60));

		mockMvc.perform(get("/api/v1/growth/overview")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalPoints").value(124))
				.andExpect(jsonPath("$.data.redeemablePoints").value(64))
				.andExpect(jsonPath("$.data.badgesCount").value(5));

		mockMvc.perform(post("/api/v1/records/blood-pressure")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "systolicPressure": 148,
								  "diastolicPressure": 96,
								  "pulseRate": 82,
								  "unit": "mmHg",
								  "source": "home-bp-device",
								  "note": "evening measure"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recordId").isNotEmpty());

		mockMvc.perform(get("/api/v1/records/blood-pressure")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].systolicPressure").value(148))
				.andExpect(jsonPath("$.data[0].riskLevel").value("YELLOW"));

		mockMvc.perform(post("/api/v1/devices/{deviceCode}/sync", deviceCode)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "items": [
								    {
								      "metricType": "WEIGHT",
								      "externalEventId": "evt-weight-1",
								      "measuredAt": "2026-04-29T10:00:00Z",
								      "value": 71.8,
								      "unit": "kg",
								      "note": "smart scale sync"
								    },
								    {
								      "metricType": "BLOOD_PRESSURE",
								      "externalEventId": "evt-bp-1",
								      "measuredAt": "2026-04-29T10:05:00Z",
								      "systolicPressure": 162,
								      "diastolicPressure": 101,
								      "pulseRate": 88,
								      "unit": "mmHg",
								      "note": "bp monitor sync"
								    }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.syncedCount").value(2))
				.andExpect(jsonPath("$.data.results[1].metricType").value("BLOOD_PRESSURE"));

		mockMvc.perform(get("/api/v1/records/blood-pressure")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].systolicPressure").value(148))
				.andExpect(jsonPath("$.data[0].riskLevel").value("YELLOW"))
				.andExpect(jsonPath("$.data[1].systolicPressure").value(162))
				.andExpect(jsonPath("$.data[1].riskLevel").value("RED"));

		mockMvc.perform(get("/api/v1/records/weight")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].value").value(71.8))
				.andExpect(jsonPath("$.data[0].source").value("family-health-station设备"));

		mockMvc.perform(get("/api/v1/devices/overview")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalDevices").value(1))
				.andExpect(jsonPath("$.data.recentlySyncedDevices").value(1))
				.andExpect(jsonPath("$.data.devices[0].syncHealthStatus").value("ACTIVE"))
				.andExpect(jsonPath("$.data.devices[0].latestMetricType").value("BLOOD_PRESSURE"));

		MvcResult centerResult = mockMvc.perform(get("/api/v1/records/center?types=WEIGHT&types=BLOOD_PRESSURE&types=URIC_ACID&limit=3")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.types[0]").value("WEIGHT"))
				.andExpect(jsonPath("$.data.totalCount").value(5))
				.andExpect(jsonPath("$.data.returnedCount").value(3))
				.andExpect(jsonPath("$.data.items[0].type").value("BLOOD_PRESSURE"))
				.andExpect(jsonPath("$.data.hasMore").value(true))
				.andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
				.andReturn();

		JsonNode centerBody = objectMapper.readTree(centerResult.getResponse().getContentAsString());
		String nextCursor = centerBody.path("data").path("nextCursor").asText();

		mockMvc.perform(get("/api/v1/records/center?types=WEIGHT&types=BLOOD_PRESSURE&types=URIC_ACID&limit=3&cursor=" + nextCursor)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.returnedCount").value(2))
				.andExpect(jsonPath("$.data.items[0].type").value("WEIGHT"))
				.andExpect(jsonPath("$.data.hasMore").value(false));

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
				.andExpect(jsonPath("$.data.type").value("WEIGHT"))
				.andExpect(jsonPath("$.data.fields[0].key").value("value"))
				.andExpect(jsonPath("$.data.source").value("family-health-station设备"));

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
				.andExpect(jsonPath("$.data.type").value("WEIGHT"))
				.andExpect(jsonPath("$.data.fields[0].value").value("70.6"))
				.andExpect(jsonPath("$.data.source").value("manual-adjustment"))
				.andExpect(jsonPath("$.data.note").value("after calibration"));

		mockMvc.perform(get("/api/v1/records/audits?type=WEIGHT&recordId=" + weightRecordId + "&limit=20")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].action").value("UPDATE"))
				.andExpect(jsonPath("$.data[0].fields[?(@.key=='value')]").isArray())
				.andExpect(jsonPath("$.data[0].fields[?(@.key=='source')]").isArray())
				.andExpect(jsonPath("$.data[0].fields[?(@.key=='note')]").isArray());

		mockMvc.perform(delete("/api/v1/records/detail?type=WEIGHT&recordId=" + weightRecordId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DELETED"));

		MvcResult deletedAuditResult = mockMvc.perform(get("/api/v1/records/audits?type=WEIGHT&recordId=" + weightRecordId + "&limit=20")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].action").value("DELETE"))
				.andExpect(jsonPath("$.data[1].action").value("UPDATE"))
				.andReturn();

		JsonNode deletedAuditBody = objectMapper.readTree(deletedAuditResult.getResponse().getContentAsString());
		String deleteAuditId = deletedAuditBody.path("data").get(0).path("auditId").asText();
		String updateAuditId = deletedAuditBody.path("data").get(1).path("auditId").asText();

		mockMvc.perform(post("/api/v1/records/restore?type=WEIGHT&recordId=" + weightRecordId + "&auditId=" + deleteAuditId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "changeReason": "undo delete"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RESTORED"))
				.andExpect(jsonPath("$.data.detail.fields[0].value").value("70.6"))
				.andExpect(jsonPath("$.data.detail.source").value("manual-adjustment"));

		mockMvc.perform(get("/api/v1/records/detail?type=WEIGHT&recordId=" + weightRecordId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fields[0].value").value("70.6"))
				.andExpect(jsonPath("$.data.note").value("after calibration"));

		mockMvc.perform(post("/api/v1/records/restore?type=WEIGHT&recordId=" + weightRecordId + "&auditId=" + updateAuditId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "changeReason": "undo update"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RESTORED"))
				.andExpect(jsonPath("$.data.detail.fields[0].value").value("71.8"));

		mockMvc.perform(get("/api/v1/records/detail?type=WEIGHT&recordId=" + weightRecordId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fields[0].value").value("71.8"));

		mockMvc.perform(get("/api/v1/records/center?types=WEIGHT&limit=5")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.returnedCount").value(1))
				.andExpect(jsonPath("$.data.items[0].recordId").value(weightRecordId));

		mockMvc.perform(get("/api/v1/records/timeline")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.events[0].type").isNotEmpty())
				.andExpect(jsonPath("$.data.events[0].riskLevel").isNotEmpty());

		mockMvc.perform(get("/api/v1/reminders")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].title").isNotEmpty())
				.andExpect(jsonPath("$.data[?(@.type=='BLOOD_PRESSURE')]").isArray());
	}

	@Test
	void shouldRejectMetricOutsideDeviceWhitelist() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/mock-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nickname": "device-guard-user"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data")
				.path("token")
				.asText();

		MvcResult bindDeviceResult = mockMvc.perform(post("/api/v1/devices")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "deviceType": "URIC_ACID_METER",
								  "vendorName": "Tongfeng",
								  "deviceModel": "UA-1",
								  "serialNumber": "UA-SN-GUARD-001",
								  "aliasName": "ua-meter"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		String deviceCode = objectMapper.readTree(bindDeviceResult.getResponse().getContentAsString())
				.path("data")
				.path("deviceCode")
				.asText();

		mockMvc.perform(post("/api/v1/devices/{deviceCode}/sync", deviceCode)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "items": [
								    {
								      "metricType": "BLOOD_PRESSURE",
								      "externalEventId": "evt-bp-blocked-1",
								      "measuredAt": "2026-04-29T10:05:00Z",
								      "systolicPressure": 142,
								      "diastolicPressure": 91,
								      "pulseRate": 80,
								      "unit": "mmHg",
								      "note": "should be blocked"
								    }
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DEVICE_METRIC_NOT_ALLOWED"));
	}
}
