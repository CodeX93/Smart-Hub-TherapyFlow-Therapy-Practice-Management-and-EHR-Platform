package com.smart.therapy.flow.system.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

public final class SystemOptionSeedDefinitions {

    private static final String SEED_RESOURCE = "system-options/default-tenant-categories.json";
    private static final List<SeedCategory> CATEGORIES = loadCategories();

    private SystemOptionSeedDefinitions() {
    }

    public record SeedCategory(
            String categoryKey,
            String categoryName,
            String description,
            boolean isSystem,
            boolean isActive,
            List<SeedOption> options
    ) {
    }

    public record SeedOption(
            String optionKey,
            String optionLabel,
            int sortOrder,
            boolean isDefault,
            boolean isSystem,
            boolean isActive,
            BigDecimal price
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedFile(List<SeedCategory> categories) {
    }

    public static List<SeedCategory> defaultTenantCategories() {
        return CATEGORIES;
    }

    private static List<SeedCategory> loadCategories() {
        try (InputStream input = SystemOptionSeedDefinitions.class.getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing system option seed resource: " + SEED_RESOURCE);
            }
            SeedFile seedFile = new ObjectMapper().readValue(input, SeedFile.class);
            if (seedFile.categories() == null || seedFile.categories().isEmpty()) {
                throw new IllegalStateException("System option seed resource is empty: " + SEED_RESOURCE);
            }
            return List.copyOf(seedFile.categories());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load system option seed resource: " + SEED_RESOURCE, ex);
        }
    }
}
