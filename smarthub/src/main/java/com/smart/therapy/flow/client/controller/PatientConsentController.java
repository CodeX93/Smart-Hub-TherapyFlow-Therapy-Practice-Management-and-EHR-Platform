package com.smart.therapy.flow.client.controller;

import com.smart.therapy.flow.client.dto.ClientConsentResponse;
import com.smart.therapy.flow.client.dto.PatientConsentManagementResponse;
import com.smart.therapy.flow.client.dto.StaffRecordConsentRequest;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.service.PatientConsentQueryService;
import com.smart.therapy.flow.client.service.ConsentCommandService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/consents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Consents", description = "Patient Consent Management (Admin)")
@SecurityRequirement(name = "bearerAuth")
public class PatientConsentController {

    private final PatientConsentQueryService patientConsentQueryService;
    private final ConsentCommandService consentCommandService;

    @GetMapping("/management")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(
            summary = "Get patient consent management list",
            description = """
                    Retrieve a summary list of all patients with their consent statuses.
                    
                    **Response includes:**
                    - Client ID, Full Name, Email
                    - Portal Access status
                    - AI Processing consent status
                    - Data Sharing consent status
                    - Research Participation consent status
                    - Marketing Communications consent status
                    
                    **Filters:**
                    - `consentType`: Filter by consent type (ALL, AI_PROCESSING, DATA_SHARING, RESEARCH, MARKETING)
                    - `status`: Filter by consent status (ALL, GRANTED, DENIED / WITHDRAWN)
                    - `search`: Search by client ID, full name, or email
                    
                    Admin and Supervisor only.
                    """
    )
    public ResponseEntity<List<PatientConsentManagementResponse>> getConsentManagementList(
            @Parameter(description = "Filter by consent type: ALL, AI_PROCESSING, DATA_SHARING, RESEARCH, MARKETING")
            @RequestParam(required = false, defaultValue = "ALL") String consentType,

            @Parameter(description = "Filter by consent status: ALL, GRANTED, DENIED / WITHDRAWN")
            @RequestParam(required = false, defaultValue = "ALL") String status,

            @Parameter(description = "Search by client ID, full name, or email")
            @RequestParam(required = false) String search,

            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        List<PatientConsentManagementResponse> result = patientConsentQueryService.getConsentManagementList(
                consentType, status, search, userPrincipal, getClientIpAddress(request), request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/management/refresh")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(
            summary = "Refresh patient consent management list",
            description = "Forces a fresh consent-management read for the current tenant using the same filters as /management."
    )
    public ResponseEntity<List<PatientConsentManagementResponse>> refreshConsentManagementList(
            @Parameter(description = "Filter by consent type: ALL, AI_PROCESSING, DATA_SHARING, RESEARCH, MARKETING")
            @RequestParam(required = false, defaultValue = "ALL") String consentType,
            @Parameter(description = "Filter by consent status: ALL, GRANTED, DENIED / WITHDRAWN")
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @Parameter(description = "Search by client ID, full name, or email")
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        List<PatientConsentManagementResponse> result = patientConsentQueryService.getConsentManagementList(
                consentType, status, search, userPrincipal, getClientIpAddress(request), request.getHeader("User-Agent"),
                "consent_management_refreshed"
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "Get all client consents", description = "Retrieve all client consents with optional filters. Admin and Supervisor only.")
    public ResponseEntity<List<ClientConsentResponse>> getAllConsents(
            @Parameter(description = "Filter by consent type") @RequestParam(required = false) String consentType,

            @Parameter(description = "Filter by granted status (true/false)") @RequestParam(required = false) Boolean granted,

            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        List<ClientConsentResponse> result = patientConsentQueryService.getAllConsents(
                consentType, granted, userPrincipal, getClientIpAddress(request), request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "Get client consents", description = "Retrieve all consents for a specific client.")
    public ResponseEntity<List<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse>> getClientConsents(
            @Parameter(description = "Client ID") @PathVariable Long clientId,

            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        List<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> response =
                patientConsentQueryService.getClientConsents(
                        clientId, userPrincipal, getClientIpAddress(request), request.getHeader("User-Agent")
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/clients/{clientId}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "Record consent on behalf of client", description = "Creates immutable staff-recorded consent history with audit metadata.")
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> recordStaffConsent(
            @PathVariable Long clientId,
            @RequestBody StaffRecordConsentRequest requestBody,
            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        return ResponseEntity.ok(consentCommandService.recordStaffConsent(
                clientId,
                requestBody.getConsentType(),
                requestBody.getGranted(),
                requestBody.getConsentVersion(),
                requestBody.getSource(),
                requestBody.getNotes(),
                requestBody.getAuditReason(),
                userPrincipal,
                getClientIpAddress(request),
                request.getHeader("User-Agent")));
    }

    @PostMapping("/clients/{clientId}/verbal-ai-consent")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "Record verbal AI consent", description = "Therapist workflow helper endpoint to record verbal consent with immutable history.")
    public ResponseEntity<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> recordVerbalAiConsent(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "true") boolean granted,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        return ResponseEntity.ok(consentCommandService.recordStaffConsent(
                clientId,
                ConsentType.AI_PROCESSING,
                granted,
                "1.0",
                "verbal_in_session",
                notes,
                "therapist verbal ai consent",
                userPrincipal,
                getClientIpAddress(request),
                request.getHeader("User-Agent")));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
