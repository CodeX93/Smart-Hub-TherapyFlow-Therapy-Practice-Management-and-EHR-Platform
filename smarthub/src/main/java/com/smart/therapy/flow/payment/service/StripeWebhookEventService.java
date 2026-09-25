package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.payment.entity.StripeWebhookEvent;
import com.smart.therapy.flow.payment.repository.StripeWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StripeWebhookEventService {

    private final StripeWebhookEventRepository repository;

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(String eventId, String contextKey) {
        if (eventId == null || eventId.isBlank() || contextKey == null || contextKey.isBlank()) {
            return false;
        }
        return repository.existsByEventIdAndContextKey(eventId, contextKey);
    }

    @Transactional
    public boolean markProcessed(String eventId, String eventType, String contextKey) {
        return tryClaimEvent(eventId, eventType, contextKey);
    }

    @Transactional
    public boolean tryClaimEvent(String eventId, String eventType, String contextKey) {
        if (eventId == null || eventId.isBlank() || contextKey == null || contextKey.isBlank()) {
            return false;
        }
        StripeWebhookEvent row = StripeWebhookEvent.builder()
                .eventId(eventId)
                .contextKey(contextKey)
                .eventType(eventType != null ? eventType : "unknown")
                .processedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        try {
            repository.save(row);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Transactional
    public void releaseClaim(String eventId, String contextKey) {
        if (eventId == null || eventId.isBlank() || contextKey == null || contextKey.isBlank()) {
            return;
        }
        repository.deleteByEventIdAndContextKey(eventId, contextKey);
    }
}
