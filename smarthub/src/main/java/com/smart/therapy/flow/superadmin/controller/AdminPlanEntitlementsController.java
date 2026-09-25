package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.AdminPlanEntitlementsRequest;
import com.smart.therapy.flow.superadmin.dto.PlanPricingTierRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminPlanCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin/plans")
@RequiredArgsConstructor
@Tag(name = "Admin Plans", description = "Plan entitlements management")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class AdminPlanEntitlementsController {

    private final SuperAdminPlanCatalogService superAdminPlanCatalogService;

    @PutMapping("/{plan}/entitlements")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Replace plan entitlements", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan entitlements updated",
                    content = @Content(schema = @Schema(implementation = com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse> updateEntitlements(
            @PathVariable String plan,
            @Valid @RequestBody AdminPlanEntitlementsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(superAdminPlanCatalogService.updateEntitlementsFromCatalog(
                plan,
                request.getFeatures(),
                principal != null ? principal.getAuthId() : null
        ));
    }

    @GetMapping("/{plan}/entitlements")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get plan entitlements", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan entitlements",
                    content = @Content(schema = @Schema(implementation = com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse> getEntitlements(@PathVariable String plan) {
        return ResponseEntity.ok(superAdminPlanCatalogService.getEntitlementsFromCatalog(plan));
    }

    @GetMapping(value = "/{plan}/entitlements/export", produces = "text/csv")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Export plan entitlements (CSV)", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV export"),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<String> exportEntitlements(@PathVariable String plan) {
        String csv = superAdminPlanCatalogService.exportEntitlementsCsv(plan);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plan_entitlements.csv")
                .contentType(org.springframework.http.MediaType.valueOf("text/csv"))
                .body(csv);
    }

    @PostMapping(value = "/{plan}/entitlements/import", consumes = "text/csv")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Import plan entitlements (CSV)", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import result",
                    content = @Content(schema = @Schema(implementation = com.smart.therapy.flow.superadmin.dto.PlanEntitlementsImportResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<com.smart.therapy.flow.superadmin.dto.PlanEntitlementsImportResponse> importEntitlements(
            @PathVariable String plan,
            @RequestBody String csv,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminPlanCatalogService.importEntitlementsCsv(plan, csv, principal != null ? principal.getAuthId() : null)
        );
    }

    @PostMapping("/{plan}/pricing-tiers")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Replace plan pricing tiers", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pricing tiers updated",
                    content = @Content(schema = @Schema(implementation = com.smart.therapy.flow.superadmin.dto.PlanPricingTierResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<com.smart.therapy.flow.superadmin.dto.PlanPricingTierResponse> replacePricingTiers(
            @PathVariable String plan,
            @Valid @RequestBody PlanPricingTierRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminPlanCatalogService.replacePlanPricingTiers(plan, request.getTiers(),
                        principal != null ? principal.getAuthId() : null)
        );
    }

    @GetMapping("/{plan}/pricing-tiers")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get plan pricing tiers", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pricing tiers",
                    content = @Content(schema = @Schema(implementation = com.smart.therapy.flow.superadmin.dto.PlanPricingTierResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<com.smart.therapy.flow.superadmin.dto.PlanPricingTierResponse> getPricingTiers(
            @PathVariable String plan
    ) {
        return ResponseEntity.ok(superAdminPlanCatalogService.getPlanPricingTiers(plan));
    }

    @PostMapping("/{plan}/entitlements/apply-defaults")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Apply default plan entitlements", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Defaults applied",
                    content = @Content(schema = @Schema(implementation = com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse> applyDefaults(
            @PathVariable String plan,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminPlanCatalogService.applyEntitlementDefaults(plan, principal != null ? principal.getAuthId() : null)
        );
    }
}
