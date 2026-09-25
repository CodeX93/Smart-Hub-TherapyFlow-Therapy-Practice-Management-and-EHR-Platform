package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.LoginAttemptRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthKnownDeviceService;
import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.MfaService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingResponse;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService routing tests")
class AuthenticationServiceRoutingTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthSessionService authSessionService;
    @Mock private AuthIdentityService authIdentityService;
    @Mock private AuthIdentityRepository authIdentityRepository;
    @Mock private AuthIdentityDetailsService authIdentityDetailsService;
    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private EmailService emailService;
    @Mock private TenantDirectoryService tenantDirectoryService;
    @Mock private PlatformTenantRoutingService platformTenantRoutingService;
    @Mock private TenantResolutionService tenantResolutionService;
    @Mock private TenantTransactionExecutor tenantTransactionExecutor;
    @Mock private EntityManager entityManager;
    @Mock private AuthRefreshTokenService authRefreshTokenService;
    @Mock private MfaService mfaService;
    @Mock private AuthKnownDeviceService authKnownDeviceService;
    @Mock private com.smart.therapy.flow.audit.service.AuditLogService auditLogService;
    @Mock private com.smart.therapy.flow.common.metrics.AuthAbuseMetrics authAbuseMetrics;

    @InjectMocks
    private AuthenticationService authenticationService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Throws TENANT_SELECTION_REQUIRED when email resolves to multiple orgs")
    void shouldRequireTenantSelection() {
        ReflectionTestUtils.setField(authenticationService, "entityManager", entityManager);

        LoginRequest request = new LoginRequest();
        request.setUsername("multi@example.com");
        request.setPassword("password");

        PlatformTenantRoutingResponse settings = PlatformTenantRoutingResponse.builder()
                .emailAutoRouting(true)
                .pathBasedRouting(false)
                .pathPrefix("/org")
                .orgIdentifier("slug")
                .updatedAt(Instant.now())
                .build();
        when(platformTenantRoutingService.getSettings()).thenReturn(settings);

        Organisation orgA = Organisation.builder().id(1L).name("A").build();
        Organisation orgB = Organisation.builder().id(2L).name("B").build();
        UserOrganisation rowA = UserOrganisation.builder().organisation(orgA).build();
        UserOrganisation rowB = UserOrganisation.builder().organisation(orgB).build();
        when(tenantResolutionService.resolveByEmail("multi@example.com", IdentityType.STAFF))
                .thenReturn(List.of(rowA, rowB));

        assertThatThrownBy(() -> authenticationService.authenticateUser(request, "127.0.0.1"))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCode()).isEqualTo("TENANT_SELECTION_REQUIRED");
                });
    }

    @Test
    @DisplayName("Uses org selection to bind tenant and returns tenant fields in login response")
    void shouldBindTenantOnOrgSelection() {
        ReflectionTestUtils.setField(authenticationService, "entityManager", entityManager);

        LoginRequest request = new LoginRequest();
        request.setUsername("staff@example.com");
        request.setPassword("password");
        request.setOrgSlug("acme");

        PlatformTenantRoutingResponse settings = PlatformTenantRoutingResponse.builder()
                .emailAutoRouting(true)
                .pathBasedRouting(true)
                .pathPrefix("/org")
                .orgIdentifier("slug")
                .updatedAt(Instant.now())
                .build();
        when(platformTenantRoutingService.getSettings()).thenReturn(settings);

        TenantDirectoryService.TenantInfo info =
                new TenantDirectoryService.TenantInfo(42L, "tenant_42", "acme", "acme", "ACTIVE", null);
        when(tenantDirectoryService.findByOrganisationIdOrSlug("acme")).thenReturn(Optional.of(info));
        when(tenantDirectoryService.findByOrganisationId(42L)).thenReturn(Optional.of(info));
        when(tenantResolutionService.emailBelongsToOrganisation("staff@example.com", 42L, IdentityType.STAFF))
                .thenReturn(true);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User testUser = TestDataFactory.createTestTherapist();
        AuthPrincipal principal = TestDataFactory.createAuthPrincipal(testUser);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(mfaService.isEnabled(1L)).thenReturn(false);
        when(mfaService.isEnrollmentRequired(any(), eq(false))).thenReturn(false);
        when(tokenProvider.generateToken(any(Authentication.class), anyString())).thenReturn("access");
        when(authRefreshTokenService.issueRefreshToken(eq(1L), eq("127.0.0.1"), isNull())).thenReturn("refresh");
        when(loginAttemptRepository.save(any())).thenReturn(null);

        JwtAuthenticationResponse response = authenticationService.authenticateUser(request, "127.0.0.1");

        assertThat(response.getTenantSchema()).isEqualTo("tenant_42");
        assertThat(response.getOrganisationId()).isEqualTo(42L);
        assertThat(response.getOrganisationSlug()).isEqualTo("acme");
        assertThat(response.getTenantSchema()).isNotEqualTo("public");
    }

    @Test
    @DisplayName("Rejects login when orgSlug is provided but email not in org")
    void shouldRejectEmailNotInOrgSelection() {
        ReflectionTestUtils.setField(authenticationService, "entityManager", entityManager);

        LoginRequest request = new LoginRequest();
        request.setUsername("user@example.com");
        request.setPassword("password");
        request.setOrgSlug("acme");

        PlatformTenantRoutingResponse settings = PlatformTenantRoutingResponse.builder()
                .emailAutoRouting(true)
                .pathBasedRouting(true)
                .pathPrefix("/org")
                .orgIdentifier("slug")
                .updatedAt(Instant.now())
                .build();
        when(platformTenantRoutingService.getSettings()).thenReturn(settings);

        TenantDirectoryService.TenantInfo info = new TenantDirectoryService.TenantInfo(44L, "tenant_44", "acme", "ACTIVE", null);
        when(tenantDirectoryService.findByOrganisationIdOrSlug("acme")).thenReturn(Optional.of(info));
        when(tenantResolutionService.emailBelongsToOrganisation("user@example.com", 44L, IdentityType.STAFF))
                .thenReturn(false);

        assertThatThrownBy(() -> authenticationService.authenticateUser(request, "127.0.0.1"))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("EMAIL_NOT_IN_ORG");
                });
    }
}
