package com.tongfeng.backend.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@org.springframework.stereotype.Component
public class AiServiceClient {

	private final RestClient restClient;

	public AiServiceClient(AppProperties appProperties) {
		this.restClient = RestClient.builder()
				.baseUrl(appProperties.getAiBaseUrl())
				.build();
	}

	public MealAiResult analyzeMeal(String userId, String mealType, String note, MultipartFile file) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new NamedByteArrayResource(readBytes(file), file.getOriginalFilename()));
		body.add("userId", userId);
		body.add("mealType", mealType);
		body.add("note", note == null ? "" : note);
		try {
			return restClient.post()
					.uri("/api/v1/vision/meal-analyze")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(MealAiResult.class);
		} catch (RestClientException ex) {
			throw new BusinessException("AI_MEAL_ERROR", "餐盘识别服务不可用: " + ex.getMessage());
		}
	}

	public LabAiResult analyzeLabReport(String userId, LocalDate reportDate, MultipartFile file) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new NamedByteArrayResource(readBytes(file), file.getOriginalFilename()));
		body.add("userId", userId);
		body.add("reportDate", reportDate == null ? "" : reportDate.toString());
		try {
			return restClient.post()
					.uri("/api/v1/ocr/lab-report-analyze")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(LabAiResult.class);
		} catch (RestClientException ex) {
			throw new BusinessException("AI_LAB_ERROR", "化验单解析服务不可用: " + ex.getMessage());
		}
	}

	public AppContracts.KnowledgeAnswerResponse askKnowledge(String question, String scene) {
		try {
			return restClient.post()
					.uri("/api/v1/knowledge/ask")
					.contentType(MediaType.APPLICATION_JSON)
					.body(new KnowledgeAskRequest(question, scene))
					.retrieve()
					.body(AppContracts.KnowledgeAnswerResponse.class);
		} catch (RestClientException ex) {
			return buildFallbackKnowledgeAnswer(question, scene, ex);
		}
	}

	private AppContracts.KnowledgeAnswerResponse buildFallbackKnowledgeAnswer(
			String question,
			String scene,
			RestClientException ex
	) {
		String sceneText = scene == null || scene.isBlank() ? "当前场景" : scene.trim();
		String answer = "知识问答服务当前未连接，已切换到本地兜底建议。"
				+ "针对“" + question + "”，建议先遵循低嘌呤饮食、规律补水、避免酒精暴露，并结合最近尿酸与发作记录综合判断。"
				+ "如果你正处于" + sceneText + "，且出现持续疼痛、红肿加重或发热，应及时线下就医。";
		return new AppContracts.KnowledgeAnswerResponse(
				answer,
				List.of("fallback://local-knowledge"),
				false,
				"当前回答为本地兜底建议，因 AI 子服务暂不可用: " + ex.getMessage()
		);
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (java.io.IOException ex) {
			throw new BusinessException("FILE_READ_ERROR", "读取上传文件失败: " + ex.getMessage());
		}
	}

	private record KnowledgeAskRequest(String question, String scene) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record MealAiResult(
			AppContracts.RiskLevel overallRiskLevel,
			Integer purineEstimateMg,
			List<AppContracts.MealItem> items,
			List<String> suggestions,
			String summary
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LabAiResult(
			List<AppContracts.LabIndicator> indicators,
			AppContracts.RiskLevel overallRiskLevel,
			List<String> suggestions,
			String summary
	) {
	}

	private static final class NamedByteArrayResource extends ByteArrayResource {

		private final String filename;

		private NamedByteArrayResource(byte[] byteArray, String filename) {
			super(byteArray);
			this.filename = filename == null ? "upload.bin" : filename;
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}
}
