package com.smart.therapy.flow.session.controller;

import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.session.dto.*;
import com.smart.therapy.flow.session.service.RecurringSessionService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.billing.dto.SessionBillingResponse;
import com.smart.therapy.flow.billing.dto.CreateSessionBillingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sessions", description = "Session management APIs including transcript upload/finalize and live transcription integration")
public class SessionController {

        private static final long TRANSCRIPTION_WS_TICKET_TTL_SECONDS = 60L;

        private final SessionService sessionService;
        private final RecurringSessionService recurringSessionService;
        private final SessionTranscriptService sessionTranscriptService;
        private final BillingService billingService;
        private final JwtTokenProvider jwtTokenProvider;

        @org.springframework.beans.factory.annotation.Autowired
        private com.smart.therapy.flow.common.config.AppProperties appProperties;

        @GetMapping
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(summary = "Get sessions (paginated)", description = """
                        Get a paginated list of sessions with filtering options.

                        **Query Parameters:**

                        **Optional (all have defaults):**
                        - `page`: Page number (default: 1)
                        - `pageSize`: Items per page (default: 25, max: 500)

                        **Optional Filters:**
                        - `startDate`: Filter sessions from this date/time (ISO 8601 format)
                        - `endDate`: Filter sessions until this date/time (ISO 8601 format)
                        - `therapistId`: Filter by therapist ID
                        - `clientId`: Filter by client ID
                        - `clientSearch`: Search by client name (partial match)
                        - `status`: Filter by status (scheduled, confirmed, completed, cancelled, no-show)
                        - `sessionType`: Filter by session mode (`online` or `in-person`)
                        - `serviceId`: Filter by billing service ID
                        - `serviceCode`: Filter by billing service code (partial match)
                        - `roomId`: Filter by room ID
                        - `mySessionsOnly`: Filter to current user's sessions only (default: false)
                        - `includeHiddenServices`: Include hidden services (default: false)

                        **Example Request:**
                        ```
                        GET /api/v1/sessions?page=1&pageSize=25&therapistId=1&status=scheduled&startDate=2025-12-01T00:00:00Z
                        ```

                        **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<?> getSessions(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Page number (optional, default: 1)", example = "1") @RequestParam(defaultValue = "1") int page,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Items per page (optional, default: 25, max: 500)", example = "25") @RequestParam(required = false) Integer pageSize,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter sessions from this date/time (optional, ISO 8601)", example = "2025-12-01T00:00:00Z") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter sessions until this date/time (optional, ISO 8601)", example = "2025-12-31T23:59:59Z") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by therapist ID (optional)", example = "1") @RequestParam(required = false) Long therapistId,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by client ID (optional)", example = "123") @RequestParam(required = false) Long clientId,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Search by client name (optional, partial match)", example = "faizan") @RequestParam(required = false) String clientSearch,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by status (optional). Valid values: scheduled, confirmed, completed, cancelled, no-show", example = "scheduled") @RequestParam(required = false) String status,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by session mode (optional). Valid values: online, in-person", example = "in-person") @RequestParam(required = false) String sessionType,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by billing service ID (optional)", example = "10") @RequestParam(required = false) Long serviceId,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by billing service code (optional, partial match)", example = "PSY") @RequestParam(required = false) String serviceCode,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter by room ID (optional)", example = "1") @RequestParam(required = false) Long roomId,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Filter to current user's sessions only (optional, default: false)", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean mySessionsOnly,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Include hidden services (optional, default: false)", example = "false") @RequestParam(required = false) Boolean includeHiddenServices,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Response shape: summary (default) or calendar (lean fields for month/week/day)", example = "calendar") @RequestParam(required = false, defaultValue = "summary") String view,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                int defaultPage = appProperties.getPagination().getDefaultPage();
                int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
                int maxPageSize = appProperties.getPagination().getMaxPageSize();
                int minPageSize = appProperties.getPagination().getMinPageSize();

                int safePage = Math.max(defaultPage, page);
                if (pageSize == null) {
                        pageSize = defaultPageSize;
                }
                int safePageSize = Math.min(Math.max(pageSize, minPageSize), maxPageSize);
                if ("calendar".equalsIgnoreCase(view)) {
                        return ResponseEntity.ok(sessionService.getSessionsForCalendar(
                                        safePage, safePageSize, startDate, endDate, therapistId, clientId,
                                        clientSearch, status, sessionType, serviceId, serviceCode, roomId, mySessionsOnly,
                                        includeHiddenServices, principal));
                }
                PaginatedResponse<SessionSummaryResponse> response = sessionService.getSessions(
                                safePage, safePageSize, startDate, endDate, therapistId, clientId,
                                clientSearch, status, sessionType, serviceId, serviceCode, roomId, mySessionsOnly,
                                includeHiddenServices, principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        public ResponseEntity<SessionResponse> getSession(
                        @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                SessionResponse session = sessionService.getSession(id, principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(session);
        }

        @PostMapping
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_CREATE_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(summary = "Create a new therapy session", description = """
                        Create a new therapy session appointment.

                        **Required Fields:**
                        - `clientId` (REQUIRED): ID of the client
                        - `therapistId` (REQUIRED): ID of the therapist
                        - `sessionDate` (REQUIRED): Date and time of the session (ISO 8601 format, must be present or future)
                        - `sessionMode` (REQUIRED): Session mode (`online` or `in-person`)
                        - `sessionType` (optional): Clinical session type label (e.g. Assessment, Physiotherapy, Consultation)
                        - `status` (optional, default: `scheduled`): Session status (scheduled, confirmed, completed, cancelled, no-show)

                        **Optional Fields:**
                        - `duration` (optional, default: 60): Session duration in minutes (min: 15, max: 480)
                        - `serviceId` (optional): Billing service ID
                        - `roomId` (optional): Room ID for in-person sessions
                        - `notes` (optional): Additional notes
                        - `zoomEnabled` (optional, default: false): Enable Zoom meeting creation
                        - `timezone` (optional): IANA timezone ID (defaults to therapist's timezone)
                        - `ignoreConflicts` (optional, default: false): Ignore scheduling conflicts (use with caution)

                        **Request Body Example:**
                        ```json
                        {
                          "clientId": 123,
                          "therapistId": 1,
                          "sessionDate": "2025-12-25T14:00:00Z",
                          "sessionMode": "in-person",
                          "sessionType": "Assessment",
                          "status": "scheduled",
                          "duration": 60,
                          "serviceId": 1,
                          "roomId": 1,
                          "notes": "First session - intake assessment",
                          "zoomEnabled": false
                        }
                        ```

                        **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"), requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Session information to create", required = true, content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateSessionRequest.class), examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Standard Session", value = """
                        {
                          "clientId": 123,
                          "therapistId": 1,
                          "sessionDate": "2025-12-25T14:00:00Z",
                          "sessionMode": "in-person",
                          "sessionType": "Assessment",
                          "status": "scheduled",
                          "duration": 60,
                          "serviceId": 1,
                          "roomId": 1,
                          "notes": "First session - intake assessment",
                          "zoomEnabled": false
                        }
                        """))))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session created successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error, scheduling conflict, or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<SessionResponse> createSession(
                        @Valid @RequestBody CreateSessionRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                SessionResponse session = sessionService.createSession(request, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.status(201).body(session);
        }

        @PostMapping("/recurring/preview")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Preview recurring session dates",
                        description = "Expand a recurrence rule and return candidate dates with conflict flags without saving.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<RecurrencePreviewResponse> previewRecurringSessions(
                        @Valid @RequestBody RecurrenceRuleRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                RecurrencePreviewResponse response = recurringSessionService.previewRecurringSessions(request, principal);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/recurring")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_CREATE_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Create recurring session series",
                        description = "Book all conflict-free occurrences in a recurring series. Conflicting dates are skipped.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<CreateRecurringSessionsResponse> createRecurringSessions(
                        @Valid @RequestBody RecurrenceRuleRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                CreateRecurringSessionsResponse response = recurringSessionService.createRecurringSessions(
                                request,
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.status(201).body(response);
        }

        @PutMapping("/recurring/{groupId}/future")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Update this and all future recurring sessions",
                        description = "Apply anchor session changes to all upcoming occurrences in the series.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<SessionResponse>> updateFutureRecurringSessions(
                        @PathVariable("groupId") String groupId,
                        @Valid @RequestBody UpdateRecurringFutureRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                List<SessionResponse> response = recurringSessionService.updateFutureRecurringSessions(
                                groupId,
                                request,
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/recurring/{groupId}")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_DELETE_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Cancel upcoming recurring series sessions",
                        description = "Cancel all upcoming scheduled/confirmed sessions in the series.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<CancelRecurringSeriesResponse> cancelRecurringSeries(
                        @PathVariable("groupId") String groupId,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                CancelRecurringSeriesResponse response = recurringSessionService.cancelRecurringSeries(
                                groupId,
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{id}")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(summary = "Update session", description = """
                        Update an existing session. **All fields are optional** - only include the fields you want to update.

                        **Optional Fields (include only what you want to update):**
                        - `clientId`: Change the client
                        - `therapistId`: Change the therapist
                        - `sessionDate`: Change the date/time
                        - `sessionMode`: Change the session mode (`online` or `in-person`)
                        - `sessionType`: Update clinical session type label
                        - `status`: Update the status
                        - `duration`: Change the duration (15-480 minutes)
                        - `serviceId`: Change the billing service
                        - `roomId`: Change the room
                        - `notes`: Update notes
                        - `zoomEnabled`: Enable/disable Zoom meeting creation
                        - `timezone`: IANA timezone for update validation/context (e.g., `Asia/Karachi`)
                        - `ignoreConflicts`: Ignore scheduling conflicts (use with caution)

                        **Request Body Example (updating only status and notes):**
                        ```json
                        {
                          "status": "completed",
                          "notes": "Session completed successfully. Client showed improvement."
                        }
                        ```

                        **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"), requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Session fields to update (all optional)", required = true, content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UpdateSessionRequest.class), examples = @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Status Update", value = """
                        {
                          "status": "completed",
                          "notes": "Session completed successfully"
                        }
                        """))))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session updated successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error, scheduling conflict, or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<SessionResponse> updateSession(
                        @PathVariable("id") Long id,
                        @Valid @RequestBody UpdateSessionRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                SessionResponse session = sessionService.updateSession(id, request, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(session);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_DELETE_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Delete session",
                        description = "Soft-delete a session by ID. The session remains in audit history but is hidden from active queries.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Session deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Session already deleted or invalid request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> deleteSession(
                        @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                sessionService.deleteSession(id, principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/conflicts/check")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        public ResponseEntity<ConflictCheckResponse> checkConflicts(
                        @RequestParam Long therapistId,
                        @RequestParam(required = false) Long roomId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant sessionDate,
                        @RequestParam(defaultValue = "60") Integer duration) {
                ConflictCheckResponse response = sessionService.checkConflicts(therapistId, roomId, sessionDate,
                                duration);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/availability")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        public ResponseEntity<AvailabilityResponse> getAvailability(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) Long therapistId,
                        @RequestParam(required = false) Long roomId) {
                AvailabilityResponse response = sessionService.getAvailability(date, therapistId, roomId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/stats/overview")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Get session overview stats",
                        description = """
                                        Get consolidated session stats for dashboard cards.

