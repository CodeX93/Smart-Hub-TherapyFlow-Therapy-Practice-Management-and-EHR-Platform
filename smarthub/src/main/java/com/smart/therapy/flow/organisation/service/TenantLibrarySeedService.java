package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.document.entity.LibraryCategory;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.entity.LibraryEntryConnection;
import com.smart.therapy.flow.document.enums.ConnectionType;
import com.smart.therapy.flow.document.repository.LibraryCategoryRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryConnectionRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryRepository;
import com.smart.therapy.flow.document.service.LibrarySeedDefinitions;
import com.smart.therapy.flow.document.service.LibrarySeedDefinitions.SeedCategory;
import com.smart.therapy.flow.document.service.LibrarySeedDefinitions.SeedConnection;
import com.smart.therapy.flow.document.service.LibrarySeedDefinitions.SeedEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Seeds tenant-scoped clinical library categories, entries, and connections from legacy ClientHub defaults.
 * Idempotent: skips when categories and entries are already present.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantLibrarySeedService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final LibraryCategoryRepository categoryRepository;
    private final LibraryEntryRepository entryRepository;
    private final LibraryEntryConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    public int seedDefaults(Long organisationId, String schemaName) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return 0;
        }

        Integer seeded = tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            long categoryCount = categoryRepository.count();
            long entryCount = entryRepository.count();
            if (categoryCount > 0 && entryCount > 0) {
                return 0;
            }

            int created = 0;
            Map<Integer, Long> categoryIdBySource = new HashMap<>();

            if (categoryCount == 0) {
                created += seedCategories(categoryIdBySource);
            } else {
                resolveExistingCategoryIds(categoryIdBySource);
            }

            if (entryCount == 0) {
                Optional<User> seedUser = userRepository.findAll().stream().findFirst();
                if (seedUser.isEmpty()) {
                    log.warn(
                            "Library categories seeded but entries deferred for org {} (schema {}): no tenant user yet",
                            organisationId,
                            schemaName);
                    return created;
                }
                Map<Integer, Long> entryIdBySource = new HashMap<>();
                created += seedEntries(categoryIdBySource, entryIdBySource, seedUser.get());
                created += seedConnections(entryIdBySource, seedUser.get());
            }

            return created;
        });

        if (seeded != null && seeded > 0) {
            log.info("Seeded {} default library record(s) for org {} (schema {})", seeded, organisationId, schemaName);
        }
        return seeded != null ? seeded : 0;
    }

    private int seedCategories(Map<Integer, Long> categoryIdBySource) {
        int created = 0;
        List<String> rootInsertOrder = List.of(
                "Session Focus",
                "Symptoms",
                "Short-term Goals",
                "Interventions",
                "Progress",
                "Anxiety",
                "Depression");
        Map<String, Integer> orderIndex = new HashMap<>();
        for (int i = 0; i < rootInsertOrder.size(); i++) {
            orderIndex.put(rootInsertOrder.get(i), i);
        }

        List<SeedCategory> roots = LibrarySeedDefinitions.categories().stream()
                .filter(category -> category.parentSourceId() == null)
                .sorted((left, right) -> Integer.compare(
                        orderIndex.getOrDefault(left.name(), Integer.MAX_VALUE),
                        orderIndex.getOrDefault(right.name(), Integer.MAX_VALUE)))
                .toList();
        for (SeedCategory seed : roots) {
            created += saveCategory(seed, null, categoryIdBySource);
        }
        List<SeedCategory> children = LibrarySeedDefinitions.categories().stream()
                .filter(category -> category.parentSourceId() != null)
                .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                .toList();
        for (SeedCategory seed : children) {
            LibraryCategory parent = Optional.ofNullable(categoryIdBySource.get(seed.parentSourceId()))
                    .flatMap(categoryRepository::findById)
                    .orElse(null);
            created += saveCategory(seed, parent, categoryIdBySource);
        }
        return created;
    }

    private int saveCategory(SeedCategory seed, LibraryCategory parent, Map<Integer, Long> categoryIdBySource) {
        LibraryCategory category = categoryRepository.save(LibraryCategory.builder()
                .name(seed.name())
                .description(seed.description())
                .parentCategory(parent)
                .sortOrder(seed.sortOrder())
                .isActive(seed.isActive())
                .build());
        categoryIdBySource.put(seed.sourceId(), category.getId());
        return 1;
    }

    private void resolveExistingCategoryIds(Map<Integer, Long> categoryIdBySource) {
        for (SeedCategory seed : LibrarySeedDefinitions.categories()) {
            categoryRepository.findByName(seed.name())
                    .ifPresent(category -> categoryIdBySource.put(seed.sourceId(), category.getId()));
        }
    }

    private int seedEntries(Map<Integer, Long> categoryIdBySource, Map<Integer, Long> entryIdBySource, User seedUser) {
        int created = 0;
        for (SeedEntry seed : LibrarySeedDefinitions.entries()) {
            Long categoryId = categoryIdBySource.get(seed.categorySourceId());
            if (categoryId == null) {
                continue;
            }
            LibraryCategory category = categoryRepository.findById(categoryId)
                    .orElse(null);
            if (category == null) {
                continue;
            }
            LibraryEntry entry = entryRepository.save(LibraryEntry.builder()
                    .category(category)
                    .title(seed.title())
                    .content(seed.content())
                    .createdByUser(seedUser)
                    .sortOrder(seed.sortOrder())
                    .usageCount(0)
                    .isActive(seed.isActive())
                    .build());
            entryIdBySource.put(seed.sourceId(), entry.getId());
            created++;
        }
        return created;
    }

    private int seedConnections(Map<Integer, Long> entryIdBySource, User seedUser) {
        int created = 0;
        for (SeedConnection seed : LibrarySeedDefinitions.connections()) {
            Long fromId = entryIdBySource.get(seed.fromSourceId());
            Long toId = entryIdBySource.get(seed.toSourceId());
            if (fromId == null || toId == null) {
                continue;
            }
            LibraryEntry fromEntry = entryRepository.findById(fromId).orElse(null);
            LibraryEntry toEntry = entryRepository.findById(toId).orElse(null);
            if (fromEntry == null || toEntry == null) {
                continue;
            }
            if (connectionRepository.existsByFromEntry_IdAndToEntry_Id(fromId, toId)) {
                continue;
            }
            connectionRepository.save(LibraryEntryConnection.builder()
                    .fromEntry(fromEntry)
                    .toEntry(toEntry)
                    .connectionType(mapConnectionType(seed.connectionType()))
                    .strength(seed.strength())
                    .description(seed.description())
                    .isActive(seed.isActive())
                    .createdByUser(seedUser)
                    .build());
            created++;
        }
        return created;
    }

    private ConnectionType mapConnectionType(String value) {
        if (value == null || value.isBlank() || "relates_to".equalsIgnoreCase(value)) {
            return ConnectionType.RELATED;
        }
        try {
            return ConnectionType.fromValue(value);
        } catch (RuntimeException ex) {
            return ConnectionType.RELATED;
        }
    }
}
