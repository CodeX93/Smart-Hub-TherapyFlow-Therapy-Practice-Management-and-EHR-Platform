package com.smart.therapy.flow.notification.controller;

import com.smart.therapy.flow.notification.dto.*;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.notification.service.NotificationSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smart.therapy.flow.common.security.PermissionConstants;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for managing notifications and preferences")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSetupService notificationSetupService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(summary = "Get user notifications", description = """
            Retrieve notifications for the authenticated user.

            **Query Parameters:**
            - `unreadOnly` (OPTIONAL): If true, returns only unread notifications (default: false)

            **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
            Therapists only receive notifications assigned to their own user account.
            """, security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @Parameter(description = "Return only unread notifications (OPTIONAL)", example = "true") @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(
                principal, unreadOnly != null && unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/all")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Get tenant notifications",
            description = "Returns notifications for a specific tenant user when userId is provided, otherwise returns all tenant notifications. Therapists must use GET /notifications for their own inbox.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<NotificationResponse>> getTenantNotifications(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(notificationService.getTenantNotifications(
                principal, userId, unreadOnly != null && unreadOnly));
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        long count = notificationService.getUnreadCount(principal);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Map<String, Long>> getUnreadCountAlt(@AuthenticationPrincipal AuthPrincipal principal) {
        long count = notificationService.getUnreadCount(principal);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.markAsRead(id, principal);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.markAllAsRead(principal);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/read-all")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Mark all as read for tenant user",
            description = "Marks all unread notifications as read for the given tenant user ID.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> markAllAsReadForUser(@PathVariable Long userId) {
        notificationService.markAllAsReadForUser(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/{id}/read")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Mark notification as read for tenant user",
            description = "Marks one notification as read for the provided tenant user ID.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> markAsReadForUser(@PathVariable Long userId, @PathVariable("id") Long id) {
        notificationService.markAsReadForUser(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mark-all-read")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Map<String, Boolean>> markAllAsReadAlt(@AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.markAllAsRead(principal);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Map<String, Boolean>> deleteNotification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.deleteNotification(id, principal);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
    @Operation(summary = "Create a notification", description = """
            Create a new notification for a user or users.

            **Request Body:**
            - See NotificationRequest DTO for required/optional fields

            **Requires:** ADMIN or SUPERVISOR role JWT token.
            """, security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        NotificationResponse notification = notificationService.createNotification(
                request, currentUserService.getCurrentUserId(principal), resolveNotificationAuthority(principal));
        return ResponseEntity.status(201).body(notification);
    }

    @PostMapping("/broadcast")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Broadcast notification within tenant",
            description = "Creates organization-level notifications by fanout to active users/clients in current tenant.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationBroadcastResponse> createBroadcastNotification(
            @Valid @RequestBody NotificationBroadcastRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.createBroadcastNotification(
                request,
                currentUserService.getCurrentUserId(principal),
                resolveNotificationAuthority(principal));
        return ResponseEntity.status(202).body(NotificationBroadcastResponse.builder()
                .status("accepted")
                .message("Broadcast queued for delivery")
                .build());
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<NotificationPreferenceResponse>> getUserPreferences(
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<NotificationPreferenceResponse> preferences = notificationService.getUserPreferences(currentUserService.getCurrentUserId(principal));
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences/{triggerType}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(summary = "Update notification preference", description = """
            Update notification preference for a specific trigger type.

            **Path Parameters:**
            - `triggerType` (REQUIRED): Type of notification trigger

            **Request Body:**
            - See NotificationPreferenceRequest DTO for required/optional fields

            **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
            """, security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationPreferenceResponse> setUserPreference(
            @Parameter(description = "Trigger type (REQUIRED)", required = true, example = "session_reminder") @PathVariable("triggerType") String triggerType,
            @Valid @RequestBody NotificationPreferenceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        NotificationPreferenceResponse preference = notificationService.setUserPreference(
                currentUserService.getCurrentUserId(principal),
                com.smart.therapy.flow.notification.enums.NotificationType.valueOf(triggerType.toUpperCase()), request);
        return ResponseEntity.ok(preference);
    }

    @GetMapping("/stats")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<NotificationStatsResponse> getStats() {
        return ResponseEntity.ok(notificationService.getStats());
    }

    @GetMapping("/setup-health")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Get notification setup health",
            description = "Returns tenant notification setup health for key session events (trigger + template coverage).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationSetupHealthResponse> getSetupHealth() {
        return ResponseEntity.ok(notificationService.getSessionNotificationSetupHealth());
    }

    @GetMapping("/setup/coverage")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Get notification coverage health",
            description = "Returns tenant-wide required event coverage for triggers/templates/channels.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationSetupHealthResponse> getCoverageHealth() {
        return ResponseEntity.ok(notificationService.getCoverageHealth());
    }

    @GetMapping("/setup/events")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Get notification event catalog",
            description = "Returns all tenant-manageable notification event keys and baseline metadata.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationEventCatalogResponse> getEventCatalog() {
        return ResponseEntity.ok(notificationService.getEventCatalog());
    }

    @GetMapping("/setup/action-metadata")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Get notification action metadata",
            description = "Returns frontend guidance for actionUrl/actionLabel and related entity mapping.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationActionMetadataResponse> getActionMetadata() {
        return ResponseEntity.ok(notificationService.getActionMetadata());
    }

    @PutMapping("/setup/action-metadata/{relatedEntityType}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Upsert notification action metadata",
            description = "Creates or updates action metadata mapping for a relatedEntityType.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationActionMetadataResponse.EntityActionDefinition> upsertActionMetadata(
            @PathVariable String relatedEntityType,
            @Valid @RequestBody NotificationActionMetadataUpsertRequest request) {
        return ResponseEntity.ok(notificationService.upsertActionMetadata(relatedEntityType, request));
    }

    @DeleteMapping("/setup/action-metadata/{relatedEntityType}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Delete notification action metadata",
            description = "Soft-deletes action metadata mapping for a relatedEntityType.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> deleteActionMetadata(@PathVariable String relatedEntityType) {
        notificationService.deleteActionMetadata(relatedEntityType);
        return ResponseEntity.ok(Map.of("success", true, "relatedEntityType", relatedEntityType));
    }

    @PostMapping("/setup/action-metadata/seed")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Seed tenant notification action metadata",
            description = "Seeds full default action metadata list into tenant DB. Set overwrite=true to refresh existing rows.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationActionMetadataResponse.SeedResult> seedActionMetadata(
            @RequestParam(defaultValue = "false") boolean overwrite) {
        return ResponseEntity.ok(notificationService.seedDefaultActionMetadata(overwrite));
    }

    @PostMapping("/setup/sync")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(summary = "Sync notification defaults",
            description = "Creates missing required templates/triggers for the current tenant without overwriting existing custom templates.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> syncNotificationDefaults() {
        notificationSetupService.syncTenantDefaults(TenantContext.getSchemaName(), TenantContext.getOrganisationId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Notification defaults synced"));
    }

    @PostMapping("/cleanup")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<Map<String, Object>> cleanupExpiredNotifications() {
        notificationService.cleanupExpiredNotifications();
        return ResponseEntity.ok(Map.of("success", true, "message", "Expired notifications cleaned up"));
    }

    @GetMapping("/triggers")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<List<NotificationTriggerResponse>> getNotificationTriggers() {
        return ResponseEntity.ok(notificationService.getAllTriggers());
    }

    @PostMapping("/triggers")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<NotificationTriggerResponse> createNotificationTrigger(
            @Valid @RequestBody NotificationTriggerRequest request) {
        return ResponseEntity.status(201).body(notificationService.createTrigger(request));
    }

    @PutMapping("/triggers/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<NotificationTriggerResponse> updateNotificationTrigger(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTriggerRequest request) {
        return ResponseEntity.ok(notificationService.updateTrigger(id, request));
    }

    @DeleteMapping("/triggers/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> deleteNotificationTrigger(@PathVariable Long id) {
        notificationService.deleteTrigger(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<List<NotificationTemplateResponse>> getNotificationTemplates(
            @RequestParam(required = false) String type) {
        List<NotificationTemplateResponse> templates = notificationService.getAllTemplates();
        // Filter by type if provided
        if (type != null && !type.isEmpty()) {
            templates = templates.stream()
                    .filter(t -> type.equalsIgnoreCase(t.getType()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<NotificationTemplateResponse> createNotificationTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.status(201).body(notificationService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<NotificationTemplateResponse> updateNotificationTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.ok(notificationService.updateTemplate(id, request));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<Void> deleteNotificationTemplate(@PathVariable Long id) {
        notificationService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveNotificationAuthority(AuthPrincipal principal) {
        if (principal == null || principal.getAuthorities() == null || principal.getAuthorities().isEmpty()) {
            return "";
        }
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        if (authorities.contains("USER_MANAGE")) return "USER_MANAGE";
        if (authorities.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (authorities.contains("ROLE_SUPERVISOR")) return "ROLE_SUPERVISOR";
        if (authorities.contains("ADMIN")) return "ADMIN";
        if (authorities.contains("SUPERVISOR")) return "SUPERVISOR";
        return authorities.get(0);
    }
}

