package com.smart.therapy.flow.client.event;

import java.util.Map;

/**
 * Event published for client lifecycle notification delivery after transaction commit.
 */
public record ClientNotificationEvent(
        String eventType,
        Map<String, Object> payload,
        Long organisationId,
        String schemaName
) {
}
