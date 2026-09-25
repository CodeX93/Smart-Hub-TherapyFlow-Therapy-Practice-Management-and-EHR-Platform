package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.dto.LoginContextResponse;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.service.LoginRoutingService;
import com.smart.therapy.flow.auth.service.SsoService;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Login-context flow: identifier (email or username) → org list → login with org.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRoutingService tenant-scoped identifier flow")
class LoginRoutingServiceTenantScopedFlowTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private TenantResolutionService tenantResolutionService;
    @Mock
    private SsoService ssoService;
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private AuthIdentityRoleRepository authIdentityRoleRepository;

    @InjectMocks
    private LoginRoutingService loginRoutingService;

    @Test
    @DisplayName("Resolves single org when identifier is a username")
    void resolvesSingleOrgByUsername() {
        Organisation org = Organisation.builder()
                .id(10L)
                .name("Acme")
                .slug("acme")
                .build();
        AuthIdentity identity = AuthIdentity.builder()
                .id(55L)
                .username("jdoe")
                .normalisedUsername("jdoe")
                .email("jdoe@acme.com")
                .loginIdentifier("jdoe")
                .identityType(IdentityType.STAFF)
                .organisation(org)
                .isActive(true)
                .build();
        UserOrganisation row = UserOrganisation.builder()
                .organisation(org)
                .auth(identity)
                .build();

        when(tenantResolutionService.resolveByEmail("jdoe", IdentityType.STAFF)).thenReturn(List.of(row));
        when(ssoService.getEnabledProvidersForOrganisation(10L)).thenReturn(List.of());

        LoginContextResponse response = loginRoutingService.resolveContext("jdoe", null);

        assertThat(response.getCount()).isEqualTo(1);
        assertThat(response.getOrganisationId()).isEqualTo(10L);
        assertThat(response.getOrgSlug()).isEqualTo("acme");
        assertThat(response.getEmail()).isEqualTo("jdoe");
    }

    @Test
    @DisplayName("Same email in two tenants returns org picker (count > 1)")
    void sameEmailAcrossTenantsRequiresPicker() {
        Organisation orgA = Organisation.builder().id(1L).name("Acme").slug("acme").build();
        Organisation orgB = Organisation.builder().id(2L).name("Beta").slug("beta").build();
        AuthIdentity authA = AuthIdentity.builder()
                .id(10L)
                .username("elena.acme")
                .email("shared@example.com")
                .loginIdentifier("elena.acme")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();
        AuthIdentity authB = AuthIdentity.builder()
                .id(20L)
                .username("elena.beta")
                .email("shared@example.com")
                .loginIdentifier("elena.beta")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();
        UserOrganisation rowA = UserOrganisation.builder().organisation(orgA).auth(authA).build();
        UserOrganisation rowB = UserOrganisation.builder().organisation(orgB).auth(authB).build();

        when(tenantResolutionService.resolveByEmail("shared@example.com", IdentityType.STAFF))
                .thenReturn(List.of(rowA, rowB));
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("shared@example.com"), eq(IdentityType.STAFF), anyLong()))
                .thenReturn(List.of());
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(10L, 1L))
                .thenReturn(List.of());
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(20L, 2L))
                .thenReturn(List.of());

        LoginContextResponse response = loginRoutingService.resolveContext("shared@example.com", null);

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getOrganisations()).extracting(LoginContextResponse.OrgSummary::getSlug)
                .containsExactly("acme", "beta");
        assertThat(response.getOrganisations()).extracting(LoginContextResponse.OrgSummary::getUsername)
                .containsExactly("elena.acme", "elena.beta");
    }

    @Test
    @DisplayName("Unknown identifier returns TENANT_NOT_FOUND")
    void unknownIdentifierNotFound() {
        when(tenantResolutionService.resolveByEmail("nobody", IdentityType.STAFF)).thenReturn(List.of());

        assertThatThrownBy(() -> loginRoutingService.resolveContext("nobody", null))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("TENANT_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Missing identifier and orgSlug fails validation")
    void requiresIdentifierOrSlug() {
        assertThatThrownBy(() -> loginRoutingService.resolveContext(null, null))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR");
                });
    }
}
