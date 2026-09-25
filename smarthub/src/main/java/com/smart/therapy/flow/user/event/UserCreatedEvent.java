package com.smart.therapy.flow.user.event;

/**
 * Event published after user creation transaction commits.
 * Triggers asynchronous post-commit processing (password generation, email sending, audit logging).
 * 
 * This event is published inside the transaction but handled AFTER_COMMIT,
 * ensuring the user is fully persisted before any side effects occur.
 */
public record UserCreatedEvent(
    Long userId,
    Long requesterId,
    String ipAddress,
    Long organisationId,
    String schemaName
) {}
