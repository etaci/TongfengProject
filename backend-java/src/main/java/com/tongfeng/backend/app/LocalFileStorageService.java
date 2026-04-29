package com.tongfeng.backend.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

	private final AppProperties appProperties;

	public LocalFileStorageService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public StoredPhysicalFile save(String fileCode, MultipartFile file) {
		if (file.isEmpty()) {
			throw new BusinessException("EMPTY_FILE", "上传文件不能为空");
		}
		String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
		String safeName = originalName.replace("\\", "_").replace("/", "_");
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

	public record StoredPhysicalFile(
			String fileName,
			String contentType,
			long size,
			String relativePath
	) {
	}
}
