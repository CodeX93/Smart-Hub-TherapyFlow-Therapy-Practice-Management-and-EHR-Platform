package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.system.repository.PracticeConfigurationRepository;
import com.smart.therapy.flow.system.entity.PracticeConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for handling timezone conversions and timezone-aware operations.
 * Ensures proper scheduling across different timezones for therapists and clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimezoneService {

    public static final ZoneId DEFAULT_PRACTICE_ZONE = ZoneId.of("UTC");

    private static final DateTimeFormatter ZOOM_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Map<String, String> TIMEZONE_ALIASES = Map.of(
            "asia/pakistan", "Asia/Karachi",
            "pakistan", "Asia/Karachi"
    );

    private final UserProfileRepository userProfileRepository;
    private final ClientRepository clientRepository;
    private final PracticeConfigurationRepository practiceConfigurationRepository;
    private final OrganisationRepository organisationRepository;

    /**
     * Resolve the practice timezone configured in Administration settings.
     * Returns empty when unset so callers can decide their own fallback.
     */
    public Optional<String> findPracticeTimezoneId() {
        try {
            PracticeConfiguration config = practiceConfigurationRepository.findFirstByOrderByIdAsc().orElse(null);
            if (config != null && config.getTimezone() != null && !config.getTimezone().isBlank()) {
                return Optional.of(normalizeTimezoneId(config.getTimezone().trim()));
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve practice timezone from settings", ex);
        }
        return findOrganisationTimezoneId();
    }

    /**
     * Onboarding records the clinic's timezone on the organisation, while Administration's
     * practice configuration is only written when someone opens that screen. Without this
     * fallback a tenant reads as UTC no matter what was chosen when it was created, and
     * every "defaults to the clinic" rule silently means UTC.
     */
    private Optional<String> findOrganisationTimezoneId() {
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            return Optional.empty();
        }
        try {
            return organisationRepository.findById(organisationId)
                    .map(Organisation::getTimezone)
                    .filter(zone -> zone != null && !zone.isBlank())
                    .map(zone -> normalizeTimezoneId(zone.trim()));
        } catch (Exception ex) {
            log.warn("Failed to resolve organisation timezone for organisation {}", organisationId, ex);
            return Optional.empty();
        }
    }

    /**
     * Return the Administration practice timezone used for tenant business dates.
     * Server timezone is deliberately ignored; missing configuration falls back to UTC.
     */
    public ZoneId getPracticeTimezone() {
        return findPracticeTimezoneId()
                .map(ZoneId::of)
                .orElse(DEFAULT_PRACTICE_ZONE);
    }

    /**
     * Get therapist's timezone from their profile.
     * Returns practice timezone fallback if timezone is not set.
     */
    public java.util.Optional<ZoneId> getTherapistTimezone(Long therapistId) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        
        try {
            UserProfile profile = userProfileRepository.findByUserId(therapistId)
                    .orElse(null);
            
            if (profile != null && profile.getTimezone() != null && !profile.getTimezone().isBlank()) {
                return java.util.Optional.of(ZoneId.of(profile.getTimezone()));
            }
        } catch (Exception e) {
            log.warn("Failed to get therapist timezone for therapistId: {}", therapistId, e);
        }
        
        return java.util.Optional.of(getPracticeTimezone());
    }

    /**
     * Resolve timezone for client portal APIs.
     * <p>
     * Order: explicit {@code timezoneParam} → client profile → Administration practice
     * timezone. A client who never picks one follows the clinic; the server's own
     * timezone is deliberately never consulted.
     */
    public ZoneId resolveClientPortalZone(Long clientId, String timezoneParam) {
        Objects.requireNonNull(clientId, "Client ID is required");
        if (timezoneParam != null && !timezoneParam.isBlank()) {
            return ZoneId.of(normalizeTimezoneId(timezoneParam.trim()));
        }
        return getClientTimezone(clientId).orElseGet(this::getPracticeTimezone);
    }

    /**
     * Get client's timezone from their profile.
     * Returns practice timezone fallback if timezone is not set.
     */
    public java.util.Optional<ZoneId> getClientTimezone(Long clientId) {
        Objects.requireNonNull(clientId, "Client ID is required");
        
        try {
            Client client = clientRepository.findById(clientId)
                    .orElse(null);
            
            if (client != null && client.getTimezone() != null && !client.getTimezone().isBlank()) {
                return java.util.Optional.of(ZoneId.of(client.getTimezone()));
            }
        } catch (Exception e) {
            log.warn("Failed to get client timezone for clientId: {}", clientId, e);
        }
        
        return java.util.Optional.of(getPracticeTimezone());
    }

    /**
     * Convert a LocalDateTime in a specific timezone to UTC Instant.
     */
    public Instant convertToUtc(LocalDateTime localDateTime, ZoneId timezone) {
        Objects.requireNonNull(localDateTime, "Local date time is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        
        return localDateTime.atZone(timezone).toInstant();
    }

    /**
     * Convert a UTC Instant to LocalDateTime in a specific timezone.
     */
    public LocalDateTime convertFromUtc(Instant utcTime, ZoneId timezone) {
        Objects.requireNonNull(utcTime, "UTC time is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        
        return utcTime.atZone(timezone).toLocalDateTime();
    }

    /**
     * Convert a LocalDateTime from one timezone to another.
     */
    public LocalDateTime convertBetweenTimezones(
            LocalDateTime sourceTime,
            ZoneId sourceTimezone,
            ZoneId targetTimezone
    ) {
        Objects.requireNonNull(sourceTime, "Source time is required");
        Objects.requireNonNull(sourceTimezone, "Source timezone is required");
        Objects.requireNonNull(targetTimezone, "Target timezone is required");
        
        Instant utcInstant = sourceTime.atZone(sourceTimezone).toInstant();
        return utcInstant.atZone(targetTimezone).toLocalDateTime();
    }

    /**
     * Convert a LocalDate from client's timezone to therapist's timezone.
     * Handles cases where the date might be different in different timezones.
     */
    public LocalDate convertDateToTherapistTimezone(LocalDate clientDate, ZoneId clientTimezone, ZoneId therapistTimezone) {
        Objects.requireNonNull(clientDate, "Client date is required");
        Objects.requireNonNull(clientTimezone, "Client timezone is required");
        Objects.requireNonNull(therapistTimezone, "Therapist timezone is required");
        
        // Convert client's date at midnight in their timezone to therapist's timezone
        LocalDateTime clientMidnight = clientDate.atStartOfDay();
        Instant utcInstant = clientMidnight.atZone(clientTimezone).toInstant();
        LocalDateTime therapistDateTime = utcInstant.atZone(therapistTimezone).toLocalDateTime();
        
        return therapistDateTime.toLocalDate();
    }

    /**
     * Format Instant for Zoom API (ISO 8601 format with timezone).
     */
    public String formatZoomDateTime(Instant instant, ZoneId timezone) {
        Objects.requireNonNull(instant, "Instant is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        
        ZonedDateTime zonedDateTime = instant.atZone(timezone);
        return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .replace("+00:00", "Z")
                .replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
    }

    /**
     * Format Instant for display in a specific timezone.
     */
    public String formatForDisplay(Instant instant, ZoneId timezone) {
        Objects.requireNonNull(instant, "Instant is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        
        ZonedDateTime zonedDateTime = instant.atZone(timezone);
        return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    private static final DateTimeFormatter SESSION_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z", Locale.US);
    private static final DateTimeFormatter SESSION_DATE_LONG =
            DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter SESSION_TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter SESSION_ZONE_ABBR =
            DateTimeFormatter.ofPattern("z", Locale.US);

    /**
     * Format a session Instant for notifications/emails using Administration practice timezone.
     * Example: {@code Sep 17, 2026 2:00 PM EDT (America/Toronto)}
     */
    public String formatSessionDateTimeForPractice(Instant instant) {
        return formatSessionDateTime(instant, getPracticeTimezone());
    }

    /**
     * Format a session Instant in the given zone, including zone abbreviation and IANA id.
     */
    public String formatSessionDateTime(Instant instant, ZoneId timezone) {
        Objects.requireNonNull(instant, "Instant is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        return SESSION_DATE_TIME.withZone(timezone).format(instant) + " (" + timezone.getId() + ")";
    }

    /**
     * Long weekday date in practice timezone (email Date row).
     * Example: {@code Thursday, Sep 17, 2026}
     */
    public String formatSessionDateOnlyForPractice(Instant instant) {
        return formatSessionDateOnly(instant, getPracticeTimezone());
    }

    /**
     * Long date in the given zone. Example: {@code Thursday, Sep 17, 2026}
     */
    public String formatSessionDateOnly(Instant instant, ZoneId timezone) {
        Objects.requireNonNull(instant, "Instant is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        return SESSION_DATE_LONG.withZone(timezone).format(instant);
    }

    /**
     * Time or time range in practice timezone, with zone abbreviation and IANA id.
     * Example: {@code 2:00 PM - 2:45 PM EDT (America/Toronto)}
     */
    public String formatSessionTimeRangeForPractice(Instant start, Integer durationMinutes) {
        return formatSessionTimeRange(start, durationMinutes, getPracticeTimezone());
    }

    /**
     * Time or time range in the given zone, with zone abbreviation and IANA id.
     * Example: {@code 2:00 PM - 2:45 PM EDT (America/Toronto)}
     */
    public String formatSessionTimeRange(Instant start, Integer durationMinutes, ZoneId timezone) {
        Objects.requireNonNull(start, "Instant is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        String startTime = SESSION_TIME.withZone(timezone).format(start);
        String range;
        if (durationMinutes != null && durationMinutes > 0) {
            Instant end = start.plus(Duration.ofMinutes(durationMinutes));
            range = startTime + " - " + SESSION_TIME.withZone(timezone).format(end);
        } else {
            range = startTime;
        }
        String abbr = SESSION_ZONE_ABBR.withZone(timezone).format(start);
        return range + " " + abbr + " (" + timezone.getId() + ")";
    }

    /**
     * Get the start of day in a specific timezone for a given date.
     */
    public Instant getStartOfDay(LocalDate date, ZoneId timezone) {
        Objects.requireNonNull(date, "Date is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        
        return date.atStartOfDay(timezone).toInstant();
    }

    /**
     * Get the end of day in a specific timezone for a given date.
     */
    public Instant getEndOfDay(LocalDate date, ZoneId timezone) {
        Objects.requireNonNull(date, "Date is required");
        Objects.requireNonNull(timezone, "Timezone is required");
        
        return date.atTime(23, 59, 59, 999_999_999).atZone(timezone).toInstant();
    }

    /**
     * Validate that a timezone string is valid IANA timezone ID.
     */
    public boolean isValidTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return false;
        }
        
        try {
            normalizeTimezoneId(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalize a timezone to a canonical IANA timezone ID.
     * Accepts a small compatibility alias set for legacy/frontend values.
     */
    public String normalizeTimezoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Timezone is required");
        }
        String raw = timezone.trim();
        String normalizedKey = raw.toLowerCase(Locale.ROOT);
        String resolved = TIMEZONE_ALIASES.getOrDefault(normalizedKey, raw);
        return ZoneId.of(resolved).getId();
    }

    /**
     * Return all supported IANA timezone IDs sorted alphabetically.
     */
    public List<String> getAvailableTimezoneIds() {
        return ZoneId.getAvailableZoneIds()
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

}
