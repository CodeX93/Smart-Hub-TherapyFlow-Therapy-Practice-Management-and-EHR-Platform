package com.smart.therapy.flow.transcription.ws;

import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscribeLiveHandshakeInterceptor implements HandshakeInterceptor {

    static final String ATTR_AUTH_PRINCIPAL = "authPrincipal";
    static final String ATTR_TENANT_SCHEMA = "tenantSchema";
    static final String ATTR_ORG_ID = "orgId";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthIdentityDetailsService authIdentityDetailsService;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        String uploadId = resolveParameter(request, "uploadId");
        String ticket = resolveTicket(request);
        if (!StringUtils.hasText(ticket)
                || !StringUtils.hasText(uploadId)
                || !jwtTokenProvider.validateTranscriptionWebSocketTicket(ticket, uploadId)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Long authId = jwtTokenProvider.getAuthIdFromToken(ticket);
            String tenantSchema = jwtTokenProvider.getTenantSchemaFromToken(ticket);
            Long organisationId = jwtTokenProvider.getOrganisationIdFromToken(ticket);

            UserDetails details = loadUserDetailsInTenantContext(authId, tenantSchema, organisationId);
            if (!(details instanceof AuthPrincipal principal)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put(ATTR_AUTH_PRINCIPAL, principal);
            attributes.put(ATTR_TENANT_SCHEMA, tenantSchema);
            attributes.put(ATTR_ORG_ID, organisationId);
            return true;
        } catch (Exception ex) {
            log.warn("Live transcript WS auth failed: {}", ex.getMessage());
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private String resolveTicket(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return resolveParameter(request, "ticket");
    }

    private String resolveParameter(ServerHttpRequest request, String name) {
        URI uri = request.getURI();
        if (uri == null || !StringUtils.hasText(uri.getQuery())) {
            return null;
        }
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        String value = servletRequest.getServletRequest().getParameter(name);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserDetails loadUserDetailsInTenantContext(Long authId, String tenantSchema, Long organisationId) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrgId = TenantContext.getOrganisationId();
        try {
            if (StringUtils.hasText(tenantSchema) && !"public".equalsIgnoreCase(tenantSchema)) {
                TenantContext.setSchemaName(tenantSchema);
                TenantContext.setOrganisationId(organisationId);
            } else {
                TenantContext.clear();
            }
            return authIdentityDetailsService.loadUserByAuthId(authId);
        } finally {
            TenantContext.clear();
            if (StringUtils.hasText(previousSchema)) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrgId != null) {
                TenantContext.setOrganisationId(previousOrgId);
            }
        }
    }
}
