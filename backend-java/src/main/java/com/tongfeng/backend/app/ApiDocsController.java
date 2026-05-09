package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiDocsController {

	private final OpenApiWebMvcResource openApiWebMvcResource;

	public ApiDocsController(OpenApiWebMvcResource openApiWebMvcResource) {
		this.openApiWebMvcResource = openApiWebMvcResource;
	}

	@GetMapping(value = "/api/openapi", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<byte[]> openApiJson(HttpServletRequest request, Locale locale) throws JsonProcessingException {
		byte[] body = openApiWebMvcResource.openapiJson(request, "/api/openapi", locale);
		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);
	}
}
