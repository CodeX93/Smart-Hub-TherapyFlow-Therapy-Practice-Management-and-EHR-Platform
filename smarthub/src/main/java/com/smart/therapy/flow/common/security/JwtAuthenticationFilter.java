package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.repository.UserOrganisationAccessBlockRepository;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Validates JWT; subject (sub) = authId. Loads AuthPrincipal; optionally attaches User/Client to request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SUPER_ADMIN_PATH_PREFIX = "/api/v1/super-admin/";

    private final JwtTokenProvider tokenProvider;
    private final AuthIdentityDetailsService authIdentityDetailsService;
    private final AuthSessionService authSessionService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TenantDirectoryService tenantDirectoryService;
    private final UserOrganisationAccessBlockRepository userOrganisationAccessBlockRepository;
    private final PlatformAuditService platformAuditService;

    public static final String REQUEST_ATTR_CURRENT_USER = "currentUser";
    public static final String REQUEST_ATTR_CURRENT_CLIENT = "currentClient";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);
        if (StringUtils.hasText(jwt)) {
            try {
                if (!tokenProvider.validateToken(jwt)) {
                    logAuthFailure(null, request, "JWT_INVALID", "Token validation failed");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid token\"}");
                    return;
                }
                if ("transcription_ws".equals(tokenProvider.getTokenType(jwt))) {
                    logAuthFailure(null, request, "JWT_WRONG_PURPOSE", "WebSocket ticket used as API token");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid token purpose\"}");
                    return;
                }
                Long authId = tokenProvider.getAuthIdFromToken(jwt);
                String jti = tokenProvider.getJtiFromToken(jwt);

                if (jti != null && authSessionService.isRevoked(
                        jti,
                        HttpRequestUtil.getClientIp(request),
                        HttpRequestUtil.getUserAgent(request))) {
                    logAuthFailure(authId, request, "JWT_REVOKED", "Token jti revoked");
                    log.debug("Token jti revoked");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token has been invalidated. Please login again.\"}");
                    return;
                }

                String tokenTenantSchema = tokenProvider.getTenantSchemaFromToken(jwt);
                Long tokenOrgId = tokenProvider.getOrganisationIdFromToken(jwt);
                String currentSchema = TenantContext.getSchemaName();
                // Platform (public) tokens must not be used on tenant subdomains — reject to avoid privilege confusion.
                if ("public".equalsIgnoreCase(tokenTenantSchema) && currentSchema != null && !currentSchema.isBlank() && !"public".equalsIgnoreCase(currentSchema)) {
                    logAuthFailure(authId, request, "JWT_TENANT_MISMATCH", "Platform token on tenant request");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Platform token cannot be used on a tenant. Use the admin domain.\"}");
                    return;
                }
                if (currentSchema != null && !currentSchema.isBlank() && !"public".equalsIgnoreCase(currentSchema)
                        && !"public".equalsIgnoreCase(tokenTenantSchema)
                        && !currentSchema.equals(tokenTenantSchema)) {
                    logAuthFailure(authId, request, "JWT_TENANT_MISMATCH", "Token tenant does not match request tenant");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token tenant does not match request tenant.\"}");
                    return;
                }
                // API host requests may not carry tenant context from host/path; recover it from signed token claims.
                if ((currentSchema == null || currentSchema.isBlank() || "public".equalsIgnoreCase(currentSchema))
                        && StringUtils.hasText(tokenTenantSchema)
                        && !"public".equalsIgnoreCase(tokenTenantSchema)) {
                    var tokenTenant = tenantDirectoryService.findBySchemaName(tokenTenantSchema);
                    if (tokenTenant.isEmpty()) {
                        logAuthFailure(authId, request, "JWT_TENANT_UNKNOWN", "Token tenant schema not found");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token tenant is invalid.\"}");
                        return;
                    }
                    var tenantInfo = tokenTenant.get();
                    if (tokenOrgId != null && !tokenOrgId.equals(tenantInfo.getOrganisationId())) {
                        logAuthFailure(authId, request, "JWT_ORG_MISMATCH", "Token org does not match tenant schema");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token organisation does not match token tenant.\"}");
                        return;
                    }
                    TenantContext.setSchemaName(tenantInfo.getSchemaName());
                    TenantContext.setOrganisationId(tenantInfo.getOrganisationId());
                    tokenOrgId = tenantInfo.getOrganisationId();
                    currentSchema = tenantInfo.getSchemaName();
                }
                if (currentSchema != null && !currentSchema.isBlank() && !"public".equalsIgnoreCase(currentSchema)) {
                    Long currentOrgId = TenantContext.getOrganisationId();
                    if (currentOrgId != null && tokenOrgId == null) {
                        logAuthFailure(authId, request, "JWT_MISSING_TENANT", "Token missing tenant binding");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token is missing tenant binding.\"}");
                        return;
                    }
                    if (currentOrgId != null && tokenOrgId != null && !currentOrgId.equals(tokenOrgId)) {
                        logAuthFailure(authId, request, "JWT_ORG_MISMATCH", "Token organisation does not match request tenant");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token organisation does not match request tenant.\"}");
                        return;
                    }
                    if (currentOrgId != null && userOrganisationAccessBlockRepository.existsByAuth_IdAndOrganisation_Id(authId, currentOrgId)) {
                        logAuthFailure(authId, request, "JWT_USER_BLOCKED", "User blocked for organisation " + currentOrgId);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"User is blocked for this organisation\",\"code\":\"USER_BLOCKED_IN_ORG\"}");
                        return;
                    }
                }

                String currentSchemaForTenant = TenantContext.getSchemaName();
                if (currentSchemaForTenant != null && !currentSchemaForTenant.isBlank() && !"public".equalsIgnoreCase(currentSchemaForTenant)
                        && !isSuperAdminPath(request)) {
                    var tenantInfo = tenantDirectoryService.findBySchemaName(currentSchemaForTenant);
                    if (tenantInfo.isPresent() && !tenantInfo.get().isActive()) {
                        logAuthFailure(authId, request, "JWT_TENANT_INACTIVE", "Tenant unavailable");
                        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Tenant is unavailable\",\"code\":\"TENANT_MAINTENANCE\"}");
                        return;
                    }
                }

                UserDetails userDetails = authIdentityDetailsService.loadUserByAuthId(authId);
                if (userDetails instanceof AuthPrincipal authPrincipal) {
                    Long impersonatorAuthId = tokenProvider.getImpersonatedByAuthIdFromToken(jwt);
                    if (impersonatorAuthId != null) {
                        userDetails = authPrincipal.withImpersonator(impersonatorAuthId);
                    }
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (userDetails instanceof AuthPrincipal) {
                    AuthPrincipal principal = (AuthPrincipal) userDetails;
                    String schema = TenantContext.getSchemaName();
                    boolean hasTenant = schema != null && !schema.isBlank() && !"public".equalsIgnoreCase(schema);
                    if (hasTenant) {
                        if (principal.getIdentityType() == IdentityType.STAFF) {
                            try {
                                Optional<User> user = userRepository.findByAuthId(authId);
                                user.ifPresent(u -> request.setAttribute(REQUEST_ATTR_CURRENT_USER, u));
                            } catch (RuntimeException ex) {
                                log.warn("Skipping currentUser request attribute for authId {}: {}", authId, ex.getMessage());
                            }
                        } else if (principal.getIdentityType() == IdentityType.CLIENT) {
                            try {
                                Optional<Client> clientOpt = clientRepository.findByAuthId(authId);
                                clientOpt.ifPresent(c -> request.setAttribute(REQUEST_ATTR_CURRENT_CLIENT, c));
                            } catch (RuntimeException ex) {
                                log.warn("Skipping currentClient request attribute for authId {}: {}", authId, ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logAuthFailure(null, request, "JWT_INVALID", "Token parsing/validation error");
                log.warn("Invalid JWT supplied", ex);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private static boolean isSuperAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(SUPER_ADMIN_PATH_PREFIX);
    }

    private void logAuthFailure(Long authId, HttpServletRequest request, String action, String details) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        String ua = request.getHeader("User-Agent");
        String path = request.getRequestURI();
        String summary = "ip=" + ip + ", path=" + path + ", ua=" + (ua != null ? ua : "");
        platformAuditService.log(authId, action, "Auth", null,
                details + " | " + summary);
    }
}
