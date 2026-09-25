package com.smart.therapy.flow.notification.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.notification.dto.*;
import com.smart.therapy.flow.notification.service.NotificationServiceExtension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Client Portal Notification Controller
 * 
 * HIPAA Compliance:
 * - Clients can only access their own notifications
 * - All PHI is behind authentication
 * - Audit trail for all notification views
 * - No PHI in push/email notifications (handled by service layer)
 */
@RestController
@RequestMapping("/api/v1/portal/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Portal - Notifications", description = "Notification APIs for client portal")
public class ClientPortalNotificationController {

    private final NotificationServiceExtension notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(
            summary = "Get client notifications",
            description = """
                    Retrieve notifications for the authenticated client.
                    
                    **Query Parameters:**
                    - `page` (OPTIONAL): Page number, default `1`
                    - `pageSize` (OPTIONAL): Page size, default `20`, max `100`
                    - `unreadOnly` (OPTIONAL): If true, returns only unread notifications (default: false)
                    
                    **HIPAA Compliance:**
                    - Client can only see their own notifications
                    - All notification content is encrypted at rest
                    - Read status is tracked for audit purposes
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a client")
    })
    public ResponseEntity<PaginatedResponse<NotificationResponse>> getNotifications(
            @Parameter(description = "Page number (OPTIONAL)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size (OPTIONAL)", example = "20")
            @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "Return only unread notifications (OPTIONAL)", example = "true")
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.info("Client getting notifications: clientId={}, unreadOnly={}, page={}, pageSize={}",
                clientId, unreadOnly, page, pageSize);

        PaginatedResponse<NotificationResponse> notifications = notificationService.getClientNotifications(
                clientId,
                unreadOnly != null && unreadOnly,
                page,
                pageSize
        );

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    @Operation(
            summary = "Get unread notification count",
            description = """
                    Get the count of unread notifications for the authenticated client.
                    
                    **Returns:** Count of unread notifications as a plain number.
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.debug("Client getting unread count: clientId={}", clientId);

        long count = notificationService.getClientUnreadCount(clientId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Get unread notification count (alternative format)",
            description = """
                    Get the count of unread notifications for the authenticated client.
                    
                    **Returns:** Count in JSON format: {"count": 5}
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<Map<String, Long>> getUnreadCountAlt(@AuthenticationPrincipal AuthPrincipal principal) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.debug("Client getting unread count (alt): clientId={}", clientId);

        long count = notificationService.getClientUnreadCount(clientId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Mark notification as read",
            description = """
                    Mark a specific notification as read for the authenticated client.
                    
                    **Path Parameters:**
                    - `id` (REQUIRED): Notification ID
                    
                    **HIPAA Compliance:**
                    - Read status is tracked for audit purposes
                    - Client can only mark their own notifications as read
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not your notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Notification ID (REQUIRED)", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.info("Client marking notification as read: clientId={}, notificationId={}", clientId, id);

        notificationService.markClientNotificationAsRead(id, clientId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = """
                    Mark all notifications as read for the authenticated client.
                    
                    **HIPAA Compliance:**
                    - Read status is tracked for audit purposes
                    - Only affects client's own notifications
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully marked all as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal AuthPrincipal principal) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.info("Client marking all notifications as read: clientId={}", clientId);

        notificationService.markAllClientNotificationsAsRead(clientId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mark-all-read")
    @Operation(
            summary = "Mark all notifications as read (alternative format)",
            description = """
                    Mark all notifications as read for the authenticated client.
                    
                    **Returns:** Success status in JSON format: {"success": true}
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully marked all as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<Map<String, Boolean>> markAllAsReadAlt(@AuthenticationPrincipal AuthPrincipal principal) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.info("Client marking all notifications as read (alt): clientId={}", clientId);

        notificationService.markAllClientNotificationsAsRead(clientId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete notification (soft delete)",
            description = """
                    Soft delete a notification for the authenticated client.
                    
                    **Path Parameters:**
                    - `id` (REQUIRED): Notification ID
                    
                    **HIPAA Compliance:**
                    - Soft delete (notification is marked as deleted, not removed from database)
                    - Audit trail is preserved
                    - Client can only delete their own notifications
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted notification"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not your notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Map<String, Boolean>> deleteNotification(
            @Parameter(description = "Notification ID (REQUIRED)", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.info("Client deleting notification: clientId={}, notificationId={}", clientId, id);

        notificationService.deleteClientNotification(id, clientId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/preferences")
    @Operation(
            summary = "Get notification preferences",
            description = """
                    Get notification preferences for the authenticated client.
                    
                    **Returns:** List of notification preferences by type
                    
                    **Preferences Include:**
                    - IN_APP notifications (always enabled)
                    - EMAIL notifications (opt-in)
                    - SMS notifications (opt-in, HIPAA compliant)
                    - Quiet hours settings
                    - Per-notification-type settings
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved preferences"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<List<NotificationPreferenceResponse>> getUserPreferences(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.debug("Client getting notification preferences: clientId={}", clientId);

        List<NotificationPreferenceResponse> preferences =
                notificationService.getClientPreferences(clientId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences/{notificationType}")
    @Operation(
            summary = "Update notification preference",
            description = """
                    Update notification preference for a specific notification type.
                    
                    **Path Parameters:**
                    - `notificationType` (REQUIRED): Type of notification (e.g., 'appointment_reminder', 'form_due')
                    
                    **Request Body:**
                    - See NotificationPreferenceRequest DTO for fields
                    
                    **HIPAA Compliance:**
                    - SMS requires explicit opt-in
                    - Email notifications are opt-in by default
                    - IN_APP notifications cannot be disabled (required for portal functionality)
                    
                    **Requires:** Valid client portal JWT token.
                    """,
            security = @SecurityRequirement(name = "ClientBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated preference"),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid preference data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<NotificationPreferenceResponse> setUserPreference(
            @Parameter(description = "Notification type (REQUIRED)", required = true, example = "appointment_reminder")
            @PathVariable("notificationType") String notificationType,
            @Valid @RequestBody NotificationPreferenceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long clientId = currentUserService.requireCurrentClient(principal).getId();
        log.info("Client updating notification preference: clientId={}, type={}", clientId, notificationType);

        NotificationPreferenceResponse preference = notificationService.setClientPreference(
                clientId,
                notificationType,
                request
        );
        return ResponseEntity.ok(preference);
    }
}

