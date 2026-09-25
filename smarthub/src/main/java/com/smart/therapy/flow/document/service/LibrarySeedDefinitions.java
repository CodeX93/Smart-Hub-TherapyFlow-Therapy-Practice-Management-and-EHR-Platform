package com.smart.therapy.flow.document.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class LibrarySeedDefinitions {

    private static final String SEED_RESOURCE = "library/default-tenant-library.json";
    private static final LibrarySeedFile SEED = loadSeed();

    private LibrarySeedDefinitions() {
    }

    public record SeedCategory(
            int sourceId,
            String name,
            String description,
            Integer parentSourceId,
            int sortOrder,
            boolean isActive
    ) {
    }

    public record SeedEntry(
            int sourceId,
            int categorySourceId,
            String title,
            String content,
            int sortOrder,
            int usageCount,
            boolean isActive
    ) {
    }

    public record SeedConnection(
            int fromSourceId,
            int toSourceId,
            String connectionType,
            int strength,
            String description,
            boolean isActive
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LibrarySeedFile(
            List<SeedCategory> categories,
            List<SeedEntry> entries,
            List<SeedConnection> connections
    ) {
    }

    public static List<SeedCategory> categories() {
        return SEED.categories();
    }

    public static List<SeedEntry> entries() {
        return SEED.entries();
    }

    public static List<SeedConnection> connections() {
        return SEED.connections();
    }

    private static LibrarySeedFile loadSeed() {
        try (InputStream input = LibrarySeedDefinitions.class.getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing library seed resource: " + SEED_RESOURCE);
            }
            LibrarySeedFile seedFile = new ObjectMapper().readValue(input, LibrarySeedFile.class);
            if (seedFile.categories() == null || seedFile.categories().isEmpty()) {
                throw new IllegalStateException("Library seed resource has no categories: " + SEED_RESOURCE);
            }
            return seedFile;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load library seed resource: " + SEED_RESOURCE, ex);
        }
    }
}