                                        **Returned counters:**
                                        - `todaySessions`
                                        - `thisWeekSessions`
                                        - `thisMonthSessions`
                                        - `upcomingSessions`
                                        - `completedSessions`
                                        - `cancelledSessions`
                                        - `totalSessions`

                                        **Optional filters:**
                                        - `therapistId`: therapist-specific stats
                                        - `clientId`: client-specific stats
                                        - `startDate`, `endDate`: restrict session pool before counters
                                        - `timezone`: IANA timezone used for Today/Week/Month buckets (default: practice timezone, else `UTC`)
                                        """,
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<SessionOverviewStatsResponse> getSessionOverviewStats(
                        @RequestParam(required = false) Long therapistId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
                        @RequestParam(required = false) String timezone,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                SessionOverviewStatsResponse response = sessionService.getSessionOverviewStats(
                                startDate, endDate, therapistId, clientId, timezone, principal);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/dashboard/role-view")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Get role-scoped session dashboard",
                        description = """
                                        Returns role-scoped session buckets for authenticated user:
                                        today, week, month, all, upcoming, cancelled, completed, recent.

                                        Scope is permission-driven:
                                        - Admin (`CLIENT_VIEW_ALL`): all sessions in organisation
                                        - Therapist (`CLIENT_VIEW_OWN`): only own sessions
                                        - Supervisor (`CLIENT_VIEW_TEAM`): supervised therapists sessions
                                        """,
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<SessionRoleDashboardResponse> getRoleScopedDashboard(
                        @RequestParam(required = false) String timezone,
                        @RequestParam(defaultValue = "10") int recentLimit,
                        @RequestParam(defaultValue = "200") int allLimit,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                SessionRoleDashboardResponse response = sessionService.getRoleBasedSessionDashboard(
                                timezone, recentLimit, allLimit, principal);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/history/all")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(summary = "Get all session history", description = "Get all sessions (complete history) ordered by date descending", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<SessionSummaryResponse>> getAllSessionHistory(
                        @AuthenticationPrincipal AuthPrincipal principal) {
                List<SessionSummaryResponse> sessions = sessionService.getAllSessionHistory(principal);
                return ResponseEntity.ok(sessions);
        }

        @GetMapping("/history/clients/{clientId}")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(summary = "Get client session history", description = "Get all sessions for a specific client (previous and scheduled)", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<SessionSummaryResponse>> getClientSessionHistory(
                        @PathVariable("clientId") Long clientId,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                List<SessionSummaryResponse> sessions = sessionService.getClientSessionHistory(clientId, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(sessions);
        }

        @GetMapping("/history/{year}/{month}/{day}/day")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(summary = "Get sessions by day", description = "Get all sessions for a specific day", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<SessionSummaryResponse>> getSessionsByDay(
                        @PathVariable("year") int year,
                        @PathVariable("month") int month,
                        @PathVariable("day") int day,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                List<SessionSummaryResponse> sessions = sessionService.getSessionsByDay(year, month, day, principal);
                return ResponseEntity.ok(sessions);
        }

        @GetMapping("/history/{year}/{week}/week")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(summary = "Get sessions by week", description = "Get all sessions for a specific week (ISO week number)", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<SessionSummaryResponse>> getSessionsByWeek(
                        @PathVariable("year") int year,
                        @PathVariable("week") int week,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                List<SessionSummaryResponse> sessions = sessionService.getSessionsByWeek(year, week, principal);
                return ResponseEntity.ok(sessions);
        }

        @GetMapping("/history/{year}/{month}/month")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(summary = "Get sessions by month", description = "Get all sessions for a specific month", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<SessionSummaryResponse>> getSessionsByMonth(
                        @PathVariable("year") int year,
                        @PathVariable("month") int month,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                List<SessionSummaryResponse> sessions = sessionService.getSessionsByMonth(year, month, principal);
                return ResponseEntity.ok(sessions);
        }

        @GetMapping("/recent")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        public ResponseEntity<List<SessionSummaryResponse>> getRecentSessions(
                        @RequestParam(defaultValue = "10") int limit,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                List<SessionSummaryResponse> sessions = sessionService.getRecentSessions(limit, principal);
                return ResponseEntity.ok(sessions);
        }

        @GetMapping("/upcoming")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Get upcoming sessions",
                        description = "Returns upcoming scheduled sessions. Optional startDate/endDate narrow the window (ISO 8601).",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<PaginatedResponse<SessionSummaryResponse>> getUpcomingSessions(
                        @RequestParam(defaultValue = "5") int limit,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                return ResponseEntity.ok(sessionService.getUpcomingSessions(limit, startDate, endDate, principal));
        }

        @GetMapping("/previous")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Get previous sessions",
                        description = "Returns previous completed sessions. Optional startDate/endDate narrow the window (ISO 8601).",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<PaginatedResponse<SessionSummaryResponse>> getPreviousSessions(
                        @RequestParam(defaultValue = "5") int limit,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                return ResponseEntity.ok(sessionService.getPreviousSessions(limit, startDate, endDate, principal));
        }

        @GetMapping("/overdue")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Get overdue sessions",
                        description = "Returns overdue sessions. Optional startDate/endDate narrow the window (ISO 8601).",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<PaginatedResponse<SessionSummaryResponse>> getOverdueSessions(
                        @RequestParam(defaultValue = "5") int limit,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                return ResponseEntity.ok(sessionService.getOverdueSessions(limit, startDate, endDate, principal));
        }

        @PostMapping("/check-overdue")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        public ResponseEntity<Object> checkOverdueSessions(
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                sessionService.checkAndMarkOverdueSessions(principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(new Object() {
                        public final String message = "Overdue sessions checked and marked";
                });
        }

        @PostMapping("/bulk-upload")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        public ResponseEntity<BulkUploadSessionsResponse> bulkUploadSessions(
                        @Valid @RequestBody BulkUploadSessionsRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                BulkUploadSessionsResponse response = sessionService.bulkUploadSessions(request, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(response);
        }

        @GetMapping(value = "/bulk-upload/template", produces = "text/csv")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Download session bulk upload CSV template",
                        description = "Downloads a CSV template for session bulk uploads. Admin and Supervisor only.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<String> downloadBulkUploadTemplate() {
                String csv = sessionService.getBulkUploadTemplateCsv();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sessions_bulk_upload_template.csv\"")
                                .contentType(MediaType.valueOf("text/csv"))
                                .body(csv);
        }

        @GetMapping(value = "/bulk-upload/template/static", produces = "text/csv")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Download static session bulk upload CSV template",
                        description = "Downloads static CSV template from resources for session bulk uploads. Falls back to generated template when static file is missing.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<String> downloadStaticBulkUploadTemplate() {
                String csv = sessionService.getStaticBulkUploadTemplateCsv();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"sessions_bulk_upload_template_static.csv\"")
                                .contentType(MediaType.valueOf("text/csv"))
                                .body(csv);
        }

        @GetMapping(value = "/bulk-upload/template.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Download session bulk upload XLSX template",
                        description = "Downloads an Excel template (.xlsx) for session bulk uploads. Admin and Supervisor only.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<byte[]> downloadBulkUploadTemplateXlsx() {
                byte[] xlsx = sessionService.getBulkUploadTemplateXlsx();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sessions_bulk_upload_template.xlsx\"")
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(xlsx);
        }

        @GetMapping(value = "/bulk-upload/template.xlsx/static", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Download static session bulk upload XLSX template",
                        description = "Downloads static Excel template from resources for session bulk uploads. Falls back to generated template when static file is missing.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<byte[]> downloadStaticBulkUploadTemplateXlsx() {
                byte[] xlsx = sessionService.getStaticBulkUploadTemplateXlsx();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"sessions_bulk_upload_template_static.xlsx\"")
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(xlsx);
        }

        @PostMapping(value = "/bulk-upload/import", consumes = "text/csv")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Bulk upload sessions from CSV",
                        description = "Uploads sessions from a CSV payload. Admin and Supervisor only.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<BulkUploadSessionsResponse> bulkUploadSessionsFromCsv(
                        @RequestBody String csv,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                BulkUploadSessionsResponse response = sessionService.bulkUploadSessionsFromCsv(csv, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/bulk-upload/import-file", consumes = "multipart/form-data")
        @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_SESSION_CREATE)
        @io.swagger.v3.oas.annotations.Operation(
                        summary = "Bulk upload sessions from CSV/XLSX file",
                        description = "Uploads sessions from a CSV or Excel (.xlsx) file (multipart/form-data). Admin and Supervisor only.",
                        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<BulkUploadSessionsResponse> bulkUploadSessionsFromCsvFile(
                        @RequestParam("file") MultipartFile file,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                BulkUploadSessionsResponse response = sessionService.bulkUploadSessionsFromCsvFile(file, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{id}/status")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @io.swagger.v3.oas.annotations.Operation(summary = "Update session status", description = """
                        Update the status of a session.

