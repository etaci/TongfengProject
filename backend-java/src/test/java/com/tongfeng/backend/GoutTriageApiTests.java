package com.tongfeng.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongfeng.backend.app.persistence.entity.ClinicalDecisionAuditEntity;
import com.tongfeng.backend.app.persistence.repo.ClinicalDecisionAuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class GoutTriageApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClinicalDecisionAuditRepository auditRepository;

	@Test
	void shouldEvaluateStructuredGoutFlareAndPersistDecisionAudit() throws Exception {
		LoginSession session = login("gout-triage-user");

		JsonNode infectionUrgent = submitTriage(session.token(), questionnaire(8, true, true, true, false, false, false, false));
		assertEquals("URGENT_OFFLINE", infectionUrgent.path("triageCode").asText());
		assertEquals("RED", infectionUrgent.path("triageLevel").asText());
		assertTrue(infectionUrgent.path("redFlags").get(0).asText().contains("关节感染"));

		JsonNode injuryUrgent = submitTriage(session.token(), questionnaire(6, false, false, false, false, true, false, false));
		assertEquals("URGENT_OFFLINE", injuryUrgent.path("triageCode").asText());

		JsonNode firstEpisode = submitTriage(session.token(), questionnaire(4, false, false, true, false, false, true, false));
		assertEquals("CONTACT_DOCTOR_SOON", firstEpisode.path("triageCode").asText());
		assertTrue(firstEpisode.path("reasons").get(0).asText().contains("首次"));

		JsonNode medicationChange = submitTriage(session.token(), questionnaire(7, false, false, true, true, false, false, false));
		assertEquals("CONTACT_DOCTOR_SOON", medicationChange.path("triageCode").asText());

		String selfManagementRequest = questionnaire(3, false, false, true, false, false, false, false);
		JsonNode selfManagement = submitTriage(session.token(), selfManagementRequest);
		JsonNode repeatedSelfManagement = submitTriage(session.token(), selfManagementRequest);
		assertEquals("SELF_MANAGEMENT", selfManagement.path("triageCode").asText());
		assertEquals("GREEN", selfManagement.path("triageLevel").asText());
		assertEquals(selfManagement.path("triageCode"), repeatedSelfManagement.path("triageCode"));
		assertEquals(selfManagement.path("summary"), repeatedSelfManagement.path("summary"));
		assertEquals(selfManagement.path("reasons"), repeatedSelfManagement.path("reasons"));

		assertResponseTrustMetadata(selfManagement);
		String decisionCode = selfManagement.path("decisionCode").asText();
		ClinicalDecisionAuditEntity audit = auditRepository
				.findByDecisionCodeAndUserCode(decisionCode, session.userId())
				.orElseThrow();
		assertEquals("GOUT_FLARE_TRIAGE", audit.getDecisionType());
		assertEquals("SELF_MANAGEMENT", audit.getDecisionResult());
		assertEquals(selfManagement.path("ruleVersion").asText(), audit.getRuleVersion());
		assertTrue(audit.getInputSnapshotJson().contains("jointLocation"));
		assertTrue(audit.getSourceReferencesJson().contains("NICE_NG219"));
		assertTrue(audit.getDecisionPayloadJson().contains(decisionCode));
		assertFalse(audit.isManualReviewed());
	}

	@Test
	void shouldPublishTriageToTodayTimelineAndReminders() throws Exception {
		LoginSession session = login("gout-triage-journey-user");
		JsonNode triage = submitTriage(
				session.token(),
				questionnaire(9, true, true, false, false, false, false, true)
		);
		String decisionCode = triage.path("decisionCode").asText();

		mockMvc.perform(get("/api/v1/home/today")
					.header("Authorization", "Bearer " + session.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.triageCode").value("URGENT_OFFLINE"))
				.andExpect(jsonPath("$.data.triageDecisionCode").value(decisionCode))
				.andExpect(jsonPath("$.data.triageRuleVersion").value("GOUT_FLARE_TRIAGE_V1.0.0"))
				.andExpect(jsonPath("$.data.triageVerificationStatus").value("RULE_EVALUATED_NOT_CLINICIAN_REVIEWED"))
				.andExpect(jsonPath("$.data.triageRedFlags[0]").isNotEmpty())
				.andExpect(jsonPath("$.data.actions[0].category").value("GOUT_TRIAGE"));

		mockMvc.perform(get("/api/v1/records/timeline")
					.header("Authorization", "Bearer " + session.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.events[0].type").value("GOUT_TRIAGE"))
				.andExpect(jsonPath("$.data.events[0].decisionCode").value(decisionCode))
				.andExpect(jsonPath("$.data.events[0].redFlags[0]").isNotEmpty())
				.andExpect(jsonPath("$.data.events[0].nextActions[0]").isNotEmpty());

		mockMvc.perform(get("/api/v1/reminders")
					.header("Authorization", "Bearer " + session.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].type").value("GOUT_TRIAGE"))
				.andExpect(jsonPath("$.data[0].riskLevel").value("RED"));
	}

	@Test
	void shouldRejectIncompleteOrInvalidTriageQuestionnaire() throws Exception {
		LoginSession session = login("gout-triage-validation-user");

		mockMvc.perform(post("/api/v1/triage/gout-flare")
					.header("Authorization", "Bearer " + session.token())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "onsetAt": "2026-07-21T08:00:00Z",
							  "jointLocation": "",
							  "painLevel": 11
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	private JsonNode submitTriage(String token, String requestBody) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/triage/gout-flare")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
	}

	private void assertResponseTrustMetadata(JsonNode response) {
		assertFalse(response.path("decisionCode").asText().isBlank());
		assertEquals("GOUT_FLARE_TRIAGE_V1.0.0", response.path("ruleVersion").asText());
		assertTrue(response.path("sourceReferences").size() >= 3);
		assertEquals("RULE_EVALUATED_NOT_CLINICIAN_REVIEWED", response.path("verificationStatus").asText());
		assertFalse(response.path("generatedAt").asText().isBlank());
		assertTrue(response.path("disclaimer").asText().contains("不构成诊断"));
		assertTrue(response.path("nextActions").size() >= 2);
	}

	private String questionnaire(
			int painLevel,
			boolean rednessOrSwelling,
			boolean fever,
			boolean canBearWeight,
			boolean recentMedicationChange,
			boolean traumaHistory,
			boolean firstEpisode,
			boolean systemicSymptoms
	) {
		return """
				{
				  "onsetAt": "2026-07-21T08:00:00Z",
				  "jointLocation": "left-big-toe",
				  "painLevel": %d,
				  "rednessOrSwelling": %s,
				  "fever": %s,
				  "canBearWeight": %s,
				  "recentMedicationChange": %s,
				  "traumaHistory": %s,
				  "firstEpisode": %s,
				  "systemicSymptoms": %s
				}
				""".formatted(
				painLevel,
				rednessOrSwelling,
				fever,
				canBearWeight,
				recentMedicationChange,
				traumaHistory,
				firstEpisode,
				systemicSymptoms
		);
	}

	private LoginSession login(String nickname) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/mock-login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nickname\":\"" + nickname + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
		return new LoginSession(data.path("token").asText(), data.path("userId").asText());
	}

	private record LoginSession(String token, String userId) {
	}
}
