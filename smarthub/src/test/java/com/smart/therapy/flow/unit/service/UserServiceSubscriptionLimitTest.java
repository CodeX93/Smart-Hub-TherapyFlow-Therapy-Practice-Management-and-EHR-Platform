package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.user.dto.CreateUserRequest;
import com.smart.therapy.flow.user.dto.UpdateUserRequest;
import com.smart.therapy.flow.user.repository.UserIdempotencyKeyRepository;
import com.smart.therapy.flow.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService subscription limit tests")
class UserServiceSubscriptionLimitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserIdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private AuditService auditService;
    @Mock
    private AuthIdentityRoleRepository authIdentityRoleRepository;
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private AuthIdentityService authIdentityService;
    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private UserOrganisationRepository userOrganisationRepository;

    @InjectMocks
    private UserService userService;

    private AuthPrincipal adminPrincipal;
    private User admin;
    private Role therapistRole;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
        TenantContext.setSchemaName("tenant_1");

        admin = TestDataFactory.createTestAdmin();
        admin.setId(1L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);

        therapistRole = TestDataFactory.createTestRole(RoleName.THERAPIST);
        therapistRole.setId(10L);

        when(permissionChecker.hasRole(eq(adminPrincipal), eq("ADMIN"))).thenReturn(true);
        lenient().when(permissionChecker.hasRole(eq(adminPrincipal), eq("SUPER_ADMIN"))).thenReturn(false);
        lenient().when(auditService.serializeUserState(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("{}");
        lenient().when(authIdentityRepository.existsByNormalisedLoginIdentifier(anyString())).thenReturn(false);
        lenient().when(userRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().doNothing().when(authIdentityService).assertUsernameAvailable(any(), anyString(), any());
        lenient().doNothing().when(authIdentityService).assertEmailAvailable(any(), anyString(), any());
        lenient().when(authIdentityService.createStaffIdentity(anyString(), anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    AuthIdentity identity = new AuthIdentity();
                    identity.setId(500L);
                    identity.setLoginIdentifier(inv.getArgument(0));
                    return identity;
                });
        when(roleRepository.findByNameForOrganisation(RoleName.THERAPIST.name(), 1L)).thenReturn(Optional.of(therapistRole));
        when(subscriptionFeatureService.getEffectiveLimit(1L, "THERAPIST_LIMIT", null)).thenReturn(1);
        lenient().when(currentUserService.requireCurrentUser(adminPrincipal)).thenReturn(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Blocks therapist creation when active therapist limit is reached")
    void shouldBlockTherapistCreationAtLimit() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("new.therapist@example.com");
        request.setEmail("new.therapist@example.com");
        request.setFullName("New Therapist");
        request.setPassword("password123");
        request.setActive(true);
        request.setRoles(Set.of("THERAPIST"));

        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(1L, RoleName.THERAPIST.name()))
                .thenReturn(1L);

        assertThatThrownBy(() -> userService.createUser(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Therapist limit reached (1)");
    }

    @Test
    @DisplayName("Blocks reactivating a therapist when active therapist limit is reached")
    void shouldBlockTherapistReactivationAtLimit() {
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("therapist@example.com", "hash");
        identity.setId(20L);

        User therapist = TestDataFactory.createTestTherapist();
        therapist.setId(2L);
        therapist.setAuthIdentity(identity);
        therapist.setIsActive(false);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setActive(true);
        request.setRoles(Set.of("THERAPIST"));

        AuthIdentityRole existingTherapistRole = AuthIdentityRole.builder()
                .authIdentity(identity)
                .role(therapistRole)
                .organisation(Organisation.builder().id(1L).build())
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(therapist));
        when(organisationRepository.findById(1L)).thenReturn(Optional.of(Organisation.builder().id(1L).build()));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(20L, 1L))
                .thenReturn(List.of(existingTherapistRole));
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(1L, RoleName.THERAPIST.name()))
                .thenReturn(1L);

        assertThatThrownBy(() -> userService.updateUser(2L, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Therapist limit reached (1)");
    }
}
