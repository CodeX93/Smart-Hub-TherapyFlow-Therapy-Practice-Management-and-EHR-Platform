package com.smart.therapy.flow.common.config;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration.
 * Provides the current user ID for @CreatedBy and @LastModifiedBy annotations.
 */
@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(0L);
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof AuthPrincipal) {
                AuthPrincipal auth = (AuthPrincipal) principal;
                // Important for schema-per-tenant setup:
                // auditing must be schema-agnostic and must never call tenant-scoped repositories.
                // Using AuthIdentity id avoids "relation users does not exist" during public-schema writes
                // (e.g. auth session creation during login).
                return Optional.ofNullable(auth.getAuthId()).or(() -> Optional.of(0L));
            }
            if (principal instanceof String && "anonymousUser".equals(principal)) {
                return Optional.of(0L);
            }

            return Optional.of(0L);
        };
    }
}
