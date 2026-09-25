package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.notification.config.TwilioProperties;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import com.smart.therapy.flow.notification.service.TwilioMessageSender;
import com.smart.therapy.flow.notification.service.TwilioSmsService;
import com.twilio.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("TwilioSmsService Unit Tests")
class TwilioSmsServiceTest {

    private static final String VALID_SID = "ACaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String VALID_TOKEN = "12345678901234567890123456789012";
    private static final String VALID_API_KEY_SID = "SKaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String VALID_API_KEY_SECRET = "abcdefghijklmnopqrstuvwxyz123456";

    private TwilioProperties properties;

    @BeforeEach
    void setUp() {
        properties = new TwilioProperties();
    }

    @Test
    void shouldReportUnconfiguredWhenSecretsMissing() {
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThat(service.isSmsConfigured()).isFalse();
    }

    @Test
    void sendSmsShouldNeverThrowWhenUnconfigured() {
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThatCode(() -> {
            SmsSendResult result = service.sendSms("+15195551234", "Test body");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.NOT_CONFIGURED);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidAccountSidBeforeCallingProvider() {
        configureValid(properties);
        properties.setAccountSid("INVALID");
        TwilioSmsService service = new TwilioSmsService(properties, (to, from, body) -> {
            throw new IllegalStateException("Provider should not be called");
        });

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
    }

    @Test
    void shouldRejectInvalidAuthTokenBeforeCallingProvider() {
        configureValid(properties);
        properties.setAuthToken("short");
        TwilioSmsService service = new TwilioSmsService(properties, (to, from, body) -> {
            throw new IllegalStateException("Provider should not be called");
        });

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
    }

    @Test
    void shouldMapAuthenticationFailureFromProvider() {
        TwilioSmsService service = configuredService((to, from, body) -> {
            throw new ApiException("Authenticate", 401, "https://www.twilio.com/docs/errors/20003", 20003, null);
        });

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.AUTHENTICATION_FAILED);
        assertThat(result.getError()).contains("authentication failed");
    }

    @Test
    void shouldMapInsufficientCreditsFromProvider() {
        TwilioSmsService service = configuredService((to, from, body) -> {
            throw new ApiException("Your account balance is too low to send this message", 400, null, null, null);
        });

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INSUFFICIENT_CREDITS);
    }

    @Test
    void shouldReturnSuccessWhenProviderSucceeds() {
        TwilioSmsService service = configuredService((to, from, body) -> "SM123456");

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSid()).isEqualTo("SM123456");
    }

    @Test
    void shouldClassifyOptOutKeywords() {
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThat(service.classifyInboundSms("STOP")).isEqualTo("opt-out");
        assertThat(service.classifyInboundSms(" unsubscribe ")).isEqualTo("opt-out");
        assertThat(service.classifyInboundSms("STOPALL")).isEqualTo("opt-out");
        assertThat(service.classifyInboundSms("CANCEL")).isEqualTo("opt-out");
    }

    @Test
    void shouldClassifyOptInKeywords() {
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThat(service.classifyInboundSms("START")).isEqualTo("opt-in");
        assertThat(service.classifyInboundSms("yes")).isEqualTo("opt-in");
        assertThat(service.classifyInboundSms("UNSTOP")).isEqualTo("opt-in");
    }

    @Test
    void sentenceContainingStopShouldNotUnsubscribe() {
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThat(service.classifyInboundSms("Please stop by the office")).isNull();
    }

    @Test
    void shouldRejectInvalidWebhookSignatureWhenUnconfigured() {
        TwilioSmsService service = new TwilioSmsService(properties);
        boolean valid = service.validateTwilioSignature(
                "sig",
                "https://example.com/api/sms/inbound",
                Map.of("Body", "STOP"));
        assertThat(valid).isFalse();
    }

    @Test
    void shouldReportConfiguredWhenAllSecretsPresent() {
        configureValid(properties);
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThat(service.isSmsConfigured()).isTrue();
        assertThat(properties.hasValidConfiguration()).isTrue();
    }

    @Test
    void shouldReportConfiguredWhenApiKeyCredentialsPresent() {
        configureValidWithApiKeys(properties);
        TwilioSmsService service = new TwilioSmsService(properties);
        assertThat(service.isSmsConfigured()).isTrue();
        assertThat(properties.hasValidConfiguration()).isTrue();
    }

    @Test
    void shouldSendSmsAddressesWhenWhatsappDisabled() {
        configureValid(properties);
        properties.setUseWhatsapp(false);
        TwilioSmsService service = new TwilioSmsService(properties, (to, from, body) -> {
            assertThat(to).isEqualTo("+15195551234");
            assertThat(from).isEqualTo("+15551234567");
            return "SM123456";
        });

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldSendWhatsappAddressesWhenWhatsappEnabled() {
        configureValid(properties);
        properties.setUseWhatsapp(true);
        TwilioSmsService service = new TwilioSmsService(properties, (to, from, body) -> {
            assertThat(to).isEqualTo("whatsapp:+15195551234");
            assertThat(from).isEqualTo("whatsapp:+15551234567");
            return "SM123456";
        });

        SmsSendResult result = service.sendSms("+15195551234", "Test body");

        assertThat(result.isSuccess()).isTrue();
    }

    private TwilioSmsService configuredService(TwilioMessageSender sender) {
        configureValid(properties);
        return new TwilioSmsService(properties, sender);
    }

    private void configureValid(TwilioProperties props) {
        props.setAccountSid(VALID_SID);
        props.setAuthToken(VALID_TOKEN);
        props.setFromNumber("+15551234567");
    }

    private void configureValidWithApiKeys(TwilioProperties props) {
        props.setAccountSid(VALID_SID);
        props.setApiKeySid(VALID_API_KEY_SID);
        props.setApiKeySecret(VALID_API_KEY_SECRET);
        props.setFromNumber("+15551234567");
    }
}
