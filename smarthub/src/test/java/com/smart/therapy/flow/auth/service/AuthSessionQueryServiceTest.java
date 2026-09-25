package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.AuthSessionDtos;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthSession;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSessionQueryServiceTest {

    @Mock AuthSessionService authSessionService;
    @Mock JwtTokenProvider tokenProvider;

    private AuthSessionQueryService service;

    @BeforeEach
    void setUp() {
        service = new AuthSessionQueryService(authSessionService, tokenProvider);
    }

    @Test
    void listsActiveSessionsWithCurrentFlagAndCount() {
        AuthIdentity identity = AuthIdentity.builder().id(7L).build();
        AuthSession current = AuthSession.builder()
                .id(1L)
                .authIdentity(identity)
                .jwtId("jti-current")
                .issuedAt(Instant.now().minusSeconds(60))
                .lastActivityAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .idleExpiresAt(Instant.now().plusSeconds(900))
                .ipAddress("1.1.1.1")
                .userAgent("Mozilla/5.0 Chrome/126 Safari/537.36")
                .revoked(false)
                .build();
        AuthSession other = AuthSession.builder()
                .id(2L)
                .authIdentity(identity)
                .jwtId("jti-other")
                .issuedAt(Instant.now().minusSeconds(120))
                .lastActivityAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(3600))
                .idleExpiresAt(Instant.now().plusSeconds(900))
                .ipAddress("2.2.2.2")
                .userAgent("Mozilla/5.0 Firefox/126")
                .revoked(false)
                .build();
        when(tokenProvider.getJtiFromToken("token")).thenReturn("jti-current");
        when(authSessionService.listActiveSessions(7L)).thenReturn(List.of(current, other));

        AuthSessionDtos.SessionListResponse response =
                service.listSessions(7L, "Bearer token");

        assertThat(response.activeCount()).isEqualTo(2);
        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions().get(0).current()).isTrue();
        assertThat(response.sessions().get(1).current()).isFalse();
        assertThat(response.sessions().get(0).deviceLabel()).contains("Chrome");
    }

    @Test
    void refusesToRevokeCurrentSession() {
        AuthIdentity identity = AuthIdentity.builder().id(7L).build();
        AuthSession current = AuthSession.builder()
                .id(1L)
                .authIdentity(identity)
                .jwtId("jti-current")
                .issuedAt(Instant.now())
                .lastActivityAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .idleExpiresAt(Instant.now().plusSeconds(900))
                .revoked(false)
                .build();
        when(tokenProvider.getJtiFromToken("token")).thenReturn("jti-current");
        when(authSessionService.listActiveSessions(7L)).thenReturn(List.of(current));

        assertThatThrownBy(() -> service.revokeSession(7L, 1L, "Bearer token"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class);
        verify(authSessionService, never()).revokeByIdForAuth(anyLong(), anyLong());
    }
}
