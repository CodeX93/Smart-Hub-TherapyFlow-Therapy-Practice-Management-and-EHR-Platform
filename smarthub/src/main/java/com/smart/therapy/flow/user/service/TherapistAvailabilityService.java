package com.smart.therapy.flow.user.service;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.user.entity.TherapistBlockedTime;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.user.dto.CreateTherapistBlockedTimeRequest;
import com.smart.therapy.flow.user.dto.TherapistBlockedTimeResponse;
import com.smart.therapy.flow.user.dto.AvailableSlotResponse;
import com.smart.therapy.flow.user.dto.TherapistAvailabilityPublicResponse;
import com.smart.therapy.flow.user.dto.TherapistScheduleResponse;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.user.repository.TherapistBlockedTimeRepository;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.user.entity.ShiftMode;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TherapistAvailabilityService {

    private final TherapistBlockedTimeRepository blockedTimeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SessionRepository sessionRepository;
    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;
    private final TimezoneService timezoneService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;

    @Transactional(readOnly = true)
    public List<TherapistBlockedTimeResponse> getTherapistBlockedTimes(Long therapistId) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");

        List<TherapistBlockedTime> blockedTimes = blockedTimeRepository
                .findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(therapistId);
        return blockedTimes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TherapistBlockedTimeResponse getBlockedTime(Long id) {
        Objects.requireNonNull(id, "Blocked time ID is required");

        TherapistBlockedTime blockedTime = blockedTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist blocked time not found"));

        return toResponse(blockedTime);
    }

    @Transactional
    public TherapistBlockedTimeResponse createBlockedTime(CreateTherapistBlockedTimeRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Only therapists, supervisors, and admins can create blocked times
        // Therapists can only create for themselves unless they're admin/supervisor
        // PBAC: Check permission to manage availability
        boolean canManageAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        if (!canManageAll && !canViewTeam) {
            Long requesterUserId = currentUserService.getCurrentUserId(requester);
            if (requesterUserId == null || !requesterUserId.equals(request.getTherapistId())) {
                throw new ForbiddenException("You can only create blocked times for yourself");
            }
        }

        User therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        // Validate time range
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        TherapistBlockedTime blockedTime = TherapistBlockedTime.builder()
                .therapist(therapist)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .allDay(request.getAllDay() != null ? request.getAllDay() : false)
                .blockType(request.getBlockType())
                .reason(request.getReason())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurrencePattern(request.getRecurrencePattern())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TherapistBlockedTime saved = blockedTimeRepository.save(blockedTime);
        return toResponse(saved);
    }

    @Transactional
    public TherapistBlockedTimeResponse updateBlockedTime(Long id, CreateTherapistBlockedTimeRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(id, "Blocked time ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        TherapistBlockedTime blockedTime = blockedTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist blocked time not found"));

        // Only therapists, supervisors, and admins can update blocked times
        // Therapists can only update their own unless they're admin/supervisor
        // PBAC: Check permission to manage availability
        boolean canManageAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        if (!canManageAll && !canViewTeam) {
            Long requesterUserId = currentUserService.getCurrentUserId(requester);
            if (requesterUserId == null || !requesterUserId.equals(blockedTime.getTherapist().getId())) {
                throw new ForbiddenException("You can only update your own blocked times");
            }
        }

        // Update fields
        if (request.getTherapistId() != null && !request.getTherapistId().equals(blockedTime.getTherapist().getId())) {
            User therapist = userRepository.findById(request.getTherapistId())
                    .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
            blockedTime.setTherapist(therapist);
        }
        if (request.getStartTime() != null) {
            blockedTime.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            blockedTime.setEndTime(request.getEndTime());
        }
        if (request.getAllDay() != null) {
            blockedTime.setAllDay(request.getAllDay());
        }
        if (request.getBlockType() != null) {
            blockedTime.setBlockType(request.getBlockType());
        }
        if (request.getReason() != null) {
            blockedTime.setReason(request.getReason());
        }
        if (request.getIsRecurring() != null) {
            blockedTime.setIsRecurring(request.getIsRecurring());
        }
        if (request.getRecurrencePattern() != null) {
            blockedTime.setRecurrencePattern(request.getRecurrencePattern());
        }
        if (request.getIsActive() != null) {
            blockedTime.setIsActive(request.getIsActive());
        }

        // Validate time range
        if (blockedTime.getStartTime().isAfter(blockedTime.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        TherapistBlockedTime updated = blockedTimeRepository.save(blockedTime);
        return toResponse(updated);
    }

    @Transactional
    public void deleteBlockedTime(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Blocked time ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        TherapistBlockedTime blockedTime = blockedTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist blocked time not found"));

        // Only therapists, supervisors, and admins can delete blocked times
        // Therapists can only delete their own unless they're admin/supervisor
        // PBAC: Check permission to manage availability
        boolean canManageAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        if (!canManageAll && !canViewTeam) {
            Long requesterUserId = currentUserService.getCurrentUserId(requester);
            if (requesterUserId == null || !requesterUserId.equals(blockedTime.getTherapist().getId())) {
                throw new ForbiddenException("You can only delete your own blocked times");
            }
        }

        blockedTimeRepository.delete(blockedTime);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private TherapistBlockedTimeResponse toResponse(TherapistBlockedTime blockedTime) {
        return TherapistBlockedTimeResponse.builder()
                .id(blockedTime.getId())
                .therapistId(blockedTime.getTherapist() != null ? blockedTime.getTherapist().getId() : null)
                .therapistName(blockedTime.getTherapist() != null ? blockedTime.getTherapist().getFullName() : null)
                .startTime(blockedTime.getStartTime())
                .endTime(blockedTime.getEndTime())
                .allDay(blockedTime.getAllDay())
                .blockType(blockedTime.getBlockType())
                .reason(blockedTime.getReason())
                .isRecurring(blockedTime.getIsRecurring())
                .recurrencePattern(blockedTime.getRecurrencePattern())
                .isActive(blockedTime.getIsActive())
                .createdAt(blockedTime.getCreatedAt())
                .updatedAt(blockedTime.getUpdatedAt())
                .build();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableTimeSlots(
            Long therapistId, LocalDate date, Long serviceId) {
        return getAvailableTimeSlots(therapistId, date, serviceId, null, null);
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableTimeSlots(
            Long therapistId, LocalDate date, Long serviceId, String clientTimezone) {
        return getAvailableTimeSlots(therapistId, date, serviceId, clientTimezone, null);
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableTimeSlots(
            Long therapistId, LocalDate date, Long serviceId, String clientTimezone, String sessionType) {
        return getAvailableTimeSlots(therapistId, date, serviceId, clientTimezone, sessionType, null);
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableTimeSlots(
            Long therapistId, LocalDate date, Long serviceId, String clientTimezone, String sessionType,
            Integer durationOverrideMinutes) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        Objects.requireNonNull(date, "Date is required");
        Objects.requireNonNull(serviceId, "Service ID is required");

        SessionType requestedSessionType = parseSessionType(sessionType);

        // Get therapist's timezone (required)
        ZoneId therapistTz = timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        // Convert client's date to therapist's timezone if client timezone is provided
        LocalDate therapistDate = date;
        ZoneId clientTz = null;
        if (clientTimezone != null && !clientTimezone.isBlank()) {
            try {
                clientTz = ZoneId.of(clientTimezone);
                therapistDate = timezoneService.convertDateToTherapistTimezone(date, clientTz, therapistTz);
            } catch (Exception e) {
                log.warn("Invalid client timezone: {}, using provided date as-is", clientTimezone, e);
            }
        }

        // Get therapist profile for working hours
        UserProfile profile = userProfileRepository.findByUserId(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist profile not found"));

        // Get service duration (public site can override with its own duration)
        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        int sessionDuration = durationOverrideMinutes != null && durationOverrideMinutes > 0
                ? durationOverrideMinutes
                : (service.getDuration() != null ? service.getDuration()
                        : (profile.getSessionDuration() != null ? profile.getSessionDuration() : 60));

        // Get working hours for the specific day (supports multiple shifts)
        java.time.DayOfWeek dayOfWeek = therapistDate.getDayOfWeek();
        String dayName = dayOfWeek.name(); // e.g., "MONDAY"

        List<UserProfileWorkingHours> dayShifts = profile.getWorkingHours() != null
                ? profile.getWorkingHours().stream()
                        .filter(h -> h.getDay().equalsIgnoreCase(dayName))
                        .filter(h -> matchesWorkingHoursService(h, service))
                        .sorted(Comparator.comparing(UserProfileWorkingHours::getStartTime))
                        .collect(Collectors.toList())
                : null;

        if (dayShifts == null || dayShifts.isEmpty()) {
            return List.of(); // Not a working day
        }

        // Get blocked times for this day in therapist's timezone
        Instant dayStart = timezoneService.getStartOfDay(therapistDate, therapistTz);
        Instant rangeEndExclusive = therapistDate.plusDays(1).atStartOfDay(therapistTz).toInstant();
        List<TherapistBlockedTime> blockedTimes = blockedTimeRepository
                .findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(therapistId)
                .stream()
                .filter(bt -> bt.getStartTime() != null
                        && !bt.getStartTime().isBefore(dayStart)
                        && bt.getStartTime().isBefore(rangeEndExclusive))
                .collect(Collectors.toList());

        // Date-scoped queries only — never sessionRepository.findAll() (that decrypts every row).
        List<Session> existingSessions = sessionRepository
                .findByTherapistAndDateRangeWithRoom(therapistId, dayStart, rangeEndExclusive)
                .stream()
                .filter(s -> s.getStatus() == null
                        || !SystemOptionKeyMatcher.matchesAny(s.getStatus(), "cancelled"))
                .collect(Collectors.toList());

        List<Session> allSessionsToday = sessionRepository
                .findByDateRangeWithRoom(dayStart, rangeEndExclusive)
                .stream()
                .filter(s -> s.getStatus() == null
                        || !SystemOptionKeyMatcher.matchesAny(s.getStatus(), "cancelled"))
                .collect(Collectors.toList());

        List<Long> physicalRoomIds = profile.getAvailablePhysicalRooms() != null
                ? profile.getAvailablePhysicalRooms().stream()
                        .map(pr -> pr.getRoom() != null ? pr.getRoom().getId() : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                : List.of();

        Map<Instant, AvailableSlotResponse> slotsByStart = new LinkedHashMap<>();
        for (UserProfileWorkingHours shift : dayShifts) {
            ShiftMode shiftMode = shift.getSessionMode() != null ? shift.getSessionMode() : ShiftMode.BOTH;
            if (!isShiftCompatible(shiftMode, requestedSessionType)) {
                continue;
            }

            LocalTime currentTime = shift.getStartTime();
            LocalTime shiftEnd = shift.getEndTime();
            while (currentTime.isBefore(shiftEnd)) {
                Instant slotStart = timezoneService.convertToUtc(therapistDate.atTime(currentTime), therapistTz);
                Instant slotEnd = slotStart.plus(sessionDuration, ChronoUnit.MINUTES);

                LocalTime slotEndLocalTime = timezoneService.convertFromUtc(slotEnd, therapistTz).toLocalTime();
                if (slotEndLocalTime.isAfter(shiftEnd) && !slotEndLocalTime.equals(shiftEnd)) {
                    break;
                }

                boolean isBlocked = blockedTimes.stream().anyMatch(blocked ->
                        slotStart.isBefore(blocked.getEndTime()) && slotEnd.isAfter(blocked.getStartTime()));

                boolean hasSession = existingSessions.stream().anyMatch(session -> {
                    Instant sessionStart = session.getSessionDate();
                    int actualDuration = session.getDuration() != null ? session.getDuration() : sessionDuration;
                    Instant sessionEnd = sessionStart.plus(actualDuration, ChronoUnit.MINUTES);
                    return slotStart.isBefore(sessionEnd) && slotEnd.isAfter(sessionStart);
                });

                boolean roomAvailable = !hasSession && isRoomAvailableForMode(
                        shiftMode, requestedSessionType, slotStart, slotEnd, allSessionsToday, sessionDuration, physicalRoomIds);

                boolean available = !isBlocked && !hasSession && roomAvailable
                        && slotStart.isAfter(Instant.now());
                AvailableSlotResponse.AvailableSlotResponseBuilder builder = AvailableSlotResponse.builder()
                        .time(slotStart)
                        .timezone(therapistTz.getId())
                        .localTime(timezoneService.convertFromUtc(slotStart, therapistTz))
                        .available(available)
                        .therapistBusy(hasSession)
                        .roomBusy(!roomAvailable)
                        .sessionMode(toPublicBookingSessionMode(shiftMode));
                if (clientTz != null) {
                    builder.clientLocalTime(timezoneService.convertFromUtc(slotStart, clientTz))
                            .clientTimezone(clientTz.getId());
                }
                // If overlapping shifts generate same slot start, keep the first computed slot.
                slotsByStart.putIfAbsent(slotStart, builder.build());
                currentTime = currentTime.plusMinutes(sessionDuration);
            }
        }

        return new ArrayList<>(slotsByStart.values());
    }

    /**
     * Resolve the booking modality for a public consultation from the therapist's
     * consultation (or scoped service) schedule covering {@code sessionStart}.
     * Public visitors do not choose online vs in-person.
     */
    @Transactional(readOnly = true)
    public String resolveSessionModeForPublicBooking(
            Long therapistId, Instant sessionStart, Integer durationMinutes, Long serviceId) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        Objects.requireNonNull(sessionStart, "Session start is required");
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BadRequestException("Session duration is required");
        }

        ZoneId therapistTz = timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        java.time.ZonedDateTime localStart = sessionStart.atZone(therapistTz);
        Instant sessionEnd = sessionStart.plus(durationMinutes, ChronoUnit.MINUTES);
        java.time.ZonedDateTime localEnd = sessionEnd.atZone(therapistTz);
        LocalDate therapistDate = localStart.toLocalDate();
        LocalTime startLocal = localStart.toLocalTime();
        LocalTime endLocal = localEnd.toLocalTime();

        UserProfile profile = userProfileRepository.findByUserId(therapistId)
                .orElseThrow(() -> new BadRequestException(
                        "Therapist schedule is not configured. Set working hours before booking."));

        com.smart.therapy.flow.billing.entity.Service scheduleService = null;
        if (serviceId != null) {
            scheduleService = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        }

        String dayName = therapistDate.getDayOfWeek().name();
        final com.smart.therapy.flow.billing.entity.Service scheduleServiceFinal = scheduleService;
        List<UserProfileWorkingHours> dayShifts = profile.getWorkingHours() != null
                ? profile.getWorkingHours().stream()
                        .filter(h -> h.getDay() != null && h.getDay().equalsIgnoreCase(dayName))
                        .filter(h -> matchesWorkingHoursService(h, scheduleServiceFinal))
                        .sorted(Comparator.comparing(UserProfileWorkingHours::getStartTime))
                        .collect(Collectors.toList())
                : List.of();

        for (UserProfileWorkingHours shift : dayShifts) {
            LocalTime shiftStart = shift.getStartTime();
            LocalTime shiftEnd = shift.getEndTime();
            if (shiftStart == null || shiftEnd == null) {
                continue;
            }
            boolean startsOk = !startLocal.isBefore(shiftStart);
            boolean endsOk = !endLocal.isAfter(shiftEnd)
                    || (endLocal.equals(LocalTime.MIDNIGHT) && shiftEnd.equals(LocalTime.MIDNIGHT));
            if (startsOk && endsOk) {
                ShiftMode mode = shift.getSessionMode() != null ? shift.getSessionMode() : ShiftMode.BOTH;
                return toPublicBookingSessionMode(mode);
            }
        }
        throw new BadRequestException(
                "Selected time is outside the therapist's consultation availability for that day.");
    }

    /** Map shift modality to the session type stored on Session / used for Zoom. */
    private static String toPublicBookingSessionMode(ShiftMode shiftMode) {
        return switch (shiftMode) {
            case IN_PERSON -> "in-person";
            case VIRTUAL, BOTH -> "online";
        };
    }

    private SessionType parseSessionType(String sessionType) {
        if (!StringUtils.hasText(sessionType)) {
            return null;
        }
        try {
            return SessionType.fromValue(sessionType);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sessionType. Use 'online' or 'in-person'.");
        }
    }

    private boolean isShiftCompatible(ShiftMode shiftMode, SessionType requestedSessionType) {
        if (requestedSessionType == null || shiftMode == ShiftMode.BOTH) {
            return true;
        }
        return (shiftMode == ShiftMode.VIRTUAL && requestedSessionType == SessionType.ONLINE)
                || (shiftMode == ShiftMode.IN_PERSON && requestedSessionType == SessionType.IN_PERSON);
    }

    /**
     * Validates that the therapist has working hours covering the requested slot for the
     * requested modality (online vs in-person). Used by single booking and bulk upload so a
     * bulk online booking cannot land on an in-person-only shift (and vice versa).
     *
     * @throws BadRequestException when the day has no compatible schedule or the slot is outside it
     */
    @Transactional(readOnly = true)
    public void assertTherapistAvailableForBooking(
            Long therapistId,
            Instant sessionStart,
            Integer durationMinutes,
            String sessionModeKey) {
        assertTherapistAvailableForBooking(therapistId, sessionStart, durationMinutes, sessionModeKey, null);
    }

    /**
     * Validates therapist schedule for a booking. When {@code serviceId} is set, uses that service's
     * working hours (e.g. Consultation); otherwise uses All-Services hours (service_id null).
     */
    @Transactional(readOnly = true)
    public void assertTherapistAvailableForBooking(
            Long therapistId,
            Instant sessionStart,
            Integer durationMinutes,
            String sessionModeKey,
            Long serviceId) {
        assertTherapistAvailableForBooking(therapistId, sessionStart, durationMinutes, sessionModeKey, serviceId, null);
    }

    /** Validate an edit without treating the session being edited as an overlapping booking. */
    @Transactional(readOnly = true)
    public void assertTherapistAvailableForBooking(
            Long therapistId, Instant sessionStart, Integer durationMinutes, String sessionModeKey,
            Long serviceId, Long excludedSessionId) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        Objects.requireNonNull(sessionStart, "Session start is required");
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BadRequestException("Session duration is required to validate therapist availability");
        }

        SessionType requestedSessionType = toAvailabilitySessionType(sessionModeKey);
        ZoneId therapistTz = timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        java.time.ZonedDateTime localStart = sessionStart.atZone(therapistTz);
        Instant sessionEnd = sessionStart.plus(durationMinutes, ChronoUnit.MINUTES);
        java.time.ZonedDateTime localEnd = sessionEnd.atZone(therapistTz);
        LocalDate therapistDate = localStart.toLocalDate();
        LocalTime startLocal = localStart.toLocalTime();
        LocalTime endLocal = localEnd.toLocalTime();

        if (localEnd.toLocalDate().isAfter(therapistDate)
                && !(endLocal.equals(LocalTime.MIDNIGHT) || endLocal.equals(LocalTime.MIN))) {
            // still validate against the start day's shifts only
        }

        UserProfile profile = userProfileRepository.findByUserId(therapistId)
                .orElseThrow(() -> new BadRequestException(
                        "Therapist schedule is not configured. Set working hours before booking."));

        com.smart.therapy.flow.billing.entity.Service scheduleService = null;
        if (serviceId != null) {
            scheduleService = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        }

        String dayName = therapistDate.getDayOfWeek().name();
        final com.smart.therapy.flow.billing.entity.Service scheduleServiceFinal = scheduleService;
        List<UserProfileWorkingHours> dayShifts = profile.getWorkingHours() != null
                ? profile.getWorkingHours().stream()
                        .filter(h -> h.getDay() != null && h.getDay().equalsIgnoreCase(dayName))
                        .filter(h -> matchesWorkingHoursService(h, scheduleServiceFinal))
                        .sorted(Comparator.comparing(UserProfileWorkingHours::getStartTime))
                        .collect(Collectors.toList())
                : List.of();

        if (dayShifts.isEmpty()) {
            throw new BadRequestException(
                    "Therapist has no working hours for "
                            + dayName.substring(0, 1) + dayName.substring(1).toLowerCase()
                            + ". Choose a day that is on their schedule.");
        }

        List<UserProfileWorkingHours> compatibleShifts = dayShifts.stream()
                .filter(shift -> {
                    ShiftMode mode = shift.getSessionMode() != null ? shift.getSessionMode() : ShiftMode.BOTH;
                    return isShiftCompatible(mode, requestedSessionType);
                })
                .collect(Collectors.toList());

        String modeLabel = requestedSessionType == SessionType.ONLINE
                ? "online/virtual"
                : (requestedSessionType == SessionType.IN_PERSON ? "in-person" : "this");

        if (compatibleShifts.isEmpty()) {
            String availableModes = dayShifts.stream()
                    .map(s -> s.getSessionMode() != null ? s.getSessionMode().toJsonValue() : "both")
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(
                    "Therapist schedule for "
                            + dayName.substring(0, 1) + dayName.substring(1).toLowerCase()
                            + " does not allow " + modeLabel + " sessions"
                            + (availableModes.isBlank() ? "" : " (configured: " + availableModes + ")")
                            + ". Adjust session mode or therapist working hours.");
        }

        boolean fitsShift = compatibleShifts.stream().anyMatch(shift -> {
            LocalTime shiftStart = shift.getStartTime();
            LocalTime shiftEnd = shift.getEndTime();
            if (shiftStart == null || shiftEnd == null) {
                return false;
            }
            boolean startsOk = !startLocal.isBefore(shiftStart);
            boolean endsOk = !endLocal.isAfter(shiftEnd)
                    || (endLocal.equals(LocalTime.MIDNIGHT) && shiftEnd.equals(LocalTime.MIDNIGHT));
            return startsOk && endsOk;
        });

        if (!fitsShift) {
            String windows = compatibleShifts.stream()
                    .map(s -> s.getStartTime() + "–" + s.getEndTime()
                            + " (" + (s.getSessionMode() != null ? s.getSessionMode().toJsonValue() : "both") + ")")
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(
                    "Selected time is outside the therapist's " + modeLabel + " availability for that day. "
                            + "Available " + modeLabel + " windows: " + windows + ".");
        }

        Instant dayStart = timezoneService.getStartOfDay(therapistDate, therapistTz);
        Instant dayEnd = timezoneService.getEndOfDay(therapistDate, therapistTz);
        boolean blocked = blockedTimeRepository
                .findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(therapistId)
                .stream()
                .filter(bt -> bt.getStartTime() != null && bt.getEndTime() != null)
                .filter(bt -> bt.getStartTime().isBefore(dayEnd) && bt.getEndTime().isAfter(dayStart))
                .anyMatch(bt -> sessionStart.isBefore(bt.getEndTime()) && sessionEnd.isAfter(bt.getStartTime()));
        if (blocked) {
            throw new BadRequestException(
                    "Therapist is blocked for the selected date/time. Choose another slot.");
        }

        // Cross-service conflict: any non-cancelled session blocks the slot
        Instant searchStart = sessionStart.minus(480, ChronoUnit.MINUTES);
        boolean hasConflict = sessionRepository.findByTherapistAndDateRange(therapistId, searchStart, sessionEnd)
                .stream()
                .filter(s -> excludedSessionId == null || !excludedSessionId.equals(s.getId()))
                .filter(s -> s.getStatus() == null
                        || !SystemOptionKeyMatcher.matchesAny(s.getStatus(), "cancelled"))
                .anyMatch(s -> {
                    Instant sStart = s.getSessionDate();
                    int sDuration = s.getDuration() != null ? s.getDuration() : durationMinutes;
                    Instant sEnd = sStart.plus(sDuration, ChronoUnit.MINUTES);
                    return sessionStart.isBefore(sEnd) && sessionEnd.isAfter(sStart);
                });
        if (hasConflict) {
            throw new BadRequestException("This time slot is no longer available. Please select another time.");
        }
    }

    /**
     * All-Services hours have null service_id. Consultation (and other service-scoped) hours
     * must match the requested service id. When requesting a non-scoped service, use null hours.
     */
    private boolean matchesWorkingHoursService(
            UserProfileWorkingHours hours,
            com.smart.therapy.flow.billing.entity.Service service) {
        return WorkingHoursServiceMatcher.matches(hours, service);
    }

    /**
     * Map system session mode keys to availability ONLINE / IN_PERSON filters.
     */
    private SessionType toAvailabilitySessionType(String sessionModeKey) {
        if (!StringUtils.hasText(sessionModeKey)) {
            return null;
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionModeKey, "online", "virtual", "telehealth", "video")) {
            return SessionType.ONLINE;
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionModeKey, "in-person", "in_person", "inperson")) {
            return SessionType.IN_PERSON;
        }
        // Unknown/custom modes: do not block booking on modality mismatch.
        return null;
    }

    private boolean isRoomAvailableForMode(
            ShiftMode shiftMode,
            SessionType requestedSessionType,
            Instant slotStart,
            Instant slotEnd,
            List<Session> allSessionsToday,
            int sessionDuration,
            List<Long> physicalRoomIds) {
        SessionType effectiveSessionType = requestedSessionType;
        if (effectiveSessionType == null) {
            effectiveSessionType = switch (shiftMode) {
                case VIRTUAL -> SessionType.ONLINE;
                case IN_PERSON -> SessionType.IN_PERSON;
                case BOTH -> null;
            };
        }

        if (effectiveSessionType == SessionType.ONLINE) {
            // Online/virtual sessions do not require a room assignment.
            // Availability is based on therapist schedule + existing sessions only.
            return true;
        }

        if (effectiveSessionType == SessionType.IN_PERSON) {
            if (physicalRoomIds.isEmpty()) {
                return false;
            }
            return physicalRoomIds.stream().anyMatch(roomId ->
                    allSessionsToday.stream().noneMatch(session ->
                            session.getRoom() != null
                                    && session.getRoom().getId().equals(roomId)
                                    && overlaps(session, slotStart, slotEnd, sessionDuration)));
        }

        // Legacy/unspecified mode: accept if any free physical room exists, otherwise
        // allow the slot when no physical rooms are configured (online-style week).
        if (!physicalRoomIds.isEmpty()) {
            return physicalRoomIds.stream().anyMatch(roomId ->
                    allSessionsToday.stream().noneMatch(session ->
                            session.getRoom() != null
                                    && session.getRoom().getId().equals(roomId)
                                    && overlaps(session, slotStart, slotEnd, sessionDuration)));
        }
        return true;
    }

    private boolean overlaps(Session session, Instant slotStart, Instant slotEnd, int defaultDuration) {
        Instant sessionStart = session.getSessionDate();
        int actualDuration = session.getDuration() != null ? session.getDuration() : defaultDuration;
        Instant sessionEnd = sessionStart.plus(actualDuration, ChronoUnit.MINUTES);
        return slotStart.isBefore(sessionEnd) && slotEnd.isAfter(sessionStart);
    }

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility in business logic validation
    // For access control, always use principal.hasPermission() instead
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleName));
    }

    @Transactional(readOnly = true)
    public TherapistAvailabilityPublicResponse getPublicTherapistAvailability(
            Long therapistId, LocalDate date, Long serviceId, String clientTimezone) {
        return getPublicTherapistAvailability(therapistId, date, serviceId, clientTimezone, null);
    }

    @Transactional(readOnly = true)
    public TherapistAvailabilityPublicResponse getPublicTherapistAvailability(
            Long therapistId, LocalDate date, Long serviceId, String clientTimezone, String sessionType) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        Objects.requireNonNull(date, "Date is required");
        Objects.requireNonNull(serviceId, "Service ID is required");

        // Get available slots using existing method
        List<AvailableSlotResponse> availableSlots = getAvailableTimeSlots(
                therapistId, date, serviceId, clientTimezone, sessionType);

        // Filter to only available slots
        List<AvailableSlotResponse> filteredSlots = availableSlots.stream()
                .filter(AvailableSlotResponse::isAvailable)
                .collect(Collectors.toList());

        // Get therapist name
        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        return TherapistAvailabilityPublicResponse.builder()
                .therapistId(therapistId)
                .therapistName(therapist.getFullName())
                .date(date)
                .availableSlots(filteredSlots)
                .hasAvailability(!filteredSlots.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public TherapistScheduleResponse getTherapistSchedule(Long therapistId, LocalDate date) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        Objects.requireNonNull(date, "Date is required");

        // Get therapist
        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        // Get day boundaries
        ZoneId therapistTz = timezoneService.getTherapistTimezone(therapistId)
                .orElse(timezoneService.getPracticeTimezone());
        Instant dayStart = timezoneService.getStartOfDay(date, therapistTz);
        Instant dayEnd = timezoneService.getEndOfDay(date, therapistTz);

        // Get all sessions for this therapist on this day
        List<Session> sessions = sessionRepository.findByTherapistAndDateRange(
                therapistId, dayStart, dayEnd);

        // Convert to schedule sessions
        List<TherapistScheduleResponse.ScheduleSession> scheduleSessions = sessions.stream()
                .map(session -> {
                    int duration = session.getDuration() != null ? session.getDuration() : 60;
                    return TherapistScheduleResponse.ScheduleSession.builder()
                            .sessionId(session.getId())
                            .clientId(session.getClient() != null ? session.getClient().getId() : null)
                            .clientName(session.getClient() != null ? session.getClient().getFullName() : null)
                            .startTime(session.getSessionDate())
                            .duration(duration)
                            .sessionType(
                                    session.getClinicalSessionType() != null
                                            ? session.getClinicalSessionType()
                                            : (session.getService() != null
                                                    ? (session.getService().getCategory() != null
                                                            ? session.getService().getCategory()
                                                            : session.getService().getServiceName())
                                                    : null))
                            .sessionMode(session.getSessionType())
                            .status(session.getStatus())
                            .roomId(session.getRoom() != null ? session.getRoom().getId() : null)
                            .roomName(session.getRoom() != null ? session.getRoom().getRoomName() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return TherapistScheduleResponse.builder()
                .therapistId(therapistId)
                .therapistName(therapist.getFullName())
                .date(date)
                .sessions(scheduleSessions)
                .totalSessions(scheduleSessions.size())
                .build();
    }
}

