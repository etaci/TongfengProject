package com.tongfeng.backend.app;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeatureAccessService {

	private static final String CAPABILITY_VERSION = "2026-07-21";

	private final AppProperties appProperties;
	private final Instant generatedAt = Instant.now();

	public FeatureAccessService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public AppContracts.AppCapabilitiesResponse getCapabilities() {
		return new AppContracts.AppCapabilitiesResponse(List.of(
				new AppContracts.FeatureStatusResponse(
						"family-care",
						"家属轻协同",
						appProperties.isFamilyEnabled(),
						"主链路能力：患者授权、家属摘要、提醒与轻协同代办。"
				),
				new AppContracts.FeatureStatusResponse(
						"daily-records",
						"日常记录闭环",
						true,
						"主链路能力：饮食、尿酸、体重、饮水、发作记录与今日行动回流。"
				),
				new AppContracts.FeatureStatusResponse(
						"lab-report-review",
						"化验单可信复盘",
						true,
						"主链路能力：上传、可信状态、人工确认、正式复盘与医生摘要。"
				),
				new AppContracts.FeatureStatusResponse(
						"device-integration",
						"设备接入",
						false,
						"legacy / disabled / internal only：当前版本不对前端开放，不承诺联调。"
				),
				new AppContracts.FeatureStatusResponse(
						"growth-system",
						"成长体系",
						false,
						"legacy / disabled / internal only：当前版本不对前端开放，不承诺联调。"
				)
		), CAPABILITY_VERSION, generatedAt);
	}

	public void ensureFamilyEnabled() {
		ensureEnabled(appProperties.isFamilyEnabled(), "家属轻协同功能当前未开放，请稍后再试。");
	}

	private void ensureEnabled(boolean enabled, String message) {
		if (!enabled) {
			throw new BusinessException("FEATURE_DISABLED", message);
		}
	}
}
