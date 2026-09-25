package com.smart.therapy.flow.unit.security;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private AuthPrincipal userPrincipal;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Set up JWT secret and expiration
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "test-secret-key-that-is-at-least-256-bits-long-for-HS512-algorithm");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", 3600000L); // 1 hour

        testUser = TestDataFactory.createTestUser();
        testUser.setId(1L);
        userPrincipal = TestDataFactory.createAuthPrincipal(testUser);
    }

    @Test
    @DisplayName("Should issue distinct refresh tokens within the same family")
    void shouldIssueDistinctRefreshTokensWithinFamily() {
        String first = jwtTokenProvider.generateRefreshToken(1L, "fixture-family", 60000L);
        String second = jwtTokenProvider.generateRefreshToken(1L, "fixture-family", 60000L);

        assertThat(jwtTokenProvider.getJtiFromToken(first)).isNotBlank();
        assertThat(jwtTokenProvider.getJtiFromToken(second)).isNotBlank()
                .isNotEqualTo(jwtTokenProvider.getJtiFromToken(first));
        assertThat(second).isNotEqualTo(first);
        for (String token : java.util.List.of(first, second)) {
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
            assertThat(jwtTokenProvider.getAuthIdFromToken(token)).isEqualTo(1L);
            assertThat(jwtTokenProvider.getRefreshFamilyIdFromToken(token)).isEqualTo("fixture-family");
        }
    }

    private Authentication createAuthentication() {
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    @DisplayName("Should generate token successfully")
    void shouldGenerateTokenSuccessfully() {
        // Arrange
        Authentication authentication = createAuthentication();
        
        // Act
        String token = jwtTokenProvider.generateToken(authentication);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // Arrange
        Authentication authentication = createAuthentication();
        String token = jwtTokenProvider.generateToken(authentication);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Arrange
        Authentication authentication = createAuthentication();
        String token = jwtTokenProvider.generateToken(authentication);

        // Act
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertThat(userId).isNotNull();
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        // Arrange
        Authentication authentication = createAuthentication();
        String token = jwtTokenProvider.generateToken(authentication);

        // Act
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertThat(username).isNotNull();
        assertThat(username).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for null token")
    void shouldReturnFalseForNullToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty token")
    void shouldReturnFalseForEmptyToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should create only the MFA challenge token type")
    void shouldCreateSinglePurposeMfaChallenge() {
        String token = jwtTokenProvider.generateMfaChallengeToken(1L, 60);

        assertThat(jwtTokenProvider.isMfaChallengeToken(token)).isTrue();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("mfa_challenge");
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getAuthIdFromToken(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should create upload-bound transcription WebSocket ticket")
    void shouldCreateUploadBoundTranscriptionWebSocketTicket() {
        String ticket = jwtTokenProvider.generateTranscriptionWebSocketTicket(
                userPrincipal, "srv-upload-123", 60);

        assertThat(jwtTokenProvider.validateTranscriptionWebSocketTicket(ticket, "srv-upload-123")).isTrue();
        assertThat(jwtTokenProvider.validateTranscriptionWebSocketTicket(ticket, "srv-different-upload")).isFalse();
        assertThat(jwtTokenProvider.getTokenType(ticket)).isEqualTo("transcription_ws");
        assertThat(jwtTokenProvider.getAuthIdFromToken(ticket)).isEqualTo(userPrincipal.getAuthId());
    }

    @Test
    @DisplayName("Should reject an access token as a transcription WebSocket ticket")
    void shouldRejectAccessTokenAsTranscriptionWebSocketTicket() {
        String accessToken = jwtTokenProvider.generateToken(createAuthentication());

        assertThat(jwtTokenProvider.validateTranscriptionWebSocketTicket(accessToken, "srv-upload-123")).isFalse();
    }

    @Test
    @DisplayName("Should reject expired MFA challenge")
    void shouldRejectExpiredMfaChallenge() {
        String token = jwtTokenProvider.generateMfaChallengeToken(1L, -1);
        assertThat(jwtTokenProvider.isMfaChallengeToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should distinguish enrollment challenge from login challenge")
    void shouldCreateSinglePurposeEnrollmentChallenge() {
        String token = jwtTokenProvider.generateMfaEnrollmentToken(1L, 60);
        assertThat(jwtTokenProvider.isMfaEnrollmentToken(token)).isTrue();
        assertThat(jwtTokenProvider.isMfaChallengeToken(token)).isFalse();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("mfa_enrollment");
    }
}
