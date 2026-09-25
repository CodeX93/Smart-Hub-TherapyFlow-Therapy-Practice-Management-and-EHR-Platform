package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.auth.dto.RefreshTokenRequest;
import com.smart.therapy.flow.auth.dto.ChangePasswordRequest;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.LoginAttempt;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.auth.repository.LoginAttemptRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.MfaService;
import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private AuthIdentityService authIdentityService;

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private AuthIdentityDetailsService authIdentityDetailsService;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private TenantDirectoryService tenantDirectoryService;

    @Mock
    private PlatformTenantRoutingService platformTenantRoutingService;

    @Mock
    private TenantResolutionService tenantResolutionService;

    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Mock
    private EntityManager entityManager;

    @Mock
    private MfaService mfaService;

    @Mock
    private AuthRefreshTokenService authRefreshTokenService;

    @Mock
    private com.smart.therapy.flow.auth.service.AuthKnownDeviceService authKnownDeviceService;

    @Mock
    private com.smart.therapy.flow.audit.service.AuditLogService auditLogService;

    @Mock
    private com.smart.therapy.flow.common.metrics.AuthAbuseMetrics authAbuseMetrics;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private AuthPrincipal userPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestTherapist();
        testUser.setId(1L);
        userPrincipal = TestDataFactory.createAuthPrincipal(testUser);

        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        ReflectionTestUtils.setField(authenticationService, "entityManager", entityManager);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should authenticate user successfully with valid credentials")
    void shouldAuthenticateUserSuccessfully() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("therapist@example.com");
        loginRequest.setPassword("password");
        String ipAddress = "127.0.0.1";

        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class), anyString())).thenReturn(accessToken);
        when(authRefreshTokenService.issueRefreshToken(eq(1L), eq(ipAddress), isNull())).thenReturn(refreshToken);
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(new LoginAttempt());

        // Act
        JwtAuthenticationResponse response = authenticationService.authenticateUser(loginRequest, ipAddress);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        // Public-schema login does not resolve tenant users.id
        assertThat(response.getUserId()).isNull();
        assertThat(response.getUsername()).isEqualTo("therapist@example.com");
        assertThat(response.getTenantSchema()).isEqualTo("public");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(any(Authentication.class), anyString());
        verify(authRefreshTokenService).issueRefreshToken(eq(1L), eq(ipAddress), isNull());
        verify(authIdentityService).recordSuccessfulLogin(1L);
        verify(authSessionService).createSession(anyLong(), anyString(), any(), any(), any());
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    @DisplayName("Should issue only an MFA challenge when MFA is enabled")
    void shouldNotIssueJwtBeforeMfaVerification() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("therapist@example.com");
        loginRequest.setPassword("password");
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("therapist@example.com", "hash");
        identity.setId(1L);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(mfaService.isEnabled(1L)).thenReturn(true);
        when(mfaService.createLoginChallenge(identity)).thenReturn("mfa-challenge");
        when(mfaService.getLoginChallengeInfo(1L)).thenReturn(
                new MfaDtos.LoginChallengeInfo(MfaMethod.TOTP, null, true, true, List.of("TOTP")));

        JwtAuthenticationResponse response =
                authenticationService.authenticateUser(loginRequest, "127.0.0.1");

        assertThat(response.getMfaRequired()).isTrue();
        assertThat(response.getMfaChallengeToken()).isEqualTo("mfa-challenge");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        verify(tokenProvider, never()).generateToken(any(), anyString());
        verify(authRefreshTokenService, never()).issueRefreshToken(anyLong(), anyString(), any());
        verify(authSessionService, never()).createSession(anyLong(), anyString(), any(), any(), any());
        verify(authIdentityService, never()).recordSuccessfulLogin(anyLong());
    }

    @Test
    @DisplayName("Should skip MFA when a valid trusted device token is presented")
    void shouldSkipMfaForTrustedDevice() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("therapist@example.com");
        loginRequest.setPassword("password");
        loginRequest.setDeviceTrustToken("trust-token");
        loginRequest.setStaySignedIn(true);
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("therapist@example.com", "hash");
        identity.setId(1L);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(mfaService.isEnabled(1L)).thenReturn(true);
        when(authKnownDeviceService.isTrustedDevice(1L, "trust-token")).thenReturn(true);
        when(authKnownDeviceService.staySignedInRefreshExpirationMs()).thenReturn(30L * 24 * 60 * 60 * 1000);
        when(tokenProvider.generateToken(any(Authentication.class), anyString())).thenReturn("access");
        when(authRefreshTokenService.issueRefreshToken(eq(1L), eq("127.0.0.1"), eq("Chrome"), anyLong()))
                .thenReturn("refresh");
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(new LoginAttempt());

        JwtAuthenticationResponse response =
                authenticationService.authenticateUser(loginRequest, "127.0.0.1", "Chrome");

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getMfaRequired()).isFalse();
        assertThat(response.getMfaSkippedTrustedDevice()).isTrue();
        assertThat(response.getStaySignedIn()).isTrue();
        verify(mfaService, never()).createLoginChallenge(any());
        verify(authKnownDeviceService).touchTrustedDevice(eq(1L), eq("trust-token"), eq("127.0.0.1"), eq("Chrome"));
        verify(authRefreshTokenService).issueRefreshToken(eq(1L), eq("127.0.0.1"), eq("Chrome"), anyLong());
    }

    @Test
    @DisplayName("Should not issue JWT when enforced MFA enrollment is incomplete")
    void shouldNotIssueJwtBeforeRequiredEnrollment() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@example.com");
        request.setPassword("password");
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("admin@example.com", "hash");
        identity.setId(1L);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(mfaService.isEnabled(1L)).thenReturn(false);
        when(mfaService.isEnrollmentRequired(any(), eq(false))).thenReturn(true);
        when(mfaService.createEnrollmentChallenge(1L)).thenReturn("enrollment-challenge");
        when(mfaService.getLoginChallengeInfo(1L)).thenReturn(
                new MfaDtos.LoginChallengeInfo(MfaMethod.TOTP, null, true, true, List.of()));

        JwtAuthenticationResponse response =
                authenticationService.authenticateUser(request, "127.0.0.1");

        assertThat(response.getMfaEnrollmentRequired()).isTrue();
        assertThat(response.getMfaChallengeToken()).isEqualTo("enrollment-challenge");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        verify(tokenProvider, never()).generateToken(any(), anyString());
        verify(authSessionService, never()).createSession(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when credentials are invalid")
    void shouldThrowExceptionWhenCredentialsInvalid() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("therapist@example.com");
        loginRequest.setPassword("wrong-password");
        String ipAddress = "127.0.0.1";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(new LoginAttempt());

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.authenticateUser(loginRequest, ipAddress))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider, never()).generateToken(any(), anyString());
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    @DisplayName("Should record failed staff credentials against lockout fields")
    void shouldRecordFailedStaffLogin() {
        LoginRequest request = new LoginRequest();
        request.setUsername("staff@example.com");
        request.setPassword("wrong");
        AuthIdentity staff = AuthIdentity.builder()
                .id(77L)
                .loginIdentifier("staff@example.com")
                .normalisedLoginIdentifier("staff@example.com")
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .build();
        when(authIdentityRepository.findAllByEmailOrUsername("staff@example.com"))
                .thenReturn(List.of(staff));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authenticationService.authenticateUser(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);

        verify(authIdentityService).recordFailedLogin(77L);
    }

    @Test
    @DisplayName("Should apply staff lockout when login uses tenant profile email fallback")
    void shouldRecordFailedStaffLoginResolvedByProfileEmail() {
        TenantContext.setSchemaName("tenant_1");
        TenantContext.setOrganisationId(1L);
        LoginRequest request = new LoginRequest();
        request.setUsername("profile-email@example.com");
        request.setPassword("wrong");
        AuthIdentity staff = AuthIdentity.builder()
                .id(1L)
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .build();
        when(authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                "profile-email@example.com", com.smart.therapy.flow.auth.entity.IdentityType.STAFF, 1L))
                .thenReturn(List.of());
        when(authIdentityDetailsService.loadUserByUsername("profile-email@example.com")).thenReturn(userPrincipal);
        when(authIdentityService.getById(1L)).thenReturn(staff);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authenticationService.authenticateUser(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);

        verify(authIdentityService).recordFailedLogin(1L);
    }

    @Test
    @DisplayName("Should reject locked staff before checking credentials")
    void shouldRejectLockedStaffBeforeAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setUsername("locked@example.com");
        request.setPassword("password");
        AuthIdentity staff = AuthIdentity.builder()
                .id(78L)
                .loginIdentifier("locked@example.com")
                .normalisedLoginIdentifier("locked@example.com")
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .accountLocked(true)
                .lockedUntil(Instant.now().plusSeconds(60))
                .build();
        when(authIdentityRepository.findAllByEmailOrUsername("locked@example.com"))
                .thenReturn(List.of(staff));
        when(authIdentityService.isLocked(staff)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.authenticateUser(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("temporarily locked");

        verify(authenticationManager, never()).authenticate(any());
        verify(authIdentityService, never()).recordFailedLogin(anyLong());
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void shouldRefreshTokenSuccessfully() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("valid-refresh-token");

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("valid-refresh-token")).thenReturn(1L);
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("therapist@example.com", "hash");
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(authIdentityDetailsService.loadUserByAuthId(1L)).thenReturn(userPrincipal);
        when(tokenProvider.generateToken(any(Authentication.class), anyString())).thenReturn(newAccessToken);
        when(authRefreshTokenService.rotateRefreshToken(eq("valid-refresh-token"), isNull(), isNull()))
                .thenReturn(newRefreshToken);

        // Act
        JwtAuthenticationResponse response = authenticationService.refreshToken(refreshTokenRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);

        verify(tokenProvider).validateToken("valid-refresh-token");
        verify(tokenProvider).getAuthIdFromToken("valid-refresh-token");
        verify(tokenProvider).generateToken(any(Authentication.class), anyString());
        verify(authRefreshTokenService).rotateRefreshToken(eq("valid-refresh-token"), isNull(), isNull());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when refresh token is invalid")
    void shouldThrowExceptionWhenRefreshTokenInvalid() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("invalid-refresh-token");

        when(tokenProvider.validateToken("invalid-refresh-token")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.refreshToken(refreshTokenRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(tokenProvider).validateToken("invalid-refresh-token");
        verify(tokenProvider, never()).getUserIdFromToken(anyString());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user not found during refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("valid-refresh-token");

        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("valid-refresh-token")).thenReturn(999L);
        when(authIdentityService.getById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.refreshToken(refreshTokenRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User account is disabled");

        verify(tokenProvider).validateToken("valid-refresh-token");
        verify(tokenProvider).getAuthIdFromToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Should reject refresh token issued before password reset")
    void shouldRejectRefreshTokenIssuedBeforePasswordReset() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh-token");
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("staff@example.com", "hash");
        identity.setPasswordChangedAt(Instant.now());

        when(tokenProvider.validateToken("old-refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("old-refresh-token")).thenReturn(1L);
        when(tokenProvider.getIssuedAtFromToken("old-refresh-token"))
                .thenReturn(Date.from(Instant.now().minusSeconds(30)));
        when(authIdentityService.getById(1L)).thenReturn(identity);

        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("predates");

        verify(authSessionService, never()).createSession(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject refresh token issued before MFA enrollment")
    void shouldRejectRefreshTokenIssuedBeforeMfaEnrollment() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("pre-mfa-refresh-token");
        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("staff@example.com", "hash");
        Date issuedAt = Date.from(Instant.now().minusSeconds(30));

        when(tokenProvider.validateToken("pre-mfa-refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("pre-mfa-refresh-token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("pre-mfa-refresh-token")).thenReturn(1L);
        when(tokenProvider.getIssuedAtFromToken("pre-mfa-refresh-token")).thenReturn(issuedAt);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(mfaService.isRefreshTokenStale(1L, issuedAt.toInstant())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("MFA enrollment");

        verify(authSessionService, never()).createSession(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should log successful login attempt")
    void shouldLogSuccessfulLoginAttempt() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("therapist@example.com");
        loginRequest.setPassword("password");
        String ipAddress = "127.0.0.1";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class), anyString())).thenReturn("access-token");
        when(authRefreshTokenService.issueRefreshToken(eq(1L), eq(ipAddress), isNull())).thenReturn("refresh-token");
        when(userRepository.findByAuthId(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(new LoginAttempt());

        // Act
        authenticationService.authenticateUser(loginRequest, ipAddress);

        // Assert
        verify(loginAttemptRepository).save(argThat(attempt ->
                attempt.getUsername().equals("therapist@example.com") &&
                attempt.getIpAddress().equals(ipAddress) &&
                attempt.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should log failed login attempt")
    void shouldLogFailedLoginAttempt() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("therapist@example.com");
        loginRequest.setPassword("wrong-password");
        String ipAddress = "127.0.0.1";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(new LoginAttempt());

        // Act
        try {
            authenticationService.authenticateUser(loginRequest, ipAddress);
        } catch (UnauthorizedException e) {
            // Expected
        }

        // Assert
        verify(loginAttemptRepository).save(argThat(attempt ->
                attempt.getUsername().equals("therapist@example.com") &&
                attempt.getIpAddress().equals(ipAddress) &&
                !attempt.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should require password change before login when identity flag is enabled")
    void shouldRequirePasswordChangeBeforeLoginWhenIdentityFlagIsEnabled() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tenant.admin@example.com");
        loginRequest.setPassword("TempPassword123!");
        String ipAddress = "127.0.0.1";

        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("tenant.admin@example.com", "hash");
        identity.setId(1L);
        identity.setMustChangePassword(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(tokenProvider.generatePasswordChangeToken(1L)).thenReturn("pw-change-token");

        JwtAuthenticationResponse response = authenticationService.authenticateUser(loginRequest, ipAddress);

        assertThat(response).isNotNull();
        assertThat(response.getPasswordChangeRequired()).isTrue();
        assertThat(response.getChangePasswordToken()).isEqualTo("pw-change-token");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();

        verify(tokenProvider, never()).generateToken(any(Authentication.class), anyString());
        verify(authRefreshTokenService, never()).issueRefreshToken(anyLong(), anyString(), any());
        verify(authSessionService, never()).createSession(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should change password with password-change token and continue to MFA enrollment")
    void shouldChangePasswordWithTokenWithoutPrincipal() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setChangePasswordToken("password-change-token");
        request.setNewPassword("NewSecurePassword123!");

        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("tenant.admin@example.com", "oldHash");
        identity.setId(1L);
        identity.setMustChangePassword(true);

        AuthPrincipal principal = AuthPrincipal.create(identity, List.of());

        when(tokenProvider.validateToken("password-change-token")).thenReturn(true);
        when(tokenProvider.getTokenType("password-change-token")).thenReturn("password_change");
        when(tokenProvider.getAuthIdFromToken("password-change-token")).thenReturn(1L);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(tokenProvider.validatePasswordChangeToken("password-change-token", 1L)).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("encodedPassword");
        when(tokenProvider.getTenantSchemaFromToken("password-change-token")).thenReturn("public");
        when(tokenProvider.getOrganisationIdFromToken("password-change-token")).thenReturn(null);
        when(authIdentityDetailsService.loadUserByAuthId(1L)).thenReturn(principal);
        when(mfaService.isEnabled(1L)).thenReturn(false);
        when(mfaService.isEnrollmentRequired(any(), eq(false))).thenReturn(true);
        when(mfaService.createEnrollmentChallenge(1L)).thenReturn("enrollment-challenge");
        when(mfaService.getLoginChallengeInfo(1L)).thenReturn(
                new MfaDtos.LoginChallengeInfo(MfaMethod.EMAIL, null, true, true, List.of()));

        JwtAuthenticationResponse response =
                authenticationService.changePassword(request, null, "127.0.0.1", "Test");

        assertThat(response.getStatusSuccess()).isTrue();
        assertThat(response.getMfaEnrollmentRequired()).isTrue();
        assertThat(response.getMfaChallengeToken()).isEqualTo("enrollment-challenge");
        assertThat(response.getMfaEmailAvailable()).isTrue();
        assertThat(response.getAccessToken()).isNull();
        assertThat(identity.getMustChangePassword()).isFalse();
        verify(authIdentityRepository).save(identity);
        verify(tokenBlacklistService).blacklistUserTokens(1L);
        verify(tokenProvider, never()).generateToken(any(Authentication.class), anyString());
    }

    @Test
    @DisplayName("Should revoke all sessions after staff password reset")
    void shouldRevokeAllSessionsAfterStaffPasswordReset() {
        AuthIdentity identity = AuthIdentity.builder()
                .id(91L)
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .passwordResetToken("stored-fingerprint")
                .passwordResetExpiry(Instant.now().plusSeconds(60))
                .build();
        when(authIdentityService.validatePasswordResetToken("reset-token")).thenReturn(identity);
        when(authIdentityService.resetPasswordByToken("reset-token", "NewPassword123!")).thenReturn(identity);
        when(authIdentityService.validatePassword(identity, "NewPassword123!")).thenReturn(true);

        authenticationService.resetPasswordStaff("reset-token", "NewPassword123!");

        verify(tokenBlacklistService).blacklistAllUserTokens(91L);
    }

    @Test
    @DisplayName("Should not send forgot-password email when multiple staff identities match and org is not provided")
    void shouldSkipForgotPasswordWhenAmbiguousWithoutOrganisation() {
        String email = "shared@example.com";
        var identityA = AuthIdentity.builder()
                .id(11L)
                .loginIdentifier(email)
                .normalisedLoginIdentifier(email)
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .build();
        var identityB = AuthIdentity.builder()
                .id(22L)
                .loginIdentifier(email)
                .normalisedLoginIdentifier(email)
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .build();

        when(authIdentityRepository.findAllByNormalisedLoginIdentifierAndIdentityType(
                email, com.smart.therapy.flow.auth.entity.IdentityType.STAFF))
                .thenReturn(List.of(identityA, identityB));

        String hint = authenticationService.requestPasswordResetStaff(email);

        assertThat(hint).isEqualTo("TENANT_SELECTION_REQUIRED");
        verify(emailService, never()).sendStaffPasswordResetEmail(anyString(), anyString(), anyString());
        verify(authIdentityService, never()).setPasswordResetToken(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("Should send forgot-password email for selected organisation when email exists in multiple organisations")
    void shouldSendForgotPasswordForExplicitOrganisation() {
        String email = "shared@example.com";
        var identity = AuthIdentity.builder()
                .id(33L)
                .loginIdentifier(email)
                .normalisedLoginIdentifier(email)
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .build();
        var tenantInfo = new TenantDirectoryService.TenantInfo(7L, "tenant_7", "acme", "ACTIVE", null);
        var user = User.builder()
                .email(email)
                .fullName("Shared User")
                .build();

        when(tenantDirectoryService.findByOrganisationIdOrSlug("acme")).thenReturn(Optional.of(tenantInfo));
        when(authIdentityRepository.findAllByNormalisedLoginIdentifierAndIdentityTypeForOrganisation(
                email, com.smart.therapy.flow.auth.entity.IdentityType.STAFF, 7L))
                .thenReturn(List.of(identity));
        when(authIdentityService.generateSecureToken()).thenReturn("reset-token");
        when(tenantDirectoryService.findByOrganisationId(7L)).thenReturn(Optional.of(tenantInfo));
        when(tenantTransactionExecutor.executeReadOnly(eq(7L), eq("tenant_7"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> work = (Supplier<Object>) invocation.getArgument(2);
                    return work.get();
                });
        when(userRepository.findByAuthId(33L)).thenReturn(Optional.of(user));

        when(tenantResolutionService.emailBelongsToOrganisation(email, 7L)).thenReturn(true);

        String hint = authenticationService.requestPasswordResetStaff(email, null, "acme", null, null);

        assertThat(hint).isNull();
        verify(authIdentityService).setPasswordResetToken(eq(33L), eq("reset-token"), any());
        verify(emailService).sendStaffPasswordResetEmail(eq(email), eq("Shared User"), eq("reset-token"));
    }

    @Test
    @DisplayName("Should send forgot-password email when org selected but login_identifier is not the profile email")
    void shouldSendForgotPasswordViaOrgMembershipWhenLoginIdentifierDiffersFromEmail() {
        String email = "elena.rostova@yopmail.com";
        var identity = AuthIdentity.builder()
                .id(44L)
                .loginIdentifier("elena.therapist")
                .normalisedLoginIdentifier("elena.therapist")
                .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                .isActive(true)
                .build();
        var org = Organisation.builder().id(42L).slug("apex-behavioral-health").build();
        var membership = UserOrganisation.builder().auth(identity).organisation(org).build();
        var tenantInfo = new TenantDirectoryService.TenantInfo(
                42L, "tenant_42", "apex-behavioral-health", "apex-behavioral-health", "ACTIVE", null);
        var user = User.builder().email(email).fullName("Elena Rostova").build();

        when(tenantDirectoryService.findByOrganisationIdOrSlug("42"))
                .thenReturn(Optional.of(tenantInfo));
        when(tenantDirectoryService.findByOrganisationIdOrSlug("apex-behavioral-health"))
                .thenReturn(Optional.of(tenantInfo));
        when(tenantResolutionService.emailBelongsToOrganisation(email, 42L)).thenReturn(true);
        when(authIdentityRepository.findAllByNormalisedLoginIdentifierAndIdentityTypeForOrganisation(
                email, com.smart.therapy.flow.auth.entity.IdentityType.STAFF, 42L))
                .thenReturn(List.of());
        when(tenantResolutionService.resolveByEmail(email, com.smart.therapy.flow.auth.entity.IdentityType.STAFF))
                .thenReturn(List.of(membership));
        when(authIdentityService.generateSecureToken()).thenReturn("reset-token");
        when(tenantDirectoryService.findByOrganisationId(42L)).thenReturn(Optional.of(tenantInfo));
        when(tenantTransactionExecutor.executeReadOnly(eq(42L), eq("tenant_42"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> work = (Supplier<Object>) invocation.getArgument(2);
                    return work.get();
                });
        when(userRepository.findByAuthId(44L)).thenReturn(Optional.of(user));

        String hint = authenticationService.requestPasswordResetStaff(
                email, "42", "apex-behavioral-health", null, null);

        assertThat(hint).isNull();
        verify(authIdentityService).setPasswordResetToken(eq(44L), eq("reset-token"), any());
        verify(emailService).sendStaffPasswordResetEmail(eq(email), eq("Elena Rostova"), eq("reset-token"));
    }

    @Test
    @DisplayName("Should not send forgot-password email when org slug is invalid")
    void shouldSkipForgotPasswordWhenOrgSlugInvalid() {
        when(tenantDirectoryService.findByOrganisationIdOrSlug("missing-org")).thenReturn(Optional.empty());

        String hint = authenticationService.requestPasswordResetStaff(
                "user@example.com", null, "missing-org", null, null);

        assertThat(hint).isNull();
        verify(emailService, never()).sendStaffPasswordResetEmail(anyString(), anyString(), anyString());
        verify(authIdentityRepository, never()).findAllByNormalisedLoginIdentifierAndIdentityType(anyString(), any());
        verify(authIdentityRepository, never()).findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("Staff refresh on public host restores tenant from refresh token claims")
    void shouldRestoreStaffTenantOnRefreshFromPublicHost() {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("staff-refresh-token");

        AuthIdentity identity = TestDataFactory.createTestAuthIdentity("staff@example.com", "hash");
        TenantDirectoryService.TenantInfo info =
                new TenantDirectoryService.TenantInfo(55L, "tenant_55", "acme", "acme", "ACTIVE", null);

        when(tokenProvider.validateToken("staff-refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("staff-refresh-token")).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken("staff-refresh-token")).thenReturn(1L);
        when(tokenProvider.getTenantSchemaFromToken("staff-refresh-token")).thenReturn("tenant_55");
        when(tokenProvider.getOrganisationIdFromToken("staff-refresh-token")).thenReturn(55L);
        when(authIdentityService.getById(1L)).thenReturn(identity);
        when(tenantDirectoryService.findBySchemaName("tenant_55")).thenReturn(Optional.of(info));
        when(tenantDirectoryService.findByOrganisationId(55L)).thenReturn(Optional.of(info));
        when(authIdentityDetailsService.loadUserByAuthId(1L)).thenReturn(userPrincipal);
        when(tokenProvider.generateToken(any(Authentication.class), anyString())).thenReturn("new-access");
        when(authRefreshTokenService.rotateRefreshToken(eq("staff-refresh-token"), isNull(), isNull()))
                .thenReturn("new-refresh");

        JwtAuthenticationResponse response = authenticationService.refreshToken(refreshTokenRequest);

        assertThat(response.getTenantSchema()).isEqualTo("tenant_55");
        assertThat(response.getOrganisationId()).isEqualTo(55L);
        assertThat(response.getOrganisationSlug()).isEqualTo("acme");
        assertThat(response.getAccessToken()).isEqualTo("new-access");
    }
}
