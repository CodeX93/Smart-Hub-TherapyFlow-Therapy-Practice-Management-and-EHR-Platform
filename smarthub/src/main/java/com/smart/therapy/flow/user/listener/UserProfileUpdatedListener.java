package com.smart.therapy.flow.user.listener;

import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.user.event.UserProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Handles post-commit processing for user profile updates.
 * 
 * This listener executes AFTER the transaction commits, ensuring:
 * - Profile is fully updated before audit logging
 * - Failures in audit logging don't affect profile update
 * - Complete audit trail with before/after states
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserProfileUpdatedListener {

    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("userUpdatedExecutor")
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event) {
        TenantContext.setSchemaName(event.schemaName());
        TenantContext.setOrganisationId(event.organisationId());
        try {
            UserProfile profile = userProfileRepository.findById(event.profileId())
                    .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + event.profileId()));

            log.info("Processing post-commit audit logging for user profile update: profileId={}, userId={}", 
                    event.profileId(), event.userId());

            auditService.recordUserProfileUpdated(
                profile,
                event.beforeState(),
                event.afterState(),
                event.changedFields(),
                event.requesterId(),
                event.ipAddress()
            );

            log.info("Completed audit logging for user profile update: profileId={}", event.profileId());

        } catch (Exception e) {
            // Critical: Log but don't propagate - profile is already updated
            log.error("CRITICAL: Failed to process post-commit audit logging for user profile: profileId={}", 
                    event.profileId(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
