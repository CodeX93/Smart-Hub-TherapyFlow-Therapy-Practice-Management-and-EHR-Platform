package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import com.smart.therapy.flow.notification.config.TwilioProperties;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import com.twilio.exception.ApiException;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Validates Twilio configuration and maps provider exceptions to safe, auditable results.
 */
public final class TwilioSmsErrorMapper {

    private static final Set<Integer> AUTH_ERROR_CODES = Set.of(20003, 20004, 20005);
    private static final Set<Integer> SUSPENDED_ERROR_CODES = Set.of(30002);
    private static final int GEO_PERMISSION_ERROR_CODE = 21408;
    private static final int DESTINATION_OPTED_OUT_ERROR_CODE = 21610;

    private TwilioSmsErrorMapper() {
    }

    public static SmsSendResult validateConfiguration(TwilioProperties properties) {
        if (properties == null || !properties.isConfigured()) {
            return SmsSendResult.notConfigured();
        }

        String accountSid = properties.getAccountSid().trim();
        if (!accountSid.startsWith("AC") || accountSid.length() != 34) {
            return SmsSendResult.failure(
                    TwilioSmsErrorCode.INVALID_CONFIGURATION,
                    "Twilio account SID is invalid. Expected an Account SID starting with AC.");
        }

        if (properties.hasApiKeyCredentials()) {
            String apiKeySid = properties.getApiKeySid().trim();
            if (!apiKeySid.startsWith("SK") || apiKeySid.length() != 34) {
                return SmsSendResult.failure(
                        TwilioSmsErrorCode.INVALID_CONFIGURATION,
                        "Twilio API key SID is invalid. Expected a Key SID starting with SK.");
            }

            String apiKeySecret = properties.getApiKeySecret().trim();
            if (apiKeySecret.length() < 16) {
                return SmsSendResult.failure(
                        TwilioSmsErrorCode.INVALID_CONFIGURATION,
                        "Twilio API key secret is invalid or too short.");
            }
        } else {
            String authToken = properties.getAuthToken().trim();
            if (authToken.length() < 16) {
                return SmsSendResult.failure(
                        TwilioSmsErrorCode.INVALID_CONFIGURATION,
                        "Twilio auth token is invalid or too short.");
            }
        }

        String fromE164 = PhoneNormalizationUtil.normalizePhoneE164(properties.getFromNumber());
        if (!StringUtils.hasText(fromE164)) {
            return SmsSendResult.failure(
                    TwilioSmsErrorCode.INVALID_CONFIGURATION,
                    "Twilio from number is invalid. Expected an E.164 phone number.");
        }

        return null;
    }

    public static SmsSendResult mapException(Exception ex) {
        if (ex instanceof ApiException apiException) {
            return mapApiException(apiException);
        }

        String message = ex.getMessage() != null ? ex.getMessage() : "Twilio send failed";
        TwilioSmsErrorCode code = classifyMessage(message);
        if (code != TwilioSmsErrorCode.UNKNOWN) {
            return failure(code, sanitizeMessage(code, message), null, null);
        }
        return failure(TwilioSmsErrorCode.PROVIDER_ERROR, message, null, null);
    }

    private static SmsSendResult failure(TwilioSmsErrorCode code, String message, Integer twilioCode, Integer statusCode) {
        return SmsSendResult.failure(code, message, twilioCode, statusCode);
    }

