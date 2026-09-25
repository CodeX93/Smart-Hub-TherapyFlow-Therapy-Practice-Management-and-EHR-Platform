package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.OrganisationDomainAlias;
import com.smart.therapy.flow.organisation.repository.OrganisationDomainAliasRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.ReservedSubdomainRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory tenant directory. Avoids DB hit on every request (TenantFilter).
 * Warm on startup; refresh every 60 seconds. Reserved subdomains loaded from DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDirectoryService {

    private final OrganisationRepository organisationRepository;
    private final ReservedSubdomainRepository reservedSubdomainRepository;
    private final OrganisationDomainAliasRepository organisationDomainAliasRepository;

    /** By subdomain (lowercase) -> tenant info. */
    @Getter
    private volatile Map<String, TenantInfo> bySubdomain = new ConcurrentHashMap<>();
    /** By schema name -> tenant info. */
    @Getter
    private volatile Map<String, TenantInfo> bySchemaName = new ConcurrentHashMap<>();
    /** By slug (lowercase) -> tenant info. */
    @Getter
    private volatile Map<String, TenantInfo> bySlug = new ConcurrentHashMap<>();
    /** By organisation id -> tenant info. */
    @Getter
    private volatile Map<Long, TenantInfo> byOrganisationId = new ConcurrentHashMap<>();
    /** Reserved subdomains (lowercase). No tenant lookup for these. */
    @Getter
    private volatile Set<String> reservedSubdomains = Set.of();
    @Value("${tenant.jobs.directory-refresh.enabled:true}")
    private boolean directoryRefreshEnabled;
    private volatile Boolean directoryRefreshEnabledOverride;

    @PostConstruct
    public void warm() {
        refreshInternal();
    }

    @Scheduled(fixedDelayString = "${tenant.directory.refresh-ms:60000}", initialDelay = 5000)
    public void refresh() {
        if (!isDirectoryRefreshEnabled()) {
            return;
        }
        refreshInternal();
    }

    public void runDirectoryRefreshNow() {
        refreshInternal();
    }

    public void setDirectoryRefreshEnabledOverride(Boolean enabled) {
        this.directoryRefreshEnabledOverride = enabled;
    }

    public Boolean getDirectoryRefreshEnabledOverride() {
        return directoryRefreshEnabledOverride;
    }

    public boolean isDirectoryRefreshEnabled() {
        return directoryRefreshEnabledOverride != null ? directoryRefreshEnabledOverride : directoryRefreshEnabled;
    }

    private void refreshInternal() {
        try {
            Map<String, TenantInfo> sub = new ConcurrentHashMap<>();
            Map<String, TenantInfo> schema = new ConcurrentHashMap<>();
            Map<Long, TenantInfo> byOrgId = new java.util.HashMap<>();
            Map<String, TenantInfo> slugMap = new ConcurrentHashMap<>();
            for (Organisation org : organisationRepository.findAll()) {
                TenantInfo info = new TenantInfo(org.getId(), org.getSchemaName(), org.getSubdomain(), org.getSlug(),
                        org.getStatus(), org.getForceDisabledReason());
                byOrgId.put(org.getId(), info);
                if (org.getSlug() != null && !org.getSlug().isBlank()) {
                    slugMap.put(org.getSlug().toLowerCase(), info);
                }
                if (org.getSchemaName() != null && !org.getSchemaName().isBlank()) {
                    schema.put(org.getSchemaName(), info);
                }
                if (org.getSubdomain() != null && !org.getSubdomain().isBlank()) {
                    sub.put(org.getSubdomain().toLowerCase(), info);
                }
            }
            for (OrganisationDomainAlias alias : organisationDomainAliasRepository.findAll()) {
                TenantInfo info = byOrgId.get(alias.getOrganisationId());
                if (info != null && alias.getAliasSubdomain() != null && !alias.getAliasSubdomain().isBlank()) {
                    sub.put(alias.getAliasSubdomain().toLowerCase(), info);
                }
            }
            bySubdomain = sub;
            bySchemaName = schema;
            bySlug = slugMap;
            byOrganisationId = new ConcurrentHashMap<>(byOrgId);

            Set<String> reserved = reservedSubdomainRepository.findAll().stream()
                    .map(r -> r.getSubdomain() != null ? r.getSubdomain().toLowerCase() : null)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
            reservedSubdomains = reserved;
            log.debug("Tenant directory refreshed: {} orgs, {} reserved subdomains", sub.size(), reserved.size());
        } catch (Exception e) {
            log.error("Tenant directory refresh failed", e);
        }
    }

    public Optional<TenantInfo> findBySubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) return Optional.empty();
        return Optional.ofNullable(bySubdomain.get(subdomain.toLowerCase()));
    }

    public Optional<TenantInfo> findBySchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) return Optional.empty();
        return Optional.ofNullable(bySchemaName.get(schemaName));
    }

    public Optional<TenantInfo> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return Optional.ofNullable(bySlug.get(slug.toLowerCase()));
    }

    public Optional<TenantInfo> findByOrganisationId(Long orgId) {
        if (orgId == null) return Optional.empty();
        return Optional.ofNullable(byOrganisationId.get(orgId));
    }

    public Optional<TenantInfo> findByOrganisationIdOrSlug(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String trimmed = value.trim();
        try {
            Long id = Long.parseLong(trimmed);
            return findByOrganisationId(id);
        } catch (NumberFormatException ignored) {
            return findBySlug(trimmed);
        }
    }

    public boolean isReservedSubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) return false;
        return reservedSubdomains.contains(subdomain.toLowerCase());
    }

    /** Call after super-admin creates/updates an org so cache is fresh without waiting for schedule. */
    public void evictCache() {
        refresh();
    }

    /** Lifecycle: ACTIVE (allow), LOCKED (503), ARCHIVED (410), DELETED (410). */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_DELETED = "DELETED";

    @Getter
    public static final class TenantInfo {
        private final Long organisationId;
        private final String schemaName;
        private final String subdomain;
        private final String slug;
        private final String status;
        private final String forceDisabledReason;

        public TenantInfo(Long organisationId, String schemaName, String subdomain, String status, String forceDisabledReason) {
            this(organisationId, schemaName, subdomain, null, status, forceDisabledReason);
        }

        public TenantInfo(Long organisationId, String schemaName, String subdomain, String slug, String status, String forceDisabledReason) {
            this.organisationId = organisationId;
            this.schemaName = schemaName;
            this.subdomain = subdomain;
            this.slug = slug;
            this.status = status;
            this.forceDisabledReason = forceDisabledReason;
        }

        public boolean isActive() {
            return STATUS_ACTIVE.equalsIgnoreCase(status);
        }

        public boolean isLocked() {
            return STATUS_LOCKED.equalsIgnoreCase(status);
        }

        public boolean isArchived() {
            return STATUS_ARCHIVED.equalsIgnoreCase(status);
        }

        public boolean isDeleted() {
            return STATUS_DELETED.equalsIgnoreCase(status);
        }

        /** True when super-admin set force_disabled_reason (emergency disable). */
        public boolean isForceDisabled() {
            return forceDisabledReason != null && !forceDisabledReason.isBlank();
        }
    }
}
