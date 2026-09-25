package com.smart.therapy.flow.unit.tenant;

import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantFilter;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingResponse;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantFilter path routing tests")
class TenantFilterPathRoutingTest {

    @Mock private TenantDirectoryService tenantDirectoryService;
    @Mock private TenantSchemaHealthService tenantSchemaHealthService;
    @Mock private PlatformTenantRoutingService platformTenantRoutingService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Resolves tenant from path prefix")
    void shouldResolveTenantFromPath() throws Exception {
        TenantFilter filter = new TenantFilter(tenantDirectoryService, tenantSchemaHealthService, platformTenantRoutingService);
        PlatformTenantRoutingResponse settings = PlatformTenantRoutingResponse.builder()
                .emailAutoRouting(true)
                .pathBasedRouting(true)
                .pathPrefix("/org")
                .orgIdentifier("slug")
                .updatedAt(Instant.now())
                .build();
        when(platformTenantRoutingService.getSettings()).thenReturn(settings);
        TenantDirectoryService.TenantInfo info = new TenantDirectoryService.TenantInfo(7L, "tenant_7", "acme", "ACTIVE", null);
        when(tenantDirectoryService.findBySlug("acme")).thenReturn(Optional.of(info));
        when(tenantSchemaHealthService.schemaExists("tenant_7")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/org/acme/api/v1/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> seenOrgId = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException {
                seenOrgId.set(TenantContext.getOrganisationId());
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(seenOrgId.get()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Leaves public schema when path identifier is unknown")
    void shouldIgnoreUnknownPathIdentifier() throws Exception {
        TenantFilter filter = new TenantFilter(tenantDirectoryService, tenantSchemaHealthService, platformTenantRoutingService);
        PlatformTenantRoutingResponse settings = PlatformTenantRoutingResponse.builder()
                .emailAutoRouting(true)
                .pathBasedRouting(true)
                .pathPrefix("/org")
                .orgIdentifier("slug")
                .updatedAt(Instant.now())
                .build();
        when(platformTenantRoutingService.getSettings()).thenReturn(settings);
        when(tenantDirectoryService.findBySlug("missing")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/org/missing/api/v1/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> seenOrgId = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException {
                seenOrgId.set(TenantContext.getOrganisationId());
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(seenOrgId.get()).isNull();
    }

    @Test
    @DisplayName("Ignores IPv4 host for subdomain resolution")
    void shouldIgnoreIpv4HostForSubdomainResolution() throws Exception {
        TenantFilter filter = new TenantFilter(tenantDirectoryService, tenantSchemaHealthService, platformTenantRoutingService);
        when(platformTenantRoutingService.getSettings()).thenReturn(PlatformTenantRoutingResponse.builder()
                .pathBasedRouting(false)
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("192.168.1.10");
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Long> seenOrgId = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seenOrgId.set(TenantContext.getOrganisationId());
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(seenOrgId.get()).isNull();
        verify(tenantDirectoryService, never()).findBySubdomain("192");
    }
}
