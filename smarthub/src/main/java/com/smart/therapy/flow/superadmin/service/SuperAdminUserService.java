package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.RolePermission;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.RolePermissionRepository;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisationAccessBlock;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationAccessBlockRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.RolesPermissionsMatrixUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminUserService {

    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserOrganisationRepository userOrganisationRepository;
    private final OrganisationRepository organisationRepository;
    private final UserOrganisationAccessBlockRepository userOrganisationAccessBlockRepository;
    private final AuthSessionService authSessionService;
    private final PlatformAuditService platformAuditService;

    @Transactional(readOnly = true)
    public Page<AuthIdentity> listUsers(
            String search,
            String role,
            String identityType,
            String status,
            Long organisationId,
            Boolean active,
            Instant lastLoginFrom,
            Instant lastLoginTo,
            int page,
            int size
    ) {
        if (lastLoginFrom != null && lastLoginTo != null && lastLoginFrom.isAfter(lastLoginTo)) {
            throw new IllegalArgumentException("lastLoginFrom must be before or equal to lastLoginTo");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        Specification<AuthIdentity> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("loginIdentifier")), pattern));
        }
        if (role != null && !role.isBlank()) {
            String roleName = role.trim().toUpperCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> {
                var sub = query.subquery(Long.class);
                var roles = sub.from(AuthIdentityRole.class);
                var roleJoin = roles.join("role");
                sub.select(roles.get("authIdentity").get("id"))
                        .where(cb.equal(cb.upper(roleJoin.get("name")), roleName));
                return cb.in(root.get("id")).value(sub);
            });
        }
        if (identityType != null && !identityType.isBlank()) {
            IdentityType type = IdentityType.valueOf(identityType.trim().toUpperCase(Locale.ROOT));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("identityType"), type));
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> {
                return switch (normalized) {
                    case "active" -> cb.and(
                            cb.isTrue(root.get("isActive")),
                            cb.isFalse(root.get("accountLocked"))
                    );
                    case "disabled", "inactive" -> cb.isFalse(root.get("isActive"));
                    case "locked" -> cb.isTrue(root.get("accountLocked"));
                    default -> throw new IllegalArgumentException("Invalid status filter: " + status);
                };
            });
        }
        if (organisationId != null) {
            spec = spec.and((root, query, cb) -> {
                var directOrg = cb.equal(root.join("organisation").get("id"), organisationId);
                var sub = query.subquery(Long.class);
                var uo = sub.from(com.smart.therapy.flow.organisation.entity.UserOrganisation.class);
                sub.select(uo.get("auth").get("id"))
                        .where(cb.equal(uo.get("organisation").get("id"), organisationId));
                var viaMembership = cb.in(root.get("id")).value(sub);
                return cb.or(directOrg, viaMembership);
            });
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
        }
        if (lastLoginFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("lastSuccessfulLogin"), lastLoginFrom));
        }
        if (lastLoginTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("lastSuccessfulLogin"), lastLoginTo));
        }

        return authIdentityRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildUserDetails(AuthIdentity identity) {
        List<AuthIdentityRole> roles = authIdentityRoleRepository.findByAuthIdWithRolesAndPermissions(identity.getId());
        List<String> roleNames = roles.stream()
                .map(r -> r.getRole().getName())
                .distinct()
                .collect(Collectors.toList());
        List<String> platformRoles = roles.stream()
                .filter(r -> r.getOrganisation() == null)
                .map(r -> r.getRole().getName())
                .distinct()
                .collect(Collectors.toList());
        List<Long> orgIds = userOrganisationRepository.findByAuth_Id(identity.getId()).stream()
                .map(uo -> uo.getOrganisation().getId())
                .distinct()
                .collect(Collectors.toList());
        List<Long> roleOrgIds = roles.stream()
                .map(r -> r.getOrganisation() != null ? r.getOrganisation().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        java.util.LinkedHashSet<Long> orgIdSet = new java.util.LinkedHashSet<>(orgIds);
        orgIdSet.addAll(roleOrgIds);
        if (identity.getOrganisation() != null && identity.getOrganisation().getId() != null) {
            orgIdSet.add(identity.getOrganisation().getId());
        }
        orgIds = new java.util.ArrayList<>(orgIdSet);
        List<Map<String, Object>> orgSummaries = organisationRepository.findAllById(orgIds).stream()
                .map(org -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("organisationId", org.getId());
                    row.put("name", org.getName());
                    row.put("slug", org.getSlug());
                    row.put("status", org.getStatus());
                    row.put("subdomain", org.getSubdomain());
                    return row;
                })
                .toList();
        java.util.LinkedHashSet<String> allRoleSet = new java.util.LinkedHashSet<>();
        allRoleSet.addAll(platformRoles);
        allRoleSet.addAll(roleNames);
        List<String> allRoles = new java.util.ArrayList<>(allRoleSet);
        String resolvedName = resolveDisplayName(identity);
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("authId", identity.getId());
        body.put("name", resolvedName);
        body.put("fullName", resolvedName);
        body.put("loginIdentifier", identity.getLoginIdentifier());
        body.put("email", identity.getLoginIdentifier());
        body.put("phone", identity.getPhone());
        body.put("identityType", identity.getIdentityType().name());
        body.put("active", identity.getIsActive());
        body.put("isActive", identity.getIsActive());
        body.put("accountLocked", identity.getAccountLocked());
        body.put("lastSuccessfulLogin", identity.getLastSuccessfulLogin());
        body.put("roles", allRoles);
        body.put("platformRoles", platformRoles);
        body.put("tenantRoles", roleNames);
        body.put("organisationIds", orgIds);
        body.put("organisations", orgSummaries);
        return body;
    }

    @Transactional(readOnly = true)
    public long countUsersByOrganisation(Long organisationId) {
        if (organisationId == null) {
            return 0;
        }
        return listUsers(
                null,
                null,
                null,
                null,
                organisationId,
                null,
                null,
                null,
                0,
                1
        ).getTotalElements();
    }

    @Transactional
    public AuthIdentity disableUser(Long authId, Long actorAuthId, String reason) {
        AuthIdentity identity = authIdentityRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("User identity not found"));
        List<AuthIdentityRole> platformRoles = authIdentityRoleRepository.findByAuthIdWithRolesAndPermissionsForPlatform(authId);
        boolean isPlatformSuperAdmin = platformRoles.stream()
                .anyMatch(r -> r.getRole() != null
                        && r.getRole().getName() != null
                        && "SUPER_ADMIN".equalsIgnoreCase(r.getRole().getName()));
        if (isPlatformSuperAdmin && authIdentityRoleRepository.countActivePlatformSuperAdmins() <= 1) {
            throw new IllegalArgumentException("Cannot disable the last platform super admin");
        }
        identity.setIsActive(false);
        identity.setAccountLocked(true);
        identity.setLockedReason(reason != null && !reason.isBlank() ? reason : "Disabled by platform super admin");
        identity.setLockedUntil(null);
        identity.setUpdatedAt(Instant.now());
        identity = authIdentityRepository.save(identity);
        authSessionService.revokeAllForAuthId(authId);
        platformAuditService.log(actorAuthId, "USER_DISABLED", "AuthIdentity", String.valueOf(authId), identity.getLockedReason());
        return identity;
    }

    @Transactional
    public AuthIdentity enableUser(Long authId, Long actorAuthId) {
        AuthIdentity identity = authIdentityRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("User identity not found"));
        identity.setIsActive(true);
        identity.setAccountLocked(false);
        identity.setLockedReason(null);
        identity.setLockedUntil(null);
        identity.setUpdatedAt(Instant.now());
        identity = authIdentityRepository.save(identity);
        platformAuditService.log(actorAuthId, "USER_ENABLED", "AuthIdentity", String.valueOf(authId), "");
        return identity;
    }

    @Transactional(readOnly = true)
    public AuthIdentity getIdentityOrThrow(Long authId) {
        return authIdentityRepository.findById(authId)
                .orElseThrow(() -> new IllegalArgumentException("User identity not found"));
    }

    @Transactional
    public UserOrganisation linkUserToOrganisation(Long authId, Long organisationId, Long actorAuthId) {
        AuthIdentity identity = getIdentityOrThrow(authId);
        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found"));
        return userOrganisationRepository.findByAuth_IdAndOrganisation_Id(authId, organisationId)
                .orElseGet(() -> {
                    UserOrganisation link = UserOrganisation.builder()
                            .auth(identity)
                            .organisation(organisation)
                            .createdAt(Instant.now())
                            .build();
                    UserOrganisation saved = userOrganisationRepository.save(link);
                    platformAuditService.log(
                            actorAuthId,
                            "USER_ORG_LINKED",
                            "UserOrganisation",
                            String.valueOf(saved.getId()),
                            "authId=" + authId + ", organisationId=" + organisationId
                    );
                    return saved;
                });
    }

    @Transactional
    public void unlinkUserFromOrganisation(Long authId, Long organisationId, Long actorAuthId) {
        UserOrganisation link = userOrganisationRepository.findByAuth_IdAndOrganisation_Id(authId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("User organisation link not found"));
        userOrganisationRepository.delete(link);
        platformAuditService.log(
                actorAuthId,
                "USER_ORG_UNLINKED",
                "UserOrganisation",
                String.valueOf(link.getId()),
                "authId=" + authId + ", organisationId=" + organisationId
        );
    }

    @Transactional
    public AuthIdentity disableUserForOrganisation(Long authId, Long organisationId, Long actorAuthId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        AuthIdentity identity = getIdentityOrThrow(authId);
        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found"));

        UserOrganisationAccessBlock block = userOrganisationAccessBlockRepository
                .findByAuth_IdAndOrganisation_Id(authId, organisationId)
                .orElseGet(() -> UserOrganisationAccessBlock.builder()
                        .auth(identity)
                        .organisation(organisation)
                        .build());
        block.setReason(reason.trim());
        block.setBlockedBy(actorAuthId);
        block.setBlockedAt(Instant.now());
        userOrganisationAccessBlockRepository.save(block);

        platformAuditService.log(
                actorAuthId,
                "USER_ORG_BLOCKED",
                "UserOrganisationAccessBlock",
                String.valueOf(block.getId()),
                "authId=" + authId + ", organisationId=" + organisationId + ", reason=" + reason.trim()
        );
        return identity;
    }

    @Transactional
    public AuthIdentity enableUserForOrganisation(Long authId, Long organisationId, Long actorAuthId) {
        AuthIdentity identity = getIdentityOrThrow(authId);
        UserOrganisationAccessBlock block = userOrganisationAccessBlockRepository
                .findByAuth_IdAndOrganisation_Id(authId, organisationId)
                .orElseThrow(() -> new IllegalArgumentException("User is not blocked for this organisation"));
        userOrganisationAccessBlockRepository.delete(block);
        platformAuditService.log(
                actorAuthId,
                "USER_ORG_UNBLOCKED",
                "UserOrganisationAccessBlock",
                String.valueOf(block.getId()),
                "authId=" + authId + ", organisationId=" + organisationId
        );
        return identity;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserRolesPermissions(Long authId, String module, String permissionGroup) {
        AuthIdentity identity = getIdentityOrThrow(authId);
        String moduleFilter = normalize(module);
        String groupFilter = normalize(permissionGroup);

        List<AuthIdentityRole> roles = authIdentityRoleRepository.findByAuthIdWithRolesAndPermissions(authId);
        List<Map<String, Object>> roleItems = roles.stream()
                .filter(roleLink -> roleLink.getRole() != null)
                .map(roleLink -> {
                    var role = roleLink.getRole();
                    var permissions = role.getRolePermissions() == null ? List.of() : role.getRolePermissions().stream()
                            .map(rp -> rp.getPermission())
                            .filter(Objects::nonNull)
                            .filter(p -> matchesPermissionFilters(p.getName(), p.getCategory(), moduleFilter, groupFilter))
                            .map(this::toPermissionMap)
                            .toList();

                    return toRoleMap(role, roleLink.getOrganisation() != null ? roleLink.getOrganisation().getId() : null, permissions);
                })
                .toList();

        java.util.LinkedHashMap<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("authId", identity.getId());
        response.put("loginIdentifier", identity.getLoginIdentifier());
        response.put("roles", roleItems);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRolesPermissionsMatrix(String module, String permissionGroup) {
        String moduleFilter = normalize(module);
        String groupFilter = normalize(permissionGroup);

        Map<String, List<Map<String, Object>>> permissionsByCategory = permissionRepository.findAll().stream()
                .filter(p -> matchesPermissionFilters(p.getName(), p.getCategory(), moduleFilter, groupFilter))
                .sorted((a, b) -> {
                    int cmp = compareNullable(normalize(a.getCategory()), normalize(b.getCategory()));
                    if (cmp != 0) {
                        return cmp;
                    }
                    return compareNullable(normalize(a.getName()), normalize(b.getName()));
                })
                .collect(Collectors.groupingBy(
                        p -> normalize(p.getCategory()) == null ? "uncategorized" : normalize(p.getCategory()),
                        java.util.LinkedHashMap::new,
                        Collectors.mapping(this::toPermissionMap, Collectors.toList())
                ));

        List<Permission> permissionRows = permissionRepository.findAll().stream()
                .filter(p -> matchesPermissionFilters(p.getName(), p.getCategory(), moduleFilter, groupFilter))
                .sorted((a, b) -> {
                    int cmp = compareNullable(normalize(a.getCategory()), normalize(b.getCategory()));
                    if (cmp != 0) {
                        return cmp;
                    }
                    return compareNullable(normalize(a.getName()), normalize(b.getName()));
                })
                .toList();
        List<Map<String, Object>> permissions = permissionRows.stream()
                .map(this::toPermissionMap)
                .toList();

        List<Map<String, Object>> roles = roleRepository.findAllWithPermissions().stream()
                .filter(role -> role.getName() == null
                        || !Set.of("CLIENT", "AUDITOR", "DEVOPS", "SUPPORT", "SYSTEM_AI_ASSISTANT")
                                .contains(role.getName().trim().toUpperCase(Locale.ROOT)))
                .map(role -> {
                    List<String> permissionNames = role.getRolePermissions() == null ? List.of()
                            : role.getRolePermissions().stream()
                                    .map(rp -> rp.getPermission())
                                    .filter(Objects::nonNull)
                                    .filter(p -> matchesPermissionFilters(p.getName(), p.getCategory(), moduleFilter, groupFilter))
                                    .map(Permission::getName)
                                    .distinct()
                                    .sorted(String::compareToIgnoreCase)
                                    .toList();
                    return toRoleMap(role, role.getOrganisation() != null ? role.getOrganisation().getId() : null, permissionNames);
                })
                .toList();

        java.util.LinkedHashMap<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("module", moduleFilter);
        response.put("permissionGroup", groupFilter);
        response.put("permissions", permissions);
        response.put("permissionsByCategory", permissionsByCategory);
        response.put("roles", roles);
        response.put("generatedAt", Instant.now());
        return response;
    }

    @Transactional(readOnly = true)
    public String exportRolesPermissionsMatrixCsv(String module, String permissionGroup) {
        String moduleFilter = normalize(module);
        String groupFilter = normalize(permissionGroup);

        List<Permission> permissionRows = permissionRepository.findAll().stream()
                .filter(p -> matchesPermissionFilters(p.getName(), p.getCategory(), moduleFilter, groupFilter))
                .sorted((a, b) -> {
                    int cmp = compareNullable(normalize(a.getCategory()), normalize(b.getCategory()));
                    if (cmp != 0) {
                        return cmp;
                    }
                    return compareNullable(normalize(a.getName()), normalize(b.getName()));
                })
                .toList();

        List<Role> roles = roleRepository.findAllWithPermissions().stream()
                .filter(role -> role.getName() == null
                        || !Set.of("CLIENT", "AUDITOR", "DEVOPS", "SUPPORT", "SYSTEM_AI_ASSISTANT")
                                .contains(role.getName().trim().toUpperCase(Locale.ROOT)))
                .sorted((a, b) -> compareNullable(normalize(a.getName()), normalize(b.getName())))
                .toList();

        StringBuilder csv = new StringBuilder("role,roleDisplayName,organisationId,isSystem,permission,permissionDisplayName,category,granted\n");
        for (Role role : roles) {
            java.util.Set<String> rolePerms = role.getRolePermissions() == null ? java.util.Set.of()
                    : role.getRolePermissions().stream()
                            .map(rp -> rp.getPermission())
                            .filter(Objects::nonNull)
                            .map(Permission::getName)
                            .collect(java.util.stream.Collectors.toSet());
            for (Permission permission : permissionRows) {
                boolean granted = rolePerms.contains(permission.getName());
                csv.append(safeCsv(role.getName())).append(',')
                        .append(safeCsv(role.getDisplayName())).append(',')
                        .append(role.getOrganisation() != null ? role.getOrganisation().getId() : "")
                        .append(',')
                        .append(role.getIsSystem() != null && role.getIsSystem() ? "true" : "false")
                        .append(',')
                        .append(safeCsv(permission.getName())).append(',')
                        .append(safeCsv(permission.getDisplayName())).append(',')
                        .append(safeCsv(permission.getCategory())).append(',')
                        .append(granted)
                        .append('\n');
            }
        }
        return csv.toString();
    }

    @Transactional
    public Map<String, Object> updateRolesPermissionsMatrix(List<RolesPermissionsMatrixUpdateRequest.UpdateItem> updates,
                                                            Long actorAuthId) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("updates are required");
        }

        List<Role> globalRoles = roleRepository.findAll().stream()
                .filter(r -> r.getOrganisation() == null)
                .toList();
        Map<String, Role> roleByName = globalRoles.stream()
                .filter(r -> r.getName() != null)
                .collect(Collectors.toMap(
                        r -> r.getName().trim().toLowerCase(Locale.ROOT),
                        r -> r,
                        (a, b) -> a
                ));

        Map<String, Permission> permissionByName = permissionRepository.findAll().stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(
                        p -> p.getName().trim().toLowerCase(Locale.ROOT),
                        p -> p,
                        (a, b) -> a
                ));

        int grantedCount = 0;
        int revokedCount = 0;
        int unchangedCount = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();

        for (RolesPermissionsMatrixUpdateRequest.UpdateItem item : updates) {
            String roleKey = item.getRoleName().trim().toLowerCase(Locale.ROOT);
            String permissionKey = item.getPermissionName().trim().toLowerCase(Locale.ROOT);

            Role role = roleByName.get(roleKey);
            if (role == null) {
                throw new IllegalArgumentException("Unknown global role: " + item.getRoleName());
            }
            Permission permission = permissionByName.get(permissionKey);
            if (permission == null) {
                throw new IllegalArgumentException("Unknown permission: " + item.getPermissionName());
            }
            if (permission.getIsActive() == null || !permission.getIsActive()) {
                throw new IllegalArgumentException("Permission is inactive: " + permission.getName());
            }

            var existing = rolePermissionRepository.findByRole_IdAndPermission_Id(role.getId(), permission.getId());
            boolean changed = false;
            String action;
            if (Boolean.TRUE.equals(item.getGranted())) {
                if (existing.isPresent()) {
                    unchangedCount++;
                    action = "UNCHANGED_GRANTED";
                } else {
                    rolePermissionRepository.save(RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .build());
                    grantedCount++;
                    changed = true;
                    action = "GRANTED";
                }
            } else {
                if (existing.isPresent()) {
                    rolePermissionRepository.delete(existing.get());
                    revokedCount++;
                    changed = true;
                    action = "REVOKED";
                } else {
                    unchangedCount++;
                    action = "UNCHANGED_REVOKED";
                }
            }

            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("roleName", role.getName());
            row.put("permissionName", permission.getName());
            row.put("granted", Boolean.TRUE.equals(item.getGranted()));
            row.put("action", action);
            row.put("changed", changed);
            results.add(row);
        }

        platformAuditService.log(
                actorAuthId,
                "ROLE_PERMISSION_MATRIX_UPDATED",
                "RolePermission",
                "BATCH",
                "updates=" + updates.size() + ", granted=" + grantedCount + ", revoked=" + revokedCount + ", unchanged=" + unchangedCount
        );

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("updated", updates.size());
        response.put("granted", grantedCount);
        response.put("revoked", revokedCount);
        response.put("unchanged", unchangedCount);
        response.put("results", results);
        return response;
    }

    @Transactional
    public Map<String, Object> updateRolePermissionCell(String roleName,
                                                        String permissionName,
                                                        Boolean granted,
                                                        Long actorAuthId) {
        RolesPermissionsMatrixUpdateRequest.UpdateItem item = new RolesPermissionsMatrixUpdateRequest.UpdateItem();
        item.setRoleName(roleName);
        item.setPermissionName(permissionName);
        item.setGranted(granted);
        return updateRolesPermissionsMatrix(List.of(item), actorAuthId);
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

    private static boolean matchesPermissionFilters(String name, String category, String module, String group) {
        if (module == null && group == null) {
            return true;
        }
        String normalizedName = normalize(name);
        String normalizedCategory = normalize(category);
        boolean moduleMatch = true;
        if (module != null) {
            moduleMatch = (normalizedCategory != null && normalizedCategory.equals(module))
                    || (normalizedName != null && normalizedName.startsWith(module + "_"));
        }
        boolean groupMatch = true;
        if (group != null) {
            groupMatch = normalizedCategory != null && normalizedCategory.equals(group);
        }
        return moduleMatch && groupMatch;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static int compareNullable(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareToIgnoreCase(right);
    }

    private static String resolveDisplayName(AuthIdentity identity) {
        if (identity == null) {
            return "Unknown";
        }
        if (identity.getFullName() != null && !identity.getFullName().isBlank()) {
            return identity.getFullName().trim();
        }
        if (identity.getLoginIdentifier() == null || identity.getLoginIdentifier().isBlank()) {
            return "Unknown";
        }
        String loginIdentifier = identity.getLoginIdentifier().trim();
        int atIndex = loginIdentifier.indexOf('@');
        if (atIndex > 0) {
            String localPart = loginIdentifier.substring(0, atIndex).replace('.', ' ').replace('_', ' ').trim();
            if (!localPart.isBlank()) {
                return localPart;
            }
        }
        return loginIdentifier;
    }

    private Map<String, Object> toPermissionMap(Permission permission) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("permissionId", permission.getId());
        map.put("name", permission.getName());
        map.put("displayName", permission.getDisplayName());
        map.put("category", permission.getCategory());
        map.put("active", permission.getIsActive());
        return map;
    }

    private Map<String, Object> toRoleMap(Role role, Long organisationId, Object permissions) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("roleId", role.getId());
        map.put("name", role.getName());
        map.put("displayName", role.getDisplayName());
        map.put("organisationId", organisationId);
        map.put("isSystem", role.getIsSystem());
        map.put("active", role.getIsActive());
        map.put("permissions", permissions);
        return map;
    }
}
