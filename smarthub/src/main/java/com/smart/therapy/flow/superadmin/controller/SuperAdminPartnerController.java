package com.smart.therapy.flow.superadmin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminPartnerAccessRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminPartnerAccessResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformPartnerAccess;
import com.smart.therapy.flow.superadmin.service.SuperAdminPartnerAccessService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/partners")
@RequiredArgsConstructor
@Tag(name = "Super Admin Partners", description = "Partner access controls per tenant")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminPartnerController {

    private final SuperAdminPartnerAccessService partnerAccessService;
    private final ObjectMapper objectMapper;

    @PutMapping("/{partnerId}/access")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert partner access per tenant", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsert(
            @PathVariable String partnerId,
            @Valid @RequestBody SuperAdminPartnerAccessRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            List<PlatformPartnerAccess> saved = partnerAccessService.upsertAccess(
                    partnerId,
                    request.getOrganisationIds(),
                    request.getFeatureFlags(),
                    request.getActive(),
                    principal != null ? principal.getAuthId() : null
            );
            return ResponseEntity.ok(saved.stream().map(this::toResponse).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{partnerId}/access")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List partner access entries", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> list(@PathVariable String partnerId) {
        try {
            return ResponseEntity.ok(partnerAccessService.listByPartner(partnerId).stream().map(this::toResponse).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private SuperAdminPartnerAccessResponse toResponse(PlatformPartnerAccess row) {
        SuperAdminPartnerAccessResponse response = new SuperAdminPartnerAccessResponse();
        response.setId(row.getId());
        response.setPartnerId(row.getPartnerId());
        response.setOrganisationId(row.getOrganisation().getId());
        response.setActive(row.getIsActive());
        response.setCreatedAt(row.getCreatedAt());
        response.setUpdatedAt(row.getUpdatedAt());
        try {
            response.setFeatureFlags(objectMapper.readValue(row.getFeatureFlagsJson(), new TypeReference<>() {}));
        } catch (Exception e) {
            response.setFeatureFlags(Map.of());
        }
        return response;
    }

}
