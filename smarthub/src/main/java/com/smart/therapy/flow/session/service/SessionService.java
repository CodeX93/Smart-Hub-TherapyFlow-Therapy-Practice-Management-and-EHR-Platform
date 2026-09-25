package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.enums.SessionHistoryScope;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientMrnService;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.session.dto.*;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.repository.SessionIntegrationRepository;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.util.CalendarDateBounds;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.user.dto.BookingCountResponse;
import com.smart.therapy.flow.user.dto.SessionStatsResponse;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionService {

    private static final int BUSINESS_START_HOUR = 8;
    private static final int BUSINESS_END_HOUR = 0; // Midnight (00:00)
    private static final String STATIC_SESSION_TEMPLATE_CSV_PATH = "templates/session_upload_template.csv";
    private static final String STATIC_SESSION_TEMPLATE_XLSX_PATH = "templates/session_upload_template.xlsx";
    /**
     * CSV columns for session bulk upload (aligned with single-session booking / CreateSessionRequest).
     * Use therapist email, service code, and room number rather than internal numeric IDs where possible.
     */
    private static final List<String> BULK_UPLOAD_HEADERS = List.of(
            "clientMrn",
            "therapistUsername",
            "sessionDate",
            "sessionTime",
            "sessionMode",
            "sessionType",
            "serviceCode",
            "roomNumber",
            "notes"
    );
    private static final List<String> BULK_UPLOAD_SAMPLE_ROW = List.of(
            "CL-2026-0001",
            "therapist@example.com",
            "2026-08-15",
            "10:00",
            "in_person",
            "assessment",
            "SRV-001",
            "R101",
            "Initial session"
    );

    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final ClientMrnService clientMrnService;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;
    private final TimezoneService timezoneService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final CaseloadScopeService caseloadScopeService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final SystemOptionResolverService systemOptionResolverService;
    private final ClientSearchHelper clientSearchHelper;
    private final com.smart.therapy.flow.client.util.ClientServiceEligibilityMessages clientServiceEligibilityMessages;
    private final com.smart.therapy.flow.session.repository.SessionTranscriptRepository sessionTranscriptRepository;
    private final SessionIntegrationRepository sessionIntegrationRepository;
    private final com.smart.therapy.flow.user.service.TherapistAvailabilityService therapistAvailabilityService;
    private final SessionOutcomeTransactionService sessionOutcomeTransactionService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private com.smart.therapy.flow.common.service.EmailService emailService;

    @Autowired(required = false)
    private com.smart.therapy.flow.notification.service.NotificationService notificationService;

    @Autowired(required = false)
    private ZoomService zoomService;

    @Autowired(required = false)
    private com.smart.therapy.flow.client.service.ClientContactService clientContactService;

    @Autowired(required = false)
    private com.smart.therapy.flow.client.service.ClientPortalSettingsService clientPortalSettingsService;

    @Autowired(required = false)
    private com.smart.therapy.flow.billing.service.BillingService billingService;

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getSessions(
            int page,
            int pageSize,
            Instant startDate,
            Instant endDate,
            Long therapistId,
            Long clientId,
            String clientSearch,
            String status,
            String sessionType,
            Long serviceId,
            String serviceCode,
            Long roomId,
            Boolean mySessionsOnly,
            Boolean includeHiddenServices,
            AuthPrincipal requester) {
        return getSessions(page, pageSize, startDate, endDate, therapistId, clientId, clientSearch, status,
                sessionType, serviceId, serviceCode, roomId, mySessionsOnly, includeHiddenServices, requester, null);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getSessions(
            int page,
            int pageSize,
            Instant startDate,
            Instant endDate,
            Long therapistId,
            Long clientId,
            String clientSearch,
            String status,
            String sessionType,
            Long serviceId,
            String serviceCode,
            Long roomId,
            Boolean mySessionsOnly,
            Boolean includeHiddenServices,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "sessionDate"));

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, therapistId, clientId, clientSearch, status, sessionType, serviceId, serviceCode,
                roomId, mySessionsOnly, includeHiddenServices,
                requester);

        Page<Session> results = sessionRepository.findAll(specification, pageable);

        List<SessionSummaryResponse> payload = toSessionSummaryResponses(results.getContent());

        if (clientId != null) {
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "sessions_viewed",
                    null, clientId, ipAddress, true);
        }

        return PaginatedResponse.of(payload, results.getTotalElements(), page, pageSize);
    }

    /**
     * Fast calendar list: projects only fields needed for month/week/day chips,
     * decrypts display names only, and skips Zoom passwords + full Client PHI hydration.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<SessionCalendarItemResponse> getSessionsForCalendar(
            int page,
            int pageSize,
            Instant startDate,
            Instant endDate,
            Long therapistId,
            Long clientId,
            String clientSearch,
            String status,
            String sessionType,
            Long serviceId,
            String serviceCode,
            Long roomId,
            Boolean mySessionsOnly,
            Boolean includeHiddenServices,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        // ClientHub parity: chronological ASC within the day; snap Instant filters to practice-local days.
        ZoneId calendarZone = resolveCalendarFilterZoneId();
        Instant normalizedStart = CalendarDateBounds.startOfLocalDay(startDate, calendarZone);
        Instant normalizedEnd = CalendarDateBounds.endOfLocalDayInclusive(endDate, calendarZone);

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(Sort.Direction.ASC, "sessionDate").and(Sort.by(Sort.Direction.ASC, "id")));
        Specification<Session> specification = buildSessionSpecification(
                normalizedStart, normalizedEnd, therapistId, clientId, clientSearch, status, sessionType, serviceId,
                serviceCode, roomId, mySessionsOnly, includeHiddenServices, requester);

        long total = sessionRepository.count(specification);
        List<Long> orderedIds = findSessionIdsForCalendar(specification, pageable);

        if (orderedIds.isEmpty()) {
            return PaginatedResponse.of(List.of(), total, page, pageSize);
        }

        Map<Long, SessionCalendarItemResponse> byId = sessionRepository.findCalendarItemsByIds(orderedIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(SessionCalendarItemResponse::getId, item -> item, (a, b) -> a));

        Set<Long> withTranscript = Set.copyOf(sessionTranscriptRepository.findSessionIdsWithTranscripts(orderedIds));

        Map<Long, String> zoomJoinBySessionId = new HashMap<>();
        for (Object[] row : sessionIntegrationRepository.findZoomJoinBySessionIds(orderedIds)) {
            if (row == null || row.length < 3 || row[0] == null) {
                continue;
            }
            Long sessionId = (Long) row[0];
            String joinUrl = row[1] != null ? row[1].toString() : null;
            if (StringUtils.hasText(joinUrl)) {
                zoomJoinBySessionId.putIfAbsent(sessionId, joinUrl);
            }
        }

        List<SessionCalendarItemResponse> payload = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            SessionCalendarItemResponse item = byId.get(id);
            if (item == null) {
                continue;
            }
            if (item.getSessionDate() != null) {
                item.setCalendarDate(item.getSessionDate().atZone(calendarZone).toLocalDate().toString());
            }
            item.setHasTranscript(withTranscript.contains(id));
            String joinUrl = zoomJoinBySessionId.get(id);
            if (StringUtils.hasText(joinUrl) && isOnlineOrVirtualMode(item.getSessionMode())) {
                item.setZoomEnabled(true);
                item.setZoomJoinUrl(joinUrl);
            }
            payload.add(item);
        }

        return PaginatedResponse.of(payload, total, page, pageSize);
    }

    /**
     * ID-only page query so encrypted session notes are not decrypted for calendar lists.
     */
    private List<Long> findSessionIdsForCalendar(Specification<Session> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Session> root = cq.from(Session.class);
        cq.select(root.get("id"));
        Predicate predicate = specification.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        cq.orderBy(cb.asc(root.get("sessionDate")), cb.asc(root.get("id")));
        return entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long sessionId, AuthPrincipal requester) {
        return getSession(sessionId, requester, null);
    }

    /**
     * Note: cached by sessionId only (ipAddress isn't part of the cache key), so the semantic
     * "session_viewed" audit below only fires on cache misses. This mirrors the existing
     * DocumentService.getDocument() precedent elsewhere in the codebase.
     */
    @Transactional(readOnly = true)
