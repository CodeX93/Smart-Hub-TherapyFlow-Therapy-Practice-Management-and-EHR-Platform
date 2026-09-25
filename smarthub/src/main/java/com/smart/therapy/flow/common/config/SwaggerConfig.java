package com.smart.therapy.flow.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class SwaggerConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI apiInfo() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtained from /api/v1/auth/login (staff) or /api/v1/portal/login (clients)")
                .name("Authorization");

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Smart Therapy Flow API")
                        .description("""
                                API documentation for a secure, scalable, HIPAA-compliant EHR & PMS for therapy practices, clinics, and health providers.
                                
                                ## Role-Based Access
                                
                                APIs are organized by role-based access levels:
                                
                                ### 🔴 Admin Only
                                - System configuration and management
                                - User and role management
                                - Full system access
                                
                                ### 🟠 Admin & Supervisor
                                - User management
                                - System options
                                - Bulk operations
                                
                                ### 🟡 Therapist, Admin & Supervisor
                                - Client management
                                - Session management
                                - Assessment management
                                - Task management
                                - Document management
                                
                                ### 🟢 Billing Specialist, Admin, Therapist & Supervisor
                                - Billing operations
                                - Payment processing
                                
                                ### 🔵 All Authenticated Users
                                - Personal profile
                                - Notifications
                                
                                ### ⚪ Public/Client Portal
                                - Client portal endpoints
                                - Authentication endpoints
                                
                                ## Authentication
                                
                                All secured endpoints require JWT Bearer token authentication. Include the token in the Authorization header:
                                ```
                                Authorization: Bearer <your-jwt-token>
                                ```
                                
                                ### Getting JWT Tokens
                                
                                **For Admin/Therapist/Staff:**
                                - Login at `/api/v1/auth/login` with username and password
                                - Response includes `accessToken` and `refreshToken`
                                
                                **For Clients:**
                                - Login at `/api/v1/portal/login` with email and password
                                - Response includes `accessToken` and `refreshToken`
                                
                                ### Token Usage
                                
                                - Use `accessToken` for API requests (expires in 24 hours)
                                - Use `refreshToken` to get a new access token when it expires
                                - Tokens are stateless and validated on every request
                                - Logout invalidates the token immediately via blacklist
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", bearerScheme)
                        // Backward-compatible alias for existing annotations using lowercase name.
                        .addSecuritySchemes("bearerAuth", bearerScheme))
                .tags(getApiTags())
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));

        // Configure servers dynamically based on environment
        // This ensures Swagger UI uses the correct base URL for API requests
        try {
            String serverUrl = System.getenv("SWAGGER_SERVER_URL");
            if (serverUrl == null || serverUrl.isEmpty()) {
                // Default: use relative path (works for same-origin requests)
                String contextPathValue = (contextPath != null && !contextPath.isEmpty()) ? contextPath : "/";
                openAPI.addServersItem(new Server().url(contextPathValue));
                log.debug("Swagger configured with context path: {}", contextPathValue);
            } else {
                // Use explicit server URL from environment variable
                openAPI.addServersItem(new Server().url(serverUrl));
                log.info("Swagger configured with server URL: {}", serverUrl);
            }
        } catch (Exception e) {
            log.warn("Error configuring Swagger server URL, using default", e);
            openAPI.addServersItem(new Server().url("/"));
        }

        return openAPI;
    }

    /**
     * Adds tenant routing headers globally for SwaggerHub/testing tools.
     * Required when requests do not use subdomain routing (e.g. SwaggerHub).
     */
    @Bean
    public OpenApiCustomizer tenantRoutingHeadersCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                if (operation.getParameters() == null) {
                    operation.setParameters(new java.util.ArrayList<>());
                }
                boolean hasSchemaHeader = operation.getParameters().stream()
                        .anyMatch(p -> "X-Tenant-Schema".equalsIgnoreCase(p.getName()));
                boolean hasSubdomainHeader = operation.getParameters().stream()
                        .anyMatch(p -> "X-Tenant-Subdomain".equalsIgnoreCase(p.getName()));

                if (!hasSchemaHeader) {
                    operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                            .in("header")
                            .name("X-Tenant-Schema")
                            .description("Optional tenant schema override (e.g. tenant_4) for non-subdomain clients")
                            .required(false)
                            .schema(new io.swagger.v3.oas.models.media.StringSchema()));
                }
                if (!hasSubdomainHeader) {
                    operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                            .in("header")
                            .name("X-Tenant-Subdomain")
                            .description("Optional tenant subdomain override (e.g. harbor) for non-subdomain clients")
                            .required(false)
                            .schema(new io.swagger.v3.oas.models.media.StringSchema()));
                }
            }));
        };
    }

    /**
     * Ensures Swagger lock/auth behavior is consistent across all APIs:
     * - Public/auth/webhook endpoints are unlocked.
     * - All other operations are locked with Bearer auth.
     * - Adds generated summaries for operations missing explicit @Operation(summary=...).
     */
    @Bean
    public OpenApiCustomizer operationSecurityLockCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (pathItem == null || pathItem.readOperationsMap() == null) {
                    return;
                }
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (operation == null) {
                        return;
                    }

                    boolean isPublic = isPublicPath(path);
                    if (isPublic) {
                        operation.setSecurity(Collections.emptyList());
                    } else if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
                        operation.setSecurity(List.of(new SecurityRequirement().addList("BearerAuth")));
                    }

                    if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                        operation.setSummary(generateSummary(method.name(), path));
                    }
                });
            });
        };
    }

    /**
     * Adds consistent default Swagger response documentation across operations.
     * - Secured operations: 401, 403
     * - All operations: 500
     */
    @Bean
    public OpenApiCustomizer defaultApiResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (pathItem == null || pathItem.readOperationsMap() == null) {
                    return;
                }
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (operation == null) {
                        return;
                    }
                    if (operation.getResponses() == null) {
                        operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
                    }
                    operation.getResponses().putIfAbsent("500", new ApiResponse().description("Internal server error"));

                    boolean secured = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
                    if (secured) {
                        operation.getResponses().putIfAbsent("401", new ApiResponse().description("Unauthorized"));
                        operation.getResponses().putIfAbsent("403", new ApiResponse().description("Forbidden"));
                    }
                });
            });
        };
    }

    private boolean isPublicPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return PUBLIC_ENDPOINT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }

    private String generateSummary(String method, String path) {
        String verb = switch (method.toUpperCase(Locale.ROOT)) {
            case "GET" -> "Get";
            case "POST" -> "Create";
            case "PUT" -> "Update";
            case "PATCH" -> "Patch";
            case "DELETE" -> "Delete";
            default -> "Call";
        };
        String normalized = path
                .replaceAll("\\{[^}]+}", "item")
                .replaceAll("^/+", "")
                .replaceAll("/+", " ")
                .trim();
        if (normalized.isBlank()) {
            return verb + " endpoint";
        }
        return verb + " " + normalized;
    }

    private static final Set<Pattern> PUBLIC_ENDPOINT_PATTERNS = Set.of(
            Pattern.compile("^/api/v1/auth/login$"),
            Pattern.compile("^/api/v1/auth/refresh$"),
            Pattern.compile("^/api/v1/auth/resolve-tenant$"),
            Pattern.compile("^/api/v1/auth/login-context$"),
            Pattern.compile("^/api/v1/auth/forgot-password$"),
            Pattern.compile("^/api/v1/auth/reset-password$"),
            Pattern.compile("^/api/v1/auth/reset-password/validate$"),
            Pattern.compile("^/api/v1/auth/sso(?:/.*)?$"),
            Pattern.compile("^/api/v1/portal/login$"),
            Pattern.compile("^/api/v1/portal/login-context$"),
            Pattern.compile("^/api/v1/portal/refresh$"),
            Pattern.compile("^/api/v1/portal/activate$"),
            Pattern.compile("^/api/v1/portal/activate/validate$"),
            Pattern.compile("^/api/v1/portal/forgot-password$"),
            Pattern.compile("^/api/v1/portal/reset-password$"),
            Pattern.compile("^/api/v1/notifications/templates$"),
            Pattern.compile("^/api/v1/stripe/webhook/platform$"),
            Pattern.compile("^/api/v1/stripe/webhook/connect$"),
            Pattern.compile("^/api/v1/stripe/webhook/tenant/[^/]+$"),
            Pattern.compile("^/api/v1/admin/stripe-connect/oauth/callback$"),
            Pattern.compile("^/actuator/health$"),
            Pattern.compile("^/v3/api-docs(?:/.*)?$"),
            Pattern.compile("^/swagger-ui(?:/.*)?$")
    );

    private List<Tag> getApiTags() {
        return Arrays.asList(
                // Authentication & Public
                new Tag().name("Authentication").description("Public authentication endpoints (no auth required) - Get JWT tokens here"),
                new Tag().name("Client Portal").description("Client portal endpoints - Public login/activate, secured endpoints require CLIENT role JWT token"),
                
                // Admin Only
                new Tag().name("Admin").description("🔴 Admin only - System administration and configuration"),
                new Tag().name("Roles & Permissions").description("🔴 Admin only - Role and permission management"),
                new Tag().name("System").description("🔴 Admin/SUPER_ADMIN only - System-level operations"),
                
                // Admin & Supervisor
                new Tag().name("System Options").description("🟠 Admin & Supervisor - System configuration options"),
                new Tag().name("User Management").description("🟠 Admin & Supervisor - User account management"),
                
                // Therapist, Admin & Supervisor
                new Tag().name("Clients").description("🟡 Therapist, Admin & Supervisor - Client management"),
                new Tag().name("Sessions").description("🟡 Therapist, Admin & Supervisor - Session management"),
                new Tag().name("Session Notes").description("🟡 Therapist, Admin & Supervisor - Session notes"),
                new Tag().name("Assessments").description("🟡 Therapist, Admin & Supervisor - Assessment management"),
                new Tag().name("Tasks").description("🟡 Therapist, Admin & Supervisor - Task management"),
                new Tag().name("Checklists").description("🟡 Therapist, Admin & Supervisor - Checklist management"),
                new Tag().name("Documents").description("🟡 Therapist, Admin & Supervisor - Document management"),
                new Tag().name("Notes").description("🟡 Therapist, Admin & Supervisor - General notes"),
                new Tag().name("Forms").description("🟡 Therapist, Admin & Supervisor - Form management"),
                new Tag().name("Library").description("🟡 Therapist, Admin & Supervisor - Library management"),
                new Tag().name("Therapist Availability").description("🟡 Therapist, Admin & Supervisor - Availability management"),
                
                // Billing
                new Tag().name("Billing").description("🟢 Billing Specialist, Admin, Therapist & Supervisor - Billing operations"),
                new Tag().name("Stripe").description("🟢 Authenticated users - Stripe payment processing"),
                
                // All Authenticated
                new Tag().name("User Profile").description("🔵 All authenticated users - Personal profile management"),
                new Tag().name("Notifications").description("🔵 All authenticated users - Notification management"),
                
                // AI
                new Tag().name("AI").description("🟣 Therapist, Admin & SYSTEM_AI_ASSISTANT - AI-powered features")
        );
    }

    // ========== ROLE-BASED API GROUPS ==========
    // These groups allow filtering endpoints by role in Swagger UI

    /**
     * All APIs - Default group showing all endpoints including authentication
     */
    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("all-apis")
                .displayName("📚 All APIs")
                .pathsToMatch(
                        "/api/v1/auth/**",      // Authentication endpoints (login, refresh, logout)
                        "/api/v1/portal/**",   // Client portal endpoints
                        "/api/**"              // All other API endpoints
                )
                .build();
    }

    /**
     * 🔴 Admin Only - System administration and configuration
     */
    @Bean
    public GroupedOpenApi adminOnlyApis() {
        return GroupedOpenApi.builder()
                .group("admin-only")
                .displayName("🔴 Admin Only")
                .pathsToMatch(
                        "/api/v1/admin/**",
                        "/api/v1/roles/**",
                        "/api/v1/permissions/**",
                        "/api/v1/system/**",
                        "/api/v1/system-options/**",
                        "/api/v1/practice-configuration/**",
                        "/api/v1/super-admin/system/**"
                )
                .build();
    }

    /**
     * 🟠 Admin & Supervisor - User management and system options
     */
    @Bean
    public GroupedOpenApi adminSupervisorApis() {
        return GroupedOpenApi.builder()
                .group("admin-supervisor")
                .displayName("🟠 Admin & Supervisor")
                .pathsToMatch("/api/v1/system-options/**", "/api/v1/practice-configuration/**", "/api/v1/users/**")
                .build();
    }

    /**
     * 🟡 Therapist, Admin & Supervisor - Client and session management
     */
    @Bean
    public GroupedOpenApi therapistAdminSupervisorApis() {
        return GroupedOpenApi.builder()
                .group("therapist-admin-supervisor")
                .displayName("🟡 Therapist, Admin & Supervisor")
                .pathsToMatch(
                        "/api/v1/clients/**",
                        "/api/v1/sessions/**",
                        "/api/v1/session-notes/**",
                        "/api/v1/assessments/**",
                        "/api/v1/tasks/**",
                        "/api/v1/checklists/**",
                        "/api/v1/documents/**",
                        "/api/v1/notes/**",
                        "/api/v1/forms/**",
                        "/api/v1/library/**",
                        "/api/v1/therapist-availability/**"
                )
                .build();
    }

    /**
     * 🟢 Billing - Billing Specialist, Admin, Therapist & Supervisor
     */
    @Bean
    public GroupedOpenApi billingApis() {
        return GroupedOpenApi.builder()
                .group("billing")
                .displayName("🟢 Billing")
                .pathsToMatch("/api/v1/billing/**", "/api/v1/stripe/**")
                .build();
    }

    /**
     * 🔵 All Authenticated Users - Personal profile and notifications
     */
    @Bean
    public GroupedOpenApi authenticatedUserApis() {
        return GroupedOpenApi.builder()
                .group("authenticated-users")
                .displayName("🔵 All Authenticated Users")
                .pathsToMatch("/api/v1/users/me/**", "/api/v1/notifications/**")
                .build();
    }

    /**
     * ⚪ Public/Client Portal - Public endpoints and client portal
     */
    @Bean
    public GroupedOpenApi publicApis() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("⚪ Public/Client Portal")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/portal/**", "/api/v1/client-portal/**")
                .build();
    }

    /**
     * 🟣 AI - AI-powered features
     */
    @Bean
    public GroupedOpenApi aiApis() {
        return GroupedOpenApi.builder()
                .group("ai")
                .displayName("🟣 AI Features")
                .pathsToMatch("/api/v1/ai/**")
                .build();
    }

}