    private static SmsSendResult mapApiException(ApiException ex) {
        Integer twilioCode = ex.getCode();
        Integer statusCode = ex.getStatusCode();
        String message = ex.getMessage() != null ? ex.getMessage() : "Twilio API error";

        if (twilioCode != null) {
            if (AUTH_ERROR_CODES.contains(twilioCode) || Integer.valueOf(401).equals(statusCode) || Integer.valueOf(403).equals(statusCode)) {
                return failure(TwilioSmsErrorCode.AUTHENTICATION_FAILED,
                        "Twilio authentication failed. Verify account SID and auth token.",
                        twilioCode, statusCode);
            }
            if (SUSPENDED_ERROR_CODES.contains(twilioCode)) {
                return failure(TwilioSmsErrorCode.ACCOUNT_SUSPENDED,
                        "Twilio account is suspended or inactive.",
                        twilioCode, statusCode);
            }
            if (twilioCode == GEO_PERMISSION_ERROR_CODE) {
                return failure(TwilioSmsErrorCode.GEO_PERMISSION_DENIED, message, twilioCode, statusCode);
            }
            if (twilioCode == DESTINATION_OPTED_OUT_ERROR_CODE) {
                return failure(
                        TwilioSmsErrorCode.DESTINATION_OPTED_OUT,
                        "This phone number is not subscribed to SMS from TherapyFlow (Twilio error 21610). "
                                + "The recipient previously replied STOP. Ask them to text START to the TherapyFlow "
                                + "SMS number to re-subscribe, or use email verification instead. "
                                + "For production SMS, ensure your Twilio account is upgraded and SMS messaging "
                                + "is enabled for this sender number.",
                        twilioCode,
                        statusCode);
            }
            if (twilioCode == 21606 || twilioCode == 21211) {
                return failure(TwilioSmsErrorCode.INVALID_DESTINATION, message, twilioCode, statusCode);
            }
        }

        TwilioSmsErrorCode classified = classifyMessage(message);
        if (classified != TwilioSmsErrorCode.UNKNOWN) {
            return failure(classified, sanitizeMessage(classified, message), twilioCode, statusCode);
        }

        return failure(TwilioSmsErrorCode.PROVIDER_ERROR, message, twilioCode, statusCode);
    }

    private static TwilioSmsErrorCode classifyMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return TwilioSmsErrorCode.UNKNOWN;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("authenticate") || lower.contains("authentication")
                || lower.contains("invalid username") || lower.contains("invalid password")
                || lower.contains("unauthorized") || lower.contains("auth token")) {
            return TwilioSmsErrorCode.AUTHENTICATION_FAILED;
        }
        if (lower.contains("balance") || lower.contains("insufficient")
                || lower.contains("fund") || lower.contains("credit")
                || lower.contains("not authorized to send")) {
            return TwilioSmsErrorCode.INSUFFICIENT_CREDITS;
        }
        if (lower.contains("suspended") || lower.contains("not active")) {
            return TwilioSmsErrorCode.ACCOUNT_SUSPENDED;
        }
        if (lower.contains("not a valid") && lower.contains("number")) {
            return TwilioSmsErrorCode.INVALID_DESTINATION;
        }
        if (lower.contains("permission to send an sms has not been enabled for the region")
                || lower.contains("geo permission")) {
            return TwilioSmsErrorCode.GEO_PERMISSION_DENIED;
        }
        if (lower.contains("unsubscribed") || lower.contains("opted out")
                || lower.contains("attempt to send to unsubscribed")) {
            return TwilioSmsErrorCode.DESTINATION_OPTED_OUT;
        }
        return TwilioSmsErrorCode.UNKNOWN;
    }

    private static String sanitizeMessage(TwilioSmsErrorCode code, String original) {
        return switch (code) {
            case AUTHENTICATION_FAILED -> "Twilio authentication failed. Verify account SID and auth token.";
            case INSUFFICIENT_CREDITS -> "Twilio account cannot send SMS. Check account balance and SMS permissions.";
            case ACCOUNT_SUSPENDED -> "Twilio account is suspended or inactive.";
            case DESTINATION_OPTED_OUT ->
                    "This phone number is not subscribed to SMS from TherapyFlow (Twilio error 21610). "
                            + "The recipient previously replied STOP. Ask them to text START to re-subscribe, "
                            + "or use email verification instead. For production SMS, ensure your Twilio account "
                            + "is upgraded and SMS messaging is enabled for this sender number.";
            case INVALID_DESTINATION -> original;
            case GEO_PERMISSION_DENIED -> original;
            default -> original;
        };
    }
}
