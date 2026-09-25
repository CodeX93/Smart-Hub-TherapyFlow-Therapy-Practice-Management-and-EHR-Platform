package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationAccessBlockRepository;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantResolutionService {

    private final AuthIdentityRepository authIdentityRepository;
    private final UserOrganisationRepository userOrganisationRepository;
    private final UserOrganisationAccessBlockRepository userOrganisationAccessBlockRepository;
    private final TenantDirectoryService tenantDirectoryService;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserOrganisation> resolveByEmail(String email) {
        return resolveByEmail(email, null);
    }

    /**
     * Resolve organisations for an email/username.
     *
     * @param identityType when non-null, only identities of that type are considered
     *                     (e.g. {@link IdentityType#STAFF} for staff login, {@link IdentityType#CLIENT} for portal)
     */
    @Transactional(readOnly = true)
    public List<UserOrganisation> resolveByEmail(String email, IdentityType identityType) {
        String normalized = normalize(email);
        if (normalized == null) {
            return List.of();
        }

        List<AuthIdentity> identities = resolveIdentities(normalized, identityType);
        if (identities.isEmpty()) {
            return List.of();
        }

        Map<Long, UserOrganisation> byOrganisationId = new LinkedHashMap<>();
        for (AuthIdentity identity : identities) {
            if (identity == null || identity.getId() == null) {
                continue;
            }

            List<Long> blockedOrgIds = userOrganisationAccessBlockRepository.findBlockedOrganisationIds(identity.getId());
            List<UserOrganisation> rows = userOrganisationRepository.findByAuth_Id(identity.getId());
            for (UserOrganisation row : rows) {
                Long orgId = row != null && row.getOrganisation() != null ? row.getOrganisation().getId() : null;
                if (orgId == null || blockedOrgIds.contains(orgId)) {
                    continue;
                }
                byOrganisationId.putIfAbsent(orgId, row);
            }

            // Backward-compatible fallback: some records may not have a user_organisations row yet.
            if (identity.getOrganisation() != null && identity.getOrganisation().getId() != null) {
                Long directOrgId = identity.getOrganisation().getId();
                if (!blockedOrgIds.contains(directOrgId) && !byOrganisationId.containsKey(directOrgId)) {
                    byOrganisationId.put(directOrgId, UserOrganisation.builder()
                            .auth(identity)
                            .organisation(identity.getOrganisation())
                            .createdAt(identity.getCreatedAt() != null ? identity.getCreatedAt() : Instant.now())
                            .build());
                }
            }
        }

        return new ArrayList<>(byOrganisationId.values());
    }

    @Transactional(readOnly = true)
    public boolean emailBelongsToOrganisation(String email, Long organisationId) {
        return emailBelongsToOrganisation(email, organisationId, null);
    }

    @Transactional(readOnly = true)
    public boolean emailBelongsToOrganisation(String email, Long organisationId, IdentityType identityType) {
        String normalized = normalize(email);
        if (normalized == null || organisationId == null) {
            return false;
        }

        for (AuthIdentity identity : resolveIdentities(normalized, identityType)) {
            if (identity == null || identity.getId() == null) {
                continue;
            }
            boolean linkedViaUserOrganisation =
                    userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(identity.getId(), organisationId);
            boolean linkedViaIdentityOrganisation =
                    identity.getOrganisation() != null && organisationId.equals(identity.getOrganisation().getId());
            if (!linkedViaUserOrganisation && !linkedViaIdentityOrganisation) {
                continue;
            }
            if (!userOrganisationAccessBlockRepository.existsByAuth_IdAndOrganisation_Id(identity.getId(), organisationId)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isEmailBlockedInOrganisation(String email, Long organisationId) {
        String normalized = normalize(email);
        if (normalized == null || organisationId == null) {
            return false;
        }
        for (AuthIdentity identity : resolveIdentities(normalized, null)) {
            if (identity != null
                    && identity.getId() != null
                    && userOrganisationAccessBlockRepository.existsByAuth_IdAndOrganisation_Id(identity.getId(), organisationId)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public boolean isAuthBlockedInOrganisation(Long authId, Long organisationId) {
        if (authId == null || organisationId == null) {
            return false;
        }
        return userOrganisationAccessBlockRepository.existsByAuth_IdAndOrganisation_Id(authId, organisationId);
    }

    private List<AuthIdentity> resolveIdentities(String normalizedInput, IdentityType identityType) {
        Map<Long, AuthIdentity> identities = new LinkedHashMap<>();
        for (AuthIdentity identity : authIdentityRepository.findAllByEmailOrUsername(normalizedInput)) {
            if (identity != null
                    && identity.getId() != null
                    && matchesIdentityType(identity, identityType)) {
                identities.put(identity.getId(), identity);
            }
        }

        // Legacy fallback: tenant profile email when public directory is incomplete.
        // Only applies to staff-style lookups (null filter or STAFF) — portal CLIENT emails live on auth_identities.
        if (looksLikeEmail(normalizedInput)
                && (identityType == null || identityType == IdentityType.STAFF)) {
            for (Long authId : findAuthIdsByTenantUserEmail(normalizedInput)) {
                authIdentityRepository.findById(authId).ifPresent(identity -> {
                    if (matchesIdentityType(identity, identityType)) {
                        identities.putIfAbsent(authId, identity);
                    }
                });
            }
        }
        return new ArrayList<>(identities.values());
    }

    private static boolean matchesIdentityType(AuthIdentity identity, IdentityType identityType) {
        if (identityType == null) {
            return true;
        }
        return identity.getIdentityType() == identityType;
    }

    private Set<Long> findAuthIdsByTenantUserEmail(String normalizedEmail) {
        Set<Long> authIds = new HashSet<>();
        for (TenantDirectoryService.TenantInfo tenantInfo : tenantDirectoryService.getByOrganisationId().values()) {
            if (tenantInfo == null || tenantInfo.getOrganisationId() == null) {
                continue;
            }
            String schemaName = tenantInfo.getSchemaName();
            if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
                continue;
            }
            try {
                Long authId = tenantTransactionExecutor.executeReadOnly(
                        tenantInfo.getOrganisationId(),
                        schemaName,
                        () -> userRepository.findByEmail(normalizedEmail)
                                .map(user -> user.getAuthIdentity() != null ? user.getAuthIdentity().getId() : null)
                                .orElse(null)
                );
                if (authId != null) {
                    authIds.add(authId);
                }
            } catch (Exception ex) {
                // Best-effort fallback path; ignore tenant read errors for routing resolution.
                log.debug("Tenant email fallback lookup skipped for org {}: {}",
                        tenantInfo.getOrganisationId(), ex.getMessage());
            }
        }
        return authIds;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }
}
