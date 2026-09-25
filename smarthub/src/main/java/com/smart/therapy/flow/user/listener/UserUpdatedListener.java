package com.smart.therapy.flow.user.listener;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.user.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Handles post-commit processing for user updates.
 * 
 * This listener executes AFTER the transaction commits, ensuring:
 * - User is fully updated before audit logging
 * - Failures in audit logging don't affect user update
 * - Complete audit trail with before/after states
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserUpdatedListener {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("userUpdatedExecutor")
    public void handleUserUpdated(UserUpdatedEvent event) {
        TenantContext.setSchemaName(event.schemaName());
        TenantContext.setOrganisationId(event.organisationId());
        try {
            User user = userRepository.findById(event.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + event.userId()));

            log.info("Processing post-commit audit logging for user update: userId={}", event.userId());

            auditService.recordUserUpdated(
                user,
                event.beforeState(),
                event.afterState(),
                event.changedFields(),
                event.requesterId(),
                event.ipAddress()
            );

            log.info("Completed audit logging for user update: userId={}", event.userId());

        } catch (Exception e) {
            // Critical: Log but don't propagate - user is already updated
            log.error("CRITICAL: Failed to process post-commit audit logging for user: userId={}", 
                    event.userId(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
