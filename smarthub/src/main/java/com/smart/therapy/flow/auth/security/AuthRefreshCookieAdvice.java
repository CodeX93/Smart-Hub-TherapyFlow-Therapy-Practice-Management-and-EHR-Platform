package com.smart.therapy.flow.auth.security;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.client.portal.dto.PortalActivateResponse;
import com.smart.therapy.flow.client.portal.dto.PortalLoginResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.function.Consumer;

/**
 * Puts every issued refresh token into its HttpOnly cookie, whichever endpoint issued it.
 *
 * Decided on the body rather than the declared return type: the endpoints return
 * {@code ResponseEntity<...>} or {@code ResponseEntity<Object>}, which a type check would miss.
 */
@ControllerAdvice
public class AuthRefreshCookieAdvice implements ResponseBodyAdvice<Object> {

    private final AuthRefreshCookie cookie;

    public AuthRefreshCookieAdvice(AuthRefreshCookie cookie) {
        this.cookie = cookie;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof JwtAuthenticationResponse auth) {
            moveToCookie(auth.getRefreshToken(), auth::setRefreshToken, request, response);
        } else if (body instanceof PortalLoginResponse login) {
            moveToCookie(login.getRefreshToken(), login::setRefreshToken, request, response);
        } else if (body instanceof PortalActivateResponse activate) {
            moveToCookie(activate.getRefreshToken(), activate::setRefreshToken, request, response);
        }
        return body;
    }

    private void moveToCookie(String refreshToken, Consumer<String> clearFromBody,
                              ServerHttpRequest request, ServerHttpResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        AuthRefreshCookie.Audience audience = AuthRefreshCookie.Audience.forRequestPath(request.getURI().getPath());
        response.getHeaders().add(HttpHeaders.SET_COOKIE, cookie.issue(audience, refreshToken).toString());
        if (!cookie.exposeInBody()) {
            clearFromBody.accept(null);
        }
    }
}
