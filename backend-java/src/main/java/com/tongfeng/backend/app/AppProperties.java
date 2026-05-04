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
	private int authVerificationCodeMinutes = 15;
	private boolean authExposeVerificationCode = true;
	private int authVerificationResendCooldownSeconds = 60;
	private int authVerificationRateLimitWindowMinutes = 30;
	private int authVerificationMaxRequestsPerWindow = 5;
	private int authVerificationMaxAttemptsPerCode = 5;
	private int authKnownDeviceDays = 90;
	private int authLoginFailureThreshold = 5;
	private int authLoginFailureLockMinutes = 15;
	private boolean authEmailEnabled = false;
	private String authEmailFrom = "no-reply@tongfeng.local";
	private String authEmailSubjectPrefix = "[痛风主动管理]";
	private String authEmailSmtpHost = "";
	private int authEmailSmtpPort = 587;
	private String authEmailSmtpUsername = "";
	private String authEmailSmtpPassword = "";
	private boolean authEmailStarttlsEnabled = true;
	private boolean authSmsEnabled = false;
	private String authSmsWebhookUrl = "";
	private String authSmsBearerToken = "";
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

	public int getAuthVerificationCodeMinutes() {
		return authVerificationCodeMinutes;
	}

	public void setAuthVerificationCodeMinutes(int authVerificationCodeMinutes) {
		this.authVerificationCodeMinutes = authVerificationCodeMinutes;
	}

	public boolean isAuthExposeVerificationCode() {
		return authExposeVerificationCode;
	}

	public void setAuthExposeVerificationCode(boolean authExposeVerificationCode) {
		this.authExposeVerificationCode = authExposeVerificationCode;
	}

	public int getAuthVerificationResendCooldownSeconds() {
		return authVerificationResendCooldownSeconds;
	}

	public void setAuthVerificationResendCooldownSeconds(int authVerificationResendCooldownSeconds) {
		this.authVerificationResendCooldownSeconds = authVerificationResendCooldownSeconds;
	}

	public int getAuthVerificationRateLimitWindowMinutes() {
		return authVerificationRateLimitWindowMinutes;
	}

	public void setAuthVerificationRateLimitWindowMinutes(int authVerificationRateLimitWindowMinutes) {
		this.authVerificationRateLimitWindowMinutes = authVerificationRateLimitWindowMinutes;
	}

	public int getAuthVerificationMaxRequestsPerWindow() {
		return authVerificationMaxRequestsPerWindow;
	}

	public void setAuthVerificationMaxRequestsPerWindow(int authVerificationMaxRequestsPerWindow) {
		this.authVerificationMaxRequestsPerWindow = authVerificationMaxRequestsPerWindow;
	}

	public int getAuthVerificationMaxAttemptsPerCode() {
		return authVerificationMaxAttemptsPerCode;
	}

	public void setAuthVerificationMaxAttemptsPerCode(int authVerificationMaxAttemptsPerCode) {
		this.authVerificationMaxAttemptsPerCode = authVerificationMaxAttemptsPerCode;
	}

	public int getAuthKnownDeviceDays() {
		return authKnownDeviceDays;
	}

	public void setAuthKnownDeviceDays(int authKnownDeviceDays) {
		this.authKnownDeviceDays = authKnownDeviceDays;
	}

	public int getAuthLoginFailureThreshold() {
		return authLoginFailureThreshold;
	}

	public void setAuthLoginFailureThreshold(int authLoginFailureThreshold) {
		this.authLoginFailureThreshold = authLoginFailureThreshold;
	}

	public int getAuthLoginFailureLockMinutes() {
		return authLoginFailureLockMinutes;
	}

	public void setAuthLoginFailureLockMinutes(int authLoginFailureLockMinutes) {
		this.authLoginFailureLockMinutes = authLoginFailureLockMinutes;
	}

	public boolean isAuthEmailEnabled() {
		return authEmailEnabled;
	}

	public void setAuthEmailEnabled(boolean authEmailEnabled) {
		this.authEmailEnabled = authEmailEnabled;
	}

	public String getAuthEmailFrom() {
		return authEmailFrom;
	}

	public void setAuthEmailFrom(String authEmailFrom) {
		this.authEmailFrom = authEmailFrom;
	}

	public String getAuthEmailSubjectPrefix() {
		return authEmailSubjectPrefix;
	}

	public void setAuthEmailSubjectPrefix(String authEmailSubjectPrefix) {
		this.authEmailSubjectPrefix = authEmailSubjectPrefix;
	}

	public String getAuthEmailSmtpHost() {
		return authEmailSmtpHost;
	}

	public void setAuthEmailSmtpHost(String authEmailSmtpHost) {
		this.authEmailSmtpHost = authEmailSmtpHost;
	}

	public int getAuthEmailSmtpPort() {
		return authEmailSmtpPort;
	}

	public void setAuthEmailSmtpPort(int authEmailSmtpPort) {
		this.authEmailSmtpPort = authEmailSmtpPort;
	}

	public String getAuthEmailSmtpUsername() {
		return authEmailSmtpUsername;
	}

	public void setAuthEmailSmtpUsername(String authEmailSmtpUsername) {
		this.authEmailSmtpUsername = authEmailSmtpUsername;
	}

	public String getAuthEmailSmtpPassword() {
		return authEmailSmtpPassword;
	}

	public void setAuthEmailSmtpPassword(String authEmailSmtpPassword) {
		this.authEmailSmtpPassword = authEmailSmtpPassword;
	}

	public boolean isAuthEmailStarttlsEnabled() {
		return authEmailStarttlsEnabled;
	}

	public void setAuthEmailStarttlsEnabled(boolean authEmailStarttlsEnabled) {
		this.authEmailStarttlsEnabled = authEmailStarttlsEnabled;
	}

	public boolean isAuthSmsEnabled() {
		return authSmsEnabled;
	}

	public void setAuthSmsEnabled(boolean authSmsEnabled) {
		this.authSmsEnabled = authSmsEnabled;
	}

	public String getAuthSmsWebhookUrl() {
		return authSmsWebhookUrl;
	}

	public void setAuthSmsWebhookUrl(String authSmsWebhookUrl) {
		this.authSmsWebhookUrl = authSmsWebhookUrl;
	}

	public String getAuthSmsBearerToken() {
		return authSmsBearerToken;
	}

	public void setAuthSmsBearerToken(String authSmsBearerToken) {
		this.authSmsBearerToken = authSmsBearerToken;
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
