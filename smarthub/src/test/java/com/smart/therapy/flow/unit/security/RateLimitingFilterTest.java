package com.smart.therapy.flow.unit.security;

import com.smart.therapy.flow.common.config.RateLimitingConfig;
import com.smart.therapy.flow.common.filter.RateLimitingFilter;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private AuthAbuseMetrics authAbuseMetrics;

    @BeforeEach
    void setUp() {
        RateLimitingConfig config = new RateLimitingConfig();
        ReflectionTestUtils.setField(config, "rateLimitingEnabled", true);
        ReflectionTestUtils.setField(config, "defaultCapacity", 300);
        ReflectionTestUtils.setField(config, "defaultRefillTokens", 300);
        ReflectionTestUtils.setField(config, "defaultRefillDurationSeconds", 60);
        authAbuseMetrics = mock(AuthAbuseMetrics.class);
        filter = new RateLimitingFilter(config, Optional.empty(), authAbuseMetrics);
    }

    @Test
    void appliesLoginLimitToActualVersionedAuthPath() throws Exception {
        MockHttpServletResponse response = null;
        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("203.0.113.10");
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, mock(FilterChain.class));
        }

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(429);
        verify(authAbuseMetrics).incrementRateLimited();
    }

    @Test
    void doesNotApplyLoginBucketToLegacyNonMatchingPath() throws Exception {
        MockHttpServletResponse response = null;
        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("203.0.113.11");
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, mock(FilterChain.class));
        }

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void capsRefreshPerSessionSoOneOfficeIpIsNotLoggedOutTogether() throws Exception {
        // Forty colleagues behind one NAT address each restore their session on a page load.
        for (int user = 0; user < 40; user++) {
            MockHttpServletResponse response = refresh("198.51.100.7", "tf_refresh", "session-" + user);
            assertThat(response.getStatus()).as("user %d", user).isEqualTo(200);
        }
    }

    @Test
    void stillCapsOneSessionThatRefreshesTooOften() throws Exception {
        MockHttpServletResponse response = null;
        for (int i = 0; i < 31; i++) {
            response = refresh("198.51.100.8", "tf_portal_refresh", "same-session");
        }
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void keepsTheStrictPerIpCapForRefreshesWithoutACookie() throws Exception {
        MockHttpServletResponse response = null;
        for (int i = 0; i < 21; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
            request.setRemoteAddr("198.51.100.9");
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, mock(FilterChain.class));
        }
        assertThat(response.getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse refresh(String ip, String cookieName, String cookieValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                cookieName.equals("tf_portal_refresh") ? "/api/v1/portal/refresh" : "/api/v1/auth/refresh");
        request.setRemoteAddr(ip);
        request.setCookies(new jakarta.servlet.http.Cookie(cookieName, cookieValue));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        return response;
    }
}
