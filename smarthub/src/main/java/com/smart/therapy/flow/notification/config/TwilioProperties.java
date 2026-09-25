package com.smart.therapy.flow.notification.config;

import com.smart.therapy.flow.notification.service.TwilioSmsErrorMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

    private String accountSid;
    private String apiKeySid;
    private String apiKeySecret;
    private String authToken;
    private String fromNumber;
    /**
     * When true, send via Twilio WhatsApp ({@code whatsapp:+...} addresses).
     * When false, send via standard SMS.
     */
    private boolean useWhatsapp = false;

    public boolean isConfigured() {
        return StringUtils.hasText(accountSid)
                && StringUtils.hasText(fromNumber)
                && (hasApiKeyCredentials() || hasLegacyCredentials());
    }

    public boolean hasApiKeyCredentials() {
        return StringUtils.hasText(apiKeySid) && StringUtils.hasText(apiKeySecret);
    }

    public boolean hasLegacyCredentials() {
        return StringUtils.hasText(authToken);
    }

    public boolean hasValidConfiguration() {
        return TwilioSmsErrorMapper.validateConfiguration(this) == null;
    }
}
