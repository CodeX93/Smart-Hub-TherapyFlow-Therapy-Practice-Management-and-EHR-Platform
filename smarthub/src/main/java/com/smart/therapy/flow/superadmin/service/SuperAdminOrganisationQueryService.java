package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.PlatformAuditLogRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationSsoConfigRepository;
import com.smart.therapy.flow.organisation.repository.TenantSchemaVersionRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantFeatureService;
import com.smart.therapy.flow.organisation.service.TenantPracticeConfigurationSeedService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.auth.service.SsoSecurityService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListItem;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListResult;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminHipaaAuditLogResponse;
import com.smart.therapy.flow.audit.util.AuditLogLevelResolver;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class SuperAdminOrganisationQueryService {

    private static final Map<String, Set<String>> ALLOWED_REGIONS_BY_RESIDENCY = Map.of(
            "US", Set.of("us-east-1", "us-west-1", "us-west-2"),
            "EU", Set.of("eu-west-1", "eu-central-1", "eu-north-1"),
            "AU", Set.of("ap-southeast-2")
    );
    private static final Pattern HEX_COLOR = Pattern.compile("^#?[0-9a-fA-F]{6}$");
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");
    private static final Pattern ORG_ID_IN_TEXT = Pattern.compile("(?:organisationId|orgId)\\s*[=:]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final OrganisationRepository organisationRepository;
    private final TenantSchemaVersionRepository tenantSchemaVersionRepository;
    private final TenantDirectoryService tenantDirectoryService;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final PlatformAuditLogRepository platformAuditLogRepository;
    private final PlatformAuditService platformAuditService;
    private final TenantFeatureService tenantFeatureService;
    private final OrganisationSsoConfigRepository organisationSsoConfigRepository;
    private final SsoSecurityService ssoSecurityService;
    private final SuperAdminOrganisationListService superAdminOrganisationListService;
    private final ObjectMapper objectMapper;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final AuditLogRepository auditLogRepository;
    private final TenantPracticeConfigurationSeedService tenantPracticeConfigurationSeedService;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<Object> listOrganisations(SuperAdminOrganisationListRequest req, boolean exportCsv) {
        SuperAdminOrganisationListResult result = superAdminOrganisationListService.search(req);
        if (exportCsv) {
            StringBuilder csv = new StringBuilder("id,name,slug,status,plan,usersCount,createdAt,region,dataResidency,timezone\n");
            for (SuperAdminOrganisationListItem item : result.getItems()) {
                csv.append(item.getId()).append(',')
                        .append(safeCsv(item.getName())).append(',')
                        .append(safeCsv(item.getSlug())).append(',')
                        .append(safeCsv(item.getStatus())).append(',')
                        .append(safeCsv(item.getPlan())).append(',')
                        .append(item.getUsersCount()).append(',')
                        .append(item.getCreatedAt()).append(',')
                        .append(safeCsv(item.getRegion())).append(',')
                        .append(safeCsv(item.getDataResidency())).append(',')
                        .append(safeCsv(item.getTimezone())).append('\n');
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=organisations.csv")
                    .body(csv.toString());
        }
        return ResponseEntity.ok(Map.of(
                "items", result.getItems(),
                "total", result.getTotal(),
                "page", result.getPage(),
                "pageSize", result.getPageSize()
        ));
    }

    @Transactional(readOnly = true)
    public Optional<Organisation> getOrganisation(Long id) {
        return organisationRepository.findById(id);
    }

    /**
     * The email of the organisation's primary administrator — the identity onboarding created.
     * Distinct from {@code supportEmail}, which is a tenant setting the platform may leave unset.
     */
    @Transactional(readOnly = true)
    public Optional<String> findPrimaryAdminEmail(Long organisationId) {
        if (organisationId == null) {
            return Optional.empty();
        }
        return authIdentityRoleRepository.findAdminEmailsByOrganisationId(organisationId).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Long resolveOrganisationId(String organisationKey) {
        if (organisationKey == null || organisationKey.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "organisation identifier is required");
        }
        String normalized = organisationKey.trim();
        try {
            Long id = Long.valueOf(normalized);
            if (!organisationRepository.existsById(id)) {
                throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
            }
            return id;
        } catch (NumberFormatException ignored) {
            return organisationRepository.findBySlug(normalized)
                    .map(Organisation::getId)
                    .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found"));
        }
    }


    @Transactional(readOnly = true)
    public boolean isSlugAvailable(String slug) {
        return !organisationRepository.existsBySlug(slug);
    }

    @Transactional
    public Optional<Organisation> updateOrganisation(Long id, Map<String, Object> updates, Long actorAuthId) {
        return organisationRepository.findById(id)
                .map(org -> {
                    Map<String, Object> before = organisationSnapshot(org);
                    if (updates.containsKey("name")) org.setName((String) updates.get("name"));
                    if (updates.containsKey("status")) org.setStatus((String) updates.get("status"));
                    if (updates.containsKey("timezone")) {
                        String timezone = asString(updates.get("timezone"));
                        validateTimezone(timezone);
                        org.setTimezone(timezone);
                    }
                    if (updates.containsKey("region") || updates.containsKey("dataResidency")) {
                        String region = updates.containsKey("region") ? asString(updates.get("region")) : org.getRegion();
                        String residency = updates.containsKey("dataResidency") ? asString(updates.get("dataResidency")) : org.getDataResidency();
                        validateRegionResidency(region, residency);
                        org.setRegion(normalizeRegion(region));
                        org.setDataResidency(normalizeResidency(residency));
                    }
                    if (updates.containsKey("data_residency")) org.setDataResidency((String) updates.get("data_residency"));
                    if (updates.containsKey("locale")) {
                        String locale = asString(updates.get("locale"));
                        validateLocale(locale);
                        org.setLocale(locale);
                    }
                    if (updates.containsKey("logoUrl")) {
                        String logoUrl = asString(updates.get("logoUrl"));
                        validateLogoUrl(logoUrl);
                        org.setLogoUrl(logoUrl);
                    }
                    if (updates.containsKey("brandPrimaryColor")) {
                        String value = asString(updates.get("brandPrimaryColor"));
                        validateHexColor("brandPrimaryColor", value);
                        org.setBrandPrimaryColor(value);
                    }
                    if (updates.containsKey("brandSecondaryColor")) {
                        String value = asString(updates.get("brandSecondaryColor"));
                        validateHexColor("brandSecondaryColor", value);
                        org.setBrandSecondaryColor(value);
                    }
                    if (updates.containsKey("brandAccentColor")) {
                        String value = asString(updates.get("brandAccentColor"));
                        validateHexColor("brandAccentColor", value);
                        org.setBrandAccentColor(value);
                    }
                    if (updates.containsKey("supportEmail")) {
                        String email = asString(updates.get("supportEmail"));
                        validateSupportEmail(email);
                        org.setSupportEmail(email);
                    }
                    if (updates.containsKey("supportAddress")) {
                        org.setSupportAddress(asString(updates.get("supportAddress")));
                    }
                    Organisation saved = organisationRepository.save(org);
                    if (updates.containsKey("timezone")) {
                        tenantPracticeConfigurationSeedService.syncTimezone(
                                saved.getId(), saved.getSchemaName(), saved.getTimezone());
                    }
                    Map<String, Object> after = organisationSnapshot(saved);
                    platformAuditService.logWithSnapshots(actorAuthId, "ORGANISATION_UPDATED", "Organisation", String.valueOf(id),
                            before, after, "updates=" + updates.keySet());
                    tenantDirectoryService.evictCache();
                    if (updates.containsKey("status")) {
                        tenantSchemaHealthService.evict(org.getSchemaName());
                    }
                    return saved;
                });
    }

    @Transactional
    public Optional<Organisation> updateTenantSettings(Long id, com.smart.therapy.flow.superadmin.dto.SuperAdminTenantSettingsRequest request,
                                                       Long actorAuthId) {
        return organisationRepository.findById(id)
                .map(org -> {
                    Map<String, Object> before = organisationSnapshot(org);
                    validateTimezone(request.getTimezone());
                    validateRegionResidency(request.getRegion(), request.getDataResidency());
                    validateLocale(request.getLocale());
                    validateLogoUrl(request.getLogoUrl());
                    validateHexColor("brandPrimaryColor", request.getBrandPrimaryColor());
                    validateHexColor("brandSecondaryColor", request.getBrandSecondaryColor());
                    validateHexColor("brandAccentColor", request.getBrandAccentColor());
                    validateSupportEmail(request.getSupportEmail());

                    org.setTimezone(request.getTimezone().trim());
                    org.setRegion(normalizeRegion(request.getRegion()));
                    org.setDataResidency(normalizeResidency(request.getDataResidency()));
                    org.setLocale(normalizeOptional(request.getLocale()));
                    org.setLogoUrl(normalizeOptional(request.getLogoUrl()));
                    org.setBrandPrimaryColor(normalizeOptional(request.getBrandPrimaryColor()));
                    org.setBrandSecondaryColor(normalizeOptional(request.getBrandSecondaryColor()));
                    org.setBrandAccentColor(normalizeOptional(request.getBrandAccentColor()));
                    org.setSupportEmail(normalizeOptional(request.getSupportEmail()));
                    org.setSupportAddress(normalizeOptional(request.getSupportAddress()));

                    Organisation saved = organisationRepository.save(org);
                    tenantPracticeConfigurationSeedService.syncTimezone(
                            saved.getId(), saved.getSchemaName(), saved.getTimezone());
                    Map<String, Object> after = organisationSnapshot(saved);
                    platformAuditService.logWithSnapshots(actorAuthId, "TENANT_SETTINGS_UPDATED", "Organisation", String.valueOf(id),
                            before, after, "fields=timezone,region,dataResidency,locale,branding");
                    tenantDirectoryService.evictCache();
                    return saved;
                });
    }

    private static String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void validateTimezone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "timezone is required");
        }
        try {
            ZoneId.of(timezone.trim());
        } catch (Exception ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "timezone must be a valid IANA zone");
        }
    }

    private static void validateRegionResidency(String region, String residency) {
        if (!StringUtils.hasText(region) || !StringUtils.hasText(residency)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "region and dataResidency are required");
        }
        String normalizedResidency = normalizeResidency(residency);
        if (!ALLOWED_REGIONS_BY_RESIDENCY.containsKey(normalizedResidency)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "dataResidency must be US, EU, or AU");
        }
        String normalizedRegion = normalizeRegion(region);
        if (!ALLOWED_REGIONS_BY_RESIDENCY.getOrDefault(normalizedResidency, Set.of()).contains(normalizedRegion)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_REGION_RESIDENCY",
                    "region is not allowed for dataResidency");
        }
    }

    private static String normalizeRegion(String region) {
        return region == null ? null : region.trim().toLowerCase();
    }

    private static String normalizeResidency(String residency) {
        return residency == null ? null : residency.trim().toUpperCase();
    }

    private Map<String, Object> organisationSnapshot(Organisation org) {
        if (org == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", org.getId());
        snapshot.put("name", org.getName());
        snapshot.put("slug", org.getSlug());
        snapshot.put("status", org.getStatus());
        snapshot.put("region", org.getRegion());
        snapshot.put("dataResidency", org.getDataResidency());
        snapshot.put("timezone", org.getTimezone());
        snapshot.put("locale", org.getLocale());
        snapshot.put("logoUrl", org.getLogoUrl());
        snapshot.put("brandPrimaryColor", org.getBrandPrimaryColor());
        snapshot.put("brandSecondaryColor", org.getBrandSecondaryColor());
        snapshot.put("brandAccentColor", org.getBrandAccentColor());
        snapshot.put("supportEmail", org.getSupportEmail());
        snapshot.put("supportAddress", org.getSupportAddress());
        snapshot.put("updatedAt", org.getUpdatedAt());
        return snapshot;
    }

    private static void validateHexColor(String field, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!HEX_COLOR.matcher(value.trim()).matches()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field + " must be hex color");
        }
    }

    private static void validateLogoUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("https://")) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "logoUrl must start with https://");
        }
        if (trimmed.length() > 500) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "logoUrl too long");
        }
    }

    private static void validateSupportEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "supportEmail must be valid");
        }
    }

    private static void validateLocale(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!LOCALE_PATTERN.matcher(value.trim()).matches()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "locale must be in format en or en-US");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getOrganisationHealth(Long id) {
        return organisationRepository.findById(id)
                .map(org -> {
                    String schemaName = org.getSchemaName();
                    boolean schemaExists = "public".equalsIgnoreCase(schemaName) || tenantSchemaHealthService.schemaExists(schemaName);
                    var versionOpt = tenantSchemaVersionRepository.findByOrganisationId(id);
                    Map<String, Object> body = new HashMap<>();
                    body.put("organisationId", id);
                    body.put("schemaName", schemaName);
                    body.put("schemaExists", schemaExists);
                    body.put("status", org.getStatus());
                    versionOpt.ifPresent(v -> {
                        body.put("version", v.getVersion());
                        body.put("migratedAt", v.getMigratedAt() != null ? v.getMigratedAt().toString() : null);
                        body.put("migrationStatus", v.getStatus());
                        if (v.getErrorMessage() != null) body.put("errorMessage", v.getErrorMessage());
                    });
                    return body;
                });
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSchemaVersion(Long id) {
        return tenantSchemaVersionRepository.findByOrganisationId(id)
                .map(v -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("organisationId", id);
                    m.put("version", v.getVersion());
                    m.put("migratedAt", v.getMigratedAt() != null ? v.getMigratedAt().toString() : null);
                    m.put("migrationStatus", v.getStatus() != null ? v.getStatus() : "SUCCESS");
                    if (v.getErrorMessage() != null) m.put("errorMessage", v.getErrorMessage());
                    return m;
                });
    }

    @Transactional(readOnly = true)
    public List<PlatformAuditLog> listAuditLogs(int page,
                                                int size,
                                                Long authId,
                                                String action,
                                                String resourceType,
                                                String resourceId,
                                                String logLevel,
                                                String q,
                                                Instant createdFrom,
                                                Instant createdTo,
                                                String sort,
                                                String order) {
        String sortField = resolveAuditSortField(sort);
        Sort.Direction direction = resolveSortDirection(order);

        Specification<PlatformAuditLog> spec = Specification.where(null);

        if (authId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("authId"), authId));
        }
        if (StringUtils.hasText(action)) {
            String exactAction = normalizeExactToken(action);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("action")), exactAction));
        }
        if (StringUtils.hasText(resourceType)) {
            String exactType = normalizeExactToken(resourceType);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("resourceType")), exactType));
        }
        if (StringUtils.hasText(resourceId)) {
            String exactResourceId = resourceId.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("resourceId")), exactResourceId));
        }
        if (StringUtils.hasText(q)) {
            // Exact match only (no substring LIKE). Numeric q also matches audit log id.
            String exact = q.trim();
            String exactLower = exact.toLowerCase(Locale.ROOT);
            String exactActionToken = normalizeExactToken(exact);
            Long exactId = null;
            try {
                exactId = Long.valueOf(exact);
            } catch (NumberFormatException ignored) {
                // non-numeric search term
            }
            Long idMatch = exactId;
            spec = spec.and((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (idMatch != null) {
                    predicates.add(cb.equal(root.get("id"), idMatch));
                }
                predicates.add(cb.equal(cb.lower(root.get("action")), exactLower));
                predicates.add(cb.equal(cb.lower(root.get("action")), exactActionToken));
                predicates.add(cb.equal(cb.lower(root.get("resourceType")), exactLower));
                predicates.add(cb.equal(cb.lower(root.get("resourceType")), exactActionToken));
                predicates.add(cb.equal(cb.lower(root.get("resourceId")), exactLower));
                predicates.add(cb.equal(cb.lower(root.get("details")), exactLower));
                return cb.or(predicates.toArray(Predicate[]::new));
            });
        }
        String normalizedLogLevel = AuditLogLevelResolver.normalizeFilter(logLevel);
        if (normalizedLogLevel != null) {
            switch (normalizedLogLevel) {
                case "ERROR" -> spec = spec.and((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("details")), "%status=5%"),
                        cb.like(cb.lower(root.get("details")), "%\"status\":5%"),
                        cb.like(cb.lower(root.get("details")), "%error=%"),
                        cb.like(cb.lower(root.get("details")), "%exception%"),
                        cb.like(cb.lower(root.get("action")), "%failed%"),
                        cb.like(cb.lower(root.get("action")), "%error%")));
                case "WARN" -> spec = spec.and((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("details")), "%status=4%"),
                        cb.like(cb.lower(root.get("details")), "%\"status\":4%"),
                        cb.like(cb.lower(root.get("details")), "%forbidden%"),
                        cb.like(cb.lower(root.get("details")), "%unauthorized%")));
                case "TRACE" -> spec = spec.and((root, query, cb) -> cb.and(
                        cb.like(cb.lower(root.get("action")), "api\\_%", '\\'),
                        cb.or(
                                cb.like(cb.lower(root.get("details")), "%details=[%"),
                                cb.like(cb.lower(root.get("details")), "%\"details\":[%"),
                                cb.like(cb.lower(root.get("details")), "%source=global_activity_aspect%"))));
                case "DEBUG" -> spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("action")), "api\\_%", '\\'));
                case "INFO" -> spec = spec.and((root, query, cb) ->
                        cb.notLike(cb.lower(root.get("action")), "api\\_%", '\\'));
                default -> {
                }
            }
        }
        if (createdFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "createdFrom must be <= createdTo");
        }

        return platformAuditLogRepository
                .findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortField)))
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntryResponse> listAuditLogResponses(int page,
                                                             int size,
                                                             Long authId,
                                                             String action,
                                                             String resourceType,
                                                             String resourceId,
                                                             String logLevel,
                                                             String q,
                                                             Instant createdFrom,
                                                             Instant createdTo,
                                                             String sort,
                                                             String order) {
        return listAuditLogs(page, size, authId, action, resourceType, resourceId, logLevel, q, createdFrom, createdTo, sort, order)
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminHipaaAuditLogResponse> listHipaaAuditLogsAcrossTenants(int page,
                                                                                  int size,
                                                                                  Long organisationId,
                                                                                  String username,
                                                                                  String action,
                                                                                  String resourceType,
                                                                                  String logLevel,
                                                                                  String riskLevel,
                                                                                  String result,
                                                                                  LocalDate startDate,
                                                                                  LocalDate endDate) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long offset = (long) safePage * safeSize;

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "startDate must be <= endDate");
        }

        List<TenantDirectoryService.TenantInfo> tenants = resolveTargetTenants(organisationId);
        if (tenants.isEmpty()) {
            return List.of();
        }

        int tenantFetchLimit = Math.max((safePage + 1) * safeSize, safeSize);
        List<SuperAdminHipaaAuditLogResponse> merged = new ArrayList<>();

        for (TenantDirectoryService.TenantInfo tenant : tenants) {
            String schema = tenant.getSchemaName();
            if (!tenantSchemaHealthService.schemaExists(schema)) {
                continue;
            }
            List<SuperAdminHipaaAuditLogResponse> tenantLogs = tenantTransactionExecutor.executeReadOnly(
                    tenant.getOrganisationId(),
                    schema,
                    () -> fetchTenantHipaaAuditLogs(tenant, username, action, resourceType, logLevel, riskLevel, result,
                            startDate, endDate, tenantFetchLimit));
            merged.addAll(tenantLogs);
        }

        merged.sort(Comparator.comparing(SuperAdminHipaaAuditLogResponse::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        if (offset >= merged.size()) {
            return List.of();
        }
        int fromIndex = (int) offset;
        int toIndex = Math.min(fromIndex + safeSize, merged.size());
        return merged.subList(fromIndex, toIndex);
    }

    private List<TenantDirectoryService.TenantInfo> resolveTargetTenants(Long organisationId) {
        if (organisationId != null) {
            return tenantDirectoryService.findByOrganisationId(organisationId)
                    .filter(this::isEligibleTenantForAuditRead)
                    .map(List::of)
                    .orElse(List.of());
        }

        return tenantDirectoryService.getBySchemaName().values().stream()
                .filter(this::isEligibleTenantForAuditRead)
                .toList();
    }

    private boolean isEligibleTenantForAuditRead(TenantDirectoryService.TenantInfo tenantInfo) {
        if (tenantInfo == null) {
            return false;
        }
        String schema = tenantInfo.getSchemaName();
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return false;
        }
        if (tenantInfo.isArchived() || tenantInfo.isDeleted() || tenantInfo.isForceDisabled()) {
            return false;
        }
        return tenantInfo.isActive();
    }

    private List<SuperAdminHipaaAuditLogResponse> fetchTenantHipaaAuditLogs(TenantDirectoryService.TenantInfo tenant,
                                                                             String username,
                                                                             String action,
                                                                             String resourceType,
                                                                             String logLevel,
                                                                             String riskLevel,
                                                                             String result,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate,
                                                                             int fetchLimit) {
        Specification<AuditLog> spec = buildTenantAuditSpecification(username, action, resourceType, logLevel, riskLevel, result,
                startDate, endDate);
        List<AuditLog> logs = auditLogRepository.findAll(
                spec,
                PageRequest.of(0, fetchLimit, Sort.by(Sort.Direction.DESC, "timestamp"))).getContent();
        Map<String, Long> activityCounts = countTenantActivitiesByUsername(spec, logs);

        String organisationName = organisationRepository.findById(tenant.getOrganisationId())
                .map(Organisation::getName)
                .orElse(null);

        return logs.stream()
                .map(log -> mapTenantAuditLog(tenant, organisationName, log, activityCounts))
                .toList();
    }

    private Specification<AuditLog> buildTenantAuditSpecification(String username,
                                                                   String action,
                                                                   String resourceType,
                                                                   String logLevel,
                                                                   String riskLevel,
                                                                   String result,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.equal(root.get("hipaaRelevant"), true);

        if (StringUtils.hasText(username)) {
            String normalized = "%" + username.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("username")), normalized));
        }
        if (StringUtils.hasText(action)) {
            String normalized = "%" + action.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("action")), normalized));
        }
        if (StringUtils.hasText(resourceType)) {
            String normalized = "%" + resourceType.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("resourceType")), normalized));
        }
        if (StringUtils.hasText(riskLevel)) {
            String normalized = riskLevel.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("riskLevel")), normalized));
        }
        if (StringUtils.hasText(result)) {
            String normalized = result.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("result")), normalized));
        }
        String normalizedLogLevel = AuditLogLevelResolver.normalizeFilter(logLevel);
        if (normalizedLogLevel != null) {
            switch (normalizedLogLevel) {
                case "ERROR" -> spec = spec.and((root, query, cb) -> cb.or(
                        cb.equal(cb.lower(root.get("result")), "failure"),
                        cb.equal(cb.lower(root.get("result")), "failed"),
                        cb.like(cb.lower(root.get("details")), "%status=5%"),
                        cb.like(cb.lower(root.get("details")), "%\"status\":5%"),
                        cb.like(cb.lower(root.get("details")), "%error=%")));
                case "WARN" -> spec = spec.and((root, query, cb) -> cb.or(
                        cb.equal(cb.lower(root.get("result")), "blocked"),
                        cb.like(cb.lower(root.get("details")), "%status=4%"),
                        cb.like(cb.lower(root.get("details")), "%\"status\":4%"),
                        cb.like(cb.lower(root.get("details")), "%forbidden%"),
                        cb.like(cb.lower(root.get("details")), "%unauthorized%")));
                case "TRACE" -> spec = spec.and((root, query, cb) -> cb.and(
                        cb.like(cb.lower(root.get("action")), "api\\_%", '\\'),
                        cb.or(
                                cb.like(cb.lower(root.get("details")), "%details=[%"),
                                cb.like(cb.lower(root.get("details")), "%\"details\":[%"),
                                cb.like(cb.lower(root.get("details")), "%source=global_activity_aspect%"))));
                case "DEBUG" -> spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("action")), "api\\_%", '\\'));
                case "INFO" -> spec = spec.and((root, query, cb) -> cb.and(
                        cb.equal(cb.lower(root.get("result")), "success"),
                        cb.notLike(cb.lower(root.get("action")), "api\\_%", '\\')));
                default -> {
                }
            }
        }
        if (startDate != null) {
            Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), startInstant));
        }
        if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_999_999);
            Instant endInstant = endDateTime.atZone(ZoneId.systemDefault()).toInstant();
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), endInstant));
        }
        return spec;
    }

    private SuperAdminHipaaAuditLogResponse mapTenantAuditLog(TenantDirectoryService.TenantInfo tenant,
                                                              String organisationName,
                                                              AuditLog log,
                                                              Map<String, Long> activityCounts) {
        String usernameKey = normalizeUsername(log.getUsername());
        long userActivityCount = usernameKey != null ? activityCounts.getOrDefault(usernameKey, 0L) : 0L;
        String rawAction = log.getAction();
        String logLevel = AuditLogLevelResolver.resolveTenantLogLevel(log.getAction(), log.getResult(), log.getDetails());

        return SuperAdminHipaaAuditLogResponse.builder()
                .organisationId(tenant.getOrganisationId())
                .organisationName(organisationName)
                .tenantSchema(tenant.getSchemaName())
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUsername())
                .action(humanizeToken(rawAction))
                .rawAction(rawAction)
                .logLevel(logLevel)
                .result(log.getResult())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .clientId(log.getClient() != null ? log.getClient().getId() : null)
                .clientName(log.getClient() != null ? log.getClient().getFullName() : null)
                .ipAddress(log.getIpAddress())
                .riskLevel(log.getRiskLevel())
                .hipaaRelevant(log.getHipaaRelevant())
                .details(toHumanReadableDetails(log))
                .userActivityCount(userActivityCount)
                .timestamp(log.getTimestamp())
                .build();
    }

    private Map<String, Long> countTenantActivitiesByUsername(Specification<AuditLog> spec, List<AuditLog> logs) {
        Set<String> usernames = new LinkedHashSet<>();
        for (AuditLog log : logs) {
            String normalized = normalizeUsername(log.getUsername());
            if (normalized != null) {
                usernames.add(normalized);
            }
        }
        if (usernames.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> counts = new HashMap<>();
        for (String username : usernames) {
            Specification<AuditLog> userSpec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("username")), username));
            counts.put(username, auditLogRepository.count(userSpec));
        }
        return counts;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String toHumanReadableDetails(AuditLog log) {
        String rawDetails = log.getDetails();
        if (StringUtils.hasText(rawDetails)) {
            String parsed = parseDetailsText(rawDetails);
            if (StringUtils.hasText(parsed)) {
                return parsed;
            }
        }
        return buildFallbackDetails(log);
    }

    private String buildFallbackDetails(AuditLog log) {
        String action = humanizeToken(log.getAction());
        String resourceType = humanizeToken(log.getResourceType());
        String resourceId = log.getResourceId();

        if (StringUtils.hasText(resourceType) && StringUtils.hasText(resourceId)) {
            return action + " on " + resourceType + " (ID " + resourceId + ")";
        }
        if (StringUtils.hasText(resourceType)) {
            return action + " on " + resourceType;
        }
        return action;
    }

    private String parseDetailsText(String rawDetails) {
        try {
            JsonNode root = objectMapper.readTree(rawDetails);
            if (root == null || root.isNull()) {
                return null;
            }
            if (!root.isObject()) {
                return root.asText(rawDetails);
            }
            List<String> fragments = new ArrayList<>();
            JsonNode summary = root.get("details");
            if (summary != null && !summary.isNull() && StringUtils.hasText(summary.asText())) {
                fragments.add(summary.asText());
            }
            var fields = root.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if ("details".equalsIgnoreCase(field.getKey())) {
                    continue;
                }
                String value = summarizeDetailValue(field.getValue());
                if (StringUtils.hasText(value)) {
                    fragments.add(humanizeToken(field.getKey()) + ": " + value);
                }
            }
            return fragments.isEmpty() ? rawDetails : String.join("; ", fragments);
        } catch (Exception ignored) {
            return rawDetails;
        }
    }

    private String summarizeDetailValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isTextual() || valueNode.isNumber() || valueNode.isBoolean()) {
            return valueNode.asText();
        }
        if (valueNode.isArray()) {
            int size = valueNode.size();
            if (size == 0) {
                return "none";
            }
            List<String> samples = new ArrayList<>();
            for (int i = 0; i < Math.min(size, 3); i++) {
                JsonNode child = valueNode.get(i);
                if (child != null && (child.isTextual() || child.isNumber() || child.isBoolean())) {
                    samples.add(child.asText());
                }
            }
            return samples.isEmpty() ? size + " item(s)" : String.join(", ", samples) + (size > 3 ? " +" + (size - 3) + " more" : "");
        }
        if (valueNode.isObject()) {
            JsonNode id = valueNode.get("id");
            JsonNode name = valueNode.get("name");
            if (name != null && !name.isNull() && StringUtils.hasText(name.asText())) {
                if (id != null && !id.isNull()) {
                    return name.asText() + " (ID " + id.asText() + ")";
                }
                return name.asText();
            }
            if (id != null && !id.isNull()) {
                return "ID " + id.asText();
            }
            return "object";
        }
        return valueNode.toString();
    }

    private String humanizeToken(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String[] parts = raw.trim().toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(switch (part) {
                case "hipaa" -> "HIPAA";
                case "phi" -> "PHI";
                case "api" -> "API";
                case "ip" -> "IP";
                case "id" -> "ID";
                case "sso" -> "SSO";
                default -> Character.toUpperCase(part.charAt(0)) + part.substring(1);
            });
        }
        return words.isEmpty() ? raw : String.join(" ", words);
    }

    private AuditLogEntryResponse toAuditLogResponse(PlatformAuditLog log) {
        AuditLogEntryResponse r = new AuditLogEntryResponse();
        r.setId(log.getId());
        r.setAuthId(log.getAuthId());
        r.setAction(log.getAction());
        r.setLogLevel(AuditLogLevelResolver.resolvePlatformLogLevel(log.getAction(), log.getDetails()));
        r.setResourceType(log.getResourceType());
        r.setResourceId(log.getResourceId());
        r.setDetails(log.getDetails());
        r.setCreatedAt(log.getCreatedAt());
        applySnapshots(r, log.getDetails());
        enrichOrganisationContext(r, log);
        if (!StringUtils.hasText(r.getActionSummary())) {
            r.setActionSummary(buildActionSummary(r));
        }
        return r;
    }

    private void applySnapshots(AuditLogEntryResponse response, String details) {
        if (details == null || details.isBlank()) {
            return;
        }
        try {
            Map<?, ?> json = objectMapper.readValue(details, Map.class);
            Object detailText = json.get("details");
            if (detailText instanceof String text && !text.isBlank()) {
                response.setDetails(text);
                response.setActionSummary(text);
            }
            if (json.containsKey("before")) {
                response.setBefore(json.get("before"));
            }
            if (json.containsKey("after")) {
                response.setAfter(json.get("after"));
            }
        } catch (Exception ignored) {
        }
    }

    private void enrichOrganisationContext(AuditLogEntryResponse response, PlatformAuditLog log) {
        Long organisationId = resolveOrganisationId(log.getResourceType(), log.getResourceId(), log.getDetails());
        if (organisationId == null) {
            return;
        }
        response.setOrganisationId(organisationId);
        organisationRepository.findById(organisationId)
                .ifPresent(org -> response.setOrganisationName(org.getName()));
    }

    private Long resolveOrganisationId(String resourceType, String resourceId, String details) {
        if ("Organisation".equalsIgnoreCase(resourceType) && StringUtils.hasText(resourceId)) {
            try {
                return Long.valueOf(resourceId.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (StringUtils.hasText(details) && details.trim().startsWith("{")) {
            try {
                Map<?, ?> json = objectMapper.readValue(details, Map.class);
                Object direct = json.get("organisationId");
                if (direct == null) {
                    direct = json.get("orgId");
                }
                if (direct instanceof Number n) {
                    return n.longValue();
                }
                if (direct instanceof String s && !s.isBlank()) {
                    return Long.valueOf(s.trim());
                }
            } catch (Exception ignored) {
            }
        }
        if (!StringUtils.hasText(details)) {
            return null;
        }
        Matcher matcher = ORG_ID_IN_TEXT.matcher(details);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildActionSummary(AuditLogEntryResponse response) {
        String action = response.getAction() != null ? response.getAction().trim() : "ACTION";
        String resource = response.getResourceType() != null ? response.getResourceType().trim() : "Resource";
        String target = response.getOrganisationName() != null
                ? response.getOrganisationName()
                : (response.getResourceId() != null ? response.getResourceId() : "N/A");
        return action + " on " + resource + " [" + target + "]";
    }

    /**
     * Exact token match helper: trim, lower-case, and normalize spaces/hyphens to underscores
     * so {@code ORGANISATION_UPDATED} and {@code Organisation Updated} resolve the same.
     */
    private static String normalizeExactToken(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("\\s+", "_");
    }

    private String resolveAuditSortField(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "createdAt";
        }
        return switch (sort.trim()) {
            case "createdAt", "action", "authId", "resourceType", "resourceId" -> sort.trim();
            default -> throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Unsupported sort field: " + sort);
        };
    }

    private Sort.Direction resolveSortDirection(String order) {
        if (!StringUtils.hasText(order)) {
            return Sort.Direction.DESC;
        }
        if ("asc".equalsIgnoreCase(order)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(order)) {
            return Sort.Direction.DESC;
        }
        throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT_ORDER", "order must be asc or desc");
    }

    @Transactional(readOnly = true)
    public boolean organisationExists(Long id) {
        return organisationRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<com.smart.therapy.flow.organisation.entity.TenantFeature> getOrganisationFeatures(Long id) {
        return tenantFeatureService.getFeaturesForOrganisation(id);
    }

    @Transactional
    public List<com.smart.therapy.flow.organisation.entity.TenantFeature> setOrganisationFeatures(Long id, Map<String, Boolean> features) {
        return tenantFeatureService.setFeatures(id, features);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrganisationSsoSettings(Long id) {
        if (!organisationRepository.existsById(id)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        var configs = organisationSsoConfigRepository.findByOrganisationIdAndIsEnabledTrue(id);
        List<Map<String, Object>> providers = configs.stream().map(cfg -> Map.<String, Object>of(
                "provider", cfg.getProvider(),
                "redirectUri", cfg.getRedirectUri(),
                "enabled", Boolean.TRUE.equals(cfg.getIsEnabled())
        )).toList();
        List<String> allowedDomains = ssoSecurityService.getAllowedDomains(id);
        return Map.of(
                "organisationId", id,
                "providers", providers,
                "allowedDomains", allowedDomains
        );
    }

    @Transactional
    public Map<String, Object> setOrganisationSsoDomains(Long id, List<String> domains, Long actorAuthId) {
        if (!organisationRepository.existsById(id)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        List<String> saved = ssoSecurityService.replaceAllowedDomains(id, domains);
        platformAuditService.log(
                actorAuthId,
                "SSO_ALLOWED_DOMAINS_UPDATED",
                "Organisation",
                String.valueOf(id),
                "count=" + saved.size()
        );
        return Map.of("organisationId", id, "allowedDomains", saved);
    }

    @Transactional
    public Map<String, Object> setOrganisationSsoRedirectUri(Long id, String provider, String redirectUri, Long actorAuthId) {
        if (!organisationRepository.existsById(id)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(redirectUri)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "provider and redirectUri are required");
        }
        String normalizedProvider = provider.trim().toUpperCase();
        String normalizedRedirect = redirectUri.trim();
        var cfg = organisationSsoConfigRepository.findByOrganisationIdAndProviderAndIsEnabledTrue(id, normalizedProvider)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "SSO_CONFIG_NOT_FOUND", "Enabled SSO config not found for provider"));
        cfg.setRedirectUri(normalizedRedirect);
        organisationSsoConfigRepository.save(cfg);
        platformAuditService.log(
                actorAuthId,
                "SSO_REDIRECT_URI_UPDATED",
                "Organisation",
                String.valueOf(id),
                "provider=" + normalizedProvider
        );
        return Map.of(
                "organisationId", id,
                "provider", normalizedProvider,
                "redirectUri", normalizedRedirect
        );
    }

    private static String safeCsv(String v) {
        if (v == null) return "";
        String escaped = v.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
