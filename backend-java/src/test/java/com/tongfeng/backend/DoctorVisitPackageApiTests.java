package com.tongfeng.backend;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class DoctorVisitPackageApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void shouldGeneratePrintPdfAndRevocableDoctorShare() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/mock-login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nickname\":\"doctor-package-user\"}"))
				.andExpect(status().isOk())
				.andReturn();
		String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data").path("token").asText();

		mockMvc.perform(get("/api/v1/doctor-visit-packages?days=30")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lookbackDays").value(30))
				.andExpect(jsonPath("$.data.dataSources[0]").isNotEmpty())
				.andExpect(jsonPath("$.data.trustNotes[0]").isNotEmpty());

		mockMvc.perform(get("/api/v1/doctor-visit-packages/print?days=30")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/html")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("医生就诊包")));

		MvcResult pdf = mockMvc.perform(get("/api/v1/doctor-visit-packages/pdf?days=30")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("doctor-visit-package.pdf")))
				.andReturn();
		org.junit.jupiter.api.Assertions.assertTrue(pdf.getResponse().getContentAsByteArray().length > 1000);
		org.junit.jupiter.api.Assertions.assertEquals("%PDF-1.4", new String(pdf.getResponse().getContentAsByteArray(), 0, 8));

		MvcResult shareResult = mockMvc.perform(post("/api/v1/doctor-visit-shares")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"lookbackDays\":30,\"expiresInHours\":24}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.shareToken").isNotEmpty())
				.andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
				.andReturn();
		JsonNode share = objectMapper.readTree(shareResult.getResponse().getContentAsString()).path("data");
		String shareToken = share.path("shareToken").asText();
		String shareCode = share.path("shareCode").asText();

		mockMvc.perform(get("/api/public/doctor-visit-shares/" + shareToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lookbackDays").value(30));

		mockMvc.perform(delete("/api/v1/doctor-visit-shares/" + shareCode)
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.revoked").value(true));

		mockMvc.perform(get("/api/public/doctor-visit-shares/" + shareToken))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("DOCTOR_SHARE_EXPIRED"));
	}
}
