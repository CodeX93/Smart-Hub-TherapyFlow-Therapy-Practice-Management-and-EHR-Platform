package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Amazon SES email service implementation.
 * Uses AWS SDK for Java to send emails via Amazon Simple Email Service.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "ses", matchIfMissing = false)
public class AmazonSesEmailService implements EmailProviderService {

    private final SesClient sesClient;
    private final String fromEmail;
    private final String fromName;
    private final JavaMailSender javaMailSender; // Fallback to SMTP if configured

    public AmazonSesEmailService(
            @Value("${app.email.ses.region:eu-north-1}") String region,
            @Value("${app.email.ses.access-key:}") String accessKey,
            @Value("${app.email.ses.secret-key:}") String secretKey,
            @Value("${app.email.ses.from-email:}") String fromEmail,
            @Value("${app.email.ses.from-name:SmartHub}") String fromName,
            @Value("${app.email.ses.use-smtp:false}") boolean useSmtp,
            JavaMailSender javaMailSender
    ) {
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.javaMailSender = javaMailSender;

        // Initialize SES client
        if (useSmtp) {
            // Use SMTP via JavaMailSender (configured for SES SMTP)
            this.sesClient = null;
            log.info("Amazon SES configured to use SMTP via JavaMailSender");
        } else {
            // Use AWS SDK
            var builder = SesClient.builder()
                    .region(Region.of(region));
            
            if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
                AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
                builder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
                log.info("Amazon SES configured with static credentials");
            } else {
                // Use default credential chain (IAM role, environment variables, etc.)
                log.info("Amazon SES configured to use default AWS credential chain");
            }
            
            this.sesClient = builder.build();
        }

        if (!StringUtils.hasText(this.fromEmail)) {
            throw new IllegalStateException("Amazon SES from-email is required. Set app.email.ses.from-email property.");
        }
    }

    @Override
    @CircuitBreaker(name = "ses", fallbackMethod = "sendNotificationEmailFallback")
    @Retry(name = "ses")
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

        sendEmail(recipient.getEmail(), subject, body);
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        if (!StringUtils.hasText(to)) {
            log.warn("Cannot send email: recipient email is empty, correlationId={}", MDC.get("correlationId"));
            return;
        }

        try {
            if (sesClient != null) {
                sendEmailViaSdk(to, subject, htmlBody);
            } else {
                sendEmailViaSmtp(to, subject, htmlBody);
            }
            log.info("Email sent successfully via Amazon SES: correlationId={}", MDC.get("correlationId"));
        } catch (Exception e) {
            log.error("Amazon SES email delivery failed: correlationId={}", MDC.get("correlationId"), e);
            throw new RuntimeException("Failed to send email via Amazon SES", e);
        }
    }

    private void sendEmailViaSdk(String to, String subject, String htmlBody) {
        try {
            Destination destination = Destination.builder()
                    .toAddresses(to)
                    .build();

            Content subjectContent = Content.builder()
                    .data(subject)
                    .charset(StandardCharsets.UTF_8.name())
                    .build();

            Content bodyContent = Content.builder()
                    .data(htmlBody)
                    .charset(StandardCharsets.UTF_8.name())
                    .build();

            Body body = Body.builder()
                    .html(bodyContent)
                    .build();

            Message message = Message.builder()
                    .subject(subjectContent)
                    .body(body)
                    .build();

            String fromAddress = StringUtils.hasText(fromName) 
                    ? String.format("%s <%s>", fromName, fromEmail)
                    : fromEmail;

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(destination)
                    .message(message)
                    .source(fromAddress)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.debug("Amazon SES email sent: messageId={}, correlationId={}", 
                    response.messageId(), MDC.get("correlationId"));
        } catch (SesException e) {
            log.error("Amazon SES API error: errorCode={}, errorMessage={}, correlationId={}", 
                    e.awsErrorDetails().errorCode(), 
                    e.awsErrorDetails().errorMessage(), 
                    MDC.get("correlationId"), e);
            throw new RuntimeException("Failed to send email via Amazon SES API", e);
        }
    }

    private void sendEmailViaSmtp(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        String fromAddress = StringUtils.hasText(fromName) 
                ? String.format("%s <%s>", fromName, fromEmail)
                : fromEmail;

        helper.setFrom(fromAddress != null ? fromAddress : (fromEmail != null ? fromEmail : "noreply@therapyflow.com"));
        helper.setTo(to != null && !to.isEmpty() ? to : "noreply@therapyflow.com");
        helper.setSubject(subject != null && !subject.isEmpty() ? subject : "No Subject");
        helper.setText(htmlBody != null && !htmlBody.isEmpty() ? htmlBody : "", true); // true = HTML

        javaMailSender.send(message);
    }

    public void sendNotificationEmailFallback(User recipient,
                                             Notification inAppNotification,
                                             NotificationTemplate emailTemplate,
                                             Map<String, Object> entityData,
                                             Exception ex) {
        log.error("Amazon SES circuit breaker fallback triggered: userId={}, correlationId={}", 
                recipient != null ? recipient.getId() : "unknown", MDC.get("correlationId"), ex);
        // Email will be queued for retry or logged for manual processing
    }

    @Override
    public String getFromAddress() {
        return fromEmail;
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
