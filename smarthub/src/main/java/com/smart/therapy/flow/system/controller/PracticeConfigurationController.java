package com.smart.therapy.flow.system.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.system.dto.PracticeConfigurationRequest;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
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
import com.smart.therapy.flow.common.security.PermissionConstants;

@RestController
@RequestMapping("/api/v1/practice-configuration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Practice Configuration", description = "Practice-wide configuration management")
public class PracticeConfigurationController {

    private final PracticeConfigurationService practiceConfigurationService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get practice configuration",
            description = """
                    Get the current practice configuration.
                    
                    **Returns:** Practice configuration including name, address, contact info, licenses, etc.
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PracticeConfigurationResponse> getPracticeConfiguration() {
        PracticeConfigurationResponse config = practiceConfigurationService.getPracticeConfiguration();
        return ResponseEntity.ok(config);
    }

    @PutMapping
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Update practice configuration",
            description = """
                    Update the practice configuration. All fields are optional - only include fields you want to update.
                    
                    **Validation:**
                    - `practiceName`: Required, max 255 characters
                    - `practiceEmail`: Valid email format
                    - `practiceWebsite`: Valid URL format (will auto-add https:// if missing)
                    - `practicePhone`: Valid phone number format
                    - `taxId`: Format XX-XXXXXXX or 9 digits
                    - `npiNumber`: Exactly 10 digits
                    - `timezone`: Valid IANA timezone ID (e.g., America/New_York)
                    
                    **Requires:** ADMIN role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PracticeConfigurationResponse> updatePracticeConfiguration(
            @Valid @RequestBody PracticeConfigurationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        PracticeConfigurationResponse config = practiceConfigurationService.updatePracticeConfiguration(
                request, principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(config);
    }
}

