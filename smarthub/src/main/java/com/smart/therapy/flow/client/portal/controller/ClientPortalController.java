package com.smart.therapy.flow.client.portal.controller;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginContextResponse;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.auth.dto.RefreshTokenRequest;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.security.AuthRefreshCookie;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.LoginRoutingService;
import com.smart.therapy.flow.client.portal.dto.*;
import com.smart.therapy.flow.client.portal.enums.SessionHistoryScope;
import com.smart.therapy.flow.client.portal.service.ClientPortalService;
import com.smart.therapy.flow.client.portal.service.PortalDocumentService;
import com.smart.therapy.flow.client.portal.service.PortalInvoiceService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.session.dto.RoomAvailabilityResponse;
import com.smart.therapy.flow.session.dto.SessionOverviewStatsResponse;
import com.smart.therapy.flow.session.dto.SessionSummaryResponse;
import com.smart.therapy.flow.session.service.RoomService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.user.dto.TherapistAvailabilityPublicResponse;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Portal", description = "Client portal endpoints for authenticated clients")
public class ClientPortalController {

    private final ClientPortalService portalService;
    private final PortalInvoiceService portalInvoiceService;
    private final PortalDocumentService portalDocumentService;
    private final com.smart.therapy.flow.assessment.service.AssessmentService assessmentService;
    private final RoomService roomService;
    private final TherapistAvailabilityService therapistAvailabilityService;
    private final CurrentUserService currentUserService;
    private final SessionService sessionService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final LoginRoutingService loginRoutingService;
    private final AuthenticationService authenticationService;
    private final AuthRefreshCookie refreshCookie;

