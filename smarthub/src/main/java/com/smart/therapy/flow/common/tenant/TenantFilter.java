package com.smart.therapy.flow.common.tenant;

import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves tenant from request and sets TenantContext.
 * Uses TenantDirectoryService (in-memory cache) — never hits DB on hot path.
 * Reserved subdomains (admin, platform, www, api, etc.) from cache → no tenant.
 * Lifecycle: ACTIVE → allow; LOCKED → 503; ARCHIVED/DELETED → 410 Gone.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
public class TenantFilter implements Filter {

    public static final String HEADER_TENANT_SCHEMA = "X-Tenant-Schema";
    public static final String HEADER_TENANT_SUBDOMAIN = "X-Tenant-Subdomain";
    public static final String HEADER_DEBUG_TENANT = "X-Debug-Tenant";

    private static final String HEADER_RESOLVED_TENANT_SCHEMA = "X-Resolved-Tenant-Schema";
    private static final String HEADER_RESOLVED_TENANT_ORG = "X-Resolved-Tenant-Org";
    private static final String HEADER_TENANT_RESOLUTION_SOURCE = "X-Tenant-Resolution-Source";

    private static final String MAINTENANCE_JSON = "{\"error\":\"Tenant is unavailable\",\"code\":\"TENANT_MAINTENANCE\"}";
    private static final String FORCE_DISABLED_JSON = "{\"error\":\"Tenant has been disabled\",\"code\":\"TENANT_FORCE_DISABLED\"}";
    private static final String SCHEMA_MISSING_JSON = "{\"error\":\"Tenant schema is not provisioned\",\"code\":\"TENANT_SCHEMA_MISSING\"}";
    private static final String ARCHIVED_JSON = "{\"error\":\"Tenant has been archived\",\"code\":\"TENANT_ARCHIVED\"}";
    private static final String DELETED_JSON = "{\"error\":\"Tenant no longer exists\",\"code\":\"TENANT_DELETED\"}";

