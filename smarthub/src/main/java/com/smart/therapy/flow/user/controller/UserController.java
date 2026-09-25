package com.smart.therapy.flow.user.controller;

import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.user.dto.*;
import com.smart.therapy.flow.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final TimezoneService timezoneService;
    
    @org.springframework.beans.factory.annotation.Autowired
    private com.smart.therapy.flow.common.config.AppProperties appProperties;

    @GetMapping
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_READ)
    public ResponseEntity<PaginatedResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int defaultPage = appProperties.getPagination().getDefaultPage();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        int maxPageSize = appProperties.getPagination().getMaxPageSize();
        int minPageSize = appProperties.getPagination().getMinPageSize();
        
        int safePage = Math.max(defaultPage, page);
        if (pageSize == null) {
            pageSize = defaultPageSize;
        }
        int safePageSize = Math.min(Math.max(pageSize, minPageSize), maxPageSize);
        return ResponseEntity.ok(userService.getUsers(safePage, safePageSize, search, role, active, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_USER_READ)
    public ResponseEntity<UserResponse> getUser(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getUser(id, principal));
    }

    @PostMapping
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.USER_CREATE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create new user",
            deprecated = true,
            description = """
                    Create a new user (staff/admin/therapist).
                    Deprecated: use `/api/v1/admin/users` for administrative user creation.
                    
                    **Required Fields:**
                    - `username` (REQUIRED): Username for login (3-50 characters)
                    - `fullName` (REQUIRED): User's full name (max 150 characters)
                    - `password` (REQUIRED): Password for login (minimum 6 characters)
                    - `email` (REQUIRED): User's email address
                    - `roles` (REQUIRED): Set of role names (at least one role required)
                    
                    **Optional Fields:**
                    - `active` (optional, default: true): Whether the user account is active
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateUserRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Therapist",
                                    value = """
                                            {
                                              "username": "john.doe",
                                              "fullName": "John Doe",
                                              "password": "SecurePassword123!",
                                              "email": "john.doe@therapyflow.pro",
                                              "roles": ["THERAPIST"],
                                              "active": true
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        log.warn("Deprecated endpoint used: POST /api/v1/users. Prefer POST /api/v1/admin/users");
        UserResponse response = userService.createUser(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.USER_EDIT)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update user",
            deprecated = true,
            description = """
                    Update an existing user. **All fields are optional** - only include the fields you want to update.
                    Deprecated: use `/api/v1/admin/users/{id}` for administrative user updates.
                    
                    **Optional Fields (include only what you want to update):**
                    - `username`: Change the username (3-50 characters)
                    - `fullName`: Change the full name (max 150 characters)
                    - `email`: Change the email address
                    - `active`: Enable/disable the user account
                    - `roles`: Update the user's roles
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponse> updateUser(
            @io.swagger.v3.oas.annotations.Parameter(description = "User ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        log.warn("Deprecated endpoint used: PUT /api/v1/users/{}. Prefer PUT /api/v1/admin/users/{}", id, id);
        UserResponse response = userService.updateUser(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.USER_DELETE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete user",
            deprecated = true,
            description = "Deprecated: use `/api/v1/admin/users/{id}` for administrative user deletion.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteUser(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        log.warn("Deprecated endpoint used: DELETE /api/v1/users/{}. Prefer DELETE /api/v1/admin/users/{}", id, id);
        userService.deleteUser(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserResponse response = userService.updateCurrentUser(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Upload my profile picture",
            description = "Upload profile picture for current user (JPG, PNG, GIF; max 800 KB).",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Map<String, Object>> uploadMyProfilePicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                userService.updateCurrentUserProfilePicture(file, principal, HttpRequestUtil.getClientIp(httpRequest))
        );
    }

    @GetMapping("/me/profile")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get my profile",
            description = "Get the current authenticated user's profile including availability settings.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal));
    }

    @PostMapping("/me/profile")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create my profile",
            description = """
                    Create or update the current authenticated user's profile.
                    
                    **Available Fields:**
                    - `licenseNumber`: Professional license number
                    - `specializations`: List of specializations
                    - `languages`: List of languages spoken
                    - `workingDays`: List of working days (e.g., ["monday", "tuesday"])
                    - `workingHours`: JSON string of working hours per day
                    - `maxClientsPerDay`: Maximum clients per day
                    - `sessionDuration`: Default session duration in minutes
                    - `availabilityStatus`: Current availability status
                    - `timezone`: IANA timezone ID (e.g., "America/New_York", "Asia/Karachi")
                    
                    **Working Hours JSON Format:**
                    ```json
                    [
                      {
                        "day": "monday",
                        "enabled": true,
                        "start": "09:00",
                        "end": "17:00"
                      },
                      {
                        "day": "tuesday",
                        "enabled": true,
                        "start": "09:00",
                        "end": "17:00"
                      }
                    ]
                    ```
                    
                    **Requires:** Authenticated user (any role).
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(userService.upsertProfile(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PutMapping("/me/profile")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update my profile",
            description = """
                    Update the current authenticated user's profile including availability settings.
                    
                    **All fields are optional** - only include fields you want to update.
                    
                    **Available Fields:**
                    - `licenseNumber`: Professional license number
                    - `specializations`: List of specializations
                    - `languages`: List of languages spoken
                    - `workingDays`: List of working days (e.g., ["monday", "tuesday"])
                    - `workingHours`: JSON string of working hours per day
                    - `maxClientsPerDay`: Maximum clients per day
                    - `sessionDuration`: Default session duration in minutes
                    - `availabilityStatus`: Current availability status
                    - `timezone`: IANA timezone ID (e.g., "America/New_York", "Asia/Karachi")
                    
                    **Working Hours JSON Format:**
                    ```json
                    [
                      {
                        "day": "monday",
                        "enabled": true,
                        "start": "09:00",
                        "end": "17:00"
                      }
                    ]
                    ```
                    
                    **Requires:** Authenticated user (any role).
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(userService.upsertProfile(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PatchMapping("/me/profile")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Patch my profile",
            description = "Partially update the current authenticated user's profile. Only provided fields are updated.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    public ResponseEntity<UserProfileResponse> patchProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(userService.upsertProfile(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @GetMapping("/timezones")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "List available timezones",
            description = """
                    Returns all supported IANA timezone IDs (e.g., "America/New_York", "Asia/Karachi")
                    sorted alphabetically for timezone dropdowns in profile settings.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    public ResponseEntity<java.util.Map<String, java.util.List<String>>> listAvailableTimezones() {
        return ResponseEntity.ok(java.util.Map.of("timezones", timezoneService.getAvailableTimezoneIds()));
    }

    @GetMapping("/me/timezone")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get my timezone",
            description = """
                    Get the current authenticated user's timezone setting.
                    
                    Returns the IANA timezone ID (e.g., "America/New_York", "Asia/Karachi").
                    If timezone is not set, returns null.
                    
                    **Requires:** Authenticated user (any role).
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    public ResponseEntity<java.util.Map<String, String>> getTimezone(@AuthenticationPrincipal AuthPrincipal principal) {
        String timezone = userService.getTimezone(principal);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("timezone", timezone);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/timezone")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update my timezone",
            description = """
                    Update the current authenticated user's timezone setting.
                    
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
                    
                    **Requires:** Authenticated user (any role).
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.tags.Tag(name = "User Profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Timezone updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserProfileResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid timezone format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User profile not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserProfileResponse> updateTimezone(
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
            @RequestBody java.util.Map<String, String> request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        String timezone = request.get("timezone");
        if (timezone == null || timezone.isBlank()) {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("Timezone is required");
        }
        
        // Validate timezone format
        try {
            timezone = timezoneService.normalizeTimezoneId(timezone);
        } catch (Exception e) {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("Invalid timezone format: " + timezone + ". Please use IANA timezone ID (e.g., 'America/New_York', 'Asia/Karachi')");
        }
        
        UserProfileRequest profileRequest = new UserProfileRequest();
        profileRequest.setTimezone(timezone);
        return ResponseEntity.ok(userService.upsertProfile(profileRequest, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        userService.changePassword(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/zoom-credentials")
    public ResponseEntity<ZoomCredentialsResponse> updateZoomCredentials(
            @Valid @RequestBody ZoomCredentialsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(userService.updateZoomCredentials(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @DeleteMapping("/me/zoom-credentials")
    public ResponseEntity<Void> deleteZoomCredentials(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        userService.deleteZoomCredentials(principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/zoom-credentials/status")
    public ResponseEntity<ZoomCredentialsResponse> getZoomStatus(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getZoomCredentialsStatus(principal));
    }

    @PostMapping("/me/zoom-credentials/test")
    public ResponseEntity<ZoomCredentialsResponse> testZoomCredentials(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(userService.testZoomCredentials(principal));
    }

    @PostMapping("/{userId}/activity")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_READ)
    public ResponseEntity<UserActivityLogResponse> logUserActivity(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody CreateUserActivityLogRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserActivityLogResponse response = userService.logUserActivity(
                userId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{userId}/activity")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_READ)
    public ResponseEntity<List<UserActivityLogResponse>> getUserActivity(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<UserActivityLogResponse> activities = userService.getUserActivityHistory(userId, limit, principal);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/{userId}/profile")
    @PreAuthorize(StaffAuthorizationExpressions.SCHEDULING_USER_PROFILE_READ)
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        UserProfileResponse profile = userService.getUserProfile(userId, principal);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{userId}/profile")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_EDIT)
    public ResponseEntity<UserProfileResponse> createUserProfile(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserProfileResponse profile = userService.createUserProfile(
                userId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(profile);
    }

    @PutMapping("/{userId}/profile")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_EDIT)
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserProfileResponse profile = userService.updateUserProfile(
                userId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/{userId}/profile")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_EDIT)
    public ResponseEntity<UserProfileResponse> patchUserProfile(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UserProfileResponse profile = userService.updateUserProfile(
                userId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/{userId}/profile")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_EDIT)
    public ResponseEntity<Void> deleteUserProfile(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        userService.deleteUserProfile(userId, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/zoom-credentials/status")
    @PreAuthorize(StaffAuthorizationExpressions.THERAPIST_ADMIN_USER_READ)
    public ResponseEntity<ZoomCredentialsResponse> getZoomCredentialsStatus(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ZoomCredentialsResponse response = userService.getZoomCredentialsStatus(userId, principal);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // SUPERVISOR ASSIGNMENT ENDPOINTS
    // ========================================

    @PostMapping("/supervisor-assignments")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Assign supervisor to therapist",
            description = """
                    Create a new supervisor assignment, linking a supervisor to a therapist.
                    
                    **Required Fields:**
                    - `supervisorId` (REQUIRED): ID of the supervisor user
                    - `therapistId` (REQUIRED): ID of the therapist user
                    - `requiredMeetingFrequency` (REQUIRED): Meeting frequency (DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY)
                    
                    **Optional Fields:**
                    - `assignmentType` (optional, default: PRIMARY): Type of assignment (PRIMARY, SECONDARY, CLINICAL)
                    - `startDate` (optional, default: today): Start date of the assignment
                    - `endDate` (optional): End date of the assignment (null means ongoing)
                    - `notes` (optional): Notes about the assignment
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Supervisor assignment created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SupervisorAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supervisor or therapist not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<SupervisorAssignmentResponse> createSupervisorAssignment(
            @Valid @RequestBody CreateSupervisorAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SupervisorAssignmentResponse response = userService.createSupervisorAssignment(
                request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/supervisor-assignments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "List supervisor assignments",
            description = """
                    Retrieve supervisor assignments with optional filtering.
                    
                    **Query Parameters:**
                    - `supervisorId` (optional): Filter by supervisor ID
                    - `therapistId` (optional): Filter by therapist ID
                    - `active` (optional): Filter by active status (true/false)
                    
                    **Authorization:**
                    - ADMIN: Can view all assignments
                    - SUPERVISOR: Can only view their own assignments
                    - THERAPIST: Can only view assignments where they are the therapist
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of supervisor assignments retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SupervisorAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<SupervisorAssignmentResponse>> getSupervisorAssignments(
            @RequestParam(required = false) Long supervisorId,
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String requiredMeetingFrequency,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SupervisorAssignmentResponse> assignments = userService.getSupervisorAssignments(
                supervisorId, therapistId, active, search, requiredMeetingFrequency, principal);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/supervisor-assignments/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get supervisor assignment by ID",
            description = """
                    Retrieve a specific supervisor assignment by its ID.
                    
                    **Authorization:**
                    - ADMIN: Can view any assignment
                    - SUPERVISOR: Can only view assignments where they are the supervisor
                    - THERAPIST: Can only view assignments where they are the therapist
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Supervisor assignment retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SupervisorAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<SupervisorAssignmentResponse> getSupervisorAssignment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SupervisorAssignmentResponse assignment = userService.getSupervisorAssignment(id, principal);
        return ResponseEntity.ok(assignment);
    }

    @PutMapping("/supervisor-assignments/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update supervisor assignment",
            description = """
                    Update an existing supervisor assignment.
                    
                    **All fields are optional** - only include fields you want to update.
                    
                    **Optional Fields:**
                    - `assignmentType`: Type of assignment (PRIMARY, SECONDARY, CLINICAL)
                    - `startDate`: Start date of the assignment
                    - `endDate`: End date of the assignment (null means ongoing)
                    - `requiredMeetingFrequency`: Meeting frequency (DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY)
                    - `notes`: Notes about the assignment
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Supervisor assignment updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SupervisorAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<SupervisorAssignmentResponse> updateSupervisorAssignment(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSupervisorAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SupervisorAssignmentResponse response = userService.updateSupervisorAssignment(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/supervisor-assignments/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete supervisor assignment",
            description = """
                    Delete a supervisor assignment.
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Supervisor assignment deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> deleteSupervisorAssignment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        userService.deleteSupervisorAssignment(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

}


