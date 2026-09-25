package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.superadmin.dto.SuperAdminResolveTenantRequest;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Routing", description = "Resolve tenant access from user identity")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class TenantRoutingController {

    private final TenantResolutionService tenantResolutionService;

    @PostMapping("/resolve")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Resolve accessible tenants by email", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> resolve(@Valid @RequestBody SuperAdminResolveTenantRequest request) {
        List<UserOrganisation> rows = tenantResolutionService.resolveByEmail(request.getEmail());
        List<Map<String, Object>> items = rows.stream()
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("organisationId", r.getOrganisation().getId());
                    item.put("name", r.getOrganisation().getName());
                    item.put("slug", r.getOrganisation().getSlug());
                    item.put("subdomain", r.getOrganisation().getSubdomain());
                    item.put("status", r.getOrganisation().getStatus());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(Map.of("email", request.getEmail(), "organisations", items, "count", items.size()));
    }

}
