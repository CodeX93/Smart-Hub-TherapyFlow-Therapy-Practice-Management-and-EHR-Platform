package com.smart.therapy.flow.auth.security;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.client.portal.dto.PortalLoginResponse;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRefreshCookieAdviceTest {

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);

    @Test
    void movesStaffRefreshTokenIntoCookieAndOutOfBody() {
        when(tokenProvider.getExpirationDateFromToken("staff-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 3_600_000L));
        JwtAuthenticationResponse body = JwtAuthenticationResponse.builder()
                .accessToken("access").refreshToken("staff-token").build();

        String setCookie = write(advice(false, true), body, "/api/v1/auth/mfa/verify-login");

        assertThat(body.getRefreshToken()).isNull();
        assertThat(body.getAccessToken()).isEqualTo("access");
        assertThat(setCookie)
                .startsWith("tf_refresh=staff-token")
                .contains("Path=/api/v1/auth", "HttpOnly", "Secure", "SameSite=Lax")
                .containsPattern("Max-Age=35\\d\\d");
    }

    @Test
    void scopesPortalTokensToThePortalAndKeepsBodyWhileExposed() {
        when(tokenProvider.getExpirationDateFromToken("portal-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000L));
        PortalLoginResponse body = PortalLoginResponse.builder()
                .accessToken("access").refreshToken("portal-token").build();

        String setCookie = write(advice(true, false), body, "/api/v1/portal/login");

        assertThat(body.getRefreshToken()).isEqualTo("portal-token");
        assertThat(setCookie)
                .startsWith("tf_portal_refresh=portal-token")
                .contains("Path=/api/v1/portal")
                .doesNotContain("Secure");
    }

    @Test
    void leavesResponsesWithoutRefreshTokenAlone() {
        PortalLoginResponse mfaChallenge = PortalLoginResponse.builder().mfaRequired(true).build();

        assertThat(write(advice(false, true), mfaChallenge, "/api/v1/portal/login")).isNull();
    }

    private AuthRefreshCookieAdvice advice(boolean exposeInBody, boolean secure) {
        return new AuthRefreshCookieAdvice(new AuthRefreshCookie(tokenProvider, secure, "Lax", "", exposeInBody));
    }

    private String write(AuthRefreshCookieAdvice advice, Object body, String path) {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null,
                new ServletServerHttpRequest(new MockHttpServletRequest("POST", path)), response);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    }
}
