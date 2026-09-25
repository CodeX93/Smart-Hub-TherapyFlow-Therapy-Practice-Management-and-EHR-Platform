package com.smart.therapy.flow.auth.security;

import com.smart.therapy.flow.common.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;

/**
 * The HttpOnly cookie that carries a refresh token, so the browser never hands it to script.
 *
 * Staff and client-portal sessions each get their own cookie, scoped to the path of their
 * refresh and logout endpoints, so a browser signed in to both keeps the two apart.
 */
@Component
public class AuthRefreshCookie {

    public enum Audience {
        STAFF("tf_refresh", "/api/v1/auth"),
        PORTAL("tf_portal_refresh", "/api/v1/portal");

        private final String cookieName;
        private final String path;

        Audience(String cookieName, String path) {
            this.cookieName = cookieName;
            this.path = path;
        }

        public String cookieName() {
            return cookieName;
        }

        public String path() {
            return path;
        }

        /** Portal endpoints issue portal sessions; every other token-issuing endpoint is staff. */
        public static Audience forRequestPath(String requestPath) {
            return requestPath != null && requestPath.startsWith(PORTAL.path) ? PORTAL : STAFF;
        }
    }

    private final JwtTokenProvider tokenProvider;
    private final boolean secure;
    private final String sameSite;
    private final String domain;
    private final boolean exposeInBody;

    public AuthRefreshCookie(
            JwtTokenProvider tokenProvider,
            @Value("${app.security.use-secure-cookies:false}") boolean secure,
            @Value("${app.security.refresh-cookie.same-site:Lax}") String sameSite,
            @Value("${app.security.refresh-cookie.domain:}") String domain,
            @Value("${app.security.refresh-cookie.expose-in-body:true}") boolean exposeInBody) {
        this.tokenProvider = tokenProvider;
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
        this.exposeInBody = exposeInBody;
    }

    /** Whether responses still carry the refresh token in JSON, for clients that predate the cookie. */
    public boolean exposeInBody() {
        return exposeInBody;
    }

    /** A cookie that lives exactly as long as the token it carries. */
    public ResponseCookie issue(Audience audience, String refreshToken) {
        Date expiry = tokenProvider.getExpirationDateFromToken(refreshToken);
        long maxAgeMs = expiry != null ? expiry.getTime() - System.currentTimeMillis() : 0L;
        return base(audience, refreshToken).maxAge(Duration.ofMillis(Math.max(0L, maxAgeMs))).build();
    }

    public ResponseCookie clear(Audience audience) {
        return base(audience, "").maxAge(Duration.ZERO).build();
    }

    /** The refresh token the browser sent for this audience, or null. */
    public String read(HttpServletRequest request, Audience audience) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (audience.cookieName().equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** A token a client still sends in the body wins; otherwise the cookie's. */
    public String resolve(String bodyToken, HttpServletRequest request, Audience audience) {
        return StringUtils.hasText(bodyToken) ? bodyToken : read(request, audience);
    }

    private ResponseCookie.ResponseCookieBuilder base(Audience audience, String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(audience.cookieName(), value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(audience.path());
        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        return builder;
    }
}
