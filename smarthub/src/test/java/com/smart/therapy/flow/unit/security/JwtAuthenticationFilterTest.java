package com.smart.therapy.flow.unit.security;

import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.JwtAuthenticationFilter;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationAccessBlockRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter tenant binding tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthIdentityDetailsService authIdentityDetailsService;
    @Mock private AuthSessionService authSessionService;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TenantDirectoryService tenantDirectoryService;
    @Mock private UserOrganisationAccessBlockRepository userOrganisationAccessBlockRepository;
    @Mock private PlatformAuditService platformAuditService;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Rejects tenant request when token missing orgId binding")
    void shouldRejectMissingOrgBinding() throws Exception {
        TenantContext.setSchemaName("tenant_42");
        TenantContext.setOrganisationId(42L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("token")).thenReturn(1L);
        when(tokenProvider.getJtiFromToken("token")).thenReturn(null);
        when(tokenProvider.getTenantSchemaFromToken("token")).thenReturn("tenant_42");
        when(tokenProvider.getOrganisationIdFromToken("token")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token is missing tenant binding");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Rejects tenant request when token orgId mismatches")
    void shouldRejectOrgMismatch() throws Exception {
        TenantContext.setSchemaName("tenant_42");
        TenantContext.setOrganisationId(42L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("token")).thenReturn(1L);
        when(tokenProvider.getJtiFromToken("token")).thenReturn(null);
        when(tokenProvider.getTenantSchemaFromToken("token")).thenReturn("tenant_42");
        when(tokenProvider.getOrganisationIdFromToken("token")).thenReturn(99L);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token organisation does not match request tenant");
        verify(filterChain, never()).doFilter(request, response);
    }
}
