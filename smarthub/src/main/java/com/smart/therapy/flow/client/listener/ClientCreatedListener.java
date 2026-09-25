package com.smart.therapy.flow.client.listener;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.event.ClientCreatedEvent;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationService;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles post-commit processing for client creation.
 * 
 * This listener executes AFTER the transaction commits, ensuring:
 * - Client is fully persisted before any side effects
 * - Failures in post-commit processing don't affect client creation
 * 
 * Steps 6, 7, 8 from the business flow:
 * - Step 6: History Logging
 * - Step 7: Audit Logging (with state snapshots)
 * - Step 8: Portal Activation, Notifications, Integrations
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClientCreatedListener {

    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("clientCreatedExecutor")
    public void handleClientCreated(ClientCreatedEvent event) {
        TenantContext.setSchemaName(event.schemaName());
        TenantContext.setOrganisationId(event.organisationId());
        try {
            Client client = clientRepository.findForCreatedEvent(event.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + event.clientId()));

            log.info("Processing post-commit actions for client creation: clientId={}", event.clientId());

            // Step 6 — History Logging
            try {
                String stageValue = client.getStage() != null ? client.getStage() : "intake";
                clientService.trackClientHistory(event.clientId(), "file_created", null, stageValue,
                        "Client file created", event.requesterId(), "SYSTEM");
            } catch (Exception e) {
                log.error("Failed to track client history (non-critical): clientId={}", event.clientId(), e);
            }

            // Step 7 — Audit Logging (REQUIRES_NEW inside service)
            try {
                auditService.recordClientCreated(client, event.requesterId(), event.ipAddress());
            } catch (Exception e) {
                log.error("Failed to record audit log (non-critical): clientId={}", event.clientId(), e);
            }

            // Step 8 — Welcome Email + Notifications + Integrations (portal activation handled in createClient)
            try {
                String clientEmail = client.getPrimaryEmail();
                if (StringUtils.hasText(clientEmail)) {
                    String therapistName = client.getAssignedTherapist() != null
                            ? client.getAssignedTherapist().getFullName() : null;

                    emailService.sendWelcomeEmail(clientEmail, client.getFullName(), therapistName);
                    log.info("Welcome email sent via SES to client: clientId={}", event.clientId());
                } else {
                    log.debug("Skipping welcome email - no email address for client: clientId={}", event.clientId());
                }
            } catch (Exception e) {
                log.error("Failed to send welcome email (non-critical): clientId={}", event.clientId(), e);
            }

            try {
                notificationService.processEvent(NotificationEventCatalog.CLIENT_CREATED, clientService.buildClientEventData(client));
            } catch (Exception e) {
                log.error("Failed to send notifications (non-critical): clientId={}", event.clientId(), e);
            }

            log.info("Completed post-commit processing for client: clientId={}", event.clientId());

        } catch (Exception e) {
            // Critical: Log but don't propagate - client is already created
            log.error("CRITICAL: Failed to process post-commit actions for client: clientId={}", 
                    event.clientId(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
