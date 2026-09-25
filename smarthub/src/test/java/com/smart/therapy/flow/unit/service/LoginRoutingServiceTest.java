package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.dto.LoginContextResponse;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.Role;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRoutingService tests")
class LoginRoutingServiceTest {

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
    @DisplayName("Returns org context for valid orgSlug")
    void shouldResolveByOrgSlug() {
        Organisation org = Organisation.builder()
                .id(10L)
                .name("Acme")
                .slug("acme")
                .logoUrl("https://cdn.example.com/logo.png")
                .brandPrimaryColor("#112233")
                .build();
        when(organisationRepository.findBySlug("acme")).thenReturn(Optional.of(org));
        when(ssoService.getEnabledProvidersForOrganisation(10L)).thenReturn(List.of("GOOGLE"));

        LoginContextResponse response = loginRoutingService.resolveContext(null, "acme");

        assertThat(response.getOrganisationId()).isEqualTo(10L);
        assertThat(response.getOrgSlug()).isEqualTo("acme");
        assertThat(response.getBranding()).isNotNull();
        assertThat(response.getBranding().getLogoUrl()).contains("logo");
        assertThat(response.getSsoProviders()).contains("GOOGLE");
    }

    @Test
    @DisplayName("Throws EMAIL_NOT_IN_ORG when email is not mapped to orgSlug")
    void shouldRejectEmailNotInOrg() {
        Organisation org = Organisation.builder()
                .id(12L)
                .name("Beta")
                .slug("beta")
                .build();
        when(organisationRepository.findBySlug("beta")).thenReturn(Optional.of(org));
        when(tenantResolutionService.emailBelongsToOrganisation("user@example.com", 12L, IdentityType.STAFF))
                .thenReturn(false);

        assertThatThrownBy(() -> loginRoutingService.resolveContext("user@example.com", "beta"))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("EMAIL_NOT_IN_ORG");
                });
    }

    @Test
    @DisplayName("Returns multiple orgs for email with multiple memberships")
    void shouldReturnMultiOrgList() {
        Organisation orgA = Organisation.builder().id(1L).name("A").slug("a").build();
        Organisation orgB = Organisation.builder().id(2L).name("B").slug("b").build();
        UserOrganisation rowA = UserOrganisation.builder().organisation(orgA).build();
        UserOrganisation rowB = UserOrganisation.builder().organisation(orgB).build();
        when(tenantResolutionService.resolveByEmail("multi@example.com", IdentityType.STAFF))
                .thenReturn(List.of(rowA, rowB));
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("multi@example.com"), eq(IdentityType.STAFF), anyLong()))
                .thenReturn(List.of());

        LoginContextResponse response = loginRoutingService.resolveContext("multi@example.com", null);

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getOrganisations()).hasSize(2);
    }

    @Test
    @DisplayName("Includes per-org role hints for multi-org login context")
    void shouldIncludeRoleHintsPerOrg() {
        Organisation orgA = Organisation.builder().id(1L).name("Acme").slug("acme").build();
        Organisation orgB = Organisation.builder().id(2L).name("Beta").slug("beta").build();
        AuthIdentity authA = AuthIdentity.builder().id(10L).loginIdentifier("elena.admin")
                .identityType(IdentityType.STAFF).isActive(true).build();
        AuthIdentity authB = AuthIdentity.builder().id(20L).loginIdentifier("elena.therapist")
                .identityType(IdentityType.STAFF).isActive(true).build();
        UserOrganisation rowA = UserOrganisation.builder().organisation(orgA).auth(authA).build();
        UserOrganisation rowB = UserOrganisation.builder().organisation(orgB).auth(authB).build();

        Role therapist = Role.builder().name("THERAPIST").isActive(true).build();
        Role admin = Role.builder().name("ADMIN").isActive(true).build();
        AuthIdentityRole airTherapist = AuthIdentityRole.builder().role(therapist).build();
        AuthIdentityRole airAdmin = AuthIdentityRole.builder().role(admin).build();

        when(tenantResolutionService.resolveByEmail("multi@example.com", IdentityType.STAFF))
                .thenReturn(List.of(rowA, rowB));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(10L, 1L))
                .thenReturn(List.of(airTherapist));
        when(authIdentityRoleRepository.findByAuthIdAndOrganisationIdWithRolesAndPermissions(20L, 2L))
                .thenReturn(List.of(airAdmin));
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                any(), any(), anyLong()))
                .thenReturn(List.of());

        LoginContextResponse response = loginRoutingService.resolveContext("multi@example.com", null);

        assertThat(response.getOrganisations()).hasSize(2);
        assertThat(response.getOrganisations().get(0).getRoles()).containsExactly("THERAPIST");
        assertThat(response.getOrganisations().get(0).getUsername()).isEqualTo("elena.admin");
        assertThat(response.getOrganisations().get(1).getRoles()).containsExactly("ADMIN");
        assertThat(response.getOrganisations().get(1).getUsername()).isEqualTo("elena.therapist");
    }
}
