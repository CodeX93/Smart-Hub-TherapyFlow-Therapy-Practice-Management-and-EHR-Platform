package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.common.security.RoleConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/permissions")
@RequiredArgsConstructor
@Tag(name = "Super Admin Permissions", description = "Permission catalog for custom role builders")
@ApiResponses({
        @ApiResponse(responseCode = "403", description = "Forbidden")
})
public class SuperAdminPermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List all permissions", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listPermissions() {
        var body = permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getName() != null ? p.getName() : ""))
                .map(p -> Map.<String, Object>of(
                        "permissionId", p.getId(),
                        "name", p.getName(),
                        "displayName", p.getDisplayName(),
                        "category", p.getCategory(),
                        "active", Boolean.TRUE.equals(p.getIsActive())
                ))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/catalog")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List permissions grouped by category", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listPermissionsCatalog() {
        List<Map<String, Object>> items = permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getName() != null ? p.getName() : ""))
                .map(p -> Map.<String, Object>of(
                        "permissionId", p.getId(),
                        "name", p.getName(),
                        "displayName", p.getDisplayName(),
                        "category", p.getCategory(),
                        "active", Boolean.TRUE.equals(p.getIsActive())
                ))
                .toList();

        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String category = item.get("category") != null ? String.valueOf(item.get("category")) : "Uncategorized";
            grouped.computeIfAbsent(category, key -> new java.util.ArrayList<>()).add(item);
        }

        return ResponseEntity.ok(Map.of(
                "items", items,
                "grouped", grouped
        ));
    }
}
