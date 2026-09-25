package com.smart.therapy.flow.system.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.system.dto.PracticeConfigurationRequest;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.entity.PracticeConfiguration;
import com.smart.therapy.flow.system.repository.PracticeConfigurationRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PracticeConfigurationService {

    private static final String RESOURCE_TYPE_PRACTICE_CONFIG = "practice_configuration";

    private final PracticeConfigurationRepository practiceConfigurationRepository;
    private final OrganisationRepository organisationRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @Cacheable(value = "practiceConfiguration", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('practiceConfiguration')")
    public PracticeConfigurationResponse getPracticeConfiguration() {
        PracticeConfiguration config = practiceConfigurationRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    // Return default configuration if none exists
                    log.warn("No practice configuration found, building defaults from organisation");
                    PracticeConfiguration newConfig = PracticeConfiguration.builder().build();
                    Long orgId = TenantContext.getOrganisationId();
                    if (orgId != null) {
                        organisationRepository.findById(orgId).ifPresent(org -> {
                            newConfig.setPracticeName(org.getName());
                            newConfig.setTimezone(org.getTimezone());
                        });
                    }
                    return newConfig;
                });
        // If practice row exists but timezone was never set, fall back to organisation settings.
        if (!StringUtils.hasText(config.getTimezone())) {
            Long orgId = TenantContext.getOrganisationId();
            if (orgId != null) {
                organisationRepository.findById(orgId).ifPresent(org -> {
                    if (StringUtils.hasText(org.getTimezone())) {
                        config.setTimezone(org.getTimezone());
                    }
                });
            }
        }
        return toPracticeConfigurationResponse(config);
    }

    @Transactional
    @CacheEvict(value = "practiceConfiguration", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('practiceConfiguration')")
    public PracticeConfigurationResponse updatePracticeConfiguration(
            PracticeConfigurationRequest request,
            AuthPrincipal requester,
            String ipAddress
    ) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update practice configuration");

        // Validate timezone if provided
        if (StringUtils.hasText(request.getTimezone())) {
            try {
                java.time.ZoneId.of(request.getTimezone());
            } catch (Exception e) {
                throw new BadRequestException("Invalid timezone: " + request.getTimezone());
            }
        }

        // Get existing configuration or create new one (singleton pattern)
        PracticeConfiguration config = practiceConfigurationRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> PracticeConfiguration.builder().build());

        // Update fields
        if (StringUtils.hasText(request.getPracticeName())) {
            config.setPracticeName(request.getPracticeName().trim());
        }
        if (request.getPracticeAddress() != null) {
            config.setPracticeAddress(request.getPracticeAddress().trim());
        }
        if (StringUtils.hasText(request.getPracticePhone())) {
            config.setPracticePhone(request.getPracticePhone().trim());
        }
        if (StringUtils.hasText(request.getPracticeEmail())) {
            config.setPracticeEmail(request.getPracticeEmail().trim());
        }
        if (StringUtils.hasText(request.getPracticeWebsite())) {
            String website = request.getPracticeWebsite().trim();
            // Ensure URL has protocol
            if (!website.startsWith("http://") && !website.startsWith("https://")) {
                website = "https://" + website;
            }
            config.setPracticeWebsite(website);
        }
        if (request.getTaxId() != null) {
            config.setTaxId(request.getTaxId().trim());
        }
        if (request.getLicenseNumber() != null) {
            config.setLicenseNumber(request.getLicenseNumber().trim());
        }
        if (request.getLicenseState() != null) {
            config.setLicenseState(request.getLicenseState().trim());
        }
        if (StringUtils.hasText(request.getNpiNumber())) {
            config.setNpiNumber(request.getNpiNumber().trim());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription().trim());
        }
        if (request.getSubtitle() != null) {
            config.setSubtitle(request.getSubtitle().trim());
        }
        boolean timezoneUpdated = false;
        String newTimezone = null;
        if (StringUtils.hasText(request.getTimezone())) {
            newTimezone = request.getTimezone().trim();
            if (!newTimezone.equals(config.getTimezone())) {
                config.setTimezone(newTimezone);
                timezoneUpdated = true;
            }
        }

        PracticeConfiguration saved = practiceConfigurationRepository.save(config);
        
        if (timezoneUpdated) {
            Long orgId = TenantContext.getOrganisationId();
            if (orgId != null) {
                final String finalTimezoneToSync = newTimezone;
                organisationRepository.findById(orgId).ifPresent(org -> {
                    org.setTimezone(finalTimezoneToSync);
                    organisationRepository.save(org);
                    log.info("Synced updated practice timezone {} back to organisation {}", finalTimezoneToSync, orgId);
                });
            }
        }

        Long savedId = saved.getId();

        recordAuditEvent(currentUserService.getCurrentUserId(requester), "practice_configuration_updated", savedId, ipAddress);

        return toPracticeConfigurationResponse(saved);
    }

    private PracticeConfigurationResponse toPracticeConfigurationResponse(PracticeConfiguration config) {
        return PracticeConfigurationResponse.builder()
                .id(config.getId())
                .practiceName(config.getPracticeName())
                .practiceAddress(config.getPracticeAddress())
                .practicePhone(config.getPracticePhone())
                .practiceEmail(config.getPracticeEmail())
                .practiceWebsite(config.getPracticeWebsite())
                .taxId(config.getTaxId())
                .licenseNumber(config.getLicenseNumber())
                .licenseState(config.getLicenseState())
                .npiNumber(config.getNpiNumber())
                .description(config.getDescription())
                .subtitle(config.getSubtitle())
                .timezone(config.getTimezone())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_PRACTICE_CONFIG, resourceId, null,
                    ipAddress, false);
        } catch (Exception e) {
            log.error("Failed to record audit event for practice configuration: {}", resourceId, e);
        }
    }

    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return permissionChecker.hasRole(principal, roleName);
    }

    private void assertAdmin(AuthPrincipal principal, String message) {
        permissionChecker.requireConsentAdminModuleAccess(principal, message);
    }
}



