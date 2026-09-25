package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import com.smart.therapy.flow.notification.config.TwilioProperties;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.security.RequestValidator;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Twilio transport layer. Knows only how to send/validate SMS — never whether to send.
 */
@Service
@Slf4j
public class TwilioSmsService {

    private static final Set<String> OPT_OUT_KEYWORDS = Set.of(
            "stop", "stopall", "unsubscribe", "cancel", "end", "quit");
    private static final Set<String> OPT_IN_KEYWORDS = Set.of(
            "start", "unstop", "yes");

    private final TwilioProperties twilioProperties;
    private final TwilioMessageSender messageSender;
    private volatile boolean clientInitialized;

    @Autowired
    public TwilioSmsService(TwilioProperties twilioProperties) {
        this(twilioProperties, null);
    }

    public TwilioSmsService(TwilioProperties twilioProperties, TwilioMessageSender messageSender) {
        this.twilioProperties = twilioProperties;
        this.messageSender = messageSender != null ? messageSender : this::sendViaTwilioSdk;
    }

    @PostConstruct
    void logStartupConfiguration() {
        if (!isSmsConfigured()) {
            log.info("[SMS] Twilio not configured — set twilio.account-sid, twilio.from-number, and either (twilio.api-key-sid + twilio.api-key-secret) or twilio.auth-token");
            return;
        }
        String rawFrom = twilioProperties.getFromNumber();
        String normalizedFrom = PhoneNormalizationUtil.normalizePhoneE164(rawFrom);
        log.info(
                "[{}] Twilio configured: accountSid={}, fromRaw={}, fromE164={}, configValid={}",
                channelLabel(),
                TwilioSmsLogUtil.maskAccountSid(twilioProperties.getAccountSid()),
                TwilioSmsLogUtil.maskPhone(rawFrom),
                TwilioSmsLogUtil.maskPhone(normalizedFrom),
                twilioProperties.hasValidConfiguration());
    }

    public boolean isSmsConfigured() {
        return twilioProperties.isConfigured();
    }

    /**
     * Send an SMS. Never throws — always returns a result object.
     *
     * @param toE164 destination in E.164 format
     * @param body   HIPAA-safe message body
     */
    public SmsSendResult sendSms(String toE164, String body) {
        if (!isSmsConfigured()) {
            return SmsSendResult.notConfigured();
        }
        if (!StringUtils.hasText(toE164) || !StringUtils.hasText(body)) {
            return SmsSendResult.failure(TwilioSmsErrorCode.INVALID_CONFIGURATION, "Missing destination or body");
        }

        SmsSendResult configurationError = TwilioSmsErrorMapper.validateConfiguration(twilioProperties);
        if (configurationError != null) {
            log.warn("[SMS] Twilio configuration invalid: {}", configurationError.getError());
            return configurationError;
        }

        String rawFrom = twilioProperties.getFromNumber();
        String fromE164 = PhoneNormalizationUtil.normalizePhoneE164(rawFrom);
        String toAddress = toTwilioAddress(toE164);
        String fromAddress = toTwilioAddress(fromE164);
        log.info(
                "[{}] Sending via Twilio: to={}, fromRaw={}, fromE164={}, bodyLength={}",
                channelLabel(),
                TwilioSmsLogUtil.maskPhone(toAddress),
                TwilioSmsLogUtil.maskPhone(rawFrom),
                TwilioSmsLogUtil.maskPhone(fromAddress),
                body.length());

        try {
            String sid = messageSender.send(toAddress, fromAddress, body);
            log.info("[{}] Twilio send succeeded: messageSid={}, to={}", channelLabel(), sid, TwilioSmsLogUtil.maskPhone(toAddress));
            return SmsSendResult.success(sid);
        } catch (Exception ex) {
            SmsSendResult mapped = TwilioSmsErrorMapper.mapException(ex);
            log.warn(
                    "[{}] Twilio send failed: to={}, errorCode={}, twilioErrorCode={}, httpStatus={}, message={}",
                    channelLabel(),
                    TwilioSmsLogUtil.maskPhone(toAddress),
                    mapped.getErrorCode(),
                    mapped.getTwilioErrorCode(),
                    mapped.getHttpStatusCode(),
                    mapped.getError());
            return mapped;
        }
    }

    /**
     * Classify inbound SMS intent from body text.
     *
     * @return "opt-out", "opt-in", or null
     */
    public String classifyInboundSms(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        String normalized = body.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalized.isEmpty()) {
            return null;
        }
        if (OPT_OUT_KEYWORDS.contains(normalized)) {
            return "opt-out";
        }
        if (OPT_IN_KEYWORDS.contains(normalized)) {
            return "opt-in";
        }
        return null;
    }

    public boolean validateTwilioSignature(String signature, String url, Map<String, String> params) {
        if (!StringUtils.hasText(twilioProperties.getAuthToken()) || !StringUtils.hasText(signature) || !StringUtils.hasText(url)) {
            return false;
        }
        try {
            RequestValidator validator = new RequestValidator(twilioProperties.getAuthToken());
            return validator.validate(url, params, signature);
        } catch (Exception ex) {
            log.warn("Twilio signature validation error: {}", ex.getMessage());
            return false;
        }
    }

    private String sendViaTwilioSdk(String toAddress, String fromAddress, String body) {
        ensureClientInitialized();
        log.debug("[{}] Twilio SDK Message.create: to={}, from={}", channelLabel(), TwilioSmsLogUtil.maskPhone(toAddress), TwilioSmsLogUtil.maskPhone(fromAddress));
        Message message = Message.creator(
                new PhoneNumber(toAddress),
                new PhoneNumber(fromAddress),
                body
        ).create();
        return message.getSid();
    }

    private String channelLabel() {
        return twilioProperties.isUseWhatsapp() ? "WhatsApp" : "SMS";
    }

    private String toTwilioAddress(String e164) {
        if (!StringUtils.hasText(e164)) {
            return e164;
        }
        if (!twilioProperties.isUseWhatsapp()) {
            return e164;
        }
        return e164.startsWith("whatsapp:") ? e164 : "whatsapp:" + e164;
    }

    private void ensureClientInitialized() {
        if (!clientInitialized) {
            synchronized (this) {
                if (!clientInitialized) {
                    if (twilioProperties.hasApiKeyCredentials()) {
                        Twilio.init(
                                twilioProperties.getApiKeySid(),
                                twilioProperties.getApiKeySecret(),
                                twilioProperties.getAccountSid());
                    } else {
                        Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
                    }
                    clientInitialized = true;
                }
            }
        }
    }
}
