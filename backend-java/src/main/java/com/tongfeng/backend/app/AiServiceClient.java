package com.tongfeng.backend.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@org.springframework.stereotype.Component
public class AiServiceClient {

	public static final String MEAL_ANALYSIS_MODE_AI_VISION = "AI_VISION";
	public static final String MEAL_ANALYSIS_MODE_SAFE_FALLBACK = "SAFE_FALLBACK";
	public static final String LAB_ANALYSIS_MODE_AI_OCR = "AI_OCR";
	public static final String LAB_ANALYSIS_MODE_SAFE_FALLBACK = "SAFE_FALLBACK";

	private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

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
			MealAiResult result = restClient.post()
					.uri("/api/v1/vision/meal-analyze")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(MealAiResult.class);
			if (result == null) {
				log.warn("Meal AI returned empty body, switch to SAFE_FALLBACK");
				return buildFallbackMealResult();
			}
			return new MealAiResult(
					result.overallRiskLevel(),
					result.purineEstimateMg(),
					result.items(),
					result.suggestions(),
					result.summary(),
					defaultMode(result.analysisMode(), MEAL_ANALYSIS_MODE_AI_VISION),
					safeList(result.trustNotes())
			);
		} catch (RestClientException ex) {
			log.warn("Meal AI unavailable, switch to SAFE_FALLBACK: {}", ex.getMessage());
			return buildFallbackMealResult();
		}
	}

	public LabAiResult analyzeLabReport(String userId, LocalDate reportDate, MultipartFile file) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new NamedByteArrayResource(readBytes(file), file.getOriginalFilename()));
		body.add("userId", userId);
		body.add("reportDate", reportDate == null ? "" : reportDate.toString());
		try {
			LabAiResult result = restClient.post()
					.uri("/api/v1/ocr/lab-report-analyze")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(LabAiResult.class);
			if (result == null) {
				log.warn("Lab AI returned empty body, switch to SAFE_FALLBACK");
				return buildFallbackLabResult();
			}
			return new LabAiResult(
					result.indicators(),
					result.overallRiskLevel(),
					result.suggestions(),
					result.summary(),
					defaultMode(result.analysisMode(), LAB_ANALYSIS_MODE_AI_OCR),
					safeList(result.trustNotes())
			);
		} catch (RestClientException ex) {
			log.warn("Lab AI unavailable, switch to SAFE_FALLBACK: {}", ex.getMessage());
			return buildFallbackLabResult();
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

	private MealAiResult buildFallbackMealResult() {
		return new MealAiResult(
				AppContracts.RiskLevel.YELLOW,
				null,
				List.of(),
				List.of(
						"AI 图像识别当前不可用，本次不会输出正式食材识别结果。",
						"请手动补充是否包含酒精、海鲜、动物内脏、浓肉汤等高风险食材。",
						"如处于发作期，请先按低嘌呤饮食、规律补水和避免饮酒处理。"
				),
				"当前已切换到安全兜底模式：本次仅保留上传记录，不输出正式食材识别结论或嘌呤估算。",
				MEAL_ANALYSIS_MODE_SAFE_FALLBACK,
				List.of(
						"本次未调用到 AI 图像识别服务，系统没有对食材做正式识别。",
						"请结合原图和实际进食内容，手动判断是否存在高风险饮食暴露。"
				)
		);
	}

	private LabAiResult buildFallbackLabResult() {
		return new LabAiResult(
				List.of(),
				AppContracts.RiskLevel.YELLOW,
				List.of(
						"AI 子服务当前不可用，本次不会估算或补造化验指标。",
						"请重新上传更清晰的报告，或直接手动补录多个关键指标后再继续复盘。",
						"在人工确认前，不要把当前结果当作正式化验结论或趋势依据。"
				),
				"当前已切换到安全兜底模式：系统保留原始报告文件，但不会输出正式 OCR 结论。",
				LAB_ANALYSIS_MODE_SAFE_FALLBACK,
				List.of(
						"本次未调用到 AI OCR 服务，系统没有生成任何估算化验值。",
						"当前化验单将直接进入人工确认流程，完成补录后再生成正式复盘。"
				)
		);
	}

	private String defaultMode(String mode, String fallback) {
		return mode == null || mode.isBlank() ? fallback : mode;
	}

	private <T> List<T> safeList(List<T> items) {
		return items == null ? List.of() : items;
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
			String summary,
			String analysisMode,
			List<String> trustNotes
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LabAiResult(
			List<AppContracts.LabIndicator> indicators,
			AppContracts.RiskLevel overallRiskLevel,
			List<String> suggestions,
			String summary,
			String analysisMode,
			List<String> trustNotes
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
