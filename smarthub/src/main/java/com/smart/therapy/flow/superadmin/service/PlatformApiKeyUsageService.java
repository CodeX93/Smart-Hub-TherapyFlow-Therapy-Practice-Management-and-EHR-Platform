package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PlatformApiKeyUsageService {

    private final PlatformApiKeyRepository apiKeyRepository;

    @Async("taskExecutor")
    @Transactional
    public void markUsed(Long keyId) {
        if (keyId == null) {
            return;
        }
        apiKeyRepository.touchLastUsedAt(keyId, Instant.now());
    }
}

