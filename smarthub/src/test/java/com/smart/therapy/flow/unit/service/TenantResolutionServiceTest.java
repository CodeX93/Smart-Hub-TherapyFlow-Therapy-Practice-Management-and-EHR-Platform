package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.UserOrganisationAccessBlockRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantResolutionService tests")
class TenantResolutionServiceTest {

    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private UserOrganisationRepository userOrganisationRepository;
    @Mock
    private UserOrganisationAccessBlockRepository userOrganisationAccessBlockRepository;
    @Mock
    private TenantDirectoryService tenantDirectoryService;
    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantResolutionService tenantResolutionService;

    @Test
    @DisplayName("resolveByEmail returns separate orgs when same email has distinct identities per tenant")
    void resolveByEmailReturnsDistinctOrgsForSameEmail() {
        Organisation orgA = Organisation.builder().id(11L).slug("acme").build();
        Organisation orgB = Organisation.builder().id(12L).slug("beta").build();
        AuthIdentity identityA = AuthIdentity.builder()
                .id(22L)
                .email("shared@example.com")
                .normalisedEmail("shared@example.com")
                .username("user.acme")
                .loginIdentifier("user.acme")
                .organisation(orgA)
                .build();
        AuthIdentity identityB = AuthIdentity.builder()
                .id(33L)
                .email("shared@example.com")
                .normalisedEmail("shared@example.com")
                .username("user.beta")
                .loginIdentifier("user.beta")
                .organisation(orgB)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsername("shared@example.com"))
                .thenReturn(List.of(identityA, identityB));
        when(userOrganisationAccessBlockRepository.findBlockedOrganisationIds(22L)).thenReturn(List.of());
        when(userOrganisationAccessBlockRepository.findBlockedOrganisationIds(33L)).thenReturn(List.of());
        when(userOrganisationRepository.findByAuth_Id(22L)).thenReturn(List.of());
        when(userOrganisationRepository.findByAuth_Id(33L)).thenReturn(List.of());

        List<UserOrganisation> resolved = tenantResolutionService.resolveByEmail("shared@example.com");

