package com.smart.therapy.flow.client.listener;

import com.smart.therapy.flow.client.event.ClientNotificationEvent;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("clientCreatedExecutor")
    public void handleClientNotification(ClientNotificationEvent event) {
        TenantContext.setSchemaName(event.schemaName());
        TenantContext.setOrganisationId(event.organisationId());
        try {
            log.info("Processing client notification event type={} schema={} orgId={}",
                    event.eventType(), event.schemaName(), event.organisationId());
            Map<String, Object> payload = event.payload() == null ? new HashMap<>() : new HashMap<>(event.payload());
            notificationService.processEvent(event.eventType(), payload);
        } catch (Exception ex) {
            log.error("Failed to process client notification event type={} schema={} orgId={}",
                    event.eventType(), event.schemaName(), event.organisationId(), ex);
        } finally {
            TenantContext.clear();
        }
    }
}
