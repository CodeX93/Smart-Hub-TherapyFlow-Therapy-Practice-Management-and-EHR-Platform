package com.smart.therapy.flow.notification.dto;

import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SmsSendResult {
    boolean success;
    String sid;
    String error;
    TwilioSmsErrorCode errorCode;
    Integer twilioErrorCode;
    Integer httpStatusCode;

    public static SmsSendResult notConfigured() {
        return SmsSendResult.builder()
                .success(false)
                .error("SMS not configured")
                .errorCode(TwilioSmsErrorCode.NOT_CONFIGURED)
                .build();
    }

    public static SmsSendResult success(String sid) {
        return SmsSendResult.builder()
                .success(true)
                .sid(sid)
                .build();
    }

    public static SmsSendResult failure(String error) {
        return failure(TwilioSmsErrorCode.PROVIDER_ERROR, error);
    }

    public static SmsSendResult failure(TwilioSmsErrorCode errorCode, String error) {
        return failure(errorCode, error, null, null);
    }

    public static SmsSendResult failure(TwilioSmsErrorCode errorCode, String error, Integer twilioErrorCode, Integer httpStatusCode) {
        return SmsSendResult.builder()
                .success(false)
                .error(error)
                .errorCode(errorCode)
                .twilioErrorCode(twilioErrorCode)
                .httpStatusCode(httpStatusCode)
                .build();
    }
}
