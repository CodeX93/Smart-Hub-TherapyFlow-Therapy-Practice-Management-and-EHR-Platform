package com.smart.therapy.flow.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration for storage service selection.
 * Validates that exactly one storage type is configured.
 */
@Configuration
@Slf4j
public class StorageServiceConfig {

    private final Environment environment;

    @Value("${app.storage.type:azure}")
    private String storageType;

    public StorageServiceConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateStorageConfiguration() {
        String validTypes = "local, s3, or azure";

        if (storageType == null || storageType.trim().isEmpty()) {
            throw new IllegalStateException("Storage type must be configured as one of: " + validTypes);
        }

        String normalizedType = storageType.toLowerCase().trim();

        if (!normalizedType.equals("local") &&
            !normalizedType.equals("s3") &&
            !normalizedType.equals("azure")) {
            throw new IllegalStateException(
                    "Invalid storage type '" + storageType + "'. Must be one of: " + validTypes
            );
        }

        if (isProduction() && !normalizedType.equals("azure")) {
            throw new IllegalStateException(
                    "Production storage must use Azure Blob Storage; configured type was '" + normalizedType + "'"
            );
        }

        log.info("Storage service configured: {}", normalizedType);

        // Log configuration requirements based on type
        switch (normalizedType) {
            case "local" -> log.info("Local storage requires: app.storage.local.path");
            case "s3" -> log.info("S3 storage requires: aws.s3.bucket, aws.s3.region, and optionally aws.s3.access-key/secret-key");
            case "azure" -> log.info("Azure storage requires: azure.storage.connection-string, azure.storage.container-name");
        }
    }

    private boolean isProduction() {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        String appEnv = environment.getProperty("app.env", "");
        return productionProfile
                || "prod".equalsIgnoreCase(appEnv)
                || "production".equalsIgnoreCase(appEnv);
    }
}

