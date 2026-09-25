package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.dto.CreateRoleRequest;
import com.smart.therapy.flow.auth.dto.PermissionResponse;
import com.smart.therapy.flow.auth.dto.RoleResponse;
import com.smart.therapy.flow.auth.entity.Permission;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.RolePermission;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.auth.repository.RolePermissionRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.RolePermissionService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolePermissionService Unit Tests")
class RolePermissionServiceTest {
    private User therapist;
    private User admin;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RolePermissionService rolePermissionService;

    private AuthPrincipal adminPrincipal;
    private AuthPrincipal therapistPrincipal;
    private Role role;
    private Permission permission;
    private RolePermission rolePermission;

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        admin = TestDataFactory.createTestAdmin();
        therapist = TestDataFactory.createTestTherapist();
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "PLATFORM_MANAGE");
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        role = TestDataFactory.createTestRole(com.smart.therapy.flow.common.util.RoleName.THERAPIST);
        role.setId(1L);
        role.setIsSystem(false);

        permission = Permission.builder()
                .name("client:read")
                .displayName("Read Clients")
                .description("Permission to read client information")
                .build();

        permission.setId(1L);
        rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .build();

        role.setRolePermissions(new HashSet<>(List.of(rolePermission)));
    }

    @Test
    @DisplayName("Should get all roles successfully")
    void shouldGetAllRolesSuccessfully() {
        // Arrange
        when(roleRepository.findByOrganisationIsNull()).thenReturn(List.of(role));

        // Act
        List<RoleResponse> roles = rolePermissionService.getRoles();

        // Assert
        assertThat(roles).isNotNull();
        assertThat(roles).hasSize(1);
        verify(roleRepository).findByOrganisationIsNull();
    }

    @Test
    @DisplayName("Should get role by ID successfully")
    void shouldGetRoleByIdSuccessfully() {
        // Arrange
        Long roleId = 1L;
        when(roleRepository.findByIdAndOrganisationIsNull(roleId)).thenReturn(Optional.of(role));

        // Act
        RoleResponse response = rolePermissionService.getRole(roleId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(roleId);
        verify(roleRepository).findByIdAndOrganisationIsNull(roleId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when role not found")
    void shouldThrowExceptionWhenRoleNotFound() {
        // Arrange
        Long roleId = 999L;
        when(roleRepository.findByIdAndOrganisationIsNull(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.getRole(roleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");

        verify(roleRepository).findByIdAndOrganisationIsNull(roleId);
    }

    @Test
    @DisplayName("Should create role successfully when admin")
    void shouldCreateRoleSuccessfullyWhenAdmin() {
        // Arrange
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NEW_ROLE");
        request.setDisplayName("New Role");
        request.setDescription("A new role");
        request.setIsActive(true);

        Role newRole = Role.builder()
                .name("NEW_ROLE")
                .displayName("New Role")
                .isActive(true)
                .isSystem(false)
                .rolePermissions(new HashSet<>())
                .build();

        when(roleRepository.findByNameAndOrganisationIsNullIgnoreCase("NEW_ROLE")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(newRole);

        // Act
        RoleResponse response = rolePermissionService.createRole(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("NEW_ROLE");
        verify(roleRepository).findByNameAndOrganisationIsNullIgnoreCase("NEW_ROLE");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when role name already exists")
    void shouldThrowExceptionWhenRoleNameExists() {
        // Arrange
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("THERAPIST");
        request.setDisplayName("Therapist");

        when(roleRepository.findByNameAndOrganisationIsNullIgnoreCase("THERAPIST")).thenReturn(Optional.of(role));

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.createRole(request, adminPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role name already exists");

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-admin creates role")
    void shouldThrowExceptionWhenNonAdminCreatesRole() {
        // Arrange
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NEW_ROLE");

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.createRole(request, therapistPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Insufficient permissions to create roles");

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should update role successfully")
    void shouldUpdateRoleSuccessfully() {
        // Arrange
        Long roleId = 1L;
        CreateRoleRequest request = new CreateRoleRequest();
        request.setDisplayName("Updated Role Name");
        request.setIsActive(false);

        when(roleRepository.findByIdAndOrganisationIsNull(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        // Act
        RoleResponse response = rolePermissionService.updateRole(roleId, request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(roleRepository).findByIdAndOrganisationIsNull(roleId);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when updating system role")
    void shouldThrowExceptionWhenUpdatingSystemRole() {
        // Arrange
        Long roleId = 1L;
        CreateRoleRequest request = new CreateRoleRequest();
        TenantContext.setOrganisationId(42L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "USER_MANAGE");
        role.setIsSystem(true);

        when(roleRepository.findByIdForOrganisation(roleId, 42L)).thenReturn(Optional.of(role));

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.updateRole(roleId, request, adminPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot modify system roles");

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should delete role successfully")
    void shouldDeleteRoleSuccessfully() {
        // Arrange
        Long roleId = 1L;
        when(roleRepository.findByIdAndOrganisationIsNull(roleId)).thenReturn(Optional.of(role));
        doNothing().when(roleRepository).delete(role);

        // Act
        rolePermissionService.deleteRole(roleId, adminPrincipal);

        // Assert
        verify(roleRepository).findByIdAndOrganisationIsNull(roleId);
        verify(roleRepository).delete(role);
    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting system role")
    void shouldThrowExceptionWhenDeletingSystemRole() {
        // Arrange
        Long roleId = 1L;
        TenantContext.setOrganisationId(42L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "USER_MANAGE");
        role.setIsSystem(true);
        when(roleRepository.findByIdForOrganisation(roleId, 42L)).thenReturn(Optional.of(role));

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.deleteRole(roleId, adminPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete system role");

        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting role with assigned users")
    void shouldThrowExceptionWhenDeletingRoleWithUsers() {
        // Arrange
        Long roleId = 1L;
        com.smart.therapy.flow.auth.entity.AuthIdentityRole air = com.smart.therapy.flow.auth.entity.AuthIdentityRole.builder()
                .role(role)
                .authIdentity(TestDataFactory.createTestAuthIdentity("u@example.com", "hash"))
                .organisation(TestDataFactory.createTestOrganisation())
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        role.setAuthIdentityRoles(new HashSet<>(List.of(air)));
        when(roleRepository.findByIdAndOrganisationIsNull(roleId)).thenReturn(Optional.of(role));

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.deleteRole(roleId, adminPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete role with assigned users");

        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    @DisplayName("Should update role permissions successfully")
    void shouldUpdateRolePermissionsSuccessfully() {
        // Arrange
        Long roleId = 1L;
        List<Long> permissionIds = List.of(1L);

        when(roleRepository.findByIdAndOrganisationIsNull(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByIdAndOrganisationIsNull(1L)).thenReturn(Optional.of(permission));
        role.getRolePermissions().clear();

        // Act
        RoleResponse response = rolePermissionService.updateRolePermissions(roleId, permissionIds, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(roleRepository).findByIdAndOrganisationIsNull(roleId);
        verify(permissionRepository).findByIdAndOrganisationIsNull(1L);
        assertThat(role.getRolePermissions()).extracting(RolePermission::getPermission).containsExactly(permission);
    }

    @Test
    @DisplayName("Should get all permissions successfully")
    void shouldGetAllPermissionsSuccessfully() {
        // Arrange
        when(permissionRepository.findByOrganisationIsNull()).thenReturn(List.of(permission));

        // Act
        List<PermissionResponse> permissions = rolePermissionService.getPermissions();

        // Assert
        assertThat(permissions).isNotNull();
        assertThat(permissions).hasSize(1);
        verify(permissionRepository).findByOrganisationIsNull();
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NEW_ROLE");

        // Act & Assert
        assertThatThrownBy(() -> rolePermissionService.createRole(request, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }
}
