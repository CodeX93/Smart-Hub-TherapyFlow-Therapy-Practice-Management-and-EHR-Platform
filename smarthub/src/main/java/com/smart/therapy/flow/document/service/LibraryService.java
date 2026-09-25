package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.ai.service.ClinicalTemplates;
import com.smart.therapy.flow.document.dto.LibraryBulkImportResponse;
import com.smart.therapy.flow.document.dto.LibraryConnectedEntriesBulkRequest;
import com.smart.therapy.flow.document.dto.LibraryConnectedEntryResponse;
import com.smart.therapy.flow.document.dto.LibraryConnectionBatchRequest;
import com.smart.therapy.flow.document.dto.LibraryConnectionBatchResponse;
import com.smart.therapy.flow.document.dto.LibraryConnectionRequest;
import com.smart.therapy.flow.document.dto.LibraryConnectionResponse;
import com.smart.therapy.flow.document.dto.LibraryConnectionUpdateRequest;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.entity.LibraryCategory;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.entity.LibraryEntryConnection;
import com.smart.therapy.flow.document.enums.ConnectionType;
import com.smart.therapy.flow.document.repository.LibraryCategoryRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryConnectionRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LibraryService {

    private static final Comparator<LibraryEntryConnection> CONNECTION_ORDER = Comparator
            .comparing(LibraryEntryConnection::getStrength, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LibraryEntryConnection::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private final LibraryCategoryRepository categoryRepository;
    private final LibraryEntryRepository entryRepository;
    private final LibraryEntryConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final LibraryTagService libraryTagService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public LibraryBulkImportResponse bulkCreateEntries(LibraryEntryBulkRequest request, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can perform bulk import");

        if (request.getCategoryId() == null) {
            throw new BadRequestException("Category id is required");
        }
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException("Entries list cannot be empty");
        }

        for (LibraryEntryBulkRequest.LibraryEntryBulkItem item : request.getEntries()) {
            if (item != null && StringUtils.hasText(item.getSubdomain()) && !StringUtils.hasText(item.getDomain())) {
                throw new BadRequestException("Subdomain requires domain");
            }
        }

        LibraryCategory tabCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));

        User creator = resolveUser(principal);
        Set<String> existingTitles = loadExistingTitles();
        Map<String, Long> categoryCache = buildCategoryCache();
        int categoriesCreated = 0;
        int connectionsCreated = 0;

        List<LibraryBulkImportResponse.ImportError> errors = new ArrayList<>();
        int total = request.getEntries().size();
        int successful = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < request.getEntries().size(); i++) {
            LibraryEntryBulkRequest.LibraryEntryBulkItem item = request.getEntries().get(i);

            try {
                validateBulkItem(item);
                String normalizedTitle = normalizeTitle(item.getTitle());
                if (existingTitles.contains(normalizedTitle)) {
                    skipped++;
                    errors.add(LibraryBulkImportResponse.ImportError.builder()
                            .row(i + 1)
                            .title(item.getTitle())
                            .error("Duplicate - entry with this title already exists")
                            .build());
                    continue;
                }

                LibraryEntry entry = LibraryEntry.builder()
                        .category(tabCategory)
                        .title(item.getTitle().trim())
                        .content(item.getContent())
                        .createdByUser(creator)
                        .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                        .isActive(true)
                        .usageCount(0)
                        .build();

                LibraryEntry saved = entryRepository.save(entry);
                if (item.getTags() != null && !item.getTags().isEmpty()) {
                    libraryTagService.assignTagsToEntry(saved, item.getTags());
                }

                existingTitles.add(normalizedTitle);
                successful++;

                if (StringUtils.hasText(item.getDomain())) {
                    ConnectionTargetResolution target = resolveConnectionTarget(
                            item.getDomain(),
                            item.getSubdomain(),
                            categoryCache,
                            creator,
                            existingTitles);
                    categoriesCreated += target.categoriesCreated();
                    if (tryCreateConnection(saved, target.entry(), creator)) {
                        connectionsCreated++;
                    }
                }
            } catch (Exception ex) {
                failed++;
                log.warn("Failed to import library entry row {}: {}", i + 1, ex.getMessage());
                errors.add(LibraryBulkImportResponse.ImportError.builder()
                        .row(i + 1)
                        .title(item != null ? item.getTitle() : null)
                        .error(ex.getMessage())
                        .build());
            }
        }

        return LibraryBulkImportResponse.builder()
                .total(total)
                .successful(successful)
                .skipped(skipped)
                .failed(failed)
                .categoriesCreated(categoriesCreated)
                .connectionsCreated(connectionsCreated)
                .errors(errors)
                .build();
    }

    private record ConnectionTargetResolution(LibraryEntry entry, int categoriesCreated) {
    }

    private ConnectionTargetResolution resolveConnectionTarget(
            String domain,
            String subdomain,
            Map<String, Long> categoryCache,
            User creator,
            Set<String> existingTitles) {
        CategoryResolution categoryResolution = resolveCategory(domain, subdomain, categoryCache);
        LibraryCategory targetCategory = categoryResolution.category();

        String connectionEntryTitle = StringUtils.hasText(subdomain) ? subdomain.trim() : domain.trim();
        Optional<LibraryEntry> existingInCategory = findEntryInCategoryByTitle(
                targetCategory.getId(),
                connectionEntryTitle);
        if (existingInCategory.isPresent()) {
            return new ConnectionTargetResolution(existingInCategory.get(), categoryResolution.createdCount());
        }

        String normalizedConnectionTitle = normalizeTitle(connectionEntryTitle);
        if (existingTitles.contains(normalizedConnectionTitle)) {
            LibraryEntry globalMatch = findEntryByTitle(connectionEntryTitle)
                    .orElseThrow(() -> new ConflictException(
                            "Connection target entry \"" + connectionEntryTitle + "\" already exists elsewhere"));
            return new ConnectionTargetResolution(globalMatch, categoryResolution.createdCount());
        }

        LibraryEntry connectionTarget = LibraryEntry.builder()
                .category(targetCategory)
                .title(connectionEntryTitle)
                .content(connectionEntryTitle)
                .createdByUser(creator)
                .sortOrder(0)
                .isActive(true)
                .usageCount(0)
                .build();
        LibraryEntry savedTarget = entryRepository.save(connectionTarget);
        existingTitles.add(normalizedConnectionTitle);
        return new ConnectionTargetResolution(savedTarget, categoryResolution.createdCount());
    }

    private Optional<LibraryEntry> findEntryInCategoryByTitle(Long categoryId, String title) {
        String normalized = normalizeTitle(title);
        return entryRepository.findByCategoryId(categoryId).stream()
                .filter(entry -> normalizeTitle(entry.getTitle()).equals(normalized))
                .findFirst();
    }

    private Optional<LibraryEntry> findEntryByTitle(String title) {
        String normalized = normalizeTitle(title);
        return entryRepository.findAll().stream()
                .filter(entry -> normalizeTitle(entry.getTitle()).equals(normalized))
                .findFirst();
    }

    private boolean tryCreateConnection(LibraryEntry fromEntry, LibraryEntry toEntry, User creator) {
        if (fromEntry == null || toEntry == null) {
            return false;
        }
        if (Objects.equals(fromEntry.getId(), toEntry.getId())) {
            return false;
        }
        if (connectionExists(fromEntry.getId(), toEntry.getId())) {
            return false;
        }

        LibraryEntryConnection connection = LibraryEntryConnection.builder()
                .fromEntry(fromEntry)
                .toEntry(toEntry)
                .connectionType(ConnectionType.RELATED)
                .strength(4)
                .description("Bulk import connection")
                .isActive(true)
                .createdByUser(creator)
                .build();
        connectionRepository.save(connection);
        return true;
    }

    private Set<String> loadExistingTitles() {
        Set<String> existingTitles = new HashSet<>();
        entryRepository.findAll().forEach(entry -> {
            if (StringUtils.hasText(entry.getTitle())) {
                existingTitles.add(normalizeTitle(entry.getTitle()));
            }
        });
        return existingTitles;
    }

    private Map<String, Long> buildCategoryCache() {
        Map<String, Long> cache = new LinkedHashMap<>();
        for (LibraryCategory category : categoryRepository.findAll()) {
            if (!Boolean.TRUE.equals(category.getIsActive())) {
                continue;
            }
            if (category.getParentCategory() == null) {
                cache.put(rootCategoryCacheKey(category.getName()), category.getId());
            } else {
                cache.put(childCategoryCacheKey(category.getParentCategory().getId(), category.getName()),
                        category.getId());
            }
        }
        return cache;
    }

    private record CategoryResolution(LibraryCategory category, int createdCount) {
    }

    private CategoryResolution resolveCategory(String domain, String subdomain, Map<String, Long> categoryCache) {
        if (!StringUtils.hasText(domain)) {
            throw new BadRequestException("Domain is required for connection targets");
        }

        String domainName = domain.trim();
        int created = 0;
        Long domainId = categoryCache.get(rootCategoryCacheKey(domainName));
        LibraryCategory domainCategory;

        if (domainId != null) {
            domainCategory = categoryRepository.findById(domainId)
                    .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));
        } else {
            domainCategory = categoryRepository.save(LibraryCategory.builder()
                    .name(domainName)
                    .sortOrder(0)
                    .isActive(true)
                    .build());
            categoryCache.put(rootCategoryCacheKey(domainName), domainCategory.getId());
            created++;
        }

        if (!StringUtils.hasText(subdomain)) {
            return new CategoryResolution(domainCategory, created);
        }

        String subdomainName = subdomain.trim();
        String childKey = childCategoryCacheKey(domainCategory.getId(), subdomainName);
        Long subdomainId = categoryCache.get(childKey);
        if (subdomainId != null) {
            LibraryCategory subdomainCategory = categoryRepository.findById(subdomainId)
                    .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));
            return new CategoryResolution(subdomainCategory, created);
        }

        LibraryCategory subdomainCategory = categoryRepository.save(LibraryCategory.builder()
                .name(subdomainName)
                .parentCategory(domainCategory)
                .sortOrder(0)
                .isActive(true)
                .build());
        categoryCache.put(childKey, subdomainCategory.getId());
        created++;
        return new CategoryResolution(subdomainCategory, created);
    }

    private String rootCategoryCacheKey(String name) {
        return "root::" + normalizeCategoryName(name);
    }

    private String childCategoryCacheKey(Long parentId, String name) {
        return parentId + "::" + normalizeCategoryName(name);
    }

    private String normalizeCategoryName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<LibraryConnectionResponse> getConnections(Long entryId) {
        List<LibraryEntryConnection> connections;
        if (entryId != null) {
            connections = mergeConnections(connectionRepository.findByFromEntry_Id(entryId),
                    connectionRepository.findByToEntry_Id(entryId));
            connections.removeIf(connection -> !Objects.equals(Boolean.TRUE, connection.getIsActive()));
        } else {
            connections = connectionRepository.findAll().stream()
                    .filter(connection -> Objects.equals(Boolean.TRUE, connection.getIsActive()))
                    .collect(Collectors.toList());
        }

        return connections.stream()
                .sorted(CONNECTION_ORDER)
                .map(this::toConnectionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LibraryConnectedEntryResponse> getConnectedEntries(Long entryId) {
        LibraryEntry source = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));

        List<LibraryEntryConnection> connections = mergeConnections(
                connectionRepository.findByFromEntry_Id(entryId),
                connectionRepository.findByToEntry_Id(entryId));

        return connections.stream()
                .filter(connection -> Boolean.TRUE.equals(connection.getIsActive()))
                .map(connection -> toConnectedEntryResponse(connection, source.getId()))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(LibraryConnectedEntryResponse::getConnectionStrength,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LibraryConnectedEntryResponse::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<LibraryConnectedEntryResponse>> getConnectedEntriesBulk(
            LibraryConnectedEntriesBulkRequest request) {
        List<Long> entryIds = request.getEntryIds();
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }

        List<LibraryEntryConnection> connections = connectionRepository
                .findByFromEntry_IdInOrToEntry_IdIn(entryIds, entryIds);

        Map<Long, List<LibraryConnectedEntryResponse>> result = new LinkedHashMap<>();
        Set<Long> distinctIds = new LinkedHashSet<>(entryIds);
        // Initialize map with empty lists to maintain ordering
        distinctIds.forEach(id -> result.put(id, new ArrayList<>()));

        for (LibraryEntryConnection connection : connections) {
            if (!Boolean.TRUE.equals(connection.getIsActive())) {
                continue;
            }
            Long fromId = safeId(connection.getFromEntry());
            Long toId = safeId(connection.getToEntry());

            if (fromId != null && distinctIds.contains(fromId)) {
                LibraryConnectedEntryResponse response = toConnectedEntryResponse(connection, fromId);
                appendConnectedEntry(result, fromId, response);
            }
            if (toId != null && distinctIds.contains(toId)) {
                LibraryConnectedEntryResponse response = toConnectedEntryResponse(connection, toId);
                appendConnectedEntry(result, toId, response);
            }
        }

        result.replaceAll((id, responses) -> responses.stream()
                .sorted(Comparator
                        .comparing(LibraryConnectedEntryResponse::getConnectionStrength,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LibraryConnectedEntryResponse::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList()));

        return result;
    }

    @Transactional
    public LibraryConnectionResponse createConnection(LibraryConnectionRequest request, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can create connections");
        LibraryEntry fromEntry = requireEntry(request.getFromEntryId());
        LibraryEntry toEntry = requireEntry(request.getToEntryId());

        if (Objects.equals(fromEntry.getId(), toEntry.getId())) {
            throw new BadRequestException("Cannot create a connection to the same entry");
        }

        if (connectionExists(fromEntry.getId(), toEntry.getId())) {
            throw new ConflictException("Connection already exists between these entries");
        }

        LibraryEntryConnection connection = LibraryEntryConnection.builder()
                .fromEntry(fromEntry)
                .toEntry(toEntry)
                .connectionType(Optional.ofNullable(request.getConnectionType())
                        .orElse(ConnectionType.RELATED))
                .strength(Optional.ofNullable(request.getStrength()).orElse(4))
                .description(request.getDescription())
                .isActive(true)
                .createdByUser(resolveUser(principal))
                .build();

        LibraryEntryConnection saved = connectionRepository.save(connection);
        return toConnectionResponse(saved);
    }

    @Transactional
    public LibraryConnectionBatchResponse createConnectionsBatch(LibraryConnectionBatchRequest request,
            AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can create connections");

        List<LibraryConnectionResponse> created = new ArrayList<>();
        List<LibraryConnectionBatchResponse.SkippedConnection> skipped = new ArrayList<>();

        for (LibraryConnectionRequest connectionRequest : request.getConnections()) {
            try {
                LibraryConnectionResponse response = createConnection(connectionRequest, principal);
                created.add(response);
            } catch (ConflictException conflict) {
                skipped.add(LibraryConnectionBatchResponse.SkippedConnection.builder()
                        .fromEntryId(connectionRequest.getFromEntryId())
                        .toEntryId(connectionRequest.getToEntryId())
                        .reason(conflict.getMessage())
                        .build());
            } catch (BadRequestException badRequest) {
                skipped.add(LibraryConnectionBatchResponse.SkippedConnection.builder()
                        .fromEntryId(connectionRequest.getFromEntryId())
                        .toEntryId(connectionRequest.getToEntryId())
                        .reason(badRequest.getMessage())
                        .build());
            }
        }

        return LibraryConnectionBatchResponse.builder()
                .total(request.getConnections().size())
                .created(created.size())
                .skipped(skipped.size())
                .createdConnections(created)
                .skippedConnections(skipped)
                .build();
    }

    @Transactional
    public LibraryConnectionResponse updateConnection(Long connectionId, LibraryConnectionUpdateRequest request,
            AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can update connections");

        LibraryEntryConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Library connection not found"));

        if (request.getFromEntryId() != null || request.getToEntryId() != null) {
            LibraryEntry fromEntry = request.getFromEntryId() != null ? requireEntry(request.getFromEntryId())
                    : connection.getFromEntry();
            LibraryEntry toEntry = request.getToEntryId() != null ? requireEntry(request.getToEntryId())
                    : connection.getToEntry();

            if (Objects.equals(fromEntry.getId(), toEntry.getId())) {
                throw new BadRequestException("Cannot connect entry to itself");
            }

            if (!Objects.equals(fromEntry.getId(), connection.getFromEntry().getId()) ||
                    !Objects.equals(toEntry.getId(), connection.getToEntry().getId())) {
                if (connectionExists(fromEntry.getId(), toEntry.getId())) {
                    throw new ConflictException("Connection already exists between these entries");
                }
                connection.setFromEntry(fromEntry);
                connection.setToEntry(toEntry);
            }
        }

        if (request.getConnectionType() != null) {
            connection.setConnectionType(request.getConnectionType());
        }
        if (request.getStrength() != null) {
            connection.setStrength(request.getStrength());
        }
        if (request.getDescription() != null) {
            connection.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            connection.setIsActive(request.getActive());
        }
        connection.setUpdatedAt(Instant.now());
        LibraryEntryConnection saved = connectionRepository.save(connection);
        return toConnectionResponse(saved);
    }

    @Transactional
    public void deleteConnection(Long connectionId, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can delete connections");
        if (!connectionRepository.existsById(connectionId)) {
            throw new ResourceNotFoundException("Library connection not found");
        }
        connectionRepository.deleteById(connectionId);
    }

    @Transactional
    public void deleteConnectionsForEntry(Long entryId, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can delete connections");
        LibraryEntry entry = requireEntry(entryId);

        List<LibraryEntryConnection> connections = mergeConnections(
                connectionRepository.findByFromEntry_Id(entry.getId()),
                connectionRepository.findByToEntry_Id(entry.getId()));

        if (!connections.isEmpty()) {
            connectionRepository.deleteAll(connections);
        }
    }

    private void appendConnectedEntry(Map<Long, List<LibraryConnectedEntryResponse>> result,
            Long sourceEntryId,
            LibraryConnectedEntryResponse response) {
        if (response == null) {
            return;
        }
        List<LibraryConnectedEntryResponse> entries = result.computeIfAbsent(sourceEntryId, key -> new ArrayList<>());
        boolean exists = entries.stream()
                .anyMatch(existing -> Objects.equals(existing.getConnectionId(), response.getConnectionId())
                        || Objects.equals(existing.getEntryId(), response.getEntryId()));
        if (!exists) {
            entries.add(response);
        }
    }

    private User resolveUser(AuthPrincipal principal) {
        return userRepository.findById(currentUserService.requireCurrentUser(principal).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean connectionExists(Long fromEntryId, Long toEntryId) {
        return connectionRepository.existsByFromEntry_IdAndToEntry_Id(fromEntryId, toEntryId) ||
                connectionRepository.existsByFromEntry_IdAndToEntry_Id(toEntryId, fromEntryId);
    }

    private LibraryEntry requireEntry(Long entryId) {
        return entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
    }

    private void assertAdminOrSupervisor(AuthPrincipal principal, String message) {
        permissionChecker.requireConsentAdminModuleAccess(principal, message);
    }

    private void validateBulkItem(LibraryEntryBulkRequest.LibraryEntryBulkItem item) {
        if (item == null) {
            throw new BadRequestException("Entry cannot be null");
        }
        if (!StringUtils.hasText(item.getTitle())) {
            throw new BadRequestException("Entry title is required");
        }
        if (!StringUtils.hasText(item.getContent())) {
            throw new BadRequestException("Entry content is required");
        }
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    private List<LibraryEntryConnection> mergeConnections(Collection<LibraryEntryConnection> first,
            Collection<LibraryEntryConnection> second) {
        Map<Long, LibraryEntryConnection> merged = new LinkedHashMap<>();
        for (LibraryEntryConnection connection : first) {
            merged.put(connection.getId(), connection);
        }
        for (LibraryEntryConnection connection : second) {
            merged.put(connection.getId(), connection);
        }
        return new ArrayList<>(merged.values());
    }

    private LibraryConnectionResponse toConnectionResponse(LibraryEntryConnection connection) {
        LibraryEntry from = connection.getFromEntry();
        LibraryEntry to = connection.getToEntry();
        User creator = connection.getCreatedByUser();
        return LibraryConnectionResponse.builder()
                .id(connection.getId())
                .fromEntryId(safeId(from))
                .fromEntryTitle(from != null ? from.getTitle() : null)
                .toEntryId(safeId(to))
                .toEntryTitle(to != null ? to.getTitle() : null)
                .connectionType(connection.getConnectionType() != null ? connection.getConnectionType()
                        : ConnectionType.RELATED)
                .strength(connection.getStrength())
                .description(connection.getDescription())
                .createdByUserId(creator != null ? creator.getId() : null)
                .createdByName(creator != null ? creator.getFullName() : null)
                .active(connection.getIsActive())
                .createdAt(connection.getCreatedAt())
                .updatedAt(connection.getUpdatedAt())
                .build();
    }

    private LibraryConnectedEntryResponse toConnectedEntryResponse(LibraryEntryConnection connection,
            Long sourceEntryId) {
        LibraryEntry from = connection.getFromEntry();
        LibraryEntry to = connection.getToEntry();

        LibraryEntry target = null;
        if (from != null && Objects.equals(from.getId(), sourceEntryId)) {
            target = to;
        } else if (to != null && Objects.equals(to.getId(), sourceEntryId)) {
            target = from;
        }

        if (target == null || !Boolean.TRUE.equals(target.getIsActive())) {
            return null;
        }

        LibraryCategory category = target.getCategory();

        return LibraryConnectedEntryResponse.builder()
                .connectionId(connection.getId())
                .fromEntryId(safeId(from))
                .toEntryId(safeId(to))
                .entryId(target.getId())
                .entryTitle(target.getTitle())
                .entryContent(target.getContent())
                .tags(target.getTags() != null ? target.getTags().stream()
                        .filter(entryTag -> entryTag != null && entryTag.getTag() != null)
                        .map(entryTag -> entryTag.getTag().getName())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .connectionType(connection.getConnectionType() != null ? connection.getConnectionType()
                        : ConnectionType.RELATED)
                .connectionStrength(connection.getStrength())
                .description(connection.getDescription())
                .active(connection.getIsActive())
                .createdAt(connection.getCreatedAt())
                .updatedAt(connection.getUpdatedAt())
                .build();
    }

    private Long safeId(LibraryEntry entry) {
        return entry != null ? entry.getId() : null;
    }

    // ========== CATEGORY METHODS ==========

    @Transactional(readOnly = true)
    @Cacheable(value = "library", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('categories')")
    public List<LibraryCategoryResponse> getCategories() {
        List<LibraryCategory> categories = categoryRepository.findAll();
        return categories.stream()
                .filter(cat -> Boolean.TRUE.equals(cat.getIsActive()))
                .sorted(Comparator.comparing(LibraryCategory::getSortOrder)
                        .thenComparing(LibraryCategory::getName))
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "library", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('category:' + #id)")
    public LibraryCategoryResponse getCategory(Long id) {
        LibraryCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));
        return toCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public LibraryCategoryResponse createCategory(
            com.smart.therapy.flow.document.dto.CreateLibraryCategoryRequest request, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can create categories");

        LibraryCategory parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
        }

        LibraryCategory category = LibraryCategory.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .parentCategory(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        LibraryCategory saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public LibraryCategoryResponse updateCategory(Long id,
            com.smart.therapy.flow.document.dto.CreateLibraryCategoryRequest request, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can update categories");

        LibraryCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));

        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getParentId() != null) {
            LibraryCategory parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        LibraryCategory updated = categoryRepository.save(category);
        return toCategoryResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public void deleteCategory(Long id, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can delete categories");

        LibraryCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));

        // Check if category has entries
        if (!category.getEntries().isEmpty()) {
            throw new BadRequestException("Cannot delete category with entries");
        }

        // Check if category has children
        if (!category.getSubcategories().isEmpty()) {
            throw new BadRequestException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(category);
    }

    // ========== ENTRY METHODS ==========

    @Transactional(readOnly = true)
    @Cacheable(value = "library", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('entries:' + (#categoryId != null ? #categoryId : 'all'))")
    public List<LibraryEntryResponse> getEntries(Long categoryId) {
        List<LibraryEntry> entries;
        if (categoryId != null) {
            entries = entryRepository.findByCategoryId(categoryId);
        } else {
            entries = entryRepository.findAll();
        }
        return entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getIsActive()))
                .sorted(Comparator
                        .comparing(LibraryEntry::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LibraryEntry::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toEntryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LibraryEntryWithConnectionsResponse> getEntriesWithConnections(Long categoryId) {
        List<LibraryEntryResponse> entries = getEntries(categoryId);
        if (entries.isEmpty()) {
            return List.of();
        }

        LibraryConnectedEntriesBulkRequest request = new LibraryConnectedEntriesBulkRequest();
        request.setEntryIds(entries.stream()
                .map(LibraryEntryResponse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        Map<Long, List<LibraryConnectedEntryResponse>> connectedByEntry = getConnectedEntriesBulk(request);

        return entries.stream()
                .map(entry -> LibraryEntryWithConnectionsResponse.builder()
                        .entry(entry)
                        .connectedEntries(connectedByEntry.getOrDefault(entry.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "library", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('entry:' + #id)")
    public LibraryEntryResponse getEntry(Long id) {
        LibraryEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
        return toEntryResponse(entry);
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public LibraryEntryResponse createEntry(com.smart.therapy.flow.document.dto.CreateLibraryEntryRequest request,
            AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can create entries");

        LibraryCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));

        // Check for duplicate title
        List<LibraryEntry> existingEntries = entryRepository.findAll();
        String normalizedRequestTitle = request.getTitle() != null ? request.getTitle().trim() : "";
        boolean duplicate = existingEntries.stream()
                .map(LibraryEntry::getTitle)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(title -> title.equalsIgnoreCase(normalizedRequestTitle));
        if (duplicate) {
            throw new ConflictException("Entry with title \"" + request.getTitle() + "\" already exists");
        }

        User creator = resolveUser(principal);
        LibraryEntry entry = LibraryEntry.builder()
                .category(category)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .createdByUser(creator)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .usageCount(0)
                .build();

        try {
            LibraryEntry saved = entryRepository.save(entry);
            
            // Assign tags using LibraryTagService
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                libraryTagService.assignTagsToEntry(saved, request.getTags());
            }
            
            return toEntryResponse(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("library_entries_title_unique")) {
                // Database unique constraint on title was violated
                throw new ConflictException("Entry with title \"" + request.getTitle() + "\" already exists");
            }
            if (msg != null && msg.toLowerCase().contains("library_entry_tags_pkey")) {
                throw new ConflictException("Duplicate tags are not allowed for the same entry");
            }
            // Fallback: generic conflict for other constraint violations
            throw new ConflictException("Unable to create library entry due to data constraint violation");
        } catch (DataAccessException ex) {
            throw new ConflictException("Unable to create library entry due to database constraints");
        }
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public LibraryEntryResponse updateEntry(Long id,
            com.smart.therapy.flow.document.dto.CreateLibraryEntryRequest request, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can update entries");

        LibraryEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));

        if (request.getCategoryId() != null && !request.getCategoryId().equals(entry.getCategory().getId())) {
            LibraryCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Library category not found"));
            entry.setCategory(category);
        }
        if (StringUtils.hasText(request.getTitle())) {
            entry.setTitle(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getContent())) {
            entry.setContent(request.getContent());
        }
        if (request.getTags() != null) {
            // Update tags using LibraryTagService - replaces all existing tags
            libraryTagService.updateEntryTags(entry, request.getTags());
        }
        if (request.getSortOrder() != null) {
            entry.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            entry.setIsActive(request.getIsActive());
        }

        LibraryEntry updated = entryRepository.save(entry);
        return toEntryResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public void deleteEntry(Long id, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can delete entries");

        if (!entryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Library entry not found");
        }
        entryRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "library", allEntries = true)
    public LibraryBulkDeleteResponse bulkDeleteEntries(LibraryBulkDeleteRequest request, AuthPrincipal principal) {
        assertAdminOrSupervisor(principal, "Only administrators and supervisors can bulk delete entries");

        List<Long> distinctIds = request.getEntryIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        int deleted = 0;
        List<LibraryBulkDeleteResponse.DeleteError> errors = new ArrayList<>();

        for (Long entryId : distinctIds) {
            try {
                if (!entryRepository.existsById(entryId)) {
                    errors.add(LibraryBulkDeleteResponse.DeleteError.builder()
                            .entryId(entryId)
                            .error("Library entry not found")
                            .build());
                    continue;
                }
                entryRepository.deleteById(entryId);
                deleted++;
            } catch (Exception ex) {
                log.warn("Failed to bulk delete library entry {}: {}", entryId, ex.getMessage());
                errors.add(LibraryBulkDeleteResponse.DeleteError.builder()
                        .entryId(entryId)
                        .error(ex.getMessage())
                        .build());
            }
        }

        return LibraryBulkDeleteResponse.builder()
                .total(distinctIds.size())
                .deleted(deleted)
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LibraryEntryResponse> searchEntries(String query, Long categoryId) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        String searchTerm = query.toLowerCase();
        List<LibraryEntry> allEntries = categoryId != null
                ? entryRepository.findByCategoryId(categoryId)
                : entryRepository.findAll();

        return allEntries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getIsActive()))
                .filter(entry ->
                // Match on title
                (entry.getTitle() != null && entry.getTitle().toLowerCase().contains(searchTerm)) ||
                // Match on content
                        (entry.getContent() != null && entry.getContent().toLowerCase().contains(searchTerm)) ||
                        // Match on tags - navigate through LibraryEntryTag to LibraryTag to get name
                        (entry.getTags() != null && entry.getTags().stream()
                                .anyMatch(entryTag -> entryTag != null && entryTag.getTag() != null 
                                        && entryTag.getTag().getName() != null
                                        && entryTag.getTag().getName().toLowerCase().contains(searchTerm)))
                        ||
                        // Match on category name (so queries like \"Anxiety Resources\" work)
                        (entry.getCategory() != null &&
                                entry.getCategory().getName() != null &&
                                entry.getCategory().getName().toLowerCase().contains(searchTerm)))
                .sorted(Comparator.comparing(LibraryEntry::getTitle))
                .map(this::toEntryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LibraryEntryResponse incrementUsage(Long id) {
        LibraryEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));

        entry.setUsageCount((entry.getUsageCount() != null ? entry.getUsageCount() : 0) + 1);
        LibraryEntry saved = entryRepository.save(entry);

        // Return the updated entry so callers (and the API) can see the new usageCount
        return toEntryResponse(saved);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private LibraryCategoryResponse toCategoryResponse(LibraryCategory category) {
        return LibraryCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .parentName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private LibraryEntryResponse toEntryResponse(LibraryEntry entry) {
        return LibraryEntryResponse.builder()
                .id(entry.getId())
                .categoryId(entry.getCategory() != null ? entry.getCategory().getId() : null)
                .categoryName(entry.getCategory() != null ? entry.getCategory().getName() : null)
                .title(entry.getTitle())
                .content(entry.getContent())
                .tags(entry.getTags() != null ? entry.getTags().stream()
                        .filter(entryTag -> entryTag != null && entryTag.getTag() != null)
                        .map(entryTag -> entryTag.getTag().getName())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .createdById(entry.getCreatedByUser() != null ? entry.getCreatedByUser().getId() : null)
                .createdByName(entry.getCreatedByUser() != null ? entry.getCreatedByUser().getFullName() : null)
                .isActive(entry.getIsActive())
                .sortOrder(entry.getSortOrder())
                .usageCount(entry.getUsageCount())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private static final Map<String, String> SESSION_NOTE_FIELD_MAPPING = Map.ofEntries(
            Map.entry("symptoms", "symptoms"),
            Map.entry("symptom", "symptoms"),
            Map.entry("intervention", "intervention"),
            Map.entry("interventions", "intervention"),
            Map.entry("sessionFocus", "sessionFocus"),
            Map.entry("session_focus", "sessionFocus"),
            Map.entry("shortTermGoals", "shortTermGoals"),
            Map.entry("short_term_goals", "shortTermGoals"),
            Map.entry("goals", "shortTermGoals"),
            Map.entry("progress", "progress"),
            Map.entry("remarks", "remarks"),
            Map.entry("recommendations", "recommendations")
    );

    @Transactional(readOnly = true)
    public SessionNoteFieldEntriesResponse getSessionNoteFieldEntries(String fieldName) {
        String normalizedField = SESSION_NOTE_FIELD_MAPPING.getOrDefault(fieldName.toLowerCase(), fieldName);

        List<LibraryEntryResponse> libraryEntries = new ArrayList<>();

        if (normalizedField != null) {
            List<LibraryEntry> entries = entryRepository.findAll();
            for (LibraryEntry entry : entries) {
                if (Boolean.TRUE.equals(entry.getIsActive()) && matchesFieldEntry(entry, normalizedField)) {
                    libraryEntries.add(toEntryResponse(entry));
                }
            }
        }

        List<SessionNoteFieldEntriesResponse.AiTemplateOption> aiOptions = new ArrayList<>();
        Map<String, Object> templates = ClinicalTemplates.getAllTemplates();

        for (String templateId : templates.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> template = (Map<String, Object>) templates.get(templateId);
            String fieldOptionsKey = normalizedField + "Options";

            @SuppressWarnings("unchecked")
            Map<String, Object> fieldOptions = (Map<String, Object>) template.get(fieldOptionsKey);

            if (fieldOptions != null) {
                for (Map.Entry<String, Object> optionEntry : fieldOptions.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> option = (Map<String, Object>) optionEntry.getValue();
                    aiOptions.add(SessionNoteFieldEntriesResponse.AiTemplateOption.builder()
                            .key(templateId + "_" + optionEntry.getKey())
                            .label((String) option.get("label"))
                            .template((String) option.get("template"))
                            .build());
                }
            }
        }

        return SessionNoteFieldEntriesResponse.builder()
                .fieldName(normalizedField)
                .libraryEntries(libraryEntries)
                .aiTemplateOptions(aiOptions)
                .build();
    }

    private boolean matchesFieldEntry(LibraryEntry entry, String fieldName) {
        if (entry.getTags() == null || entry.getTags().isEmpty()) {
            return false;
        }
        for (var tag : entry.getTags()) {
            if (tag != null && tag.getTag() != null) {
                String tagName = tag.getTag().getName().toLowerCase();
                if (tagName.equals(fieldName.toLowerCase()) || tagName.contains(fieldName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}



