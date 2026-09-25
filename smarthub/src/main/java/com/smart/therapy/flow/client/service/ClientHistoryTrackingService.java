package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientHistory;
import com.smart.therapy.flow.client.enums.ClientEventType;
import com.smart.therapy.flow.client.enums.EventSource;
import com.smart.therapy.flow.client.repository.ClientHistoryRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists client history in the current transaction.
 * <p>
 * Must NOT use {@code REQUIRES_NEW}: a nested transaction that inserts into
 * {@code client_history} (FK to {@code clients}) while the outer update still
 * holds the client row lock deadlocks until Postgres idle-in-transaction timeout.
 * Failures are swallowed so history issues do not fail the primary mutation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientHistoryTrackingService {

    private final ClientRepository clientRepository;
    private final ClientHistoryRepository clientHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void persistHistory(Long clientId,
            ClientEventType eventType,
            EventSource eventSource,
            String fromValue,
            String toValue,
            String description,
            Long createdByUserId,
            String createdByName) {
        try {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                log.warn("Cannot track history for non-existent client: {}", clientId);
                return;
            }

            ClientHistory history = ClientHistory.builder()
                    .client(client)
                    .eventType(eventType)
                    .eventSource(eventSource != null ? eventSource : EventSource.API)
                    .fromValue(fromValue)
                    .toValue(toValue)
                    .description(description)
                    .createdByUser(createdByUserId != null
                            ? userRepository.findById(createdByUserId).orElse(null)
                            : null)
                    .createdByName(createdByName)
                    .build();

            clientHistoryRepository.saveAndFlush(history);
        } catch (Exception e) {
            log.error("Failed to track client history for client {} event {}: {}",
                    clientId, eventType, e.getMessage(), e);
        }
    }
}
