package com.smart.therapy.flow.client.event;

/**
 * Event published after client creation transaction commits.
 * Triggers asynchronous post-commit processing (history, audit, portal, notifications).
 * 
 * This event is published inside the transaction but handled AFTER_COMMIT,
 * ensuring the client is fully persisted before any side effects occur.
 */
public record ClientCreatedEvent(
    Long clientId,
    Long requesterId,
    String ipAddress,
    Long organisationId,
    String schemaName
) {}