                        **Valid Status Values:**
                        - `SCHEDULED`: Session is scheduled
                        - `CONFIRMED`: Session is confirmed
                        - `IN_PROGRESS`: Session is currently in progress
                        - `COMPLETED`: Session is completed
                        - `CANCELLED`: Session is cancelled
                        - `RESCHEDULING`: Session is being rescheduled
                        - `NO_SHOW`: Client did not show up
                        - `OVERDUE`: Session is overdue

                        **Special Actions:**
                        - When status is set to `COMPLETED`, billing is automatically created if it doesn't exist

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<SessionResponse> updateSessionStatus(
                        @PathVariable("id") Long id,
                        @Valid @RequestBody UpdateSessionStatusRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                SessionResponse session = sessionService.updateSessionStatus(
                                id, request.getStatus(), principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(session);
        }

        @GetMapping("/{id}/zoom/start")
        @PreAuthorize("hasAnyAuthority('CONSENT_ADMIN_VIEW', 'CLIENT_PORTAL_ACCESS') and (hasAuthority('SESSION_VIEW') or hasAuthority('CLIENT_VIEW_OWN_SESSIONS'))")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get Zoom meeting details to start meeting", description = """
                        Get Zoom meeting join URL and password for a session.
                        Returns meeting details if Zoom is enabled for the session.

                        **Requires:** ADMIN, SUPERVISOR, THERAPIST, or CLIENT role.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<ZoomMeetingDetailsResponse> getZoomMeetingDetails(
                        @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                SessionResponse session = sessionService.getSession(id, principal, HttpRequestUtil.getClientIp(httpRequest));

                if (!Boolean.TRUE.equals(session.getZoomEnabled())) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException(
                                        "Zoom is not enabled for this session");
                }

                if (session.getZoomJoinUrl() == null || session.getZoomJoinUrl().isBlank()) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException(
                                        "Zoom meeting URL is not available");
                }

