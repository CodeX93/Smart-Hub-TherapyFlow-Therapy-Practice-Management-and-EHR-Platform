package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.notification.dto.NotificationTriggerRequest;
import com.smart.therapy.flow.notification.dto.NotificationTriggerResponse;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminNotificationHistoryItemResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminNotificationRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminTargetedNotificationRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUpsertNotificationTemplateRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationJob;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationTemplate;
import com.smart.therapy.flow.superadmin.service.SuperAdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Super Admin Notifications", description = "Broadcast, targeted, scheduled platform notifications")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminNotificationController {

    private final SuperAdminNotificationService notificationService;
    private final PlatformAuditService platformAuditService;

    @PostMapping("/broadcast")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create broadcast notification", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createBroadcast(
            @Valid @RequestBody SuperAdminNotificationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return create("BROADCAST", request, principal);
    }

    @PostMapping("/targeted")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create targeted notification", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createTargeted(
            @Valid @RequestBody SuperAdminTargetedNotificationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminNotificationRequest payload = new SuperAdminNotificationRequest();
        payload.setTitle(request.getTitle());
        payload.setMessage(request.getMessage());
        payload.setChannel(request.getChannel());
        payload.setScheduledAt(request.getScheduledAt());
        payload.setOrganisationIds(request.getOrganisationIds());
        return create("TARGETED", payload, principal);
    }

    @PostMapping("/scheduled")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create scheduled notification", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createScheduled(
            @Valid @RequestBody SuperAdminNotificationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return create("SCHEDULED", request, principal);
    }

    @GetMapping("/history")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Notification history", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminNotificationHistoryItemResponse>> history(
            @RequestParam(required = false) String orgId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long parsedOrgId = parseOrgId(orgId);
        return ResponseEntity.ok(notificationService.getHistoryWithReadState(parsedOrgId, requireAuthId(principal)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Notification unread count", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Long>> unreadCount(
            @RequestParam(required = false) String orgId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long parsedOrgId = parseOrgId(orgId);
        long count = notificationService.getUnreadCount(requireAuthId(principal), parsedOrgId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Mark notification as read", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        boolean created = notificationService.markJobAsRead(id, requireAuthId(principal));
        return ResponseEntity.ok(Map.of("success", true, "newlyMarked", created));
    }

    @PatchMapping("/read-all")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Mark all notifications as read", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestParam(required = false) String orgId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long parsedOrgId = parseOrgId(orgId);
        long updated = notificationService.markAllAsRead(requireAuthId(principal), parsedOrgId);
        return ResponseEntity.ok(Map.of("success", true, "updated", updated));
    }

    @GetMapping("/templates")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Notification templates", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<PlatformNotificationTemplate>> templates() {
        return ResponseEntity.ok(notificationService.getTemplates());
    }

    @GetMapping("/triggers")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Notification triggers", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<NotificationTriggerResponse>> triggers() {
        return ResponseEntity.ok(notificationService.getGlobalTriggers());
    }

    @GetMapping("/triggers/metadata")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Notification trigger metadata", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> triggerMetadata() {
        return ResponseEntity.ok(notificationService.getTriggerMetadata());
    }

    @PostMapping("/triggers")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create notification trigger", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationTriggerResponse> createTrigger(
            @Valid @RequestBody NotificationTriggerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createGlobalTrigger(request));
    }

    @PutMapping("/triggers/{id}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update notification trigger", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<NotificationTriggerResponse> updateTrigger(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTriggerRequest request
    ) {
        return ResponseEntity.ok(notificationService.updateGlobalTrigger(id, request));
    }

    @DeleteMapping("/triggers/{id}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete notification trigger", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> deleteTrigger(@PathVariable Long id) {
        notificationService.deleteGlobalTrigger(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/templates/{templateKey}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert notification template", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertTemplate(
            @PathVariable String templateKey,
            @Valid @RequestBody SuperAdminUpsertNotificationTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        PlatformNotificationTemplate saved = notificationService.upsertTemplate(
                templateKey,
                request.getSubjectTemplate(),
                request.getBodyTemplate(),
                request.getActive(),
                principal != null ? principal.getAuthId() : null
        );
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "PLATFORM_NOTIFICATION_TEMPLATE_UPSERTED",
                "PlatformNotificationTemplate",
                saved.getTemplateKey(),
                "active=" + saved.getIsActive()
        );
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/templates/{templateKey}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete notification template", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable String templateKey,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        notificationService.deleteTemplateByKey(templateKey);
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "PLATFORM_NOTIFICATION_TEMPLATE_DELETED",
                "PlatformNotificationTemplate",
                templateKey,
                "deleted=true"
        );
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Object> create(String type, SuperAdminNotificationRequest request, AuthPrincipal principal) {
        PlatformNotificationJob saved = notificationService.createJob(
                type,
                request.getTitle(),
                request.getMessage(),
                request.getChannel(),
                request.getScheduledAt(),
                Map.of("organisationIds", request.getOrganisationIds() != null ? request.getOrganisationIds() : List.of()),
                principal != null ? principal.getAuthId() : null
        );
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "PLATFORM_NOTIFICATION_JOB_CREATED",
                "PlatformNotificationJob",
                String.valueOf(saved.getId()),
                "type=" + type
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private Long parseOrgId(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return null;
        }
        String value = orgId.trim();
        if (value.startsWith("org_")) {
            value = value.substring(4);
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new com.smart.therapy.flow.common.exception.StoryApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_ORG_ID", "orgId must be a numeric id"
            );
        }
    }

    private Long requireAuthId(AuthPrincipal principal) {
        if (principal == null || principal.getAuthId() == null) {
            throw new com.smart.therapy.flow.common.exception.StoryApiException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "Authentication required"
            );
        }
        return principal.getAuthId();
    }

}
