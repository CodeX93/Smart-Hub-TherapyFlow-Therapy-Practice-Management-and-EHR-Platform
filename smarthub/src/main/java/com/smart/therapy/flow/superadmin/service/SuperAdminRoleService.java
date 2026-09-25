package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.dto.PermissionResponse;
import com.smart.therapy.flow.auth.dto.RoleResponse;
import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.RolePermission;
import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminCreateRoleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminRoleService {

    private static final Set<String> HIDDEN_FROM_ROLE_CATALOG = Set.of(
            "CLIENT",
            "AUDITOR",
            "DEVOPS",
            "SUPPORT",
            "SYSTEM_AI_ASSISTANT"
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganisationRepository organisationRepository;
    private final PlatformAuditService platformAuditService;

    @Transactional(readOnly = true)
    public List<Permission> getPermissionCatalog() {
        return permissionRepository.findAll();
    }

    @Transactional
    public RoleResponse createRole(SuperAdminCreateRoleRequest request, Long actorAuthId) {
        Objects.requireNonNull(request, "request is required");
        String name = normalizeRequired(request.getName(), "Role name is required");
        String displayName = StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : name;

        Long organisationId = request.getOrganisationId();
        Organisation organisation = null;
        if (organisationId != null) {
            organisation = organisationRepository.findById(organisationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organisation not found"));
        }

        if (organisationId == null) {
            if (roleRepository.findByNameAndOrganisationIsNullIgnoreCase(name).isPresent()) {
                throw new IllegalArgumentException("Role name already exists");
            }
        } else {
            if (roleRepository.findByNameForOrganisation(name, organisationId).isPresent()) {
                throw new IllegalArgumentException("Role name already exists");
            }
        }

        List<Long> permissionIds = request.getPermissions() == null
                ? List.of()
                : request.getPermissions().stream().filter(Objects::nonNull).distinct().toList();
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions are invalid");
        }
        if (permissions.stream().anyMatch(p -> p.getIsActive() == null || !p.getIsActive())) {
            throw new IllegalArgumentException("One or more permissions are inactive");
        }

        Role role = Role.builder()
                .organisation(organisation)
                .name(name)
                .displayName(displayName)
                .description(normalizeOptional(request.getDescription()))
                .isSystem(false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        Role saved = roleRepository.save(role);
        syncRolePermissions(saved, permissions);

        platformAuditService.logWithSnapshots(
                actorAuthId,
                "ROLE_CREATED",
                "Role",
                String.valueOf(saved.getId()),
                null,
                roleSnapshot(saved),
                "name=" + saved.getName() + ", organisationId=" + organisationId
        );

        return toRoleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(Long organisationId) {
        List<Role> roles = organisationId == null
                ? roleRepository.findAllWithPermissions()
                : roleRepository.findAllForOrganisation(organisationId);
        return roles.stream()
                .filter(role -> !isHiddenFromRoleCatalog(role))
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, SuperAdminCreateRoleRequest request, Long actorAuthId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Map<String, Object> before = roleSnapshot(role);
        String name = normalizeRequired(request.getName(), "Role name is required");
        String displayName = StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : name;

        Long organisationId = request.getOrganisationId();
        if (organisationId != null && !organisationRepository.existsById(organisationId)) {
            throw new IllegalArgumentException("Organisation not found");
        }

        if (role.getOrganisation() == null && organisationId != null
                || role.getOrganisation() != null && organisationId == null
                || role.getOrganisation() != null && organisationId != null && !role.getOrganisation().getId().equals(organisationId)) {
            throw new IllegalArgumentException("Role organisation cannot be changed");
        }

        if (organisationId == null) {
            roleRepository.findByNameAndOrganisationIsNullIgnoreCase(name)
                    .filter(existing -> !existing.getId().equals(roleId))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Role name already exists"); });
        } else {
            roleRepository.findByNameForOrganisation(name, organisationId)
                    .filter(existing -> !existing.getId().equals(roleId))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Role name already exists"); });
        }

        List<Long> permissionIds = request.getPermissions() == null
                ? List.of()
                : request.getPermissions().stream().filter(Objects::nonNull).distinct().toList();
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions are invalid");
        }
        if (permissions.stream().anyMatch(p -> p.getIsActive() == null || !p.getIsActive())) {
            throw new IllegalArgumentException("One or more permissions are inactive");
        }

        role.setName(name);
        role.setDisplayName(displayName);
        role.setDescription(normalizeOptional(request.getDescription()));
        boolean nextActive = request.getIsActive() != null
                ? request.getIsActive()
                : Boolean.TRUE.equals(role.getIsActive());
        role.setIsActive(nextActive);

        // Diff existing mappings instead of clear()+re-insert — Hibernate may INSERT before DELETE
        // and trip role_permissions(role_id, permission_id) unique constraint (409 DB_003).
        syncRolePermissions(role, permissions);

        Role updated = roleRepository.save(role);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "ROLE_UPDATED",
                "Role",
                String.valueOf(updated.getId()),
                before,
                roleSnapshot(updated),
                "name=" + updated.getName()
        );
        return toRoleResponse(updated);
    }

    @Transactional
    public void deleteRole(Long roleId, Long actorAuthId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Map<String, Object> before = roleSnapshot(role);
        if (role.getIsSystem() != null && role.getIsSystem()) {
            throw new IllegalArgumentException("Cannot delete system role");
        }
        if (role.getAuthIdentityRoles() != null && !role.getAuthIdentityRoles().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete role with assigned users");
        }
        roleRepository.delete(role);
        platformAuditService.logWithSnapshots(actorAuthId, "ROLE_DELETED", "Role", String.valueOf(roleId), before, null,
                "name=" + role.getName());
    }

    /**
     * Sync role↔permission joins in place. Clearing and re-adding the same permission IDs
     * makes Hibernate INSERT before DELETE and violates {@code (role_id, permission_id)}.
     */
    private void syncRolePermissions(Role role, List<Permission> permissions) {
        Set<RolePermission> managed = role.getRolePermissions();
        if (managed == null) {
            role.setRolePermissions(new java.util.HashSet<>());
            managed = role.getRolePermissions();
        }

        Set<Long> targetIds = permissions.stream()
                .map(Permission::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        managed.removeIf(existing ->
                existing.getPermission() == null
                        || existing.getPermission().getId() == null
                        || !targetIds.contains(existing.getPermission().getId()));

        Set<Long> existingIds = managed.stream()
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(Permission::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Permission permission : permissions) {
            if (permission.getId() == null || existingIds.contains(permission.getId())) {
                continue;
            }
            managed.add(RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build());
            existingIds.add(permission.getId());
        }
    }

    private static String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> roleSnapshot(Role role) {
        if (role == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", role.getId());
        snapshot.put("name", role.getName());
        snapshot.put("displayName", role.getDisplayName());
        snapshot.put("description", role.getDescription());
        snapshot.put("isSystem", role.getIsSystem());
        snapshot.put("isActive", role.getIsActive());
        snapshot.put("organisationId", role.getOrganisation() != null ? role.getOrganisation().getId() : null);
        List<Map<String, Object>> permissions = new java.util.ArrayList<>();
        if (role.getRolePermissions() != null) {
            for (RolePermission rp : role.getRolePermissions()) {
                if (rp.getPermission() == null) {
                    continue;
                }
                Map<String, Object> p = new java.util.LinkedHashMap<>();
                p.put("id", rp.getPermission().getId());
                p.put("name", rp.getPermission().getName());
                permissions.add(p);
            }
        }
        snapshot.put("permissions", permissions);
        snapshot.put("updatedAt", role.getUpdatedAt());
        return snapshot;
    }

    private RoleResponse toRoleResponse(Role role) {
        RoleResponse response = RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .isActive(role.getIsActive())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .userCount(role.getAuthIdentityRoles() != null ? role.getAuthIdentityRoles().size() : 0)
                .build();

        if (role.getRolePermissions() != null) {
            List<PermissionResponse> permissions = role.getRolePermissions().stream()
                    .map(RolePermission::getPermission)
                    .filter(Objects::nonNull)
                    .map(p -> PermissionResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .displayName(p.getDisplayName())
                            .description(p.getDescription())
                            .category(p.getCategory())
                            .isActive(p.getIsActive())
                            .createdAt(p.getCreatedAt())
                            .updatedAt(p.getUpdatedAt())
                            .build())
                    .toList();
            response.setPermissions(permissions);
        }
        return response;
    }

    private static boolean isHiddenFromRoleCatalog(Role role) {
        if (role == null || role.getName() == null) {
            return false;
        }
        return HIDDEN_FROM_ROLE_CATALOG.contains(role.getName().trim().toUpperCase(Locale.ROOT));
    }
}
