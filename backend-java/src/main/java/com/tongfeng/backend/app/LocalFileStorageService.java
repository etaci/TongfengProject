package com.tongfeng.backend.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

	private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);
	private static final int MAX_STORED_FILE_NAME_LENGTH = 180;

	private final AppProperties appProperties;

	public LocalFileStorageService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public StoredPhysicalFile save(String fileCode, MultipartFile file) {
		if (file.isEmpty()) {
			throw new BusinessException("EMPTY_FILE", "上传文件不能为空");
		}
		String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
		String safeName = sanitizeFileName(originalName);
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		Path relativePath = Paths.get("uploads", today.toString(), fileCode + "_" + safeName);
		Path rootPath = Paths.get(appProperties.getStorageRoot()).toAbsolutePath().normalize();
		Path targetPath = rootPath.resolve(relativePath).normalize();
		try {
			Files.createDirectories(targetPath.getParent());
			file.transferTo(targetPath);
		} catch (IOException ex) {
			throw new BusinessException("FILE_SAVE_ERROR", "保存文件失败: " + ex.getMessage());
		}
		return new StoredPhysicalFile(
				safeName,
				StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream",
				file.getSize(),
				relativePath.toString()
		);
	}

	public Resource loadAsResource(String relativePath) {
		Path targetPath = Paths.get(appProperties.getStorageRoot())
				.toAbsolutePath()
				.normalize()
				.resolve(relativePath)
				.normalize();
		Resource resource = new FileSystemResource(targetPath);
		if (!resource.exists()) {
			throw new BusinessException("FILE_NOT_FOUND", "文件已丢失");
		}
		return resource;
	}

	public void deleteQuietly(String relativePath) {
		if (!StringUtils.hasText(relativePath)) {
			return;
		}
		Path rootPath = Paths.get(appProperties.getStorageRoot()).toAbsolutePath().normalize();
		Path targetPath = rootPath.resolve(relativePath).normalize();
		if (!targetPath.startsWith(rootPath)) {
			log.warn("Skip deleting file outside storage root: {}", relativePath);
			return;
		}
		try {
			Files.deleteIfExists(targetPath);
		} catch (IOException ex) {
			log.warn("Failed to delete orphaned upload: {}", relativePath, ex);
		}
	}

	private String sanitizeFileName(String originalName) {
		String safeName = originalName
				.replace("\\", "_")
				.replace("/", "_")
				.replaceAll("[\\p{Cntrl}]", "_")
				.trim();
		if (!StringUtils.hasText(safeName)) {
			safeName = "upload.bin";
		}
		return safeName.length() <= MAX_STORED_FILE_NAME_LENGTH
				? safeName
				: safeName.substring(0, MAX_STORED_FILE_NAME_LENGTH);
	}

	public record StoredPhysicalFile(
			String fileName,
			String contentType,
			long size,
			String relativePath
	) {
	}
}
