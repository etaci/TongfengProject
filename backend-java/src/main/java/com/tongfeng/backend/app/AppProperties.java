package com.tongfeng.backend.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private String aiBaseUrl = "http://localhost:8001";
	private String storageRoot = "./storage";
	private int authTokenDays = 30;
	private boolean redisEnabled = false;
	private String sessionCachePrefix = "tongfeng:session:";
	private boolean schedulerEnabled = true;
	private String reminderRefreshCron = "0 0/30 * * * *";
	private String summaryRefreshCron = "0 5 0 * * * *";
	private boolean weatherLiveEnabled = true;
	private String weatherGeocodingBaseUrl = "https://geocoding-api.open-meteo.com";
	private String weatherForecastBaseUrl = "https://api.open-meteo.com";
	private boolean familyEnabled = true;

	public String getAiBaseUrl() {
		return aiBaseUrl;
	}

	public void setAiBaseUrl(String aiBaseUrl) {
		this.aiBaseUrl = aiBaseUrl;
	}

	public String getStorageRoot() {
		return storageRoot;
	}

	public void setStorageRoot(String storageRoot) {
		this.storageRoot = storageRoot;
	}

	public int getAuthTokenDays() {
		return authTokenDays;
	}

	public void setAuthTokenDays(int authTokenDays) {
		this.authTokenDays = authTokenDays;
	}

	public boolean isRedisEnabled() {
		return redisEnabled;
	}

	public void setRedisEnabled(boolean redisEnabled) {
		this.redisEnabled = redisEnabled;
	}

	public String getSessionCachePrefix() {
		return sessionCachePrefix;
	}

	public void setSessionCachePrefix(String sessionCachePrefix) {
		this.sessionCachePrefix = sessionCachePrefix;
	}

	public boolean isSchedulerEnabled() {
		return schedulerEnabled;
	}

	public void setSchedulerEnabled(boolean schedulerEnabled) {
		this.schedulerEnabled = schedulerEnabled;
	}

	public String getReminderRefreshCron() {
		return reminderRefreshCron;
	}

	public void setReminderRefreshCron(String reminderRefreshCron) {
		this.reminderRefreshCron = reminderRefreshCron;
	}

	public String getSummaryRefreshCron() {
		return summaryRefreshCron;
	}

	public void setSummaryRefreshCron(String summaryRefreshCron) {
		this.summaryRefreshCron = summaryRefreshCron;
	}

	public boolean isWeatherLiveEnabled() {
		return weatherLiveEnabled;
	}

	public void setWeatherLiveEnabled(boolean weatherLiveEnabled) {
		this.weatherLiveEnabled = weatherLiveEnabled;
	}

	public String getWeatherGeocodingBaseUrl() {
		return weatherGeocodingBaseUrl;
	}

	public void setWeatherGeocodingBaseUrl(String weatherGeocodingBaseUrl) {
		this.weatherGeocodingBaseUrl = weatherGeocodingBaseUrl;
	}

	public String getWeatherForecastBaseUrl() {
		return weatherForecastBaseUrl;
	}

	public void setWeatherForecastBaseUrl(String weatherForecastBaseUrl) {
		this.weatherForecastBaseUrl = weatherForecastBaseUrl;
	}

	public boolean isFamilyEnabled() {
		return familyEnabled;
	}

	public void setFamilyEnabled(boolean familyEnabled) {
		this.familyEnabled = familyEnabled;
	}
}
