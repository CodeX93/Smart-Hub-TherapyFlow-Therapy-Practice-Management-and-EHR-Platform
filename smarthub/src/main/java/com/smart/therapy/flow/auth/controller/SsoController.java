package com.smart.therapy.flow.auth.controller;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.service.SsoService;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SSO login: initiate redirect to provider, handle callback and issue JWT.
 * Tenant is resolved from subdomain (TenantFilter) or explicit org selection on single-domain setups.
 */
@RestController
@RequestMapping("/api/v1/auth/sso")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SSO", description = "Single Sign-On (Google, Azure, Okta) — no password login")
public class SsoController {

    private final SsoService ssoService;
    private final TenantDirectoryService tenantDirectoryService;

    /**
     * Initiate SSO: redirect user to provider (e.g. Google).
     * Tenant may come from subdomain, or from explicit org selection on single-domain setups.
     * Optional query params: redirect_uri (default from org config), orgSlug/orgId.
     */
    @GetMapping("/{provider}")
    @Operation(summary = "Start SSO login", description = "Redirects to provider (Google/Azure/Okta). Tenant can be resolved by subdomain or explicit orgSlug/orgId.")
    public void startSso(
            @Parameter(description = "Provider: GOOGLE, AZURE, OKTA") @PathVariable String provider,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String orgSlug,
            @RequestParam(required = false) String orgIdentifier,
            @RequestParam(required = false) String orgValue,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Long organisationId = resolveOrganisationId(orgId, orgSlug, orgIdentifier, orgValue);
        if (organisationId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant context required. Use tenant subdomain or pass orgSlug/orgId.");
            return;
        }
        String redirectUrl = ssoService.buildAuthorizationUrl(provider, organisationId, redirectUri(request, redirect_uri));
        response.sendRedirect(redirectUrl);
    }

    /**
     * Callback from provider: exchange code for identity, find or create user, return JWT (same shape as password login).
     */
    @GetMapping("/callback")
    @Operation(summary = "SSO callback", description = "Called by provider with code and state. Returns JWT response (same as /login).")
    public ResponseEntity<Object> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request) {
        if (error != null && !error.isBlank()) {
            log.warn("SSO callback error: {}", error);
            return ResponseEntity.badRequest().body(Map.of("error", "SSO login failed: " + error));
        }
        if (state == null || state.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing or invalid state"));
        }
        try {
            JwtAuthenticationResponse jwt = ssoService.handleCallback(
                    code,
                    state,
                    HttpRequestUtil.getClientIp(request),
                    HttpRequestUtil.getUserAgent(request));
            return ResponseEntity.ok(jwt);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("SSO callback failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * For login page: returns enabled SSO providers for current tenant so UI can show "Login with Clinic Account".
     */
    @GetMapping("/config")
    @Operation(summary = "SSO config for login page", description = "Returns enabled providers for tenant resolved by subdomain or explicit orgSlug/orgId. No auth required.")
    public ResponseEntity<Map<String, Object>> ssoConfig(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String orgSlug,
            @RequestParam(required = false) String orgIdentifier,
            @RequestParam(required = false) String orgValue) {
        Long organisationId = resolveOrganisationId(orgId, orgSlug, orgIdentifier, orgValue);
        if (organisationId == null) {
            return ResponseEntity.ok(Map.of("ssoEnabled", false, "providers", List.<String>of()));
        }
        List<String> providers = ssoService.getEnabledProvidersForOrganisation(organisationId);
        return ResponseEntity.ok(Map.of(
                "ssoEnabled", !providers.isEmpty(),
                "providers", providers));
    }

    private static String redirectUri(HttpServletRequest request, String param) {
        if (param != null && !param.isBlank()) return param;
        return buildCallbackRedirectUri(request);
    }

    private Long resolveOrganisationId(String orgId, String orgSlug, String orgIdentifier, String orgValue) {
        Long fromContext = TenantContext.getOrganisationId();
        if (fromContext != null) {
            return fromContext;
        }

        String explicitIdentifier = normalizeIdentifier(orgId, orgSlug, orgIdentifier, orgValue);
        if (explicitIdentifier == null) {
            return null;
        }

        return tenantDirectoryService.findByOrganisationIdOrSlug(explicitIdentifier)
                .map(TenantDirectoryService.TenantInfo::getOrganisationId)
                .orElse(null);
    }

    private static String normalizeIdentifier(String orgId, String orgSlug, String orgIdentifier, String orgValue) {
        if (orgId != null && !orgId.isBlank()) {
            return orgId.trim();
        }
        if (orgSlug != null && !orgSlug.isBlank()) {
            return orgSlug.trim();
        }
        if (orgIdentifier != null && orgValue != null) {
            String idType = orgIdentifier.trim().toLowerCase();
            String idValue = orgValue.trim();
            if (("id".equals(idType) || "slug".equals(idType)) && !idValue.isBlank()) {
                return idValue;
            }
        }
        return null;
    }

    private static String buildCallbackRedirectUri(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String path = request.getContextPath() + "/api/v1/auth/sso/callback";
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            return scheme + "://" + host + path;
        }
        return scheme + "://" + host + ":" + port + path;
    }

}
