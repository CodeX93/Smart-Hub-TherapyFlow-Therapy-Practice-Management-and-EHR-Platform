package com.smart.therapy.flow.admin.controller;

import com.smart.therapy.flow.admin.dto.DirectoryEntityType;
import com.smart.therapy.flow.admin.dto.DirectoryEntryResponse;
import com.smart.therapy.flow.admin.service.AdminDirectoryService;
import com.smart.therapy.flow.common.config.AppProperties;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/directory")
@RequiredArgsConstructor
@Tag(name = "Admin Directory", description = "Reusable directory listing for tenant users and clients")
public class AdminDirectoryController {

    private final AdminDirectoryService adminDirectoryService;
    private final AppProperties appProperties;

    @GetMapping
    @PreAuthorize(
            PermissionConstants.USER_VIEW + " or " +
            PermissionConstants.CLIENT_VIEW_OWN + " or " +
            PermissionConstants.CLIENT_VIEW_TEAM + " or " +
            PermissionConstants.CLIENT_VIEW_ALL
    )
    @Operation(
            summary = "Get tenant directory list",
            description = """
                    Reusable directory endpoint for tenant-scoped lists.

                    Use `entityType=USER` to list therapists/admins/supervisors/other staff roles.
                    Use `role` (e.g. THERAPIST, ADMIN, SUPERVISOR) to filter user roles.

                    Use `entityType=CLIENT` to list clients.
                    Use `clientStatus` (e.g. ACTIVE, INACTIVE, PENDING) to filter clients.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<DirectoryEntryResponse>> getDirectory(
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page", example = "25")
            @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Entity type: USER or CLIENT", example = "USER")
            @RequestParam(defaultValue = "USER") DirectoryEntityType entityType,
            @Parameter(description = "Search by name/email/username/phone")
            @RequestParam(required = false) String search,
            @Parameter(description = "User role filter for USER entityType (THERAPIST, ADMIN, SUPERVISOR, ...)")
            @RequestParam(required = false) String role,
            @Parameter(description = "Active filter for USER entityType")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Client status filter for CLIENT entityType (ACTIVE, INACTIVE, PENDING, ...)")
            @RequestParam(required = false) String clientStatus,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int safePage = Math.max(appProperties.getPagination().getDefaultPage(), page);
        if (pageSize == null) {
            pageSize = appProperties.getPagination().getDefaultPageSize();
        }
        int safePageSize = Math.min(
                Math.max(pageSize, appProperties.getPagination().getMinPageSize()),
                appProperties.getPagination().getMaxPageSize());

        PaginatedResponse<DirectoryEntryResponse> response = adminDirectoryService.listDirectory(
                entityType,
                safePage,
                safePageSize,
                search,
                role,
                active,
                clientStatus,
                principal);

        return ResponseEntity.ok(response);
    }
}
