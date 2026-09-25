package com.smart.therapy.flow.user.event;

import java.util.List;

/**
 * Event published after user update transaction commits.
 * Triggers asynchronous post-commit audit logging with before/after state snapshots.
 */
public record UserUpdatedEvent(
    Long userId,
    Long requesterId,
    String ipAddress,
    String beforeState,
    String afterState,
    List<String> changedFields,
    Long organisationId,
    String schemaName
) {}
