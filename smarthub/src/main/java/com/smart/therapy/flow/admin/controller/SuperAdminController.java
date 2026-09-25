package com.smart.therapy.flow.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import com.smart.therapy.flow.organisation.service.TenantFlywayMigrator;
import com.smart.therapy.flow.subscription.entity.BillingContact;
import com.smart.therapy.flow.subscription.entity.BillingNotificationLog;
import com.smart.therapy.flow.subscription.entity.BillingNotificationTemplate;
import com.smart.therapy.flow.subscription.entity.BillingExportJob;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.InvoiceAdjustment;
import com.smart.therapy.flow.subscription.entity.InvoiceDispute;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationListService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationQueryService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOnboardingService;
import com.smart.therapy.flow.superadmin.service.SuperAdminPlanCatalogService;
import com.smart.therapy.flow.superadmin.service.SuperAdminSubscriptionService;
import com.smart.therapy.flow.superadmin.service.SuperAdminAddonService;
import com.smart.therapy.flow.superadmin.service.SuperAdminTenantOpsFacade;
import com.smart.therapy.flow.superadmin.service.SuperAdminBillingService;
import com.smart.therapy.flow.superadmin.service.SuperAdminUsageService;
import com.smart.therapy.flow.superadmin.service.SuperAdminUserService;
import com.smart.therapy.flow.superadmin.service.SuperAdminRoleService;
import com.smart.therapy.flow.superadmin.service.OrganisationFeatureOverrideService;
import com.smart.therapy.flow.superadmin.service.SuperAdminImpersonationService;
import com.smart.therapy.flow.superadmin.service.SuperAdminTenantStaffBootstrapService;
import com.smart.therapy.flow.superadmin.service.SuperAdminClinicalTemplateBackfillService;
import com.smart.therapy.flow.payment.dto.TenantStripeConfigResponse;
import com.smart.therapy.flow.payment.dto.TenantStripeConfigUpsertRequest;
import com.smart.therapy.flow.payment.service.StripeTenantConfigService;
import com.smart.therapy.flow.superadmin.dto.*;
import com.smart.therapy.flow.superadmin.dto.SuperAdminHipaaAuditLogResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOnboardingCreateOrganisationRequest;
import com.smart.therapy.flow.superadmin.dto.OrganisationImpersonationRequest;
import com.smart.therapy.flow.superadmin.entity.FeatureRolloutRule;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationSession;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Platform super-admin API. Operates above tenants (public schema only).
 * Use admin.yourapp.com or X-Tenant-Schema: public so no tenant is set.
 * Read-only (list, get, health, schema-version, features, audit-logs): PLATFORM_SUPER_ADMIN or PLATFORM_AUDITOR.
 * Write (create, update, provision, lock, backup, features): PLATFORM_SUPER_ADMIN only.
 */
