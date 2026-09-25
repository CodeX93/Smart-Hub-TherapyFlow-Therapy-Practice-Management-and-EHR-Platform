package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantStaffProfileBootstrapService;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationSession;
import com.smart.therapy.flow.superadmin.repository.PlatformImpersonationPolicyRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformImpersonationSessionRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminImpersonationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminImpersonationServiceTest {

    @Mock private PlatformImpersonationPolicyRepository policyRepository;
    @Mock private PlatformImpersonationSessionRepository sessionRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private AuthIdentityRepository authIdentityRepository;
    @Mock private UserOrganisationRepository userOrganisationRepository;
    @Mock private PlatformAuditService platformAuditService;
    @Mock private TenantStaffProfileBootstrapService tenantStaffProfileBootstrapService;
    @Mock private AuthIdentityDetailsService authIdentityDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthSessionService authSessionService;
    @InjectMocks private SuperAdminImpersonationService service;

    @Test
    void endingImpersonationImmediatelyRevokesItsIssuedAuthSessions() {
        PlatformImpersonationSession session = PlatformImpersonationSession.builder()
                .id(42L)
                .status("active")
                .updatedAt(Instant.now())
                .build();
        when(sessionRepository.findById(42L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        PlatformImpersonationSession ended = service.endSession(42L, 9L);

        assertThat(ended.getStatus()).isEqualTo("ended");
        verify(authSessionService).revokeAllForImpersonationSession(42L);
    }
}
