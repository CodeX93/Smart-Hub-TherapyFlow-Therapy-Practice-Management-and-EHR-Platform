package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.*;
import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.RolePermission;
import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.audit.support.AuditEventDraft;
import com.smart.therapy.flow.auth.repository.RolePermissionRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionChecker permissionChecker;
    private final OrganisationRepository organisationRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Value("${security.rbac.tenant-custom-permissions-enabled:false}")
    private boolean tenantCustomPermissionsEnabled;

    private static final java.util.regex.Pattern AUTHORITY_NAME_PATTERN =
            java.util.regex.Pattern.compile("^[A-Z][A-Z0-9_]{2,99}$");

    private static final Set<String> RESERVED_ROLE_NAMES = Arrays.stream(RoleName.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> NON_TENANT_ASSIGNABLE_SYSTEM_PERMISSIONS = Set.of(
            "PLATFORM_READ",
            "PLATFORM_MANAGE"
    );

    /** Roles that must not appear in tenant role catalogs (tenant-admin UI / assignment lists). */
    private static final Set<String> HIDDEN_FROM_TENANT_ROLE_CATALOG = Set.of(
            "SUPER_ADMIN",
            "CLIENT",
            "AUDITOR",
            "DEVOPS",
            "SUPPORT",
            "SYSTEM_AI_ASSISTANT"
    );

    // ========== ROLE METHODS ==========

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return getRoles(null);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(String search) {
        List<Role> roles;
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            roles = roleRepository.findAllForOrganisation(orgId);
        } else {
            roles = roleRepository.findByOrganisationIsNull();
        }
        return roles.stream()
                .filter(role -> !isHiddenFromTenantRoleCatalog(role))
                .filter(role -> matchesRoleSearch(role, search))
                .sorted(Comparator.comparing(role -> Optional.ofNullable(role.getDisplayName()).orElse(""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<RoleResponse> getRolesPaged(String search, int page, int pageSize, String sortBy, String sortDirection) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 200);
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String resolvedSortBy = resolveSortBy(sortBy);

        PageRequest pageable = PageRequest.of(safePage - 1, safePageSize, Sort.by(direction, resolvedSortBy));
        Long orgId = TenantContext.getOrganisationId();

        Page<Role> result = roleRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (orgId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("organisation").get("id"), orgId),
                        cb.isNull(root.get("organisation"))
                ));
                predicates.add(cb.not(root.get("name").in(HIDDEN_FROM_TENANT_ROLE_CATALOG)));
            } else {
                predicates.add(cb.isNull(root.get("organisation")));
                predicates.add(cb.not(root.get("name").in(HIDDEN_FROM_TENANT_ROLE_CATALOG)));
            }

            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);

        List<RoleResponse> items = result.getContent().stream()
                .map(this::toRoleResponse)
                .toList();

        return PaginatedResponse.of(items, result.getTotalElements(), safePage, safePageSize);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        Role role = findRoleInScope(id);
        return toRoleResponseWithPermissions(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to create roles");
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Role name is required");
        }
        if (!StringUtils.hasText(request.getDisplayName())) {
            throw new BadRequestException("Role display name is required");
        }

        Long orgId = TenantContext.getOrganisationId();
        String normalizedRoleName = request.getName().trim().toUpperCase(Locale.ROOT);
        assertValidAuthorityLikeName(normalizedRoleName, "Role name");
        if (orgId != null) {
            assertTenantRoleNameAllowed(normalizedRoleName);
        }
        Role.RoleBuilder roleBuilder = Role.builder()
                .name(normalizedRoleName)
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (orgId != null) {
            Organisation org = organisationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
            if (roleRepository.findByNameIgnoreCaseAndOrganisation_Id(normalizedRoleName, orgId).isPresent()) {
                throw new BadRequestException("Role name already exists");
            }
            roleBuilder.organisation(org).isSystem(false);
        } else {
            if (roleRepository.findByNameAndOrganisationIsNullIgnoreCase(normalizedRoleName).isPresent()) {
                throw new BadRequestException("Role name already exists");
            }
            roleBuilder.organisation(null)
                    .isSystem(request.getIsSystem() != null ? request.getIsSystem() : true);
        }

        Role role = roleBuilder.build();

        Role saved = roleRepository.save(role);

        // Assign permissions if provided
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            updateRolePermissions(saved.getId(), request.getPermissions(), requester);
        }
        auditTenantRbacEvent(requester, "ROLE_CREATED", "Role", saved.getId(),
                "name=" + saved.getName() + ",scope=" + (saved.getOrganisation() == null ? "platform" : "tenant"));

        return toRoleResponseWithPermissions(saved);
    }

    @Transactional
    public RoleResponse updateRole(Long id, CreateRoleRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Role ID is required");
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to update roles");
        Role role = findRoleInScope(id);

        if (isTenantScope() && (Boolean.TRUE.equals(role.getIsSystem()) || role.getOrganisation() == null)) {
            throw new ForbiddenException("Cannot modify system roles");
        }

        // Update fields
        if (StringUtils.hasText(request.getName())) {
            String normalizedRoleName = request.getName().trim().toUpperCase(Locale.ROOT);
            assertValidAuthorityLikeName(normalizedRoleName, "Role name");
            if (isTenantScope()) {
                assertTenantRoleNameAllowed(normalizedRoleName);
                roleRepository.findByNameIgnoreCaseAndOrganisation_Id(normalizedRoleName, TenantContext.getOrganisationId())
                        .filter(existing -> !existing.getId().equals(role.getId()))
                        .ifPresent(existing -> { throw new BadRequestException("Role name already exists"); });
            } else {
                roleRepository.findByNameAndOrganisationIsNullIgnoreCase(normalizedRoleName)
                        .filter(existing -> !existing.getId().equals(role.getId()))
                        .ifPresent(existing -> { throw new BadRequestException("Role name already exists"); });
            }
            role.setName(normalizedRoleName);
        }
        if (StringUtils.hasText(request.getDisplayName())) {
            role.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            role.setIsActive(request.getIsActive());
        }

        Role updated = roleRepository.save(role);

        // Update permissions if provided
        if (request.getPermissions() != null) {
            updateRolePermissions(updated.getId(), request.getPermissions(), requester);
        }
        auditTenantRbacEvent(requester, "ROLE_UPDATED", "Role", updated.getId(), "name=" + updated.getName());

        return toRoleResponseWithPermissions(updated);
    }

    @Transactional
    public void deleteRole(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Role ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to delete roles");
        Role role = findRoleInScope(id);

        if (isTenantScope() && (Boolean.TRUE.equals(role.getIsSystem()) || role.getOrganisation() == null)) {
            throw new BadRequestException("Cannot delete system role");
        }

        // Check if role has identities (users/clients) assigned
        if (role.getAuthIdentityRoles() != null && !role.getAuthIdentityRoles().isEmpty()) {
            throw new BadRequestException("Cannot delete role with assigned users");
        }

        roleRepository.delete(role);
        auditTenantRbacEvent(requester, "ROLE_DELETED", "Role", id, "name=" + role.getName());
    }

    @Transactional
    public RoleResponse updateRolePermissions(Long roleId, List<Long> permissionIds, AuthPrincipal requester) {
        Objects.requireNonNull(roleId, "Role ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to update role permissions");
        Role role = findRoleInScope(roleId);
        Long orgId = TenantContext.getOrganisationId();

        if (orgId != null && role.getOrganisation() == null) {
            throw new ForbiddenException("Tenant administrators can only change tenant-owned role permissions");
        }
        if (orgId != null && Boolean.TRUE.equals(role.getIsSystem())) {
            throw new ForbiddenException("Cannot modify permissions of system roles");
        }
        if (orgId == null && role.getOrganisation() != null) {
            throw new ForbiddenException("Platform administrators can only change platform role permissions");
        }

        // Resolve permissions first, then mutate the managed collection in place.
        // Replacing the collection instance breaks Hibernate orphan-removal tracking.
        List<Permission> assignablePermissions = Collections.emptyList();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Long> dedupedPermissionIds = permissionIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            assignablePermissions = dedupedPermissionIds.stream()
                    .map(permissionId -> findAssignablePermission(permissionId, orgId))
                    .toList();
        }

        Set<RolePermission> managedRolePermissions = role.getRolePermissions();

        Set<Long> targetPermissionIds = assignablePermissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        // Remove mappings that are no longer requested.
        managedRolePermissions.removeIf(existing ->
                existing.getPermission() == null
                        || !targetPermissionIds.contains(existing.getPermission().getId()));

        // Add only missing mappings to avoid duplicate inserts in the same transaction.
        Set<Long> existingPermissionIds = managedRolePermissions.stream()
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(Permission::getId)
                .collect(Collectors.toSet());

        for (Permission permission : assignablePermissions) {
            if (permission.getId() == null || existingPermissionIds.contains(permission.getId())) {
                continue;
            }
            managedRolePermissions.add(RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build());
        }
        auditTenantRbacEvent(requester, "ROLE_PERMISSIONS_UPDATED", "Role", roleId,
                "permissionCount=" + (permissionIds == null ? 0 : permissionIds.size()));

        return toRoleResponseWithPermissions(role);
    }

    // ========== PERMISSION METHODS ==========

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        Long orgId = TenantContext.getOrganisationId();
        List<Permission> permissions;
        if (orgId != null) {
            permissions = new ArrayList<>();
            permissions.addAll(permissionRepository.findByOrganisationIsNull().stream()
                    .filter(this::isTenantAssignableSystemPermission)
                    .toList());
            permissions.addAll(permissionRepository.findByOrganisation_Id(orgId));
        } else {
            permissions = permissionRepository.findByOrganisationIsNull();
        }
        return permissions.stream()
                .filter(Permission::getIsActive)
                .sorted(Comparator.comparing(permission -> Optional.ofNullable(permission.getDisplayName()).orElse(""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantPermissionPolicyResponse getTenantPermissionPolicy() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new ForbiddenException("Organisation context is required");
        }
        List<String> assignableSystem = permissionRepository.findByOrganisationIsNull().stream()
                .filter(this::isTenantAssignableSystemPermission)
                .map(Permission::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> blockedSystem = permissionRepository.findByOrganisationIsNull().stream()
                .map(Permission::getName)
                .filter(NON_TENANT_ASSIGNABLE_SYSTEM_PERMISSIONS::contains)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return TenantPermissionPolicyResponse.builder()
                .tenantCustomPermissionsEnabled(tenantCustomPermissionsEnabled)
                .tenantAssignableSystemPermissions(assignableSystem)
                .blockedSystemPermissions(blockedSystem)
                .build();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getPermission(Long id) {
        Permission permission = findPermissionInScope(id);
        return toPermissionResponse(permission);
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to create permissions");
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Permission name is required");
        }
        if (!StringUtils.hasText(request.getDisplayName())) {
            throw new BadRequestException("Permission display name is required");
        }
        if (!StringUtils.hasText(request.getCategory())) {
            throw new BadRequestException("Permission category is required");
        }

        Long orgId = TenantContext.getOrganisationId();
        String normalizedPermissionName = request.getName().trim().toUpperCase(Locale.ROOT);
        assertValidAuthorityLikeName(normalizedPermissionName, "Permission name");
        if (orgId != null) {
            if (!tenantCustomPermissionsEnabled) {
                throw new ForbiddenException("Tenant custom permissions are disabled by security policy");
            }
            assertTenantPermissionNameAllowed(normalizedPermissionName);
            if (permissionRepository.findByNameAndOrganisationIsNullIgnoreCase(normalizedPermissionName).isPresent()) {
                throw new BadRequestException("Permission name conflicts with a system permission");
            }
        }
        boolean exists = orgId != null
                ? permissionRepository.findByNameIgnoreCaseAndOrganisation_Id(normalizedPermissionName, orgId).isPresent()
                : permissionRepository.findByNameAndOrganisationIsNullIgnoreCase(normalizedPermissionName).isPresent();
        if (exists) {
            throw new BadRequestException("Permission name already exists");
        }

        Permission.PermissionBuilder permissionBuilder = Permission.builder()
                .name(normalizedPermissionName)
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (orgId != null) {
            Organisation org = organisationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
            permissionBuilder.organisation(org);
        } else {
            permissionBuilder.organisation(null);
        }

        Permission permission = permissionBuilder.build();

        Permission saved = permissionRepository.save(permission);
        auditTenantRbacEvent(requester, "PERMISSION_CREATED", "Permission", saved.getId(),
                "name=" + saved.getName() + ",scope=" + (saved.getOrganisation() == null ? "platform" : "tenant"));
        return toPermissionResponse(saved);
    }

    @Transactional
    public PermissionResponse updatePermission(Long id, CreatePermissionRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Permission ID is required");
        Objects.requireNonNull(request, "Request payload is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to update permissions");

        Permission permission = findPermissionInScope(id);
        enforcePermissionWriteScope(permission);
        if (isTenantScope() && !tenantCustomPermissionsEnabled) {
            throw new ForbiddenException("Tenant custom permissions are disabled by security policy");
        }

        // Update fields
        if (StringUtils.hasText(request.getName())) {
            String normalizedPermissionName = request.getName().trim().toUpperCase(Locale.ROOT);
            assertValidAuthorityLikeName(normalizedPermissionName, "Permission name");
            if (isTenantScope()) {
                assertTenantPermissionNameAllowed(normalizedPermissionName);
                if (permissionRepository.findByNameAndOrganisationIsNullIgnoreCase(normalizedPermissionName).isPresent()) {
                    throw new BadRequestException("Permission name conflicts with a system permission");
                }
                permissionRepository.findByNameIgnoreCaseAndOrganisation_Id(normalizedPermissionName, TenantContext.getOrganisationId())
                        .filter(existing -> !existing.getId().equals(permission.getId()))
                        .ifPresent(existing -> { throw new BadRequestException("Permission name already exists"); });
            } else {
                permissionRepository.findByNameAndOrganisationIsNullIgnoreCase(normalizedPermissionName)
                        .filter(existing -> !existing.getId().equals(permission.getId()))
                        .ifPresent(existing -> { throw new BadRequestException("Permission name already exists"); });
            }
            permission.setName(normalizedPermissionName);
        }
        if (StringUtils.hasText(request.getDisplayName())) {
            permission.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getCategory())) {
            permission.setCategory(request.getCategory());
        }
        if (request.getIsActive() != null) {
            permission.setIsActive(request.getIsActive());
        }

        Permission updated = permissionRepository.save(permission);
        auditTenantRbacEvent(requester, "PERMISSION_UPDATED", "Permission", updated.getId(), "name=" + updated.getName());
        return toPermissionResponse(updated);
    }

    @Transactional
    public void deletePermission(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Permission ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertScopeManager(requester, "Insufficient permissions to delete permissions");
        Permission permission = findPermissionInScope(id);
        enforcePermissionWriteScope(permission);
        if (isTenantScope() && !tenantCustomPermissionsEnabled) {
            throw new ForbiddenException("Tenant custom permissions are disabled by security policy");
        }

        // Check if permission is assigned to any roles
        if (!permission.getRolePermissions().isEmpty()) {
            throw new BadRequestException("Cannot delete permission assigned to roles");
        }

        permissionRepository.delete(permission);
        auditTenantRbacEvent(requester, "PERMISSION_DELETED", "Permission", id, "name=" + permission.getName());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private RoleResponse toRoleResponse(Role role) {
        int permissionsAssignedCount = role.getRolePermissions() != null ? role.getRolePermissions().size() : 0;
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .isActive(role.getIsActive())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .permissionsAssignedCount(permissionsAssignedCount)
                .userCount(role.getAuthIdentityRoles() != null ? role.getAuthIdentityRoles().size() : 0)
                .build();
    }

    private RoleResponse toRoleResponseWithPermissions(Role role) {
        RoleResponse response = toRoleResponse(role);
        
        if (role.getRolePermissions() != null) {
            List<PermissionResponse> permissions = role.getRolePermissions().stream()
                    .map(RolePermission::getPermission)
                    .filter(Permission::getIsActive)
                    .map(this::toPermissionResponse)
                    .collect(Collectors.toList());
            response.setPermissions(permissions);
        }
        
        return response;
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .displayName(permission.getDisplayName())
                .description(permission.getDescription())
                .category(permission.getCategory())
                .isActive(permission.getIsActive())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }

    private boolean matchesRoleSearch(Role role, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String value = search.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(role.getName(), value)
                || containsIgnoreCase(role.getDisplayName(), value)
                || containsIgnoreCase(role.getDescription(), value);
    }

    private boolean containsIgnoreCase(String source, String lowerNeedle) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    private String resolveSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "displayName";
        }
        return switch (sortBy) {
            case "name", "displayName", "description", "createdAt", "updatedAt", "isActive" -> sortBy;
            default -> "displayName";
        };
    }

    private Role findRoleInScope(Long roleId) {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            return roleRepository.findByIdForOrganisation(roleId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        }
        return roleRepository.findByIdAndOrganisationIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private Permission findPermissionInScope(Long permissionId) {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            return permissionRepository.findByIdAndOrganisation_Id(permissionId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        }
        return permissionRepository.findByIdAndOrganisationIsNull(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
    }

    private Permission findAssignablePermission(Long permissionId, Long orgId) {
        if (orgId != null) {
            Optional<Permission> tenantPermission = permissionRepository.findByIdAndOrganisation_Id(permissionId, orgId);
            if (tenantPermission.isPresent()) {
                if (!tenantCustomPermissionsEnabled) {
                    throw new ForbiddenException("Tenant custom permissions are disabled by security policy");
                }
                return tenantPermission.get();
            }
            Permission platformPermission = permissionRepository.findByIdAndOrganisationIsNull(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
            if (NON_TENANT_ASSIGNABLE_SYSTEM_PERMISSIONS.contains(platformPermission.getName())) {
                throw new ForbiddenException("Permission is not assignable in tenant scope: " + platformPermission.getName());
            }
            return platformPermission;
        }
        return permissionRepository.findByIdAndOrganisationIsNull(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
    }

    private void enforcePermissionWriteScope(Permission permission) {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && permission.getOrganisation() == null) {
            throw new ForbiddenException("Tenant administrators cannot modify platform permissions");
        }
    }

    private boolean isTenantScope() {
        return TenantContext.getOrganisationId() != null;
    }

    private boolean isHiddenFromTenantRoleCatalog(Role role) {
        if (role == null || role.getName() == null) {
            return false;
        }
        return HIDDEN_FROM_TENANT_ROLE_CATALOG.contains(role.getName().trim().toUpperCase(Locale.ROOT));
    }

    private void assertValidAuthorityLikeName(String value, String label) {
        if (!AUTHORITY_NAME_PATTERN.matcher(value).matches()) {
            throw new BadRequestException(label + " must be uppercase snake case (e.g. CUSTOM_PERMISSION)");
        }
    }

    private void assertTenantRoleNameAllowed(String roleName) {
        if (RESERVED_ROLE_NAMES.contains(roleName) || roleName.startsWith("PLATFORM_") || roleName.startsWith("SYSTEM_")) {
            throw new BadRequestException("Role name is reserved by the system");
        }
    }

    private void assertTenantPermissionNameAllowed(String permissionName) {
        if (permissionName.startsWith("PLATFORM_") || permissionName.startsWith("SYSTEM_") || permissionName.startsWith("ROLE_")) {
            throw new BadRequestException("Permission name is reserved by the system");
        }
        if (extractPermissionNamesFromConstants().contains(permissionName)) {
            throw new BadRequestException("Permission name is reserved by the platform");
        }
    }

    private boolean isTenantAssignableSystemPermission(Permission permission) {
        if (permission == null || permission.getOrganisation() != null) {
            return false;
        }
        return !NON_TENANT_ASSIGNABLE_SYSTEM_PERMISSIONS.contains(permission.getName());
    }

    private Set<String> extractPermissionNamesFromConstants() {
        Set<String> names = new HashSet<>();
        java.lang.reflect.Field[] fields = PermissionConstants.class.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            if (field.getType() != String.class) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (!(value instanceof String expression)) {
                    continue;
                }
                int start = expression.indexOf("hasAuthority('");
                if (start < 0) {
                    continue;
                }
                int from = start + "hasAuthority('".length();
                int end = expression.indexOf("')", from);
                if (end > from) {
                    names.add(expression.substring(from, end));
                }
            } catch (IllegalAccessException ignored) {
                // ignore inaccessible static constants
            }
        }
        return names;
    }

    private void auditTenantRbacEvent(AuthPrincipal requester, String action, String resourceType, Long resourceId, String details) {
        if (requester == null || requester.getAuthId() == null || !isTenantScope()) {
            return;
        }
        try {
            Long actorId = userRepository.findByAuthId(requester.getAuthId())
                    .map(user -> user.getId())
                    .orElse(null);
            auditLogService.record(AuditEventDraft.of(action, resourceType)
                    .actorId(actorId)
                    .username(requester.getLoginIdentifier())
                    .resourceId(resourceId)
                    .details(details)
                    .riskLevel("medium")
                    .hipaaRelevant(false)
                    .result("success"));
        } catch (Exception e) {
            log.warn("Failed to write tenant RBAC audit event: action={}, resourceType={}, resourceId={}", action, resourceType, resourceId, e);
        }
    }

    private void assertScopeManager(AuthPrincipal principal, String message) {
        if (principal == null) {
            throw new ForbiddenException(message);
        }

        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            if (!permissionChecker.hasPermission(principal, "USER_MANAGE")) {
                throw new ForbiddenException(message);
            }
            return;
        }

        boolean isPlatformSuperAdmin = permissionChecker.hasRole(principal, "PLATFORM_SUPER_ADMIN");
        boolean hasPlatformManage = permissionChecker.hasPermission(principal, "PLATFORM_MANAGE");
        if (!isPlatformSuperAdmin && !hasPlatformManage) {
            throw new ForbiddenException(message);
        }
    }
}
