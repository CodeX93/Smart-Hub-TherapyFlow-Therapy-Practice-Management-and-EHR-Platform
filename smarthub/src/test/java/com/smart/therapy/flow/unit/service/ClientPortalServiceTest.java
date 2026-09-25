package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSession;
import com.smart.therapy.flow.client.portal.dto.*;
import com.smart.therapy.flow.client.portal.service.ClientPortalService;
import com.smart.therapy.flow.client.repository.ClientPortalSessionRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.auth.service.MfaService;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.PortalAccessDisabledException;
import com.smart.therapy.flow.common.exception.PortalActivationPendingException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClientPortalService Unit Tests")
class ClientPortalServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientPortalSessionRepository sessionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.smart.therapy.flow.session.repository.SessionRepository sessionRepositoryForAppointments;

    @Mock
    private com.smart.therapy.flow.billing.repository.ServiceRepository serviceRepository;

    @Mock
    private com.smart.therapy.flow.session.repository.RoomRepository roomRepository;

    @Mock
    private com.smart.therapy.flow.notification.repository.NotificationRepository notificationRepository;

    @Mock
    private com.smart.therapy.flow.billing.repository.SessionBillingRepository sessionBillingRepository;

    @Mock
    private com.smart.therapy.flow.user.repository.UserProfileRepository userProfileRepository;

    @Mock
    private com.smart.therapy.flow.user.repository.TherapistBlockedTimeRepository therapistBlockedTimeRepository;

    @Mock
    private com.smart.therapy.flow.auth.repository.UserRepository userRepository;

    @Mock
    private AuthIdentityService authIdentityService;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private ClientPortalSettingsService portalSettingsService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private com.smart.therapy.flow.common.security.AuthIdentityDetailsService authIdentityDetailsService;

    @Mock
    private com.smart.therapy.flow.document.repository.DocumentRepository documentRepository;

    @Mock
    private com.smart.therapy.flow.document.repository.FormAssignmentRepository formAssignmentRepository;

    @Mock
    private com.smart.therapy.flow.document.repository.FormTemplateRepository formTemplateRepository;

    @Mock
    private com.smart.therapy.flow.document.repository.FormFieldRepository formFieldRepository;

    @Mock
    private com.smart.therapy.flow.document.repository.FormResponseRepository formResponseRepository;

    @Mock
    private com.smart.therapy.flow.document.repository.FormSignatureRepository formSignatureRepository;

    @Mock
    private com.smart.therapy.flow.client.repository.PatientConsentRepository patientConsentRepository;

    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Mock
    private TenantDirectoryService tenantDirectoryService;

    @Mock
    private TenantSchemaHealthService tenantSchemaHealthService;

    @Mock
    private com.smart.therapy.flow.superadmin.service.TenantResolutionService tenantResolutionService;

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private AuthRefreshTokenService authRefreshTokenService;

    @Mock
    private MfaService mfaService;

    @Mock
    private com.smart.therapy.flow.auth.service.AuthKnownDeviceService authKnownDeviceService;

    @InjectMocks
    private ClientPortalService clientPortalService;

    private Client client;

    @BeforeEach
    void setUp() {
        TenantContext.setSchemaName("tenant_test");
        TenantContext.setOrganisationId(1L);
        client = TestDataFactory.createTestClient();
        client.setId(1L);
        when(tenantTransactionExecutor.executeWriteIsolated(anyLong(), anyString(), any()))
                .thenAnswer(invocation -> {
                    String schema = TenantContext.getSchemaName();
                    Long org = TenantContext.getOrganisationId();
                    try {
                        TenantContext.setSchemaName(invocation.getArgument(1));
                        TenantContext.setOrganisationId(invocation.getArgument(0));
                        return ((Supplier<?>) invocation.getArgument(2)).get();
                    } finally {
                        TenantContext.setSchemaName(schema);
                        TenantContext.setOrganisationId(org);
                    }
                });
        when(mfaService.isEnabled(anyLong())).thenReturn(false);
        when(mfaService.isEnrollmentRequired(any(), eq(false))).thenReturn(false);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should reject login when portal activation is pending")
    void shouldRejectLoginWhenPortalActivationIsPending() {
        PortalLoginRequest request = new PortalLoginRequest();
        request.setEmail("client@example.com");
        request.setPassword("password123");

        AuthIdentity identity = AuthIdentity.builder()
                .id(1L)
                .loginIdentifier("client@example.com")
                .normalisedLoginIdentifier("client@example.com")
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .build();
        when(authIdentityService.getByNormalisedLogin(anyString())).thenReturn(identity);
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("client@example.com"), eq(IdentityType.CLIENT), eq(1L)))
                .thenReturn(List.of(identity));
        when(clientRepository.findByAuthId(1L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(true);
        when(portalSettingsService.isActivated(1L)).thenReturn(false);

        assertThatThrownBy(() -> clientPortalService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(PortalActivationPendingException.class)
                .hasMessageContaining("Portal activation is pending");
    }

    @Test
    @DisplayName("Should revoke all sessions after portal password reset")
    void shouldRevokeAllSessionsAfterPortalPasswordReset() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setPassword("NewPassword123!");
        Organisation organisation = Organisation.builder().id(1L).build();
        AuthIdentity identity = AuthIdentity.builder()
                .id(44L)
                .identityType(IdentityType.CLIENT)
                .organisation(organisation)
                .build();

        when(authIdentityService.validatePasswordResetToken("reset-token")).thenReturn(identity);
        when(tenantDirectoryService.findByOrganisationId(1L)).thenReturn(Optional.of(
                new TenantDirectoryService.TenantInfo(1L, "tenant_test", "test", "ACTIVE", null)));
        when(tenantSchemaHealthService.schemaExists("tenant_test")).thenReturn(true);
        when(tenantSchemaHealthService.tableExists("tenant_test", "clients")).thenReturn(true);
        when(tenantTransactionExecutor.executeReadOnly(eq(1L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> work = invocation.getArgument(2);
                    return work.get();
                });
        when(clientRepository.findByAuthId(44L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(true);
        when(authIdentityService.resetPasswordByToken("reset-token", "NewPassword123!")).thenReturn(identity);
        when(authIdentityService.validatePassword(identity, "NewPassword123!")).thenReturn(true);

        clientPortalService.resetPassword(request);

        verify(tokenBlacklistService).blacklistAllUserTokens(44L);
        verify(authRefreshTokenService).revokeAllForAuthId(44L);
        verify(authKnownDeviceService).revokeAllTrustedDevices(44L);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Arrange: unified auth flow (AuthIdentity + ClientPortalSettings)
        PortalLoginRequest request = new PortalLoginRequest();
        request.setEmail("client@example.com");
        request.setPassword("password123");

        AuthIdentity identity = AuthIdentity.builder()
                .id(1L)
                .loginIdentifier("client@example.com")
                .normalisedLoginIdentifier("client@example.com")
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .build();
        when(authIdentityService.getByNormalisedLogin(anyString())).thenReturn(identity);
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("client@example.com"), eq(IdentityType.CLIENT), eq(1L)))
                .thenReturn(List.of(identity));
        when(clientRepository.findByAuthId(1L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(true);
        when(portalSettingsService.isActivated(1L)).thenReturn(true);
        when(authIdentityService.isLocked(identity)).thenReturn(false);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(authIdentityService.validatePassword(identity, "password123")).thenReturn(true);
        AuthPrincipal authPrincipal = TestDataFactory.createAuthPrincipalForClient(1L);
        when(authIdentityDetailsService.loadUserByAuthId(1L)).thenReturn(authPrincipal);
        when(jwtTokenProvider.generateToken(any(), anyString())).thenReturn("test-access-token");
        when(jwtTokenProvider.getExpirationDateFromToken("test-access-token"))
                .thenReturn(java.util.Date.from(Instant.now().plusSeconds(3600)));
        when(authRefreshTokenService.issueRefreshToken(eq(1L), eq("127.0.0.1"), eq("test-agent")))
                .thenReturn("test-refresh-token");
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));

        // Act
        PortalLoginResponse response = clientPortalService.login(request, "127.0.0.1", "test-agent");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getClient()).isNotNull();
        verify(authSessionService).createSession(eq(1L), anyString(), any(), eq("127.0.0.1"), eq("test-agent"));
        verify(authKnownDeviceService).recordSuccessfulLogin(1L, "127.0.0.1", "test-agent");
    }

    @Test
    @DisplayName("Should login successfully from public schema when portal access is enabled in tenant")
    void shouldLoginSuccessfullyFromPublicSchemaWhenPortalAccessEnabled() {
        TenantContext.setSchemaName("public");
        TenantContext.setOrganisationId(null);

        PortalLoginRequest request = new PortalLoginRequest();
        request.setEmail("client@example.com");
        request.setPassword("password123");

        Organisation organisation = TestDataFactory.createTestOrganisation();
        AuthIdentity identity = AuthIdentity.builder()
                .id(1L)
                .loginIdentifier("client@example.com")
                .normalisedLoginIdentifier("client@example.com")
                .identityType(IdentityType.CLIENT)
                .organisation(organisation)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByEmailOrUsername(eq("client@example.com")))
                .thenReturn(List.of(identity));
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("client@example.com"), eq(IdentityType.CLIENT), eq(1L)))
                .thenReturn(List.of(identity));
        when(tenantResolutionService.isEmailBlockedInOrganisation("client@example.com", 1L)).thenReturn(false);
        when(tenantDirectoryService.findByOrganisationId(1L))
                .thenReturn(Optional.of(new TenantDirectoryService.TenantInfo(1L, "tenant_test", "test-org", "ACTIVE", null)));
        when(tenantTransactionExecutor.executeReadOnly(eq(1L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> work = (Supplier<Object>) invocation.getArgument(2);
                    return work.get();
                });
        when(clientRepository.findByAuthId(1L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(true);
        when(portalSettingsService.isActivated(1L)).thenReturn(true);
        when(authIdentityService.isLocked(identity)).thenReturn(false);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(authIdentityService.validatePassword(identity, "password123")).thenReturn(true);
        AuthPrincipal authPrincipal = TestDataFactory.createAuthPrincipalForClient(1L);
        when(authIdentityDetailsService.loadUserByAuthId(1L)).thenReturn(authPrincipal);
        when(jwtTokenProvider.generateToken(any(), anyString())).thenReturn("test-access-token");
        when(jwtTokenProvider.getExpirationDateFromToken("test-access-token"))
                .thenReturn(java.util.Date.from(Instant.now().plusSeconds(3600)));
        when(authRefreshTokenService.issueRefreshToken(eq(1L), eq("127.0.0.1"), eq("test-agent")))
                .thenReturn("test-refresh-token");
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));

        PortalLoginResponse response = clientPortalService.login(request, "127.0.0.1", "test-agent");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test-access-token");
        verify(portalSettingsService).hasPortalAccess(1L);
        verify(portalSettingsService).isActivated(1L);
        verify(authSessionService).createSession(eq(1L), anyString(), any(), eq("127.0.0.1"), eq("test-agent"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email is missing")
    void shouldThrowExceptionWhenEmailIsMissing() {
        // Arrange
        PortalLoginRequest request = new PortalLoginRequest();
        request.setPassword("password123");

        // Act & Assert
        assertThatThrownBy(() -> clientPortalService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email and password are required");

        verify(authIdentityService, never()).getByNormalisedLogin(anyString());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when client not found")
    void shouldThrowExceptionWhenClientNotFound() {
        // Arrange: identity exists but no client linked
        PortalLoginRequest request = new PortalLoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        AuthIdentity identity = AuthIdentity.builder()
                .id(99L)
                .loginIdentifier("nonexistent@example.com")
                .normalisedLoginIdentifier("nonexistent@example.com")
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .build();
        when(authIdentityService.getByNormalisedLogin(anyString())).thenReturn(identity);
        when(clientRepository.findByAuthId(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> clientPortalService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should reject login when portal access is disabled")
    void shouldRejectLoginWhenPortalAccessIsDisabled() {
        PortalLoginRequest request = new PortalLoginRequest();
        request.setEmail("client@example.com");
        request.setPassword("password123");

        AuthIdentity identity = AuthIdentity.builder()
                .id(1L)
                .loginIdentifier("client@example.com")
                .normalisedLoginIdentifier("client@example.com")
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .build();
        when(authIdentityService.getByNormalisedLogin(anyString())).thenReturn(identity);
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("client@example.com"), eq(IdentityType.CLIENT), eq(1L)))
                .thenReturn(List.of(identity));
        when(clientRepository.findByAuthId(1L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(false);

        assertThatThrownBy(() -> clientPortalService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(PortalAccessDisabledException.class)
                .hasMessage(ClientPortalSettingsService.PORTAL_ACCESS_DISABLED_MESSAGE);
    }

    @Test
    @DisplayName("Should reject portal API access when portal access is disabled")
    void shouldRejectPortalApiAccessWhenPortalAccessIsDisabled() {
        AuthPrincipal authPrincipal = TestDataFactory.createAuthPrincipalForClient(1L);
        Authentication authentication = new UsernamePasswordAuthenticationToken(authPrincipal, null, authPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class)))
                .thenThrow(new PortalAccessDisabledException());

        assertThatThrownBy(() -> clientPortalService.getCurrentClient())
                .isInstanceOf(PortalAccessDisabledException.class)
                .hasMessage(ClientPortalSettingsService.PORTAL_ACCESS_DISABLED_MESSAGE);

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when password is incorrect")
    void shouldThrowExceptionWhenPasswordIsIncorrect() {
        // Arrange
        PortalLoginRequest request = new PortalLoginRequest();
        request.setEmail("client@example.com");
        request.setPassword("wrong-password");

        AuthIdentity identity = AuthIdentity.builder()
                .id(1L)
                .loginIdentifier("client@example.com")
                .normalisedLoginIdentifier("client@example.com")
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .build();
        when(authIdentityService.getByNormalisedLogin(anyString())).thenReturn(identity);
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                eq("client@example.com"), eq(IdentityType.CLIENT), eq(1L)))
                .thenReturn(List.of(identity));
        when(clientRepository.findByAuthId(1L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(true);
        when(portalSettingsService.isActivated(1L)).thenReturn(true);
        when(authIdentityService.isLocked(identity)).thenReturn(false);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(authIdentityService.validatePassword(identity, "wrong-password")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> clientPortalService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should get current client successfully with valid session")
    void shouldGetCurrentClientSuccessfullyWithValidSession() {
        // Arrange: AuthPrincipal (CLIENT) + CurrentUserService returns client
        AuthPrincipal authPrincipal = TestDataFactory.createAuthPrincipalForClient(1L);
        Authentication authentication = new UsernamePasswordAuthenticationToken(authPrincipal, null, authPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(client.getId())).thenReturn(Optional.of(client));

        // Act
        PortalClientResponse response = clientPortalService.getCurrentClient();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(client.getId());
        assertThat(response.getFullName()).isEqualTo(client.getFullName());
        verify(currentUserService).requireCurrentClient(any(AuthPrincipal.class));
        verify(clientRepository).findByIdWithTherapist(client.getId());

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when not authenticated")
    void shouldThrowExceptionWhenNotAuthenticated() {
        // Arrange - clear security context
        SecurityContextHolder.clearContext();

        // Act & Assert
        assertThatThrownBy(() -> clientPortalService.getCurrentClient())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> clientPortalService.login(null, "127.0.0.1", "test-agent"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Login request is required");
    }

    @Mock private com.smart.therapy.flow.system.service.SystemOptionResolverService systemOptionResolverService;
    @Mock private com.smart.therapy.flow.common.service.TimezoneService timezoneService;
    @Mock private com.smart.therapy.flow.subscription.service.SubscriptionFeatureService subscriptionFeatureService;
    @Mock private com.smart.therapy.flow.notification.service.NotificationService notificationService;

    /** Tomorrow at 10:00 UTC — fixed so it always lands inside the availability row below. */
    private static final Instant BOOKABLE_START = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
            .plusDays(1).atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);

    private com.smart.therapy.flow.user.entity.UserProfile therapistProfileAvailableForBooking() {
        var profile = new com.smart.therapy.flow.user.entity.UserProfile();
        profile.getWorkingHours().add(com.smart.therapy.flow.user.entity.UserProfileWorkingHours.builder()
                .day(BOOKABLE_START.atZone(java.time.ZoneOffset.UTC).getDayOfWeek().name())
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(17, 0))
                .sessionMode(com.smart.therapy.flow.user.entity.ShiftMode.BOTH)
                .build());
        return profile;
    }

    @Test
    void bookingSavesAppointmentAndDefersNotificationUntilCommit() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                clientPortalService, "notificationService", notificationService);
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        client.setAssignedTherapist(TestDataFactory.createTestTherapist());
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        var service = new com.smart.therapy.flow.billing.entity.Service();
        service.setId(5L);
        service.setServiceName("Test assessment");
        service.setDuration(60);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(userProfileRepository.findByUserIdWithRooms(any())).thenReturn(
                Optional.of(therapistProfileAvailableForBooking()));
        when(timezoneService.getTherapistTimezone(any()))
                .thenReturn(Optional.of(java.time.ZoneId.of("UTC")));
        when(systemOptionResolverService.requireOptionKey(anyString(), eq("online"))).thenReturn("online");
        when(timezoneService.resolveClientPortalZone(1L, null)).thenReturn(java.time.ZoneId.of("Asia/Karachi"));
        when(sessionRepositoryForAppointments.save(any())).thenAnswer(invocation -> {
            com.smart.therapy.flow.session.entity.Session session = invocation.getArgument(0);
            session.setId(123L);
            return session;
        });
        client.setAuthIdentity(AuthIdentity.builder().loginIdentifier("booking-client@example.com").email("booking-client@example.com").build());
        var zoom = mock(com.smart.therapy.flow.session.service.SessionService.ZoomService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(clientPortalService, "zoomService", zoom);
        when(zoom.isTherapistConfigured(any())).thenReturn(true);
        var meeting = mock(com.smart.therapy.flow.session.dto.ZoomMeetingResponse.class);
        when(meeting.getJoinUrl()).thenReturn("https://zoom.us/j/123456789");
        when(meeting.getPassword()).thenReturn("test-passcode");
        when(zoom.createMeeting(any(), any())).thenReturn(meeting);
        var request = new BookAppointmentRequest();
        request.setServiceId(5L);
        request.setSessionType("online");
        request.setSessionStartUtc(BOOKABLE_START.toString());

        var response = clientPortalService.bookAppointment(request, "127.0.0.1", "unit-test");

        assertThat(response.getAppointment().getId()).isEqualTo(123L);
        verify(emailService, never()).sendSessionConfirmationEmail(any(com.smart.therapy.flow.client.entity.Client.class), any(), any());
        verify(emailService, never()).sendSessionConfirmationEmail(any(com.smart.therapy.flow.auth.entity.User.class), any(), any());
        verify(notificationService).processEventInNewTransaction(eq("session_scheduled"), argThat(data ->
                String.valueOf(data.get("sessionDetailsHtml")).contains("Test assessment")
                && String.valueOf(data.get("sessionDetailsHtml")).contains("Asia/Karachi")
                && String.valueOf(data.get("calendarUrl")).startsWith("https://calendar.google.com/")
                && data.get("sessionUrl") != null
                && String.valueOf(data.get("sessionDetailsHtml")).contains("https://zoom.us/j/123456789")
                && String.valueOf(data.get("sessionDetailsHtml")).contains("test-passcode")));
        verify(notificationService, never()).processEvent(any(), any());
    }

    @Test
    void onlineBookingRequestNotifiesTherapistOncePerCooldown() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                clientPortalService, "notificationService", notificationService);
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        var therapist = TestDataFactory.createTestTherapist();
        therapist.setEmail("therapist@example.com");
        client.setAssignedTherapist(therapist);
        client.setClientId("CL-2024-0417");
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));

        var settings = new com.smart.therapy.flow.client.entity.ClientPortalSettings();
        when(portalSettingsService.getOrCreate(1L)).thenReturn(settings);

        var first = clientPortalService.requestOnlineBooking("127.0.0.1", "unit-test");

        assertThat(first.getOnlineBookingAvailable()).isFalse();
        assertThat(first.getRequestedAt()).isNotNull();
        assertThat(first.getNextRequestAllowedAt()).isEqualTo(first.getRequestedAt().plus(java.time.Duration.ofDays(7)));
        assertThat(settings.getOnlineBookingRequestedAt()).isEqualTo(first.getRequestedAt());

        // MRN only - the therapist alert must never carry the client's name or email.
        verify(notificationService).createSystemNotification(eq(therapist),
                eq(com.smart.therapy.flow.notification.enums.NotificationType.ONLINE_BOOKING_REQUESTED),
                anyString(),
                argThat(message -> message.contains("CL-2024-0417")
                        && !message.contains(client.getFullName())),
                any(), any());
        // The email is the digest job's, not this request's.
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());

        var second = clientPortalService.requestOnlineBooking("127.0.0.1", "unit-test");

        assertThat(second.getRequestedAt()).isEqualTo(first.getRequestedAt());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onlineBookingRequestIsANoOpWhenTherapistAlreadyHasZoom() {
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        client.setAssignedTherapist(TestDataFactory.createTestTherapist());
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));
        var zoom = mock(com.smart.therapy.flow.session.service.SessionService.ZoomService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(clientPortalService, "zoomService", zoom);
        when(zoom.isTherapistConfigured(any())).thenReturn(true);

        var response = clientPortalService.requestOnlineBooking("127.0.0.1", "unit-test");

        assertThat(response.getOnlineBookingAvailable()).isTrue();
        assertThat(response.getRequestedAt()).isNull();
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(portalSettingsService, never()).save(any());
    }

    @Test
    void bookingRejectsOnlineSessionWhenTherapistZoomIsNotConfigured() {
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        client.setAssignedTherapist(TestDataFactory.createTestTherapist());
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));
        var service = new com.smart.therapy.flow.billing.entity.Service();
        service.setId(5L);
        service.setServiceName("Test assessment");
        service.setDuration(60);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(userProfileRepository.findByUserIdWithRooms(any())).thenReturn(
                Optional.of(therapistProfileAvailableForBooking()));
        when(timezoneService.getTherapistTimezone(any()))
                .thenReturn(Optional.of(java.time.ZoneId.of("UTC")));
        when(systemOptionResolverService.requireOptionKey(anyString(), eq("online"))).thenReturn("online");

        var request = new BookAppointmentRequest();
        request.setServiceId(5L);
        request.setSessionType("online");
        request.setSessionStartUtc(BOOKABLE_START.toString());

        assertThatThrownBy(() -> clientPortalService.bookAppointment(request, "127.0.0.1", "unit-test"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class)
                .hasMessageContaining("video meetings");

        verify(sessionRepositoryForAppointments, never()).save(any());
        verify(subscriptionFeatureService, never())
                .consumeUsageOrThrow(any(), any(), any(), anyLong(), any());
    }

    @Test
    void bookingWithARequestOffsetKeepsTheClientsOwnTimezone() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                clientPortalService, "notificationService", notificationService);
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        client.setAssignedTherapist(TestDataFactory.createTestTherapist());
        client.setTimezone("America/New_York");
        client.setAuthIdentity(AuthIdentity.builder()
                .loginIdentifier("booking-client@example.com").email("booking-client@example.com").build());
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        var service = new com.smart.therapy.flow.billing.entity.Service();
        service.setId(5L);
        service.setServiceName("Test assessment");
        service.setDuration(60);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(userProfileRepository.findByUserIdWithRooms(any())).thenReturn(
                Optional.of(therapistProfileAvailableForBooking()));
        when(timezoneService.getTherapistTimezone(any()))
                .thenReturn(Optional.of(java.time.ZoneId.of("UTC")));
        when(timezoneService.resolveClientPortalZone(1L, null))
                .thenReturn(java.time.ZoneId.of("America/New_York"));
        when(systemOptionResolverService.requireOptionKey(anyString(), eq("online"))).thenReturn("online");
        var zoom = mock(com.smart.therapy.flow.session.service.SessionService.ZoomService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(clientPortalService, "zoomService", zoom);
        when(zoom.isTherapistConfigured(any())).thenReturn(true);
        var meeting = mock(com.smart.therapy.flow.session.dto.ZoomMeetingResponse.class);
        when(meeting.getJoinUrl()).thenReturn("https://zoom.us/j/987654321");
        when(zoom.createMeeting(any(), any())).thenReturn(meeting);
        when(sessionRepositoryForAppointments.save(any())).thenAnswer(invocation -> {
            com.smart.therapy.flow.session.entity.Session session = invocation.getArgument(0);
            session.setId(321L);
            return session;
        });

        // Same instant as BOOKABLE_START, but expressed as Karachi wall time (+05:00).
        var karachiOffset = java.time.ZoneOffset.ofHours(5);
        var request = new BookAppointmentRequest();
        request.setServiceId(5L);
        request.setSessionType("online");
        request.setSessionStartUtc(BOOKABLE_START.atOffset(karachiOffset).toString());

        var response = clientPortalService.bookAppointment(request, "127.0.0.1", "unit-test");

        // The offset still pins the correct instant.
        var savedSession = org.mockito.ArgumentCaptor
                .forClass(com.smart.therapy.flow.session.entity.Session.class);
        verify(sessionRepositoryForAppointments).save(savedSession.capture());
        assertThat(savedSession.getValue().getSessionDate()).isEqualTo(BOOKABLE_START);

        // ...but it must not become the client's timezone.
        assertThat(client.getTimezone()).isEqualTo("America/New_York");
        verify(clientRepository, never()).saveAndFlush(any());

        // The confirmation is rendered in the stored timezone, not the request's.
        String newYorkTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                .withZone(java.time.ZoneId.of("America/New_York")).format(BOOKABLE_START);
        String karachiTime = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                .withZone(java.time.ZoneId.of("Asia/Karachi")).format(BOOKABLE_START);
        assertThat(response.getAppointment().getSessionTime()).isEqualTo(newYorkTime);
        assertThat(response.getAppointment().getSessionTime()).isNotEqualTo(karachiTime);
    }

    @Test
    void bookingRejectsTimeOutsideTherapistAvailability() {
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        client.setAssignedTherapist(TestDataFactory.createTestTherapist());
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));
        var service = new com.smart.therapy.flow.billing.entity.Service();
        service.setId(5L);
        service.setServiceName("Test assessment");
        service.setDuration(60);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(userProfileRepository.findByUserIdWithRooms(any())).thenReturn(
                Optional.of(therapistProfileAvailableForBooking()));
        when(timezoneService.getTherapistTimezone(any()))
                .thenReturn(Optional.of(java.time.ZoneId.of("UTC")));
        when(systemOptionResolverService.requireOptionKey(anyString(), eq("online"))).thenReturn("online");

        var request = new BookAppointmentRequest();
        request.setServiceId(5L);
        request.setSessionType("online");
        // 20:00 UTC sits well outside the 09:00-17:00 availability row.
        request.setSessionStartUtc(BOOKABLE_START.plus(10, java.time.temporal.ChronoUnit.HOURS).toString());

        assertThatThrownBy(() -> clientPortalService.bookAppointment(request, "127.0.0.1", "unit-test"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class)
                .hasMessageContaining("outside your therapist's availability");

        verify(sessionRepositoryForAppointments, never()).save(any());
    }

    @Test
    void bookingRejectsConsultationOnlyHoursForAClinicalService() {
        var principal = TestDataFactory.createAuthPrincipalForClient(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        client.setAssignedTherapist(TestDataFactory.createTestTherapist());
        when(currentUserService.requireCurrentClient(any(AuthPrincipal.class))).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(1L)).thenReturn(Optional.of(client));
        var service = new com.smart.therapy.flow.billing.entity.Service();
        service.setId(5L);
        service.setServiceName("Test assessment");
        service.setDuration(60);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));

        // Availability exists at the requested time, but only on the consultation /
        // public-site schedule, which clinical portal booking must never use.
        var consultation = new com.smart.therapy.flow.billing.entity.Service();
        consultation.setId(99L);
        consultation.setServiceCode("CONSULTATION");
        var profile = new com.smart.therapy.flow.user.entity.UserProfile();
        profile.getWorkingHours().add(com.smart.therapy.flow.user.entity.UserProfileWorkingHours.builder()
                .service(consultation)
                .day(BOOKABLE_START.atZone(java.time.ZoneOffset.UTC).getDayOfWeek().name())
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(17, 0))
                .sessionMode(com.smart.therapy.flow.user.entity.ShiftMode.BOTH)
                .build());
        when(userProfileRepository.findByUserIdWithRooms(any())).thenReturn(Optional.of(profile));
        when(timezoneService.getTherapistTimezone(any()))
                .thenReturn(Optional.of(java.time.ZoneId.of("UTC")));
        when(systemOptionResolverService.requireOptionKey(anyString(), eq("online"))).thenReturn("online");

        var request = new BookAppointmentRequest();
        request.setServiceId(5L);
        request.setSessionType("online");
        request.setSessionStartUtc(BOOKABLE_START.toString());

        assertThatThrownBy(() -> clientPortalService.bookAppointment(request, "127.0.0.1", "unit-test"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class)
                .hasMessageContaining("outside your therapist's availability");

        verify(sessionRepositoryForAppointments, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void publicPortalLoginBindsClinicBeforeMfaChallenge(boolean enrolled) {
        TenantContext.setSchemaName("public");
        TenantContext.setOrganisationId(null);
        var request = new PortalLoginRequest();
        request.setEmail("client@example.com");
        request.setPassword("password123");
        request.setOrgId("1");
        when(tenantDirectoryService.findByOrganisationIdOrSlug("1")).thenReturn(Optional.of(
                new TenantDirectoryService.TenantInfo(1L, "tenant_test", "test", "ACTIVE", null)));
        var identity = AuthIdentity.builder().id(1L).identityType(IdentityType.CLIENT)
                .loginIdentifier("client@example.com").isActive(true)
                .organisation(Organisation.builder().id(1L).build()).build();
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "client@example.com", IdentityType.CLIENT, 1L)).thenReturn(List.of(identity));
        when(tenantDirectoryService.findByOrganisationId(1L)).thenReturn(Optional.of(
                new TenantDirectoryService.TenantInfo(1L, "tenant_test", "test", "ACTIVE", null)));
        when(tenantTransactionExecutor.executeReadOnly(eq(1L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get());
        when(clientRepository.findByAuthId(1L)).thenReturn(Optional.of(client));
        when(portalSettingsService.hasPortalAccess(1L)).thenReturn(true);
        when(portalSettingsService.isActivated(1L)).thenReturn(true);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(authIdentityService.validatePassword(identity, "password123")).thenReturn(true);
        when(authIdentityDetailsService.loadUserByAuthId(1L)).thenAnswer(invocation -> {
            assertThat(TenantContext.getSchemaName()).isEqualTo("tenant_test");
            assertThat(TenantContext.getOrganisationId()).isEqualTo(1L);
            return TestDataFactory.createAuthPrincipalForClient(1L);
        });
        when(mfaService.isEnabled(1L)).thenReturn(enrolled);
        when(mfaService.isEnrollmentRequired(any(), eq(enrolled))).thenReturn(!enrolled);
        when(mfaService.getLoginChallengeInfo(1L)).thenReturn(
                new com.smart.therapy.flow.auth.dto.MfaDtos.LoginChallengeInfo(
                        com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL, "masked", false, true, List.of("EMAIL")));
        org.mockito.stubbing.Answer<String> clinicChallenge = invocation -> {
            assertThat(TenantContext.getSchemaName()).isEqualTo("tenant_test");
            assertThat(TenantContext.getOrganisationId()).isEqualTo(1L);
            return "clinic-bound-challenge";
        };
        when(mfaService.createEnrollmentChallenge(1L)).thenAnswer(clinicChallenge);
        when(mfaService.createLoginChallenge(identity)).thenAnswer(clinicChallenge);

        var response = clientPortalService.login(request, "127.0.0.1", "unit-test");
        assertThat(response.getMfaChallengeToken()).isEqualTo("clinic-bound-challenge");
        assertThat(TenantContext.getSchemaName()).isEqualTo("public");
        assertThat(TenantContext.getOrganisationId()).isNull();
    }
}
