package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUpsertNotificationTemplateRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformNotificationTemplate;
import com.smart.therapy.flow.superadmin.service.SuperAdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/super-admin/email-templates")
@RequiredArgsConstructor
@Tag(name = "Super Admin Email Templates", description = "Alias endpoints for platform email/notification templates")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
})
public class SuperAdminEmailTemplateController {

    private final SuperAdminNotificationService notificationService;
    private final PlatformAuditService platformAuditService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List email templates", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<PlatformNotificationTemplate>> listTemplates() {
        return ResponseEntity.ok(notificationService.getTemplates());
    }

    @GetMapping("/{templateKey}")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get email template by key", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlatformNotificationTemplate> getTemplateByKey(@PathVariable String templateKey) {
        return ResponseEntity.ok(notificationService.getTemplateByKey(templateKey));
    }

    @PutMapping("/{templateKey}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert email template", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlatformNotificationTemplate> upsertTemplate(
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
                "PLATFORM_EMAIL_TEMPLATE_UPSERTED",
                "PlatformNotificationTemplate",
                saved.getTemplateKey(),
                "active=" + saved.getIsActive()
        );
        return ResponseEntity.ok(saved);
    }
}
