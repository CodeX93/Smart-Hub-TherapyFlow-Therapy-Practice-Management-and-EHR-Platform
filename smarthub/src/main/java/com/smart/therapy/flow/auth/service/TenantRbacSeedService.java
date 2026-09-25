package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.RolePermission;
import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.auth.repository.RolePermissionRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seeds tenant roles from existing platform role templates.
 * Idempotent by design: does not duplicate roles or role-permission rows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantRbacSeedService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final OrganisationRepository organisationRepository;

    private static final List<String> DEFAULT_TENANT_ROLE_NAMES = List.of(
            "ADMIN",
            "THERAPIST",
            "SUPERVISOR",
            "BILLING_SPECIALIST"
    );

    private static final Set<String> BLOCKED_PLATFORM_PERMISSIONS = Set.of(
            "PLATFORM_READ",
            "PLATFORM_MANAGE"
    );

    @Transactional
    public void seedDefaultsForOrganisation(Long organisationId) {
        if (organisationId == null) {
            return;
        }
        Organisation org = organisationRepository.findById(organisationId).orElse(null);
        if (org == null) {
            return;
        }

        for (String roleName : DEFAULT_TENANT_ROLE_NAMES) {
            Role platformRole = roleRepository.findByNameAndOrganisationIsNullIgnoreCase(roleName).orElse(null);
            if (platformRole == null) {
                log.warn("Tenant RBAC seed skipped missing platform role template: {}", roleName);
                continue;
            }

            // `roles.name` is globally unique in DB (`roles_name_key`), so tenant reseed
            // must reuse the existing platform role if a tenant-specific one doesn't exist.
            Role tenantRole = roleRepository.findByNameIgnoreCaseAndOrganisation_Id(roleName, organisationId)
                    .orElse(platformRole);

            if (platformRole.getRolePermissions() == null || platformRole.getRolePermissions().isEmpty()) {
                continue;
            }

            for (RolePermission rp : platformRole.getRolePermissions()) {
                Permission permission = rp.getPermission();
                if (permission == null || permission.getName() == null) {
                    continue;
                }
                if (isBlockedForTenant(permission.getName())) {
                    continue;
                }

                Permission platformPermission = permissionRepository
                        .findByNameAndOrganisationIsNullIgnoreCase(permission.getName())
                        .orElse(null);
                if (platformPermission == null) {
                    continue;
                }

                boolean exists = rolePermissionRepository
                        .findByRole_IdAndPermission_Id(tenantRole.getId(), platformPermission.getId())
                        .isPresent();
                if (!exists) {
                    rolePermissionRepository.save(RolePermission.builder()
                            .role(tenantRole)
                            .permission(platformPermission)
                            .build());
                }
            }
        }
        log.info("Tenant RBAC defaults seeded for organisation {}", organisationId);
    }

    @Transactional
    public int seedDefaultsForAllOrganisations() {
        List<Organisation> organisations = organisationRepository.findAll();
        int processed = 0;
        for (Organisation org : organisations) {
            if (org == null || org.getId() == null) {
                continue;
            }
            seedDefaultsForOrganisation(org.getId());
            processed++;
        }
        log.info("Tenant RBAC defaults seeded for {} organisations", processed);
        return processed;
    }

    private boolean isBlockedForTenant(String permissionName) {
        String normalized = permissionName.trim().toUpperCase(Locale.ROOT);
        return BLOCKED_PLATFORM_PERMISSIONS.contains(normalized) || normalized.startsWith("PLATFORM_");
    }
}