                ZoomMeetingDetailsResponse response = ZoomMeetingDetailsResponse.builder()
                                .sessionId(id)
                                .meetingId(session.getZoomMeetingId())
                                .joinUrl(session.getZoomJoinUrl())
                                .password(session.getZoomPassword())
                                .build();

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}/billing")
        @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
        public ResponseEntity<SessionBillingResponse> getSessionBilling(
                        @PathVariable("id") Long id) {
                SessionBillingResponse billing = billingService.getSessionBilling(id);
                return ResponseEntity.ok(billing);
        }

        @PostMapping("/{id}/billing")
        @PreAuthorize(PermissionConstants.BILLING_CREATE_ACCESS)
        public ResponseEntity<SessionBillingResponse> createSessionBilling(
                        @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                CreateSessionBillingRequest request = new CreateSessionBillingRequest();
                request.setSessionId(id);
                SessionBillingResponse billing = billingService.createSessionBilling(request, principal,
                                HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.status(201).body(billing);
        }

        @PostMapping("/{sessionId}/transcribe-start")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @Operation(summary = "Start transcript upload", description = """
                        Creates a server-owned transcript uploadId for chunked Whisper transcription.
                        Use the returned uploadId for:
                        1) REST chunking: POST /api/v1/sessions/{sessionId}/transcribe-chunk, then /transcribe-finalize
                        2) Live preview websocket: wss://<host>/ws/transcribe-live?uploadId=<id>&language=en&ticket=<websocketTicket>
                        The ticket is scoped to this upload and expires after 60 seconds. Do not place an access JWT in the URL.
                        WebSocket expects binary audio frames and supports optional text control message: 'finalize'.
                        """)
        public ResponseEntity<TranscribeStartResponse> startTranscriptUpload(
                        @PathVariable("sessionId") Long sessionId,
                        @Valid @RequestBody(required = false) TranscribeStartRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                TranscribeStartResponse response = sessionTranscriptService.startUpload(
                                sessionId,
                                request != null ? request : new TranscribeStartRequest(),
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent"));
                response.setWebsocketTicket(jwtTokenProvider.generateTranscriptionWebSocketTicket(
                                principal, response.getUploadId(), TRANSCRIPTION_WS_TICKET_TTL_SECONDS));
                response.setWebsocketTicketExpiresInSeconds(TRANSCRIPTION_WS_TICKET_TTL_SECONDS);
                return ResponseEntity.ok(response);
        }

        @PostMapping(value = "/{sessionId}/transcribe-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @Operation(summary = "Upload and transcribe audio chunk", description = """
                        Accepts one standalone-decodable audio chunk (WebM/Opus or MP4), transcribes it with Whisper,
                        and stores the text keyed by chunkIndex for later finalize stitching.
                        """)
        public ResponseEntity<TranscribeChunkResponse> transcribeTranscriptChunk(
                        @PathVariable("sessionId") Long sessionId,
                        @RequestParam("uploadId") String uploadId,
                        @RequestParam("chunkIndex") Integer chunkIndex,
                        @RequestParam("chunkDurationSeconds") Double chunkDurationSeconds,
                        @RequestParam("audio") MultipartFile audioFile,
                        @RequestParam(value = "language", required = false) String language,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                if (audioFile == null || audioFile.isEmpty()) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException("Missing audio chunk");
                }
                String contentType = audioFile.getContentType();
                if (contentType == null
                                || (!contentType.startsWith("audio/") && !contentType.equals("video/webm"))) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException(
                                        "Only audio files are allowed");
                }
                if (audioFile.getSize() > 25 * 1024 * 1024) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException(
                                        "Audio file size exceeds 25MB limit");
                }
                try {
                        TranscribeChunkResponse response = sessionTranscriptService.processChunk(
                                        sessionId,
                                        uploadId,
                                        chunkIndex,
                                        chunkDurationSeconds,
                                        audioFile.getBytes(),
                                        audioFile.getOriginalFilename(),
                                        language,
                                        principal,
                                        HttpRequestUtil.getClientIp(httpRequest),
                                        httpRequest.getHeader("User-Agent"));
                        return ResponseEntity.ok(response);
                } catch (java.io.IOException ex) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException("Failed to read uploaded audio");
                }
        }

        @PostMapping("/{sessionId}/transcribe-finalize")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @Operation(summary = "Finalize transcript upload", description = """
                        Verifies all uploaded and silent chunks are accounted for, stitches text in timestamp order
                        with Therapist labels, and stores the finalized transcript. Returns 409 if chunks are missing.
                        """)
        public ResponseEntity<TranscribeFinalizeResponse> finalizeTranscript(
                        @PathVariable("sessionId") Long sessionId,
                        @Valid @RequestBody TranscribeFinalizeRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                TranscribeFinalizeResponse response = sessionTranscriptService.finalizeUpload(
                                sessionId,
                                request,
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent"));
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{sessionId}/transcript")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @Operation(summary = "Get session transcript", description = "Returns the latest transcript and chunk metadata for a session.")
        public ResponseEntity<SessionTranscriptResponse> getTranscript(
                        @PathVariable("sessionId") Long sessionId,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                return ResponseEntity.ok(sessionTranscriptService.getSessionTranscript(sessionId, principal));
        }

        @GetMapping("/{sessionId}/transcript/download")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @Operation(summary = "Download transcript text file", description = "Downloads the finalized transcript as plain text attachment.")
        public ResponseEntity<String> downloadTranscript(
                        @PathVariable("sessionId") Long sessionId,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                String body = sessionTranscriptService.downloadSessionTranscriptText(sessionId, principal);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"session-transcript-" + sessionId + ".txt\"")
                                .contentType(MediaType.TEXT_PLAIN)
                                .body(body);
        }

        @DeleteMapping("/{sessionId}/transcript")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @Operation(summary = "Delete session transcript", description = "Soft-deletes transcript and related chunks for the session.")
        public ResponseEntity<Void> deleteTranscript(
                        @PathVariable("sessionId") Long sessionId,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                sessionTranscriptService.deleteSessionTranscript(
                                sessionId,
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent"));
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/{sessionId}/transcript/smart-fill")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
        @Operation(summary = "Smart-fill note fields from transcript", description = "Uses AI to map finalized transcript text into structured note fields.")
        public ResponseEntity<TranscriptSmartFillResponse> smartFillTranscript(
                        @PathVariable("sessionId") Long sessionId,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                return ResponseEntity.ok(sessionTranscriptService.smartFillFromTranscript(sessionId, principal));
        }

        @PostMapping("/{sessionId}/transcript/diarize")
        @PreAuthorize(StaffAuthorizationExpressions.SESSION_EDIT_ACCESS)
        @Operation(summary = "Identify speakers in transcript", description = """
                        Uses GPT-4o to re-label each turn as Therapist: or Client: based on conversational patterns.
                        Does not send the client name to OpenAI. Requires AI consent. Idempotent when already diarized.
                        Original finalTranscript is never modified.
                        """)
        public ResponseEntity<SessionTranscriptResponse> diarizeTranscript(
                        @PathVariable("sessionId") Long sessionId,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                return ResponseEntity.ok(sessionTranscriptService.diarizeTranscript(
                                sessionId,
                                principal,
                                HttpRequestUtil.getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent")));
        }

}
