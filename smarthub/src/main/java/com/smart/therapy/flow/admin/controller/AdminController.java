package com.smart.therapy.flow.admin.controller;

import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.admin.dto.AdminDashboardSummaryResponse;
import com.smart.therapy.flow.admin.dto.TestEmailRequest;
import com.smart.therapy.flow.admin.service.AdminDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin", description = "Administrative APIs")
public class AdminController {

    private final EmailService emailService;
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard/summary")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and "
            + PermissionConstants.CLIENT_VIEW + " and "
            + PermissionConstants.SESSION_VIEW + " and "
            + PermissionConstants.CONSENT_ADMIN_VIEW + " and "
            + PermissionConstants.BILLING_VIEW)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get tenant admin dashboard summary",
            description = "Returns top dashboard card metrics for tenant admin in a single response.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<AdminDashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(adminDashboardService.getDashboardSummary(principal));
    }

    @PostMapping("/test-email")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Send test email",
            description = """
                    Send a test email to verify email configuration.
                    
                    **Request Body:**
                    - `toEmail` (REQUIRED): Email address to send test email to
                    
                    **Requires:** ADMIN role JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Test email request",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Test Email",
                                    value = """
                                            {
                                              "toEmail": "test@example.com"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Test email sent successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to send email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> testEmail(
            @Valid @RequestBody TestEmailRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        log.info("Admin requested test email: authId={}", principal.getAuthId());
        
        try {
            emailService.sendTestEmail(request.getToEmail());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent to " + request.getToEmail(),
                    "from", emailService.getFromAddress()
            ));
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to send test email"));
        }
    }

}
