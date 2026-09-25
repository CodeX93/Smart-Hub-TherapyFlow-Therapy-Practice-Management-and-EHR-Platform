package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.FeatureCatalogCreateRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminFeatureCatalogService;
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
@RequestMapping({"/api/v1/super-admin/features/catalog", "/api/super-admin/features/catalog"})
@RequiredArgsConstructor
@Tag(name = "Feature Catalog", description = "Global feature catalog management")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminFeatureCatalogController {

    private final SuperAdminFeatureCatalogService superAdminFeatureCatalogService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List feature catalog", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listCatalog(@RequestParam(defaultValue = "false") boolean includeDeprecated) {
        return ResponseEntity.ok(superAdminFeatureCatalogService.listCatalog(includeDeprecated));
    }

    @GetMapping("/{key}")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get feature catalog entry by key", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> getCatalogItem(@PathVariable String key) {
        return ResponseEntity.ok(superAdminFeatureCatalogService.getCatalogItem(key));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Export feature catalog (CSV)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> exportCatalog() {
        String csv = superAdminFeatureCatalogService.exportCatalogCsv();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=feature_catalog.csv")
                .contentType(org.springframework.http.MediaType.valueOf("text/csv"))
                .body(csv);
    }

    @PostMapping
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create feature catalog entry", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createFeature(
            @Valid @RequestBody FeatureCatalogCreateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.status(201)
                .body(superAdminFeatureCatalogService.createFeature(request, principal != null ? principal.getAuthId() : null));
    }

    @PostMapping(value = "/import", consumes = "text/csv")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Import feature catalog (CSV)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> importCatalog(
            @RequestBody String csv,
            @RequestParam(defaultValue = "false") boolean upsert,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(superAdminFeatureCatalogService.importCatalogCsv(csv, upsert, principal != null ? principal.getAuthId() : null));
    }

    @PutMapping("/bulk")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Bulk upsert feature catalog entries", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertCatalogBulk(
            @RequestBody List<FeatureCatalogCreateRequest> items,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminFeatureCatalogService.upsertCatalogBulk(items, principal != null ? principal.getAuthId() : null)
        );
    }

    @PutMapping("/{key}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update feature catalog entry", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> updateFeature(
            @PathVariable String key,
            @Valid @RequestBody FeatureCatalogCreateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminFeatureCatalogService.updateFeature(key, request, principal != null ? principal.getAuthId() : null)
        );
    }

    @PatchMapping("/{key}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Patch feature catalog entry", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> patchFeature(
            @PathVariable String key,
            @Valid @RequestBody com.smart.therapy.flow.superadmin.dto.FeatureCatalogUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminFeatureCatalogService.patchFeature(key, request, principal != null ? principal.getAuthId() : null)
        );
    }

    @GetMapping("/audit-logs")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Feature catalog audit logs", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> featureCatalogAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(superAdminFeatureCatalogService.listCatalogAuditLogs(page, size));
    }

    @GetMapping("/{key}/history")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Feature catalog change history by key", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> featureCatalogHistoryByKey(
            @PathVariable String key,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(superAdminFeatureCatalogService.listCatalogAuditLogsByKey(key, page, size));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete feature catalog entry", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> deleteFeature(
            @PathVariable String key,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        superAdminFeatureCatalogService.deleteFeature(key, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.noContent().build();
    }
}
