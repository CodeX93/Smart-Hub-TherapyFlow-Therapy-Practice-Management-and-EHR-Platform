package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.auth.service.SsoSecurityService;
import com.smart.therapy.flow.auth.service.SsoService;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationSsoConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SsoService, including mandatory safeguard: TenantContext rehydration from state
 * before any tenant-scoped repository access (user load/create, JWT issue).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SsoService Unit Tests")
class SsoServiceTest {

    private static final long ORG_ID = 99L;
    private static final String SCHEMA_NAME = "tenant_99";
    private static final String STATE = ORG_ID + ":GOOGLE";

    @Mock
    private OrganisationSsoConfigRepository ssoConfigRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private AuthIdentityRoleRepository authIdentityRoleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthIdentityDetailsService authIdentityDetailsService;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AuthSessionService authSessionService;
    @Mock
    private AuthIdentityService authIdentityService;

    @Mock
    private SsoSecurityService ssoSecurityService;

    @InjectMocks
    private SsoService ssoService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        when(ssoSecurityService.consumeStateOrThrow(eq(STATE), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SsoSecurityService.ConsumedState(ORG_ID, "GOOGLE", "http://127.0.0.1/callback", "test-nonce", "test-pkce"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("handleCallback rehydrates TenantContext from state before any tenant-scoped access")
    void handleCallback_rehydratesTenantContextFromStateBeforeTenantScopedAccess() {
        Organisation org = Organisation.builder()
                .id(ORG_ID)
                .name("Test Org")
                .slug("test-org")
                .status("ACTIVE")
                .schemaName(SCHEMA_NAME)
                .subdomain("testorg")
                .build();

        when(organisationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(ssoConfigRepository.findByOrganisationIdAndProviderAndIsEnabledTrue(eq(ORG_ID), eq("GOOGLE")))
                .thenAnswer(inv -> {
                    assertThat(TenantContext.getSchemaName()).isEqualTo(SCHEMA_NAME);
                    assertThat(TenantContext.getOrganisationId()).isEqualTo(ORG_ID);
                    return Optional.empty();
                });

        assertThatThrownBy(() -> ssoService.handleCallback(
                "some-code",
                STATE,
                "127.0.0.1"))
                .isInstanceOf(StoryApiException.class)
                .hasMessageContaining("SSO not configured");

        // Mandatory: TenantContext must have been set from state (org id + schema) before any tenant-scoped
        // repository access (userRepository, roleRepository). We failed at ssoConfigRepository, so we never
        // reached userRepository — but TenantContext was already rehydrated after loading org.
        assertThat(TenantContext.getOrganisationId()).isNull();
        assertThat(TenantContext.getSchemaName()).isIn(null, "public");
    }

    @Test
    @DisplayName("handleCallback throws TenantUnavailableException when organisation is not ACTIVE")
    void handleCallback_rejectsNonActiveOrganisation() {
        Organisation lockedOrg = Organisation.builder()
                .id(ORG_ID)
                .name("Locked Org")
                .slug("locked-org")
                .status("LOCKED")
                .schemaName(SCHEMA_NAME)
                .subdomain("lockedorg")
                .build();

        when(organisationRepository.findById(ORG_ID)).thenReturn(Optional.of(lockedOrg));

        assertThatThrownBy(() -> ssoService.handleCallback(
                "code",
                STATE,
                "127.0.0.1"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.TenantUnavailableException.class)
                .hasMessageContaining("Tenant is unavailable");

        assertThat(TenantContext.getOrganisationId()).isNull();
        assertThat(TenantContext.getSchemaName()).isIn(null, "public");
    }
}
