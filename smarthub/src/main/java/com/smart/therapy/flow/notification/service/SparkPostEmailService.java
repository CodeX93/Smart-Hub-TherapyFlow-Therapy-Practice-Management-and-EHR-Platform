package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("!test")
@ConditionalOnProperty(name = "app.email.provider", havingValue = "sparkpost", matchIfMissing = false)
public class SparkPostEmailService implements NotificationService.EmailService, com.smart.therapy.flow.common.service.EmailProviderService {

    private static final String TRANSMISSIONS_ENDPOINT = "/transmissions";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final String baseUrl;

    public SparkPostEmailService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${email.sparkpost.api-key:}") String apiKey,
            @Value("${email.sparkpost.from-email:noreply@therapyflow.local}") String fromEmail,
            @Value("${email.sparkpost.from-name:SmartHub}") String fromName,
            @Value("${email.sparkpost.base-url:https://api.sparkpost.com/api/v1}") String baseUrl
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("SparkPost API key is required but not configured. Set email.sparkpost.api-key property or SPARKPOST_API_KEY environment variable.");
        }
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(5000); // 5 seconds in milliseconds
                    factory.setReadTimeout(15000); // 15 seconds in milliseconds
                    return factory;
                })
                .build();
    }

    @Override
    @CircuitBreaker(name = "sparkpost", fallbackMethod = "sendNotificationEmailFallback")
    @Retry(name = "sparkpost")
    public void sendNotificationEmail(User recipient,
                                      Notification inAppNotification,
                                      NotificationTemplate emailTemplate,
                                      Map<String, Object> entityData) {
        if (recipient == null || !StringUtils.hasText(recipient.getEmail())) {
            log.debug("Skipping email notification. Recipient email not available. correlationId={}", 
                    MDC.get("correlationId"));
            return;
        }

        String subject = renderTemplate(emailTemplate.getSubject(), entityData, "TherapyFlow Notification");
        String body = renderTemplate(emailTemplate.getBodyTemplate(), entityData, subject);

        try {
            log.debug("Sending email via SparkPost: userId={}, correlationId={}",
                    recipient.getId(), MDC.get("correlationId"));
            Map<String, Object> payload = buildPayload(recipient, subject, body, inAppNotification);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response =
                    restTemplate.exchange(baseUrl + TRANSMISSIONS_ENDPOINT, HttpMethod.POST, entity, Map.class);
            verifyAcceptedAndLog(response, maskEmail(recipient.getEmail()), subject);
        } catch (Exception ex) {
            log.error("SparkPost email delivery failed: userId={}, correlationId={}", 
                    recipient.getId(), MDC.get("correlationId"), ex);
            throw new RuntimeException("Failed to send email via SparkPost", ex);
        }
    }

    public void sendNotificationEmailFallback(User recipient,
                                              Notification inAppNotification,
                                              NotificationTemplate emailTemplate,
                                              Map<String, Object> entityData,
                                              Exception ex) {
        log.error("SparkPost circuit breaker fallback triggered: userId={}, correlationId={}", 
                recipient != null ? recipient.getId() : "unknown", MDC.get("correlationId"), ex);
        // Email will be queued for retry or logged for manual processing
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", apiKey);
        return headers;
    }

    private Map<String, Object> buildPayload(User recipient, String subject, String body, Notification inAppNotification) {
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> from = new HashMap<>();
        from.put("email", fromEmail);
        if (StringUtils.hasText(fromName)) {
            from.put("name", fromName);
        }
        content.put("from", from);
        content.put("subject", subject);
        content.put("html", body);

        Map<String, Object> recipientAddress = new HashMap<>();
        recipientAddress.put("email", recipient.getEmail());
        if (StringUtils.hasText(recipient.getFullName())) {
            recipientAddress.put("name", recipient.getFullName());
        }

        Map<String, Object> recipientWrapper = new HashMap<>();
        recipientWrapper.put("address", recipientAddress);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("recipients", List.of(recipientWrapper));

        if (inAppNotification != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("notificationId", inAppNotification.getId());
            metadata.put("notificationType", inAppNotification.getType());
            payload.put("metadata", metadata);
        }

        return payload;
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        if (!StringUtils.hasText(to)) {
            log.warn("Cannot send email: recipient email is empty, correlationId={}", MDC.get("correlationId"));
            return;
        }

        try {
            log.debug("Sending email via SparkPost: correlationId={}", MDC.get("correlationId"));
            
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> from = new HashMap<>();
            from.put("email", fromEmail);
            if (StringUtils.hasText(fromName)) {
                from.put("name", fromName);
            }
            content.put("from", from);
            content.put("subject", subject);
            content.put("html", htmlBody);

            Map<String, Object> recipientAddress = new HashMap<>();
            recipientAddress.put("email", to);

            Map<String, Object> recipientWrapper = new HashMap<>();
            recipientWrapper.put("address", recipientAddress);

            Map<String, Object> payload = new HashMap<>();
            payload.put("content", content);
            payload.put("recipients", List.of(recipientWrapper));

            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response =
                    restTemplate.exchange(baseUrl + TRANSMISSIONS_ENDPOINT, HttpMethod.POST, entity, Map.class);
            verifyAcceptedAndLog(response, maskEmail(to), subject);
        } catch (Exception ex) {
            log.error("SparkPost email delivery failed: correlationId={}", MDC.get("correlationId"), ex);
            throw new RuntimeException("Failed to send email via SparkPost", ex);
        }
    }

    @Override
    public String getFromAddress() {
        return fromEmail;
    }

    /**
     * SparkPost answers 200 even when it accepted nobody (e.g. every recipient is on the
     * account suppression list). Treating that as success made prod drops invisible: the log
     * said "sent successfully" while nothing was ever going to leave SparkPost. Read the
     * transmission result, keep the id so the send can be found in SparkPost's event explorer,
     * and fail loudly when no recipient was accepted so the caller records a FAILED delivery.
     */
    private void verifyAcceptedAndLog(ResponseEntity<Map> response, String maskedTo, String subject) {
        Object transmissionId = null;
        Number accepted = null;
        Number rejected = null;
        Object body = response != null ? response.getBody() : null;
        if (body instanceof Map<?, ?> bodyMap && bodyMap.get("results") instanceof Map<?, ?> results) {
            transmissionId = results.get("id");
            if (results.get("total_accepted_recipients") instanceof Number n) {
                accepted = n;
            }
            if (results.get("total_rejected_recipients") instanceof Number n) {
                rejected = n;
            }
        }

        if (rejected != null && rejected.intValue() > 0 && (accepted == null || accepted.intValue() == 0)) {
            throw new IllegalStateException("SparkPost rejected the recipient (transmissionId=" + transmissionId
                    + ", rejected=" + rejected + "); the address is likely suppressed");
        }
        if (accepted != null && accepted.intValue() == 0) {
            throw new IllegalStateException(
                    "SparkPost accepted no recipients (transmissionId=" + transmissionId + ")");
        }

        log.info("Email accepted by SparkPost: to={}, subject=\"{}\", transmissionId={}, correlationId={}",
                maskedTo, abbreviate(subject), transmissionId, MDC.get("correlationId"));
    }

    /** First char + *** + domain, so prod logs can be correlated without exposing the address. */
    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "[empty]";
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at > 0 && at < trimmed.length() - 1) {
            return trimmed.charAt(0) + "***@" + trimmed.substring(at + 1);
        }
        return "[masked]";
    }

    private static String abbreviate(String subject) {
        if (subject == null) {
            return "";
        }
        return subject.length() <= 80 ? subject : subject.substring(0, 77) + "...";
    }

    private String renderTemplate(String template, Map<String, Object> data, String fallback) {
        if (!StringUtils.hasText(template)) {
            return fallback;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            Object value = entry.getValue();
            result = result.replace(placeholder, value != null ? value.toString() : "");
        }
        return result;
    }
}
