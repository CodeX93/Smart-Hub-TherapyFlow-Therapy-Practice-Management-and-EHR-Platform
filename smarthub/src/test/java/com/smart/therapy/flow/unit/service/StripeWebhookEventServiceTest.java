package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.payment.entity.StripeWebhookEvent;
import com.smart.therapy.flow.payment.repository.StripeWebhookEventRepository;
import com.smart.therapy.flow.payment.service.StripeWebhookEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookEventService Unit Tests")
class StripeWebhookEventServiceTest {

    @Mock
    private StripeWebhookEventRepository repository;

    @InjectMocks
    private StripeWebhookEventService service;

    @Test
    @DisplayName("Should return true when event is already processed")
    void shouldReturnTrueWhenAlreadyProcessed() {
        when(repository.existsByEventIdAndContextKey("evt_123", "platform")).thenReturn(true);

        boolean result = service.alreadyProcessed("evt_123", "platform");

        assertThat(result).isTrue();
        verify(repository).existsByEventIdAndContextKey("evt_123", "platform");
    }

    @Test
    @DisplayName("Should mark event as processed on first insert")
    void shouldMarkProcessedOnFirstInsert() {
        when(repository.save(any(StripeWebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.markProcessed("evt_first", "invoice.payment_succeeded", "platform");

        assertThat(result).isTrue();
        verify(repository).save(any(StripeWebhookEvent.class));
    }

    @Test
    @DisplayName("Should return false for duplicate event insert")
    void shouldReturnFalseForDuplicateInsert() {
        when(repository.save(any(StripeWebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        boolean result = service.markProcessed("evt_dup", "invoice.payment_failed", "platform");

        assertThat(result).isFalse();
    }
}