        assertThat(resolved).hasSize(2);
        assertThat(resolved)
                .extracting(row -> row.getOrganisation().getId())
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(resolved)
                .extracting(row -> row.getAuth().getId())
                .containsExactlyInAnyOrder(22L, 33L);
    }

    @Test
    @DisplayName("resolveByEmail falls back to identity.organisation when user_organisations row is missing")
    void resolveByEmailFallsBackToIdentityOrganisation() {
        Organisation org = Organisation.builder().id(11L).slug("acme").build();
        AuthIdentity identity = AuthIdentity.builder()
                .id(22L)
                .loginIdentifier("adnan")
                .normalisedLoginIdentifier("adnan")
                .organisation(org)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsername("adnan"))
                .thenReturn(List.of(identity));
        when(userOrganisationAccessBlockRepository.findBlockedOrganisationIds(22L))
                .thenReturn(List.of());
        when(userOrganisationRepository.findByAuth_Id(22L))
                .thenReturn(List.of());

        List<UserOrganisation> resolved = tenantResolutionService.resolveByEmail("adnan");

        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).getOrganisation().getId()).isEqualTo(11L);
        assertThat(resolved.get(0).getAuth().getId()).isEqualTo(22L);
    }

    @Test
    @DisplayName("resolveByEmail can resolve by tenant users.email when login identifier differs")
    void resolveByEmailResolvesByTenantUserEmailFallback() {
        Organisation org = Organisation.builder().id(11L).slug("acme").build();
        AuthIdentity identity = AuthIdentity.builder()
                .id(22L)
                .loginIdentifier("adnan")
                .normalisedLoginIdentifier("adnan")
                .organisation(org)
                .build();
        User tenantUser = User.builder()
                .email("adnanazad522@gmail.com")
                .authIdentity(identity)
                .build();

        TenantDirectoryService.TenantInfo tenantInfo =
                new TenantDirectoryService.TenantInfo(11L, "tenant_11", "acme", "ACTIVE", null);

        when(authIdentityRepository.findAllByEmailOrUsername("adnanazad522@gmail.com"))
                .thenReturn(List.of());
        when(tenantDirectoryService.getByOrganisationId()).thenReturn(Map.of(11L, tenantInfo));
        when(tenantTransactionExecutor.executeReadOnly(eq(11L), eq("tenant_11"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> work = (Supplier<Object>) invocation.getArgument(2);
                    return work.get();
                });
        when(userRepository.findByEmail("adnanazad522@gmail.com")).thenReturn(Optional.of(tenantUser));
        when(authIdentityRepository.findById(22L)).thenReturn(Optional.of(identity));
        when(userOrganisationAccessBlockRepository.findBlockedOrganisationIds(22L))
                .thenReturn(List.of());
        when(userOrganisationRepository.findByAuth_Id(22L)).thenReturn(List.of());

        List<UserOrganisation> resolved = tenantResolutionService.resolveByEmail("adnanazad522@gmail.com");

        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).getOrganisation().getId()).isEqualTo(11L);
        assertThat(resolved.get(0).getAuth().getId()).isEqualTo(22L);
    }

    @Test
    @DisplayName("resolveByEmail merges login-identifier and tenant users.email matches across organisations")
    void resolveByEmailMergesLoginAndTenantEmailMatches() {
        Organisation orgA = Organisation.builder().id(11L).slug("acme").build();
        Organisation orgB = Organisation.builder().id(12L).slug("blue").build();

        AuthIdentity identityFromLogin = AuthIdentity.builder()
                .id(22L)
                .loginIdentifier("adnanazad522@gmail.com")
                .normalisedLoginIdentifier("adnanazad522@gmail.com")
                .organisation(orgA)
                .build();
        AuthIdentity identityFromTenantEmail = AuthIdentity.builder()
                .id(33L)
                .loginIdentifier("another.login")
                .normalisedLoginIdentifier("another.login")
                .organisation(orgB)
                .build();
        User tenantUser = User.builder()
                .email("adnanazad522@gmail.com")
                .authIdentity(identityFromTenantEmail)
                .build();

        TenantDirectoryService.TenantInfo tenantInfoB =
                new TenantDirectoryService.TenantInfo(12L, "tenant_12", "blue", "ACTIVE", null);
        Map<Long, TenantDirectoryService.TenantInfo> tenants = new LinkedHashMap<>();
        tenants.put(12L, tenantInfoB);

        when(authIdentityRepository.findAllByEmailOrUsername("adnanazad522@gmail.com"))
                .thenReturn(List.of(identityFromLogin));
        when(tenantDirectoryService.getByOrganisationId()).thenReturn(tenants);
        when(tenantTransactionExecutor.executeReadOnly(eq(12L), eq("tenant_12"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> work = (Supplier<Object>) invocation.getArgument(2);
                    return work.get();
                });
        when(userRepository.findByEmail("adnanazad522@gmail.com")).thenReturn(Optional.of(tenantUser));
        when(authIdentityRepository.findById(33L)).thenReturn(Optional.of(identityFromTenantEmail));
        when(userOrganisationAccessBlockRepository.findBlockedOrganisationIds(22L)).thenReturn(List.of());
        when(userOrganisationAccessBlockRepository.findBlockedOrganisationIds(33L)).thenReturn(List.of());
        when(userOrganisationRepository.findByAuth_Id(22L)).thenReturn(List.of());
        when(userOrganisationRepository.findByAuth_Id(33L)).thenReturn(List.of());

        List<UserOrganisation> resolved = tenantResolutionService.resolveByEmail("adnanazad522@gmail.com");

        assertThat(resolved).hasSize(2);
        assertThat(resolved)
                .extracting(row -> row.getOrganisation().getId())
                .containsExactlyInAnyOrder(11L, 12L);
    }

    @Test
    @DisplayName("emailBelongsToOrganisation checks direct identity organisation mapping")
    void emailBelongsToOrganisationUsesIdentityOrganisation() {
        Organisation org = Organisation.builder().id(11L).slug("acme").build();
        AuthIdentity identity = AuthIdentity.builder()
                .id(22L)
                .loginIdentifier("adnan")
                .normalisedLoginIdentifier("adnan")
                .organisation(org)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsername("adnan"))
                .thenReturn(List.of(identity));
        when(userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(22L, 11L))
                .thenReturn(false);
        when(userOrganisationAccessBlockRepository.existsByAuth_IdAndOrganisation_Id(22L, 11L))
                .thenReturn(false);

        boolean belongs = tenantResolutionService.emailBelongsToOrganisation("adnan", 11L);

        assertThat(belongs).isTrue();
    }
}
