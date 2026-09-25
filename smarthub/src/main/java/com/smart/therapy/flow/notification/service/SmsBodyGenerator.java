package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Generates HIPAA-safe SMS bodies: practice name, date/time, and STOP line only.
 */
@Component
@RequiredArgsConstructor
public class SmsBodyGenerator {

    private static final String DEFAULT_PRACTICE_NAME = "SmartHub";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String STOP_LINE = " Reply STOP to opt out.";
    private static final DateTimeFormatter SMS_DATE_TIME = DateTimeFormatter
            .ofPattern("EEE, MMM d 'at' h:mm a z", Locale.US);

    private final PracticeConfigurationService practiceConfigurationService;

    public String generate(String eventType, boolean isScheduled, Map<String, Object> entityData) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        String practiceName = resolvePracticeName();
        String dateTime = formatSmsDateTime(resolveDateValue(entityData, isScheduled),
                resolveRecipientTimezone(entityData));

        if (isScheduled) {
            return "Reminder: your appointment with " + practiceName + " is on " + dateTime + "." + STOP_LINE;
        }

        return switch (eventType.toLowerCase(Locale.ROOT)) {
            case NotificationEventCatalog.SESSION_SCHEDULED ->
                    "Your appointment with " + practiceName + " is confirmed for " + dateTime + "." + STOP_LINE;
            case NotificationEventCatalog.SESSION_SERIES_SCHEDULED -> {
                Object count = entityData != null ? entityData.get("sessionCount") : null;
                String countText = count != null ? String.valueOf(count) : "your";
                yield "Your recurring appointments with " + practiceName + " are confirmed ("
                        + countText + " sessions, first " + dateTime + ")." + STOP_LINE;
            }
            case NotificationEventCatalog.SESSION_RESCHEDULED ->
                    "Your appointment with " + practiceName + " has been rescheduled to " + dateTime + "." + STOP_LINE;
            case NotificationEventCatalog.SESSION_CANCELLED ->
                    "Your appointment with " + practiceName + " on " + dateTime + " has been cancelled." + STOP_LINE;
            case NotificationEventCatalog.SESSION_REMINDER ->
                    "Reminder: your appointment with " + practiceName + " is on " + dateTime + "." + STOP_LINE;
            default -> null;
        };
    }

    String formatSmsDateTime(Object value) {
        return formatSmsDateTime(value, null);
    }

    String formatSmsDateTime(Object value, ZoneId recipientZone) {
        Instant instant = toInstant(value);
        if (instant == null) {
            return "your scheduled time";
        }
        ZoneId zoneId = recipientZone != null ? recipientZone : resolveTimezone();
        return SMS_DATE_TIME.format(instant.atZone(zoneId));
    }

    /**
     * Delivery stamps the recipient's own zone onto the payload. Fall back to the practice
     * timezone only when it is absent, so an SMS never quotes a stranger's local time.
     */
    private ZoneId resolveRecipientTimezone(Map<String, Object> entityData) {
        if (entityData == null) {
            return null;
        }
        Object raw = entityData.get("recipientTimezone");
        if (!(raw instanceof String zoneId) || !StringUtils.hasText(zoneId)) {
            return null;
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object resolveDateValue(Map<String, Object> entityData, boolean isScheduled) {
        if (entityData == null) {
            return null;
        }
        if (isScheduled) {
            return firstNonNull(entityData.get("sessionDate"), entityData.get("firstSessionDate"), entityData.get("executeAt"));
        }
        return firstNonNull(
                entityData.get("sessionDate"),
                entityData.get("firstSessionDate"),
                entityData.get("executeAt"));
    }

    private String resolvePracticeName() {
        try {
            PracticeConfigurationResponse config = practiceConfigurationService.getPracticeConfiguration();
            if (config != null && StringUtils.hasText(config.getPracticeName())) {
                return config.getPracticeName().trim();
            }
        } catch (Exception ignored) {
            // Fall back to default practice name
        }
        return DEFAULT_PRACTICE_NAME;
    }

    private ZoneId resolveTimezone() {
        try {
            PracticeConfigurationResponse config = practiceConfigurationService.getPracticeConfiguration();
            if (config != null && StringUtils.hasText(config.getTimezone())) {
                return ZoneId.of(config.getTimezone().trim());
            }
        } catch (Exception ignored) {
            // Fall back to default timezone
        }
        return ZoneId.of(DEFAULT_TIMEZONE);
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Instant.parse(text.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
