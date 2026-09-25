package com.smart.therapy.flow.common.config;

import com.smart.therapy.flow.common.security.ApiKeyAuthenticationFilter;
import com.smart.therapy.flow.common.security.JsonAccessDeniedHandler;
import com.smart.therapy.flow.common.security.JwtAuthenticationFilter;
import com.smart.therapy.flow.common.security.PublicEndpointRateLimitFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.env.Environment;

import java.util.Arrays;

import static com.smart.therapy.flow.common.security.PermissionConstants.USER_VIEW;
import static com.smart.therapy.flow.common.security.PermissionConstants.BILLING_MODULE_ACCESS;
import static com.smart.therapy.flow.common.security.PermissionConstants.BILLING_MANAGE;
import static com.smart.therapy.flow.common.security.PermissionConstants.CLIENT_PORTAL_ACCESS;
import static com.smart.therapy.flow.common.security.PermissionConstants.CONSENT_ADMIN_VIEW;
import static com.smart.therapy.flow.common.security.PermissionConstants.USER_MANAGE;
import static com.smart.therapy.flow.common.security.PermissionConstants.PLATFORM_MANAGE;
import static com.smart.therapy.flow.common.security.RoleConstants.PLATFORM_READ;
import static com.smart.therapy.flow.common.security.RoleConstants.ROLE_PLATFORM_SUPER_ADMIN;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self'; "
            + "img-src 'self' data:; "
            + "font-src 'self' data:; "
            + "connect-src 'self'; "
            + "object-src 'none'; "
            + "base-uri 'none'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final PublicEndpointRateLimitFilter publicEndpointRateLimitFilter;
    private final UserDetailsService userDetailsService;
    private final Environment environment;
    @Value("${app.env:dev}")
    private String appEnv;

    /**
     * Check if the application is running in production environment.
     * 
     * @return true if production profile is active, false otherwise
     */
    private boolean isProductionEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProdEnv() {
        return "prod".equalsIgnoreCase(appEnv);
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        // Only show detailed error messages in non-production environments
        // In production, hide user not found exceptions to prevent username enumeration attacks
        boolean isProduction = isProductionEnvironment();
        authProvider.setHideUserNotFoundExceptions(isProduction);
        if (!isProduction) {
            log.debug("Detailed authentication error messages enabled (non-production environment)");
        }
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        // Spring Boot automatically configures AuthenticationManager
        // when it detects UserDetailsService and PasswordEncoder beans
        // Our explicit DaoAuthenticationProvider bean will be used
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers
                            .contentTypeOptions(Customizer.withDefaults())
                            .frameOptions(frame -> frame.deny())
                            .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                            .contentSecurityPolicy(csp -> csp
                                    .policyDirectives(CONTENT_SECURITY_POLICY));
                    if (isProdEnv()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000));
                    }
                })
                .authorizeHttpRequests(auth -> auth
                        // Always allow CORS preflight requests before auth checks.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/mfa/verify-login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/mfa/send-login-code").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/mfa/required-enrollment",
                                "/api/v1/auth/mfa/required-enrollment/confirm").permitAll()
                        .requestMatchers("/api/v1/auth/mfa/**").authenticated()
                        .requestMatchers("/api/v1/auth/devices", "/api/v1/auth/devices/**").authenticated()
                        .requestMatchers("/api/v1/auth/sessions", "/api/v1/auth/sessions/**").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/ws/transcribe-live").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/email-icons/**").permitAll()
                        // Azure managed certificate HTTP domain validation
                        .requestMatchers("/.well-known/acme-challenge/**").permitAll()

                        // Twilio inbound SMS webhook (public, secured by Twilio signature verification)
                        .requestMatchers("/api/sms/inbound").permitAll()

                        // Stripe webhooks (public, secured by Stripe signature verification)
                        .requestMatchers("/api/v1/stripe/webhook/platform", "/api/v1/stripe/webhook/connect", "/api/v1/stripe/webhook/tenant/*").permitAll()

                        // Stripe Connect OAuth callback (public redirect from Stripe)
                        .requestMatchers("/api/v1/admin/stripe-connect/oauth/callback").permitAll()
                        
                        // Org Stripe Connect setup endpoints (billing-manage)
                        .requestMatchers("/api/v1/admin/stripe-connect/**").access(new WebExpressionAuthorizationManager(BILLING_MANAGE))

                        // Super-admin read-only: list orgs, get one, health, schema-version, features, audit logs — SUPER_ADMIN or AUDITOR.
                        .requestMatchers(HttpMethod.GET, "/api/v1/super-admin/organisations", "/api/v1/super-admin/organisations/*", "/api/v1/super-admin/organisations/*/health", "/api/v1/super-admin/organisations/*/schema-version", "/api/v1/super-admin/organisations/*/features", "/api/v1/super-admin/organisations/*/features/list", "/api/v1/super-admin/organisations/*/features/details", "/api/v1/super-admin/organisations/*/subscription", "/api/v1/super-admin/organisations/*/usage", "/api/v1/super-admin/organisations/*/users", "/api/v1/super-admin/organisations/*/admins", "/api/v1/super-admin/organisations/*/invoices", "/api/v1/super-admin/organisations/*/audit-logs", "/api/v1/super-admin/organisations/*/rollouts", "/api/v1/super-admin/organisations/*/addons", "/api/v1/super-admin/organisations/*/billing/contacts", "/api/v1/super-admin/organisations/*/billing/notification-logs", "/api/v1/super-admin/organisations/*/stripe-config", "/api/v1/super-admin/rollouts", "/api/v1/super-admin/rollouts/*", "/api/v1/super-admin/plans", "/api/v1/super-admin/plans/*", "/api/v1/super-admin/plans/*/details", "/api/v1/super-admin/plans/*/entitlements", "/api/v1/super-admin/plans/*/entitlements/export", "/api/v1/super-admin/plans/*/pricing-tiers", "/api/v1/super-admin/billing/dunning-policy", "/api/v1/super-admin/billing/invoices", "/api/v1/super-admin/billing/invoices/recent", "/api/v1/super-admin/billing/invoices/export", "/api/v1/super-admin/billing/invoices/*", "/api/v1/super-admin/billing/invoices/*/adjustments", "/api/v1/super-admin/billing/invoices/*/disputes", "/api/v1/super-admin/billing/invoices/*/pdf", "/api/v1/super-admin/billing/revenue-report", "/api/v1/super-admin/billing/revenue-analytics", "/api/v1/super-admin/billing/addon-catalog", "/api/v1/super-admin/billing/notification-templates", "/api/v1/super-admin/billing/exports/*", "/api/v1/super-admin/billing/exports/*/download", "/api/v1/super-admin/permissions", "/api/v1/super-admin/permissions/**", "/api/v1/super-admin/email-templates").access(new WebExpressionAuthorizationManager(PLATFORM_READ))
                        .requestMatchers(HttpMethod.GET, "/api/v1/super-admin/audit-logs", "/api/v1/super-admin/hipaa-audit-logs").access(new WebExpressionAuthorizationManager(PLATFORM_READ))
                        .requestMatchers(HttpMethod.GET, "/api/v1/super-admin/system/health", "/api/v1/super-admin/system/uptime", "/api/v1/super-admin/system/incidents", "/api/v1/super-admin/system/global-audit-health", "/api/v1/super-admin/integrations", "/api/v1/super-admin/integrations/*", "/api/v1/super-admin/api-keys", "/api/v1/super-admin/notifications/history", "/api/v1/super-admin/notifications/unread-count", "/api/v1/super-admin/notifications/templates", "/api/v1/super-admin/notifications/triggers", "/api/v1/super-admin/notifications/triggers/metadata", "/api/v1/super-admin/jobs/scheduled", "/api/v1/super-admin/usage", "/api/v1/super-admin/security", "/api/v1/super-admin/organisations/*/backups", "/api/v1/super-admin/organisations/*/subscription/status", "/api/v1/super-admin/impersonation/policy", "/api/v1/super-admin/impersonation/sessions", "/api/v1/super-admin/users", "/api/v1/super-admin/users/*/roles-permissions", "/api/v1/super-admin/users/roles-permissions-matrix", "/api/v1/super-admin/users/roles-permissions-matrix/export", "/api/v1/super-admin/partners/*/access", "/api/v1/super-admin/features/catalog/**", "/api/v1/super-admin/cms/landing-page", "/api/v1/super-admin/cms/global-settings", "/api/v1/super-admin/cms/learning-hubs", "/api/v1/super-admin/cms/learning-hubs/*").access(new WebExpressionAuthorizationManager(PLATFORM_READ))
                        .requestMatchers(HttpMethod.POST, "/api/v1/super-admin/tenants/resolve").access(new WebExpressionAuthorizationManager(PLATFORM_READ))
                        // Super-admin write: create, update, provision, lock, backup — SUPER_ADMIN only.
                        .requestMatchers("/api/v1/super-admin/**").access(new WebExpressionAuthorizationManager(ROLE_PLATFORM_SUPER_ADMIN))
                        // Admin / system endpoints - PBAC (permissions)
                        .requestMatchers("/api/v1/admin/features/catalog/**", "/api/admin/features/catalog/**", "/api/v1/admin/plans/**", "/api/admin/plans/**", "/api/v1/admin/catalog/addons/**", "/api/admin/catalog/addons/**")
                        .access(new WebExpressionAuthorizationManager(ROLE_PLATFORM_SUPER_ADMIN))
                        // Staff directory reads (scheduling, library) — USER_VIEW for custom roles; writes still USER_MANAGE below
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/users", "/api/v1/admin/users/**")
                        .access(new WebExpressionAuthorizationManager(USER_VIEW))
                        // Client consent read/write for therapists & supervisors (PBAC on controller); not full admin
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/consents/clients/**")
                        .access(new WebExpressionAuthorizationManager(CONSENT_ADMIN_VIEW))
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/consents/clients/**")
                        .access(new WebExpressionAuthorizationManager(CONSENT_ADMIN_VIEW))
                        .requestMatchers("/api/v1/admin/**").access(new WebExpressionAuthorizationManager(USER_MANAGE))
                        .requestMatchers("/api/v1/system/**").access(new WebExpressionAuthorizationManager(USER_MANAGE))
                        .requestMatchers(HttpMethod.GET, "/api/v1/system-options/**").authenticated()
                        .requestMatchers("/api/v1/system-options/**").access(new WebExpressionAuthorizationManager(USER_MANAGE))
                        
                        // Role and Permission endpoints (v1) - tenant USER_MANAGE or platform super-admin
                        .requestMatchers("/api/v1/roles/**").access(new WebExpressionAuthorizationManager("(" + USER_MANAGE + " or " + ROLE_PLATFORM_SUPER_ADMIN + " or " + PLATFORM_MANAGE + ")"))
                        .requestMatchers("/api/v1/permissions/**").access(new WebExpressionAuthorizationManager("(" + USER_MANAGE + " or " + ROLE_PLATFORM_SUPER_ADMIN + " or " + PLATFORM_MANAGE + ")"))
                        
                        // Client endpoints (v1) - allow authenticated; fine-grained PBAC in controllers/services
                        .requestMatchers(HttpMethod.GET, "/api/v1/clients/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/clients/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clients/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clients/**").authenticated()
                        .requestMatchers("/api/v1/client-filters/**").authenticated()
                        
                        // Session endpoints (v1) - authenticated; PBAC at controller/service level
                        .requestMatchers("/api/v1/sessions/**").authenticated()
                        .requestMatchers("/api/v1/session-notes/**").authenticated()
                        
                        // Assessment endpoints (v1)
                        .requestMatchers("/api/v1/assessments/**").authenticated()
                        
                        // Task endpoints (v1)
                        .requestMatchers("/api/v1/tasks/**").authenticated()
                        .requestMatchers("/api/v1/checklists/**").authenticated()
                        
                        // Document endpoints (v1)
                        .requestMatchers("/api/v1/clients/*/documents/**").authenticated()
                        .requestMatchers("/api/v1/documents/**").authenticated()
                        .requestMatchers("/api/v1/notes/**").authenticated()
                        .requestMatchers("/api/v1/forms/**").authenticated()
                        .requestMatchers("/api/v1/library/**").authenticated()
                        
                        // Billing endpoints (v1) - PBAC via BILLING_VIEW/BILLING_MANAGE
                        .requestMatchers("/api/v1/billing/**").access(new WebExpressionAuthorizationManager(BILLING_MODULE_ACCESS))
                        .requestMatchers("/api/v1/stripe/**").authenticated() // Stripe payment endpoints (specific webhooks are permitAll above)
                        
                        // User endpoints (v1)
                        // Allow any authenticated user to access their own profile
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers("/api/v1/users/timezones").authenticated()
                        // Supervisor/therapist assignment list (scoped in service); not USER_MANAGE
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/supervisor-assignments", "/api/v1/users/supervisor-assignments/**")
                        .access(new WebExpressionAuthorizationManager(CONSENT_ADMIN_VIEW))
                        // Scheduling: read another staff profile when SESSION_VIEW/CLIENT_VIEW_TEAM (service enforces caseload)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/profile")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users")
                        .access(new WebExpressionAuthorizationManager(USER_VIEW))
                        // Managing other users controlled via USER_MANAGE
                        .requestMatchers("/api/v1/users/**").access(new WebExpressionAuthorizationManager(USER_MANAGE))
                        .requestMatchers("/api/v1/therapist-availability/**").authenticated()
                        
                        // Notification endpoints (v1)
                        .requestMatchers("/api/v1/notifications/templates").authenticated()
                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        
                        // AI endpoints (v1) - authenticated; PBAC at controller/service level
                        .requestMatchers("/api/v1/ai/**").authenticated()
                        
                        // Portal endpoints (v1) - client portal access (separate authentication)
                        // Public portal endpoints (no auth required)
                        .requestMatchers("/api/v1/portal/login",
                                        "/api/v1/portal/login-context",
                                        "/api/v1/portal/mfa/verify-login",
                                        "/api/v1/portal/refresh",
                                        "/api/v1/portal/activate", 
                                        "/api/v1/portal/activate/validate",
                                        "/api/v1/portal/forgot-password", 
                                        "/api/v1/portal/reset-password").permitAll()
                        // Protected portal endpoints - dynamic permission based access
                        .requestMatchers("/api/v1/portal/**").access(new WebExpressionAuthorizationManager(CLIENT_PORTAL_ACCESS))
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(jsonAccessDeniedHandler))
                .addFilterBefore(publicEndpointRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // SECURITY: Restrict origins in production and dev - use environment variable
        // For local development, allow localhost; for production/dev, use specific domains
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (isProdEnv() && (allowedOrigins == null || allowedOrigins.isEmpty())) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be set in production");
        }
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            java.util.List<String> origins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .toList();
            configuration.setAllowedOriginPatterns(origins);
        } else {
            configuration.setAllowedOriginPatterns(Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://localhost:5173",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:8080",
                    "http://127.0.0.1:5173",
                    "https://trappy-flow-frontend.vercel.app"
            ));
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        // Include common custom headers used by gateway/frontend and allow additional preflight headers.
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-API-Key",
                "X-Tenant-Schema",
                "X-Tenant-Subdomain",
                "X-Forwarded-Host",
                // Sent by Security settings / login to mark the current trusted browser.
                "X-Device-Trust-Token",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Content-Disposition",
                "Content-Length"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
