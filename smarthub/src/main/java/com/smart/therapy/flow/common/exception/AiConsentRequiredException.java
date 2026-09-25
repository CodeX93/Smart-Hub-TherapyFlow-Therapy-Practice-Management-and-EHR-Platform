package com.smart.therapy.flow.common.exception;

import java.util.Map;

public class AiConsentRequiredException extends ForbiddenException {

    private final Map<String, Object> details;

    public AiConsentRequiredException(String message) {
        super(message);
        this.details = Map.of("consentRequired", true);
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
