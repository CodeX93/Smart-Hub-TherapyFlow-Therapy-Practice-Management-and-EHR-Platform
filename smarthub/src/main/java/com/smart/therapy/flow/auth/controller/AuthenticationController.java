package com.smart.therapy.flow.auth.controller;

import com.smart.therapy.flow.auth.dto.ChangePasswordRequest;
import com.smart.therapy.flow.auth.dto.AuthMeResponse;
import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.dto.LoginContextResponse;
import com.smart.therapy.flow.auth.dto.LogoutRequest;
import com.smart.therapy.flow.auth.dto.RefreshTokenRequest;
import com.smart.therapy.flow.auth.dto.PasswordResetTokenValidationResponse;
import com.smart.therapy.flow.auth.dto.StaffForgotPasswordRequest;
import com.smart.therapy.flow.auth.dto.StaffResetPasswordRequest;
import com.smart.therapy.flow.auth.dto.TenantResolveRequest;
import com.smart.therapy.flow.auth.dto.TenantResolveResponse;
import com.smart.therapy.flow.auth.service.AuthMeService;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.LoginRoutingService;
import com.smart.therapy.flow.auth.security.AuthRefreshCookie;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public authentication endpoints for staff/admin/therapist. No authentication required for login/refresh.")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final TenantResolutionService tenantResolutionService;
    private final LoginRoutingService loginRoutingService;
    private final AuthMeService authMeService;
    private final AuthRefreshCookie refreshCookie;

    @PostMapping("/login")
    @Operation(
            summary = "Staff/Admin/Therapist Login",
            description = """
                    Authenticate a staff member, admin, or therapist and receive JWT tokens.
                    
                    **Request Body Example:**
                    ```json
                    {
                      "username": "admin",
                      "password": "SecurePassword123!"
                    }
                    ```
                    
                    **Response includes:**
                    - `accessToken`: JWT token for API authentication (expires in 24 hours)
                    - `refreshToken`: Token to refresh the access token
                    - `user`: User profile information
                    - `roles`: User role names
                    - `permissions`: User permissions
                    
                    **Usage:**
                    Include the `accessToken` in the Authorization header for subsequent requests:
                    ```
                    Authorization: Bearer <accessToken>
                    ```
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "Admin Login",
                                    value = """
                                            {
                                              "username": "admin",
                                              "password": "SecurePassword123!"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthenticationResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "user": {
                                                "id": 1,
                                                "username": "admin",
                                                "email": "admin@therapyflow.pro",
                                                "fullName": "Admin User"
                                              },
                                              "roles": ["ADMIN"],
                                              "permissions": ["USER_VIEW", "USER_CREATE"]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Validation error - username and password are required")
    })
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        String ipAddress = HttpRequestUtil.getClientIp(request);
        JwtAuthenticationResponse response = authenticationService.authenticateUser(
                loginRequest, ipAddress, HttpRequestUtil.getUserAgent(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/login-context")
    @Operation(
            summary = "Resolve login context",
            description = "Resolve org by email/username (identifier) or orgSlug and return branding + SSO options for login page."
    )
    public ResponseEntity<LoginContextResponse> getLoginContext(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String orgSlug
    ) {
        String loginIdentifier = firstNonBlank(identifier, email, username);
        return ResponseEntity.ok(loginRoutingService.resolveContext(loginIdentifier, orgSlug));
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

    @PostMapping("/resolve-tenant")
    @Operation(
            summary = "Resolve tenant by email (pre-login)",
            description = "Returns organisations associated with an email to allow tenant selection before login."
    )
    public ResponseEntity<TenantResolveResponse> resolveTenant(
            @Valid @RequestBody TenantResolveRequest request
    ) {
        var rows = tenantResolutionService.resolveByEmail(request.getEmail(), IdentityType.STAFF);
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var items = rows.stream()
                .map(r -> TenantResolveResponse.TenantResolveItem.builder()
                        .organisationId(r.getOrganisation().getId())
                        .name(r.getOrganisation().getName())
                        .slug(r.getOrganisation().getSlug())
                        .subdomain(r.getOrganisation().getSubdomain())
                        .status(r.getOrganisation().getStatus())
                        .build())
                .toList();
        TenantResolveResponse body = TenantResolveResponse.builder()
                .email(request.getEmail())
                .organisations(items)
                .count(items.size())
                .build();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Access Token",
            description = """
                    Refresh an expired access token using a valid refresh token, taken from the
                    body or, when the body carries none, from the HttpOnly `tf_refresh` cookie.
                    
                    **Request Body Example:**
                    ```json
                    {
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    ```
                    
                    **Response:** New access token and refresh token pair.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token request (optional when the tf_refresh cookie is sent)",
                    required = false,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(
                                    name = "Refresh Token",
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<JwtAuthenticationResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        String refreshToken = refreshCookie.resolve(
                refreshTokenRequest != null ? refreshTokenRequest.getRefreshToken() : null,
                request,
                AuthRefreshCookie.Audience.STAFF);
        JwtAuthenticationResponse response = authenticationService.refreshToken(
                new RefreshTokenRequest(refreshToken),
                HttpRequestUtil.getClientIp(request),
                HttpRequestUtil.getUserAgent(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout User",
            description = """
                    Invalidate the current JWT access token (and optionally refresh token).
                    
                    **Optional Request Body:**
                    ```json
                    {
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    ```
                    
                    If refreshToken is provided (or the tf_refresh cookie is sent), both tokens will be
                    blacklisted and the cookie is cleared.
                    If not provided, only the access token from Authorization header will be blacklisted.
                    
                    **Requires:** Valid JWT token in Authorization header.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Object> logout(
            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletRequest request) {
        String refreshToken = refreshCookie.resolve(
                logoutRequest != null ? logoutRequest.getRefreshToken() : null,
                request,
                AuthRefreshCookie.Audience.STAFF);
        Long authId = userPrincipal != null ? userPrincipal.getAuthId() : null;
        if (StringUtils.hasText(refreshToken)) {
            // Holding the refresh token is proof enough to revoke it, so this also runs
            // when the access token has already expired.
            authenticationService.logout(token, refreshToken, authId);
        } else if (authId != null) {
            authenticationService.logout(token, authId);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear(AuthRefreshCookie.Audience.STAFF).toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get Current User",
            description = """
                    Get the currently authenticated user's profile information.
                    
                    **Requires:** Valid JWT token in Authorization header.
                    
                    **Response includes:**
                    - User ID, username, email
                    - User roles and authorities
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Info",
                                    value = """
                                            {
                                              "authId": 169,
                                              "username": "169",
                                              "email": "platform.superadmin@therapyflowseed.com",
                                              "identityType": "STAFF",
                                              "tenantSchema": "public",
                                              "organisationId": null,
                                              "roles": ["PLATFORM_SUPER_ADMIN"],
                                              "permissions": ["USER_VIEW", "SESSION_EDIT"],
                                              "authorities": ["ROLE_PLATFORM_SUPER_ADMIN", "USER_VIEW", "SESSION_EDIT"],
                                              "isPlatformAdmin": true,
                                              "isTenantAdmin": false,
                                              "isTherapist": false,
                                              "isSupervisor": false,
                                              "isClient": false,
                                              "user": {
                                                "id": 12,
                                                "username": "platform.superadmin@therapyflowseed.com",
                                                "fullName": "Platform Super Admin",
                                                "email": "platform.superadmin@therapyflowseed.com",
                                                "roles": ["PLATFORM_SUPER_ADMIN"]
                                              },
                                              "client": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<AuthMeResponse> getCurrentUser(@AuthenticationPrincipal AuthPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authMeService.getCurrentUserContext(userPrincipal));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Staff Forgot Password",
            description = """
                    Request a password reset email for a staff account (admin/therapist/supervisor).
                    If the email is registered, a reset link is sent. The response is always success to avoid revealing account existence.
                    
                    **Request Body:**
                    ```json
                    { "email": "therapist@therapyflow.com" }
                    ```

                    **Multi-org support (optional):** include `orgSlug` or `orgId` when the same email can exist across organisations.
                    
                    **Flow:** User receives email with a link; the link opens the staff login page with a reset token.
                    The frontend then calls `POST /api/v1/auth/reset-password` with `token` and `newPassword`.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Staff email address",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StaffForgotPasswordRequest.class)
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "If the email is registered, a reset link was sent. Check your inbox."),
            @ApiResponse(responseCode = "400", description = "Validation error - email required and must be valid")
    })
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody StaffForgotPasswordRequest request) {
        String hint = authenticationService.requestPasswordResetStaff(
                request.getEmail(),
                request.getOrgId(),
                request.getOrgSlug(),
                request.getOrgIdentifier(),
                request.getOrgValue()
        );
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (StringUtils.hasText(hint)) {
            builder.header("X-Auth-Hint", hint);
        }
        return builder.body(Map.of(
                "message", "If an account exists for this email, you will receive a password reset link.",
                "status", "success"
        ));
    }

    @GetMapping("/reset-password/validate")
    @Operation(
            summary = "Validate password reset token",
            description = """
                    Check whether a reset token from email is still valid before showing the set-new-password form.
                    Returns account type (`staff` or `client`) so the frontend can call the correct reset endpoint.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation result (valid true or false)"),
            @ApiResponse(responseCode = "400", description = "Token query parameter missing")
    })
    public ResponseEntity<PasswordResetTokenValidationResponse> validateResetPasswordToken(
            @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(authenticationService.validatePasswordResetToken(token));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Staff Reset Password",
            description = """
                    Reset staff password using the token received by email from forgot-password.
                    
                    **Request Body:**
                    ```json
                    {
                      "token": "abc123xyz789",
                      "newPassword": "NewSecurePassword123!"
                    }
                    ```
                    
                    **Requires:** Valid reset token from the forgot-password email (expires in 24 hours).
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Reset token and new password",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StaffResetPasswordRequest.class)
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully. You can now login with the new password."),
            @ApiResponse(responseCode = "400", description = "Validation error - token and newPassword required, password min 6 characters"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired reset token")
    })
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody StaffResetPasswordRequest request) {
        authenticationService.resetPasswordStaff(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "message", "Password has been reset successfully. You can now login with your new password.",
                "status", "success"
        ));
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Change Password",
            description = """
                    Change user password. Can be used for:
                    - First login password change (requires changePasswordToken from login response)
                    - Regular password change (requires current password)
                    
                    **For First Login:**
                    ```json
                    {
                      "changePasswordToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "newPassword": "NewSecurePassword123!"
                    }
                    ```
                    
                    **For Regular Password Change:**
                    ```json
                    {
                      "currentPassword": "OldPassword123!",
                      "newPassword": "NewSecurePassword123!"
                    }
                    ```
                    
                    **Requires:** Valid JWT token in Authorization header (or changePasswordToken for first login).
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "message": "Password changed successfully. Please login again.",
                                              "status": "success"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid current password"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired password change token"),
            @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        if (principal == null && !StringUtils.hasText(request.getChangePasswordToken())) {
            return ResponseEntity.status(403).body(Map.of("error", "Authentication required"));
        }

        JwtAuthenticationResponse response = authenticationService.changePassword(
                request,
                principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

}
