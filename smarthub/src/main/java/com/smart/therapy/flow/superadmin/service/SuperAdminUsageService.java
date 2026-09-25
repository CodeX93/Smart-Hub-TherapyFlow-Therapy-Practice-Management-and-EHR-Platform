package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.subscription.entity.FeatureUsage;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageMetricResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageTargetResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageTargetsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminUsageService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OrganisationRepository organisationRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final TenantDirectoryService tenantDirectoryService;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final UserRepository userRepository;
    private final TenantTransactionExecutor tenantTransactionExecutor;

    @Transactional(readOnly = true)
    public SuperAdminUsageResponse getUsage(Long organisationId, String periodText, String targetKey) {
        if (organisationId == null || !organisationRepository.existsById(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }

        String normalizedTargetKey = normalizeAndValidateTargetKey(organisationId, targetKey);

        YearMonth period = parsePeriod(periodText);
        Instant periodStart = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<FeatureUsage> usageRows = subscriptionFeatureService.listUsageForPeriod(organisationId, period, normalizedTargetKey);
        Map<String, Long> usedByFeature = usageRows.stream()
                .filter(u -> u.getFeature() != null && u.getFeature().getCode() != null)
                .collect(Collectors.toMap(
                        u -> u.getFeature().getCode(),
                        FeatureUsage::getUsageCount,
                        Math::max
                ));

        List<SuperAdminUsageMetricResponse> metrics = Arrays.stream(CoreFeature.values())
                .filter(CoreFeature::isLimitType)
                .sorted(Comparator.comparing(CoreFeature::getCode))
                .map(feature -> {
                    Long used = usedByFeature.getOrDefault(feature.getCode(), 0L);
                    long normalizedUsed = normalizeUsedForFeature(feature.getCode(), used);
                    Integer limit = subscriptionFeatureService.getEffectiveLimit(organisationId, normalizedTargetKey, feature.getCode(), periodStart);
                    return new SuperAdminUsageMetricResponse(feature.getCode(), normalizedUsed, limit);
                })
                .toList();

        SuperAdminUsageResponse response = new SuperAdminUsageResponse();
        response.setOrganisationId(organisationId);
        response.setPeriod(period.format(YEAR_MONTH_FORMATTER));
        response.setTargetKey(normalizedTargetKey);
        response.setMetrics(metrics);
        return response;
    }

    @Transactional(readOnly = true)
    public SuperAdminUsageTargetsResponse getUsageTargets(Long organisationId, String periodText) {
        if (organisationId == null || !organisationRepository.existsById(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }

        YearMonth period = parsePeriod(periodText);
        Instant periodStart = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<FeatureUsage> usageRows = subscriptionFeatureService.listUsageForPeriodTargets(organisationId, period);
        Map<String, Map<String, Long>> usedByTarget = new HashMap<>();
        for (FeatureUsage usage : usageRows) {
            if (usage.getFeature() == null || usage.getFeature().getCode() == null || usage.getTargetKey() == null) {
                continue;
            }
            usedByTarget
                    .computeIfAbsent(usage.getTargetKey(), k -> new HashMap<>())
                    .merge(usage.getFeature().getCode(), usage.getUsageCount(), Math::max);
        }

        Set<Long> userIds = usedByTarget.keySet().stream()
                .map(this::safeParseUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, com.smart.therapy.flow.auth.entity.User> users = resolveUsersInTenant(organisationId, userIds);

        List<SuperAdminUsageTargetResponse> targets = new ArrayList<>();
        for (String targetKey : usedByTarget.keySet().stream().sorted().toList()) {
            Long userId = safeParseUserId(targetKey);
            com.smart.therapy.flow.auth.entity.User user = userId != null ? users.get(userId) : null;
            Map<String, Long> usedByFeature = usedByTarget.getOrDefault(targetKey, Map.of());

            List<SuperAdminUsageMetricResponse> metrics = Arrays.stream(CoreFeature.values())
                    .filter(CoreFeature::isLimitType)
                    .sorted(Comparator.comparing(CoreFeature::getCode))
                    .map(feature -> {
                        Long used = usedByFeature.getOrDefault(feature.getCode(), 0L);
                        long normalizedUsed = normalizeUsedForFeature(feature.getCode(), used);
                        Integer limit = subscriptionFeatureService.getEffectiveLimit(organisationId, targetKey, feature.getCode(), periodStart);
                        return new SuperAdminUsageMetricResponse(feature.getCode(), normalizedUsed, limit);
                    })
                    .toList();

            targets.add(new SuperAdminUsageTargetResponse(
                    targetKey,
                    userId,
                    user != null ? user.getFullName() : null,
                    user != null ? user.getEmail() : null,
                    metrics
            ));
        }

        SuperAdminUsageTargetsResponse response = new SuperAdminUsageTargetsResponse();
        response.setOrganisationId(organisationId);
        response.setPeriod(period.format(YEAR_MONTH_FORMATTER));
        response.setTargets(targets);
        return response;
    }

    @Transactional(readOnly = true)
    public String getUsageTargetsCsv(Long organisationId, String periodText) {
        SuperAdminUsageTargetsResponse response = getUsageTargets(organisationId, periodText);
        StringBuilder csv = new StringBuilder();
        csv.append("targetKey,userId,userName,userEmail,featureKey,used,limit\n");
        if (response.getTargets() == null) {
            return csv.toString();
        }
        for (SuperAdminUsageTargetResponse target : response.getTargets()) {
            if (target.getMetrics() == null || target.getMetrics().isEmpty()) {
                csv.append(escapeCsv(target.getTargetKey())).append(',')
                        .append(target.getUserId() != null ? target.getUserId() : "").append(',')
                        .append(escapeCsv(target.getUserName())).append(',')
                        .append(escapeCsv(target.getUserEmail())).append(',')
                        .append("").append(',')
                        .append("0").append(',')
                        .append("")
                        .append('\n');
                continue;
            }
            for (SuperAdminUsageMetricResponse metric : target.getMetrics()) {
                csv.append(escapeCsv(target.getTargetKey())).append(',')
                        .append(target.getUserId() != null ? target.getUserId() : "").append(',')
                        .append(escapeCsv(target.getUserName())).append(',')
                        .append(escapeCsv(target.getUserEmail())).append(',')
                        .append(escapeCsv(metric.getFeatureKey())).append(',')
                        .append(metric.getUsed()).append(',')
                        .append(metric.getLimit() != null ? metric.getLimit() : "")
                        .append('\n');
            }
        }
        return csv.toString();
    }

    private String normalizeAndValidateTargetKey(Long organisationId, String targetKey) {
        if (targetKey == null || targetKey.isBlank()) {
            return null;
        }
        String normalized = targetKey.trim();
        Long userId;
        try {
            userId = Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_TARGET_KEY", "targetKey must be a numeric user id");
        }

        TenantDirectoryService.TenantInfo tenant = tenantDirectoryService.findByOrganisationId(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found"));
        String schema = tenant.getSchemaName();
        if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema) || !tenantSchemaHealthService.schemaExists(schema)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TENANT_SCHEMA_MISSING", "Tenant schema is missing");
        }

        boolean exists = tenantTransactionExecutor.executeReadOnly(
                organisationId,
                schema,
                () -> userRepository.findById(userId).isPresent()
        );
        if (!exists) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "TARGET_NOT_FOUND", "targetKey user not found in organisation");
        }

        return normalized;
    }

    private Map<Long, com.smart.therapy.flow.auth.entity.User> resolveUsersInTenant(Long organisationId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        TenantDirectoryService.TenantInfo tenant = tenantDirectoryService.findByOrganisationId(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found"));
        String schema = tenant.getSchemaName();
        if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema) || !tenantSchemaHealthService.schemaExists(schema)) {
            return Map.of();
        }

        return tenantTransactionExecutor.executeReadOnly(
                organisationId,
                schema,
                () -> userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(com.smart.therapy.flow.auth.entity.User::getId, u -> u))
        );
    }

    private Long safeParseUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.contains(",") || trimmed.contains("\"") || trimmed.contains("\n") || trimmed.contains("\r")) {
            return "\"" + trimmed.replace("\"", "\"\"") + "\"";
        }
        return trimmed;
    }

    private long normalizeUsedForFeature(String featureKey, long used) {
        if (featureKey == null) {
            return used;
        }
        if (CoreFeature.DOCUMENT_UPLOAD_GB.getCode().equalsIgnoreCase(featureKey)) {
            return bytesToGbCeil(used);
        }
        return used;
    }

    private long bytesToGbCeil(long bytes) {
        if (bytes <= 0) {
            return 0;
        }
        long gb = 1024L * 1024L * 1024L;
        return (bytes + gb - 1) / gb;
    }

    private YearMonth parsePeriod(String periodText) {
        if (periodText == null || periodText.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        String normalized = periodText.trim();
        if (!PERIOD_PATTERN.matcher(normalized).matches()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be in YYYY-MM format");
        }
        try {
            return YearMonth.parse(normalized, YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be in YYYY-MM format");
        }
    }

}
