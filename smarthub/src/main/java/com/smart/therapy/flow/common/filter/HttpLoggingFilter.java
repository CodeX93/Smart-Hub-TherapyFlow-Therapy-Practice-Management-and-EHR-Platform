package com.smart.therapy.flow.common.filter;

import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATH_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs");

    private static final String MDC_ORG_ID = "organisationId";
    private static final String MDC_TENANT_SCHEMA = "tenantSchema";
    private static final String MDC_REQUEST_SCOPE = "requestScope";
    private static final Pattern STREAMING_DOCUMENT_PATH = Pattern.compile(
            "^/api/v1/clients/\\d+/documents/\\d+/(download|viewer|file)$");

    private final SensitiveDataMasker sensitiveDataMasker;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        for (String excluded : EXCLUDED_PATH_PREFIXES) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startNanos = System.nanoTime();
        boolean streamingDocumentRequest = isStreamingDocumentRequest(request);
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = streamingDocumentRequest
                ? null
                : new ContentCachingResponseWrapper(response);

        String schema = normalizeSchema(TenantContext.getSchemaName());
        Long orgId = TenantContext.getOrganisationId();
        boolean tenantScoped = !"public".equals(schema) || orgId != null;
        String requestScope = tenantScoped ? "tenant" : "platform";

        MDC.put(MDC_TENANT_SCHEMA, schema);
        MDC.put(MDC_ORG_ID, orgId != null ? String.valueOf(orgId) : "platform");
        MDC.put(MDC_REQUEST_SCOPE, requestScope);

        try {
            logIncomingRequest(requestWrapper, requestScope, schema, orgId);
            if (streamingDocumentRequest) {
                filterChain.doFilter(requestWrapper, response);
            } else {
                filterChain.doFilter(requestWrapper, responseWrapper);
            }
        } finally {
            if (streamingDocumentRequest) {
                logOutgoingStreamingResponse(requestWrapper, response, startNanos, requestScope, schema, orgId);
            } else {
                logOutgoingResponse(requestWrapper, responseWrapper, startNanos, requestScope, schema, orgId);
                responseWrapper.copyBodyToResponse();
            }
            MDC.remove(MDC_REQUEST_SCOPE);
            MDC.remove(MDC_ORG_ID);
            MDC.remove(MDC_TENANT_SCHEMA);
        }
    }

    private void logIncomingRequest(ContentCachingRequestWrapper request, String requestScope, String schema, Long orgId) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String sanitizedQuery = sensitiveDataMasker.sanitizeQueryString(query);
        String fullPath = !sanitizedQuery.isEmpty() ? path + "?" + sanitizedQuery : path;

        String headers = sensitiveDataMasker.maskHeaders(request);
        log.info("HTTP_IN scope={} orgId={} schema={} method={} path={} headers={}",
                requestScope, orgId, schema, method, fullPath, headers);
    }

    private void logOutgoingResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
            long startNanos, String requestScope, String schema, Long orgId) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        int status = response.getStatus();
        String method = request.getMethod();
        String path = request.getRequestURI();
        int responseSize = response.getContentAsByteArray() != null ? response.getContentAsByteArray().length : 0;

        if (status >= 500) {
            log.error("HTTP_OUT scope={} orgId={} schema={} method={} path={} status={} durationMs={} responseBytes={}",
                    requestScope, orgId, schema, method, path, status, durationMs, responseSize);
        } else if (status >= 400) {
            log.warn("HTTP_OUT scope={} orgId={} schema={} method={} path={} status={} durationMs={} responseBytes={}",
                    requestScope, orgId, schema, method, path, status, durationMs, responseSize);
        } else {
            log.info("HTTP_OUT scope={} orgId={} schema={} method={} path={} status={} durationMs={} responseBytes={}",
                    requestScope, orgId, schema, method, path, status, durationMs, responseSize);
        }

    }

    private void logOutgoingStreamingResponse(ContentCachingRequestWrapper request, HttpServletResponse response,
            long startNanos, String requestScope, String schema, Long orgId) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        int status = response.getStatus();
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (status >= 500) {
            log.error("HTTP_OUT scope={} orgId={} schema={} method={} path={} status={} durationMs={} responseBytes={}",
                    requestScope, orgId, schema, method, path, status, durationMs, 0);
        } else if (status >= 400) {
            log.warn("HTTP_OUT scope={} orgId={} schema={} method={} path={} status={} durationMs={} responseBytes={}",
                    requestScope, orgId, schema, method, path, status, durationMs, 0);
        } else {
            log.info("HTTP_OUT scope={} orgId={} schema={} method={} path={} status={} durationMs={} responseBytes={}",
                    requestScope, orgId, schema, method, path, status, durationMs, 0);
        }

        if (log.isDebugEnabled()) {
            log.debug("HTTP_OUT_BODY scope={} method={} path={} status={} contentType={} body=<omitted-streaming>",
                    requestScope, method, path, status, response.getContentType());
        }
    }

    private boolean isStreamingDocumentRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && STREAMING_DOCUMENT_PATH.matcher(path).matches();
    }

    private String normalizeSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return "public";
        }
        return schema;
    }
}
