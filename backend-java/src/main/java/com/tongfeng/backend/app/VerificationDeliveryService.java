package com.tongfeng.backend.app;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VerificationDeliveryService {

	private final AppProperties appProperties;
	private final RestClient restClient;

	public VerificationDeliveryService(AppProperties appProperties) {
		this.appProperties = appProperties;
		this.restClient = RestClient.create();
	}

	public DeliveryResult deliver(
			String purpose,
			String accountType,
			String principal,
			String maskedTarget,
			String verificationCode,
			Instant expiresAt
	) {
		String deliveryChannel = "EMAIL".equals(accountType) ? "EMAIL" : "SMS";
		if ("EMAIL".equals(deliveryChannel)) {
			return deliverEmail(purpose, principal, maskedTarget, verificationCode, expiresAt);
		}
		return deliverSms(purpose, principal, maskedTarget, verificationCode, expiresAt);
	}

	private DeliveryResult deliverEmail(
			String purpose,
			String email,
			String maskedTarget,
			String verificationCode,
			Instant expiresAt
	) {
		if (appProperties.isAuthEmailEnabled() && StringUtils.hasText(appProperties.getAuthEmailSmtpHost())) {
			try {
				JavaMailSenderImpl sender = buildMailSender();
				SimpleMailMessage message = new SimpleMailMessage();
				message.setFrom(appProperties.getAuthEmailFrom());
				message.setTo(email);
				message.setSubject(appProperties.getAuthEmailSubjectPrefix() + " " + subjectText(purpose));
				message.setText(buildMessageBody(purpose, maskedTarget, verificationCode, expiresAt));
				sender.send(message);
				return new DeliveryResult(
						"EMAIL",
						"SMTP_EMAIL",
						"DELIVERED",
						appProperties.isAuthExposeVerificationCode() ? verificationCode : null,
						"验证码已通过邮件发送到 " + maskedTarget
				);
			} catch (MailException ex) {
				return handleDeliveryFallback("EMAIL", verificationCode, "邮件发送失败：" + ex.getMessage());
			}
		}
		return handleDeliveryFallback("EMAIL", verificationCode, "当前未配置邮件投递通道，已切换为联调模式");
	}

	private DeliveryResult deliverSms(
			String purpose,
			String principal,
			String maskedTarget,
			String verificationCode,
			Instant expiresAt
	) {
		if (appProperties.isAuthSmsEnabled() && StringUtils.hasText(appProperties.getAuthSmsWebhookUrl())) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("purpose", purpose);
			body.put("phone", principal);
			body.put("maskedTarget", maskedTarget);
			body.put("verificationCode", verificationCode);
			body.put("expiresAt", expiresAt.toString());
			body.put("message", buildMessageBody(purpose, maskedTarget, verificationCode, expiresAt));
			try {
				RestClient.RequestBodySpec request = restClient.post()
						.uri(appProperties.getAuthSmsWebhookUrl())
						.contentType(MediaType.APPLICATION_JSON);
				if (StringUtils.hasText(appProperties.getAuthSmsBearerToken())) {
					request.header("Authorization", "Bearer " + appProperties.getAuthSmsBearerToken());
				}
				request.body(body).retrieve().toBodilessEntity();
				return new DeliveryResult(
						"SMS",
						"SMS_WEBHOOK",
						"DELIVERED",
						appProperties.isAuthExposeVerificationCode() ? verificationCode : null,
						"验证码已通过短信发送到 " + maskedTarget
				);
			} catch (RestClientException ex) {
				return handleDeliveryFallback("SMS", verificationCode, "短信发送失败：" + ex.getMessage());
			}
		}
		return handleDeliveryFallback("SMS", verificationCode, "当前未配置短信投递通道，已切换为联调模式");
	}

	private DeliveryResult handleDeliveryFallback(String channel, String verificationCode, String fallbackReason) {
		if (appProperties.isAuthExposeVerificationCode()) {
			return new DeliveryResult(
					channel,
					"SIMULATED",
					"SIMULATED",
					verificationCode,
					fallbackReason + "。当前响应中会返回模拟验证码，便于联调。"
			);
		}
		throw new BusinessException("VERIFICATION_DELIVERY_UNAVAILABLE", fallbackReason + "，请先配置真实投递通道后再试");
	}

	private JavaMailSenderImpl buildMailSender() {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(appProperties.getAuthEmailSmtpHost());
		sender.setPort(appProperties.getAuthEmailSmtpPort());
		sender.setUsername(appProperties.getAuthEmailSmtpUsername());
		sender.setPassword(appProperties.getAuthEmailSmtpPassword());
		Properties properties = sender.getJavaMailProperties();
		properties.put("mail.smtp.auth", String.valueOf(
				StringUtils.hasText(appProperties.getAuthEmailSmtpUsername())
						|| StringUtils.hasText(appProperties.getAuthEmailSmtpPassword())
		));
		properties.put("mail.smtp.starttls.enable", String.valueOf(appProperties.isAuthEmailStarttlsEnabled()));
		return sender;
	}

	private String subjectText(String purpose) {
		if ("ACCOUNT_VERIFY".equals(purpose)) {
			return "账号验证";
		}
		return "找回密码";
	}

	private String buildMessageBody(String purpose, String maskedTarget, String verificationCode, Instant expiresAt) {
		return "你好，" + maskedTarget + "。\n\n"
				+ ("ACCOUNT_VERIFY".equals(purpose) ? "你正在进行账号验证。" : "你正在进行密码重置。") + "\n"
				+ "验证码：" + verificationCode + "\n"
				+ "失效时间：" + expiresAt + "\n"
				+ "如果这不是你本人操作，请忽略本消息并尽快检查账号安全。";
	}

	public record DeliveryResult(
			String deliveryChannel,
			String deliveryProvider,
			String deliveryStatus,
			String exposedCode,
			String message
	) {
	}
}