@RestController
@RequestMapping("/api/v1/super-admin")
@PreAuthorize(RoleConstants.PLATFORM_READ)
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Super Admin", description = "Platform-level operations (admin.yourapp.com, no tenant)")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminController {

    private final SuperAdminOrganisationListService superAdminOrganisationListService;
    private final SuperAdminOrganisationQueryService superAdminOrganisationQueryService;
    private final SuperAdminSubscriptionService superAdminSubscriptionService;
    private final SuperAdminOnboardingService superAdminOnboardingService;
    private final SuperAdminPlanCatalogService superAdminPlanCatalogService;
    private final SuperAdminAddonService superAdminAddonService;
    private final SuperAdminTenantOpsFacade superAdminTenantOpsFacade;
    private final SuperAdminBillingService superAdminBillingService;
    private final SuperAdminUsageService superAdminUsageService;
    private final SuperAdminUserService superAdminUserService;
    private final OrganisationFeatureOverrideService organisationFeatureOverrideService;
    private final SuperAdminImpersonationService superAdminImpersonationService;
    private final SuperAdminTenantStaffBootstrapService superAdminTenantStaffBootstrapService;
    private final SuperAdminClinicalTemplateBackfillService superAdminClinicalTemplateBackfillService;
    private final TenantFlywayMigrator tenantFlywayMigrator;
    private final StripeTenantConfigService stripeTenantConfigService;
    private final SuperAdminRoleService superAdminRoleService;
    private final ObjectMapper objectMapper;

    @GetMapping("/organisations")
    @Operation(summary = "List and filter organisations", description = "Paginated organisation listing with filters and sorting.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged list of organisations"),
            @ApiResponse(responseCode = "400", description = "Invalid filters"),
            @ApiResponse(responseCode = "403", description = "Requires PLATFORM_SUPER_ADMIN or PLATFORM_AUDITOR")
    })
    public ResponseEntity<Object> listOrganisations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String dataResidency,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "25") Integer pageSize,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false, defaultValue = "false") Boolean export,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminOrganisationListRequest req = new SuperAdminOrganisationListRequest();
        req.setSearch(search);
        req.setStatus(status);
        req.setPlan(plan);
        req.setCreatedFrom(createdFrom);
        req.setCreatedTo(createdTo);
        req.setRegion(region);
        req.setDataResidency(dataResidency);
        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSort(sort);
        req.setOrder(order);
        req.setExportCsv(Boolean.TRUE.equals(export));

        return superAdminOrganisationQueryService.listOrganisations(req, Boolean.TRUE.equals(export));
    }

    private static AuditLogEntryResponse toAuditLogResponse(PlatformAuditLog log) {
        AuditLogEntryResponse r = new AuditLogEntryResponse();
        r.setId(log.getId());
        r.setAuthId(log.getAuthId());
        r.setAction(log.getAction());
        r.setResourceType(log.getResourceType());
        r.setResourceId(log.getResourceId());
        r.setDetails(log.getDetails());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }

    @GetMapping("/organisations/{id}")
    @Operation(summary = "Get organisation by ID", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<OrganisationResponse> getOrganisation(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return superAdminOrganisationQueryService.getOrganisation(id)
                .map(org -> toResponseWithSubscription(id, org))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/organisations")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create organisation (atomic onboarding)",
            description = "Creates organisation + primary admin identity + subscription in one transaction and queues tenant provisioning.",
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = SuperAdminOnboardingCreateOrganisationRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organisation created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Slug/email conflict"),
            @ApiResponse(responseCode = "422", description = "Plan/region/trial validation failed"),
            @ApiResponse(responseCode = "403", description = "Requires PLATFORM_SUPER_ADMIN")
    })
    public ResponseEntity<Object> createOrganisation(
            @Valid @RequestBody SuperAdminOnboardingCreateOrganisationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        log.info("Super-admin {} onboarding organisation: {}", principal.getLoginIdentifier(), request.getName());
        Organisation org = superAdminOnboardingService.onboard(request, principal != null ? principal.getAuthId() : null);
        String provisioningStatus = (request.getProvisionTenant() == null || request.getProvisionTenant()) ? "PENDING" : "SKIPPED";
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(org, Map.of("provisioningStatus", provisioningStatus)));
    }

    @GetMapping("/organisations/slugs/{slug}/available")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Check slug availability", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> checkSlugAvailability(@PathVariable String slug) {
        boolean available = superAdminOrganisationQueryService.isSlugAvailable(slug);
        return ResponseEntity.ok(Map.of("slug", slug, "available", available));
    }

    @PatchMapping("/organisations/{id}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update organisation (subdomain is immutable)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> updateOrganisation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (updates.containsKey("subdomain")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Subdomain is immutable and cannot be changed after creation"));
        }
        try {
            return superAdminOrganisationQueryService.updateOrganisation(id, updates, principal != null ? principal.getAuthId() : null)
                    .map(saved -> {
                        OrganisationResponse response = toResponse(saved);
                        superAdminOrganisationQueryService.findPrimaryAdminEmail(id)
                                .ifPresent(response::setPrimaryAdminEmail);
                        return ResponseEntity.ok((Object) response);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/organisations/{id}/health")
    @Operation(summary = "Get tenant health", description = "Checks org exists, schema exists, schema version.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health status"),
            @ApiResponse(responseCode = "404", description = "Organisation not found")
    })
    public ResponseEntity<Map<String, Object>> getOrganisationHealth(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return superAdminOrganisationQueryService.getOrganisationHealth(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/organisations/{id}/settings")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update tenant settings (residency, timezone, branding)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> updateTenantSettings(
            @PathVariable Long id,
            @Valid @RequestBody SuperAdminTenantSettingsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return superAdminOrganisationQueryService.updateTenantSettings(id, request, principal != null ? principal.getAuthId() : null)
                .map(SuperAdminController::toResponse)
                .map(response -> ResponseEntity.ok((Object) response))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organisations/{id}/settings")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get tenant settings (residency, timezone, branding)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminTenantSettingsResponse> getTenantSettings(
            @PathVariable Long id
    ) {
        return superAdminOrganisationQueryService.getOrganisation(id)
                .map(SuperAdminController::toSettingsResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organisations/{id}/stripe-config")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get tenant Stripe config", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<TenantStripeConfigResponse> getTenantStripeConfig(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(stripeTenantConfigService.getConfig(id));
    }

    @PutMapping("/organisations/{id}/stripe-config")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update tenant Stripe config", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<TenantStripeConfigResponse> upsertTenantStripeConfig(
            @PathVariable Long id,
            @RequestBody TenantStripeConfigUpsertRequest request
    ) {
        return ResponseEntity.ok(stripeTenantConfigService.upsertConfig(id, request));
    }

    @GetMapping("/organisations/{id}/schema-version")
    @Operation(summary = "Get tenant schema version", description = "Which migration version this tenant schema is on.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> getSchemaVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return superAdminOrganisationQueryService.getSchemaVersion(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List platform audit logs (read-only)", description = "Paginated platform audit trail. Allowed for PLATFORM_AUDITOR and PLATFORM_SUPER_ADMIN.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AuditLogEntryResponse>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long authId,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false, defaultValue = "all") String logLevel,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long effectiveAuthId = authId;
        if (mine) {
            if (principal == null || principal.getAuthId() == null) {
                throw new StoryApiException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Authenticated principal required for mine=true");
            }
            effectiveAuthId = principal.getAuthId();
        }
        List<AuditLogEntryResponse> body = superAdminOrganisationQueryService.listAuditLogResponses(
                page,
                size,
                effectiveAuthId,
                action,
                resourceType,
                resourceId,
                logLevel,
                q,
                createdFrom,
                createdTo,
                sort,
                order
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/organisations/{id}/features")
    @Operation(summary = "Get effective feature flags for organisation", description = "Returns merged feature state (catalog + plan + org override).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, FeatureValueResponse>> getOrganisationFeatures(
            @PathVariable("id") String organisationKey,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long id = resolveOrganisationId(organisationKey);
        Map<String, OrganisationFeatureOverrideService.FeatureState> effective = organisationFeatureOverrideService.getEffectiveFeatures(id);
        Map<String, FeatureValueResponse> body = new LinkedHashMap<>();
        effective.forEach((k, v) -> body.put(k, new FeatureValueResponse(Boolean.TRUE.equals(v.enabled()), v.usageLimit())));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/hipaa-audit-logs")
    @Operation(summary = "List HIPAA audit logs across tenant organisations", description = "Aggregates tenant schema HIPAA logs. Platform super-admin/auditor only.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminHipaaAuditLogResponse>> listHipaaAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long organisationId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false, defaultValue = "all") String logLevel,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (organisationId != null && !superAdminOrganisationQueryService.organisationExists(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }

        List<SuperAdminHipaaAuditLogResponse> body = superAdminOrganisationQueryService.listHipaaAuditLogsAcrossTenants(
                page, size, organisationId, username, action, resourceType, logLevel, riskLevel, result, startDate, endDate);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/organisations/{id}/features/list")
    @Operation(summary = "Get effective feature flags as list", description = "UI-friendly list shape for feature state.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<FeatureFlagItemResponse>> getOrganisationFeaturesList(
            @PathVariable("id") String organisationKey,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long id = resolveOrganisationId(organisationKey);
        Map<String, OrganisationFeatureOverrideService.FeatureState> effective = organisationFeatureOverrideService.getEffectiveFeatures(id);
        List<FeatureFlagItemResponse> body = effective.entrySet().stream()
                .map(entry -> new FeatureFlagItemResponse(
                        entry.getKey(),
                        Boolean.TRUE.equals(entry.getValue().enabled()),
                        entry.getValue().usageLimit()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/organisations/{id}/features/details")
    @Operation(summary = "Get effective feature flags with module, ACL, and usage limitation metadata",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<FeatureFlagDetailedResponse>> getOrganisationFeatureDetails(
            @PathVariable("id") String organisationKey,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long id = resolveOrganisationId(organisationKey);
        Map<String, OrganisationFeatureOverrideService.FeatureState> effective = organisationFeatureOverrideService.getEffectiveFeatures(id);
        List<Permission> permissions = superAdminRoleService.getPermissionCatalog();
        List<FeatureFlagDetailedResponse> body = effective.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String module = inferModule(entry.getKey());
                    CoreFeature feature = CoreFeature.fromCode(entry.getKey());
                    return new FeatureFlagDetailedResponse(
                            entry.getKey(),
                            module,
                            inferAccessControlList(module, permissions),
                            Boolean.TRUE.equals(entry.getValue().enabled()),
                            entry.getValue().usageLimit(),
                            buildUsageLimitations(feature, entry.getValue().usageLimit())
                    );
                })
                .toList();
        return ResponseEntity.ok(body);
    }

    @PutMapping("/organisations/{id}/features")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Replace feature overrides for organisation", description = "Replaces org override set atomically.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, FeatureValueResponse>> setOrganisationFeatures(
            @PathVariable("id") String organisationKey,
            @RequestBody JsonNode body,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long id = resolveOrganisationId(organisationKey);
        Map<String, OrganisationFeatureOverrideService.FeatureState> request = parseFeatureOverrideBody(body);
        Map<String, OrganisationFeatureOverrideService.FeatureState> effective = organisationFeatureOverrideService.replaceOverrides(
                id,
                request,
                principal != null ? principal.getAuthId() : null
        );
        Map<String, FeatureValueResponse> response = new LinkedHashMap<>();
        effective.forEach((k, v) -> response.put(k, new FeatureValueResponse(Boolean.TRUE.equals(v.enabled()), v.usageLimit())));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organisations/{id}/sso/settings")
    @Operation(summary = "Get SSO settings for organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> getOrganisationSsoSettings(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminOrganisationQueryService.getOrganisationSsoSettings(id));
    }

    @PutMapping("/organisations/{id}/sso/domains")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Replace SSO allowed domains for organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> setOrganisationSsoDomains(
            @PathVariable Long id,
            @RequestBody SsoAllowedDomainsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(superAdminOrganisationQueryService.setOrganisationSsoDomains(
                id,
                request != null ? request.getDomains() : List.of(),
                principal != null ? principal.getAuthId() : null
        ));
    }

    @PutMapping("/organisations/{id}/sso/providers/{provider}/redirect-uri")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update SSO redirect URI for provider", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> setOrganisationSsoRedirectUri(
            @PathVariable Long id,
            @PathVariable String provider,
            @RequestBody SsoRedirectUriRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(superAdminOrganisationQueryService.setOrganisationSsoRedirectUri(
                id,
                provider,
                request != null ? request.getRedirectUri() : null,
                principal != null ? principal.getAuthId() : null
        ));
    }

    @GetMapping("/organisations/{id}/rollouts")
    @Operation(summary = "List rollout overrides for organisation",
            description = "Returns dynamic feature overrides for ORGANISATION and THERAPIST scopes.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<RolloutRuleResponse>> getOrganisationRollouts(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (!superAdminSubscriptionService.organisationExists(id)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        List<RolloutRuleResponse> body = superAdminSubscriptionService.listRollouts(id).stream()
                .map(SuperAdminController::toRolloutRuleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/organisations/{id}/rollouts")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create or update rollout overrides",
            description = "Super-admin only. Scope can be ORGANISATION or THERAPIST.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertOrganisationRollouts(
            @PathVariable Long id,
            @RequestBody JsonNode request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SuperAdminSubscriptionService.RolloutRuleInput> inputs = parseRolloutRequest(request);
        boolean canonicalSingle = request.has("featureKey");
        SuperAdminSubscriptionService.UpsertRolloutResult result = superAdminSubscriptionService.upsertRollouts(
                id,
                inputs,
                principal != null ? principal.getAuthId() : null
        );
        if (!result.success()) {
            if (result.invalidScope()) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_SCOPE", result.error());
            }
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", result.error());
        }
        List<RolloutRuleResponse> saved = result.rules().stream()
                .map(SuperAdminController::toRolloutRuleResponse)
                .collect(Collectors.toList());
        if (canonicalSingle) {
            return ResponseEntity.ok(saved.isEmpty() ? null : saved.get(0));
        }
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/organisations/{id}/rollouts/{ruleId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete rollout override", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> deleteOrganisationRollout(
            @PathVariable Long id,
            @PathVariable Long ruleId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (!superAdminSubscriptionService.organisationExists(id)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        superAdminSubscriptionService.deleteRollout(id, ruleId, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/billing/dunning-policy")
    @Operation(summary = "Get effective dunning policy",
            description = "Returns global dunning policy.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<DunningPolicyResponse> getDunningPolicy(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(toDunningPolicyResponse(superAdminSubscriptionService.getEffectiveDunningPolicy()));
    }

    @PutMapping("/billing/dunning-policy")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create or update dunning policy",
            description = "Set policy globally (organisationId omitted) or for a specific organisation.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<DunningPolicyResponse> upsertDunningPolicy(
            @Valid @RequestBody UpsertDunningPolicyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService.DunningStep> steps = request.getSteps().stream()
                .map(step -> new com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService.DunningStep(
                        step.getDay(),
                        com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService.DunningStep.Action.valueOf(step.getAction().name())
                ))
                .collect(Collectors.toList());
        var policy = superAdminSubscriptionService.upsertDunningPolicy(
                steps,
                request.getGracePeriodDays(),
                request.getTrialNoticeDays(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toDunningPolicyResponse(policy));
    }

    @GetMapping("/organisations/{id}/billing/contacts")
    @Operation(summary = "List organisation billing contacts",
            description = "Contacts used for trial expiry and dunning notifications.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listBillingContacts(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (!superAdminBillingService.organisationExists(id)) {
            return ResponseEntity.notFound().build();
        }
        List<BillingContactResponse> body = superAdminBillingService.listBillingContacts(id).stream()
                .map(SuperAdminController::toBillingContactResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/organisations/{id}/billing/contacts")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert organisation billing contact",
            description = "Adds a billing notification recipient for this organisation.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertBillingContact(
            @PathVariable Long id,
            @RequestBody UpsertBillingContactRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.UpsertBillingContactResult result = superAdminBillingService.upsertBillingContact(
                id,
                request.getEmail(),
                request.getFullName(),
                request.getPrimary(),
                request.getActive(),
                principal != null ? principal.getAuthId() : null
        );
        if (result.notFound()) {
            return ResponseEntity.notFound().build();
        }
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of("error", result.error()));
        }
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(toBillingContactResponse(result.contact()));
    }

    @DeleteMapping("/organisations/{id}/billing/contacts/{contactId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Deactivate billing contact",
            description = "Soft-removes billing contact from notification flow.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> removeBillingContact(
            @PathVariable Long id,
            @PathVariable Long contactId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.RemoveBillingContactResult result = superAdminBillingService.removeBillingContact(
                id,
                contactId,
                principal != null ? principal.getAuthId() : null
        );
        if (result.orgNotFound()) {
            return ResponseEntity.notFound().build();
        }
        if (result.contactNotFound()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Billing contact not found"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/billing/notification-templates")
    @Operation(summary = "List billing notification templates",
            description = "Templates used for trial and dunning email notifications.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<BillingNotificationTemplateResponse>> listBillingNotificationTemplates(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<BillingNotificationTemplateResponse> body = superAdminBillingService.listTemplates().stream()
                .map(SuperAdminController::toBillingTemplateResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/billing/notification-templates/{eventKey}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert billing notification template",
            description = "Updates message subject/body used by billing lifecycle notifications.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertBillingNotificationTemplate(
            @PathVariable String eventKey,
            @RequestBody UpsertBillingNotificationTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.UpsertTemplateResult result = superAdminBillingService.upsertTemplate(
                eventKey,
                request.getSubjectTemplate(),
                request.getBodyTemplate(),
                request.getActive(),
                principal != null ? principal.getAuthId() : null
        );
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of("error", result.error()));
        }
        return ResponseEntity.ok(toBillingTemplateResponse(result.template()));
    }

    @GetMapping("/organisations/{id}/billing/notification-logs")
    @Operation(summary = "List billing notification delivery logs",
            description = "Latest email delivery logs for billing lifecycle events.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listBillingNotificationLogs(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.ListBillingLogsResult result = superAdminBillingService.listLogs(id);
        if (result.notFound()) {
            return ResponseEntity.notFound().build();
        }
        List<BillingNotificationLogResponse> body = result.logs().stream()
                .map(SuperAdminController::toBillingLogResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/billing/lifecycle/run")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Run lifecycle jobs immediately",
            description = "Executes trial notice, trial expiry and dunning processing now.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> runBillingLifecycleNow(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Map<String, Integer> lifecycle = superAdminSubscriptionService.runBillingLifecycle(
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(Map.of(
                "trialNotices", lifecycle.getOrDefault("notices", 0),
                "trialExpiredToPastDue", lifecycle.getOrDefault("expired", 0),
                "dunningProcessed", lifecycle.getOrDefault("dunning", 0)
        ));
    }

    @GetMapping("/organisations/{id}/users")
    @Operation(summary = "List users by organisation",
            description = "Organisation-scoped user listing for super-admin panel.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> listOrganisationUsers(
            @PathVariable("id") String organisationKey,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        Long id = resolveOrganisationId(organisationKey);
        var result = superAdminUserService.listUsers(
                search,
                role,
                null,
                status,
                id,
                null,
                null,
                null,
                page,
                pageSize
        );
        List<Map<String, Object>> items = result.getContent().stream()
                .map(superAdminUserService::buildUserDetails)
                .toList();
        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", result.getNumber(),
                "pageSize", result.getSize(),
                "total", result.getTotalElements()
        ));
    }

    @GetMapping("/organisations/{id}/admins")
    @Operation(summary = "List organisation admins", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> listOrganisationAdmins(
            @PathVariable("id") String organisationKey,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        Long id = resolveOrganisationId(organisationKey);
        var result = superAdminUserService.listUsers(
                search,
                "ADMIN",
                null,
                status,
                id,
                null,
                null,
                null,
                page,
                pageSize
        );
        List<Map<String, Object>> items = result.getContent().stream()
                .map(superAdminUserService::buildUserDetails)
                .toList();
        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", result.getNumber(),
                "pageSize", result.getSize(),
                "total", result.getTotalElements()
        ));
    }

    @PostMapping("/organisations/{organisationKey}/impersonate")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Start impersonation for organisation target user",
            description = "Supports organisation id or slug in path (e.g. /organisations/12/impersonate or /organisations/acme/impersonate).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> startOrganisationImpersonation(
            @PathVariable String organisationKey,
            @Valid @RequestBody OrganisationImpersonationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            Long organisationId = resolveOrganisationId(organisationKey);
            SuperAdminImpersonationService.CreatedImpersonation created = superAdminImpersonationService.startSession(
                    principal != null ? principal.getAuthId() : null,
                    request.getTargetAuthId(),
                    organisationId,
                    request.getReason(),
                    request.getDurationMinutes()
            );
            PlatformImpersonationSession s = created.session();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "sessionId", s.getId(),
                    "organisationId", s.getOrganisation().getId(),
                    "targetAuthId", s.getTargetAuthId(),
                    "status", s.getStatus(),
                    "expiresAt", s.getExpiresAt(),
                    "impersonationToken", created.rawToken()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/organisations/{id}/audit-logs")
    @Operation(summary = "List organisation audit logs", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AuditLogEntryResponse>> listOrganisationAuditLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false, defaultValue = "all") String logLevel,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        if (!superAdminOrganisationQueryService.organisationExists(id)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        List<AuditLogEntryResponse> body = superAdminOrganisationQueryService.listAuditLogResponses(
                page,
                size,
                null,
                action,
                resourceType,
                String.valueOf(id),
                logLevel,
                q,
                createdFrom,
                createdTo,
                sort,
                order
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/organisations/{id}/usage")
    @Operation(summary = "Get usage for organisation",
            description = "Usage snapshot for a single organisation.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminUsageResponse> getOrganisationUsage(
            @PathVariable Long id,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "targetKey", required = false) String targetKey
    ) {
        return ResponseEntity.ok(superAdminUsageService.getUsage(id, period, targetKey));
    }

    @GetMapping("/organisations/{id}/invoices")
    @Operation(summary = "List invoices by organisation",
            description = "Organisation-scoped invoice listing for super-admin panel.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> listOrganisationInvoices(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        var result = superAdminBillingService.listInvoices(id, status, sort, page, pageSize);
        List<InvoiceAdminResponse> items = result.getContent().stream()
                .map(SuperAdminController::toInvoiceResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", result.getNumber(),
                "pageSize", result.getSize(),
                "total", result.getTotalElements()
        ));
    }

    @GetMapping("/billing/invoices")
    @Operation(summary = "List invoices (global or by organisation)",
            description = "Enterprise billing control-plane endpoint for super admin.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> listInvoices(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long organisationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long resolvedOrgId = orgId != null ? orgId : organisationId;
        var result = superAdminBillingService.listInvoices(resolvedOrgId, status, sort, page, pageSize);
        List<InvoiceAdminResponse> items = result.getContent().stream()
                .map(SuperAdminController::toInvoiceResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", result.getNumber(),
                "pageSize", result.getSize(),
                "total", result.getTotalElements()
        ));
    }

    @GetMapping("/billing/invoices/{invoiceId}")
    @Operation(summary = "Get subscription invoice detail",
            description = "Returns a single platform subscription invoice with organisation/plan context, adjustments, and disputes.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<InvoiceAdminDetailResponse> getInvoiceDetail(@PathVariable Long invoiceId) {
        Invoice invoice = superAdminBillingService.getInvoiceDetailed(invoiceId);
        InvoiceAdminDetailResponse detail = toInvoiceDetailResponse(invoice);
        SuperAdminBillingService.ListInvoiceAdjustmentsResult adjustments =
                superAdminBillingService.listInvoiceAdjustments(invoiceId);
        detail.setAdjustments(adjustments.adjustments().stream()
                .map(SuperAdminController::toInvoiceAdjustmentResponse)
                .collect(Collectors.toList()));
        SuperAdminBillingService.ListInvoiceDisputesResult disputes =
                superAdminBillingService.listInvoiceDisputes(invoiceId);
        detail.setDisputes(disputes.disputes().stream()
                .map(SuperAdminController::toInvoiceDisputeResponse)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/billing/invoices/{invoiceId}/pdf")
    @Operation(summary = "Download subscription invoice PDF",
            description = "Generates a PDF for a platform subscription invoice.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long invoiceId) {
        byte[] pdf = superAdminBillingService.generateInvoicePdf(invoiceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "subscription-invoice-" + invoiceId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PostMapping("/billing/invoices/{invoiceId}/send-reminder")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Send invoice payment reminder",
            description = "Emails organisation admins and billing contacts, and creates in-app notifications for tenant admins to pay the subscription invoice.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> sendInvoicePaymentReminder(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.InvoiceReminderResult result = superAdminBillingService.sendInvoicePaymentReminder(
                invoiceId,
                principal != null ? principal.getAuthId() : null
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("invoiceId", result.invoiceId());
        body.put("organisationId", result.organisationId());
        body.put("emailsSent", result.emailsSent());
        body.put("emailsFailed", result.emailsFailed());
        body.put("inAppNotificationsCreated", result.inAppNotificationsCreated());
        body.put("emailRecipients", result.emailRecipients());
        if (result.warning() != null) {
            body.put("warning", result.warning());
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/billing/invoices/recent")
    @Operation(summary = "List recent invoices",
            description = "Returns latest invoices for global super-admin billing dashboard.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> listRecentInvoices(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long organisationId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long resolvedOrgId = orgId != null ? orgId : organisationId;
        int safeLimit = Math.max(1, Math.min(limit, 100));
        var result = superAdminBillingService.listInvoices(resolvedOrgId, null, "createdat_desc", 0, safeLimit);
        List<InvoiceAdminResponse> items = result.getContent().stream()
                .map(SuperAdminController::toInvoiceResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "items", items,
                "total", result.getTotalElements()
        ));
    }

    @GetMapping("/billing/invoices/export")
    @Operation(summary = "Export invoices as CSV",
            description = "Exports invoice ledger for finance/revenue analysis.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<String> exportInvoicesCsv(
            @RequestParam(required = false) Long organisationId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<Invoice> invoices = superAdminBillingService.listInvoicesForExport(organisationId, status);

        StringBuilder csv = new StringBuilder();
        csv.append("invoiceId,organisationId,subscriptionId,status,amount,dueDate,billingPeriodStart,billingPeriodEnd,createdAt\n");
        for (Invoice i : invoices) {
            csv.append(i.getId()).append(",")
                    .append(i.getSubscription().getOrganisation().getId()).append(",")
                    .append(i.getSubscription().getId()).append(",")
                    .append(safeCsv(i.getStatus() != null ? i.getStatus().name() : null)).append(",")
                    .append(i.getAmount()).append(",")
                    .append(i.getDueDate()).append(",")
                    .append(i.getBillingPeriodStart()).append(",")
                    .append(i.getBillingPeriodEnd()).append(",")
                    .append(i.getCreatedAt())
                    .append("\n");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"billing-invoices.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.toString());
    }

    @GetMapping("/billing/invoices/{invoiceId}/adjustments")
    @Operation(summary = "List invoice adjustments",
            description = "Returns credits/refunds recorded for this invoice.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listInvoiceAdjustments(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.ListInvoiceAdjustmentsResult result = superAdminBillingService.listInvoiceAdjustments(invoiceId);
        if (result.notFound()) {
            return ResponseEntity.notFound().build();
        }
        List<InvoiceAdjustmentResponse> body = new ArrayList<>(result.adjustments().stream()
                .map(SuperAdminController::toInvoiceAdjustmentResponse)
                .collect(Collectors.toList()));
        SuperAdminBillingService.ListInvoiceDisputesResult disputes = superAdminBillingService.listInvoiceDisputes(invoiceId);
        body.addAll(disputes.disputes().stream().map(d -> {
            InvoiceAdjustmentResponse r = new InvoiceAdjustmentResponse();
            r.setAdjustmentId(d.getId());
            r.setInvoiceId(d.getInvoice().getId());
            r.setType("DISPUTE");
            r.setAmount(d.getAmountUsd());
            r.setReason(d.getReason());
            r.setStatus(d.getStatus().name());
            r.setCreatedAt(d.getCreatedAt());
            return r;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/billing/invoices/{invoiceId}/credits")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Apply credit to invoice",
            description = "Creates CREDIT adjustment for enterprise invoicing workflow.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> applyInvoiceCredit(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceAdjustmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var adjustment = superAdminBillingService.applyCredit(
                invoiceId,
                request.getAmountUsd(),
                request.getReason(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toInvoiceAdjustmentResponse(adjustment));
    }

    @PostMapping("/billing/invoices/{invoiceId}/refunds")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Apply refund to invoice",
            description = "Creates REFUND adjustment for enterprise dispute/refund workflow.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> applyInvoiceRefund(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceAdjustmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var adjustment = superAdminBillingService.applyRefund(
                invoiceId,
                request.getAmountUsd(),
                request.getReason(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toInvoiceAdjustmentResponse(adjustment));
    }

    @GetMapping("/billing/invoices/{invoiceId}/disputes")
    @Operation(summary = "List invoice disputes",
            description = "Returns dispute cases for the invoice.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listInvoiceDisputes(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.ListInvoiceDisputesResult result = superAdminBillingService.listInvoiceDisputes(invoiceId);
        if (result.notFound()) {
            return ResponseEntity.notFound().build();
        }
        List<InvoiceDisputeResponse> body = result.disputes().stream()
                .map(SuperAdminController::toInvoiceDisputeResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/billing/invoices/{invoiceId}/disputes")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Open invoice dispute",
            description = "Creates dispute case for enterprise billing operations.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> openInvoiceDispute(
            @PathVariable Long invoiceId,
            @Valid @RequestBody UpsertInvoiceDisputeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.DisputeActionResult result = superAdminBillingService.openDispute(
                invoiceId,
                request.getExternalCaseId(),
                request.getAmountUsd(),
                request.getReason(),
                request.getStatus(),
                principal != null ? principal.getAuthId() : null
        );
        if (result.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return ResponseEntity.notFound().build();
        }
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of("error", result.error()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toInvoiceDisputeResponse(result.dispute()));
    }

    @PutMapping("/billing/invoices/{invoiceId}/disputes/{disputeId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update invoice dispute",
            description = "Updates dispute status and resolution details.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> updateInvoiceDispute(
            @PathVariable Long invoiceId,
            @PathVariable Long disputeId,
            @Valid @RequestBody UpsertInvoiceDisputeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.DisputeActionResult result = superAdminBillingService.updateDispute(
                invoiceId,
                disputeId,
                request.getExternalCaseId(),
                request.getAmountUsd(),
                request.getReason(),
                request.getStatus(),
                principal != null ? principal.getAuthId() : null
        );
        if (!result.success() && result.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Dispute not found"));
        }
        return ResponseEntity.ok(toInvoiceDisputeResponse(result.dispute()));
    }

    @GetMapping("/billing/revenue-report")
    @Operation(summary = "Revenue report",
            description = "Returns MRR/ARR/churn and grouped movements for selected period (YYYY-MM).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam String period,
            @RequestParam(defaultValue = "plan") String groupBy,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.RevenueReportResult report = superAdminBillingService.getRevenueReport(period, groupBy);

        RevenueReportResponse response = new RevenueReportResponse();
        response.setPeriod(report.period());
        response.setGroupBy(report.groupBy());
        response.setMrr(report.mrr());
        response.setArr(report.arr());
        response.setChurnRate(report.churnRate());
        response.setMovements(report.movements().stream().map(row -> {
            RevenueReportResponse.RevenueMovement movement = new RevenueReportResponse.RevenueMovement();
            movement.setKey(row.key());
            movement.setAmount(row.amount());
            movement.setPaidInvoices(row.paidInvoices());
            return movement;
        }).toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/billing/revenue-analytics")
    @Operation(summary = "Revenue analytics (MRR/ARR/Churn)",
            description = "Returns monthly analytics for enterprise SaaS billing.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<RevenueAnalyticsResponse> getRevenueAnalytics(
            @RequestParam(defaultValue = "12") @Min(1) @Max(24) Integer months,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.RevenueAnalyticsResult analytics = superAdminBillingService.getRevenueAnalytics(months);
        List<RevenueAnalyticsMonth> rows = analytics.rows().stream().map(rowData -> {
            RevenueAnalyticsMonth row = new RevenueAnalyticsMonth();
            row.setMonth(rowData.month());
            row.setMrr(rowData.mrr());
            row.setArr(rowData.arr());
            row.setActiveSubscriptions(rowData.activeSubscriptions());
            row.setEndedSubscriptions(rowData.endedSubscriptions());
            row.setChurnRatePct(rowData.churnRatePct());
            return row;
        }).collect(Collectors.toList());

        RevenueAnalyticsResponse response = new RevenueAnalyticsResponse();
        response.setMonths(analytics.months());
        response.setGeneratedAt(analytics.generatedAt());
        response.setRows(rows);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/billing/exports")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create async billing export job",
            description = "Creates queued export job and returns signed download token reference.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createBillingExportJob(
            @Valid @RequestBody CreateBillingExportJobRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.CreateExportJobResult result = superAdminBillingService.createExportJob(
                principal != null ? principal.getAuthId() : null,
                request.getExportType(),
                request.getOrganisationId(),
                request.getStatus(),
                request.getMonths()
        );
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of("error", result.error()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toExportJobResponse(result.job(), result.downloadToken()));
    }

    @GetMapping("/billing/exports/{jobId}")
    @Operation(summary = "Get billing export job status",
            description = "Returns queued/in_progress/completed/failed status and metadata.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> getBillingExportJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        BillingExportJob job = superAdminBillingService.getExportJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toExportJobResponse(job, null));
    }

    @GetMapping("/billing/exports/{jobId}/download")
    @Operation(summary = "Download completed billing export",
            description = "Requires export job id + signed token returned by create/status endpoint.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> downloadBillingExport(
            @PathVariable Long jobId,
            @RequestParam String token,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminBillingService.DownloadExportResult result = superAdminBillingService.downloadExport(jobId, token);
        if (!result.success()) {
            if (result.statusCode() == HttpStatus.FORBIDDEN.value()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", result.error()));
            }
            if (result.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", result.error()));
            }
            if (result.statusCode() == HttpStatus.GONE.value()) {
                return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", result.error()));
            }
            if (result.statusCode() == HttpStatus.CONFLICT.value()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", result.error()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", result.error()));
        }
        BillingExportJob job = result.job();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + (job.getFileName() != null ? job.getFileName() : "export.csv") + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(result.bytes().length)
                .body(result.bytes());
    }

    @GetMapping("/billing/addon-catalog")
    @Operation(summary = "List add-on catalog",
            description = "Returns feature catalog with current per-unit pricing for add-ons.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AddonCatalogResponse>> listAddonCatalog(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AddonCatalogResponse> items = superAdminAddonService.listCatalog().stream()
                .map(item -> {
                    AddonCatalogResponse r = new AddonCatalogResponse();
                    r.setFeatureCode(item.featureCode());
                    r.setFeatureName(item.featureName());
                    r.setDescription(item.description());
                    r.setPricePerUnit(item.pricePerUnit());
                    r.setBillingCycle(item.billingCycle());
                    r.setUnitValue(item.unitValue());
                    r.setStatus(item.status());
                    return r;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @PutMapping("/billing/addon-catalog/{featureCode}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert add-on catalog price",
            description = "Sets per-unit price for a feature in add-on catalog.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertAddonPrice(
            @PathVariable String featureCode,
            @Valid @RequestBody UpsertAddonCatalogRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminAddonService.CatalogItem item = superAdminAddonService.upsertCatalogPrice(
                featureCode,
                request.getPricePerUnit(),
                request.getBillingCycle(),
                request.getUnitValue(),
                request.getStatus(),
                principal != null ? principal.getAuthId() : null
        );
        AddonCatalogResponse response = new AddonCatalogResponse();
        response.setFeatureCode(item.featureCode());
        response.setFeatureName(item.featureName());
        response.setDescription(item.description());
        response.setPricePerUnit(item.pricePerUnit());
        response.setBillingCycle(item.billingCycle());
        response.setUnitValue(item.unitValue());
        response.setStatus(item.status());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/billing/addon-catalog/{featureCode}/archive")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Archive add-on catalog entry",
            description = "Marks add-on catalog entry as inactive (archived).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<AddonCatalogResponse> archiveAddonCatalogEntry(
            @PathVariable String featureCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminAddonService.CatalogItem item = superAdminAddonService.deactivateCatalogEntry(
                featureCode,
                principal != null ? principal.getAuthId() : null
        );
        AddonCatalogResponse response = new AddonCatalogResponse();
        response.setFeatureCode(item.featureCode());
        response.setFeatureName(item.featureName());
        response.setDescription(item.description());
        response.setPricePerUnit(item.pricePerUnit());
        response.setBillingCycle(item.billingCycle());
        response.setUnitValue(item.unitValue());
        response.setStatus(item.status());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/billing/addon-catalog/{featureCode}/unarchive")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Unarchive add-on catalog entry",
            description = "Restores add-on catalog entry to active state.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<AddonCatalogResponse> unarchiveAddonCatalogEntry(
            @PathVariable String featureCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminAddonService.CatalogItem item = superAdminAddonService.activateCatalogEntry(
                featureCode,
                principal != null ? principal.getAuthId() : null
        );
        AddonCatalogResponse response = new AddonCatalogResponse();
        response.setFeatureCode(item.featureCode());
        response.setFeatureName(item.featureName());
        response.setDescription(item.description());
        response.setPricePerUnit(item.pricePerUnit());
        response.setBillingCycle(item.billingCycle());
        response.setUnitValue(item.unitValue());
        response.setStatus(item.status());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organisations/{id}/addons")
    @Operation(summary = "List organisation add-ons",
            description = "Returns add-on purchases tied to the organisation current subscription.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listOrganisationAddons(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SuperAdminAddonService.OrgAddonItem> serviceItems = superAdminAddonService.listOrganisationAddons(id);
        List<OrgAddonResponse> items = serviceItems.stream()
                .map(p -> {
                    OrgAddonResponse r = new OrgAddonResponse();
                    r.setPurchaseId(p.purchaseId());
                    r.setFeatureCode(p.featureCode());
                    r.setFeatureName(p.featureName());
                    r.setQuantity(p.quantity());
                    r.setPricePerUnitAtTime(p.pricePerUnitAtTime());
                    r.setUnitValue(p.unitValue());
                    r.setBillingCycle(p.billingCycle());
                    r.setStartAt(p.startAt());
                    r.setEndAt(p.endAt());
                    return r;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/organisations/{id}/addons")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Assign add-on to organisation",
            description = "Adds additive feature purchase to current organisation subscription.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> assignAddonToOrganisation(
            @PathVariable Long id,
            @Valid @RequestBody UpsertOrgAddonRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminAddonService.OrgAddonItem item = superAdminAddonService.assignAddon(
                id,
                request.getFeatureCode(),
                request.getQuantity(),
                principal != null ? principal.getAuthId() : null
        );
        OrgAddonResponse response = new OrgAddonResponse();
        response.setPurchaseId(item.purchaseId());
        response.setFeatureCode(item.featureCode());
        response.setFeatureName(item.featureName());
        response.setQuantity(item.quantity());
        response.setPricePerUnitAtTime(item.pricePerUnitAtTime());
        response.setUnitValue(item.unitValue());
        response.setBillingCycle(item.billingCycle());
        response.setStartAt(item.startAt());
        response.setEndAt(item.endAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/organisations/{id}/addons/{purchaseId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Unassign organisation add-on by purchase id",
            description = "Ends an active add-on purchase (sets endAt=now). Use this before permanently deleting a catalog add-on that is still assigned.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<OrgAddonResponse> unassignOrganisationAddon(
            @PathVariable Long id,
            @PathVariable Long purchaseId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminAddonService.OrgAddonItem item = superAdminAddonService.unassignAddon(
                id,
                purchaseId,
                principal != null ? principal.getAuthId() : null
        );
        OrgAddonResponse response = new OrgAddonResponse();
        response.setPurchaseId(item.purchaseId());
        response.setFeatureCode(item.featureCode());
        response.setFeatureName(item.featureName());
        response.setQuantity(item.quantity());
        response.setPricePerUnitAtTime(item.pricePerUnitAtTime());
        response.setUnitValue(item.unitValue());
        response.setBillingCycle(item.billingCycle());
        response.setStartAt(item.startAt());
        response.setEndAt(item.endAt());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/organisations/{id}/addons")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Unassign organisation add-on by feature code",
            description = "Ends all active purchases of the given featureCode on the organisation's current subscription.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<OrgAddonResponse>> unassignOrganisationAddonByFeatureCode(
            @PathVariable Long id,
            @RequestParam String featureCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<OrgAddonResponse> items = superAdminAddonService.unassignAddonByFeatureCode(
                        id,
                        featureCode,
                        principal != null ? principal.getAuthId() : null
                ).stream()
                .map(p -> {
                    OrgAddonResponse r = new OrgAddonResponse();
                    r.setPurchaseId(p.purchaseId());
                    r.setFeatureCode(p.featureCode());
                    r.setFeatureName(p.featureName());
                    r.setQuantity(p.quantity());
                    r.setPricePerUnitAtTime(p.pricePerUnitAtTime());
                    r.setUnitValue(p.unitValue());
                    r.setBillingCycle(p.billingCycle());
                    r.setStartAt(p.startAt());
                    r.setEndAt(p.endAt());
                    return r;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @PutMapping("/plans/{planName}/entitlements")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Edit plan entitlements (catalog keys)",
            description = "Replaces plan entitlements using feature catalog keys (app_features), aligned with GET /plans/{planName}/entitlements.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanEntitlementsResponse> updatePlanEntitlements(
            @PathVariable String planName,
            @RequestBody JsonNode request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<PlanEntitlementItem> payloadItems = parsePlanEntitlementItems(request);
        List<AdminPlanEntitlementsRequest.PlanFeatureItem> items = payloadItems.stream().map(item -> {
            AdminPlanEntitlementsRequest.PlanFeatureItem next = new AdminPlanEntitlementsRequest.PlanFeatureItem();
            next.setKey(item.resolveFeatureCode());
            next.setEnabled(item.getEnabled());
            next.setLimit(item.getUsageLimit());
            return next;
        }).collect(Collectors.toList());

        PlanEntitlementsResponse updated = superAdminPlanCatalogService.updateEntitlementsFromCatalog(
                planName,
                items,
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/plans/{planName}/entitlements")
    @Operation(summary = "Get plan entitlements",
            description = "Returns current plan entitlements for a specific plan.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanEntitlementsResponse> getPlanEntitlements(
            @PathVariable String planName
    ) {
        return ResponseEntity.ok(superAdminPlanCatalogService.getEntitlementsFromCatalog(planName));
    }

    @GetMapping("/plans")
    @Operation(summary = "List plans and pricing",
            description = "Returns plan catalog with base/annual pricing and billing cycle.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<PlanPricingResponse>> listPlans(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SubscriptionPlan> plans = superAdminPlanCatalogService.listPlans();
        List<Long> planIds = plans.stream().map(SubscriptionPlan::getId).toList();
        var tiersByPlan = superAdminPlanCatalogService.listPricingTiersForPlans(planIds);
        var entitlementsByPlan = superAdminPlanCatalogService.listEntitlementsForPlans(planIds);

        List<PlanPricingResponse> body = plans.stream()
                .map(plan -> {
                    PlanPricingResponse r = new PlanPricingResponse();
                    r.setPlanCode(plan.getCode());
                    r.setPlanName(plan.getName());
                    r.setDescription(plan.getDescription());
                    r.setBasePrice(plan.getBasePrice());
                    r.setAnnualPrice(plan.getAnnualPrice());
                    r.setBillingCycle(plan.getBillingCycle());
                    r.setTrialDays(plan.getTrialDays());
                    r.setProviderPriceIdMonthly(plan.getProviderPriceIdMonthly());
                    r.setProviderPriceIdAnnual(plan.getProviderPriceIdAnnual());
                    r.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
                    r.setPricingTiers(tiersByPlan.getOrDefault(plan.getId(), List.of()));
                    r.setEntitlements(entitlementsByPlan.getOrDefault(plan.getId(), List.of()));
                    applyPlanUserLimits(r, r.getEntitlements());
                    return r;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/plans")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create plan", description = "Creates a new subscription plan.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> createPlan(
            @Valid @RequestBody CreatePlanRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SubscriptionPlan plan = superAdminPlanCatalogService.createPlan(
                request.getCode(),
                request.getName(),
                request.getDescription(),
                request.getBasePrice(),
                request.getAnnualPrice(),
                request.getBillingCycle(),
                request.getTrialDays(),
                request.getStatus(),
                request.getProviderPriceIdMonthly(),
                request.getProviderPriceIdAnnual(),
                principal != null ? principal.getAuthId() : null
        );
        PlanPricingResponse response = new PlanPricingResponse();
        response.setPlanCode(plan.getCode());
        response.setPlanName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setBasePrice(plan.getBasePrice());
        response.setAnnualPrice(plan.getAnnualPrice());
        response.setBillingCycle(plan.getBillingCycle());
        response.setTrialDays(plan.getTrialDays());
        response.setProviderPriceIdMonthly(plan.getProviderPriceIdMonthly());
        response.setProviderPriceIdAnnual(plan.getProviderPriceIdAnnual());
        response.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        applyPlanUserLimits(response, getPlanEntitlementRows(plan));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/plans/{planCode}")
    @Operation(summary = "Get plan", description = "Returns plan details by code or name.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> getPlan(@PathVariable String planCode) {
        SubscriptionPlan plan = superAdminPlanCatalogService.getPlanByCodeOrName(planCode);
        var tiers = superAdminPlanCatalogService.getPlanPricingTiers(plan.getCode()).getTiers();
        var entitlements = superAdminPlanCatalogService.listCurrentEntitlements(plan.getId()).stream()
                .map(ent -> {
                    PlanEntitlementResponse row = new PlanEntitlementResponse();
                    row.setPlanName(plan.getName());
                    row.setFeatureCode(ent.getFeature().getCode());
                    row.setEnabled(Boolean.TRUE.equals(ent.getIsEnabled()));
                    row.setUsageLimit(ent.getUsageLimit());
                    row.setTrialAvailable(Boolean.TRUE.equals(ent.getIsTrialAvailable()));
                    row.setEffectiveFrom(ent.getEffectiveFrom());
                    return row;
                }).collect(Collectors.toList());
        PlanPricingResponse response = new PlanPricingResponse();
        response.setPlanCode(plan.getCode());
        response.setPlanName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setBasePrice(plan.getBasePrice());
        response.setAnnualPrice(plan.getAnnualPrice());
        response.setBillingCycle(plan.getBillingCycle());
        response.setTrialDays(plan.getTrialDays());
        response.setProviderPriceIdMonthly(plan.getProviderPriceIdMonthly());
        response.setProviderPriceIdAnnual(plan.getProviderPriceIdAnnual());
        response.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        response.setPricingTiers(tiers);
        response.setEntitlements(entitlements);
        applyPlanUserLimits(response, entitlements);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/plans/{planCode}/details")
    @Operation(summary = "Get plan details", description = "Returns plan details with entitlements and pricing tiers.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> getPlanDetails(@PathVariable String planCode) {
        return getPlan(planCode);
    }

    @PutMapping("/plans/{planName}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Edit plan and pricing",
            description = "Updates plan description, monthly price, annual price, and default billing cycle.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> updatePlan(
            @PathVariable String planName,
            @Valid @RequestBody UpdatePlanRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SubscriptionPlan plan = superAdminPlanCatalogService.updatePlanDetails(
                planName,
                request.getName(),
                request.getDescription(),
                request.getBasePrice(),
                request.getAnnualPrice(),
                request.getBillingCycle(),
                request.getTrialDays(),
                request.getStatus(),
                request.getProviderPriceIdMonthly(),
                request.getProviderPriceIdAnnual(),
                principal != null ? principal.getAuthId() : null
        );
        PlanPricingResponse response = new PlanPricingResponse();
        response.setPlanCode(plan.getCode());
        response.setPlanName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setBasePrice(plan.getBasePrice());
        response.setAnnualPrice(plan.getAnnualPrice());
        response.setBillingCycle(plan.getBillingCycle());
        response.setTrialDays(plan.getTrialDays());
        response.setProviderPriceIdMonthly(plan.getProviderPriceIdMonthly());
        response.setProviderPriceIdAnnual(plan.getProviderPriceIdAnnual());
        response.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        applyPlanUserLimits(response, getPlanEntitlementRows(plan));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/plans/{planCode}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Archive plan", description = "Archives a plan (soft delete).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> deletePlan(
            @PathVariable String planCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SubscriptionPlan plan = superAdminPlanCatalogService.archivePlan(planCode, principal != null ? principal.getAuthId() : null);
        PlanPricingResponse response = new PlanPricingResponse();
        response.setPlanCode(plan.getCode());
        response.setPlanName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setBasePrice(plan.getBasePrice());
        response.setAnnualPrice(plan.getAnnualPrice());
        response.setBillingCycle(plan.getBillingCycle());
        response.setTrialDays(plan.getTrialDays());
        response.setProviderPriceIdMonthly(plan.getProviderPriceIdMonthly());
        response.setProviderPriceIdAnnual(plan.getProviderPriceIdAnnual());
        response.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        applyPlanUserLimits(response, getPlanEntitlementRows(plan));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/plans/{planCode}/default")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Apply default entitlements to plan",
            description = "Resets plan entitlements to the platform default template.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanEntitlementsResponse> setPlanDefault(
            @PathVariable String planCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(
                superAdminPlanCatalogService.applyEntitlementDefaults(planCode, principal != null ? principal.getAuthId() : null)
        );
    }

    @PutMapping("/plans/{planCode}/archive")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Archive plan",
            description = "Soft archives a plan.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> archivePlan(
            @PathVariable String planCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return deletePlan(planCode, principal);
    }

    @PutMapping("/plans/{planCode}/unarchive")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Unarchive plan",
            description = "Restores an archived plan to active status.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlanPricingResponse> unarchivePlan(
            @PathVariable String planCode,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SubscriptionPlan plan = superAdminPlanCatalogService.updatePlanDetails(
                planCode,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                null,
                null,
                principal != null ? principal.getAuthId() : null
        );
        PlanPricingResponse response = new PlanPricingResponse();
        response.setPlanCode(plan.getCode());
        response.setPlanName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setBasePrice(plan.getBasePrice());
        response.setAnnualPrice(plan.getAnnualPrice());
        response.setBillingCycle(plan.getBillingCycle());
        response.setTrialDays(plan.getTrialDays());
        response.setProviderPriceIdMonthly(plan.getProviderPriceIdMonthly());
        response.setProviderPriceIdAnnual(plan.getProviderPriceIdAnnual());
        response.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        applyPlanUserLimits(response, getPlanEntitlementRows(plan));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/organisations/{id}/provision")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Provision tenant schema for organisation",
            description = "Queues tenant schema provisioning by default. Pass sync=true to run the full "
                    + "reprovision immediately (template clone, Flyway, and default seeds) — same as the scheduler.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Provisioning queued"),
            @ApiResponse(responseCode = "200", description = "Synchronous reprovision completed"),
            @ApiResponse(responseCode = "404", description = "Organisation not found"),
            @ApiResponse(responseCode = "400", description = "Invalid org (e.g. schema_name is public)")
    })
    public ResponseEntity<Map<String, Object>> provisionTenant(
            @PathVariable Long id,
            @RequestParam(name = "sync", defaultValue = "false") boolean sync,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        log.info("Super-admin {} {} tenant provisioning for organisation {}",
                principal.getLoginIdentifier(), sync ? "running synchronous" : "queueing and triggering", id);
        if (sync) {
            return toReprovisionResponse(superAdminTenantOpsFacade.reprovisionSynchronously(
                    id,
                    principal != null ? principal.getAuthId() : null
            ));
        }
        SuperAdminTenantOpsFacade.ProvisionResult result = superAdminTenantOpsFacade.queueAndTriggerProvisioning(
                id,
                principal != null ? principal.getAuthId() : null
        );
        if (result.success()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("success", true, "organisationId", id, "status", "PENDING", "message", "Tenant schema provisioning queued and migration triggered"));
        }
        if (result.statusCode() == 400) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", result.error()));
        }
        if (result.statusCode() == 500) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", result.error()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", "Unexpected provisioning result"));
    }

    @PostMapping("/organisations/{id}/reprovision")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Reprovision tenant schema synchronously",
            description = "Runs template clone (if needed), Flyway tenant migrations, and default seeds "
                    + "(staff profiles, system options, library, billing services). Same pipeline as auto-migration.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reprovision completed"),
            @ApiResponse(responseCode = "404", description = "Organisation not found"),
            @ApiResponse(responseCode = "400", description = "Invalid org"),
            @ApiResponse(responseCode = "500", description = "Migration or seed failed")
    })
    public ResponseEntity<Map<String, Object>> reprovisionTenant(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        log.info("Super-admin {} reprovisioning organisation {}", principal.getLoginIdentifier(), id);
        return toReprovisionResponse(superAdminTenantOpsFacade.reprovisionSynchronously(
                id,
                principal != null ? principal.getAuthId() : null
        ));
    }

    private ResponseEntity<Map<String, Object>> toReprovisionResponse(SuperAdminTenantOpsFacade.ReprovisionResult result) {
        if (result.success() && result.migration() != null) {
            TenantFlywayMigrator.TenantMigrationResult migration = result.migration();
            TenantFlywayMigrator.TenantSeedResult seeds = migration.seeds();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "organisationId", result.organisationId(),
                    "migrationStatus", migration.migrationStatus(),
                    "version", migration.version(),
                    "staffProfilesCreated", seeds.staffProfiles(),
                    "systemOptionRecordsCreated", seeds.systemOptions(),
                    "libraryRecordsCreated", seeds.libraryRecords(),
                    "billingServicesCreated", seeds.billingServices(),
                    "clinicalTemplateRecordsCreated", seeds.clinicalTemplateRecords(),
                    "message", "Tenant reprovisioned with migrations and default seeds"));
        }
        if (result.statusCode() == 400) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "organisationId", result.organisationId(),
                    "error", result.error()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "organisationId", result.organisationId(),
                "error", result.error() != null ? result.error() : "Reprovision failed"));
    }

    @PostMapping("/organisations/{id}/tenant-defaults/seed")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Seed tenant default catalogs",
            description = "Loads system options, clinical library, billing services, clinical templates, and staff profiles for a tenant. "
                    + "Use after manual schema provisioning when Flyway post-migration seeds were skipped.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> seedOrganisationTenantDefaults(@PathVariable Long id) {
        Organisation org = superAdminOrganisationQueryService.getOrganisation(id).orElse(null);
        if (org == null || org.getSchemaName() == null || org.getSchemaName().isBlank()
                || "public".equalsIgnoreCase(org.getSchemaName())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "organisationId", id,
                    "error", "Organisation or tenant schema not found"));
        }
        TenantFlywayMigrator.TenantSeedResult result =
                tenantFlywayMigrator.seedTenantDefaults(org);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "organisationId", org.getId(),
                "schemaName", org.getSchemaName(),
                "staffProfilesCreated", result.staffProfiles(),
                "systemOptionRecordsCreated", result.systemOptions(),
                "libraryRecordsCreated", result.libraryRecords(),
                "billingServicesCreated", result.billingServices(),
                "clinicalTemplateRecordsCreated", result.clinicalTemplateRecords()));
    }

    @PostMapping("/organisations/{id}/clinical-templates/backfill")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Backfill clinical templates for organisation",
            description = "Seeds missing assessment/form/checklist/report templates from ClientHub defaults, "
                    + "and uploads report template .docx files into TherapyFlow storage when missing.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> backfillOrganisationClinicalTemplates(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            SuperAdminClinicalTemplateBackfillService.BackfillResult result =
                    superAdminClinicalTemplateBackfillService.backfillOne(
                            id, principal != null ? principal.getAuthId() : null);
            if (!result.success()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "organisationId", result.organisationId(),
                        "schemaName", result.schemaName(),
                        "error", result.error()
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "organisationId", result.organisationId(),
                    "schemaName", result.schemaName(),
                    "created", result.created()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "organisationId", id,
                    "error", "Clinical template backfill failed: "
                            + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
            ));
        }
    }

    @PostMapping("/organisations/clinical-templates/backfill")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Backfill clinical templates for all organisations",
            description = "Seeds ClientHub-derived clinical templates into existing tenant schemas (bounded by limit).",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> backfillClinicalTemplatesBulk(
            @RequestParam(defaultValue = "200") int limit,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            SuperAdminClinicalTemplateBackfillService.BackfillBatchResult result =
                    superAdminClinicalTemplateBackfillService.backfillAll(
                            limit, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "processed", result.processed(),
                    "successful", result.success(),
                    "failed", result.failed(),
                    "created", result.created(),
                    "results", result.results()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "processed", 0,
                    "successful", 0,
                    "failed", 0,
                    "created", 0,
                    "error", "Bulk clinical template backfill failed: "
                            + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
            ));
        }
    }

    @PostMapping("/organisations/{id}/staff-profiles/bootstrap")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Bootstrap tenant staff profiles for organisation",
            description = "Creates missing tenant users records from public auth identities linked to this organisation.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> bootstrapOrganisationStaffProfiles(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            SuperAdminTenantStaffBootstrapService.BootstrapResult result =
                    superAdminTenantStaffBootstrapService.bootstrapOne(id, principal != null ? principal.getAuthId() : null);
            if (!result.success()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "organisationId", result.organisationId(),
                        "schemaName", result.schemaName(),
                        "error", result.error()
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "organisationId", result.organisationId(),
                    "schemaName", result.schemaName(),
                    "created", result.created()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "organisationId", id,
                    "error", "Bootstrap failed: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
            ));
        }
    }

    @PostMapping("/organisations/staff-profiles/bootstrap")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Bootstrap tenant staff profiles in bulk",
            description = "Repairs missing tenant users records across organisations.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> bootstrapStaffProfilesBulk(
            @RequestParam(defaultValue = "200") int limit,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            SuperAdminTenantStaffBootstrapService.BootstrapBatchResult result =
                    superAdminTenantStaffBootstrapService.bootstrapAll(limit, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "processed", result.processed(),
                    "successful", result.success(),
                    "failed", result.failed(),
                    "created", result.created(),
                    "results", result.results()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "processed", 0,
                    "successful", 0,
                    "failed", 0,
                    "created", 0,
                    "error", "Bulk bootstrap failed: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
            ));
        }
    }

    @PostMapping("/organisations/{id}/lock")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Lock tenant for maintenance (stub)", description = "Design placeholder. Implement to set maintenance flag / block requests for this tenant.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> lockTenantForMaintenance(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminTenantOpsFacade.LockResult result = superAdminTenantOpsFacade.lockForMaintenance(
                id,
                principal != null ? principal.getAuthId() : null
        );
        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "organisationId", id,
                    "status", result.status(),
                    "message", "Tenant locked for maintenance"
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", result.error()));
    }

    @PostMapping("/organisations/{id}/backup")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Trigger tenant backup (stub)", description = "Design placeholder. Implement to trigger pg_dump -n tenant_XX or external backup.",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> triggerTenantBackup(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminTenantOpsFacade.BackupResult result = superAdminTenantOpsFacade.requestBackup(
                id,
                principal != null ? principal.getAuthId() : null
        );
        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "organisationId", id,
                    "jobId", result.jobId(),
                    "status", result.status(),
                    "message", "Tenant backup job queued"
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", result.error()));
    }

    private static OrganisationResponse toResponse(Organisation org) {
        return toResponse(org, null);
    }

    private OrganisationResponse toResponseWithSubscription(Long organisationId, Organisation org) {
        OrganisationResponse response = toResponse(org, null);
        response.setTotalUserCount(superAdminUserService.countUsersByOrganisation(organisationId));
        superAdminOrganisationQueryService.findPrimaryAdminEmail(organisationId)
                .ifPresent(response::setPrimaryAdminEmail);
        try {
            SuperAdminSubscriptionService.StrictSubscriptionView strictSubscription = superAdminSubscriptionService.getStrictSubscription(organisationId);
            response.setPlanInfo(strictSubscription.plan());
            response.setSubscriptionDetails(toSubscriptionDetailsResponse(strictSubscription));
        } catch (RuntimeException ex) {
            // Legacy orgs may have no current subscription; keep org payload available.
            log.warn("Organisation {} subscription details unavailable: {}", organisationId, ex.getMessage());
        }
        return response;
    }

    private static RolloutRuleResponse toRolloutRuleResponse(FeatureRolloutRule rule) {
        RolloutRuleResponse r = new RolloutRuleResponse();
        r.setId(rule.getId());
        r.setOrganisationId(rule.getOrganisation().getId());
        r.setScope(rule.getScope().name());
        r.setTargetId(rule.getTargetId());
        r.setTargetKey(rule.getTargetKey());
        r.setFeatureKey(rule.getFeatureKey());
        r.setEnabled(Boolean.TRUE.equals(rule.getEnabled()));
        r.setUsageLimit(rule.getUsageLimit());
        r.setStartAt(rule.getStartAt());
        r.setEndAt(rule.getEndAt());
        r.setUpdatedAt(rule.getUpdatedAt());
        return r;
    }

    private Map<String, OrganisationFeatureOverrideService.FeatureState> parseFeatureOverrideBody(JsonNode body) {
        if (body == null || body.isNull()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "features is required");
        }
        LinkedHashMap<String, OrganisationFeatureOverrideService.FeatureState> parsed = new LinkedHashMap<>();
        JsonNode featuresNode = body.has("features") ? body.get("features") : body;
        if (!featuresNode.isObject()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "features must be an object");
        }
        Iterator<Map.Entry<String, JsonNode>> fields = featuresNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();
            if (value.isBoolean()) {
                parsed.put(key, new OrganisationFeatureOverrideService.FeatureState(value.asBoolean(), null));
                continue;
            }
            FeatureValueRequest state = objectMapper.convertValue(value, FeatureValueRequest.class);
            parsed.put(key, new OrganisationFeatureOverrideService.FeatureState(state.getEnabled(), state.getUsageLimit()));
        }
        return parsed;
    }

    private List<PlanEntitlementItem> parsePlanEntitlementItems(JsonNode body) {
        if (body == null || body.isNull()) {
            return List.of();
        }
        try {
            if (body.isArray()) {
                return objectMapper.readerForListOf(PlanEntitlementItem.class).readValue(body);
            }
            UpsertPlanEntitlementsRequest wrapped = objectMapper.treeToValue(body, UpsertPlanEntitlementsRequest.class);
            return wrapped != null && wrapped.resolveItems() != null ? wrapped.resolveItems() : List.of();
        } catch (Exception ex) {
            throw new StoryApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Malformed entitlements payload. Expected array or {\"items\":[...]} / {\"features\":[...]}"
            );
        }
    }

    private List<SuperAdminSubscriptionService.RolloutRuleInput> parseRolloutRequest(JsonNode request) {
        List<RolloutRuleRequest> rules = new ArrayList<>();
        if (request.has("rules") && request.get("rules").isArray()) {
            for (JsonNode ruleNode : request.get("rules")) {
                rules.add(objectMapper.convertValue(ruleNode, RolloutRuleRequest.class));
            }
        } else if (request.isObject()) {
            rules.add(objectMapper.convertValue(request, RolloutRuleRequest.class));
        } else {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid rollout payload");
        }

        return rules.stream().map(ruleRequest -> new SuperAdminSubscriptionService.RolloutRuleInput(
                ruleRequest.getScope(),
                ruleRequest.getTargetId(),
                ruleRequest.getTargetKey(),
                ruleRequest.getFeatureKey(),
                ruleRequest.getEnabled(),
                ruleRequest.getUsageLimit(),
                ruleRequest.getStartAt(),
                ruleRequest.getEndAt()
        )).collect(Collectors.toList());
    }

    @lombok.Data
    private static class SsoAllowedDomainsRequest {
        private List<String> domains;
    }

    @lombok.Data
    private static class SsoRedirectUriRequest {
        private String redirectUri;
    }

    private static InvoiceAdminResponse toInvoiceResponse(Invoice invoice) {
        InvoiceAdminResponse r = new InvoiceAdminResponse();
        r.setInvoiceId(invoice.getId());
        if (invoice.getSubscription() != null) {
            r.setSubscriptionId(invoice.getSubscription().getId());
            r.setBillingCycle(invoice.getSubscription().getBillingCycleAtTime());
            if (invoice.getSubscription().getOrganisation() != null) {
                r.setOrganisationId(invoice.getSubscription().getOrganisation().getId());
                r.setOrganisationName(invoice.getSubscription().getOrganisation().getName());
            }
            if (invoice.getSubscription().getPlan() != null) {
                r.setPlanCode(invoice.getSubscription().getPlan().getCode());
                r.setPlanName(invoice.getSubscription().getPlan().getName());
            }
        }
        r.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        r.setAmount(invoice.getAmount());
        r.setOutstandingBalance(invoice.getOutstandingBalance());
        r.setTotalPaid(invoice.getTotalPaid());
        r.setRefundedAmount(invoice.getRefundedAmount());
        r.setDueDate(invoice.getDueDate());
        r.setBillingPeriodStart(invoice.getBillingPeriodStart());
        r.setBillingPeriodEnd(invoice.getBillingPeriodEnd());
        r.setPaidAt(invoice.getPaidAt());
        r.setCreatedAt(invoice.getCreatedAt());
        r.setProviderInvoiceId(invoice.getProviderInvoiceId());
        r.setProviderChargeId(invoice.getProviderChargeId());
        r.setProviderPaymentIntentId(invoice.getProviderPaymentIntentId());
        return r;
    }

    private static InvoiceAdminDetailResponse toInvoiceDetailResponse(Invoice invoice) {
        InvoiceAdminDetailResponse detail = new InvoiceAdminDetailResponse();
        InvoiceAdminResponse base = toInvoiceResponse(invoice);
        detail.setInvoiceId(base.getInvoiceId());
        detail.setOrganisationId(base.getOrganisationId());
        detail.setOrganisationName(base.getOrganisationName());
        detail.setSubscriptionId(base.getSubscriptionId());
        detail.setPlanCode(base.getPlanCode());
        detail.setPlanName(base.getPlanName());
        detail.setBillingCycle(base.getBillingCycle());
        detail.setStatus(base.getStatus());
        detail.setAmount(base.getAmount());
        detail.setOutstandingBalance(base.getOutstandingBalance());
        detail.setTotalPaid(base.getTotalPaid());
        detail.setRefundedAmount(base.getRefundedAmount());
        detail.setDueDate(base.getDueDate());
        detail.setBillingPeriodStart(base.getBillingPeriodStart());
        detail.setBillingPeriodEnd(base.getBillingPeriodEnd());
        detail.setPaidAt(base.getPaidAt());
        detail.setCreatedAt(base.getCreatedAt());
        detail.setProviderInvoiceId(base.getProviderInvoiceId());
        detail.setProviderChargeId(base.getProviderChargeId());
        detail.setProviderPaymentIntentId(base.getProviderPaymentIntentId());
        return detail;
    }

    private static String safeCsv(String v) {
        if (v == null) {
            return "";
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static InvoiceAdjustmentResponse toInvoiceAdjustmentResponse(InvoiceAdjustment adjustment) {
        InvoiceAdjustmentResponse r = new InvoiceAdjustmentResponse();
        r.setAdjustmentId(adjustment.getId());
        r.setInvoiceId(adjustment.getInvoice().getId());
        r.setType(adjustment.getAdjustmentType() != null ? adjustment.getAdjustmentType().name() : null);
        r.setAmount(adjustment.getAmount());
        r.setReason(adjustment.getReason());
        r.setStatus(adjustment.getStatus() != null ? adjustment.getStatus().name() : null);
        r.setCreatedAt(adjustment.getCreatedAt());
        return r;
    }

    private static InvoiceDisputeResponse toInvoiceDisputeResponse(InvoiceDispute dispute) {
        InvoiceDisputeResponse r = new InvoiceDisputeResponse();
        r.setDisputeId(dispute.getId());
        r.setInvoiceId(dispute.getInvoice().getId());
        r.setExternalCaseId(dispute.getExternalCaseId());
        r.setStatus(dispute.getStatus() != null ? dispute.getStatus().name() : null);
        r.setAmountUsd(dispute.getAmountUsd());
        r.setReason(dispute.getReason());
        r.setOpenedAt(dispute.getOpenedAt());
        r.setResolvedAt(dispute.getResolvedAt());
        return r;
    }

    private static BillingContactResponse toBillingContactResponse(BillingContact contact) {
        BillingContactResponse r = new BillingContactResponse();
        r.setContactId(contact.getId());
        r.setOrganisationId(contact.getOrganisation().getId());
        r.setFullName(contact.getFullName());
        r.setEmail(contact.getEmail());
        r.setPrimary(Boolean.TRUE.equals(contact.getIsPrimary()));
        r.setActive(Boolean.TRUE.equals(contact.getIsActive()));
        r.setCreatedAt(contact.getCreatedAt());
        r.setUpdatedAt(contact.getUpdatedAt());
        return r;
    }

    private static BillingNotificationTemplateResponse toBillingTemplateResponse(BillingNotificationTemplate t) {
        BillingNotificationTemplateResponse r = new BillingNotificationTemplateResponse();
        r.setTemplateId(t.getId());
        r.setEventKey(t.getEventKey());
        r.setSubjectTemplate(t.getSubjectTemplate());
        r.setBodyTemplate(t.getBodyTemplate());
        r.setActive(Boolean.TRUE.equals(t.getIsActive()));
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }

    private static BillingNotificationLogResponse toBillingLogResponse(BillingNotificationLog log) {
        BillingNotificationLogResponse r = new BillingNotificationLogResponse();
        r.setLogId(log.getId());
        r.setOrganisationId(log.getOrganisation() != null ? log.getOrganisation().getId() : null);
        r.setEventKey(log.getEventKey());
        r.setChannel(log.getChannel());
        r.setRecipient(log.getRecipient());
        r.setStatus(log.getStatus());
        r.setErrorMessage(log.getErrorMessage());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }

    private static BillingExportJobResponse toExportJobResponse(BillingExportJob job, String downloadToken) {
        BillingExportJobResponse r = new BillingExportJobResponse();
        r.setJobId(job.getId());
        r.setExportType(job.getExportType());
        r.setStatus(job.getStatus());
        r.setFileName(job.getFileName());
        r.setTokenExpiresAt(job.getTokenExpiresAt());
        r.setErrorMessage(job.getErrorMessage());
        r.setCompletedAt(job.getCompletedAt());
        r.setCreatedAt(job.getCreatedAt());
        r.setUpdatedAt(job.getUpdatedAt());
        if (downloadToken != null) {
            r.setDownloadToken(downloadToken);
        }
        return r;
    }

    private static DunningPolicyResponse toDunningPolicyResponse(SuperAdminSubscriptionService.DunningPolicyView view) {
        DunningPolicyResponse r = new DunningPolicyResponse();
        r.setGracePeriodDays(view.gracePeriodDays());
        r.setTrialNoticeDays(view.trialNoticeDays());
        List<DunningPolicyResponse.DunningStep> steps = view.steps().stream().map(step -> {
            DunningPolicyResponse.DunningStep dto = new DunningPolicyResponse.DunningStep();
            dto.setDay(step.day());
            dto.setAction(step.action().name());
            return dto;
        }).collect(Collectors.toList());
        r.setSteps(steps);
        return r;
    }

    private static OrganisationResponse toResponse(Organisation org, Map<String, Object> extra) {
        OrganisationResponse r = new OrganisationResponse();
        r.setId(org.getId());
        r.setName(org.getName());
        r.setSlug(org.getSlug());
        r.setStatus(org.getStatus());
        r.setSubdomain(org.getSubdomain());
        r.setSchemaName(org.getSchemaName());
        r.setCreatedAt(org.getCreatedAt());
        r.setLastBackupAt(org.getLastBackupAt());
        r.setBackupStatus(org.getBackupStatus());
        r.setBackupLocation(org.getBackupLocation());
        r.setTimezone(org.getTimezone());
        r.setRegion(org.getRegion());
        r.setDataResidency(org.getDataResidency());
        r.setLocale(org.getLocale());
        r.setLogoUrl(org.getLogoUrl());
        r.setBrandPrimaryColor(org.getBrandPrimaryColor());
        r.setBrandSecondaryColor(org.getBrandSecondaryColor());
        r.setBrandAccentColor(org.getBrandAccentColor());
        r.setSupportEmail(org.getSupportEmail());
        r.setSupportAddress(org.getSupportAddress());
        r.setTerminationEffectiveAt(org.getTerminationEffectiveAt());
        if (extra != null) r.setExtra(extra);
        return r;
    }

    private static SuperAdminSubscriptionDetailsResponse toSubscriptionDetailsResponse(
            SuperAdminSubscriptionService.StrictSubscriptionView view
    ) {
        SuperAdminSubscriptionDetailsResponse body = new SuperAdminSubscriptionDetailsResponse();
        body.setOrganisationId(view.organisationId());
        body.setSubscriptionId(view.subscriptionId());
        body.setPlan(view.plan());
        body.setStatus(view.status());
        body.setBillingCycle(view.billingCycle());
        body.setPriceAtTime(view.priceAtTime());
        body.setStartAt(view.startAt());
        body.setEndAt(view.endAt());
        body.setTrialEndsAt(view.trialEndsAt());
        body.setProviderCustomerId(view.providerCustomerId());
        body.setProviderSubscriptionId(view.providerSubscriptionId());

        SuperAdminSubscriptionDetailsResponse.UserLimits limits = new SuperAdminSubscriptionDetailsResponse.UserLimits();
        limits.setTherapistLimit(view.therapistLimit());
        limits.setSupervisorLimit(view.supervisorLimit());
        limits.setClientLimit(view.clientLimit());
        body.setUserLimits(limits);

        SuperAdminSubscriptionDetailsResponse.UserUsage usage = new SuperAdminSubscriptionDetailsResponse.UserUsage();
        usage.setTherapistUsers(view.therapistUsers());
        usage.setSupervisorUsers(view.supervisorUsers());
        usage.setClientUsers(view.clientUsers());
        usage.setTotalUsers(view.therapistUsers() + view.supervisorUsers() + view.clientUsers());
        body.setUserUsage(usage);
        return body;
    }

    private List<PlanEntitlementResponse> getPlanEntitlementRows(SubscriptionPlan plan) {
        if (plan == null || plan.getId() == null) {
            return List.of();
        }
        return superAdminPlanCatalogService.listCurrentEntitlements(plan.getId()).stream()
                .map(ent -> {
                    PlanEntitlementResponse row = new PlanEntitlementResponse();
                    row.setPlanName(plan.getName());
                    row.setFeatureCode(ent.getFeature().getCode());
                    row.setEnabled(Boolean.TRUE.equals(ent.getIsEnabled()));
                    row.setUsageLimit(ent.getUsageLimit());
                    row.setTrialAvailable(Boolean.TRUE.equals(ent.getIsTrialAvailable()));
                    row.setEffectiveFrom(ent.getEffectiveFrom());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private static void applyPlanUserLimits(PlanPricingResponse response, List<PlanEntitlementResponse> entitlements) {
        response.setTherapistLimit(findUsageLimit(entitlements, "THERAPIST_LIMIT"));
        response.setSupervisorLimit(findUsageLimit(entitlements, "SUPERVISOR_LIMIT"));
        response.setClientLimit(findUsageLimit(entitlements, "CLIENT_LIMIT"));
    }

    private static Integer findUsageLimit(List<PlanEntitlementResponse> entitlements, String featureCode) {
        if (entitlements == null || entitlements.isEmpty()) {
            return null;
        }
        return entitlements.stream()
                .filter(row -> row != null && row.getFeatureCode() != null)
                .filter(row -> featureCode.equalsIgnoreCase(row.getFeatureCode()))
                .map(PlanEntitlementResponse::getUsageLimit)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static SuperAdminTenantSettingsResponse toSettingsResponse(Organisation org) {
        SuperAdminTenantSettingsResponse r = new SuperAdminTenantSettingsResponse();
        r.setTimezone(org.getTimezone());
        r.setRegion(org.getRegion());
        r.setDataResidency(org.getDataResidency());
        r.setLocale(org.getLocale());
        r.setLogoUrl(org.getLogoUrl());
        r.setBrandPrimaryColor(org.getBrandPrimaryColor());
        r.setBrandSecondaryColor(org.getBrandSecondaryColor());
        r.setBrandAccentColor(org.getBrandAccentColor());
        r.setSupportEmail(org.getSupportEmail());
        r.setSupportAddress(org.getSupportAddress());
        return r;
    }

    private Long resolveOrganisationId(String organisationKey) {
        return superAdminOrganisationQueryService.resolveOrganisationId(organisationKey);
    }

    private static String inferModule(String featureKey) {
        String k = featureKey == null ? "" : featureKey.toUpperCase(Locale.ROOT);
        if (k.contains("BILLING") || k.contains("STRIPE")) return "BILLING";
        if (k.contains("CLIENT")) return "CLIENT";
        if (k.contains("SESSION") || k.contains("ZOOM")) return "SESSION";
        if (k.contains("FORM") || k.contains("ASSESSMENT") || k.contains("TASK")) return "CLINICAL";
        if (k.contains("ROLE") || k.contains("PERMISSION") || k.contains("AUDIT")) return "ADMIN";
        if (k.contains("AI")) return "AI";
        return "PLATFORM";
    }

    private static List<String> inferAccessControlList(String module, List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        String normalizedModule = module == null ? "PLATFORM" : module.toUpperCase(Locale.ROOT);
        return permissions.stream()
                .filter(Objects::nonNull)
                .map(Permission::getName)
                .filter(Objects::nonNull)
                .filter(name -> matchesAclModule(name, normalizedModule))
                .sorted()
                .distinct()
                .toList();
    }

    private static boolean matchesAclModule(String permissionName, String module) {
        String p = permissionName.toUpperCase(Locale.ROOT);
        return switch (module) {
            case "BILLING" -> p.startsWith("BILLING_");
            case "CLIENT" -> p.startsWith("CLIENT_");
            case "SESSION" -> p.startsWith("SESSION_") || p.startsWith("ROOM_");
            case "CLINICAL" -> p.startsWith("FORM_") || p.startsWith("ASSESSMENT_")
                    || p.startsWith("TASK_") || p.startsWith("CHECKLIST_");
            case "ADMIN" -> p.startsWith("USER_") || p.startsWith("ROLE_")
                    || p.startsWith("AUDIT_") || p.startsWith("CONSENT_");
            case "AI" -> p.startsWith("AI_");
            default -> p.startsWith("PLATFORM_");
        };
    }

    private static Map<String, Object> buildUsageLimitations(CoreFeature feature, Integer usageLimit) {
        // Toggle features and unlimited quotas legitimately have null limits.
        Map<String, Object> limitations = new LinkedHashMap<>();
        limitations.put("type", feature == null ? "UNKNOWN" : feature.getValueType().name());
        limitations.put("currentUsageLimit", usageLimit);
        if (feature != null) {
            limitations.put("minUsageLimit", feature.getMinUsageLimit());
            limitations.put("catalogDefaultUsageLimit", feature.getCatalogDefaultUsageLimit());
        }
        return limitations;
    }

}
