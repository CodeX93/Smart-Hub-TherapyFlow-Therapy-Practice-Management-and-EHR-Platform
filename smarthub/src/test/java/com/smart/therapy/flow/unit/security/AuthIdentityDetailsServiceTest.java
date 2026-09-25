package com.smart.therapy.flow.unit.security;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthIdentityDetailsService tests")
class AuthIdentityDetailsServiceTest {

    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private AuthIdentityRoleRepository authIdentityRoleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserOrganisationRepository userOrganisationRepository;

    @InjectMocks
    private AuthIdentityDetailsService authIdentityDetailsService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Org-scoped login resolves STAFF by username")
    void loadsOrgScopedStaffByUsername() {
        TenantContext.setSchemaName("tenant_10");
        TenantContext.setOrganisationId(10L);

        AuthIdentity identity = AuthIdentity.builder()
                .id(5L)
                .username("jdoe")
                .normalisedUsername("jdoe")
                .email("jdoe@acme.com")
                .normalisedEmail("jdoe@acme.com")
                .loginIdentifier("jdoe")
                .normalisedLoginIdentifier("jdoe")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "jdoe", IdentityType.STAFF, 10L))
                .thenReturn(List.of(identity));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(5L, 10L))
                .thenReturn(List.of());

        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService.loadUserByUsername("jdoe");

        assertThat(principal.getAuthId()).isEqualTo(5L);
        assertThat(principal.getLoginIdentifier()).isEqualTo("jdoe");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Without tenant context, ambiguous email across orgs does not pick an identity")
    void ambiguousEmailWithoutTenantContextFails() {
        TenantContext.setSchemaName("public");

        AuthIdentity a = AuthIdentity.builder()
                .id(1L)
                .email("shared@example.com")
                .normalisedEmail("shared@example.com")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .organisation(com.smart.therapy.flow.organisation.entity.Organisation.builder().id(1L).build())
                .build();
        AuthIdentity b = AuthIdentity.builder()
                .id(2L)
                .email("shared@example.com")
                .normalisedEmail("shared@example.com")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .organisation(com.smart.therapy.flow.organisation.entity.Organisation.builder().id(2L).build())
                .build();

        when(authIdentityRepository.findAllByEmailOrUsername("shared@example.com"))
                .thenReturn(List.of(a, b));

        assertThatThrownBy(() -> authIdentityDetailsService.loadUserByUsername("shared@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Org-scoped login uses organisation-specific STAFF identity")
    void loadsOrgScopedStaffIdentity() {
        TenantContext.setSchemaName("tenant_10");
        TenantContext.setOrganisationId(10L);

        AuthIdentity identity = AuthIdentity.builder()
                .id(5L)
                .loginIdentifier("multi@example.com")
                .normalisedLoginIdentifier("multi@example.com")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "multi@example.com", IdentityType.STAFF, 10L))
                .thenReturn(List.of(identity));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(5L, 10L))
                .thenReturn(List.of());

        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService.loadUserByUsername("multi@example.com");

        assertThat(principal.getAuthId()).isEqualTo(5L);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Picks correct org-scoped identity when multiple identities share email")
    void picksCorrectIdentityForSelectedOrg() {
        TenantContext.setSchemaName("tenant_20");
        TenantContext.setOrganisationId(20L);

        AuthIdentity orgBIdentity = AuthIdentity.builder()
                .id(200L)
                .loginIdentifier("shared@example.com")
                .normalisedLoginIdentifier("shared@example.com")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "shared@example.com", IdentityType.STAFF, 20L))
                .thenReturn(List.of(orgBIdentity));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(200L, 20L))
                .thenReturn(List.of());

        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService.loadUserByUsername("shared@example.com");

        assertThat(principal.getAuthId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Fails when multiple active STAFF identities exist in the same organisation")
    void failsWhenAmbiguousInSameOrg() {
        TenantContext.setSchemaName("tenant_21");
        TenantContext.setOrganisationId(21L);

        AuthIdentity a = AuthIdentity.builder().id(1L).isActive(true).identityType(IdentityType.STAFF).build();
        AuthIdentity b = AuthIdentity.builder().id(2L).isActive(true).identityType(IdentityType.STAFF).build();

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "dup@example.com", IdentityType.STAFF, 21L))
                .thenReturn(List.of(a, b));

        assertThatThrownBy(() -> authIdentityDetailsService.loadUserByUsername("dup@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Falls back to tenant users.email when org-scoped login identifier is not found")
    void fallsBackToTenantUserEmail() {
        TenantContext.setSchemaName("tenant_11");
        TenantContext.setOrganisationId(11L);

        AuthIdentity identity = AuthIdentity.builder()
                .id(22L)
                .loginIdentifier("adnan")
                .normalisedLoginIdentifier("adnan")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "adnanazad522@gmail.com", IdentityType.STAFF, 11L))
                .thenReturn(List.of());
        when(userRepository.findAuthIdByEmail("adnanazad522@gmail.com"))
                .thenReturn(Optional.of(22L));
        when(userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(22L, 11L))
                .thenReturn(true);
        when(authIdentityRepository.findById(22L)).thenReturn(Optional.of(identity));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(22L, 11L))
                .thenReturn(List.of());

        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService.loadUserByUsername("adnanazad522@gmail.com");

        assertThat(principal.getAuthId()).isEqualTo(22L);
        assertThat(principal.getLoginIdentifier()).isEqualTo("adnan");
        verify(userRepository).findAuthIdByEmail("adnanazad522@gmail.com");
    }

    @Test
    @DisplayName("Email fallback resolves second-org identity when login_identifier is not the email")
    void resolvesSecondOrgIdentityViaProfileEmail() {
        TenantContext.setSchemaName("tenant_42");
        TenantContext.setOrganisationId(42L);

        AuthIdentity therapistIdentity = AuthIdentity.builder()
                .id(99L)
                .loginIdentifier("elena.apex")
                .normalisedLoginIdentifier("elena.apex")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "elena.rostova@yopmail.com", IdentityType.STAFF, 42L))
                .thenReturn(List.of());
        when(userRepository.findAuthIdByEmail("elena.rostova@yopmail.com"))
                .thenReturn(Optional.of(99L));
        when(userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(99L, 42L))
                .thenReturn(true);
        when(authIdentityRepository.findById(99L)).thenReturn(Optional.of(therapistIdentity));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(99L, 42L))
                .thenReturn(List.of());

        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService
                .loadUserByUsername("elena.rostova@yopmail.com");

        assertThat(principal.getAuthId()).isEqualTo(99L);
        assertThat(principal.getLoginIdentifier()).isEqualTo("elena.apex");
    }

    @Test
    @DisplayName("Public/platform context uses unscoped email/username lookup")
    void usesUnscopedLookupOnPublicSchema() {
        TenantContext.setSchemaName("public");

        AuthIdentity identity = AuthIdentity.builder()
                .id(9L)
                .loginIdentifier("platform@example.com")
                .normalisedLoginIdentifier("platform@example.com")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsername("platform@example.com"))
                .thenReturn(List.of(identity));
        when(authIdentityRoleRepository.findByAuthIdWithRolesAndPermissionsForPlatform(9L))
                .thenReturn(List.of());

        AuthPrincipal principal = (AuthPrincipal) authIdentityDetailsService.loadUserByUsername("platform@example.com");

        assertThat(principal.getAuthId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Throws UsernameNotFoundException when not found by org-scoped lookup or tenant email")
    void throwsWhenNotFoundByAnyIdentifier() {
        TenantContext.setSchemaName("tenant_12");
        TenantContext.setOrganisationId(12L);

        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "missing@example.com", IdentityType.STAFF, 12L))
                .thenReturn(List.of());
        when(userRepository.findAuthIdByEmail("missing@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authIdentityDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Identity not found");
    }
}
