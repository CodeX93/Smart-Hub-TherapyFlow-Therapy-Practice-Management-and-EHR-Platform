package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.notification.enums.NotificationCategory;
import com.smart.therapy.flow.notification.enums.NotificationPriority;
import com.smart.therapy.flow.notification.enums.NotificationTiming;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps ClientHubAI free-form notification type/priority/status values onto SmartHub enums.
 */
final class ClientHubNotificationTypeMapper {

    private static final Map<String, NotificationType> TYPE_ALIASES = Map.ofEntries(
            Map.entry("session_scheduled", NotificationType.APPOINTMENT_CONFIRMED),
            Map.entry("session_rescheduled", NotificationType.APPOINTMENT_RESCHEDULED),
            Map.entry("session_cancelled", NotificationType.APPOINTMENT_CANCELLED),
            Map.entry("appointment_cancelled", NotificationType.APPOINTMENT_CANCELLED),
            Map.entry("appointment_confirmed", NotificationType.APPOINTMENT_CONFIRMED),
            Map.entry("appointment_rescheduled", NotificationType.APPOINTMENT_RESCHEDULED),
            Map.entry("appointment_reminder", NotificationType.APPOINTMENT_REMINDER),
            Map.entry("session_reminder_24h", NotificationType.APPOINTMENT_24H_REMINDER),
            Map.entry("appointment_24h_reminder", NotificationType.APPOINTMENT_24H_REMINDER),
            Map.entry("appointment_1h_reminder", NotificationType.APPOINTMENT_1H_REMINDER),
            Map.entry("document_uploaded", NotificationType.DOCUMENT_SHARED),
            Map.entry("document_shared", NotificationType.DOCUMENT_SHARED),
            Map.entry("document_updated", NotificationType.DOCUMENT_UPDATED),
            Map.entry("document_review", NotificationType.DOCUMENT_UPDATED),
            Map.entry("document_review_reminder", NotificationType.DOCUMENT_UPDATED),
            Map.entry("invoice_sent", NotificationType.INVOICE_GENERATED),
            Map.entry("invoice_generated", NotificationType.INVOICE_GENERATED),
            Map.entry("payment_due", NotificationType.PAYMENT_DUE),
            Map.entry("payment_overdue", NotificationType.PAYMENT_OVERDUE),
            Map.entry("payment_received", NotificationType.PAYMENT_RECEIVED),
            Map.entry("payment_failed", NotificationType.PAYMENT_FAILED),
            Map.entry("form_assigned", NotificationType.FORM_ASSIGNED),
            Map.entry("form_submitted", NotificationType.FORM_SUBMITTED),
            Map.entry("form_reviewed", NotificationType.FORM_REVIEWED),
            Map.entry("session_notes_available", NotificationType.SESSION_NOTES_AVAILABLE),
            Map.entry("new_message", NotificationType.NEW_MESSAGE)
    );

    private ClientHubNotificationTypeMapper() {
    }

    static boolean isGlobalPreferenceTrigger(String triggerType) {
        return triggerType != null && "__global__".equalsIgnoreCase(triggerType.trim());
    }

    static Optional<NotificationType> mapType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        try {
            return Optional.of(NotificationType.valueOf(trimmed.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            // fall through to aliases
        }
        NotificationType alias = TYPE_ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(alias);
    }

    static NotificationType mapTypeOrFallback(String raw) {
        return mapType(raw).orElse(NotificationType.SYSTEM_MAINTENANCE);
    }

    static NotificationCategory categoryFor(NotificationType type) {
        return type.getCategory();
    }

    static NotificationPriority mapPriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return NotificationPriority.MEDIUM;
        }
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return NotificationPriority.valueOf(normalised);
        } catch (IllegalArgumentException ex) {
            return switch (normalised) {
                case "LOW" -> NotificationPriority.LOW;
                case "HIGH" -> NotificationPriority.HIGH;
                case "URGENT", "CRITICAL" -> NotificationPriority.URGENT;
                default -> NotificationPriority.MEDIUM;
            };
        }
    }

    static NotificationTiming mapTiming(String raw) {
        if (raw == null || raw.isBlank()) {
            return NotificationTiming.IMMEDIATE;
        }
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return NotificationTiming.valueOf(normalised);
        } catch (IllegalArgumentException ex) {
            return NotificationTiming.IMMEDIATE;
        }
    }

    static ScheduledNotificationStatus mapScheduledStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return ScheduledNotificationStatus.PENDING;
        }
        String normalised = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalised) {
            case "sent", "completed", "processed" -> ScheduledNotificationStatus.COMPLETED;
            case "failed", "error" -> ScheduledNotificationStatus.FAILED;
            case "cancelled", "canceled" -> ScheduledNotificationStatus.CANCELLED;
            case "processing", "running" -> ScheduledNotificationStatus.PROCESSING;
            default -> {
                try {
                    yield ScheduledNotificationStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    yield ScheduledNotificationStatus.PENDING;
                }
            }
        };
    }
}
