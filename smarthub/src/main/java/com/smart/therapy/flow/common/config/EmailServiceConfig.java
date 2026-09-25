package com.smart.therapy.flow.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Configuration class to validate email service provider selection
 * and log required configuration properties.
 */
@Component
@Slf4j
public class EmailServiceConfig {

    @Value("${app.email.provider:sparkpost}")
    private String emailProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void validateEmailConfiguration() {
        log.info("Email service provider: {}", emailProvider);
        
        switch (emailProvider.toLowerCase()) {
            case "sparkpost":
                log.info("SparkPost email service is active");
                log.info("Required configuration: email.sparkpost.api-key, email.sparkpost.from-email");
                break;
            case "ses":
                log.info("Amazon SES email service is active");
                log.info("Required configuration: app.email.ses.from-email, app.email.ses.region");
                log.info("Optional: app.email.ses.access-key, app.email.ses.secret-key (uses default credential chain if not provided)");
                break;
            default:
                log.warn("Unknown email provider: {}. Valid options are: sparkpost, ses", emailProvider);
        }
    }
}

