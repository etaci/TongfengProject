package com.tongfeng.backend.app;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeatureAccessService {

	private final AppProperties appProperties;

	public FeatureAccessService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public AppContracts.AppCapabilitiesResponse getCapabilities() {
		return new AppContracts.AppCapabilitiesResponse(List.of(
				new AppContracts.FeatureStatusResponse(
						"family-care",
						"家属协同",
						appProperties.isFamilyEnabled(),
						"用于授权绑定、家属摘要和轻量提醒。"
				)
		));
	}

	public void ensureFamilyEnabled() {
		ensureEnabled(appProperties.isFamilyEnabled(), "家属协同功能当前未开放，请稍后再试");
	}

	private void ensureEnabled(boolean enabled, String message) {
		if (!enabled) {
			throw new BusinessException("FEATURE_DISABLED", message);
		}
	}
}
