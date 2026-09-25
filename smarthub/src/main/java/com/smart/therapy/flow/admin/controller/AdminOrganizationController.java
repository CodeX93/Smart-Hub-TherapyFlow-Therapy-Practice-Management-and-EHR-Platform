package com.smart.therapy.flow.admin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.admin.dto.AdminOrganizationSearchResponse;
import com.smart.therapy.flow.admin.service.AdminOrganizationQueryService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
@PreAuthorize(RoleConstants.PLATFORM_READ)
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin Organizations", description = "Search and filter organizations")
public class AdminOrganizationController {

    private final AdminOrganizationQueryService adminOrganizationQueryService;

    @GetMapping({"/organizations", "/organisations"})
    public ResponseEntity<Object> searchOrganizations(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAtFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAtTo,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String dataResidency
    ) {
        if (createdAtFrom != null && createdAtTo != null && createdAtFrom.isAfter(createdAtTo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "createdAtFrom must be before or equal to createdAtTo"));
        }
        AdminOrganizationSearchResponse response = adminOrganizationQueryService.searchOrganizations(
                search, status, plan, createdAtFrom, createdAtTo, timezone, region, dataResidency
        );
        log.info("Admin {} searched organizations: total={}, filters=[search={}, status={}, plan={}]",
                principal != null ? principal.getLoginIdentifier() : "unknown",
                response.getTotal(),
                search,
                status,
                plan
        );
        return ResponseEntity.ok(response);
    }
}
