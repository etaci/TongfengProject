package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonCodec {

	private final ObjectMapper objectMapper;

	public JsonCodec(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new BusinessException("JSON_ENCODE_ERROR", "JSON 编码失败: " + ex.getMessage());
		}
	}

	public <T> T fromJson(String json, Class<T> type) {
		try {
			return objectMapper.readValue(json, type);
		} catch (JsonProcessingException ex) {
			throw new BusinessException("JSON_DECODE_ERROR", "JSON 解码失败: " + ex.getMessage());
		}
	}

	public <T> T fromJson(String json, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(json, typeReference);
		} catch (JsonProcessingException ex) {
			throw new BusinessException("JSON_DECODE_ERROR", "JSON 解码失败: " + ex.getMessage());
		}
	}
}
