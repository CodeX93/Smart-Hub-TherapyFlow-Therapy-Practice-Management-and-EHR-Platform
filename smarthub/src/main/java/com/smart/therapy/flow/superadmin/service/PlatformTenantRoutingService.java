package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingRequest;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformTenantRoutingSettings;
import com.smart.therapy.flow.superadmin.repository.PlatformTenantRoutingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformTenantRoutingService {

    private static final Set<String> IDENTIFIERS = Set.of("slug", "id");
    private static final Pattern PATH_PREFIX = Pattern.compile("^/[A-Za-z0-9/_-]+$");

    private final PlatformTenantRoutingSettingsRepository repository;
    private final OrganisationRepository organisationRepository;

    @Transactional
    public PlatformTenantRoutingResponse getSettings() {
        return toResponse(requireSettings());
    }

    /**
     * Returns existing routing settings, creating safe defaults when missing (e.g. after a data wipe).
     */
    @Transactional
    public PlatformTenantRoutingSettings requireSettings() {
        return repository.findTopByOrderByIdAsc().orElseGet(() -> {
            Instant now = Instant.now();
            PlatformTenantRoutingSettings created = repository.save(PlatformTenantRoutingSettings.builder()
                    .emailAutoRouting(true)
                    .pathBasedRouting(false)
                    .pathPrefix("/t")
                    .orgIdentifier("slug")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            log.warn("Created missing platform_tenant_routing_settings with email_auto_routing=true");
            return created;
        });
    }

    @Transactional
    public PlatformTenantRoutingResponse upsert(PlatformTenantRoutingRequest request, Long actorAuthId) {
        validate(request);

        PlatformTenantRoutingSettings row = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> PlatformTenantRoutingSettings.builder()
                        .createdAt(Instant.now())
                        .build());
        row.setEmailAutoRouting(request.getEmailAutoRouting());
        row.setPathBasedRouting(request.getPathBasedRouting());
        row.setPathPrefix(request.getPathPrefix().trim());
        row.setOrgIdentifier(normalizeIdentifier(request.getOrgIdentifier()));
        row.setUpdatedAt(Instant.now());
        row.setUpdatedByAuthId(actorAuthId);
        PlatformTenantRoutingSettings saved = repository.save(row);
        return toResponse(saved);
    }

    private void validate(PlatformTenantRoutingRequest request) {
        if (request == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request body is required");
        }
        String prefix = request.getPathPrefix();
        if (!StringUtils.hasText(prefix)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "pathPrefix is required");
        }
        String trimmed = prefix.trim();
        if (!PATH_PREFIX.matcher(trimmed).matches()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "pathPrefix must be URL-safe");
        }
        String identifier = normalizeIdentifier(request.getOrgIdentifier());
        if (!IDENTIFIERS.contains(identifier)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "orgIdentifier must be slug or id");
        }
        if (organisationRepository.count() == 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "org records missing");
        }
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }

    private static PlatformTenantRoutingResponse toResponse(PlatformTenantRoutingSettings row) {
        return PlatformTenantRoutingResponse.builder()
                .emailAutoRouting(row.getEmailAutoRouting())
                .pathBasedRouting(row.getPathBasedRouting())
                .pathPrefix(row.getPathPrefix())
                .orgIdentifier(row.getOrgIdentifier())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
