package com.tongfeng.backend.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "医生就诊包")
public class DoctorVisitPackageController {

	private final DoctorVisitPackageService packageService;
	private final DoctorVisitPdfRenderer pdfRenderer;

	public DoctorVisitPackageController(DoctorVisitPackageService packageService, DoctorVisitPdfRenderer pdfRenderer) {
		this.packageService = packageService;
		this.pdfRenderer = pdfRenderer;
	}

	@GetMapping(value = "/api/v1/doctor-visit-packages/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "下载医生就诊包 PDF")
	public ResponseEntity<byte[]> downloadPdf(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "30") @Min(30) @Max(90) int days
	) {
		byte[] pdf = pdfRenderer.render(packageService.buildPackage(userId, days));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=doctor-visit-package.pdf")
				.body(pdf);
	}

	@GetMapping("/api/v1/doctor-visit-packages")
	public ApiResponse<AppContracts.DoctorVisitPackageResponse> getPackage(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "30") @Min(30) @Max(90) int days
	) {
		return ApiResponse.success(packageService.buildPackage(userId, days));
	}

	@GetMapping(value = "/api/v1/doctor-visit-packages/print", produces = MediaType.TEXT_HTML_VALUE)
	@Operation(summary = "获取可打印就诊包", description = "返回打印友好的 HTML，可直接打印或由浏览器另存为 PDF。")
	public ResponseEntity<String> printPackage(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestParam(defaultValue = "30") @Min(30) @Max(90) int days
	) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
				.body(packageService.renderPrintHtml(packageService.buildPackage(userId, days)));
	}

	@PostMapping("/api/v1/doctor-visit-shares")
	public ApiResponse<AppContracts.DoctorVisitShareResponse> createShare(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.DoctorVisitShareCreateRequest request
	) {
		return ApiResponse.success(packageService.createShare(userId, request));
	}

	@GetMapping("/api/public/doctor-visit-shares/{shareToken}")
	@SecurityRequirements
	public ApiResponse<AppContracts.DoctorVisitPackageResponse> readShare(@PathVariable String shareToken) {
		return ApiResponse.success(packageService.readSharedPackage(shareToken));
	}

	@DeleteMapping("/api/v1/doctor-visit-shares/{shareCode}")
	public ApiResponse<AppContracts.DoctorVisitShareResponse> revokeShare(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@PathVariable String shareCode
	) {
		return ApiResponse.success(packageService.revokeShare(userId, shareCode));
	}
}
