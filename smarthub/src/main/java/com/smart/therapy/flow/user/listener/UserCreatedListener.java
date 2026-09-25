package com.smart.therapy.flow.user.listener;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.user.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles post-commit processing for user creation.
 * 
 * This listener executes AFTER the transaction commits, ensuring:
 * - User is fully persisted before any side effects
 * - Failures in post-commit processing don't affect user creation
 * - All operations are retriable and idempotent
 * 
 * Steps:
 * - Audit logging (with state snapshots)
 * - Welcome email sending (async, no credentials in email)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserCreatedListener {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("userCreatedExecutor")
    public void handleUserCreated(UserCreatedEvent event) {
        TenantContext.setSchemaName(event.schemaName());
        TenantContext.setOrganisationId(event.organisationId());
        try {
            User user = userRepository.findById(event.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + event.userId()));

            log.info("Processing post-commit actions for user creation: userId={}", event.userId());

            // Audit logging (REQUIRES_NEW inside service)
            try {
                auditService.recordUserCreated(user, event.requesterId(), event.ipAddress());
            } catch (Exception e) {
                log.error("Failed to record audit log (non-critical): userId={}", event.userId(), e);
            }

            // Send welcome email (async)
            try {
                emailService.sendUserWelcomeEmailNoPasswordAsync(
                    user.getEmail(),
                    user.getFullName(),
                    user.getAuthIdentity() != null ? user.getAuthIdentity().getLoginIdentifier() : user.getEmail()
                );
                log.info("Welcome email queued via SES to user: userId={}", event.userId());
            } catch (Exception e) {
                log.error("Failed to send welcome email (non-critical): userId={}", event.userId(), e);
            }

            log.info("Completed post-commit processing for user: userId={}", event.userId());

        } catch (Exception e) {
            // Critical: Log but don't propagate - user is already created
            log.error("CRITICAL: Failed to process post-commit actions for user: userId={}", 
                    event.userId(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
