package com.tongfeng.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PrivacyDataRightsApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void shouldExportWithdrawAndPermanentlyDeleteOwnedData() throws Exception {
		MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "nickname": "privacy-rights-user",
							  "accountType": "EMAIL",
							  "account": "privacy-rights@example.com",
							  "password": "Password123",
							  "confirmPassword": "Password123",
							  "consent": {
							    "consentVersion": "v1.0",
							    "privacyPolicyVersion": "privacy-v1.1",
							    "privacyAccepted": true,
							    "termsAccepted": true,
							    "medicalDataAuthorized": true,
							    "familyCollaborationAuthorized": true,
							    "notificationAuthorized": true
							  }
							}
							"""))
				.andExpect(status().isOk())
				.andReturn();
		String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
				.path("data").path("token").asText();

		mockMvc.perform(post("/api/v1/records/uric-acid")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"value\":480,\"unit\":\"umol/L\",\"source\":\"manual\"}"))
				.andExpect(status().isOk());

		MvcResult exportResult = mockMvc.perform(get("/api/v1/privacy/data-export")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("health-data-export.zip")))
				.andReturn();
		String exportedJson = readZipEntry(exportResult.getResponse().getContentAsByteArray(), "data.json");
		assertFalse(exportedJson.contains("Password123"));
		assertFalse(exportedJson.contains(token));

		mockMvc.perform(post("/api/v1/privacy/consents/withdraw")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "scopes": ["MEDICAL_DATA", "FAMILY_COLLABORATION"],
							  "reason": "停止相关数据处理"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.medicalDataAuthorized").value(false))
				.andExpect(jsonPath("$.data.familyCollaborationAuthorized").value(false))
				.andExpect(jsonPath("$.data.notificationAuthorized").value(true))
				.andExpect(jsonPath("$.data.sourceType").value("WITHDRAWAL"));

		mockMvc.perform(delete("/api/v1/privacy/account")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"confirmation\":\"DELETE\"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(delete("/api/v1/privacy/account")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"confirmation\":\"DELETE_MY_ACCOUNT\",\"reason\":\"不再使用\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("COMPLETED"))
				.andExpect(jsonPath("$.data.deletedDatabaseRows").isNumber());

		mockMvc.perform(get("/api/v1/profile")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	private String readZipEntry(byte[] archive, String expectedName) throws Exception {
		try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (expectedName.equals(entry.getName())) {
					return new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
				}
			}
		}
		throw new AssertionError("ZIP 中缺少 " + expectedName);
	}

	@Test
	void shouldExposePublicPrivacyNotice() throws Exception {
		mockMvc.perform(get("/api/public/privacy-notice"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userRights[0]").isNotEmpty())
				.andExpect(jsonPath("$.data.medicalBoundary").isNotEmpty());
	}
}
