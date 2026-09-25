package com.smart.therapy.flow.admin.controller;

import com.smart.therapy.flow.common.config.AppProperties;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.user.dto.*;
import com.smart.therapy.flow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only REST API for user (staff) management.
 * All endpoints require ADMIN role and appropriate permissions.
 * Base path: {@code /api/v1/admin/users}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Users", description = "Administrative user management: CRUD, activate, deactivate, assign roles")
public class AdminUserController {

    private final UserService userService;
    private final AppProperties appProperties;

    @GetMapping
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_READ)
    @Operation(
            summary = "List users (paginated)",
            description = """
                    List all staff users with optional filters. Only administrators can access this endpoint.
                    
                    **Query parameters:**
                    - `page` (optional, default: 1): Page number (1-based).
                    - `pageSize` (optional): Items per page (bounded by app defaults).
                    - `search` (optional): Search by username, full name, or email.
                    - `role` (optional): Filter by role name (e.g. THERAPIST, ADMIN).
                    - `active` (optional): Filter by active status (true/false).
                    
                    **Returns:** Paginated list of user summaries (id, username, fullName, email, active, roles, createdAt, updatedAt).
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of users",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions – ADMIN required"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PaginatedResponse<UserResponse>> getUsers(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Search by username, full name, or email") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by role name") @RequestParam(required = false) String role,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int safePage = Math.max(appProperties.getPagination().getDefaultPage(), page);
        if (pageSize == null) {
            pageSize = appProperties.getPagination().getDefaultPageSize();
        }
        int safePageSize = Math.min(Math.max(pageSize, appProperties.getPagination().getMinPageSize()),
                appProperties.getPagination().getMaxPageSize());
        return ResponseEntity.ok(userService.getUsers(safePage, safePageSize, search, role, active, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_READ)
    @Operation(
            summary = "Get user by ID",
            description = "Retrieve a single user by ID. Requires ADMIN and USER_VIEW.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getUser(id, principal));
    }

    @PostMapping
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_CREATE)
    @Operation(
            summary = "Create user",
            description = """
                    Create a new staff user. Requires username, fullName, password, email, and at least one role.
                    Optional: active (default true), idempotencyKey for duplicate prevention.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateUserRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create therapist",
                                    value = "{\"username\":\"jane.doe\",\"fullName\":\"Jane Doe\",\"password\":\"SecurePass1!\",\"email\":\"jane@example.com\",\"roles\":[\"THERAPIST\"],\"active\":true}"
                            ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or username/email already in use"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserResponse response = userService.createUser(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_EDIT)
    @Operation(
            summary = "Update user",
            description = "Update an existing user. All request fields are optional; only provided fields are updated. Only ADMIN can change roles.",
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateUserRequest.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate username/email"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserResponse response = userService.updateUser(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_DELETE)
    @Operation(
            summary = "Delete user (soft)",
            description = "Soft-delete a user. You cannot delete your own account. Requires ADMIN and USER_DELETE.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete own account or already deleted"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        userService.deleteUser(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_EDIT)
    @Operation(
            summary = "Activate user",
            description = "Set the user's active flag to true. Requires ADMIN and USER_EDIT.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User activated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> activateUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setActive(true);
        UserResponse response = userService.updateUser(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_EDIT)
    @Operation(
            summary = "Deactivate user",
            description = "Set the user's active flag to false. Requires ADMIN and USER_EDIT.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deactivated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> deactivateUser(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setActive(false);
        UserResponse response = userService.updateUser(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign-role")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_EDIT)
    @Operation(
            summary = "Assign roles to user",
            description = """
                    Replace the user's roles with the given set. At least one role is required.
                    Valid role names: ADMIN, SUPERVISOR, THERAPIST, BILLING_SPECIALIST.
                    Requires ADMIN and USER_EDIT.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssignRoleRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Assign therapist and supervisor",
                                    value = "{\"roles\":[\"THERAPIST\",\"SUPERVISOR\"]}"
                            ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles assigned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid role name"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> assignRole(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setRoles(request.getRoles());
        UserResponse response = userService.updateUser(id, updateRequest, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }
}