@Cacheable(value = "sessions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#sessionId)")
    public SessionResponse getSession(Long sessionId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        validateSessionAccess(session, requester);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "session_viewed",
                sessionId, session.getClient().getId(), ipAddress, true);
        return toSessionResponse(session);
    }

    @Transactional
    @CacheEvict(value = "sessions", allEntries = true)
    public SessionResponse createSession(CreateSessionRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        if (StringUtils.hasText(request.getStatus())
                && !SystemOptionKeyMatcher.matchesAny(request.getStatus(), "scheduled")) {
            throw new BadRequestException("New sessions must be created with scheduled status");
        }
        String requestedSessionMode = resolveSessionModeKey(request.getSessionMode());

        // Get therapist's timezone for business hours validation
        ZoneId therapistTz = timezoneService.getTherapistTimezone(request.getTherapistId())
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));

        validateNotInPast(request.getSessionDate());

        // Validate business hours in therapist's timezone
        validateBusinessHours(request.getSessionDate(), request.getDuration(), therapistTz);

        // Get service duration if serviceId provided
        if (request.getServiceId() != null) {
            com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
            if (request.getDuration() == null) {
                request.setDuration(service.getDuration());
            }
        }
        if (request.getDuration() == null || request.getDuration() <= 0) {
            throw new BadRequestException("Session duration is required (select a service with duration)");
        }

        validateRoomSelectionForSession(requestedSessionMode, request.getRoomId());

        // Therapist schedule must include this mode at this clock time (blocks bulk forced online on in-person-only shifts)
        therapistAvailabilityService.assertTherapistAvailableForBooking(
                request.getTherapistId(),
                request.getSessionDate(),
                request.getDuration(),
                requestedSessionMode);

        // Check for conflicts (therapist / room / client time overlap)
        if (!Boolean.TRUE.equals(request.getIgnoreConflicts())) {
            ConflictCheckResponse conflictCheck = checkConflicts(
                    request.getTherapistId(),
                    request.getRoomId(),
                    request.getClientId(),
                    request.getSessionDate(),
                    request.getDuration());

            if (Boolean.TRUE.equals(conflictCheck.getHasConflicts())) {
                throw new ConflictException(buildConflictMessage(conflictCheck), conflictCheck);
            }
        }

        // Load entities
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!client.canReceiveServices()) {
            throw new BadRequestException(clientServiceEligibilityMessages.schedulingBlocked(client));
        }

        User therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        // Assigned therapist lock: sessions must use the client's assigned therapist when set.
        if (client.getAssignedTherapist() != null
                && !Objects.equals(client.getAssignedTherapist().getId(), request.getTherapistId())) {
            User assigned = client.getAssignedTherapist();
            String mrn = clientMrnService.displayMrn(client);
            String label = formatTherapistLabel(assigned);
            throw new BadRequestException(
                    "Therapist does not match the assigned therapist for client MRN "
                            + (mrn != null ? mrn : client.getId())
                            + ". Assigned therapist is " + label
                            + ". Use that therapist (or leave therapist blank in bulk upload to auto-fill).");
        }

        // PBAC: Validate creation scope via caseload
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.ALL) {
            // No restrictions.
        } else if (resolved.scope() == CaseloadScope.NONE) {
            throw new ForbiddenException("You do not have permission to create sessions");
        } else if (!resolved.includesTherapist(request.getTherapistId())) {
            throw new ForbiddenException("You can only create sessions for therapists in your caseload");
        }
        if (resolved.scope() != CaseloadScope.ALL
                && client.getAssignedTherapist() != null
                && !resolved.includesTherapist(client.getAssignedTherapist().getId())) {
            throw new ForbiddenException(
                    "You can only create sessions for clients assigned to therapists in your caseload");
        }

        validateOnlineSessionZoomRequirements(requestedSessionMode, request.getZoomEnabled(), therapist);

        com.smart.therapy.flow.billing.entity.Service service = request.getServiceId() != null
                ? serviceRepository.findById(request.getServiceId()).orElse(null)
                : null;

        Room room = request.getRoomId() != null
                ? roomRepository.findById(request.getRoomId()).orElse(null)
                : null;

        // Plan limit: SESSIONS_PER_MONTH (atomic consumption)
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            String therapistTargetKey = request.getTherapistId() != null ? String.valueOf(request.getTherapistId()) : null;
            subscriptionFeatureService.consumeUsageOrThrow(
                    orgId,
                    therapistTargetKey,
                    SubscriptionFeatureService.FEATURE_SESSIONS_PER_MONTH,
                    1L,
                    "Session creation");
        }

        // Create session
        Session session = Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .room(room)
                .sessionDate(request.getSessionDate())
                .duration(request.getDuration())
                .sessionType(requestedSessionMode)
                .clinicalSessionType(resolveClinicalSessionType(request.getSessionType(), service))
                .status("scheduled")
                .notes(request.getNotes())
                .build();

        // Create Zoom meeting if enabled via SessionIntegration.
        // For online sessions Zoom is mandatory and failures must block creation.
        if (Boolean.TRUE.equals(request.getZoomEnabled())) {
            boolean canUseZoom = zoomService != null && zoomService.isTherapistConfigured(therapist);
            if (!canUseZoom) {
                String message = "Therapist Zoom integration is not configured. Please set Zoom credentials and enable Zoom before scheduling online sessions.";
                if (SystemOptionKeyMatcher.matchesAny(requestedSessionMode, "online")) {
                    throw new BadRequestException(message);
                }
                log.warn("Zoom requested but unavailable for therapist {}. Proceeding without Zoom integration.",
                        therapist.getId());
            } else {
                try {
                    if (orgId != null) {
                        String therapistTargetKey = request.getTherapistId() != null
                                ? String.valueOf(request.getTherapistId())
                                : null;
                        subscriptionFeatureService.consumeUsageOrThrow(
                                orgId,
                                therapistTargetKey,
                                SubscriptionFeatureService.FEATURE_ZOOM_SESSIONS_PER_MONTH,
                                1L,
                                "Zoom session creation");
                    }

                    ZoomMeetingResponse zoomMeeting = zoomService.createMeeting(
                            buildZoomMeetingRequest(session, therapist),
                            therapist);

                    // Create SessionIntegration for Zoom
                    SessionIntegration zoomIntegration = SessionIntegration.builder()
                            .session(session)
                            .provider("zoom")
                            .meetingId(zoomMeeting.getMeetingId())
                            .joinUrl(zoomMeeting.getJoinUrl())
                            .password(zoomMeeting.getPassword())
                            .build();
                    session.getIntegrations().add(zoomIntegration);
                } catch (Exception e) {
                    if (SystemOptionKeyMatcher.matchesAny(requestedSessionMode, "online")) {
                        throw new BadRequestException(
                                "Unable to create Zoom meeting for online session. Please verify therapist Zoom credentials and try again.");
                    }
                    log.error("Zoom requested but meeting setup failed for therapist {}. Proceeding without Zoom.",
                            therapist.getId(), e);
                    // Continue without Zoom - don't fail session creation
                }
            }
        }

        Session saved = Objects.requireNonNull(sessionRepository.save(session), "Persisted session must not be null");
        Long savedId = requireSessionId(saved);
        refreshClientSessionScheduleDates(client);

        // First session assigns the client to this therapist when none is set.
        if (client.getAssignedTherapist() == null) {
            client.setAssignedTherapist(therapist);
            clientRepository.save(client);
        }

        // Send confirmation emails
        sendSessionConfirmationEmails(saved);

        // Trigger notification
        if (notificationService != null) {
            try {
                notificationService.processEvent(NotificationEventCatalog.SESSION_SCHEDULED, buildSessionEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger session_scheduled notification", e);
            }
        }

        SessionResponse response = toSessionResponse(saved);

        // Audit log
        String auditDetails = String.format("{\"mrn\":\"%s\", \"sessionDate\":\"%s\", \"sessionMode\":\"%s\", \"sessionType\":\"%s\"}",
                saved.getClient().getClientId(),
                saved.getSessionDate(),
                saved.getSessionType(),
                saved.getClinicalSessionType());
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "session_created", savedId, saved.getClient().getId(), ipAddress, true, auditDetails);

        return response;
    }


    @Transactional
    @CacheEvict(value = "sessions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#sessionId)")
    public SessionResponse updateSession(Long sessionId, UpdateSessionRequest request, AuthPrincipal requester,
String ipAddress) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        validateSessionAccess(session, requester);

        Instant oldDate = session.getSessionDate();
        boolean dateChanged = false;
        boolean scheduleChanged = false;
        boolean durationChanged = false;
        boolean therapistChanged = false;
        ZoneId validationTimezone = resolveSessionTimezoneForUpdate(request.getTimezone(), session.getTherapist().getId());

        // Update fields
        if (request.getSessionDate() != null && !request.getSessionDate().equals(session.getSessionDate())) {
            validateNotInPast(request.getSessionDate());
            validateBusinessHours(request.getSessionDate(),
                    request.getDuration() != null ? request.getDuration() : session.getDuration(), validationTimezone);
            session.setSessionDate(request.getSessionDate());
            dateChanged = true;
            scheduleChanged = true;
        }

        if (request.getDuration() != null) {
            if (!Objects.equals(request.getDuration(), session.getDuration())) {
                scheduleChanged = true;
                durationChanged = true;
            }
            session.setDuration(request.getDuration());
        }

        if (request.getSessionMode() != null) {
            String sessionModeKey = resolveSessionModeKey(request.getSessionMode());
            if (!Objects.equals(sessionModeKey, session.getSessionType())) {
                scheduleChanged = true;
            }
            session.setSessionType(sessionModeKey);
        }

        if (StringUtils.hasText(request.getStatus())) {
            String requestedStatus = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.SESSION_STATUS, request.getStatus());
            if (!SystemOptionKeyMatcher.matchesAny(requestedStatus, session.getStatus())) {
                throw new BadRequestException("Use the dedicated session status endpoint to change status");
            }
        }

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            session.setClient(client);
        }

        if (request.getTherapistId() != null) {
            User therapist = userRepository.findById(request.getTherapistId())
                    .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
            if (!Objects.equals(therapist.getId(), session.getTherapist() != null ? session.getTherapist().getId() : null)) {
                scheduleChanged = true;
                therapistChanged = true;
            }
            session.setTherapist(therapist);
        }

        if (request.getServiceId() != null) {
            com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(request.getServiceId())
                    .orElse(null);
            session.setService(service);
        }

        // Online/virtual sessions never keep/require a room. In-person keeps explicit room assignment.
        if (SystemOptionKeyMatcher.matchesAny(session.getSessionType(), "online", "virtual")) {
            if (session.getRoom() != null) {
                scheduleChanged = true;
            }
            session.setRoom(null);
        } else if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId()).orElse(null);
            if (!Objects.equals(room != null ? room.getId() : null, session.getRoom() != null ? session.getRoom().getId() : null)) {
                scheduleChanged = true;
            }
            session.setRoom(room);
        }

        if (request.getNotes() != null) {
            session.setNotes(request.getNotes());
        }

        if (StringUtils.hasText(request.getSessionType())) {
            session.setClinicalSessionType(resolveClinicalSessionType(request.getSessionType(), session.getService()));
        }

        // Zoom integration updates handled separately through SessionIntegration

        validateRoomSelectionForSession(
                session.getSessionType(),
                session.getRoom() != null ? session.getRoom().getId() : null);

        // In-person: strip Zoom. Online/virtual: create Zoom if missing, update if schedule changed.
        if (SystemOptionKeyMatcher.matchesAny(session.getSessionType(), "in-person", "in_person", "inperson")) {
            clearZoomIntegration(session);
        } else if (isOnlineOrVirtualMode(session.getSessionType())) {
            // Online always needs Zoom (default true if client omits zoomEnabled when switching mode)
            Boolean requestedZoom = request.getZoomEnabled();
            boolean wantZoom = requestedZoom == null || Boolean.TRUE.equals(requestedZoom);
            validateOnlineSessionZoomRequirements(session.getSessionType(), wantZoom, session.getTherapist());

            boolean zoomUpdateRequested = dateChanged
                    || durationChanged
                    || therapistChanged
                    || StringUtils.hasText(request.getTimezone());

            SessionIntegration existingZoom = getZoomIntegration(session);
            if (wantZoom && existingZoom == null) {
                ensureZoomMeetingForSession(session, request.getTimezone(), true);
            } else if (wantZoom && existingZoom != null && zoomUpdateRequested && zoomService != null) {
                try {
                    ZoomMeetingResponse zoomMeeting = zoomService.updateMeeting(
                            existingZoom.getMeetingId(),
                            buildZoomMeetingRequest(session, session.getTherapist(), request.getTimezone()),
                            session.getTherapist());
                    existingZoom.setJoinUrl(zoomMeeting.getJoinUrl());
                    if (StringUtils.hasText(zoomMeeting.getPassword())) {
                        existingZoom.setPassword(zoomMeeting.getPassword());
                    }
                } catch (Exception e) {
                    log.error("Failed to update Zoom meeting for session {}", sessionId, e);
                    throw new BadRequestException(
                            "Unable to update Zoom meeting for online session. Please verify therapist Zoom credentials and try again.");
                }
            }
        }

        if (scheduleChanged && session.getDuration() != null && session.getSessionDate() != null
                && session.getTherapist() != null) {
            therapistAvailabilityService.assertTherapistAvailableForBooking(
                    session.getTherapist().getId(),
                    session.getSessionDate(),
                    session.getDuration(),
                    session.getSessionType(), null, sessionId);
        }

        // Check for conflicts on updates
        if (!Boolean.TRUE.equals(request.getIgnoreConflicts()) && scheduleChanged) {
            ConflictCheckResponse conflictCheck = checkConflicts(
                    session.getTherapist().getId(),
                    session.getRoom() != null ? session.getRoom().getId() : null,
                    session.getClient() != null ? session.getClient().getId() : null,
                    session.getSessionDate(),
                    session.getDuration());

            if (Boolean.TRUE.equals(conflictCheck.getHasConflicts())) {
                // Exclude current session from conflicts
                List<ConflictInfo> therapistConflicts = conflictCheck.getTherapistConflicts().stream()
                        .filter(c -> !c.getSessionId().equals(sessionId))
                        .collect(Collectors.toList());

                List<ConflictInfo> roomConflicts = conflictCheck.getRoomConflicts().stream()
                        .filter(c -> !c.getSessionId().equals(sessionId))
                        .collect(Collectors.toList());

                List<ConflictInfo> clientConflicts = conflictCheck.getClientConflicts() == null
                        ? List.of()
                        : conflictCheck.getClientConflicts().stream()
                                .filter(c -> !c.getSessionId().equals(sessionId))
                                .collect(Collectors.toList());

                if (!therapistConflicts.isEmpty() || !roomConflicts.isEmpty() || !clientConflicts.isEmpty()) {
                    ConflictCheckResponse filteredConflicts = ConflictCheckResponse.builder()
                            .hasConflicts(true)
                            .therapistConflicts(therapistConflicts)
                            .roomConflicts(roomConflicts)
                            .clientConflicts(clientConflicts)
                            .build();
                    throw new ConflictException(buildConflictMessage(filteredConflicts), filteredConflicts);
                }
            }
        }

        Session updated = Objects.requireNonNull(sessionRepository.save(session), "Persisted session must not be null");
        Long updatedId = requireSessionId(updated);
        if (session.getClient() != null) {
            refreshClientSessionScheduleDates(session.getClient());
        }

        // Trigger rescheduled notification if date changed
        if (dateChanged && notificationService != null) {
            try {
                Map<String, Object> eventData = buildSessionEventData(updated);
                eventData.put("oldDate", oldDate);
                eventData.put("newDate", updated.getSessionDate());
                notificationService.processEvent(NotificationEventCatalog.SESSION_RESCHEDULED, eventData);
            } catch (Exception e) {
                log.error("Failed to trigger session_rescheduled notification", e);
            }
        }

        SessionResponse response = toSessionResponse(updated);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "session_updated", updatedId, updated.getClient().getId(), ipAddress, true);

        return response;
    }

    private SessionResponse finishBillableOutcomeStatusUpdate(
            Session updated,
            String statusValue,
            AuthPrincipal requester,
            String ipAddress) {
        if (updated.getClient() != null) {
            refreshClientSessionScheduleDates(updated.getClient());
        }
        if (notificationService != null && SystemOptionKeyMatcher.matchesAny(statusValue, "completed")) {
            try {
                notificationService.processEvent(
                        NotificationEventCatalog.SESSION_COMPLETED,
                        buildSessionEventData(updated));
            } catch (Exception e) {
                log.error("Failed to trigger session_completed notification for session {}", updated.getId(), e);
            }
        }

        Long updatedId = requireSessionId(updated);
        recordAuditEvent(
                currentUserService.requireCurrentUser(requester).getId(),
                "session_status_updated",
                updatedId,
                updated.getClient() != null ? updated.getClient().getId() : null,
                ipAddress,
                true);
        return toSessionResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "sessions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#sessionId)")
    public SessionResponse updateSessionStatus(Long sessionId, String status, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(requester, "Requester is required");

        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        validateSessionAccess(session, requester);

        // Validate status transition
        String statusValue = systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.SESSION_STATUS, status);
        String currentStatusStr = session.getStatus();
        if (currentStatusStr != null && !isValidStatusTransition(currentStatusStr, statusValue)) {

            String message = String.format("This session status cannot be changed from %s to %s. Please create a new session",
                    currentStatusStr, statusValue);
            throw new BadRequestException(message);
        }

        validateOutcomeStatusAfterScheduledTime(session, statusValue);

        if (shouldCreateBillingForStatus(statusValue)) {
            sessionOutcomeTransactionService.persistScheduledFallback(sessionId);
            try {
                sessionOutcomeTransactionService.applyOutcomeAndCreateBilling(
                                sessionId, statusValue, requester, ipAddress);
            } catch (Exception e) {
                log.error("Billing generation failed for session {} outcome {}; scheduled fallback retained",
                        sessionId, statusValue, e);
                String reason = e instanceof BadRequestException && StringUtils.hasText(e.getMessage())
                        ? e.getMessage()
                        : "Billing service error";
                throw new BadRequestException(
                        "Unable to mark session as " + statusValue
                                + " because billing could not be generated: " + reason
                                + ". Session status remains scheduled.");
            }
            // The outcome transaction has committed. Refresh the outer transaction's managed
            // entity so lazy relations remain usable and status/version reflect that commit.
            // Response failures must not be reported as a rolled-back billing operation.
            entityManager.refresh(session);
            return finishBillableOutcomeStatusUpdate(session, statusValue, requester, ipAddress);
        }

        if (SystemOptionKeyMatcher.matchesAny(statusValue, "cancelled") && session.getBilling() != null) {
            throw new BadRequestException("A session with an existing bill cannot be cancelled");
        }

        session.setStatus(statusValue);
        Session updated = Objects.requireNonNull(sessionRepository.save(session), "Persisted session must not be null");
        Long updatedId = requireSessionId(updated);

        // Status-specific business logic
        if (updated.getClient() != null) {
            refreshClientSessionScheduleDates(updated.getClient());
        }

        if (SystemOptionKeyMatcher.matchesAny(statusValue, "cancelled")) {
            // Cancel related resources
            // Note: Room booking is kept for audit purposes (no status field to update)
            // The room booking will remain but session status indicates cancellation

            // Zoom cancel is best-effort only. Online session cancel must never fail with 500
            // when Zoom is misconfigured, credentials expired, meeting already deleted, etc.
            SessionIntegration zoomIntegration = getZoomIntegration(updated);
            if (zoomIntegration != null) {
                try {
                    if (zoomService != null
                            && StringUtils.hasText(zoomIntegration.getMeetingId())
                            && updated.getTherapist() != null) {
                        boolean zoomCancelled = zoomService.cancelMeeting(
                                zoomIntegration.getMeetingId(), updated.getTherapist());
                        if (!zoomCancelled) {
                            log.warn(
                                    "Zoom meeting was not cancelled for session {} (meetingId={}). Session status still set to cancelled.",
                                    sessionId, zoomIntegration.getMeetingId());
                        }
                    } else {
                        log.warn(
                                "Skipping Zoom meeting cancel for session {} (zoomAvailable={}, meetingIdPresent={}, therapistPresent={})",
                                sessionId,
                                zoomService != null,
                                StringUtils.hasText(zoomIntegration.getMeetingId()),
                                updated.getTherapist() != null);
                    }
                } catch (Exception e) {
                    log.error(
                            "Zoom meeting cancel failed for session {}; continuing with session cancellation",
                            sessionId, e);
                }
                // Always clear join credentials so cancelled online sessions lose meeting access.
                zoomIntegration.setJoinUrl(null);
                zoomIntegration.setPassword(null);
            }

            // Update billing status if exists
            if (updated.getBilling() != null && billingService != null) {
                try {
                    com.smart.therapy.flow.billing.dto.ChangeBillingStatusRequest billingStatusRequest = 
                            new com.smart.therapy.flow.billing.dto.ChangeBillingStatusRequest();
                    billingStatusRequest.setBillingStatus("CANCELLED");
                    billingService.changeBillingStatus(updated.getBilling().getId(), billingStatusRequest, requester, ipAddress);
                } catch (Exception e) {
                    log.error("Failed to update billing status for cancelled session {}", sessionId, e);
                }
            }

            if (notificationService != null) {
                try {
                    notificationService.processEvent(NotificationEventCatalog.SESSION_CANCELLED, buildSessionEventData(updated));
                } catch (Exception e) {
                    log.error("Failed to trigger session_cancelled notification", e);
                }
            }

            sendSessionCancellationEmails(updated);
        } else if (SystemOptionKeyMatcher.matchesAny(statusValue, "scheduled")) {
            sendSessionConfirmationEmails(updated);
        }

        SessionResponse response = toSessionResponse(updated);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "session_status_updated", updatedId, updated.getClient().getId(), ipAddress,
                true);

        return response;
    }

    @Transactional
    @CacheEvict(value = "sessions", allEntries = true)
    public void deleteSession(Long sessionId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Session session = sessionRepository.findByIdIncludingDeleted(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (Boolean.TRUE.equals(session.getIsDeleted())) {
            throw new BadRequestException("Session is already deleted");
        }

        validateSessionAccess(session, requester);

        session.softDelete();
        sessionRepository.save(session);
        sessionRepository.flush();
        if (session.getClient() != null) {
            refreshClientSessionScheduleDates(session.getClient());
        }

        recordAuditEvent(
                currentUserService.requireCurrentUser(requester).getId(),
                "session_deleted",
                sessionId,
                session.getClient() != null ? session.getClient().getId() : null,
                ipAddress,
                true);
    }

    /**
     * Statuses that auto-create a session billing row.
     * Matches ClientHub: only {@code completed} and {@code no-show} / {@code no_show}.
     * Cancelled and rescheduling must not create a new bill.
     */
    private boolean shouldCreateBillingForStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return SystemOptionKeyMatcher.matchesAny(status, "completed", "no-show", "no_show");
    }

    /**
     * Block billable outcomes until strictly after the stored scheduled instant. Session wall-clock
     * input and display use the Admin Settings practice timezone; comparing instants keeps this gate
     * independent of JVM timezone and correct across timezone offsets and DST changes.
     */
    private void validateOutcomeStatusAfterScheduledTime(Session session, String statusValue) {
        if (!SystemOptionKeyMatcher.matchesAny(statusValue, "completed", "no-show", "no_show")) {
            return;
        }
        Instant sessionDate = session.getSessionDate();
        if (sessionDate == null) {
            throw new BadRequestException("Session date is required before marking the session as " + statusValue);
        }
        if (!Instant.now().isAfter(sessionDate)) {
            throw new BadRequestException(String.format(
                    "Cannot mark session as %s until after its scheduled time (%s)",
                    statusValue,
                    sessionDate));
        }
    }

    /**
     * Keep denormalized client schedule timestamps aligned with live session rows:
     * last = latest non-cancelled session at/before now; next = earliest upcoming non-cancelled booking.
     */
    private void refreshClientSessionScheduleDates(Client client) {
        if (client == null || client.getId() == null) {
            return;
        }
        try {
            Instant asOf = Instant.now();
            Instant lastHeld = sessionRepository.findLastHeldSessionDate(client.getId(), asOf);
            Instant nextAppt = sessionRepository.findNextAppointmentDate(client.getId(), asOf);
            client.setLastSessionDate(lastHeld);
            client.setNextAppointmentDate(nextAppt);
            clientRepository.save(client);
        } catch (Exception e) {
            log.warn("Failed to refresh last/next session dates for client {}: {}", client.getId(), e.getMessage());
        }
    }

    /**
     * Validate session status transition
     * Business rule: Only certain transitions are allowed
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            return true; // Allow if either is null (backward compatibility)
        }

        String current = currentStatus.toLowerCase();
        String next = newStatus.toLowerCase();

        // Standard transition policy for now:
        // - forward-only flow is allowed (for example scheduled -> completed)
        // - backward flow is blocked (for example confirmed -> scheduled)
        // - cancelled stays terminal
        if ("cancelled".equals(current)) {
            return false;
        }

        // Completed is terminal in this policy.
        if ("completed".equals(current)) {
            return false;
        }

        // Cancellation is allowed from any non-terminal status.
        if ("cancelled".equals(next)) {
            return true;
        }

        Integer currentOrder = getStatusWorkflowOrder(current);
        Integer nextOrder = getStatusWorkflowOrder(next);

        if (currentOrder != null && nextOrder != null) {
            return nextOrder >= currentOrder;
        }

        // Unknown/custom statuses: keep backward compatibility.
        return true;
    }

    private Integer getStatusWorkflowOrder(String status) {
        return switch (status) {
            case "scheduled", "rescheduling" -> 1;
            case "confirmed", "overdue" -> 2;
            case "in-progress" -> 3;
            case "no-show", "completed" -> 4;
            default -> null;
        };
    }

    private void validateRoomSelectionForSession(String sessionMode, Long roomId) {
        if (sessionMode == null) {
            return;
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionMode, "in-person") && roomId == null) {
            throw new BadRequestException("Room is required for in-person sessions");
        }
        if (roomId == null) {
            return;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (!Boolean.TRUE.equals(room.getIsActive())) {
            throw new BadRequestException("Selected room is inactive");
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionMode, "online")
                && room.getRoomType() != RoomType.VIRTUAL) {
            throw new BadRequestException("Online sessions require a virtual room");
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionMode, "in-person")
                && room.getRoomType() != RoomType.PHYSICAL) {
            throw new BadRequestException("In-person sessions require a physical room");
        }
    }

    @Transactional(readOnly = true)
    public ConflictCheckResponse checkConflicts(Long therapistId, Long roomId, Instant sessionDate, Integer duration) {
        return checkConflicts(therapistId, roomId, null, sessionDate, duration);
    }

    /**
     * Detects overlapping scheduled sessions for therapist, room, and/or client.
     * Used by single booking and bulk upload so re-uploading the same time slot is rejected as a conflict.
     */
    @Transactional(readOnly = true)
    public ConflictCheckResponse checkConflicts(Long therapistId, Long roomId, Long clientId,
            Instant sessionDate, Integer duration) {
        Objects.requireNonNull(sessionDate, "Session date is required");
        Objects.requireNonNull(duration, "Duration is required");

        Instant sessionEnd = sessionDate.plus(duration, ChronoUnit.MINUTES);
        List<ConflictInfo> therapistConflicts = new ArrayList<>();
        List<ConflictInfo> roomConflicts = new ArrayList<>();
        List<ConflictInfo> clientConflicts = new ArrayList<>();

        if (therapistId != null) {
            List<Session> overlappingTherapist = sessionRepository.findOverlappingTherapistSessions(
                    therapistId, sessionDate, sessionEnd);
            if (overlappingTherapist != null) {
                therapistConflicts = overlappingTherapist.stream()
                        .filter(this::isActiveSessionForScheduling)
                        .map(this::toConflictInfoSafe)
                        .collect(Collectors.toList());
            }
        }

        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            int schedulingCapacity = resolveRoomSchedulingCapacity(room);

            List<Session> overlappingSessions = sessionRepository.findOverlappingRoomSessions(
                    roomId, sessionDate, sessionEnd);

            if (overlappingSessions != null) {
                roomConflicts = overlappingSessions.stream()
                        .filter(this::isActiveSessionForScheduling)
                        .map(this::toConflictInfoSafe)
                        .collect(Collectors.toList());
            }

            if (roomConflicts.size() < schedulingCapacity) {
                roomConflicts = List.of();
            }
        }

        // Client already scheduled at this date/time (covers re-upload of the same bulk file)
        if (clientId != null) {
            List<Session> overlappingClient = sessionRepository.findOverlappingClientSessions(
                    clientId, sessionDate, sessionEnd);
            if (overlappingClient != null) {
                clientConflicts = overlappingClient.stream()
                        .filter(this::isActiveSessionForScheduling)
                        .map(this::toConflictInfoSafe)
                        .collect(Collectors.toList());
            }
        }

        boolean hasConflicts = !therapistConflicts.isEmpty()
                || !roomConflicts.isEmpty()
                || !clientConflicts.isEmpty();

        return ConflictCheckResponse.builder()
                .hasConflicts(hasConflicts)
                .therapistConflicts(therapistConflicts)
                .roomConflicts(roomConflicts)
                .clientConflicts(clientConflicts)
                .build();
    }

    /**
     * Build a short, human-readable reason describing why a scheduling conflict occurred.
     */
    private String buildConflictMessage(ConflictCheckResponse conflictCheck) {
        boolean hasTherapist = conflictCheck.getTherapistConflicts() != null
                && !conflictCheck.getTherapistConflicts().isEmpty();
        boolean hasRoom = conflictCheck.getRoomConflicts() != null
                && !conflictCheck.getRoomConflicts().isEmpty();
        boolean hasClient = conflictCheck.getClientConflicts() != null
                && !conflictCheck.getClientConflicts().isEmpty();

        List<String> parts = new ArrayList<>();
        if (hasTherapist) {
            ConflictInfo therapistConflict = conflictCheck.getTherapistConflicts().get(0);
            parts.add(String.format(
                    "therapist %s already has an overlapping session%s",
                    conflictName(therapistConflict.getTherapistName(), "the therapist"),
                    conflictSessionHint(therapistConflict)));
        }
        if (hasRoom) {
            ConflictInfo roomConflict = conflictCheck.getRoomConflicts().get(0);
            parts.add(String.format(
                    "room %s is already booked for an overlapping session%s",
                    conflictName(roomConflict.getRoomName(), "the selected room"),
                    conflictSessionHint(roomConflict)));
        }
        if (hasClient) {
            ConflictInfo clientConflict = conflictCheck.getClientConflicts().get(0);
            parts.add(String.format(
                    "client already has a session scheduled at this date/time%s",
                    conflictSessionHint(clientConflict)));
        }

        if (parts.isEmpty()) {
            return "Scheduling conflict: a session is already scheduled at this date and time";
        }
        return "Scheduling conflict: " + String.join("; ", parts) + ".";
    }

    private String conflictSessionHint(ConflictInfo info) {
        if (info == null || info.getSessionId() == null) {
            return "";
        }
        return " (existing session id " + info.getSessionId() + ")";
    }

    private String conflictName(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private ConflictInfo toConflictInfoSafe(Session session) {
        try {
            return toConflictInfo(session);
        } catch (Exception e) {
            log.debug("Could not fully load conflict session details for id={}: {}",
                    session != null ? session.getId() : null, e.getMessage());
            return ConflictInfo.builder()
                    .sessionId(session != null ? session.getId() : null)
                    .sessionDate(session != null ? session.getSessionDate() : null)
                    .duration(session != null ? session.getDuration() : null)
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(LocalDate date, Long therapistId, Long roomId) {
        Objects.requireNonNull(date, "Date is required");

        // Use therapist's timezone (required)
        ZoneId timezone = therapistId != null
                ? timezoneService.getTherapistTimezone(therapistId)
                        .orElseThrow(() -> new BadRequestException(
                                "Therapist timezone is not configured. Please set timezone in therapist profile."))
                : ZoneId.of("UTC"); // Fallback to UTC only if therapistId is null

        Instant startOfDay = date.atStartOfDay(timezone).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(timezone).toInstant();

        List<Session> therapistSessions = therapistId != null
                ? sessionRepository.findByTherapistAndDateRange(therapistId, startOfDay, endOfDay)
                : List.of();
        List<Session> roomSessions = roomId != null
                ? sessionRepository.findByRoomAndDateRange(roomId, startOfDay, endOfDay)
                : List.of();

        List<TimeSlot> timeSlots = new ArrayList<>();

        // Generate time slots from 8 AM to 11:59 PM
        for (int hour = BUSINESS_START_HOUR; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                Instant slotTime = date.atTime(hour, minute).atZone(timezone).toInstant();
                Instant slotEnd = slotTime.plus(60, ChronoUnit.MINUTES);

                boolean therapistBusy = hasOverlap(therapistSessions, slotTime, slotEnd);
                boolean roomBusy = hasOverlap(roomSessions, slotTime, slotEnd);

                boolean available = !therapistBusy && !roomBusy;

                timeSlots.add(TimeSlot.builder()
                        .time(slotTime)
                        .datetime(slotTime)
                        .available(available)
                        .therapistBusy(therapistBusy)
                        .roomBusy(roomBusy)
                        .build());
            }
        }

        return AvailabilityResponse.builder()
                .date(date)
                .therapistId(therapistId)
                .roomId(roomId)
                .timeSlots(timeSlots)
                .build();
    }

    private boolean hasOverlap(List<Session> sessions, Instant slotStart, Instant slotEnd) {
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        for (Session session : sessions) {
            if (session.getStatus() != null && SystemOptionKeyMatcher.matchesAny(session.getStatus(), "cancelled")) {
                continue;
            }
            Instant sStart = session.getSessionDate();
            int sDuration = session.getDuration() != null ? session.getDuration() : 60;
            Instant sEnd = sStart.plus(sDuration, ChronoUnit.MINUTES);
            if (slotStart.isBefore(sEnd) && slotEnd.isAfter(sStart)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsByMonth(int year, int month, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        YearMonth yearMonth = YearMonth.of(year, month);
        ZoneId zone = resolveCalendarFilterZoneId();
        Instant startDate = CalendarDateBounds.startOfDay(yearMonth.atDay(1), zone);
        Instant endDate = CalendarDateBounds.endOfDayInclusive(yearMonth.atEndOfMonth(), zone);

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, null, null, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, calendarChronologicalSort());
        List<SessionSummaryResponse> result = toSessionSummaryResponses(sessions);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "session_calendar_month_viewed", null, null, null, true,
                "year=" + year + ",month=" + month + ",resultCount=" + result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsByDay(int year, int month, int day, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        LocalDate date = LocalDate.of(year, month, day);
        ZoneId zone = resolveCalendarFilterZoneId();
        Instant startDate = CalendarDateBounds.startOfDay(date, zone);
        Instant endDate = CalendarDateBounds.endOfDayInclusive(date, zone);

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, null, null, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, calendarChronologicalSort());
        List<SessionSummaryResponse> result = toSessionSummaryResponses(sessions);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "session_calendar_day_viewed", null, null, null, true,
                "year=" + year + ",month=" + month + ",day=" + day + ",resultCount=" + result.size());
        return result;
    }

    /**
     * Fast path for dashboard KPI cards: count today's scheduled/confirmed/in-progress sessions
     * without hydrating full session summary DTOs.
     */
    @Transactional(readOnly = true)
    public long countScheduledSessionsForDay(int year, int month, int day, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        LocalDate date = LocalDate.of(year, month, day);
        ZoneId zone = resolveCalendarFilterZoneId();
        Instant startDate = CalendarDateBounds.startOfDay(date, zone);
        Instant endDate = CalendarDateBounds.endOfDayInclusive(date, zone);

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, null, null, null, null, null, null, null, null, false, false, requester);
        specification = specification.and(sessionStatusIn(
                SessionStatus.SCHEDULED.getValue(),
                SessionStatus.CONFIRMED.getValue(),
                SessionStatus.IN_PROGRESS.getValue()));
        return sessionRepository.count(specification);
    }

    /**
     * Therapist/caseload-scoped variant of {@link #countScheduledSessionsForDay}.
     */
    @Transactional(readOnly = true)
    public long countScheduledSessionsForDayForAssignedTherapist(int year, int month, int day, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        Long therapistId = resolved.scope() == CaseloadScope.OWN ? resolved.currentUserId() : null;
        LocalDate date = LocalDate.of(year, month, day);
        ZoneId zone = resolveCalendarFilterZoneId();
        Instant startDate = CalendarDateBounds.startOfDay(date, zone);
        Instant endDate = CalendarDateBounds.endOfDayInclusive(date, zone);

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, therapistId, null, null, null, null, null, null, null, false, false, requester);
        specification = specification.and(sessionStatusIn(
                SessionStatus.SCHEDULED.getValue(),
                SessionStatus.CONFIRMED.getValue(),
                SessionStatus.IN_PROGRESS.getValue()));
        return sessionRepository.count(specification);
    }

    /**
     * Sessions for a calendar day scoped to the current therapist. Used by therapist dashboard cards.
     */
    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsByDayForAssignedTherapist(int year, int month, int day, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        // OWN keeps self filter; TEAM / TEAM_AND_OWN / NONE / ALL rely on applyCaseloadScope
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        Long therapistId = resolved.scope() == CaseloadScope.OWN ? resolved.currentUserId() : null;
        LocalDate date = LocalDate.of(year, month, day);
        ZoneId zone = resolveCalendarFilterZoneId();
        Instant startDate = CalendarDateBounds.startOfDay(date, zone);
        Instant endDate = CalendarDateBounds.endOfDayInclusive(date, zone);

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, therapistId, null, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, calendarChronologicalSort());
        List<SessionSummaryResponse> result = toSessionSummaryResponses(sessions);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "session_calendar_day_viewed", null, null, null, true,
                "year=" + year + ",month=" + month + ",day=" + day + ",assignedTherapist=true,resultCount="
                        + result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsByWeek(int year, int week, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        LocalDate weekStart = LocalDate.of(year, 1, 1)
                .with(java.time.temporal.WeekFields.ISO.weekOfYear(), week)
                .with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        ZoneId zone = resolveCalendarFilterZoneId();
        Instant startDate = CalendarDateBounds.startOfDay(weekStart, zone);
        Instant endDate = CalendarDateBounds.endOfDayInclusive(weekEnd, zone);

        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, null, null, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, calendarChronologicalSort());
        List<SessionSummaryResponse> result = toSessionSummaryResponses(sessions);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "session_calendar_week_viewed", null, null, null, true,
                "year=" + year + ",week=" + week + ",resultCount=" + result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getAllSessionHistory(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Session> specification = buildSessionSpecification(
                null, null, null, null, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "sessionDate"));
        List<SessionSummaryResponse> result = toSessionSummaryResponses(sessions);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "session_history_list_viewed", null, null, null, true,
                "resultCount=" + result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getClientSessionHistory(Long clientId, AuthPrincipal requester) {
        return getClientSessionHistory(clientId, requester, null);
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getClientSessionHistory(Long clientId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify client exists
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Long requesterUserId = currentUserService.requireCurrentUser(requester).getId();
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.NONE) {
            throw new ForbiddenException("Insufficient permissions");
        }
        if (resolved.scope() != CaseloadScope.ALL) {
            Long assignedTherapistId = client.getAssignedTherapist() != null
                    ? client.getAssignedTherapist().getId()
                    : null;
            if (!resolved.includesTherapist(assignedTherapistId)) {
                throw new ForbiddenException("Access denied to this client's session history");
            }
        }

        Specification<Session> specification = buildSessionSpecification(
                null, null, null, clientId, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "sessionDate"));
        recordAuditEvent(requesterUserId, "session_history_viewed", null, clientId, ipAddress, true);
        return toSessionSummaryResponses(sessions);
    }

    /**
     * Get session history for the currently authenticated client (portal "me").
     * Allowed only when requester has CLIENT identity and is the client.
     */
    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getCurrentClientSessionHistory(
            AuthPrincipal requester,
            com.smart.therapy.flow.client.portal.enums.SessionHistoryScope scope,
            String timezoneParam) {
        Objects.requireNonNull(requester, "Requester is required");
        Objects.requireNonNull(scope, "Scope is required");
        Long clientId = currentUserService.getCurrentClientId(requester);
        if (clientId == null) {
            throw new ForbiddenException("Not authenticated as a client");
        }
        resolveClientPortalHistoryZone(clientId, timezoneParam);

        Instant now = Instant.now();
        List<Session> sessions = sessionRepository.findByClientId(clientId).stream()
                .filter(session -> com.smart.therapy.flow.session.util.SessionHistoryClassifier.matchesScope(
                        session, scope, now))
                .sorted(com.smart.therapy.flow.session.util.SessionHistoryClassifier.comparator(scope))
                .collect(Collectors.toList());
        return toSessionSummaryResponses(sessions);
    }

    private ZoneId resolveClientPortalHistoryZone(Long clientId, String timezoneParam) {
        if (StringUtils.hasText(timezoneParam)) {
            try {
                return ZoneId.of(timezoneService.normalizeTimezoneId(timezoneParam.trim()));
            } catch (Exception ex) {
                throw new BadRequestException("Invalid timezone.");
            }
        }
        return timezoneService.getClientTimezone(clientId).orElse(ZoneId.of("UTC"));
    }

    @Transactional(readOnly = true)
    public SessionOverviewStatsResponse getSessionOverviewStats(
            Instant startDate,
            Instant endDate,
            Long therapistId,
            Long clientId,
            String timezone,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        ZoneId zone = resolveStatsZone(timezone);
        Specification<Session> specification = buildSessionSpecification(
                startDate, endDate, therapistId, clientId, null, null, null, null, null, null, false, false, requester);
        // Count queries — do not load the full session list (All Sessions can be thousands of rows).
        specification = specification.and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));

        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);

        String scope = therapistId != null ? "THERAPIST" : (clientId != null ? "CLIENT" : "ACCESSIBLE");
        Long resolvedTherapistId = therapistId;

        if (therapistId == null && clientId == null) {
            if (resolved.scope() == CaseloadScope.OWN) {
                resolvedTherapistId = resolved.currentUserId();
                scope = "THERAPIST";
            } else if (resolved.scope() == CaseloadScope.TEAM
                    || resolved.scope() == CaseloadScope.TEAM_AND_OWN) {
                scope = "TEAM";
            }
        }

        return buildSessionOverviewStatsByCount(
                specification,
                zone,
                scope,
                resolvedTherapistId,
                clientId,
                startDate,
                endDate);
    }

    @Transactional(readOnly = true)
    public SessionOverviewStatsResponse getCurrentClientSessionOverview(String timezone, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        Long clientId = currentUserService.getCurrentClientId(requester);
        if (clientId == null) {
            throw new ForbiddenException("Not authenticated as a client");
        }

        ZoneId zone = resolveStatsZone(timezone);
        List<Session> sessions = sessionRepository.findByClientId(clientId);
        return buildSessionOverviewStats(sessions, zone, "CLIENT_SELF", null, clientId, null, null);
    }

    @Transactional(readOnly = true)
    public SessionRoleDashboardResponse getRoleBasedSessionDashboard(
            String timezone,
            int recentLimit,
            int allLimit,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        ZoneId zone = resolveStatsZone(timezone);
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth currentMonth = YearMonth.from(today);

        Specification<Session> spec = buildSessionSpecification(
                null, null, null, null, null, null, null, null, null, null, false, false, requester);

        List<Session> scopedSessions = sessionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "sessionDate"));

        List<Session> todaySessionsRaw = scopedSessions.stream()
                .filter(s -> s.getSessionDate() != null && s.getSessionDate().atZone(zone).toLocalDate().equals(today))
                .collect(Collectors.toList());

        List<Session> weekSessionsRaw = scopedSessions.stream()
                .filter(s -> {
                    if (s.getSessionDate() == null) {
                        return false;
                    }
                    LocalDate d = s.getSessionDate().atZone(zone).toLocalDate();
                    return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
                })
                .collect(Collectors.toList());

        List<Session> monthSessionsRaw = scopedSessions.stream()
                .filter(s -> s.getSessionDate() != null
                        && YearMonth.from(s.getSessionDate().atZone(zone).toLocalDate()).equals(currentMonth))
                .collect(Collectors.toList());

        List<Session> upcomingSessionsRaw = scopedSessions.stream()
                .filter(s -> s.getSessionDate() != null && s.getSessionDate().isAfter(now))
                .filter(s -> s.getStatus() == null || (!isSessionStatusCancelled(s.getStatus())
                        && !isSessionStatusCompleted(s.getStatus())))
                .sorted(Comparator.comparing(Session::getSessionDate))
                .collect(Collectors.toList());

        List<Session> cancelledSessionsRaw = scopedSessions.stream()
                .filter(s -> s.getStatus() != null && isSessionStatusCancelled(s.getStatus()))
                .collect(Collectors.toList());

        List<Session> completedSessionsRaw = scopedSessions.stream()
                .filter(s -> s.getStatus() != null && isSessionStatusCompleted(s.getStatus()))
                .collect(Collectors.toList());

        int safeRecentLimit = Math.max(recentLimit, 1);
        int safeAllLimit = Math.max(allLimit, 1);

        SessionSummaryMappingContext mappingContext = prepareSessionSummaryContext(scopedSessions);

        List<SessionSummaryResponse> recentSessions = scopedSessions.stream()
                .limit(safeRecentLimit)
                .map(mappingContext::map)
                .collect(Collectors.toList());

        return SessionRoleDashboardResponse.builder()
                .totalSessions(scopedSessions.size())
                .todayCount(todaySessionsRaw.size())
                .weekCount(weekSessionsRaw.size())
                .monthCount(monthSessionsRaw.size())
                .upcomingCount(upcomingSessionsRaw.size())
                .cancelledCount(cancelledSessionsRaw.size())
                .completedCount(completedSessionsRaw.size())
                .recentCount(recentSessions.size())
                .todaySessions(todaySessionsRaw.stream().map(mappingContext::map).collect(Collectors.toList()))
                .weekSessions(weekSessionsRaw.stream().map(mappingContext::map).collect(Collectors.toList()))
                .monthSessions(monthSessionsRaw.stream().map(mappingContext::map).collect(Collectors.toList()))
                .allSessions(scopedSessions.stream().limit(safeAllLimit).map(mappingContext::map).collect(Collectors.toList()))
                .upcomingSessions(upcomingSessionsRaw.stream().map(mappingContext::map).collect(Collectors.toList()))
                .cancelledSessions(cancelledSessionsRaw.stream().map(mappingContext::map).collect(Collectors.toList()))
                .completedSessions(completedSessionsRaw.stream().map(mappingContext::map).collect(Collectors.toList()))
                .recentSessions(recentSessions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getRecentSessions(int limit, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        Specification<Session> specification = buildSessionSpecification(
                null, null, null, null, null, null, null, null, null, null, false, false, requester);

        List<Session> sessions = sessionRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "sessionDate"));
        return toSessionSummaryResponses(sessions.stream().limit(limit).collect(Collectors.toList()));
    }

    private ZoneId resolveStatsZone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            // ClientHub parity: Today/Week/Month buckets use practice TZ, not UTC/browser.
            return resolveCalendarFilterZoneId();
        }
        try {
            return ZoneId.of(timezoneService.normalizeTimezoneId(timezone.trim()));
        } catch (Exception ex) {
            throw new BadRequestException(
                    "Invalid timezone: " + timezone + ". Please use an IANA timezone ID (e.g., Asia/Karachi)");
        }
    }

    /**
     * Practice timezone for calendar DATE(session_date) parity with ClientHubAI.
     * Falls back to UTC when practice timezone is unset; server timezone is never used.
     */
    private ZoneId resolveCalendarFilterZoneId() {
        Optional<String> practiceTz = timezoneService.findPracticeTimezoneId();
        if (practiceTz.isPresent()) {
            try {
                return ZoneId.of(practiceTz.get());
            } catch (Exception ex) {
                log.debug("Invalid practice timezone {}, falling back to UTC: {}",
                        practiceTz.get(), ex.getMessage());
            }
        }
        return ZoneId.of("UTC");
    }

    private static Sort calendarChronologicalSort() {
        return Sort.by(Sort.Direction.ASC, "sessionDate").and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private SessionOverviewStatsResponse buildSessionOverviewStatsByCount(
            Specification<Session> baseSpec,
            ZoneId zone,
            String scope,
            Long therapistId,
            Long clientId,
            Instant startDate,
            Instant endDate) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth currentMonth = YearMonth.from(today);

        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant todayEndExclusive = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant weekStartInst = weekStart.atStartOfDay(zone).toInstant();
        Instant weekEndExclusive = weekEnd.plusDays(1).atStartOfDay(zone).toInstant();
        Instant monthStartInst = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant monthEndExclusive = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        long totalSessions = sessionRepository.count(baseSpec);
        long todaySessions = sessionRepository.count(baseSpec.and(sessionDateInHalfOpenRange(todayStart, todayEndExclusive)));
        long thisWeekSessions = sessionRepository.count(baseSpec.and(sessionDateInHalfOpenRange(weekStartInst, weekEndExclusive)));
        long thisMonthSessions = sessionRepository.count(baseSpec.and(sessionDateInHalfOpenRange(monthStartInst, monthEndExclusive)));
        long completedSessions = sessionRepository.count(baseSpec.and(sessionStatusEquals("completed")));
        long cancelledSessions = sessionRepository.count(baseSpec.and(sessionStatusIn("cancelled", "no-show")));
        long upcomingSessions = sessionRepository.count(baseSpec.and(sessionUpcoming(now)));

        return SessionOverviewStatsResponse.builder()
                .scope(scope)
                .therapistId(therapistId)
                .clientId(clientId)
                .timezone(zone.getId())
                .totalSessions(totalSessions)
                .todaySessions(todaySessions)
                .thisWeekSessions(thisWeekSessions)
                .thisMonthSessions(thisMonthSessions)
                .upcomingSessions(upcomingSessions)
                .completedSessions(completedSessions)
                .cancelledSessions(cancelledSessions)
                .sessionsByStatus(Map.of())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private Specification<Session> sessionDateInHalfOpenRange(Instant startInclusive, Instant endExclusive) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("sessionDate"), startInclusive),
                cb.lessThan(root.get("sessionDate"), endExclusive));
    }

    private Specification<Session> sessionStatusEquals(String status) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase(Locale.ROOT));
    }

    private Specification<Session> sessionStatusIn(String... statuses) {
        List<String> normalized = Arrays.stream(statuses)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
        return (root, query, cb) -> cb.lower(root.get("status")).in(normalized);
    }

    private Specification<Session> sessionUpcoming(Instant now) {
        return (root, query, cb) -> cb.and(
                cb.greaterThan(root.get("sessionDate"), now),
                cb.not(cb.lower(root.get("status")).in(List.of("completed", "cancelled", "no-show"))));
    }

    private SessionOverviewStatsResponse buildSessionOverviewStats(
            List<Session> sessions,
            ZoneId zone,
            String scope,
            Long therapistId,
            Long clientId,
            Instant startDate,
            Instant endDate) {
        List<Session> safeSessions = sessions != null ? sessions : List.of();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth currentMonth = YearMonth.from(today);

        long totalSessions = safeSessions.size();
        long todaySessions = safeSessions.stream()
                .filter(s -> s.getSessionDate() != null && s.getSessionDate().atZone(zone).toLocalDate().equals(today))
                .count();
        long thisWeekSessions = safeSessions.stream()
                .filter(s -> {
                    if (s.getSessionDate() == null) {
                        return false;
                    }
                    LocalDate d = s.getSessionDate().atZone(zone).toLocalDate();
                    return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
                })
                .count();
        long thisMonthSessions = safeSessions.stream()
                .filter(s -> s.getSessionDate() != null && YearMonth.from(s.getSessionDate().atZone(zone).toLocalDate()).equals(currentMonth))
                .count();
        long upcomingSessions = safeSessions.stream()
                .filter(s -> s.getSessionDate() != null && s.getSessionDate().isAfter(now))
                .filter(s -> s.getStatus() == null || (!isSessionStatusCancelled(s.getStatus())
                        && !isSessionStatusCompleted(s.getStatus())))
                .count();
        long completedSessions = safeSessions.stream()
                .filter(s -> s.getStatus() != null && isSessionStatusCompleted(s.getStatus()))
                .count();
        long cancelledSessions = safeSessions.stream()
                .filter(s -> s.getStatus() != null && isSessionStatusCancelled(s.getStatus()))
                .count();
        Map<String, Long> byStatus = safeSessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStatus() != null ? s.getStatus() : "unknown",
                        Collectors.counting()));

        return SessionOverviewStatsResponse.builder()
                .scope(scope)
                .therapistId(therapistId)
                .clientId(clientId)
                .timezone(zone.getId())
                .totalSessions(totalSessions)
                .todaySessions(todaySessions)
                .thisWeekSessions(thisWeekSessions)
                .thisMonthSessions(thisMonthSessions)
                .upcomingSessions(upcomingSessions)
                .completedSessions(completedSessions)
                .cancelledSessions(cancelledSessions)
                .sessionsByStatus(byStatus)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getUpcomingSessions(int limit, AuthPrincipal requester) {
        return getUpcomingSessions(limit, null, null, requester);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getUpcomingSessions(
            int limit,
            Instant rangeStart,
            Instant rangeEnd,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        int pageSize = normalizeDashboardLimit(limit);

        Instant now = Instant.now();
        Instant effectiveStart = laterInstant(now, rangeStart);
        Instant effectiveEnd = rangeEnd;
        if (effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            return PaginatedResponse.of(List.of(), 0, 1, pageSize);
        }

        Specification<Session> specification = buildSessionSpecification(
                effectiveStart, effectiveEnd, null, null, null, "scheduled",
                null, null, null, null, false, false, requester);

        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "sessionDate"));
        Page<Session> page = sessionRepository.findAll(specification, pageable);
        List<SessionSummaryResponse> items = toSessionSummaryResponses(page.getContent());
        return PaginatedResponse.of(items, page.getTotalElements(), 1, pageSize);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getPreviousSessions(int limit, AuthPrincipal requester) {
        return getPreviousSessions(limit, null, null, requester);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getPreviousSessions(
            int limit,
            Instant rangeStart,
            Instant rangeEnd,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        int pageSize = normalizeDashboardLimit(limit);

        Instant now = Instant.now();
        Instant effectiveStart = rangeStart;
        Instant effectiveEnd = earlierInstant(now, rangeEnd);
        if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            return PaginatedResponse.of(List.of(), 0, 1, pageSize);
        }

        Specification<Session> specification = buildSessionSpecification(
                effectiveStart, effectiveEnd, null, null, null, "completed",
                null, null, null, null, false, false, requester);

        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "sessionDate"));
        Page<Session> page = sessionRepository.findAll(specification, pageable);
        List<SessionSummaryResponse> items = toSessionSummaryResponses(page.getContent());
        return PaginatedResponse.of(items, page.getTotalElements(), 1, pageSize);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getOverdueSessions(int limit, AuthPrincipal requester) {
        return getOverdueSessions(limit, null, null, requester);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SessionSummaryResponse> getOverdueSessions(
            int limit,
            Instant rangeStart,
            Instant rangeEnd,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        int pageSize = normalizeDashboardLimit(limit);

        Instant now = Instant.now();
        Instant effectiveStart = rangeStart;
        Instant effectiveEnd = earlierInstant(now, rangeEnd);
        if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            return PaginatedResponse.of(List.of(), 0, 1, pageSize);
        }

        Specification<Session> specification = (root, query, cb) -> {
            Predicate pastDate = cb.lessThan(root.get("sessionDate"), now);
            Predicate notCompleted = cb.notEqual(root.get("status"), "completed");
            Predicate notCancelled = cb.notEqual(root.get("status"), "cancelled");
            Predicate predicate = cb.and(pastDate, notCompleted, notCancelled);
            if (effectiveStart != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("sessionDate"), effectiveStart));
            }
            if (effectiveEnd != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("sessionDate"), effectiveEnd));
            }
            return predicate;
        };

        specification = applyCaseloadScope(specification, requester);

        Pageable overduePageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "sessionDate"));
        Page<Session> overduePage = sessionRepository.findAll(specification, overduePageable);
        List<SessionSummaryResponse> overdueItems = toSessionSummaryResponses(overduePage.getContent());
        return PaginatedResponse.of(overdueItems, overduePage.getTotalElements(), 1, pageSize);
    }

    private static Instant laterInstant(Instant first, Instant second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private static Instant earlierInstant(Instant first, Instant second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    private static int normalizeDashboardLimit(int limit) {
        if (limit < 1) {
            return 5;
        }
        return Math.min(limit, 50);
    }

    @Transactional
    public void checkAndMarkOverdueSessions(AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");

        // PBAC: Check permission to manage sessions
        if (!permissionChecker.hasPermission(requester, "SESSION_VIEW") && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            throw new ForbiddenException("Insufficient permissions to check and mark overdue sessions");
        }

        Instant now = Instant.now();
        Specification<Session> specification = (root, query, cb) -> {
            Predicate pastDate = cb.lessThan(root.get("sessionDate"), now);
            Predicate notCompleted = cb.notEqual(root.get("status"), "completed");
            Predicate notCancelled = cb.notEqual(root.get("status"), "cancelled");
            Predicate notOverdue = cb.notEqual(root.get("status"), "overdue");
            return cb.and(pastDate, notCompleted, notCancelled, notOverdue);
        };

        // Apply role-based filtering
        if (hasRole(requester, "THERAPIST")) {
            specification = specification
                    .and((root, query, cb) -> cb.equal(root.get("therapist").get("id"), currentUserService.requireCurrentUser(requester).getId()));
        } else if (hasRole(requester, "SUPERVISOR")) {
            List<Long> supervisedTherapistIds = getSupervisedTherapistIds(currentUserService.requireCurrentUser(requester).getId());
            if (!supervisedTherapistIds.isEmpty()) {
                specification = specification
                        .and((root, query, cb) -> root.get("therapist").get("id").in(supervisedTherapistIds));
            } else {
                // If supervisor has no assigned therapists, return empty list
                return;
            }
        }

        List<Session> overdueSessions = sessionRepository.findAll(specification);

        for (Session session : overdueSessions) {
            session.setStatus("overdue");
            sessionRepository.save(session);

            // Trigger notification
            if (notificationService != null) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("sessionId", session.getId());
                eventData.put("clientId", session.getClient() != null ? session.getClient().getId() : null);
                eventData.put("therapistId", session.getTherapist() != null ? session.getTherapist().getId() : null);
                notificationService.processEvent(NotificationEventCatalog.SESSION_OVERDUE, eventData);
            }

            // Audit
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "session_marked_overdue", session.getId(), session.getClient().getId(),
                    ipAddress, true);
            log.info("Session {} for client {} marked as overdue.", session.getId(), session.getClient().getId());
        }
    }

    // Private helper methods

    private void validateNotInPast(Instant sessionDate) {
        if (sessionDate != null && sessionDate.isBefore(Instant.now())) {
            throw new BadRequestException("Session date cannot be in the past");
        }
    }

    private void validateBusinessHours(Instant sessionDate, Integer duration, ZoneId timezone) {
        ZonedDateTime localDateTime = sessionDate.atZone(timezone);
        int hour = localDateTime.getHour();
        int minute = localDateTime.getMinute();

        // Check if session starts at or after midnight (00:00)
        if (hour == BUSINESS_END_HOUR && minute == 0) {
            throw new BadRequestException(
                    "Sessions cannot be scheduled at or after 12:00 AM (midnight) in " + timezone.getId());
        }

        // Check if session would end after midnight
        if (duration != null) {
            Instant sessionEnd = sessionDate.plus(duration, ChronoUnit.MINUTES);
            ZonedDateTime localEndDateTime = sessionEnd.atZone(timezone);
            if (localEndDateTime.getHour() == BUSINESS_END_HOUR
                    || localEndDateTime.toLocalDate().isAfter(localDateTime.toLocalDate())) {
                throw new BadRequestException(
                        "Sessions cannot extend beyond 12:00 AM (midnight) in " + timezone.getId());
            }
        }

        // Check if session starts before business hours (8 AM)
        if (hour < BUSINESS_START_HOUR) {
            throw new BadRequestException("Sessions cannot be scheduled before 8:00 AM in " + timezone.getId());
        }
    }

    private Specification<Session> buildSessionSpecification(
            Instant startDate, Instant endDate, Long therapistId, Long clientId,
            String clientSearch, String status, String sessionType, Long serviceId, String serviceCode, Long roomId,
            Boolean mySessionsOnly, Boolean includeHiddenServices,
            AuthPrincipal requester) {
        Specification<Session> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.equal(root.get("client").get("isDeleted"), false));

        if (startDate != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.greaterThanOrEqualTo(root1.get("sessionDate"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.lessThanOrEqualTo(root1.get("sessionDate"), endDate));
        }

        if (therapistId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("therapist").get("id"), therapistId));
        }

        if (clientId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("client").get("id"), clientId));
        }

        if (StringUtils.hasText(clientSearch)) {
            String term = clientSearch.trim();
            spec = spec.and((root1, query1, cb1) -> clientSearchHelper.predicateForClientPath(
                    root1.get("client"), query1, cb1, term));
        }

        if (StringUtils.hasText(status)) {
            String statusKey = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.SESSION_STATUS, status);
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("status"), statusKey));
        }

        if (StringUtils.hasText(sessionType)) {
            String modeKey = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.SESSION_MODE, sessionType);
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("sessionType"), modeKey));
        }

        if (serviceId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("service").get("id"), serviceId));
        }

        if (StringUtils.hasText(serviceCode)) {
            String likeValue = "%" + serviceCode.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root1, query1, cb1) -> cb1.like(
                    cb1.lower(root1.get("service").get("serviceCode")),
                    likeValue));
        }

        if (roomId != null) {
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("room").get("id"), roomId));
        }

        if (Boolean.TRUE.equals(mySessionsOnly)) {
            Long currentUserId = currentUserService.requireCurrentUser(requester).getId();
            spec = spec.and((root1, query1, cb1) -> cb1.equal(root1.get("therapist").get("id"), currentUserId));
        }

        return applyCaseloadScope(spec, requester);
    }

    private Specification<Session> applyCaseloadScope(Specification<Session> spec, AuthPrincipal requester) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        return switch (resolved.scope()) {
            case ALL -> spec;
            case NONE -> spec.and((root, query, cb) -> cb.disjunction());
            case OWN -> spec.and((root, query, cb) ->
                    cb.equal(root.get("therapist").get("id"), resolved.currentUserId()));
            case TEAM -> {
                if (resolved.supervisedTherapistIds().isEmpty()) {
                    yield spec.and((root, query, cb) -> cb.disjunction());
                }
                yield spec.and((root, query, cb) ->
                        root.get("therapist").get("id").in(resolved.supervisedTherapistIds()));
            }
            case TEAM_AND_OWN -> {
                java.util.List<Long> ids = new java.util.ArrayList<>(resolved.supervisedTherapistIds());
                if (resolved.currentUserId() != null) ids.add(resolved.currentUserId());
                if (ids.isEmpty()) {
                    yield spec.and((root, query, cb) -> cb.disjunction());
                }
                yield spec.and((root, query, cb) -> root.get("therapist").get("id").in(ids));
            }
        };
    }

    private void validateSessionAccess(Session session, AuthPrincipal requester) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.ALL) {
            return;
        }
        if (resolved.scope() == CaseloadScope.NONE) {
            throw new ForbiddenException("You do not have permission to access sessions");
        }
        Long therapistId = session.getTherapist() != null ? session.getTherapist().getId() : null;
        if (!resolved.includesTherapist(therapistId)) {
            if (resolved.scope() == CaseloadScope.OWN) {
                throw new ForbiddenException("You can only access your own sessions");
            }
            throw new ForbiddenException("You can only access sessions of therapists you supervise");
        }
    }

    private List<Long> getSupervisedTherapistIds(Long supervisorId) {
        return caseloadScopeService.getSupervisedTherapistIds(supervisorId);
    }

    private void sendSessionConfirmationEmails(Session session) {
        // Obsolete: Notifications are now handled centrally by NotificationService
        // via the NotificationEventCatalog.SESSION_SCHEDULED event trigger.
    }

    private void sendSessionCancellationEmails(Session session) {
        // Obsolete: Notifications are now handled centrally by NotificationService
        // via the NotificationEventCatalog.SESSION_CANCELLED event trigger.
    }

    private void validateOnlineSessionZoomRequirements(String sessionMode, Boolean zoomEnabled, User therapist) {
        if (!isOnlineOrVirtualMode(sessionMode)) {
            return;
        }
        if (!Boolean.TRUE.equals(zoomEnabled)) {
            throw new BadRequestException(
                    "Online session requires Zoom. Please enable Zoom for the session and set therapist Zoom credentials.");
        }
        if (zoomService == null || !zoomService.isTherapistConfigured(therapist)) {
            throw new BadRequestException(
                    "Therapist Zoom is not configured or disabled. Please set Zoom credentials and enable Zoom before scheduling an online session.");
        }
    }

    /**
     * Create a Zoom meeting and attach SessionIntegration when the session is online
     * and has no meeting yet (e.g. in-person → online edit).
     */
    private void ensureZoomMeetingForSession(Session session, String timezone, boolean failHard) {
        User therapist = session.getTherapist();
        if (therapist == null) {
            if (failHard) {
                throw new BadRequestException("Therapist is required to create a Zoom meeting");
            }
            return;
        }
        if (getZoomIntegration(session) != null) {
            return;
        }
        if (zoomService == null || !zoomService.isTherapistConfigured(therapist)) {
            if (failHard) {
                throw new BadRequestException(
                        "Therapist Zoom is not configured or disabled. Please set Zoom credentials and enable Zoom before converting to an online session.");
            }
            log.warn("Zoom requested for session {} but therapist {} is not configured",
                    session.getId(), therapist.getId());
            return;
        }

        try {
            Long orgId = TenantContext.getOrganisationId();
            if (orgId != null && therapist.getId() != null) {
                subscriptionFeatureService.consumeUsageOrThrow(
                        orgId,
                        String.valueOf(therapist.getId()),
                        SubscriptionFeatureService.FEATURE_ZOOM_SESSIONS_PER_MONTH,
                        1L,
                        "Zoom session creation");
            }

            ZoomMeetingResponse zoomMeeting = zoomService.createMeeting(
                    buildZoomMeetingRequest(session, therapist, timezone),
                    therapist);

            SessionIntegration zoomIntegration = SessionIntegration.builder()
                    .session(session)
                    .provider("zoom")
                    .meetingId(zoomMeeting.getMeetingId())
                    .joinUrl(zoomMeeting.getJoinUrl())
                    .password(zoomMeeting.getPassword())
                    .build();
            if (session.getIntegrations() == null) {
                session.setIntegrations(new ArrayList<>());
            }
            session.getIntegrations().add(zoomIntegration);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Zoom meeting for session {}", session.getId(), e);
            if (failHard) {
                throw new BadRequestException(
                        "Unable to create Zoom meeting for online session. Please verify therapist Zoom credentials and try again.");
            }
        }
    }

    private Map<String, Object> buildZoomMeetingRequest(Session session, User therapist) {
        return buildZoomMeetingRequest(session, therapist, null);
    }

    private Map<String, Object> buildZoomMeetingRequest(Session session, User therapist, String requestedTimezone) {
        ZoneId meetingTz = resolveSessionTimezoneForUpdate(requestedTimezone, therapist.getId());

        Map<String, Object> request = new HashMap<>();
        request.put("topic", "Therapy Session with " + therapist.getFullName());
        request.put("startTime", session.getSessionDate());
        request.put("duration", session.getDuration());
        request.put("timezone", meetingTz.getId());
        request.put("settings", Map.of(
                "waiting_room", true,
                "video_host", true,
                "video_participant", true,
                "mute_upon_entry", true));
        return request;
    }

    private ZoneId resolveSessionTimezoneForUpdate(String requestedTimezone, Long therapistId) {
        if (StringUtils.hasText(requestedTimezone)) {
            try {
                return ZoneId.of(requestedTimezone.trim());
            } catch (Exception ex) {
                throw new BadRequestException(
                        "Invalid timezone: " + requestedTimezone + ". Please use an IANA timezone ID (e.g., Asia/Karachi)");
            }
        }
        return timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Please set timezone in therapist profile."));
    }

    private Map<String, Object> buildSessionEventData(Session session) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", session.getId());
        data.put("clientId", session.getClient().getId());
        NotificationPayloadFactory.putClientIdentity(data, session.getClient());
        data.put("therapistId", session.getTherapist().getId());
        data.put("therapistName", session.getTherapist().getFullName());
        data.put("sessionDate", session.getSessionDate());
        
        if (session.getSessionDate() != null) {
            data.put("sessionDateFormatted",
                    timezoneService.formatSessionDateTimeForPractice(session.getSessionDate()));
            data.put("practiceTimezone", timezoneService.getPracticeTimezone().getId());
        }

        data.put("duration", session.getDuration());
        String sessionMode = session.getSessionType();
        String clinicalSessionType = resolveClinicalSessionType(
                session.getClinicalSessionType(),
                session.getService());
        data.put("sessionType", clinicalSessionType);
        
        String formattedMode = "Unknown";
        if (sessionMode != null) {
            formattedMode = switch (sessionMode.toLowerCase()) {
                case "in_person" -> "In Person";
                case "video" -> "Video";
                case "phone" -> "Phone";
                default -> org.springframework.util.StringUtils.capitalize(sessionMode);
            };
        }
        data.put("sessionMode", sessionMode);
        data.put("status", session.getStatus());
        
        String serviceName = session.getService() != null ? session.getService().getServiceName() : null;
        data.put("serviceId", session.getService() != null ? session.getService().getId() : null);
        data.put("serviceName", serviceName);
        
        String roomName = session.getRoom() != null ? session.getRoom().getRoomName() : null;
        data.put("roomId", session.getRoom() != null ? session.getRoom().getId() : null);
        data.put("roomName", roomName);

        // Get Zoom data from SessionIntegration
        SessionIntegration zoomIntegration = getZoomIntegration(session);
        data.put("zoomEnabled", zoomIntegration != null);
        String zoomJoinUrl = null;
        if (zoomIntegration != null) {
            data.put("zoomMeetingId", zoomIntegration.getMeetingId());
            zoomJoinUrl = zoomIntegration.getJoinUrl();
            data.put("zoomJoinUrl", zoomJoinUrl);
            data.put("zoomPassword", zoomIntegration.getPassword());
        }
        
        StringBuilder detailsHtml = new StringBuilder();
        String dateOnly = null;
        String timeRange = null;
        if (session.getSessionDate() != null) {
            dateOnly = timezoneService.formatSessionDateOnlyForPractice(session.getSessionDate());
            timeRange = timezoneService.formatSessionTimeRangeForPractice(
                    session.getSessionDate(), session.getDuration());
            data.put("sessionDateOnlyFormatted", dateOnly);
            data.put("sessionTimeRangeFormatted", timeRange);
        }

        String locationLabel = formattedMode;
        if (org.springframework.util.StringUtils.hasText(zoomJoinUrl)) {
            locationLabel = "<a href=\"" + zoomJoinUrl + "\" style=\"color:"
                    + com.smart.therapy.flow.common.service.EmailHtmlComponents.PRIMARY
                    + ";\">SmartHub Video Link</a>";
        } else if (org.springframework.util.StringUtils.hasText(roomName)) {
            locationLabel = roomName + " (" + formattedMode + ")";
        }
        data.put("locationLabel", locationLabel);

        Object clientMrn = data.get("clientMrn");
        String mrnText = clientMrn != null ? clientMrn.toString() : null;
        String therapistName = session.getTherapist() != null ? session.getTherapist().getFullName() : null;

        detailsHtml.append(com.smart.therapy.flow.common.service.EmailHtmlComponents.sessionDetailsCardHtml(
                dateOnly,
                timeRange,
                mrnText,
                therapistName,
                locationLabel,
                serviceName));
        if (org.springframework.util.StringUtils.hasText(zoomJoinUrl)) {
            // Embedded in {{sessionDetailsHtml}} so existing email templates reach therapist + client
            // without requiring DB template re-seed for zoomMeetingHtml.
            String zoomPassword = zoomIntegration != null ? zoomIntegration.getPassword() : null;
            String zoomCard = com.smart.therapy.flow.common.service.EmailHtmlComponents.zoomMeetingCard(
                    zoomJoinUrl, zoomPassword);
            detailsHtml.append(zoomCard);
            data.put("zoomMeetingHtml", zoomCard);
        } else {
            data.put("zoomMeetingHtml", "");
        }
        data.put("sessionDetailsHtml", detailsHtml.toString());
        String frontendBase = com.smart.therapy.flow.common.service.EmailAppLinks.DEFAULT_FRONTEND_BASE;
        data.put("sessionUrl", com.smart.therapy.flow.common.service.EmailAppLinks.absolute(
                frontendBase,
                com.smart.therapy.flow.common.service.EmailAppLinks.STAFF_SCHEDULING));

        java.time.Instant start = session.getSessionDate();
        java.time.Instant end = start;
        if (start != null && session.getDuration() != null && session.getDuration() > 0) {
            end = start.plus(java.time.Duration.ofMinutes(session.getDuration()));
        }
        String calendarTitle = serviceName != null ? serviceName : "Therapy Session";
        if (mrnText != null && !mrnText.isBlank()) {
            calendarTitle = calendarTitle + " (" + mrnText + ")";
        }
        String locationPlain = locationLabel != null
                ? locationLabel.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim()
                : formattedMode;
        String calendarDetails = "SmartHub session"
                + (mrnText != null ? " · Client MRN: " + mrnText : "")
                + (therapistName != null ? " · Provider: " + therapistName : "");
        data.put("calendarUrl", com.smart.therapy.flow.common.service.EmailHtmlComponents.googleCalendarUrl(
                calendarTitle,
                start,
                end,
                calendarDetails,
                locationPlain));

        return data;
    }

    /**
     * Record audit via {@link AuditLogService#recordStaffEvent}.
     */
    private void recordAuditEvent(Long actorId, String action, Long sessionId, Long clientId, String ipAddress,
            boolean hipaaRelevant) {
        recordAuditEvent(actorId, action, sessionId, clientId, ipAddress, hipaaRelevant, null);
    }

    private void recordAuditEvent(Long actorId, String action, Long sessionId, Long clientId, String ipAddress,
            boolean hipaaRelevant, String details) {
        try {
            auditLogService.recordStaffEvent(actorId, action, "session", sessionId, clientId, ipAddress, hipaaRelevant,
                    details);
        } catch (Exception e) {
            log.error("Failed to record audit event for session: {}", sessionId, e);
        }
    }

    private SessionResponse toSessionResponse(Session session) {
        // Get Zoom integration data
        SessionIntegration zoomIntegration = getZoomIntegration(session);
        String modeValue = resolveEffectiveSessionMode(session);
        String clinicalSessionType = resolveClinicalSessionType(
                session.getClinicalSessionType(),
                session.getService());
        boolean exposeZoom = isOnlineOrVirtualMode(modeValue) && zoomIntegration != null;

        return SessionResponse.builder()
                .id(session.getId())
                .clientId(session.getClient().getId())
                .clientName(session.getClient().getFullName())
                .therapistId(session.getTherapist().getId())
                .therapistName(session.getTherapist().getFullName())
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .sessionType(clinicalSessionType)
                .sessionMode(modeValue)
                .status(session.getStatus())
                .serviceId(session.getService() != null ? session.getService().getId() : null)
                .serviceName(session.getService() != null ? session.getService().getServiceName() : null)
                .roomId(session.getRoom() != null ? session.getRoom().getId() : null)
                .roomName(session.getRoom() != null ? session.getRoom().getRoomName() : null)
                .notes(session.getNotes())
                .zoomEnabled(exposeZoom)
                .zoomMeetingId(exposeZoom ? zoomIntegration.getMeetingId() : null)
                .zoomJoinUrl(exposeZoom ? zoomIntegration.getJoinUrl() : null)
                .zoomPassword(exposeZoom ? zoomIntegration.getPassword() : null)
                .recurrenceGroupId(session.getRecurrenceGroupId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private SessionSummaryResponse toSessionSummaryResponse(Session session) {
        boolean hasTranscript = session.getId() != null
                && !sessionTranscriptRepository.findSessionIdsWithTranscripts(List.of(session.getId())).isEmpty();
        return toSessionSummaryResponse(session, hasTranscript);
    }

    /**
     * Map a page/list of sessions to summaries with batched relation + transcript lookups.
     * Avoids the per-row {@code findSessionIdsWithTranscripts} and lazy-load N+1 that made
     * scheduling month views (100+ sessions) take several seconds.
     */
    private List<SessionSummaryResponse> toSessionSummaryResponses(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }
        SessionSummaryMappingContext context = prepareSessionSummaryContext(sessions);
        List<SessionSummaryResponse> result = new ArrayList<>(sessions.size());
        for (Session session : sessions) {
            result.add(context.map(session));
        }
        return result;
    }

    private SessionSummaryMappingContext prepareSessionSummaryContext(Collection<Session> sessions) {
        List<Long> orderedIds = sessions.stream()
                .map(Session::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Session> byId = new HashMap<>();
        Set<Long> withTranscript = Set.of();
        if (!orderedIds.isEmpty()) {
            for (Session hydrated : sessionRepository.findByIdsForSummary(orderedIds)) {
                byId.put(hydrated.getId(), hydrated);
            }
            // Load Zoom/etc integrations without SELECT DISTINCT on json metadata
            // (Postgres cannot DISTINCT json columns).
            Map<Long, List<SessionIntegration>> integrationsBySessionId = sessionIntegrationRepository
                    .findBySessionIdIn(orderedIds)
                    .stream()
                    .filter(si -> si.getSession() != null && si.getSession().getId() != null)
                    .collect(Collectors.groupingBy(si -> si.getSession().getId()));
            for (Session session : byId.values()) {
                List<SessionIntegration> integrations = integrationsBySessionId.get(session.getId());
                if (integrations != null) {
                    session.setIntegrations(new ArrayList<>(integrations));
                } else if (session.getIntegrations() == null) {
                    session.setIntegrations(new ArrayList<>());
                }
            }
            withTranscript = Set.copyOf(sessionTranscriptRepository.findSessionIdsWithTranscripts(orderedIds));
        }

        // Keep any sessions that hydration missed (should be rare) so mapping still works.
        for (Session session : sessions) {
            if (session.getId() != null) {
                byId.putIfAbsent(session.getId(), session);
            }
        }

        return new SessionSummaryMappingContext(byId, withTranscript);
    }

    private final class SessionSummaryMappingContext {
        private final Map<Long, Session> byId;
        private final Set<Long> withTranscript;

        private SessionSummaryMappingContext(Map<Long, Session> byId, Set<Long> withTranscript) {
            this.byId = byId;
            this.withTranscript = withTranscript;
        }

        private SessionSummaryResponse map(Session session) {
            Session hydrated = session.getId() != null
                    ? byId.getOrDefault(session.getId(), session)
                    : session;
            boolean hasTranscript = session.getId() != null && withTranscript.contains(session.getId());
            return toSessionSummaryResponse(hydrated, hasTranscript);
        }
    }

    private SessionSummaryResponse toSessionSummaryResponse(Session session, boolean hasTranscript) {
        SessionIntegration zoomIntegration = getZoomIntegration(session);
        String modeValue = resolveEffectiveSessionMode(session);
        String clinicalSessionType = resolveClinicalSessionType(
                session.getClinicalSessionType(),
                session.getService());
        boolean exposeZoom = isOnlineOrVirtualMode(modeValue) && zoomIntegration != null;

        return SessionSummaryResponse.builder()
                .id(session.getId())
                .clientId(session.getClient().getId())
                .clientName(session.getClient().getFullName())
                .therapistId(session.getTherapist().getId())
                .therapistName(session.getTherapist().getFullName())
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .sessionType(clinicalSessionType)
                .sessionMode(modeValue)
                .status(session.getStatus())
                .serviceId(session.getService() != null ? session.getService().getId() : null)
                .serviceName(session.getService() != null ? session.getService().getServiceName() : null)
                .roomId(session.getRoom() != null ? session.getRoom().getId() : null)
                .roomName(session.getRoom() != null ? session.getRoom().getRoomName() : null)
                .zoomEnabled(exposeZoom)
                .zoomMeetingId(exposeZoom ? zoomIntegration.getMeetingId() : null)
                .zoomJoinUrl(exposeZoom ? zoomIntegration.getJoinUrl() : null)
                .zoomPassword(exposeZoom ? zoomIntegration.getPassword() : null)
                .recurrenceGroupId(session.getRecurrenceGroupId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .billingId(session.getBilling() != null ? session.getBilling().getId() : null)
                .hasInvoice(session.getBilling() != null)
                .hasTranscript(hasTranscript)
                .build();
    }

    /**
     * Prefer stored mode, but if the assigned room is virtual treat the session as online.
     * Guards against migrated rows that kept in-person mode with a virtual room.
     */
    private String resolveEffectiveSessionMode(Session session) {
        String modeValue = session.getSessionType();
        Room room = session.getRoom();
        if (room != null && room.getRoomType() == RoomType.VIRTUAL && !isOnlineOrVirtualMode(modeValue)) {
            return "online";
        }
        return modeValue;
    }

    private boolean isOnlineOrVirtualMode(String sessionMode) {
        return SystemOptionKeyMatcher.matchesAny(sessionMode, "online", "virtual", "telehealth", "video");
    }

    private String resolveSessionModeKey(String sessionMode) {
        if (!StringUtils.hasText(sessionMode)) {
            throw new BadRequestException("Session mode is required.");
        }
        return systemOptionResolverService.requireOptionKey(
                SystemOptionCategories.SESSION_MODE, sessionMode);
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred;
        }
        return StringUtils.hasText(fallback) ? fallback : null;
    }

    private String resolveClinicalSessionType(String requestedClinicalType, com.smart.therapy.flow.billing.entity.Service service) {
        if (StringUtils.hasText(requestedClinicalType)) {
            return systemOptionResolverService.parseOptionKey(
                    SystemOptionCategories.SESSION_TYPE,
                    requestedClinicalType.trim(),
                    requestedClinicalType.trim());
        }
        if (service == null) {
            return systemOptionResolverService.resolveDefaultOptionKey(SystemOptionCategories.SESSION_TYPE) != null
                    ? systemOptionResolverService.resolveDefaultOptionKey(SystemOptionCategories.SESSION_TYPE)
                    : "General";
        }
        return firstNonBlank(service.getCategory(), service.getServiceName());
    }

    private boolean isSessionModeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return systemOptionResolverService.resolveOptionKey(SystemOptionCategories.SESSION_MODE, value) != null;
    }

    private int resolveRoomSchedulingCapacity(Room room) {
        if (room.getRoomType() == RoomType.PHYSICAL) {
            return 1;
        }
        return (room.getCapacity() != null && room.getCapacity() > 0) ? room.getCapacity() : 1;
    }

    private boolean isActiveSessionForScheduling(Session session) {
        if (session.getStatus() == null) {
            return true;
        }
        return !SystemOptionKeyMatcher.matchesAny(session.getStatus(), "cancelled", "completed");
    }

    private ConflictInfo toConflictInfo(Session session) {
        return ConflictInfo.builder()
                .sessionId(session.getId())
                .sessionDate(session.getSessionDate())
                .duration(session.getDuration())
                .clientName(session.getClient().getFullName())
                .therapistName(session.getTherapist().getFullName())
                .roomName(session.getRoom() != null ? session.getRoom().getRoomName() : null)
                .build();
    }

    private Long requireSessionId(Session session) {
        Objects.requireNonNull(session, "Session is required");
        return Objects.requireNonNull(session.getId(), "Session id must not be null");
    }

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility in business logic validation
    // For access control, always use principal.hasPermission() instead
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleName));
    }

    private boolean isSessionStatusCancelled(String status) {
        return SystemOptionKeyMatcher.matchesAny(status, "cancelled", "no-show");
    }

    private boolean isSessionStatusCompleted(String status) {
        return SystemOptionKeyMatcher.matchesAny(status, "completed");
    }

    // Placeholder interfaces for optional services
    public interface EmailService {
        void sendSessionConfirmationEmail(Object recipient, Session session, Object otherParty);
    }

    public interface ZoomService {
        boolean isTherapistConfigured(User therapist);

        ZoomMeetingResponse createMeeting(Map<String, Object> request, User therapist);

        ZoomMeetingResponse updateMeeting(String meetingId, Map<String, Object> request, User therapist);

        boolean cancelMeeting(String meetingId, User therapist);

        boolean testIntegration(User therapist);
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    public BulkUploadSessionsResponse bulkUploadSessions(BulkUploadSessionsRequest request, AuthPrincipal requester,
             String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new BadRequestException("Invalid input: sessions must be a non-empty array");
        }

        int total = request.getSessions().size();
        int successful = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < request.getSessions().size(); i++) {
            Map<String, Object> sessionData = request.getSessions().get(i);
            try {
                // Convert map to CreateSessionRequest
                CreateSessionRequest createRequest = mapToCreateSessionRequest(sessionData);
                // Never skip schedule conflict checks for bulk (re-upload of same file must fail rows)
                createRequest.setIgnoreConflicts(false);

                // Create session (throws ConflictException when therapist/room/client time already booked)
                createSession(createRequest, requester, ipAddress);
                // Make this row visible to later rows in the same upload (same-file duplicates)
                sessionRepository.flush();
                successful++;
            } catch (ConflictException e) {
                failed++;
                Map<String, Object> error = new HashMap<>();
                error.put("row", i + 1);
                error.put("data", sessionData);
                error.put("type", "conflict");
                error.put("message", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "Scheduling conflict: a session is already scheduled at this date and time");
                errors.add(error);
            } catch (Exception e) {
                failed++;
                Map<String, Object> error = new HashMap<>();
                error.put("row", i + 1);
                error.put("data", sessionData);
                error.put("type", "error");
                String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                // Surface nested conflict messages that may be wrapped
                if (message.toLowerCase(Locale.ROOT).contains("conflict")) {
                    error.put("type", "conflict");
                }
                error.put("message", message);
                errors.add(error);
            }
        }

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "bulk_upload_sessions", null, null, ipAddress, true);

        return BulkUploadSessionsResponse.builder()
                .total(total)
                .successful(successful)
                .failed(failed)
                .errors(errors)
                .build();
    }

    public BulkUploadSessionsResponse bulkUploadSessionsFromCsv(String csvContent, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");
        if (!org.springframework.util.StringUtils.hasText(csvContent)) {
            throw new BadRequestException("CSV content is required");
        }

        List<String> lines = csvContent.lines().toList();
        if (lines.isEmpty()) {
            throw new BadRequestException("CSV content is empty");
        }

        // Skip instruction/comment lines (lines starting with #)
        int headerIndex = 0;
        while (headerIndex < lines.size()) {
            String line = lines.get(headerIndex);
            if (line != null && !line.isBlank() && !line.trim().startsWith("#")) {
                break;
            }
            headerIndex++;
        }
        if (headerIndex >= lines.size()) {
            throw new BadRequestException("CSV has no header row");
        }

        int startIndex = headerIndex;
        List<String> header = parseCsvLine(lines.get(headerIndex));
        if (!header.isEmpty() && header.get(0).toLowerCase(java.util.Locale.ROOT).contains("client")) {
            startIndex = headerIndex + 1;
        }

        List<java.util.Map<String, Object>> sessions = new java.util.ArrayList<>();
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank() || line.trim().startsWith("#")) {
                continue;
            }
            List<String> cols = parseCsvLine(line);
            if (cols.isEmpty()) {
                continue;
            }
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            for (int c = 0; c < Math.min(header.size(), cols.size()); c++) {
                String key = header.get(c);
                if (key == null || key.isBlank()) {
                    continue;
                }
                String value = cols.get(c);
                if (value != null && !value.isBlank()) {
                    row.put(key.trim(), value.trim());
                }
            }
            if (!row.isEmpty()) {
                sessions.add(row);
            }
        }

        if (sessions.isEmpty()) {
            throw new BadRequestException("CSV has no data rows");
        }

        BulkUploadSessionsRequest request = new BulkUploadSessionsRequest();
        request.setSessions(sessions);
        return bulkUploadSessions(request, requester, ipAddress);
    }

    public BulkUploadSessionsResponse bulkUploadSessionsFromCsvFile(
            org.springframework.web.multipart.MultipartFile file,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(requester, "Requester is required");
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV/XLSX file is required");
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        boolean isXlsx = fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm")
                || contentType.contains("spreadsheetml.sheet");

        if (isXlsx) {
            return bulkUploadSessionsFromXlsxFile(file, requester, ipAddress);
        }

        try {
            String csvContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return bulkUploadSessionsFromCsv(csvContent, requester, ipAddress);
        } catch (java.io.IOException e) {
            throw new BadRequestException("Failed to read CSV file");
        }
    }

    public String getBulkUploadTemplateCsv() {
        StringBuilder csv = new StringBuilder();
        // Leading instruction lines (ignored by parsers that skip # comments / empty rows).
        csv.append("# Instructions: All columns are required except therapistUsername and notes.\n");
        csv.append("# clientMrn = client medical record number (example CL-2026-0001). Must exist in the system.\n");
        csv.append("# therapistUsername = optional staff email. Leave blank to use the client's assigned therapist.\n");
        csv.append("# If filled, it must match the client's assigned therapist (error otherwise).\n");
        csv.append("# sessionDate = YYYY-MM-DD (example 2026-08-15).\n");
        csv.append("# sessionTime = 24-hour (09:00, 14:30) OR 12-hour with AM/PM (9:00 AM, 2:30 PM).\n");
        csv.append("# Times are interpreted in the therapist's schedule timezone (not UTC).\n");
        csv.append("# sessionMode = system option key/label (e.g. in_person, Online). Must exist in session_mode options.\n");
        csv.append("# sessionType = clinical session type key/label from system options (session_type). Must exist in the system.\n");
        csv.append("# serviceCode = billing service code. Must exist in the system.\n");
        csv.append("# roomNumber = required for in-person sessions (same as single booking). Room must exist in the system.\n");
        csv.append("# notes = optional.\n");
        csv.append("# Status is not in the CSV — every uploaded session is created as scheduled.\n");
        csv.append("# Duration is not in the CSV — it is taken automatically from the selected service.\n");
        csv.append(String.join(",", BULK_UPLOAD_HEADERS)).append("\n");
        csv.append(String.join(",", BULK_UPLOAD_SAMPLE_ROW)).append("\n");
        // Second sample: empty therapist (auto assigned) + online mode (no room) + 12-hour AM/PM time
        csv.append("CL-2026-0001,,2026-08-15,2:30 PM,Online,assessment,SRV-001,,PM time example\n");
        return csv.toString();
    }

    public byte[] getBulkUploadTemplateXlsx() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet instructions = workbook.createSheet("Instructions");
            String[] instructionLines = {
                    "Session bulk upload instructions",
                    "",
                    "All fields are required except therapistUsername and notes.",
                    "clientMrn: client's MRN (example: CL-2026-0001). Must exist in the system.",
                    "therapistUsername: optional. Leave blank to use the client's assigned therapist.",
                    "If therapistUsername is set, it must match that assigned therapist or the row fails.",
                    "sessionDate: YYYY-MM-DD (example: 2026-08-15).",
                    "sessionTime: 24-hour (09:00, 14:30) OR 12-hour with AM/PM (9:00 AM, 2:30 PM).",
                    "Times are local wall-clock values for the therapist's schedule timezone (not UTC).",
                    "sessionMode: must match a configured session_mode option key or label (e.g. in_person, Online).",
                    "sessionType: must match a configured session_type option key or label in the system.",
                    "serviceCode: must match an existing billing service code.",
                    "roomNumber: required for in-person sessions (same as single-session booking). Room must exist.",
                    "notes: optional.",
                    "Status is not uploaded — every row is created as scheduled.",
                    "Duration is not uploaded — taken automatically from the selected service (same as booking)."
            };
            for (int i = 0; i < instructionLines.length; i++) {
                instructions.createRow(i).createCell(0).setCellValue(instructionLines[i]);
            }
            instructions.autoSizeColumn(0);

            Sheet sheet = workbook.createSheet("Sessions");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < BULK_UPLOAD_HEADERS.size(); i++) {
                headerRow.createCell(i).setCellValue(BULK_UPLOAD_HEADERS.get(i));
            }

            Row sampleRow = sheet.createRow(1);
            for (int i = 0; i < BULK_UPLOAD_SAMPLE_ROW.size(); i++) {
                sampleRow.createCell(i).setCellValue(BULK_UPLOAD_SAMPLE_ROW.get(i));
            }
            // Second sample row with 12-hour AM/PM time
            Row sampleRowPm = sheet.createRow(2);
            sampleRowPm.createCell(0).setCellValue("CL-2026-0001");
            sampleRowPm.createCell(1).setCellValue("therapist@example.com");
            sampleRowPm.createCell(2).setCellValue("2026-08-15");
            sampleRowPm.createCell(3).setCellValue("2:30 PM");
            sampleRowPm.createCell(4).setCellValue("Online");
            sampleRowPm.createCell(5).setCellValue("assessment");
            sampleRowPm.createCell(6).setCellValue("SRV-001");
            sampleRowPm.createCell(7).setCellValue("");
            sampleRowPm.createCell(8).setCellValue("PM time example");

            for (int i = 0; i < BULK_UPLOAD_HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate XLSX template");
        }
    }

    public String getStaticBulkUploadTemplateCsv() {
        ClassPathResource resource = new ClassPathResource(STATIC_SESSION_TEMPLATE_CSV_PATH);
        if (!resource.exists()) {
            return getBulkUploadTemplateCsv();
        }

        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read static session CSV template, using generated template: {}", e.getMessage());
            return getBulkUploadTemplateCsv();
        }
    }

    public byte[] getStaticBulkUploadTemplateXlsx() {
        ClassPathResource resource = new ClassPathResource(STATIC_SESSION_TEMPLATE_XLSX_PATH);
        if (!resource.exists()) {
            return getBulkUploadTemplateXlsx();
        }

        try {
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.warn("Failed to read static session XLSX template, using generated template: {}", e.getMessage());
            return getBulkUploadTemplateXlsx();
        }
    }

    private BulkUploadSessionsResponse bulkUploadSessionsFromXlsxFile(
            org.springframework.web.multipart.MultipartFile file,
            AuthPrincipal requester,
            String ipAddress) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("XLSX file has no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new BadRequestException("XLSX file is empty");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null || headerRow.getLastCellNum() <= 0) {
                throw new BadRequestException("XLSX header row is missing");
            }

            DataFormatter formatter = new DataFormatter();
            List<String> header = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String key = formatter.formatCellValue(headerRow.getCell(c)).trim();
                header.add(key);
            }

            List<Map<String, Object>> sessions = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row rowData = sheet.getRow(r);
                if (rowData == null) {
                    continue;
                }

                Map<String, Object> rowMap = new HashMap<>();
                for (int c = 0; c < header.size(); c++) {
                    String key = header.get(c);
                    if (!StringUtils.hasText(key)) {
                        continue;
                    }
                    Cell cell = rowData.getCell(c);
                    String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    if (StringUtils.hasText(value)) {
                        rowMap.put(key, value);
                    }
                }
                if (!rowMap.isEmpty()) {
                    sessions.add(rowMap);
                }
            }

            if (sessions.isEmpty()) {
                throw new BadRequestException("XLSX has no data rows");
            }

            BulkUploadSessionsRequest request = new BulkUploadSessionsRequest();
            request.setSessions(sessions);
            return bulkUploadSessions(request, requester, ipAddress);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to read XLSX file");
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new java.util.ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }

    private CreateSessionRequest mapToCreateSessionRequest(Map<String, Object> data) {
        CreateSessionRequest request = new CreateSessionRequest();
        if (data == null || data.isEmpty()) {
            throw new BadRequestException("Session row is empty");
        }

        // Client: required MRN (maps to internal client id for session creation)
        String clientMrn = bulkCell(data, "clientMrn", "client_mrn", "clientMRN", "mrn", "clientId", "client_id");
        if (!StringUtils.hasText(clientMrn)) {
            throw new BadRequestException("clientMrn is required (client medical record number, e.g. CL-2026-0001)");
        }
        Client client = clientMrnService.findActiveByMrn(clientMrn)
                .orElseThrow(() -> new BadRequestException(
                        "Client MRN '" + clientMrn.trim() + "' does not exist in the system"));
        request.setClientId(client.getId());

        // Therapist: optional when the client has an assigned therapist (defaults to that assignment).
        // If provided, it must match the assigned therapist — same lock as single-session booking.
        String therapistIdRaw = bulkCell(data, "therapistId", "therapist_id");
        String therapistUsername = bulkCell(data,
                "therapistUsername", "therapist_username", "therapistEmail", "therapist", "therapistName");
        if (StringUtils.hasText(therapistIdRaw)
                && !StringUtils.hasText(therapistUsername)) {
            try {
                Long.parseLong(therapistIdRaw.trim());
            } catch (NumberFormatException e) {
                therapistUsername = therapistIdRaw;
            }
        }
        final String resolvedTherapistKey = StringUtils.hasText(therapistUsername)
                ? therapistUsername.trim()
                : null;
        User resolvedTherapist = null;
        if (StringUtils.hasText(therapistIdRaw)) {
            try {
                Long parsedId = Long.parseLong(therapistIdRaw.trim());
                resolvedTherapist = userRepository.findById(parsedId)
                        .orElseThrow(() -> new BadRequestException(
                                "Therapist id '" + therapistIdRaw.trim() + "' does not exist in the system"));
            } catch (NumberFormatException ignored) {
                // treated as username via resolvedTherapistKey above
            }
        }
        if (resolvedTherapist == null && StringUtils.hasText(resolvedTherapistKey)) {
            Optional<User> byEmail = userRepository.findByEmail(resolvedTherapistKey);
            if (byEmail.isEmpty()) {
                byEmail = userRepository.findByEmail(resolvedTherapistKey.toLowerCase(Locale.ROOT));
            }
            resolvedTherapist = byEmail.orElseThrow(() -> new BadRequestException(
                    "Therapist '" + resolvedTherapistKey + "' does not exist in the system"
                            + " (use the staff email, or leave blank to use the client's assigned therapist)"));
        }

        User assignedTherapist = client.getAssignedTherapist();
        String resolvedMrn = clientMrnService.displayMrn(client);
        if (resolvedMrn == null) {
            resolvedMrn = clientMrn.trim();
        }
        if (resolvedTherapist != null && assignedTherapist != null
                && !Objects.equals(resolvedTherapist.getId(), assignedTherapist.getId())) {
            throw new BadRequestException(
                    "Therapist does not match the assigned therapist for client MRN " + resolvedMrn
                            + ". Assigned therapist is " + formatTherapistLabel(assignedTherapist)
                            + ". Use that therapist, or leave therapistUsername blank to auto-fill.");
        }
        if (resolvedTherapist == null) {
            if (assignedTherapist != null) {
                resolvedTherapist = assignedTherapist;
            } else {
                throw new BadRequestException(
                        "therapistUsername is required for client MRN " + resolvedMrn
                                + " because that client has no assigned therapist");
            }
        }
        request.setTherapistId(resolvedTherapist.getId());

        // Service: serviceCode preferred (mirrors selecting a billing service) — must exist
        String serviceCode = bulkCell(data, "serviceCode", "service_code");
        String serviceIdRaw = bulkCell(data, "serviceId", "service_id");
        com.smart.therapy.flow.billing.entity.Service resolvedService = null;
        if (StringUtils.hasText(serviceCode)) {
            resolvedService = serviceRepository.findByServiceCode(serviceCode.trim())
                    .orElseThrow(() -> new BadRequestException(
                            "Service code '" + serviceCode.trim() + "' does not exist in the system"));
        } else if (StringUtils.hasText(serviceIdRaw)) {
            try {
                Long serviceId = Long.parseLong(serviceIdRaw.trim());
                resolvedService = serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new BadRequestException(
                                "Service id '" + serviceIdRaw.trim() + "' does not exist in the system"));
            } catch (NumberFormatException e) {
                resolvedService = serviceRepository.findByServiceCode(serviceIdRaw.trim())
                        .orElseThrow(() -> new BadRequestException(
                                "Service code '" + serviceIdRaw.trim() + "' does not exist in the system"));
            }
        }
        if (resolvedService == null) {
            throw new BadRequestException("serviceCode is required");
        }
        request.setServiceId(resolvedService.getId());

        // Session mode (required) — must match system options, same as single-session booking
        String sessionModeRaw = bulkCell(data, "sessionMode", "session_mode", "mode");
        String sessionTypeRaw = bulkCell(data, "sessionType", "session_type", "clinicalSessionType");
        if (!StringUtils.hasText(sessionModeRaw)
                && StringUtils.hasText(sessionTypeRaw)
                && isSessionModeValue(sessionTypeRaw)) {
            // legacy: mode stored under sessionType column
            sessionModeRaw = sessionTypeRaw;
            sessionTypeRaw = null;
        }
        if (!StringUtils.hasText(sessionModeRaw)) {
            throw new BadRequestException(
                    "sessionMode is required (e.g. in_person, Online). Use a session mode that exists in the system");
        }
        String resolvedSessionMode = systemOptionResolverService.resolveOptionKey(
                SystemOptionCategories.SESSION_MODE, sessionModeRaw.trim());
        if (resolvedSessionMode == null) {
            throw new BadRequestException(
                    "Session mode '" + sessionModeRaw.trim() + "' does not exist in the system");
        }
        request.setSessionMode(resolvedSessionMode);

        // Clinical session type (required) — match system options used by single-session booking
        // (session form uses service_type catalog; backend also accepts session_type)
        if (!StringUtils.hasText(sessionTypeRaw)) {
            throw new BadRequestException(
                    "sessionType is required. Use a session type that exists in the system");
        }
        String resolvedSessionType = systemOptionResolverService.resolveOptionKey(
                SystemOptionCategories.SESSION_TYPE, sessionTypeRaw.trim());
        if (resolvedSessionType == null) {
            resolvedSessionType = systemOptionResolverService.resolveOptionKey(
                    SystemOptionCategories.SERVICE_TYPE, sessionTypeRaw.trim());
        }
        if (resolvedSessionType == null) {
            throw new BadRequestException(
                    "Session type '" + sessionTypeRaw.trim() + "' does not exist in the system");
        }
        request.setSessionType(resolvedSessionType);

        // Room: required for in-person (same rule as single-session booking); must exist when provided
        resolveBulkUploadRoom(request, resolvedSessionMode, data);

        // Date / time as wall-clock in the therapist's schedule timezone (not UTC).
        // Same approach as single-session booking: date + time → Instant via therapist TZ.
        request.setSessionDate(parseBulkSessionDateTime(data, request.getTherapistId()));
        timezoneService.getTherapistTimezone(request.getTherapistId())
                .ifPresent(zone -> request.setTimezone(zone.getId()));

        // Status is never taken from CSV — bulk upload always creates scheduled sessions
        request.setStatus("scheduled");

        // Duration is never taken from CSV — createSession fills it from the selected service
        // (same as normal session booking). Clear the CreateSessionRequest default of 60.
        request.setDuration(null);

        // notes remain optional
        String notes = bulkCell(data, "notes", "note", "comments");
        if (StringUtils.hasText(notes)) {
            request.setNotes(notes);
        }

        // Online booking enables Zoom the same way as the UI
        if (SystemOptionKeyMatcher.matchesAny(resolvedSessionMode, "online", "virtual")) {
            request.setZoomEnabled(true);
            // Double-ensure no room is attached for online sessions
            request.setRoomId(null);
        } else {
            request.setZoomEnabled(false);
        }

        return request;
    }

    /**
     * Resolve and validate room for bulk upload — aligned with single-session booking:
     * - in-person: room is required and must be an existing active physical room
     * - online/virtual: room is ignored (even if roomNumber is present in the CSV)
     */
    private void resolveBulkUploadRoom(CreateSessionRequest request, String sessionModeKey, Map<String, Object> data) {
        // Online/virtual sessions never use a room — ignore CSV room columns completely.
        if (SystemOptionKeyMatcher.matchesAny(sessionModeKey, "online", "virtual")) {
            request.setRoomId(null);
            return;
        }

        String roomNumber = bulkCell(data, "roomNumber", "room_number", "room");
        String roomIdRaw = bulkCell(data, "roomId", "room_id");
        Room room = null;

        if (StringUtils.hasText(roomNumber)) {
            String roomKey = roomNumber.trim();
            room = roomRepository.findByRoomNumber(roomKey)
                    .or(() -> roomRepository.findByRoomName(roomKey))
                    .orElseThrow(() -> new BadRequestException(
                            "Room '" + roomKey + "' does not exist in the system"));
        } else if (StringUtils.hasText(roomIdRaw)) {
            String roomKey = roomIdRaw.trim();
            try {
                Long roomId = Long.parseLong(roomKey);
                room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new BadRequestException(
                                "Room id '" + roomKey + "' does not exist in the system"));
            } catch (NumberFormatException e) {
                room = roomRepository.findByRoomNumber(roomKey)
                        .or(() -> roomRepository.findByRoomName(roomKey))
                        .orElseThrow(() -> new BadRequestException(
                                "Room '" + roomKey + "' does not exist in the system"));
            }
        }

        boolean requiresRoom = SystemOptionKeyMatcher.matchesAny(sessionModeKey, "in_person", "in-person");
        if (requiresRoom && room == null) {
            throw new BadRequestException(
                    "roomNumber is required for in-person sessions (same as single session booking). "
                            + "Enter a room that exists in the system");
        }
        if (room == null) {
            return;
        }
        if (!Boolean.TRUE.equals(room.getIsActive())) {
            String label = StringUtils.hasText(room.getRoomNumber()) ? room.getRoomNumber() : String.valueOf(room.getId());
            throw new BadRequestException(
                    "Room '" + label + "' is inactive and cannot be used for scheduling");
        }
        if (requiresRoom && room.getRoomType() != RoomType.PHYSICAL) {
            throw new BadRequestException(
                    "In-person sessions require a physical room. Room '"
                            + room.getRoomNumber() + "' is not a physical room");
        }
        request.setRoomId(room.getId());
    }

    /**
     * Resolve a cell by exact key or case-insensitive key match.
     */
    private static String bulkCell(Map<String, Object> data, String... keys) {
        if (data == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object direct = data.get(key);
            if (direct != null && StringUtils.hasText(direct.toString())) {
                return direct.toString().trim();
            }
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String entryKey = entry.getKey().trim();
            for (String key : keys) {
                if (key != null && entryKey.equalsIgnoreCase(key)
                        && StringUtils.hasText(entry.getValue().toString())) {
                    return entry.getValue().toString().trim();
                }
            }
        }
        return null;
    }

    /**
     * Parse sessionDate (YYYY-MM-DD) + sessionTime (HH:MM) as wall-clock values in the
     * therapist's schedule timezone (same as New Session booking). Convert to Instant for storage.
     * Timezone is never read from the CSV.
     */
    private String formatTherapistLabel(User therapist) {
        if (therapist == null) {
            return "unknown";
        }
        String name = therapist.getFullName();
        String email = therapist.getEmail();
        if (StringUtils.hasText(name) && StringUtils.hasText(email)) {
            return name + " (" + email + ")";
        }
        if (StringUtils.hasText(email)) {
            return email;
        }
        if (StringUtils.hasText(name)) {
            return name;
        }
        return "id " + therapist.getId();
    }

    private Instant parseBulkSessionDateTime(Map<String, Object> data, Long therapistId) {
        String dateStr = bulkCell(data, "sessionDate", "session_date", "date");
        if (!StringUtils.hasText(dateStr)) {
            throw new BadRequestException("sessionDate is required (YYYY-MM-DD)");
        }
        dateStr = dateStr.trim();
        // If a full datetime was pasted, keep only the calendar date portion
        if (dateStr.contains("T")) {
            dateStr = dateStr.substring(0, Math.min(10, dateStr.length()));
        }

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new BadRequestException("Invalid sessionDate. Use YYYY-MM-DD (e.g. 2026-08-15)");
        }

        String timeRaw = bulkCell(data, "sessionTime", "session_time", "time", "startTime");
        if (!StringUtils.hasText(timeRaw)) {
            throw new BadRequestException(
                    "sessionTime is required. Use 24-hour (09:00, 14:30) or 12-hour with AM/PM (9:00 AM, 2:30 PM) in the therapist's local schedule");
        }
        LocalTime localTime = parseBulkLocalTime(timeRaw);

        ZoneId zone = timezoneService.getTherapistTimezone(therapistId)
                .orElseThrow(() -> new BadRequestException(
                        "Therapist timezone is not configured. Set timezone on the therapist profile before bulk upload."));

        return ZonedDateTime.of(localDate, localTime, zone).toInstant();
    }

    private static LocalTime parseBulkLocalTime(String raw) {
        String value = raw.trim();
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH));
        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalTime.parse(value.toUpperCase(Locale.ENGLISH), formatter);
            } catch (Exception ignored) {
                // try next
            }
        }
        throw new BadRequestException(
                "Invalid sessionTime. Use 24-hour format (09:00, 14:30) or 12-hour with AM/PM (9:00 AM, 2:30 PM)");
    }

    /**
     * Helper method to get Zoom integration from a session
     */
    private SessionIntegration getZoomIntegration(Session session) {
        if (session.getIntegrations() == null || session.getIntegrations().isEmpty()) {
            return null;
        }
        return session.getIntegrations().stream()
                .filter(integration -> "zoom".equalsIgnoreCase(integration.getProvider()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove Zoom meeting + session_integrations row when mode switches to in-person (or zoom is no longer valid).
     */
    private void clearZoomIntegration(Session session) {
        SessionIntegration zoom = getZoomIntegration(session);
        if (zoom == null) {
            return;
        }
        if (zoomService != null && StringUtils.hasText(zoom.getMeetingId()) && session.getTherapist() != null) {
            try {
                zoomService.cancelMeeting(zoom.getMeetingId(), session.getTherapist());
            } catch (Exception e) {
                log.warn("Failed to cancel Zoom meeting {} while clearing integration for session {}: {}",
                        zoom.getMeetingId(), session.getId(), e.getMessage());
            }
        }
        if (session.getIntegrations() != null) {
            session.getIntegrations().remove(zoom);
        }
        zoom.setSession(null);
    }

    @Transactional(readOnly = true)
    public BookingCountResponse getTherapistBookingCount(Long therapistId, Instant startDate, Instant endDate) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");

        // Get therapist
        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        // Get all sessions for this therapist
        List<Session> allSessions = sessionRepository.findByTherapistId(therapistId);

        // Filter by date range if provided
        if (startDate != null || endDate != null) {
            allSessions = allSessions.stream()
                    .filter(s -> {
                        if (s.getSessionDate() == null) return false;
                        if (startDate != null && s.getSessionDate().isBefore(startDate)) return false;
                        if (endDate != null && s.getSessionDate().isAfter(endDate)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        Instant now = Instant.now();

        // Count by status
        long totalBookings = allSessions.size();
        long upcomingBookings = allSessions.stream()
                .filter(s -> s.getSessionDate() != null && s.getSessionDate().isAfter(now) &&
                        (s.getStatus() == null || !SystemOptionKeyMatcher.matchesAny(s.getStatus(), "cancelled")))
                .count();
        long completedBookings = allSessions.stream()
                .filter(s -> s.getStatus() != null && SystemOptionKeyMatcher.matchesAny(s.getStatus(), "completed"))
                .count();
        long cancelledBookings = allSessions.stream()
                .filter(s -> s.getStatus() != null && SystemOptionKeyMatcher.matchesAny(s.getStatus(), "cancelled"))
                .count();

        return BookingCountResponse.builder()
                .therapistId(therapistId)
                .therapistName(therapist.getFullName())
                .totalBookings(totalBookings)
                .upcomingBookings(upcomingBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    @Transactional(readOnly = true)
    public SessionStatsResponse getTherapistSessionStats(Long therapistId, Instant startDate, Instant endDate) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");

        // Get therapist
        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        // Get all sessions for this therapist
        List<Session> allSessions = sessionRepository.findByTherapistId(therapistId);

        // Filter by date range if provided
        if (startDate != null || endDate != null) {
            allSessions = allSessions.stream()
                    .filter(s -> {
                        if (s.getSessionDate() == null) return false;
                        if (startDate != null && s.getSessionDate().isBefore(startDate)) return false;
                        if (endDate != null && s.getSessionDate().isAfter(endDate)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // Calculate statistics
        long totalSessions = allSessions.size();

        // Count by status
        Map<String, Long> sessionsByStatus = allSessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStatus() != null ? s.getStatus().toLowerCase(Locale.ROOT) : "unknown",
                        Collectors.counting()));

        // Count by type
        Map<String, Long> sessionsByType = allSessions.stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            String clinicalType = resolveClinicalSessionType(s.getClinicalSessionType(), s.getService());
                            return clinicalType != null ? clinicalType.toLowerCase(Locale.ROOT) : "unknown";
                        },
                        Collectors.counting()));

        // Calculate average duration
        double averageDuration = allSessions.stream()
                .filter(s -> s.getDuration() != null)
                .mapToInt(Session::getDuration)
                .average()
                .orElse(0.0);

        // Calculate total hours
        double totalHours = allSessions.stream()
                .filter(s -> s.getDuration() != null)
                .mapToInt(Session::getDuration)
                .sum() / 60.0;

        // Count unique clients
        long uniqueClients = allSessions.stream()
                .filter(s -> s.getClient() != null)
                .map(s -> s.getClient().getId())
                .distinct()
                .count();

        return SessionStatsResponse.builder()
                .therapistId(therapistId)
                .therapistName(therapist.getFullName())
                .totalSessions(totalSessions)
                .sessionsByStatus(sessionsByStatus)
                .sessionsByType(sessionsByType)
                .averageDuration(averageDuration)
                .totalHours(totalHours)
                .uniqueClients(uniqueClients)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
