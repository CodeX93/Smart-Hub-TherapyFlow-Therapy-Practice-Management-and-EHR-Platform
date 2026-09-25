package com.smart.therapy.flow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        // Enable API versioning via path prefix
        // All controllers should use @RequestMapping("/api/v1/...")
        // This allows for future versioning: /api/v2/, /api/v3/, etc.
        // Note: setUseTrailingSlashMatch() is deprecated in Spring 6.0
        // Trailing slash matching is disabled by default in Spring 6.0+
    }
}

