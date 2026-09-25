package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.feature.FeatureScope;
import com.smart.therapy.flow.subscription.feature.FeatureType;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.superadmin.dto.FeatureCatalogCreateRequest;
import com.smart.therapy.flow.superadmin.dto.FeatureCatalogResponse;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
import com.smart.therapy.flow.organisation.repository.PlatformAuditLogRepository;
import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SuperAdminFeatureCatalogService {

    private static final Pattern CUSTOM_KEY_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    private final AppFeatureRepository appFeatureRepository;
    private final PlatformAuditService platformAuditService;
    private final PlatformAuditLogRepository platformAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<FeatureCatalogResponse> listCatalog(boolean includeDeprecated) {
        return appFeatureRepository.findAll().stream()
                .filter(f -> (f.getIsDeleted() == null || !f.getIsDeleted())
                        && (includeDeprecated || f.getIsDeprecated() == null || !f.getIsDeprecated()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeatureCatalogResponse getCatalogItem(String key) {
        String rawKey = required(key, "key is required");
        AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(rawKey)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));
        if (feature.getIsDeleted() != null && feature.getIsDeleted()) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found");
        }
        return toResponse(feature);
    }

    @Transactional
    public FeatureCatalogResponse createFeature(FeatureCatalogCreateRequest request, Long actorAuthId) {
        String rawKey = required(request.getKey(), "key is required");
        String name = required(request.getName(), "name is required");
        FeatureScope scope = parseScope(request.getScope());
        FeatureType type = parseType(request.getType());

        String normalizedKey = normalizeKey(rawKey, type);
        if (appFeatureRepository.existsByCodeIgnoreCase(normalizedKey)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "KEY_EXISTS", "feature key already exists");
        }

        if (type == FeatureType.CORE) {
            CoreFeature core = CoreFeature.fromCode(normalizedKey);
            if (core == null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_CORE_FEATURE", "core key not found in CoreFeature enum");
            }
        } else {
            if (CoreFeature.isCoreCode(normalizedKey)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "CORE_KEY_COLLISION", "custom key collides with core feature");
            }
        }

        AppFeature feature = AppFeature.builder()
                .code(normalizedKey)
                .name(name.trim())
                .description(normalizeOptional(request.getDescription()))
                .scope(scope)
                .type(type)
                .defaultEnabled(Boolean.TRUE.equals(request.getDefaultEnabled()))
                .build();
        feature = appFeatureRepository.save(feature);

        platformAuditService.log(
                actorAuthId,
                "FEATURE_CATALOG_CREATED",
                "AppFeature",
                String.valueOf(feature.getId()),
                "key=" + feature.getCode() + ", type=" + feature.getType() + ", scope=" + feature.getScope()
        );

        return toResponse(feature);
    }

    @Transactional
    public FeatureCatalogResponse updateFeature(String key, FeatureCatalogCreateRequest request, Long actorAuthId) {
        String rawKey = required(key, "key is required");
        AppFeature existing = appFeatureRepository.findByCodeIgnoreCase(rawKey)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));

        FeatureType type = existing.getType() != null ? existing.getType() : FeatureType.CORE;
        String normalizedKey = normalizeKey(rawKey, type);
        if (!existing.getCode().equalsIgnoreCase(normalizedKey)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "KEY_MISMATCH", "key mismatch");
        }

        FeatureScope scope = parseScope(request.getScope());
        boolean defaultEnabled = Boolean.TRUE.equals(request.getDefaultEnabled());
        String name = required(request.getName(), "name is required");

        Map<String, Object> before = featureSnapshot(existing);
        existing.setName(name.trim());
        existing.setDescription(normalizeOptional(request.getDescription()));
        existing.setScope(scope);
        existing.setDefaultEnabled(defaultEnabled);
        AppFeature saved = appFeatureRepository.save(existing);

        platformAuditService.logWithSnapshots(
                actorAuthId,
                "FEATURE_CATALOG_UPDATED",
                "AppFeature",
                String.valueOf(saved.getId()),
                before,
                featureSnapshot(saved),
                buildFeatureUpdateDetails(before, featureSnapshot(saved), saved.getCode())
        );

        return toResponse(saved);
    }

    @Transactional
    public void deleteFeature(String key, Long actorAuthId) {
        String rawKey = required(key, "key is required");
        AppFeature existing = appFeatureRepository.findByCodeIgnoreCase(rawKey)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));
        if (existing.getType() == FeatureType.CORE || CoreFeature.isCoreCode(existing.getCode())) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CORE_FEATURE_LOCKED", "core features cannot be deleted");
        }
        existing.setIsDeprecated(true);
        existing.setDeprecatedAt(java.time.Instant.now());
        appFeatureRepository.save(existing);
        platformAuditService.log(
                actorAuthId,
                "FEATURE_CATALOG_DEPRECATED",
                "AppFeature",
                String.valueOf(existing.getId()),
                "key=" + existing.getCode()
        );
    }

    @Transactional
    public FeatureCatalogResponse patchFeature(String key, com.smart.therapy.flow.superadmin.dto.FeatureCatalogUpdateRequest request, Long actorAuthId) {
        String rawKey = required(key, "key is required");
        AppFeature existing = appFeatureRepository.findByCodeIgnoreCase(rawKey)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));
        if (existing.getIsDeleted() != null && existing.getIsDeleted()) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found");
        }
        if (existing.getIsDeprecated() != null && existing.getIsDeprecated()) {
            throw new StoryApiException(HttpStatus.CONFLICT, "FEATURE_DEPRECATED", "feature is deprecated");
        }

        Map<String, Object> before = featureSnapshot(existing);
        if (StringUtils.hasText(request.getName())) {
            existing.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            existing.setDescription(normalizeOptional(request.getDescription()));
        }
        if (request.getScope() != null) {
            existing.setScope(parseScope(request.getScope()));
        }
        if (request.getDefaultEnabled() != null) {
            existing.setDefaultEnabled(request.getDefaultEnabled());
        }
        if (request.getDeprecated() != null) {
            if (Boolean.TRUE.equals(request.getDeprecated()) && CoreFeature.isCoreCode(existing.getCode())) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "CORE_FEATURE_LOCKED", "core features cannot be deprecated");
            }
            existing.setIsDeprecated(request.getDeprecated());
            existing.setDeprecatedAt(Boolean.TRUE.equals(request.getDeprecated()) ? java.time.Instant.now() : null);
        }
        AppFeature saved = appFeatureRepository.save(existing);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "FEATURE_CATALOG_PATCHED",
                "AppFeature",
                String.valueOf(saved.getId()),
                before,
                featureSnapshot(saved),
                buildFeatureUpdateDetails(before, featureSnapshot(saved), saved.getCode())
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public String exportCatalogCsv() {
        StringBuilder csv = new StringBuilder("id,key,name,description,scope,type,defaultEnabled,deprecated,deprecatedAt,createdAt\n");
        for (AppFeature feature : appFeatureRepository.findAll()) {
            if (feature.getIsDeleted() != null && feature.getIsDeleted()) {
                continue;
            }
            if (feature.getIsDeprecated() != null && feature.getIsDeprecated()) {
                continue;
            }
            csv.append("feat_").append(feature.getId()).append(',')
                    .append(safeCsv(feature.getCode())).append(',')
                    .append(safeCsv(feature.getName())).append(',')
                    .append(safeCsv(feature.getDescription())).append(',')
                    .append(safeCsv(feature.getScope() != null ? feature.getScope().name().toLowerCase(Locale.ROOT) : null)).append(',')
                    .append(safeCsv(feature.getType() != null ? feature.getType().name().toLowerCase(Locale.ROOT) : null)).append(',')
                    .append(Boolean.TRUE.equals(feature.getDefaultEnabled()))
                    .append(',')
                    .append(Boolean.TRUE.equals(feature.getIsDeprecated()))
                    .append(',')
                    .append(feature.getDeprecatedAt())
                    .append(',')
                    .append(feature.getCreatedAt())
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional
    public Map<String, Object> importCatalogCsv(String csvContent, boolean upsert, Long actorAuthId) {
        if (!StringUtils.hasText(csvContent)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CSV content is required");
        }
        List<String> lines = csvContent.lines().toList();
        if (lines.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CSV content is empty");
        }
        int startIndex = 0;
        List<String> header = parseCsvLine(lines.get(0));
        if (!header.isEmpty() && header.get(0).toLowerCase(Locale.ROOT).contains("key")) {
            startIndex = 1;
        }

        int created = 0;
        int updated = 0;
        List<Map<String, Object>> errors = new java.util.ArrayList<>();

        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 6) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid CSV row");
                }
                FeatureCatalogCreateRequest request = new FeatureCatalogCreateRequest();
                request.setKey(cols.get(0));
                request.setName(cols.get(1));
                request.setDescription(cols.get(2));
                request.setScope(cols.get(3));
                request.setType(cols.get(4));
                request.setDefaultEnabled(Boolean.parseBoolean(cols.get(5)));

                if (upsert && appFeatureRepository.findByCodeIgnoreCase(cols.get(0)).isPresent()) {
                    updateFeature(cols.get(0), request, actorAuthId);
                    updated++;
                } else {
                    createFeature(request, actorAuthId);
                    created++;
                }
            } catch (Exception ex) {
                errors.add(Map.of(
                        "line", i + 1,
                        "error", ex.getMessage()
                ));
            }
        }

        return Map.of(
                "created", created,
                "updated", updated,
                "errors", errors,
                "total", created + updated + errors.size()
        );
    }

    @Transactional
    public Map<String, Object> upsertCatalogBulk(List<FeatureCatalogCreateRequest> requests, Long actorAuthId) {
        if (requests == null || requests.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "items are required");
        }
        int created = 0;
        int updated = 0;
        List<Map<String, Object>> errors = new java.util.ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            FeatureCatalogCreateRequest request = requests.get(i);
            try {
                if (request == null || !StringUtils.hasText(request.getKey())) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "key is required");
                }
                if (appFeatureRepository.findByCodeIgnoreCase(request.getKey()).isPresent()) {
                    updateFeature(request.getKey(), request, actorAuthId);
                    updated++;
                } else {
                    createFeature(request, actorAuthId);
                    created++;
                }
            } catch (Exception ex) {
                errors.add(Map.of(
                        "index", i,
                        "key", request != null ? request.getKey() : null,
                        "error", ex.getMessage()
                ));
            }
        }
        return Map.of(
                "created", created,
                "updated", updated,
                "errors", errors,
                "total", requests.size()
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntryResponse> listCatalogAuditLogs(int page, int size) {
        return platformAuditLogRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .filter(log -> log.getAction() != null && log.getAction().startsWith("FEATURE_CATALOG_"))
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntryResponse> listCatalogAuditLogsByKey(String key, int page, int size) {
        String rawKey = required(key, "key is required");
        AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(rawKey)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));
        String resourceId = String.valueOf(feature.getId());
        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );
        Specification<PlatformAuditLog> spec = (root, query, cb) -> cb.and(
                cb.like(root.get("action"), "FEATURE_CATALOG_%"),
                cb.equal(root.get("resourceType"), "AppFeature"),
                cb.equal(root.get("resourceId"), resourceId)
        );
        return platformAuditLogRepository.findAll(spec, pageable)
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new java.util.ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    private static String normalizeKey(String key, FeatureType type) {
        if (type == FeatureType.CORE) {
            return key.trim().toUpperCase(Locale.ROOT);
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (!CUSTOM_KEY_PATTERN.matcher(normalized).matches()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_KEY", "key must match [a-z0-9_]+");
        }
        return normalized;
    }

    private static FeatureScope parseScope(String value) {
        if (!StringUtils.hasText(value)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_SCOPE", "scope is required");
        }
        try {
            return FeatureScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_SCOPE", "scope must be global, tenant, or user");
        }
    }

    private static FeatureType parseType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "type is required");
        }
        try {
            return FeatureType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "type must be core or custom");
        }
    }

    private static String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private FeatureCatalogResponse toResponse(AppFeature feature) {
        return FeatureCatalogResponse.builder()
                .id("feat_" + feature.getId())
                .key(feature.getCode())
                .name(feature.getName())
                .description(feature.getDescription())
                .scope(feature.getScope() != null ? feature.getScope().name().toLowerCase(Locale.ROOT) : null)
                .type(feature.getType() != null ? feature.getType().name().toLowerCase(Locale.ROOT) : null)
                .defaultEnabled(feature.getDefaultEnabled())
                .deprecated(Boolean.TRUE.equals(feature.getIsDeprecated()))
                .deprecatedAt(feature.getDeprecatedAt())
                .createdAt(feature.getCreatedAt())
                .build();
    }

    private AuditLogEntryResponse toAuditLogResponse(PlatformAuditLog log) {
        AuditLogEntryResponse r = new AuditLogEntryResponse();
        r.setId(log.getId());
        r.setAuthId(log.getAuthId());
        r.setAction(log.getAction());
        r.setResourceType(log.getResourceType());
        r.setResourceId(log.getResourceId());
        String rawDetails = log.getDetails();
        r.setDetails(rawDetails);
        if (rawDetails != null && rawDetails.trim().startsWith("{")) {
            try {
                Map<String, Object> payload = objectMapper.readValue(rawDetails, new TypeReference<>() {});
                if (payload.get("before") != null) {
                    r.setBefore(payload.get("before"));
                }
                if (payload.get("after") != null) {
                    r.setAfter(payload.get("after"));
                }
                Object details = payload.get("details");
                if (details instanceof String detailsText && !detailsText.isBlank()) {
                    r.setDetails(detailsText);
                }
            } catch (Exception ignored) {
                // Keep raw details for backward compatibility with non-JSON historical entries.
            }
        }
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }

    private static Map<String, Object> featureSnapshot(AppFeature feature) {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("key", feature.getCode());
        snapshot.put("name", feature.getName());
        snapshot.put("description", feature.getDescription());
        snapshot.put("scope", feature.getScope() != null ? feature.getScope().name() : null);
        snapshot.put("defaultEnabled", Boolean.TRUE.equals(feature.getDefaultEnabled()));
        snapshot.put("deprecated", Boolean.TRUE.equals(feature.getIsDeprecated()));
        return snapshot;
    }

    private static String buildFeatureUpdateDetails(Map<String, Object> before, Map<String, Object> after, String key) {
        List<String> changes = new java.util.ArrayList<>();
        for (String field : List.of("name", "description", "scope", "defaultEnabled", "deprecated")) {
            Object b = before.get(field);
            Object a = after.get(field);
            if (!java.util.Objects.equals(b, a)) {
                changes.add(field + ": " + b + " -> " + a);
            }
        }
        if (changes.isEmpty()) {
            return "Feature " + key + " updated (no material field changes)";
        }
        return "Feature " + key + " updated; " + String.join(", ", changes);
    }

    private static String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
