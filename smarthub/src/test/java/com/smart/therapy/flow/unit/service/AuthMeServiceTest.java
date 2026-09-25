package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.dto.AuthMeResponse;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.service.AuthMeService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.user.dto.UserResponse;
import com.smart.therapy.flow.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthMeService tests")
class AuthMeServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthMeService authMeService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Public platform identity returns auth roles without accessing tenant user tables")
    void shouldAvoidTenantUserLookupForPlatformContext() {
        TenantContext.setSchemaName("public");

        AuthPrincipal principal = new AuthPrincipal(
                169L,
                "platform.superadmin@therapyflowseed.com",
                "hash",
                List.of(
                        new SimpleGrantedAuthority("ROLE_PLATFORM_SUPER_ADMIN"),
                        new SimpleGrantedAuthority("USER_VIEW")),
                true,
                IdentityType.STAFF,
                null
        );

        AuthMeResponse response = authMeService.getCurrentUserContext(principal);

        assertThat(response.getIsPlatformAdmin()).isTrue();
        assertThat(response.getRoles()).containsExactly("PLATFORM_SUPER_ADMIN");
        assertThat(response.getUser()).isNull();
        assertThat(response.getPermissions()).containsExactly("USER_VIEW");
        verifyNoInteractions(currentUserService, userService);
    }

    @Test
    @DisplayName("SUPERVISOR role sets isSupervisor true")
    void shouldMarkSupervisorRole() {
        TenantContext.setSchemaName("tenant_acme");
        TenantContext.setOrganisationId(101L);

        AuthPrincipal principal = new AuthPrincipal(
                77L,
                "supervisor@acme.com",
                "hash",
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPERVISOR"),
                        new SimpleGrantedAuthority("SESSION_VIEW")),
                true,
                IdentityType.STAFF,
                null
        );

        when(currentUserService.getCurrentUser(principal)).thenReturn(Optional.of(mock(com.smart.therapy.flow.auth.entity.User.class)));
        when(userService.getCurrentUser(principal)).thenReturn(UserResponse.builder()
                .id(55L)
                .email("supervisor@acme.com")
                .roles(List.of())
                .build());

        AuthMeResponse response = authMeService.getCurrentUserContext(principal);

        assertThat(response.getIsSupervisor()).isTrue();
        assertThat(response.getIsTherapist()).isFalse();
        assertThat(response.getRoles()).containsExactly("SUPERVISOR");
        assertThat(response.getUser().getRoles()).containsExactly("SUPERVISOR");
    }

    @Test
    @DisplayName("CLIENT identity returns client summary and empty authority lists when authorities are missing")
    void shouldReturnClientSummaryWithEmptyAuthorityLists() {
        TenantContext.setSchemaName("tenant_acme");
        TenantContext.setOrganisationId(101L);

        AuthPrincipal principal = new AuthPrincipal(
                501L,
                "client@example.com",
                "hash",
                null,
                true,
                IdentityType.CLIENT,
                null
        );

        Client client = mock(Client.class);
        when(client.getId()).thenReturn(123L);
        when(client.getFullName()).thenReturn("John Doe");
        when(client.getPrimaryEmail()).thenReturn("client@example.com");
        when(client.getPrimaryPhone()).thenReturn("+1-555-111-2222");
        when(currentUserService.getCurrentClient(principal)).thenReturn(Optional.of(client));

        AuthMeResponse response = authMeService.getCurrentUserContext(principal);

        assertThat(response.getIsClient()).isTrue();
        assertThat(response.getRoles()).isEmpty();
        assertThat(response.getPermissions()).isEmpty();
        assertThat(response.getAuthorities()).isEmpty();
        assertThat(response.getUser()).isNull();
        assertThat(response.getClient()).isNotNull();
        assertThat(response.getClient().getEmail()).isEqualTo("client@example.com");
        assertThat(response.getClient().getPortalEmail()).isEqualTo("client@example.com");
    }
}
