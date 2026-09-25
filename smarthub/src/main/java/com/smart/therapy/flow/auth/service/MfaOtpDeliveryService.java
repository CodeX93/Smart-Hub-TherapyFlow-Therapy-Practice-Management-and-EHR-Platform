package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import com.smart.therapy.flow.notification.service.TwilioSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaOtpDeliveryService {

    private final TwilioSmsService twilioSmsService;
    private final EmailService emailService;

    @Value("${app.auth.mfa.otp-sms-enabled:true}")
    private boolean smsEnabled;

    @Value("${app.auth.mfa.otp-email-enabled:true}")
    private boolean emailEnabled;

    public void sendOtp(MfaMethod channel, String destination, String code) {
        if (channel == MfaMethod.SMS) {
            sendSmsOtp(destination, code);
            return;
        }
        if (channel == MfaMethod.EMAIL) {
            sendEmailOtp(destination, code);
            return;
        }
        throw new BadRequestException("Unsupported OTP channel: " + channel);
    }

    public boolean isChannelAvailable(MfaMethod channel) {
        if (channel == MfaMethod.SMS) {
            return smsEnabled && twilioSmsService.isSmsConfigured();
        }
        if (channel == MfaMethod.EMAIL) {
            return emailEnabled;
        }
        return false;
    }

    private void sendSmsOtp(String rawPhone, String code) {
        if (!smsEnabled) {
            throw new BadRequestException("SMS verification is not enabled");
        }
        String phoneE164 = PhoneNormalizationUtil.normalizePhoneE164(rawPhone);
        if (!StringUtils.hasText(phoneE164)) {
            throw new BadRequestException("A valid mobile number is required for SMS verification");
        }
        SmsSendResult result = twilioSmsService.sendSms(
                phoneE164,
                "Your TherapyFlow sign-in code is " + code + ". It expires in 5 minutes.");
        if (!result.isSuccess()) {
            log.warn("MFA SMS delivery failed: code={}, twilioCode={}, error={}",
                    result.getErrorCode(), result.getTwilioErrorCode(), result.getError());
            throw new BadRequestException(userFacingSmsError(result));
        }
    }

    private static String userFacingSmsError(SmsSendResult result) {
        if (result.getErrorCode() == null) {
            return "Unable to send SMS verification code. Check the phone number or try email instead.";
        }
        return switch (result.getErrorCode()) {
            case DESTINATION_OPTED_OUT -> result.getError() != null
                    ? result.getError()
                    : "This phone number is not subscribed to SMS (Twilio error 21610). "
                            + "Text START to the TherapyFlow SMS number to re-subscribe, or use email verification. "
                            + "For production SMS, configure and upgrade your Twilio account.";
            case NOT_CONFIGURED, INVALID_CONFIGURATION ->
                    "SMS is not fully configured. Configure a production Twilio account "
                            + "(Account SID, Auth Token, and From number) and try again, or use email verification.";
            case AUTHENTICATION_FAILED, ACCOUNT_SUSPENDED, INSUFFICIENT_CREDITS ->
                    "Twilio could not send SMS (" + result.getErrorCode().name() + "). "
                            + "Configure a production Twilio account with active SMS messaging, or use email verification.";
            case GEO_PERMISSION_DENIED ->
                    "Twilio cannot send SMS to this country/region. Enable geo permissions in your Twilio account "
                            + "for production, or use email verification.";
            case INVALID_DESTINATION ->
                    "Unable to send SMS to this phone number. Check that it is a valid mobile number in E.164 format "
                            + "(for example +12265551234), or use email verification.";
            default -> StringUtils.hasText(result.getError())
                    ? result.getError()
                    : "Unable to send SMS verification code. Check the phone number or try email instead.";
        };
    }

    private void sendEmailOtp(String email, String code) {
        if (!emailEnabled) {
            throw new BadRequestException("Email verification is not enabled");
        }
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new BadRequestException("A valid email address is required for email verification");
        }
        emailService.sendEmail(
                email.trim(),
                "Your SmartHub sign-in code",
                EmailHtmlComponents.otpEmailBody(code));
    }

    public static String maskDestination(MfaMethod method, String destination) {
        if (!StringUtils.hasText(destination)) {
            return null;
        }
        if (method == MfaMethod.EMAIL) {
            int at = destination.indexOf('@');
            if (at <= 1) {
                return "***@" + destination.substring(Math.max(at + 1, 0));
            }
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        if (method == MfaMethod.SMS) {
            String digits = destination.replaceAll("\\D", "");
            if (digits.length() < 4) {
                return "***";
            }
            return "***" + digits.substring(digits.length() - 4);
        }
        return null;
    }
}
