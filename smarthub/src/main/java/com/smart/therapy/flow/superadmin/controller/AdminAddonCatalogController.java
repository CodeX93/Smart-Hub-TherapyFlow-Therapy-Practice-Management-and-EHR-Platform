package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.AddOnCatalogCreateRequest;
import com.smart.therapy.flow.superadmin.dto.AddOnCatalogItemResponse;
import com.smart.therapy.flow.superadmin.dto.AddOnCatalogUpdateRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminAddonService;
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

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping({"/api/v1/super-admin/catalog/addons", "/api/super-admin/catalog/addons"})
@RequiredArgsConstructor
@Tag(name = "Super Admin Addons", description = "Add-on catalog management")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class AdminAddonCatalogController {

    private final SuperAdminAddonService superAdminAddonService;

    @GetMapping
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "List add-on catalog", description = "Returns active add-on catalog entries with pricing.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of add-ons",
                    content = @Content(schema = @Schema(implementation = AddOnCatalogItemResponse.class)))
    })
    public ResponseEntity<List<AddOnCatalogItemResponse>> listAddons() {
        List<AddOnCatalogItemResponse> body = superAdminAddonService.listCatalogEntries().stream()
                .map(AdminAddonCatalogController::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{code}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Get add-on catalog item", description = "Fetch a single add-on entry by code.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Add-on entry",
                    content = @Content(schema = @Schema(implementation = AddOnCatalogItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Add-on not found")
    })
    public ResponseEntity<AddOnCatalogItemResponse> getAddon(@PathVariable String code) {
        return ResponseEntity.ok(toResponse(superAdminAddonService.getCatalogEntry(code)));
    }

    @PostMapping
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create add-on catalog item", description = "Creates a new add-on entry.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Add-on created",
                    content = @Content(schema = @Schema(implementation = AddOnCatalogItemResponse.class))),
            @ApiResponse(responseCode = "409", description = "Add-on code already exists")
    })
    public ResponseEntity<AddOnCatalogItemResponse> createAddon(
            @Valid @RequestBody AddOnCatalogCreateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var item = superAdminAddonService.createCatalogEntry(
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.getPriceUsd(),
                request.getBillingCycle(),
                request.getStatus(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.status(201).body(toResponse(item));
    }

    @PutMapping("/{code}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update add-on catalog item", description = "Replaces all mutable fields for the add-on entry.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Add-on updated",
                    content = @Content(schema = @Schema(implementation = AddOnCatalogItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Add-on not found")
    })
    public ResponseEntity<AddOnCatalogItemResponse> updateAddon(
            @PathVariable String code,
            @Valid @RequestBody AddOnCatalogUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var item = superAdminAddonService.upsertCatalogEntry(
                code,
                request.getName(),
                request.getDescription(),
                request.getPriceUsd(),
                request.getBillingCycle(),
                request.getStatus(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toResponse(item));
    }

    @PatchMapping("/{code}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Patch add-on catalog item", description = "Partially updates the add-on entry.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Add-on patched",
                    content = @Content(schema = @Schema(implementation = AddOnCatalogItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Add-on not found")
    })
    public ResponseEntity<AddOnCatalogItemResponse> patchAddon(
            @PathVariable String code,
            @RequestBody AddOnCatalogUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var item = superAdminAddonService.patchCatalogEntry(
                code,
                request.getName(),
                request.getDescription(),
                request.getPriceUsd(),
                request.getBillingCycle(),
                request.getStatus(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toResponse(item));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Permanently delete add-on catalog item",
            description = "Hard-deletes the add-on catalog pricing entry. Use archive/deactivate to soft-disable instead. Fails with 409 if organisations still have active purchases.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Add-on permanently deleted"),
            @ApiResponse(responseCode = "404", description = "Add-on not found"),
            @ApiResponse(responseCode = "409", description = "Add-on still assigned to organisations")
    })
    public ResponseEntity<Void> deleteAddon(
            @PathVariable String code,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        superAdminAddonService.deleteCatalogEntryPermanently(code, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/activate")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Activate add-on catalog item", description = "Re-activates an inactive add-on entry.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Add-on activated",
                    content = @Content(schema = @Schema(implementation = AddOnCatalogItemResponse.class)))
    })
    public ResponseEntity<AddOnCatalogItemResponse> activateAddon(
            @PathVariable String code,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var item = superAdminAddonService.activateCatalogEntry(code, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.ok(toResponse(item));
    }

    private static AddOnCatalogItemResponse toResponse(SuperAdminAddonService.CatalogItem item) {
        AddOnCatalogItemResponse response = new AddOnCatalogItemResponse();
        String code = item.featureCode();
        response.setId("addon_" + (code != null ? code.toLowerCase(Locale.ROOT) : "unknown"));
        response.setCode(code != null ? code.toLowerCase(Locale.ROOT) : null);
        response.setName(item.featureName());
        response.setDescription(item.description());
        response.setPriceUsd(item.pricePerUnit());
        response.setBillingCycle(item.billingCycle());
        response.setStatus(item.status());
        return response;
    }
}