    /** Require CLIENT_PORTAL plan feature for this organisation. */
    private void requireClientPortalEnabled() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !subscriptionFeatureService.isFeatureEnabled(orgId, SubscriptionFeatureService.FEATURE_CLIENT_PORTAL, null)) {
            throw new ForbiddenException("Client portal is not available on your plan. Please upgrade to enable the client portal.");
        }
    }

    @GetMapping("/login-context")
    @Operation(
            summary = "Resolve client portal login context",
            description = """
                    Resolve organisations for a portal email before login.
                    Only CLIENT identities are considered — staff memberships with the same email are excluded.
                    When multiple client organisations match, the response includes an organisations list for selection.
                    """
    )
    public ResponseEntity<LoginContextResponse> getPortalLoginContext(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String orgSlug
    ) {
        String loginIdentifier = firstNonBlank(identifier, email);
        return ResponseEntity.ok(
                loginRoutingService.resolveContext(loginIdentifier, orgSlug, IdentityType.CLIENT));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Client Portal Login",
            description = """
                    Authenticate a client and receive JWT tokens for client portal access.
                    
                    **Request Body Example:**
                    ```json
                    {
                      "email": "client@example.com",
                      "password": "ClientPassword123!"
                    }
                    ```
                    
                    **Response includes:**
                    - `accessToken`: JWT token for API authentication (expires in 24 hours)
                    - `refreshToken`: Token to refresh the access token
                    - `client`: Client profile information
                    
                    **Usage:**
                    Include the `accessToken` in the Authorization header for subsequent requests:
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    
                    **Note:** Client must have portal access enabled and a valid portal email.

                    **Simple login:** send only `email` and `password`. Tenant is resolved automatically from the client account.
                    Optional `orgSlug` / `orgId` is only needed when the same email exists in multiple organisations.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Client login credentials",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PortalLoginRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Client Login",
                                    value = """
                                            {
                                              "email": "client@example.com",
                                              "password": "ClientPassword123!"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "client": {
                                                "id": 123,
                                                "fullName": "John Doe",
                                                "email": "client@example.com",
                                                "portalEmail": "client@example.com"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or portal access not enabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error - email and password are required")
    })
    public ResponseEntity<PortalLoginResponse> login(
            @Valid @RequestBody PortalLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        requireClientPortalEnabled();
        PortalLoginResponse response = portalService.login(
                request, 
                HttpRequestUtil.getClientIp(httpRequest), 
                HttpRequestUtil.getUserAgent(httpRequest)
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify-login")
    @Operation(
            summary = "Verify portal MFA and complete login",
            description = """
                    Complete client portal login after password authentication when MFA is enabled
                    on the client's AuthIdentity. Uses the same MFA challenge token returned by
                    `/portal/login`. Enrollment still uses shared `/auth/mfa/*` endpoints.
                    """
    )
    public ResponseEntity<PortalLoginResponse> verifyMfaLogin(
            @Valid @RequestBody MfaDtos.LoginVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        requireClientPortalEnabled();
        PortalLoginResponse response = portalService.verifyMfaLogin(
                request,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh client portal access token",
            description = """
                    Refresh an expired client portal access token using a valid refresh token, taken
                    from the body or, when the body carries none, from the HttpOnly `tf_portal_refresh` cookie.

                    **Request Body Example:**
                    ```json
                    {
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    ```

                    **Response:** New access token and refresh token pair.
                    """
    )
    public ResponseEntity<JwtAuthenticationResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request
    ) {
        String refreshToken = refreshCookie.resolve(
                refreshTokenRequest != null ? refreshTokenRequest.getRefreshToken() : null,
                request,
                AuthRefreshCookie.Audience.PORTAL);
        JwtAuthenticationResponse response = authenticationService.refreshToken(
                new RefreshTokenRequest(refreshToken),
                HttpRequestUtil.getClientIp(request),
                HttpRequestUtil.getUserAgent(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current client profile",
            description = """
                    Get the authenticated client's profile information including timezone.
                    
                    Returns client details including:
                    - Basic info (id, clientId, fullName, email, phone)
                    - Assigned therapist ID
                    - Timezone (IANA timezone ID, e.g., "America/New_York", "Asia/Karachi")
                    
                    If timezone is null, it means the client's timezone is not configured.
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PortalClientResponse> getCurrentClient() {
        requireClientPortalEnabled();
        PortalClientResponse client = portalService.getCurrentClient();
        return ResponseEntity.ok(client);
    }

    @GetMapping("/me/sessions-history")
    @Operation(
            summary = "Get my session history",
            description = """
                    Returns the authenticated client's sessions filtered by scope.
                    Requires CLIENT role JWT token.
                    
                    **Query parameters:**
                    - `scope` (required): `upcoming` or `past`
                    - `timezone` (optional): IANA timezone; defaults to client profile timezone
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<SessionSummaryResponse>> getMySessionsHistory(
            @Parameter(description = "Session tab filter", required = true, example = "upcoming")
            @RequestParam String scope,
            @Parameter(description = "IANA timezone override (optional)", example = "America/New_York")
            @RequestParam(required = false) String timezone,
            @AuthenticationPrincipal AuthPrincipal principal) {
        requireClientPortalEnabled();
        SessionHistoryScope parsedScope = SessionHistoryScope.from(scope);
        return ResponseEntity.ok(sessionService.getCurrentClientSessionHistory(principal, parsedScope, timezone));
    }

    @GetMapping("/me/sessions-stats")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Get my session stats",
            description = """
                    Get current client's session dashboard counters:
                    today, this week, this month, upcoming, completed, cancelled, total.
                    Requires CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionOverviewStatsResponse> getMySessionStats(
            @RequestParam(required = false) String timezone,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireClientPortalEnabled();
        return ResponseEntity.ok(sessionService.getCurrentClientSessionOverview(timezone, principal));
    }

    @PostMapping("/me/sessions/{sessionId}/rating")
    @Operation(summary = "Rate my session", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> rateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody RateSessionRequest request,
            HttpServletRequest httpRequest) {
        requireClientPortalEnabled();
        return ResponseEntity.ok(portalService.rateSession(
                sessionId,
                request.getRating(),
                request.getComment(),
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)));
    }

    @PostMapping(value = "/me/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload my avatar",
            description = "Upload a profile avatar image (JPEG, PNG, GIF, WebP; max 5 MB). Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Avatar URL returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file type or size"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authenticated as a client")
    })
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = portalService.uploadAvatar(file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @GetMapping("/me/timezone")
    @Operation(
            summary = "Get my timezone",
            description = """
                    Get the current authenticated client's timezone setting.
                    
                    Returns the IANA timezone ID (e.g., "America/New_York", "Asia/Karachi").
                    If timezone is not set, returns null.
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Map<String, String>> getTimezone() {
        requireClientPortalEnabled();
        String timezone = portalService.getClientTimezone();
        return ResponseEntity.ok(Map.of("timezone", timezone != null ? timezone : "null"));
    }

    @PutMapping("/me/timezone")
    @Operation(
            summary = "Update my timezone",
            description = """
                    Update the current authenticated client's timezone setting.
                    
                    **Required Field:**
                    - `timezone`: IANA timezone ID (e.g., "America/New_York", "Asia/Karachi", "Europe/London")
                    
                    **Valid IANA Timezone Examples:**
                    - `America/New_York` (Eastern Time)
                    - `America/Chicago` (Central Time)
                    - `America/Denver` (Mountain Time)
                    - `America/Los_Angeles` (Pacific Time)
                    - `Europe/London` (UK Time)
                    - `Asia/Karachi` (Pakistan Time)
                    - `Asia/Dubai` (UAE Time)
                    - `Asia/Singapore` (Singapore Time)
                    
                    **Request Body Example:**
                    ```json
                    {
                      "timezone": "Asia/Karachi"
                    }
                    ```
                    
                    **Note:** The timezone is automatically updated when booking/rescheduling appointments if you include a timezone offset in the request. This endpoint allows you to set it explicitly.
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Timezone updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PortalClientResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid timezone format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<PortalClientResponse> updateTimezone(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Timezone to set",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Set Timezone",
                                    value = """
                                            {
                                              "timezone": "Asia/Karachi"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        requireClientPortalEnabled();
        String timezone = request.get("timezone");
        if (timezone == null || timezone.isBlank()) {
            throw new BadRequestException("Timezone is required");
        }
        
        // Validate timezone format
        try {
            java.time.ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BadRequestException("Invalid timezone format: " + timezone + ". Please use IANA timezone ID (e.g., 'America/New_York', 'Asia/Karachi')");
        }
        
        PortalClientResponse updated = portalService.updateClientTimezone(timezone, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout client",
            description = """
                    Invalidate the current JWT access token (and optionally refresh token).
                    
                    **Optional Fields:**
                    - `refreshToken` (optional): If provided, refresh token is also blacklisted
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional refresh token to revoke",
                    required = false,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Logout With Refresh Token",
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            )
    )
    public ResponseEntity<Object> logout(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        requireClientPortalEnabled();
        // Get JWT token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String jwtToken = (authHeader != null && authHeader.startsWith("Bearer ")) 
                ? authHeader 
                : null;

        String refreshToken = refreshCookie.resolve(
                request != null ? request.get("refreshToken") : null,
                httpRequest,
                AuthRefreshCookie.Audience.PORTAL);
        portalService.logout(jwtToken, refreshToken, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear(AuthRefreshCookie.Audience.PORTAL).toString())
                .body(new Object() {
            public final String message = "Logged out successfully";
        });
    }

    @GetMapping("/activate/validate")
    @Operation(summary = "Validate client portal activation token",
            description = "Check whether an activation token from email is still valid before showing the activation form.")
    public ResponseEntity<PortalActivationTokenValidationResponse> validateActivationToken(
            @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(portalService.validateActivationToken(token));
    }

    @PostMapping("/activate")
    @Operation(
            summary = "Activate client portal account",
            description = "Activate a client portal account using activation token and set password. Returns JWT tokens."
    )
    public ResponseEntity<PortalActivateResponse> activate(
            @Valid @RequestBody PortalActivateRequest request
    ) {
        PortalActivateResponse response = portalService.activate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = """
                    Request a password reset link to be sent to the client's email.
                    
                    **Required Fields:**
                    - `email` (REQUIRED): Client's portal email address

                    **Multi-org support (optional):** include `orgSlug` or `orgId` when the same email can exist across organisations.
                    
                    **Note:** Always returns success message to prevent email enumeration, even if email doesn't exist.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Password reset request",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ForgotPasswordRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Forgot Password",
                                    value = """
                                            {
                                              "email": "client@example.com"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "If an account exists with this email, a password reset link has been sent",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "message": "If an account exists with this email, a password reset link has been sent."
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error - email is required")
    })
    public ResponseEntity<Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        portalService.forgotPassword(request);
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(new Object() {
            public final String message = "If an account exists with this email, a password reset link has been sent.";
        });
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = """
                    Reset password using reset token from email.
                    
                    **Required Fields:**
                    - `token` (REQUIRED): Password reset token from email
                    - `password` (REQUIRED): New password (minimum 6 characters)
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Password reset information",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ResetPasswordRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Reset Password",
                                    value = """
                                            {
                                              "token": "xyz789abc123",
                                              "password": "NewPassword123!"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "message": "Password reset successfully. You can now log in with your new password."
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid/expired token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid reset token")
    })
    public ResponseEntity<Object> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        portalService.resetPassword(request);
        return ResponseEntity.ok(new Object() {
            public final String message = "Password reset successfully. You can now log in with your new password.";
        });
    }

    // ========== APPOINTMENTS ENDPOINT ==========

    @GetMapping("/appointments")
    @Operation(
            summary = "Get client appointments",
            description = """
                    Get paginated appointments for the authenticated client. Ordered by session date descending.
                    Optional `status` filters by session status (scheduled, confirmed, in_progress, completed, cancelled, etc.).
                    Requires CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<com.smart.therapy.flow.client.portal.dto.PortalAppointmentResponse>> getAppointments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest
    ) {
        PaginatedResponse<com.smart.therapy.flow.client.portal.dto.PortalAppointmentResponse> appointments =
                portalService.getAppointments(
                        page,
                        pageSize,
                        status,
                        HttpRequestUtil.getClientIp(httpRequest),
                        HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(appointments);
    }

    // ========== NOTIFICATIONS ENDPOINT ==========
    // NOTE: Kept only for backward compatibility.
    // The primary portal notifications API lives in:
    //   com.smart.therapy.flow.notification.controller.ClientPortalNotificationController
    // at /api/v1/portal/notifications
    @Deprecated
    @GetMapping("/notifications/legacy")
    @Operation(
            summary = "Get client notifications (legacy)",
            description = "LEGACY endpoint. Prefer /api/v1/portal/notifications (ClientPortalNotificationController).",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<com.smart.therapy.flow.notification.dto.NotificationResponse>> getNotifications(
            HttpServletRequest httpRequest
    ) {
        List<com.smart.therapy.flow.notification.dto.NotificationResponse> notifications = 
                portalService.getNotifications(HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(notifications);
    }

    // ========== SERVICES ENDPOINT ==========

    @GetMapping("/services")
    @Operation(
            summary = "Get available services",
            description = "Get all services available in the client portal. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<com.smart.therapy.flow.client.portal.dto.PortalServiceResponse>> getServices() {
        List<com.smart.therapy.flow.client.portal.dto.PortalServiceResponse> services = 
                portalService.getServices();
        return ResponseEntity.ok(services);
    }

    // ========== AVAILABLE SLOTS ENDPOINT ==========

    @GetMapping("/available-slots")
    @Operation(
            summary = "Get available appointment slots",
            description = "Get available appointment slots for booking within a date range. " +
                    "Dates must be in ISO 8601 format (yyyy-MM-dd). " +
                    "Example: startDate=2025-01-15, endDate=2025-01-20. " +
                    "Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse> getAvailableSlots(
            @Parameter(
                    description = "Start date in ISO 8601 format (yyyy-MM-dd). Example: 2025-01-15",
                    required = true,
                    example = "2025-01-15"
            )
            @RequestParam("startDate") String startDate,
            @Parameter(
                    description = "End date in ISO 8601 format (yyyy-MM-dd). Example: 2025-01-20",
                    required = true,
                    example = "2025-01-20"
            )
            @RequestParam("endDate") String endDate,
            @Parameter(
                    description = "Session type. Must be either 'online' or 'in-person'",
                    required = true,
                    example = "online"
            )
            @RequestParam("sessionType") String sessionType,
            @Parameter(
                    description = "Service ID used to determine slot duration (same as admin/therapist booking)",
                    required = true,
                    example = "1"
            )
            @RequestParam("serviceId") Long serviceId
    ) {
        com.smart.therapy.flow.client.portal.dto.AvailableSlotResponse response = 
                portalService.getAvailableSlots(startDate, endDate, sessionType, serviceId);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(response);
    }

    // ========== BOOK APPOINTMENT ENDPOINT ==========

    @PostMapping("/book-appointment")
    @Operation(
            summary = "Book an appointment",
            description = """
                    Book a new appointment/session.
                    
                    **Required Fields:**
                    - `sessionStartUtc` (REQUIRED): Session start time in UTC (ISO 8601 format)
                    - `serviceId` (REQUIRED): ID of the billing service
                    - `sessionType` (REQUIRED): Type of session - "online" (ONLINE) or "in-person" (IN_PERSON)
                    
                    **Flow:**
                    1. Client selects session type (enum), date, time, and service
                    2. System automatically checks room availability if session type is IN_PERSON
                    3. If session type is ONLINE, system creates Zoom meeting and sends invitations
                    4. Notifications are sent to: Client, Therapist, Admin, and Supervisor
                    
                    **Optional Fields:**
                    - `duration` (optional): Session duration in minutes (will use service duration if not provided)
                    - `location` (optional): Location notes for in-person sessions
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Appointment booking information",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BookAppointmentRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Book Online Appointment",
                                    value = """
                                            {
                                              "sessionStartUtc": "2025-12-25T14:00:00Z",
                                              "serviceId": 1,
                                              "sessionType": "online",
                                              "duration": 60
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Appointment booked successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.smart.therapy.flow.client.portal.dto.BookAppointmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error, slot not available, or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.BookAppointmentResponse> bookAppointment(
            @Valid @RequestBody com.smart.therapy.flow.client.portal.dto.BookAppointmentRequest request,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.BookAppointmentResponse response = 
                portalService.bookAppointment(request, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/online-booking-request")
    @Operation(
            summary = "Ask the assigned therapist to enable online sessions",
            description = "Notifies the client's own therapist that online booking is unavailable because "
                    + "their Zoom account is not connected. The therapist is resolved server-side, the client "
                    + "sends no message content, and repeat calls inside the 7-day cooldown are a no-op. "
                    + "Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.OnlineBookingRequestResponse> requestOnlineBooking(
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(portalService.requestOnlineBooking(
                HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest)));
    }

    @GetMapping("/appointments/{id}")
    @Operation(
            summary = "Get single appointment",
            description = "Get detailed information about a specific appointment. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PortalAppointmentResponse> getAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.PortalAppointmentResponse appointment = 
                portalService.getAppointment(id, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(appointment);
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(
            summary = "Cancel an appointment",
            description = "Cancel an existing appointment. Cannot cancel completed appointments. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.CancelAppointmentResponse> cancelAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.CancelAppointmentResponse response = 
                portalService.cancelAppointment(id, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/appointments/{id}/reschedule")
    @Operation(
            summary = "Reschedule an appointment",
            description = "Reschedule an existing appointment to a new date/time. Cannot reschedule cancelled or completed appointments. New time must be in the future. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.RescheduleAppointmentResponse> rescheduleAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody com.smart.therapy.flow.client.portal.dto.RescheduleAppointmentRequest request,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.RescheduleAppointmentResponse response = 
                portalService.rescheduleAppointment(id, request, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    // ========== INVOICE ENDPOINTS ==========

    @GetMapping("/invoices/stats")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Get client invoice overview stats",
            description = "Unfiltered totals for the authenticated client (total invoices, billed, paid).",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PortalInvoiceStatsResponse> getInvoiceStats(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(portalInvoiceService.getInvoiceStats(principal));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Get client invoices",
            description = """
                    Paginated invoice list for the authenticated client.
                    Filters: paymentStatus, insuranceCovered, startDate, endDate, search.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<PortalInvoiceResponse>> getInvoices(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Boolean insuranceCovered,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest
    ) {
        PaginatedResponse<PortalInvoiceResponse> invoices = portalInvoiceService.getInvoices(
                principal,
                page,
                pageSize,
                paymentStatus,
                insuranceCovered,
                startDate,
                endDate,
                search,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoices/{invoiceId}/receipt")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Download invoice receipt PDF",
            description = "Download receipt PDF for a paid or partially paid invoice belonging to the authenticated client.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<byte[]> downloadInvoiceReceipt(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("invoiceId") Long invoiceId,
            HttpServletRequest httpRequest
    ) {
        byte[] pdf = portalInvoiceService.downloadInvoiceReceipt(
                principal,
                invoiceId,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-receipt-" + invoiceId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/invoices/{invoiceId}/receipt-html")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Open invoice receipt HTML for print",
            description = "Returns printable invoice HTML for a paid or partially paid invoice belonging to the authenticated client.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<String> downloadInvoiceReceiptHtml(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("invoiceId") Long invoiceId,
            HttpServletRequest httpRequest
    ) {
        String html = portalInvoiceService.downloadInvoiceReceiptHtml(
                principal,
                invoiceId,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invoice-receipt-" + invoiceId + ".html\"");
        return ResponseEntity.ok().headers(headers).body(html);
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Pay an invoice",
            description = "Process payment for an invoice. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PayInvoiceResponse> payInvoice(
            @PathVariable("invoiceId") Long invoiceId,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.PayInvoiceResponse response = 
                portalService.payInvoice(invoiceId, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(response);
    }

    // ========== DOCUMENTS ENDPOINTS ==========

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Get client documents",
            description = """
                    Paginated list of documents visible in the client portal
                    (client uploads and staff-shared documents).
                    Filters: documentType, category, search.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<com.smart.therapy.flow.document.dto.DocumentResponse>> getDocuments(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest
    ) {
        PaginatedResponse<com.smart.therapy.flow.document.dto.DocumentResponse> documents =
                portalDocumentService.getDocuments(
                        principal,
                        page,
                        pageSize,
                        documentType,
                        category,
                        search,
                        HttpRequestUtil.getClientIp(httpRequest),
                        HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(documents);
    }

    @PostMapping(value = "/upload-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Upload a document",
            description = "Upload a document file. Maximum file size is 50MB. Use multipart/form-data content type. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.document.dto.DocumentResponse> uploadDocument(
            @Parameter(description = "The file to upload (multipart/form-data)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional document type (e.g., intake_form, consent, insurance_card, id_document, medical_record, prescription, lab_result, referral_letter)")
            @RequestParam(required = false) String documentType,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.document.dto.DocumentResponse document = 
                portalService.uploadDocument(file, documentType, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.status(201).body(document);
    }

    @GetMapping("/documents/{id}/view")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "View a document",
            description = "View a document file in the browser (inline). Supports images, PDFs, and other viewable formats. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<org.springframework.core.io.Resource> viewDocument(
            @PathVariable("id") Long documentId,
            HttpServletRequest httpRequest
    ) {
        ClientPortalService.DocumentViewResult viewResult =
                portalService.viewDocument(documentId, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));

        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.parseMediaType(
                viewResult.getMimeType() != null ? viewResult.getMimeType() : "application/octet-stream");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(viewResult.getContent().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + viewResult.getFileName() + "\"")
                .body(new org.springframework.core.io.ByteArrayResource(viewResult.getContent()));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Download a document",
            description = "Download a document file. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(
            @PathVariable("id") Long documentId,
            HttpServletRequest httpRequest
    ) {
        ClientPortalService.DocumentDownloadResult downloadResult =
                portalService.downloadDocument(documentId, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));

        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.parseMediaType(
                downloadResult.getMimeType() != null ? downloadResult.getMimeType() : "application/octet-stream");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(downloadResult.getContent().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadResult.getFileName() + "\"")
                .body(new org.springframework.core.io.ByteArrayResource(downloadResult.getContent()));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Delete a document",
            description = "Delete a document the client uploaded in the portal. Staff-shared documents cannot be deleted by the client.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.common.dto.SuccessResponse> deleteDocument(
            @PathVariable("id") Long documentId,
            HttpServletRequest httpRequest
    ) {
        portalService.deleteDocument(
                documentId,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(
                com.smart.therapy.flow.common.dto.SuccessResponse.of("Document deleted successfully"));
    }

    // ========== FORMS ENDPOINTS ==========

    @GetMapping("/forms/assignments")
    @Operation(
            summary = "Get form assignments",
            description = "Get all form assignments for the authenticated client. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<com.smart.therapy.flow.document.dto.FormAssignmentResponse>> getFormAssignments(
            HttpServletRequest httpRequest
    ) {
        List<com.smart.therapy.flow.document.dto.FormAssignmentResponse> assignments = 
                portalService.getFormAssignments(HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/forms/assignments/{id}")
    @Operation(
            summary = "Get form assignment details",
            description = "Get details of a specific form assignment. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Map<String, Object>> getFormAssignment(
            @PathVariable("id") Long assignmentId,
            HttpServletRequest httpRequest
    ) {
        Map<String, Object> assignment = 
                portalService.getFormAssignment(assignmentId, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/forms/responses/{assignmentId}")
    @Operation(
            summary = "Get form responses",
            description = "Get all responses for a form assignment. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<com.smart.therapy.flow.document.dto.FormResponseDto>> getFormResponses(
            @PathVariable("assignmentId") Long assignmentId
    ) {
        List<com.smart.therapy.flow.document.dto.FormResponseDto> responses = 
                portalService.getFormResponses(assignmentId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/forms/responses")
    @Operation(
            summary = "Save form response",
            description = "Save a response to a form field. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.document.dto.FormResponseDto> saveFormResponse(
            @RequestBody Map<String, Object> request
    ) {
        // Validate and extract assignmentId
        if (request.get("assignmentId") == null) {
            throw new BadRequestException("assignmentId is required");
        }
        Long assignmentId;
        try {
            assignmentId = Long.valueOf(request.get("assignmentId").toString());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BadRequestException("Invalid assignmentId format");
        }
        
        // Validate and extract assignmentFieldId (preferred) or fieldId (for backward compatibility)
        Long assignmentFieldId = null;
        if (request.get("assignmentFieldId") != null) {
            try {
                assignmentFieldId = Long.valueOf(request.get("assignmentFieldId").toString());
            } catch (NumberFormatException | NullPointerException e) {
                throw new BadRequestException("Invalid assignmentFieldId format");
            }
        } else if (request.get("fieldId") != null) {
            try {
                Long fieldId = Long.valueOf(request.get("fieldId").toString());
                assignmentFieldId = portalService.resolveAssignmentFieldId(assignmentId, fieldId);
            } catch (NumberFormatException | NullPointerException e) {
                throw new BadRequestException("Invalid fieldId format");
            }
        } else {
            throw new BadRequestException("assignmentFieldId is required");
        }
        
        String value = request.get("value") != null ? request.get("value").toString() : null;
        com.smart.therapy.flow.document.dto.FormResponseDto response = 
                portalService.saveFormResponse(assignmentId, assignmentFieldId, value);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/forms/signature/{assignmentId}")
    @Operation(
            summary = "Get form signature",
            description = "Get the signature for a form assignment. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.document.dto.FormSignatureResponse> getFormSignature(
            @PathVariable("assignmentId") Long assignmentId
    ) {
        com.smart.therapy.flow.document.dto.FormSignatureResponse signature = 
                portalService.getFormSignature(assignmentId);
        return ResponseEntity.ok(signature);
    }

    @GetMapping("/forms/signature/{assignmentId}/image")
    @Operation(
            summary = "Get form signature image",
            description = "Get decoded signature image bytes for preview rendering. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<byte[]> getFormSignatureImage(
            @PathVariable("assignmentId") Long assignmentId
    ) {
        ClientPortalService.SignatureImageResult image = portalService.getFormSignatureImage(assignmentId);
        MediaType mediaType = MediaType.parseMediaType(
                image.getMimeType() != null ? image.getMimeType() : "image/png");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(image.getContent());
    }

    @PostMapping("/forms/signature")
    @Operation(
            summary = "Save form signature",
            description = "Save a signature for a form assignment. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.document.dto.FormSignatureResponse> saveFormSignature(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        // Validate and extract assignmentId
        if (request.get("assignmentId") == null) {
            throw new BadRequestException("assignmentId is required");
        }
        Long assignmentId;
        try {
            assignmentId = Long.valueOf(request.get("assignmentId").toString());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BadRequestException("Invalid assignmentId format");
        }
        
        String signatureData = request.get("signatureData") != null ? request.get("signatureData").toString() : null;
        // ClientHub-compatible callers omit this flag: signing itself denotes acceptance.
        // An explicitly supplied value must be a boolean and must never be silently treated as true.
        Boolean agreedToTerms = Boolean.TRUE;
        if (request.containsKey("agreedToTerms")) {
            if (!(request.get("agreedToTerms") instanceof Boolean agreed)) {
                throw new BadRequestException("agreedToTerms must be a boolean");
            }
            agreedToTerms = agreed;
        }
        com.smart.therapy.flow.document.dto.FormSignatureResponse signature =
                portalService.saveFormSignature(assignmentId, signatureData, agreedToTerms,
                        HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(signature);
    }

    @PostMapping("/forms/submit/{assignmentId}")
    @Operation(
            summary = "Submit form",
            description = "Submit a completed form assignment. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.document.dto.FormAssignmentResponse> submitForm(
            @PathVariable("assignmentId") Long assignmentId,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.document.dto.FormAssignmentResponse assignment = 
                portalService.submitForm(assignmentId, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(assignment);
    }

    // ========== CONSENTS ENDPOINTS ==========

    @GetMapping("/consents")
    @Operation(
            summary = "Get client consents",
            description = "Get all consents for the authenticated client. Requires CLIENT role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse>> getConsents() {
        List<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> consents = 
                portalService.getConsents();
        return ResponseEntity.ok(consents);
    }

    @PostMapping("/consents")
    @Operation(
            summary = "Grant consent",
            description = """
                    Grant consent for a specific consent type.
                    
                    **Required Fields:**
                    - `consentType` (REQUIRED): Type of consent (treatment, privacy, billing, communication)
                    - `granted` (REQUIRED): Whether consent is granted (true/false)
                    - `consentVersion` (REQUIRED): Version of the consent document
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Consent information",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GrantConsentRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Grant Treatment Consent",
                                    value = """
                                            {
                                              "consentType": "treatment",
                                              "granted": true,
                                              "consentVersion": "1.0"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Consent granted successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.smart.therapy.flow.client.portal.dto.PortalConsentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> grantConsent(
            @Valid @RequestBody com.smart.therapy.flow.client.portal.dto.GrantConsentRequest request,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.PortalConsentResponse consent = 
                portalService.grantConsent(request, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(consent);
    }

    @PostMapping("/consents/withdraw")
    @Operation(
            summary = "Withdraw consent",
            description = """
                    Withdraw a previously granted consent.
                    
                    **Required Fields:**
                    - `consentType` (REQUIRED): Type of consent to withdraw (treatment, privacy, billing, communication)
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Consent withdrawal information",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = WithdrawConsentRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Withdraw Consent",
                                    value = """
                                            {
                                              "consentType": "treatment"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Consent withdrawn successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.smart.therapy.flow.client.portal.dto.PortalConsentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or consent not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> withdrawConsent(
            @Valid @RequestBody com.smart.therapy.flow.client.portal.dto.WithdrawConsentRequest request,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.client.portal.dto.PortalConsentResponse consent = 
                portalService.withdrawConsent(request, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(consent);
    }

    @PutMapping("/consents/toggle")
    @Operation(
            summary = "Toggle consent (grant or deny)",
            description = """
                    Toggle consent for a specific consent type. This is a simplified endpoint for frontend toggle buttons.
                    
                    **Required Fields:**
                    - `consentType` (REQUIRED): Type of consent enum (e.g., "AI_PROCESSING", "DATA_SHARING", "RESEARCH", "MARKETING")
                    - `granted` (REQUIRED): true to grant consent, false to deny/withdraw consent
                    
                    **Optional Fields:**
                    - `consentVersion` (optional): Version of the consent document (defaults to "1.0" if not provided)
                    
                    **Requires:** CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Consent toggle information",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.smart.therapy.flow.client.portal.dto.ToggleConsentRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Grant AI Processing Consent",
                                    value = """
                                            {
                                              "consentType": "AI_PROCESSING",
                                              "granted": true,
                                              "consentVersion": "1.0"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Consent toggled successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.smart.therapy.flow.client.portal.dto.PortalConsentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> toggleConsent(
            @Valid @RequestBody com.smart.therapy.flow.client.portal.dto.ToggleConsentRequest request,
            HttpServletRequest httpRequest
    ) {
        // Convert toggle request to grant consent request
        // The service expects the display name string, so convert enum to display name
        com.smart.therapy.flow.client.portal.dto.GrantConsentRequest grantRequest = 
                new com.smart.therapy.flow.client.portal.dto.GrantConsentRequest();
        grantRequest.setConsentType(request.getConsentType() != null ? request.getConsentType().getDisplayName() : null);
        grantRequest.setGranted(request.getGranted());
        grantRequest.setConsentVersion(request.getConsentVersion() != null ? request.getConsentVersion() : "1.0");
        
        com.smart.therapy.flow.client.portal.dto.PortalConsentResponse consent = 
                portalService.grantConsent(grantRequest, HttpRequestUtil.getClientIp(httpRequest), HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok(consent);
    }

    // Note: Invoice endpoints are handled by StripeController for payment
    // Additional invoice viewing endpoints can be added here if needed

    // ========== ASSESSMENTS ==========

    @GetMapping("/assessments")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Get client assessments",
            description = "Retrieves all assessment assignments for the authenticated client."
    )
    public ResponseEntity<List<com.smart.therapy.flow.assessment.dto.AssessmentAssignmentResponse>> getAssessments(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        List<com.smart.therapy.flow.assessment.dto.AssessmentAssignmentResponse> assessments =
                assessmentService.getClientAssignments(clientId);
        return ResponseEntity.ok(assessments);
    }

    @GetMapping("/assessments/{assignmentId}")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Get assessment assignment",
            description = "Retrieves a specific assessment assignment for the authenticated client."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.dto.AssessmentAssignmentResponse> getAssessment(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        com.smart.therapy.flow.assessment.dto.AssessmentAssignmentResponse assessment =
                assessmentService.getClientAssignment(assignmentId, clientId);
        return ResponseEntity.ok(assessment);
    }

    @PostMapping("/assessments/{assignmentId}/responses")
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    @Operation(
            summary = "Submit assessment responses",
            description = "Submits responses for an assessment assignment. Can submit single or multiple responses."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.dto.AssessmentAssignmentResponse> submitAssessmentResponse(
            @PathVariable("assignmentId") Long assignmentId,
            @Valid @RequestBody com.smart.therapy.flow.assessment.dto.SubmitAssessmentResponseRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        request.setAssignmentId(assignmentId);
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        com.smart.therapy.flow.assessment.dto.AssessmentAssignmentResponse assignment =
                assessmentService.submitClientResponse(
                        assignmentId,
                        clientId,
                        request,
                        HttpRequestUtil.getClientIp(httpRequest)
                );
        return ResponseEntity.ok(assignment);
    }

    // ========== ROOM AND THERAPIST AVAILABILITY ENDPOINTS ==========

    @GetMapping("/rooms/availability")
    @Operation(
            summary = "Get room availability",
            description = """
                    Get availability for all active rooms on a specific date.
                    Requires CLIENT role JWT token.
                    
                    **Query Parameters:**
                    - `date` (REQUIRED): Date in ISO format (yyyy-MM-dd), e.g., "2025-01-25"
                    
                    **Returns:** List of rooms with their availability status and time slots
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    // Note: Public room availability - clients can view but not book (booking requires additional permissions)
    public ResponseEntity<RoomAvailabilityResponse> getRoomsAvailability(
            @Parameter(description = "Date in ISO format yyyy-MM-dd (REQUIRED)", required = true, example = "2025-01-25")
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date
    ) {
        RoomAvailabilityResponse response = roomService.getPublicRoomsAvailability(date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/available-slot")
    @Operation(
            summary = "Get available rooms for selected slot",
            description = """
                    Returns therapist room options for selected exact slot.
                    Requires CLIENT role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    public ResponseEntity<List<com.smart.therapy.flow.session.dto.RoomResponse>> getAvailableRoomsForSlot(
            @RequestParam("therapistId") Long therapistId,
            @RequestParam("sessionDate") java.time.Instant sessionDate,
            @RequestParam("sessionType") String sessionType,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "duration", required = false) Integer duration
    ) {
        List<com.smart.therapy.flow.session.dto.RoomResponse> rooms = roomService.getAvailableRoomsForSlot(
                therapistId, sessionDate, duration, serviceId, sessionType);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/therapists/{therapistId}/availability")
    @Operation(
            summary = "Get therapist availability",
            description = """
                    Get availability for a specific therapist on a specific date.
                    Requires CLIENT role JWT token.
                    
                    **Path Parameters:**
                    - `therapistId` (REQUIRED): ID of the therapist (User entity)
                    
                     **Query Parameters:**
                     - `date` (REQUIRED): Date in ISO format (yyyy-MM-dd), e.g., "2025-01-25"
                     - `serviceId` (REQUIRED): ID of the service to check availability for
                     - `timezone` (optional): Client's timezone (IANA timezone ID) for time conversion
                     - `sessionType` (optional): `online` or `in-person`
                    
                    **Returns:** Therapist availability with available time slots
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PreAuthorize("hasAuthority('CLIENT_PORTAL_ACCESS')")
    // Note: Public therapist availability - clients can view but not book (booking requires additional permissions)
    public ResponseEntity<TherapistAvailabilityPublicResponse> getTherapistAvailability(
            @Parameter(description = "Therapist ID (REQUIRED) - User entity ID", required = true, example = "1")
            @PathVariable("therapistId") Long therapistId,
            @Parameter(description = "Date in ISO format yyyy-MM-dd (REQUIRED)", required = true, example = "2025-01-25")
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @Parameter(description = "Service ID (REQUIRED)", required = true, example = "1")
            @RequestParam("serviceId") Long serviceId,
            @Parameter(description = "Client timezone (optional, IANA timezone ID)", example = "America/New_York")
            @RequestParam(required = false) String timezone,
            @Parameter(description = "Session type (optional): online or in-person", example = "in-person")
            @RequestParam(value = "sessionType", required = false) String sessionType
    ) {
        String resolvedTimezone = timezone;
        if (resolvedTimezone == null || resolvedTimezone.isBlank()) {
            resolvedTimezone = portalService.getClientTimezone();
        }
        TherapistAvailabilityPublicResponse response = therapistAvailabilityService.getPublicTherapistAvailability(
                therapistId, date, serviceId, resolvedTimezone, sessionType);
        return ResponseEntity.ok(response);
    }
}

