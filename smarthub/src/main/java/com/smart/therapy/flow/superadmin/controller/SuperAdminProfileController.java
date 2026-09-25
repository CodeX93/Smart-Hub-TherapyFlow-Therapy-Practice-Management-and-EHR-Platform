package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.MfaService;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.user.dto.AvailabilityStatus;
import com.smart.therapy.flow.user.dto.ChangePasswordRequest;
import com.smart.therapy.flow.user.dto.UpdateUserRequest;
import com.smart.therapy.flow.user.dto.UserProfileRequest;
import com.smart.therapy.flow.user.dto.UserProfileResponse;
import com.smart.therapy.flow.user.dto.UserResponse;
import com.smart.therapy.flow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/super-admin/me")
@RequiredArgsConstructor
@Tag(name = "Super Admin Profile", description = "Profile and security settings for platform super admins")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminProfileController {

    private final UserService userService;
    private final AuthIdentityService authIdentityService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthenticationService authenticationService;
    private final MfaService mfaService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get current super-admin account", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        if (isPublicPlatformPrincipal(principal)) {
            return ResponseEntity.ok(toPlatformUserResponse(principal, authIdentityService.getById(principal.getAuthId())));
        }
        return ResponseEntity.ok(userService.getCurrentUser(principal));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Update current super-admin account", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        if (isPublicPlatformPrincipal(principal)) {
            AuthIdentity identity = updatePlatformIdentity(
                    principal,
                    request.getUsername(),
                    request.getEmail(),
                    request.getFullName(),
                    request.getPhone()
            );
            return ResponseEntity.ok(toPlatformUserResponse(principal, identity));
        }
        return ResponseEntity.ok(userService.updateCurrentUser(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @GetMapping("/profile")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get current super-admin profile", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthPrincipal principal) {
        if (isPublicPlatformPrincipal(principal)) {
            AuthIdentity identity = authIdentityService.getById(principal.getAuthId());
            return ResponseEntity.ok(toPlatformProfileResponse(identity));
        }
        return ResponseEntity.ok(userService.getProfile(principal));
    }

    @PutMapping("/profile")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Update current super-admin profile", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        if (isPublicPlatformPrincipal(principal)) {
            AuthIdentity identity = updatePlatformIdentity(
                    principal,
                    null,
                    request.getEmail(),
                    request.getFullName(),
                    null
            );
            return ResponseEntity.ok(toPlatformProfileResponse(identity));
        }
        return ResponseEntity.ok(userService.upsertProfile(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Upload profile picture", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        if (isPublicPlatformPrincipal(principal)) {
            return ResponseEntity.ok(Map.of(
                    "supported", false,
                    "message", "Profile picture is not supported for platform public-schema identities yet"
            ));
        }
        return ResponseEntity.ok(userService.updateCurrentUserProfilePicture(file, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PostMapping("/change-password")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Change current super-admin password", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.auth.dto.ChangePasswordRequest authRequest = new com.smart.therapy.flow.auth.dto.ChangePasswordRequest();
        authRequest.setCurrentPassword(request.getCurrentPassword());
        authRequest.setNewPassword(request.getNewPassword());
        authenticationService.changePassword(
                authRequest,
                principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/2fa")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get 2FA status", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<MfaDtos.StatusResponse> getTwoFactorStatus(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(mfaService.status(principal.getAuthId(), principal.getAuthorities()));
    }

    @PostMapping("/2fa/enable")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Start 2FA enrollment", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<MfaDtos.EnrollmentStartResponse> enableTwoFactor(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mfaService.startEnrollment(
                        principal.getAuthId(), principal.getLoginIdentifier()));
    }

    @PostMapping("/2fa/confirm")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Confirm 2FA enrollment", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<MfaDtos.ConfirmResponse> confirmTwoFactor(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mfaService.confirmEnrollment(principal.getAuthId(), request.code()));
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Disable 2FA", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> disableTwoFactor(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request) {
        mfaService.disable(principal.getAuthId(), request.code());
        return ResponseEntity.noContent().build();
    }

    private static boolean isPublicPlatformPrincipal(AuthPrincipal principal) {
        if (principal == null || principal.getIdentityType() != IdentityType.STAFF) {
            return false;
        }
        String schema = TenantContext.getSchemaName();
        return schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);
    }

    private AuthIdentity updatePlatformIdentity(
            AuthPrincipal principal,
            String username,
            String email,
            String fullName,
            String phone
    ) {
        AuthIdentity identity = authIdentityService.getById(principal.getAuthId());
        if (identity == null) {
            throw new com.smart.therapy.flow.common.exception.ForbiddenException("Staff user not found");
        }
        boolean changed = false;
        if (StringUtils.hasText(username)) {
            String trimmed = username.trim();
            String currentUsername = identity.getUsername() != null ? identity.getUsername() : identity.getLoginIdentifier();
            if (!trimmed.equals(currentUsername)) {
                authIdentityService.updateUsername(identity, trimmed);
                changed = true;
            }
        }
        if (StringUtils.hasText(email)) {
            String trimmedEmail = email.trim();
            String currentEmail = identity.getEmail() != null ? identity.getEmail() : identity.getLoginIdentifier();
            if (!trimmedEmail.equalsIgnoreCase(currentEmail)) {
                authIdentityService.updateEmail(identity, trimmedEmail);
                changed = true;
            }
        }
        if (StringUtils.hasText(fullName)) {
            String trimmedFullName = fullName.trim();
            if (!Objects.equals(trimmedFullName, identity.getFullName())) {
                identity.setFullName(trimmedFullName);
                changed = true;
            }
        }
        if (StringUtils.hasText(phone)) {
            String trimmedPhone = phone.trim();
            if (!Objects.equals(trimmedPhone, identity.getPhone())) {
                identity.setPhone(trimmedPhone);
                changed = true;
            }
        }
        return changed ? authIdentityRepository.save(identity) : identity;
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) return primary;
        if (secondary != null && !secondary.isBlank()) return secondary;
        return null;
    }

    private static UserResponse toPlatformUserResponse(AuthPrincipal principal, AuthIdentity identity) {
        String login = identity != null ? identity.getLoginIdentifier() : principal.getLoginIdentifier();
        String fullName = (identity != null && StringUtils.hasText(identity.getFullName()))
                ? identity.getFullName()
                : login;
        return UserResponse.builder()
                .id(principal.getAuthId())
                .username(identity != null ? firstNonBlank(identity.getUsername(), login) : login)
                .fullName(fullName)
                .email(identity != null ? firstNonBlank(identity.getEmail(), login) : login)
                .phone(identity != null ? identity.getPhone() : null)
                .active(identity == null || Boolean.TRUE.equals(identity.getIsActive()))
                .roles(extractRoleNames(principal))
                .createdAt(identity != null ? identity.getCreatedAt() : null)
                .updatedAt(identity != null ? identity.getUpdatedAt() : null)
                .build();
    }

    private static UserProfileResponse toPlatformProfileResponse(AuthIdentity identity) {
        String login = identity != null ? identity.getLoginIdentifier() : null;
        String fullName = (identity != null && StringUtils.hasText(identity.getFullName()))
                ? identity.getFullName()
                : login;
        return UserProfileResponse.builder()
                .fullName(fullName)
                .email(identity != null ? firstNonBlank(identity.getEmail(), login) : login)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .zoomConfigured(false)
                .passwordChangeRequired(false)
                .build();
    }

    private static List<String> extractRoleNames(AuthPrincipal principal) {
        if (principal == null || principal.getAuthorities() == null) {
            return List.of();
        }
        return principal.getAuthorities().stream()
                .map(a -> a != null ? a.getAuthority() : null)
                .filter(Objects::nonNull)
                .filter(v -> v.startsWith("ROLE_"))
                .map(v -> v.substring(5))
                .distinct()
                .collect(Collectors.toList());
    }
}
