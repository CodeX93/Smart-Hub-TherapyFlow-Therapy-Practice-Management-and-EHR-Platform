package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.session.dto.*;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.RecurrenceDateExpander.ExpandedOccurrence;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringSessionService {

    private static final String RESOURCE_TYPE_SESSION = "session";
    private static final int BUSINESS_START_HOUR = 8;
    private static final int BUSINESS_END_HOUR = 0;
    private static final long THERAPIST_LOCK_BASE = 9_000_000_000L;
    private static final long ROOM_LOCK_BASE = 9_100_000_000L;
    private static final List<String> ACTIVE_CONFLICT_STATUSES = List.of(
            "scheduled", "confirmed", "in-progress");
    private static final List<String> CANCELLABLE_STATUSES = List.of(
            "scheduled", "confirmed");

    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final RecurrenceDateExpander recurrenceDateExpander;
    private final TimezoneService timezoneService;
    private final SystemOptionResolverService systemOptionResolverService;
    private final com.smart.therapy.flow.client.util.ClientServiceEligibilityMessages clientServiceEligibilityMessages;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private SessionService.ZoomService zoomService;

    @Autowired(required = false)
    private com.smart.therapy.flow.client.service.ClientContactService clientContactService;

    @Autowired(required = false)
    private com.smart.therapy.flow.client.service.ClientPortalSettingsService clientPortalSettingsService;

    @Transactional(readOnly = true)
    public RecurrencePreviewResponse previewRecurringSessions(RecurrenceRuleRequest request, AuthPrincipal requester) {
        validateCreatePermission(request, requester);
        ZoneId timezone = resolveRecurrenceTimezone(request);
        int duration = resolveDuration(request);
        validateBusinessHours(request.getSessionDate(), duration, timezone);

        List<ExpandedOccurrence> candidates = recurrenceDateExpander.expand(request, timezone);
        List<EvaluatedOccurrence> evaluated = evaluateConflicts(candidates, request, duration, null);

        List<RecurrencePreviewOccurrence> sessions = evaluated.stream()
                .map(this::toPreviewOccurrence)
                .collect(Collectors.toList());

        int conflictCount = (int) evaluated.stream().filter(EvaluatedOccurrence::hasConflict).count();

        return RecurrencePreviewResponse.builder()
                .sessions(sessions)
                .totalRequested(sessions.size())
                .freeCount(sessions.size() - conflictCount)
                .conflictCount(conflictCount)
                .build();
    }

    @Transactional
    @CacheEvict(value = "sessions", allEntries = true)
    public CreateRecurringSessionsResponse createRecurringSessions(
            RecurrenceRuleRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        validateCreatePermission(request, requester);
        ZoneId timezone = resolveRecurrenceTimezone(request);
        int duration = resolveDuration(request);
        validateBusinessHours(request.getSessionDate(), duration, timezone);

        List<ExpandedOccurrence> candidates = recurrenceDateExpander.expand(request, timezone);
        List<EvaluatedOccurrence> evaluated = evaluateConflicts(candidates, request, duration, null);
        List<EvaluatedOccurrence> free = evaluated.stream()
                .filter(occ -> !occ.hasConflict())
                .collect(Collectors.toList());

        if (free.isEmpty()) {
            throw new ConflictException("All recurrence dates conflict with existing sessions", buildAllSkippedResponse(evaluated));
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        if (!client.canReceiveServices()) {
            throw new BadRequestException(clientServiceEligibilityMessages.schedulingBlocked(client));
        }
        User therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        if (client.getAssignedTherapist() != null
                && !java.util.Objects.equals(client.getAssignedTherapist().getId(), request.getTherapistId())) {
            throw new BadRequestException("Session must be with the client's assigned therapist");
        }

        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        String sessionModeKey = systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.SESSION_MODE, request.getSessionMode());
        Room room = null;
        if (SystemOptionKeyMatcher.matchesAny(sessionModeKey, "in-person")) {
            if (request.getRoomId() == null) {
                throw new BadRequestException("Room is required for in-person sessions");
            }
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            if (!Boolean.TRUE.equals(room.getIsActive())) {
                throw new BadRequestException("Selected room is inactive");
            }
            if (room.getRoomType() != RoomType.PHYSICAL) {
                throw new BadRequestException("In-person sessions require a physical room");
            }
        } else if (request.getRoomId() != null) {
            // Online/virtual series ignore any provided room
            room = null;
        }

        validateOnlineSessionZoomRequirements(
                sessionModeKey,
                request.getZoomEnabled(),
                therapist);

        String groupId = "rec-" + UUID.randomUUID();
        String warning = null;
        List<Session> createdSessions = new ArrayList<>();
        List<SkippedRecurringOccurrence> skipped = new ArrayList<>();

        List<EvaluatedOccurrence> preSkipped = evaluated.stream()
                .filter(EvaluatedOccurrence::hasConflict)
                .collect(Collectors.toList());
        skipped.addAll(preSkipped.stream().map(this::toSkippedOccurrence).collect(Collectors.toList()));

        acquireBookingLocks(request.getTherapistId(), request.getRoomId());
        List<Session> activeInRange = loadActiveSessionsForRange(candidates, duration);

        for (EvaluatedOccurrence occurrence : free) {
            List<String> txReasons = findConflictReasons(
                    occurrence.occurrence().utcDate(),
                    duration,
                    request.getTherapistId(),
                    request.getRoomId(),
                    null,
                    activeInRange);

            if (!txReasons.isEmpty()) {
                skipped.add(toSkippedOccurrence(occurrence, txReasons));
                continue;
            }

            Session session = buildSession(request, client, therapist, service, room, occurrence, duration, groupId);
            Session saved = sessionRepository.save(session);
            activeInRange.add(saved);
            createdSessions.add(saved);

            Long orgId = TenantContext.getOrganisationId();
            if (orgId != null) {
                subscriptionFeatureService.consumeUsageOrThrow(
                        orgId,
                        String.valueOf(request.getTherapistId()),
                        SubscriptionFeatureService.FEATURE_SESSIONS_PER_MONTH,
                        1L,
                        "Recurring session creation");
            }
        }

        if (createdSessions.isEmpty()) {
            throw new ConflictException("All recurrence dates conflict with existing sessions", skipped);
        }

        String zoomWarning = createZoomMeetingsBestEffort(createdSessions, therapist, request.getZoomEnabled());
        if (zoomWarning != null) {
            warning = zoomWarning;
        }

        for (Session session : createdSessions) {
            recordAuditEvent(
                    currentUserService.requireCurrentUser(requester).getId(),
                    "session_created",
                    session.getId(),
                    session.getClient().getId(),
                    ipAddress,
                    true);
        }

        if (!createdSessions.isEmpty() && client.getAssignedTherapist() == null) {
            client.setAssignedTherapist(therapist);
            clientRepository.save(client);
        }

        sendSeriesConfirmation(createdSessions, groupId);

        List<SessionResponse> createdResponses = createdSessions.stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());

        return CreateRecurringSessionsResponse.builder()
                .groupId(groupId)
                .created(createdResponses)
                .createdCount(createdResponses.size())
                .skipped(skipped)
                .skippedCount(skipped.size())
                .warning(warning)
                .sessions(createdResponses)
                .requested(evaluated.size())
                .failed(skipped.size())
                .errors(skipped.stream()
                        .map(s -> s.getLocalDate() + ": " + String.join(", ", s.getReasons()))
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    @CacheEvict(value = "sessions", allEntries = true)
    public List<SessionResponse> updateFutureRecurringSessions(
            String groupId,
            UpdateRecurringFutureRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(groupId, "Group id is required");
        if (!groupId.startsWith("rec-")) {
            throw new BadRequestException("Invalid recurrence group id");
        }

        Session anchor = sessionRepository.findByIdWithRelations(request.getAnchorId())
                .orElseThrow(() -> new ResourceNotFoundException("Anchor session not found"));

        if (!groupId.equals(anchor.getRecurrenceGroupId())) {
            throw new BadRequestException("Session is not part of this series");
        }

        validateSessionEditAccess(anchor, requester);

        ZoneId seriesTimezone = timezoneService.getTherapistTimezone(anchor.getTherapist().getId())
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        Instant anchorOriginalDate = anchor.getSessionDate();
        List<Session> futureSessions = sessionRepository.findFutureByRecurrenceGroupId(
                groupId, anchorOriginalDate, CANCELLABLE_STATUSES);

        if (futureSessions.isEmpty()) {
            throw new ResourceNotFoundException("No upcoming sessions found in this series");
        }

        ZonedDateTime anchorOriginalLocal = anchorOriginalDate.atZone(seriesTimezone);
        ZonedDateTime anchorNewLocal = request.getSessionDate().atZone(seriesTimezone);
        long dayDelta = ChronoUnit.DAYS.between(anchorOriginalLocal.toLocalDate(), anchorNewLocal.toLocalDate());
        LocalTime newTimeOfDay = anchorNewLocal.toLocalTime();

        int duration = anchor.getDuration() != null ? anchor.getDuration() : resolveDurationFromService(anchor.getService());
        validateBusinessHours(request.getSessionDate(), duration, seriesTimezone);

        User targetTherapist = request.getTherapistId() != null
                ? userRepository.findById(request.getTherapistId())
                        .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"))
                : anchor.getTherapist();
        String targetMode = StringUtils.hasText(request.getSessionMode())
                ? systemOptionResolverService.requireOptionKey(
                        SystemOptionCategories.SESSION_MODE, request.getSessionMode())
                : anchor.getSessionType();
        Long targetRoomId;
        Room targetRoom = null;
        if (SystemOptionKeyMatcher.matchesAny(targetMode, "online", "virtual")) {
            // Online/virtual series do not keep a room assignment.
            targetRoomId = null;
        } else {
            targetRoomId = request.getRoomId() != null
                    ? request.getRoomId()
                    : anchor.getRoom() != null ? anchor.getRoom().getId() : null;
            if (targetRoomId == null) {
                throw new BadRequestException("Room is required for in-person sessions");
            }
            targetRoom = roomRepository.findById(targetRoomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            if (!Boolean.TRUE.equals(targetRoom.getIsActive())) {
                throw new BadRequestException("Selected room is inactive");
            }
            if (targetRoom.getRoomType() != RoomType.PHYSICAL) {
                throw new BadRequestException("In-person sessions require a physical room");
            }
        }
        com.smart.therapy.flow.billing.entity.Service targetService = request.getServiceId() != null
                ? serviceRepository.findById(request.getServiceId()).orElse(null)
                : anchor.getService();

        List<Session> updatedSessions = new ArrayList<>();
        for (Session session : futureSessions) {
            ZonedDateTime originalLocal = session.getSessionDate().atZone(seriesTimezone);
            ZonedDateTime newLocal = originalLocal.toLocalDate()
                    .plusDays(dayDelta)
                    .atTime(newTimeOfDay)
                    .atZone(seriesTimezone);
            Instant newUtc = newLocal.toInstant();

            if (!Boolean.TRUE.equals(request.getIgnoreConflicts())) {
                List<Session> externalSessions = loadActiveSessionsForInstant(newUtc, duration).stream()
                        .filter(s -> s.getRecurrenceGroupId() == null || !groupId.equals(s.getRecurrenceGroupId()))
                        .collect(Collectors.toList());
                List<String> reasons = findConflictReasons(
                        newUtc, duration, targetTherapist.getId(), targetRoomId, null, externalSessions);
                if (!reasons.isEmpty()) {
                    throw new ConflictException("Scheduling conflict detected for series update", reasons);
                }
            }

            session.setSessionDate(newUtc);
            if (request.getTherapistId() != null) {
                session.setTherapist(targetTherapist);
            }
            if (request.getServiceId() != null) {
                session.setService(targetService);
            }
            // Always apply mode-driven room rule for this series scope
            session.setRoom(targetRoom);
            if (request.getNotes() != null) {
                session.setNotes(request.getNotes());
            }
            if (request.getSessionType() != null) {
                session.setClinicalSessionType(resolveClinicalSessionType(request.getSessionType(), session.getService()));
            }
            if (StringUtils.hasText(request.getSessionMode())) {
                session.setSessionType(targetMode);
            }

            updatedSessions.add(sessionRepository.save(session));
        }

        for (Session session : updatedSessions) {
            updateZoomBestEffort(session, request.getZoomEnabled());
            recordAuditEvent(
                    currentUserService.requireCurrentUser(requester).getId(),
                    "recurring_series_updated",
                    session.getId(),
                    session.getClient().getId(),
                    ipAddress,
                    true);
        }

        return updatedSessions.stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "sessions", allEntries = true)
    public CancelRecurringSeriesResponse cancelRecurringSeries(
            String groupId,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(groupId, "Group id is required");
        if (!groupId.startsWith("rec-")) {
            throw new BadRequestException("Invalid recurrence group id");
        }

        List<Session> seriesSessions = sessionRepository.findByRecurrenceGroupId(groupId);
        if (seriesSessions.isEmpty()) {
            throw new ResourceNotFoundException("Recurring series not found");
        }

        validateSessionEditAccess(seriesSessions.get(0), requester);

        Instant now = Instant.now();
        List<Session> toCancel = seriesSessions.stream()
                .filter(s -> s.getStatus() != null && CANCELLABLE_STATUSES.contains(s.getStatus()))
                .filter(s -> s.getSessionDate().isAfter(now) || s.getSessionDate().equals(now))
                .collect(Collectors.toList());

        boolean containsBilledSession = toCancel.stream().anyMatch(s -> s.getBilling() != null);
        if (containsBilledSession) {
            throw new BadRequestException("A recurring series containing a billed session cannot be cancelled");
        }

        for (Session session : toCancel) {
            session.setStatus("cancelled");
            sessionRepository.save(session);

            if (notificationService != null) {
                try {
                    notificationService.processEvent(
                            NotificationEventCatalog.SESSION_CANCELLED,
                            buildSessionEventData(session));
                } catch (Exception e) {
                    log.error("Failed to trigger session_cancelled notification for {}", session.getId(), e);
                }
            }

            recordAuditEvent(
                    currentUserService.requireCurrentUser(requester).getId(),
                    "recurring_series_cancelled",
                    session.getId(),
                    session.getClient().getId(),
                    ipAddress,
                    true);
        }

        return CancelRecurringSeriesResponse.builder()
                .groupId(groupId)
                .cancelledCount(toCancel.size())
                .build();
    }

    private List<EvaluatedOccurrence> evaluateConflicts(
            List<ExpandedOccurrence> candidates,
            RecurrenceRuleRequest request,
            int duration,
            Long excludeSessionId) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Instant rangeStart = candidates.get(0).utcDate();
        Instant rangeEnd = candidates.get(candidates.size() - 1).utcDate()
                .plus(duration, ChronoUnit.MINUTES);
        List<Session> activeSessions = sessionRepository.findActiveSessionsInRange(
                rangeStart, rangeEnd, ACTIVE_CONFLICT_STATUSES);

        return candidates.stream()
                .map(candidate -> {
                    List<String> reasons = findConflictReasons(
                            candidate.utcDate(),
                            duration,
                            request.getTherapistId(),
                            request.getRoomId(),
                            excludeSessionId,
                            activeSessions);
                    return new EvaluatedOccurrence(candidate, !reasons.isEmpty(), reasons);
                })
                .collect(Collectors.toList());
    }

    private List<String> findConflictReasons(
            Instant sessionDate,
            int duration,
            Long therapistId,
            Long roomId,
            Long excludeSessionId,
            List<Session> activeSessions) {
        Instant sessionEnd = sessionDate.plus(duration, ChronoUnit.MINUTES);
        List<String> reasons = new ArrayList<>();

        boolean therapistBusy = activeSessions.stream()
                .filter(s -> excludeSessionId == null || !excludeSessionId.equals(s.getId()))
                .filter(s -> therapistId != null && s.getTherapist() != null
                        && therapistId.equals(s.getTherapist().getId()))
                .anyMatch(s -> overlaps(sessionDate, sessionEnd, s));

        if (therapistBusy) {
            reasons.add("Therapist is busy");
        }

        if (roomId != null) {
            boolean roomOccupied = activeSessions.stream()
                    .filter(s -> excludeSessionId == null || !excludeSessionId.equals(s.getId()))
                    .filter(s -> s.getRoom() != null && roomId.equals(s.getRoom().getId()))
                    .anyMatch(s -> overlaps(sessionDate, sessionEnd, s));
            if (roomOccupied) {
                reasons.add("Room is occupied");
            }
        }

        return reasons;
    }

    private boolean overlaps(Instant newStart, Instant newEnd, Session existing) {
        Instant existingStart = existing.getSessionDate();
        int existingDuration = existing.getDuration() != null ? existing.getDuration() : 60;
        Instant existingEnd = existingStart.plus(existingDuration, ChronoUnit.MINUTES);
        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }

    private List<Session> loadActiveSessionsForRange(List<ExpandedOccurrence> candidates, int duration) {
        Instant rangeStart = candidates.get(0).utcDate();
        Instant rangeEnd = candidates.get(candidates.size() - 1).utcDate()
                .plus(duration, ChronoUnit.MINUTES);
        return new ArrayList<>(sessionRepository.findActiveSessionsInRange(
                rangeStart, rangeEnd, ACTIVE_CONFLICT_STATUSES));
    }

    private List<Session> loadActiveSessionsForInstant(Instant sessionDate, int duration) {
        Instant rangeStart = sessionDate.minus(8, ChronoUnit.HOURS);
        Instant rangeEnd = sessionDate.plus(duration + 480, ChronoUnit.MINUTES);
        return sessionRepository.findActiveSessionsInRange(rangeStart, rangeEnd, ACTIVE_CONFLICT_STATUSES);
    }

    private void acquireBookingLocks(Long therapistId, Long roomId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?)")
                .setParameter(1, THERAPIST_LOCK_BASE + therapistId)
                .getSingleResult();
        if (roomId != null) {
            entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?)")
                    .setParameter(1, ROOM_LOCK_BASE + roomId)
                    .getSingleResult();
        }
    }

    private Session buildSession(
            RecurrenceRuleRequest request,
            Client client,
            User therapist,
            com.smart.therapy.flow.billing.entity.Service service,
            Room room,
            EvaluatedOccurrence occurrence,
            int duration,
            String groupId) {
        String clinicalType = resolveClinicalSessionType(request.getSessionType(), service);
        return Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .room(room)
                .sessionDate(occurrence.occurrence().utcDate())
                .duration(duration)
                .sessionType(systemOptionResolverService.requireOptionKey(
                        SystemOptionCategories.SESSION_MODE, request.getSessionMode()))
                .clinicalSessionType(clinicalType)
                .status("scheduled")
                .notes(request.getNotes())
                .recurrenceGroupId(groupId)
                .build();
    }

    private int resolveDuration(RecurrenceRuleRequest request) {
        com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        return service.getDuration() != null && service.getDuration() > 0 ? service.getDuration() : 60;
    }

    private int resolveDurationFromService(com.smart.therapy.flow.billing.entity.Service service) {
        if (service != null && service.getDuration() != null && service.getDuration() > 0) {
            return service.getDuration();
        }
        return 60;
    }

    private void validateBusinessHours(Instant sessionDate, int duration, ZoneId timezone) {
        ZonedDateTime localDateTime = sessionDate.atZone(timezone);
        int hour = localDateTime.getHour();
        int minute = localDateTime.getMinute();

        if (hour == BUSINESS_END_HOUR && minute == 0) {
            throw new BadRequestException(
                    "Sessions cannot be scheduled at or after 12:00 AM (midnight) in " + timezone.getId());
        }

        ZonedDateTime end = localDateTime.plusMinutes(duration);
        if (end.getHour() == BUSINESS_END_HOUR && end.getMinute() == 0
                || end.toLocalDate().isAfter(localDateTime.toLocalDate())) {
            throw new BadRequestException(
                    "Sessions cannot extend beyond 12:00 AM (midnight) in " + timezone.getId());
        }

        if (hour < BUSINESS_START_HOUR) {
            throw new BadRequestException("Sessions cannot be scheduled before 8:00 AM in " + timezone.getId());
        }
    }

    private ZoneId resolveRecurrenceTimezone(RecurrenceRuleRequest request) {
        if (StringUtils.hasText(request.getTimezone())) {
            try {
                return ZoneId.of(timezoneService.normalizeTimezoneId(request.getTimezone()));
            } catch (Exception ex) {
                throw new BadRequestException(
                        "Invalid timezone: " + request.getTimezone()
                                + ". Please use an IANA timezone ID (e.g., Asia/Karachi)");
            }
        }
        return timezoneService.getTherapistTimezone(request.getTherapistId())
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));
    }

    private void validateCreatePermission(RecurrenceRuleRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");
        Long requesterUserId = currentUserService.requireCurrentUser(requester).getId();

        if (!canViewAll) {
            if (canViewOwn && Objects.equals(request.getTherapistId(), requesterUserId)) {
                return;
            }
            if (canViewTeam) {
                List<Long> supervisedTherapistIds = getSupervisedTherapistIds(requesterUserId);
                if (!supervisedTherapistIds.contains(request.getTherapistId())) {
                    throw new ForbiddenException("You can only create sessions for therapists you supervise");
                }
                if (client.getAssignedTherapist() != null
                        && !supervisedTherapistIds.contains(client.getAssignedTherapist().getId())) {
                    throw new ForbiddenException(
                            "You can only create sessions for clients assigned to therapists you supervise");
                }
                return;
            }
            if (canViewOwn) {
                throw new ForbiddenException("You can only create sessions for yourself");
            }
            throw new ForbiddenException("You do not have permission to create sessions");
        }
    }

    private void validateSessionEditAccess(Session session, AuthPrincipal requester) {
        if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            return;
        }

        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");

        if (canViewOwn) {
            if (!Objects.equals(session.getTherapist().getId(), currentUserService.requireCurrentUser(requester).getId())) {
                throw new ForbiddenException("You can only edit your own sessions");
            }
        } else if (canViewTeam) {
            Long therapistId = session.getTherapist().getId();
            if (!getSupervisedTherapistIds(currentUserService.requireCurrentUser(requester).getId()).contains(therapistId)) {
                throw new ForbiddenException("You can only edit sessions of therapists you supervise");
            }
        } else {
            throw new ForbiddenException("You do not have permission to edit sessions");
        }
    }

    private List<Long> getSupervisedTherapistIds(Long supervisorId) {
        return supervisorAssignmentRepository.findBySupervisorId(supervisorId).stream()
                .map(SupervisorAssignment::getTherapist)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateOnlineSessionZoomRequirements(String sessionMode, Boolean zoomEnabled, User therapist) {
        if (!SystemOptionKeyMatcher.matchesAny(sessionMode, "online")) {
            return;
        }
        if (!Boolean.TRUE.equals(zoomEnabled)) {
            throw new BadRequestException(
                    "Online session requires Zoom. Please enable Zoom for the session.");
        }
        if (zoomService == null || !zoomService.isTherapistConfigured(therapist)) {
            throw new BadRequestException(
                    "Therapist Zoom is not configured. Please set Zoom credentials before scheduling online sessions.");
        }
    }

    private String createZoomMeetingsBestEffort(List<Session> sessions, User therapist, Boolean zoomEnabled) {
        if (!Boolean.TRUE.equals(zoomEnabled) || zoomService == null || !zoomService.isTherapistConfigured(therapist)) {
            if (Boolean.TRUE.equals(zoomEnabled)) {
                return "Zoom is not configured for this therapist. Sessions were booked without Zoom links.";
            }
            return null;
        }

        boolean anyFailure = false;
        for (Session session : sessions) {
            try {
                Long orgId = TenantContext.getOrganisationId();
                if (orgId != null) {
                    subscriptionFeatureService.consumeUsageOrThrow(
                            orgId,
                            String.valueOf(therapist.getId()),
                            SubscriptionFeatureService.FEATURE_ZOOM_SESSIONS_PER_MONTH,
                            1L,
                            "Recurring Zoom session creation");
                }

                ZoomMeetingResponse zoomMeeting = zoomService.createMeeting(
                        buildZoomMeetingRequest(session, therapist),
                        therapist);
                SessionIntegration zoomIntegration = SessionIntegration.builder()
                        .session(session)
                        .provider("zoom")
                        .meetingId(zoomMeeting.getMeetingId())
                        .joinUrl(zoomMeeting.getJoinUrl())
                        .password(zoomMeeting.getPassword())
                        .build();
                session.getIntegrations().add(zoomIntegration);
                sessionRepository.save(session);
            } catch (Exception e) {
                anyFailure = true;
                log.error("Zoom meeting creation failed for recurring session {}", session.getId(), e);
            }
        }

        return anyFailure ? "Some Zoom meetings could not be created. Sessions were still booked." : null;
    }

    private void updateZoomBestEffort(Session session, Boolean zoomEnabled) {
        if (zoomService == null) {
            return;
        }
        SessionIntegration zoomIntegration = getZoomIntegration(session);
        if (zoomIntegration == null) {
            return;
        }
        try {
            ZoomMeetingResponse zoomMeeting = zoomService.updateMeeting(
                    zoomIntegration.getMeetingId(),
                    buildZoomMeetingRequest(session, session.getTherapist()),
                    session.getTherapist());
            zoomIntegration.setJoinUrl(zoomMeeting.getJoinUrl());
            sessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to update Zoom meeting for session {}", session.getId(), e);
        }
    }

    private Map<String, Object> buildZoomMeetingRequest(Session session, User therapist) {
        Map<String, Object> request = new HashMap<>();
        request.put("topic", "Therapy Session with " + therapist.getFullName());
        request.put("startTime", session.getSessionDate());
        request.put("duration", session.getDuration());
        request.put("timezone", timezoneService.getTherapistTimezone(therapist.getId())
                .map(ZoneId::getId)
                .orElse("UTC"));
        request.put("settings", Map.of(
                "waiting_room", true,
                "video_host", true,
                "video_participant", true,
                "mute_upon_entry", true));
        return request;
    }

    private void sendSeriesConfirmation(List<Session> createdSessions, String groupId) {
        if (createdSessions.isEmpty()) {
            return;
        }

        Session first = createdSessions.get(0);
        try {
            if (notificationService != null) {
                Map<String, Object> eventData = buildSeriesEventData(createdSessions, groupId);
                notificationService.processEvent(NotificationEventCatalog.SESSION_SERIES_SCHEDULED, eventData);
            }
        } catch (Exception e) {
            log.error("Failed to send recurring series confirmation for group {}", groupId, e);
        }
    }

    private Map<String, Object> buildSeriesEventData(List<Session> sessions, String groupId) {
        Session first = sessions.get(0);
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("clientId", first.getClient().getId());
        NotificationPayloadFactory.putClientIdentity(data, first.getClient());
        data.put("therapistId", first.getTherapist().getId());
        data.put("therapistName", first.getTherapist().getFullName());
        data.put("sessionCount", sessions.size());
        data.put("firstSessionDate", first.getSessionDate());
        data.put("lastSessionDate", sessions.get(sessions.size() - 1).getSessionDate());
        
        if (first.getSessionDate() != null) {
            data.put("firstSessionDateFormatted",
                    timezoneService.formatSessionDateTimeForPractice(first.getSessionDate()));
        }
        if (sessions.get(sessions.size() - 1).getSessionDate() != null) {
            data.put("lastSessionDateFormatted",
                    timezoneService.formatSessionDateTimeForPractice(
                            sessions.get(sessions.size() - 1).getSessionDate()));
        }
        data.put("practiceTimezone", timezoneService.getPracticeTimezone().getId());

        data.put("serviceName", first.getService() != null ? first.getService().getServiceName() : null);
        return data;
    }

    private Map<String, Object> buildSessionEventData(Session session) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", session.getId());
        data.put("clientId", session.getClient().getId());
        NotificationPayloadFactory.putClientIdentity(data, session.getClient());
        data.put("therapistId", session.getTherapist().getId());
        data.put("therapistName", session.getTherapist().getFullName());
        data.put("sessionDate", session.getSessionDate());
        data.put("duration", session.getDuration());
        data.put("sessionType", session.getClinicalSessionType());
        data.put("sessionMode", session.getSessionType());
        data.put("status", session.getStatus());
        data.put("recurrenceGroupId", session.getRecurrenceGroupId());
        return data;
    }

    private void recordAuditEvent(Long actorId, String action, Long sessionId, Long clientId, String ipAddress,
            boolean hipaaRelevant) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_SESSION, sessionId, clientId, ipAddress,
                    hipaaRelevant);
        } catch (Exception e) {
            log.error("Failed to record audit event for session {}", sessionId, e);
        }
    }

    private RecurrencePreviewOccurrence toPreviewOccurrence(EvaluatedOccurrence evaluated) {
        ExpandedOccurrence occurrence = evaluated.occurrence();
        return RecurrencePreviewOccurrence.builder()
                .sessionDate(occurrence.utcDate())
                .localDate(recurrenceDateExpander.formatLocalDate(occurrence.localDate()))
                .sessionTime(occurrence.sessionTime())
                .hasConflict(evaluated.hasConflict())
                .reasons(evaluated.reasons())
                .build();
    }

    private SkippedRecurringOccurrence toSkippedOccurrence(EvaluatedOccurrence evaluated) {
        return toSkippedOccurrence(evaluated, evaluated.reasons());
    }

    private SkippedRecurringOccurrence toSkippedOccurrence(EvaluatedOccurrence evaluated, List<String> reasons) {
        ExpandedOccurrence occurrence = evaluated.occurrence();
        return SkippedRecurringOccurrence.builder()
                .sessionDate(occurrence.utcDate())
                .localDate(recurrenceDateExpander.formatLocalDate(occurrence.localDate()))
                .sessionTime(occurrence.sessionTime())
                .reasons(reasons)
                .build();
    }

    private List<SkippedRecurringOccurrence> buildAllSkippedResponse(List<EvaluatedOccurrence> evaluated) {
        return evaluated.stream().map(this::toSkippedOccurrence).collect(Collectors.toList());
    }

    private SessionIntegration getZoomIntegration(Session session) {
        if (session.getIntegrations() == null) {
            return null;
        }
        return session.getIntegrations().stream()
                .filter(i -> "zoom".equalsIgnoreCase(i.getProvider()))
                .findFirst()
                .orElse(null);
    }

    private String resolveClinicalSessionType(String requestedSessionType,
            com.smart.therapy.flow.billing.entity.Service service) {
        if (StringUtils.hasText(requestedSessionType)) {
            return systemOptionResolverService.parseOptionKey(
                    SystemOptionCategories.SESSION_TYPE,
                    requestedSessionType.trim(),
                    requestedSessionType.trim());
        }
        if (service == null) {
            return systemOptionResolverService.resolveDefaultOptionKey(SystemOptionCategories.SESSION_TYPE) != null
                    ? systemOptionResolverService.resolveDefaultOptionKey(SystemOptionCategories.SESSION_TYPE)
                    : "General";
        }
        String fallback = service.getCategory() != null ? service.getCategory() : service.getServiceName();
        return StringUtils.hasText(fallback) ? fallback : "General";
    }

    private SessionResponse toSessionResponse(Session session) {
        SessionIntegration zoomIntegration = getZoomIntegration(session);
        return SessionResponse.builder()
                .id(session.getId())
                .clientId(session.getClient().getId())
                .clientName(session.getClient().getFullName())
                .therapistId(session.getTherapist().getId())
                .therapistName(session.getTherapist().getFullName())
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .sessionType(session.getClinicalSessionType())
                .sessionMode(session.getSessionType())
                .status(session.getStatus())
                .serviceId(session.getService() != null ? session.getService().getId() : null)
                .serviceName(session.getService() != null ? session.getService().getServiceName() : null)
                .roomId(session.getRoom() != null ? session.getRoom().getId() : null)
                .roomName(session.getRoom() != null ? session.getRoom().getRoomName() : null)
                .notes(session.getNotes())
                .zoomEnabled(zoomIntegration != null)
                .zoomMeetingId(zoomIntegration != null ? zoomIntegration.getMeetingId() : null)
                .zoomJoinUrl(zoomIntegration != null ? zoomIntegration.getJoinUrl() : null)
                .zoomPassword(zoomIntegration != null ? zoomIntegration.getPassword() : null)
                .recurrenceGroupId(session.getRecurrenceGroupId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private record EvaluatedOccurrence(ExpandedOccurrence occurrence, boolean hasConflict, List<String> reasons) {}
}
