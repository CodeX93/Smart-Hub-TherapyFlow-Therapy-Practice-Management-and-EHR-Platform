package com.smart.therapy.flow.ai.service;

import com.smart.therapy.flow.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsentPolicyService {

    private final ConsentService consentService;

    public void requireAiConsent(Long clientId) {
        ConsentService.ConsentCheckResult consentCheck = consentService.checkAIProcessingConsent(clientId);
        if (!consentCheck.isHasConsent()) {
            throw new ForbiddenException(consentCheck.getMessage());
        }
    }
}