    private final TenantDirectoryService tenantDirectoryService;
    private final com.smart.therapy.flow.organisation.service.TenantSchemaHealthService tenantSchemaHealthService;
    private final PlatformTenantRoutingService platformTenantRoutingService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        try {
            if (!resolveAndSetTenant(req, res)) {
                return;
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * @return false if response was committed (e.g. 503 for locked tenant), true to continue.
     */
    private boolean resolveAndSetTenant(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // 1) Explicit schema (dev / API)
        String schemaHeader = req.getHeader(HEADER_TENANT_SCHEMA);
        if (schemaHeader != null && !schemaHeader.isBlank()) {
            return setTenantBySchema(schemaHeader.trim(), req, res);
        }

        // 2) Subdomain from header (e.g. reverse proxy sets it)
        String subdomainHeader = req.getHeader(HEADER_TENANT_SUBDOMAIN);
        if (subdomainHeader != null && !subdomainHeader.isBlank()) {
            return setTenantBySubdomain(subdomainHeader.trim(), req, res, "subdomain-header");
        }

        // 3) Subdomain from Host: clinicA.yourapp.com → clinicA
        String host = req.getServerName();
        if (host != null && host.contains(".") && !isIpv4Address(host)) {
            String subdomain = host.substring(0, host.indexOf('.'));
            if (!subdomain.isBlank() && !tenantDirectoryService.isReservedSubdomain(subdomain)) {
                return setTenantBySubdomain(subdomain, req, res, "host-subdomain");
            }
        }

        // 4) Path-based routing if enabled (e.g. /org/{slug}/...)
        if (resolveByPath(req, res)) {
            return true;
        }

        // No tenant resolved — public-only (e.g. login, health, admin.yourapp.com).
        log.trace("No tenant resolved for request; using public schema only.");
        addTenantDebugHeaders(req, res, "public");
        return true;
    }

    private boolean setTenantBySchema(String schemaName, HttpServletRequest req, HttpServletResponse res) throws IOException {
        Optional<TenantDirectoryService.TenantInfo> info = tenantDirectoryService.findBySchemaName(schemaName);
        if (info.isEmpty()) {
            log.warn("Unknown tenant schema from header: {}", schemaName);
            addTenantDebugHeaders(req, res, "schema-header-unknown");
            return true;
        }
        TenantDirectoryService.TenantInfo ti = info.get();
        if (ti.isForceDisabled()) {
            sendForceDisabled(res);
            return false;
        }
        if (ti.isArchived()) {
            sendGone(res, ARCHIVED_JSON);
            return false;
        }
        if (ti.isDeleted()) {
            sendGone(res, DELETED_JSON);
            return false;
        }
        if (!ti.isActive()) {
            sendMaintenance(res);
            return false;
        }
        if (!tenantSchemaHealthService.schemaExists(ti.getSchemaName())) {
            sendSchemaMissing(res);
            return false;
        }
        TenantContext.setSchemaName(ti.getSchemaName());
        TenantContext.setOrganisationId(ti.getOrganisationId());
        addTenantDebugHeaders(req, res, "schema-header");
        return true;
    }

    private boolean setTenantBySubdomain(String subdomain, HttpServletRequest req, HttpServletResponse res,
            String source) throws IOException {
        Optional<TenantDirectoryService.TenantInfo> info = tenantDirectoryService.findBySubdomain(subdomain);
        if (info.isEmpty()) {
            log.warn("Unknown tenant subdomain: {}", subdomain);
            addTenantDebugHeaders(req, res, source + "-unknown");
            return true;
        }
        TenantDirectoryService.TenantInfo ti = info.get();
        if (ti.isForceDisabled()) {
            sendForceDisabled(res);
            return false;
        }
        if (ti.isArchived()) {
            sendGone(res, ARCHIVED_JSON);
            return false;
        }
        if (ti.isDeleted()) {
            sendGone(res, DELETED_JSON);
            return false;
        }
        if (!ti.isActive()) {
            sendMaintenance(res);
            return false;
        }
        if (!tenantSchemaHealthService.schemaExists(ti.getSchemaName())) {
            sendSchemaMissing(res);
            return false;
        }
        TenantContext.setSchemaName(ti.getSchemaName());
        TenantContext.setOrganisationId(ti.getOrganisationId());
        addTenantDebugHeaders(req, res, source);
        return true;
    }

    private boolean resolveByPath(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var settings = platformTenantRoutingService.getSettings();
        if (settings == null || !Boolean.TRUE.equals(settings.getPathBasedRouting())) {
            return false;
        }
        String prefix = settings.getPathPrefix();
        if (prefix == null || prefix.isBlank()) {
            return false;
        }
        String path = req.getRequestURI();
        if (path == null || !path.startsWith(prefix + "/")) {
            return false;
        }
        String remaining = path.substring((prefix + "/").length());
        String key = remaining.contains("/") ? remaining.substring(0, remaining.indexOf('/')) : remaining;
        if (key.isBlank()) {
            return false;
        }
        String identifier = settings.getOrgIdentifier();
        var info = "id".equalsIgnoreCase(identifier)
                ? tenantDirectoryService.findByOrganisationIdOrSlug(key) // numeric id is supported in resolver
                : tenantDirectoryService.findBySlug(key);
        if (info.isEmpty()) {
            log.warn("Unknown tenant path identifier: {}", key);
            addTenantDebugHeaders(req, res, "path-unknown");
            return true;
        }
        return setTenantByInfo(info.get(), req, res, "path");
    }

    private boolean setTenantByInfo(TenantDirectoryService.TenantInfo ti, HttpServletRequest req,
            HttpServletResponse res, String source) throws IOException {
        if (ti.isForceDisabled()) {
            sendForceDisabled(res);
            return false;
        }
        if (ti.isArchived()) {
            sendGone(res, ARCHIVED_JSON);
            return false;
        }
        if (ti.isDeleted()) {
            sendGone(res, DELETED_JSON);
            return false;
        }
        if (!ti.isActive()) {
            sendMaintenance(res);
            return false;
        }
        if (!tenantSchemaHealthService.schemaExists(ti.getSchemaName())) {
            sendSchemaMissing(res);
            return false;
        }
        TenantContext.setSchemaName(ti.getSchemaName());
        TenantContext.setOrganisationId(ti.getOrganisationId());
        addTenantDebugHeaders(req, res, source);
        return true;
    }

    private void addTenantDebugHeaders(HttpServletRequest req, HttpServletResponse res, String source) {
        if (!isDebugTenantRequest(req)) {
            return;
        }
        String schema = TenantContext.getSchemaName();
        Long orgId = TenantContext.getOrganisationId();
        String resolvedSchema = (schema == null || schema.isBlank()) ? "public" : schema;
        String resolvedOrg = orgId == null ? "none" : orgId.toString();

        res.setHeader(HEADER_RESOLVED_TENANT_SCHEMA, resolvedSchema);
        res.setHeader(HEADER_RESOLVED_TENANT_ORG, resolvedOrg);
        res.setHeader(HEADER_TENANT_RESOLUTION_SOURCE, source);
    }

    private boolean isDebugTenantRequest(HttpServletRequest req) {
        String flag = req.getHeader(HEADER_DEBUG_TENANT);
        if (flag == null) {
            return false;
        }
        return "true".equalsIgnoreCase(flag)
                || "1".equals(flag)
                || "yes".equalsIgnoreCase(flag);
    }

    private void sendMaintenance(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(MAINTENANCE_JSON);
    }

    private void sendSchemaMissing(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(SCHEMA_MISSING_JSON);
    }

    private void sendGone(HttpServletResponse res, String json) throws IOException {
        res.setStatus(HttpServletResponse.SC_GONE);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json);
    }

    private void sendForceDisabled(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(FORCE_DISABLED_JSON);
    }

    private static boolean isIpv4Address(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isBlank() || part.length() > 3) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) {
                    return false;
                }
            }
            int octet = Integer.parseInt(part);
            if (octet < 0 || octet > 255) {
                return false;
            }
        }
        return true;
    }
}
