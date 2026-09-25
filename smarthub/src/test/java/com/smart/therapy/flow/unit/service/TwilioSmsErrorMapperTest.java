package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.notification.config.TwilioProperties;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import com.smart.therapy.flow.notification.service.TwilioSmsErrorMapper;
import com.twilio.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TwilioSmsErrorMapper Unit Tests")
class TwilioSmsErrorMapperTest {

    @Test
    void shouldReturnNotConfiguredWhenPropertiesMissing() {
        SmsSendResult result = TwilioSmsErrorMapper.validateConfiguration(new TwilioProperties());
        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.NOT_CONFIGURED);
    }

    @Test
    void shouldRejectInvalidAccountSid() {
        TwilioProperties properties = validProperties();
        properties.setAccountSid("bad-sid");

        SmsSendResult result = TwilioSmsErrorMapper.validateConfiguration(properties);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
        assertThat(result.getError()).contains("account SID");
    }

    @Test
    void shouldRejectShortAuthToken() {
        TwilioProperties properties = validProperties();
        properties.setAuthToken("short");

        SmsSendResult result = TwilioSmsErrorMapper.validateConfiguration(properties);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
        assertThat(result.getError()).contains("auth token");
    }

    @Test
    void shouldRejectInvalidApiKeySid() {
        TwilioProperties properties = validApiKeyProperties();
        properties.setApiKeySid("bad-sid");

        SmsSendResult result = TwilioSmsErrorMapper.validateConfiguration(properties);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
        assertThat(result.getError()).contains("API key SID");
    }

    @Test
    void shouldRejectShortApiKeySecret() {
        TwilioProperties properties = validApiKeyProperties();
        properties.setApiKeySecret("short");

        SmsSendResult result = TwilioSmsErrorMapper.validateConfiguration(properties);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
        assertThat(result.getError()).contains("API key secret");
    }

    @Test
    void shouldRejectInvalidFromNumber() {
        TwilioProperties properties = validProperties();
        properties.setFromNumber("not-a-phone");

        SmsSendResult result = TwilioSmsErrorMapper.validateConfiguration(properties);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_CONFIGURATION);
        assertThat(result.getError()).contains("from number");
    }

    @Test
    void shouldAcceptValidConfiguration() {
        assertThat(TwilioSmsErrorMapper.validateConfiguration(validProperties())).isNull();
    }

    @Test
    void shouldAcceptValidApiKeyConfiguration() {
        assertThat(TwilioSmsErrorMapper.validateConfiguration(validApiKeyProperties())).isNull();
    }

    @Test
    void shouldMapAuthenticationApiException() {
        ApiException ex = new ApiException("Authenticate", 401, "https://www.twilio.com/docs/errors/20003", 20003, null);

        SmsSendResult result = TwilioSmsErrorMapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.AUTHENTICATION_FAILED);
    }

    @Test
    void shouldMapInsufficientCreditsFromMessage() {
        ApiException ex = new ApiException("Unable to create record: Your account balance is too low", 400, null, null, null);

        SmsSendResult result = TwilioSmsErrorMapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INSUFFICIENT_CREDITS);
    }

    @Test
    void shouldMapSuspendedAccount() {
        ApiException ex = new ApiException("Account is suspended", 400, null, 30002, null);

        SmsSendResult result = TwilioSmsErrorMapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    void shouldMapOptedOutDestination() {
        // Twilio ApiException(message, code, moreInfo, status, cause)
        ApiException ex = new ApiException(
                "Attempt to send to unsubscribed recipient",
                21610,
                "https://www.twilio.com/docs/errors/21610",
                400,
                null);

        SmsSendResult result = TwilioSmsErrorMapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.DESTINATION_OPTED_OUT);
        assertThat(result.getTwilioErrorCode()).isEqualTo(21610);
        assertThat(result.getError()).contains("not subscribed");
        assertThat(result.getError()).contains("21610");
        assertThat(result.getError()).containsIgnoringCase("twilio");
    }

    @Test
    void shouldMapInvalidDestination() {
        ApiException ex = new ApiException("The 'To' number is not a valid mobile number", 400, null, 21211, null);

        SmsSendResult result = TwilioSmsErrorMapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.INVALID_DESTINATION);
    }

    @Test
    void shouldMapGeoPermissionDenied() {
        ApiException ex = new ApiException(
                "Permission to send an SMS has not been enabled for the region indicated by the 'To' number: +92321337XXXX",
                400,
                "https://www.twilio.com/docs/errors/21408",
                21408,
                null);

        SmsSendResult result = TwilioSmsErrorMapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(TwilioSmsErrorCode.GEO_PERMISSION_DENIED);
    }

    private TwilioProperties validProperties() {
        TwilioProperties properties = new TwilioProperties();
        properties.setAccountSid("ACaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        properties.setAuthToken("12345678901234567890123456789012");
        properties.setFromNumber("+15551234567");
        return properties;
    }

    private TwilioProperties validApiKeyProperties() {
        TwilioProperties properties = new TwilioProperties();
        properties.setAccountSid("ACaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        properties.setApiKeySid("SKaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        properties.setApiKeySecret("12345678901234567890123456789012");
        properties.setFromNumber("+15551234567");
        return properties;
    }
}
