package com.smart.therapy.flow.system.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.client.enums.ClientStatus;
import com.smart.therapy.flow.client.enums.ClientType;
import com.smart.therapy.flow.client.enums.Gender;
import com.smart.therapy.flow.client.enums.MaritalStatus;
import com.smart.therapy.flow.client.enums.ServiceFrequency;
import com.smart.therapy.flow.client.enums.ServiceType;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.system.dto.*;
import com.smart.therapy.flow.system.entity.OptionCategory;
import com.smart.therapy.flow.system.entity.SystemOption;
import com.smart.therapy.flow.system.enums.OptionCategorySourceType;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.task.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemOptionService {

    private final OptionCategoryRepository categoryRepository;
    private final SystemOptionRepository optionRepository;
    private final PermissionChecker permissionChecker;
    private final EntityManager entityManager;

    // ========== CATEGORY METHODS ==========

    // The includeInactive flag is part of the cache key: the admin screen's
    // unfiltered list must never be served to consumers from a shared entry.
    @Transactional(readOnly = true)
    @Cacheable(value = "systemOptions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('categories' + (#includeInactive ? ':all' : ''))")
    public List<OptionCategoryResponse> getCategories(boolean includeInactive) {
        List<OptionCategory> categories = categoryRepository.findByIsActive(true);
        return categories.stream()
                .map(category -> toCategoryResponseWithOptions(category, includeInactive))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "systemOptions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('category:' + #id + (#includeInactive ? ':all' : ''))")
    public OptionCategoryResponse getCategory(Long id, boolean includeInactive) {
        OptionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));
        return toCategoryResponseWithOptions(category, includeInactive);
    }

    @Transactional(readOnly = true)
    public OptionCategoryUsageResponse getCategoryUsage(Long id) {
        Objects.requireNonNull(id, "Category ID is required");
        OptionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));

        List<SystemOption> options = optionRepository.findByCategoryId(id);
        List<SystemOptionUsageResponse> optionUsage = options.stream()
                .map(this::toOptionUsageResponse)
                .toList();
        long totalReferences = optionUsage.stream()
                .mapToLong(SystemOptionUsageResponse::getTotalReferences)
                .sum();

        return OptionCategoryUsageResponse.builder()
                .categoryId(category.getId())
                .categoryKey(category.getCategoryKey())
                .categoryName(category.getCategoryName())
                .optionCount(options.size())
                .inUse(totalReferences > 0)
                .totalReferences(totalReferences)
                .optionUsage(optionUsage)
                .build();
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public OptionCategoryResponse createCategory(CreateOptionCategoryRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create option categories");

        // Validate category key format
        validateCategoryKey(request.getCategoryKey());

        // Check if category key already exists
        if (categoryRepository.findByCategoryKey(request.getCategoryKey()).isPresent()) {
            throw new BadRequestException("Category key already exists");
        }

        String normalizedCategoryKey = request.getCategoryKey().trim();
        OptionCategory category = OptionCategory.builder()
                .categoryKey(normalizedCategoryKey)
                .categoryName(request.getCategoryName().trim())
                .description(request.getDescription())
                .isSystem(SystemOptionPolicy.isWorkflowStrict(normalizedCategoryKey)
                        || (request.getIsSystem() != null ? request.getIsSystem() : false))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        OptionCategory saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public OptionCategoryResponse updateCategory(Long id, UpdateOptionCategoryRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Category ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update option categories");

        OptionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));

        if (category.getIsSystem() || SystemOptionPolicy.isWorkflowStrict(category.getCategoryKey())) {
            throw new ForbiddenException("Cannot modify system categories");
        }

        // Validate category key format if provided
        if (StringUtils.hasText(request.getCategoryKey())) {
            validateCategoryKey(request.getCategoryKey());
        }

        // Update fields
        if (StringUtils.hasText(request.getCategoryKey())) {
            // Check if new key conflicts with existing
            categoryRepository.findByCategoryKey(request.getCategoryKey())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new BadRequestException("Category key already exists");
                        }
                    });
            category.setCategoryKey(request.getCategoryKey().trim());
        }
        if (StringUtils.hasText(request.getCategoryName())) {
            category.setCategoryName(request.getCategoryName().trim());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        OptionCategory updated = categoryRepository.save(category);
        return toCategoryResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public void deleteCategory(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Category ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete option categories");

        OptionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));

        if (category.getIsSystem() || SystemOptionPolicy.isWorkflowStrict(category.getCategoryKey())) {
            throw new ForbiddenException("Cannot delete system categories");
        }

        // Check if category has options
        List<SystemOption> options = optionRepository.findByCategoryId(id);
        if (!options.isEmpty()) {
            throw new BadRequestException("Cannot delete category with existing options");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public List<SystemOptionResponse> reorderCategoryOptions(
            Long categoryId,
            ReorderCategoryOptionsRequest request,
            AuthPrincipal requester
    ) {
        Objects.requireNonNull(categoryId, "Category ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can reorder system options");

        OptionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));

        if (SystemOptionPolicy.isWorkflowStrict(category.getCategoryKey())) {
            throw new ForbiddenException("Cannot reorder options for workflow-strict categories");
        }

        List<SystemOption> categoryOptions = optionRepository.findByCategoryId(categoryId);
        validateReorderRequest(categoryOptions, request);

        Map<Long, Integer> sortOrderByOptionId = request.getOptions().stream()
                .collect(Collectors.toMap(
                        ReorderCategoryOptionsRequest.OptionOrderItem::getOptionId,
                        ReorderCategoryOptionsRequest.OptionOrderItem::getSortOrder
                ));

        for (SystemOption option : categoryOptions) {
            option.setSortOrder(sortOrderByOptionId.get(option.getId()));
        }
        optionRepository.saveAll(categoryOptions);

        return categoryOptions.stream()
                .sorted((a, b) -> {
                    int sortCompare = Integer.compare(
                            a.getSortOrder() != null ? a.getSortOrder() : 0,
                            b.getSortOrder() != null ? b.getSortOrder() : 0
                    );
                    if (sortCompare != 0) return sortCompare;
                    return a.getOptionLabel().compareToIgnoreCase(b.getOptionLabel());
                })
                .map(opt -> toOptionResponse(opt, category))
                .collect(Collectors.toList());
    }

    // ========== OPTION METHODS ==========

    @Transactional(readOnly = true)
    @Cacheable(value = "systemOptions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('options:' + (#categoryId != null ? #categoryId : 'all'))")
    public List<SystemOptionResponse> getOptions(Long categoryId) {
        if (categoryId != null) {
            List<SystemOption> options = optionRepository.findByCategoryId(categoryId);
            return options.stream()
                    .sorted((a, b) -> {
                        int sortCompare = Integer.compare(
                                a.getSortOrder() != null ? a.getSortOrder() : 0,
                                b.getSortOrder() != null ? b.getSortOrder() : 0
                        );
                        if (sortCompare != 0) return sortCompare;
                        return a.getOptionLabel().compareToIgnoreCase(b.getOptionLabel());
                    })
                    .map(this::toOptionResponse)
                    .collect(Collectors.toList());
        } else {
            List<SystemOption> options = optionRepository.findByIsActive(true);
            return options.stream()
                    .map(this::toOptionResponse)
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "systemOptions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('options:key:' + #categoryKey)")
    public List<SystemOptionResponse> getOptionsByCategoryKey(String categoryKey) {
        OptionCategory category = categoryRepository.findByCategoryKey(categoryKey)
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found: " + categoryKey));

        List<SystemOption> options = optionRepository.findByCategoryId(category.getId());
        return options.stream()
                .filter(SystemOption::getIsActive)
                .sorted((a, b) -> {
                    int sortCompare = Integer.compare(
                            a.getSortOrder() != null ? a.getSortOrder() : 0,
                            b.getSortOrder() != null ? b.getSortOrder() : 0
                    );
                    if (sortCompare != 0) return sortCompare;
                    return a.getOptionLabel().compareToIgnoreCase(b.getOptionLabel());
                })
                .map(opt -> toOptionResponse(opt, category))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "systemOptions", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('option:' + #id)")
    public SystemOptionResponse getOption(Long id) {
        SystemOption option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System option not found"));
        return toOptionResponse(option);
    }

    @Transactional(readOnly = true)
    public SystemOptionUsageResponse getOptionUsage(Long id) {
        Objects.requireNonNull(id, "Option ID is required");
        SystemOption option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System option not found"));
        return toOptionUsageResponse(option);
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public SystemOptionResponse createOption(CreateSystemOptionRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create system options");

        OptionCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));
        enforceOwnershipPolicyForOptionMutation(category, request.getOptionKey(), request.getOptionLabel());

        // Validate option key format
        validateOptionKey(request.getOptionKey());

        // Check if option key already exists in this category
        List<SystemOption> existingOptions = optionRepository.findByCategoryId(category.getId());
        boolean keyExists = existingOptions.stream()
                .anyMatch(opt -> opt.getOptionKey().equals(request.getOptionKey()));
        if (keyExists) {
            throw new BadRequestException("Option key already exists in this category");
        }

        // If setting as default, unset other defaults in this category
        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();
        if (isDefault) {
            List<SystemOption> defaultOptions = existingOptions.stream()
                    .filter(SystemOption::getIsDefault)
                    .collect(Collectors.toList());
            for (SystemOption opt : defaultOptions) {
                opt.setIsDefault(false);
                optionRepository.save(opt);
            }
        }

        SystemOption option = SystemOption.builder()
                .category(category)
                .optionKey(request.getOptionKey().trim())
                .optionLabel(request.getOptionLabel().trim())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isDefault(isDefault)
                .isSystem(request.getIsSystem() != null ? request.getIsSystem() : false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .price(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO)
                .build();

        SystemOption saved = optionRepository.save(option);
        return toOptionResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public SystemOptionResponse updateOption(Long id, UpdateSystemOptionRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Option ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update system options");

        SystemOption option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System option not found"));

        if (option.getIsSystem() || SystemOptionPolicy.isWorkflowStrict(option.getCategory().getCategoryKey())) {
            throw new ForbiddenException("Cannot modify system options");
        }

        // Update category if changed
        if (request.getCategoryId() != null && !request.getCategoryId().equals(option.getCategory().getId())) {
            OptionCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Option category not found"));
            option.setCategory(newCategory);
        }

        // Update fields
        if (StringUtils.hasText(request.getOptionKey())) {
            validateOptionKey(request.getOptionKey());
            String normalizedNewKey = request.getOptionKey().trim();
            List<SystemOption> existingOptions = optionRepository.findByCategoryId(option.getCategory().getId());
            boolean keyExists = existingOptions.stream()
                    .anyMatch(existing -> !existing.getId().equals(option.getId())
                            && existing.getOptionKey().equals(normalizedNewKey));
            if (keyExists) {
                throw new BadRequestException("Option key already exists in this category");
            }
            option.setOptionKey(request.getOptionKey().trim());
        }
        if (StringUtils.hasText(request.getOptionLabel())) {
            option.setOptionLabel(request.getOptionLabel().trim());
        }
        if (request.getSortOrder() != null) {
            option.setSortOrder(request.getSortOrder());
        }
        if (request.getIsDefault() != null) {
            // If setting as default, unset other defaults in this category
            if (request.getIsDefault()) {
                List<SystemOption> categoryOptions = optionRepository.findByCategoryId(option.getCategory().getId());
                for (SystemOption opt : categoryOptions) {
                    if (!opt.getId().equals(option.getId()) && opt.getIsDefault()) {
                        opt.setIsDefault(false);
                        optionRepository.save(opt);
                    }
                }
            }
            option.setIsDefault(request.getIsDefault());
        }
        if (request.getIsActive() != null) {
            option.setIsActive(request.getIsActive());
        }
        if (request.getPrice() != null) {
            option.setPrice(request.getPrice());
        }

        SystemOption updated = optionRepository.save(option);
        return toOptionResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "systemOptions", allEntries = true)
    public void deleteOption(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Option ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete system options");

        SystemOption option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System option not found"));

        if (option.getIsSystem() || SystemOptionPolicy.isWorkflowStrict(option.getCategory().getCategoryKey())) {
            throw new ForbiddenException("Cannot delete system options");
        }

        // Check if option is in use
        checkOptionUsage(option);

        optionRepository.delete(option);
    }

    /**
     * Check if an option is being used in clients, sessions, or tasks
     * Note: This is a basic check - actual usage depends on how options are stored in entities
     */
    private void checkOptionUsage(SystemOption option) {
        List<SystemOptionUsageResponse.UsageReference> references = collectOptionUsage(option);
        if (!references.isEmpty()) {
            String inUseLocations = references.stream()
                    .map(ref -> ref.getTable() + "." + ref.getColumn() + "(" + ref.getCount() + ")")
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Cannot delete option because it is currently used in: " + inUseLocations);
        }
    }

    private List<SystemOptionUsageResponse.UsageReference> collectOptionUsage(SystemOption option) {
        String optionKey = option.getOptionKey();
        String categoryKey = option.getCategory().getCategoryKey();
        List<SystemOptionUsageResponse.UsageReference> references = new ArrayList<>();

        switch (categoryKey) {
            case "preferred_language" -> addUsageReferenceIfInUse(references, "clients", "preferred_language", optionKey);
            case "client_stage" -> addUsageReferenceIfInUse(references, "clients", "stage", optionKey);
            case "client_status" -> addUsageReferenceIfInUse(references, "clients", "status", optionKey);
            case "client_type" -> addUsageReferenceIfInUse(references, "clients", "client_type", optionKey);
            case "gender" -> addUsageReferenceIfInUse(references, "clients", "gender", optionKey);
            case "marital_status" -> addUsageReferenceIfInUse(references, "clients", "marital_status", optionKey);
            case "service_type", "service_types" -> addUsageReferenceIfInUse(references, "clients", "service_type", optionKey);
            case "service_frequency" -> addUsageReferenceIfInUse(references, "clients", "service_frequency", optionKey);
            case "employment_status" -> addUsageReferenceIfInUse(references, "client_employment", "employment_status", optionKey);
            case "education_level" -> addUsageReferenceIfInUse(references, "client_employment", "education_level", optionKey);
            case "referral_sources" -> addUsageReferenceIfInUse(references, "client_referrals", "referral_source", optionKey);
            case "session_mode", "session_modes" -> addUsageReferenceIfInUse(references, "sessions", "session_mode", optionKey);
            case "session_status" -> addUsageReferenceIfInUse(references, "sessions", "status", optionKey);
            case "clinical_session_type", "session_type" -> addUsageReferenceIfInUse(references, "sessions", "session_type", optionKey);
            case "task_status" -> addUsageReferenceIfInUse(references, "tasks", "status", optionKey);
            case "task_types" -> addUsageReferenceIfInUse(references, "tasks", "task_type", optionKey);
            case "task_titles" -> addUsageReferenceIfInUse(references, "tasks", "title_key", optionKey);
            case "task_priority", "task_priorities" -> addUsageReferenceIfInUse(references, "tasks", "priority", optionKey);
            case "insurance_providers" -> addUsageReferenceIfInUse(references, "client_insurance", "insurance_provider", optionKey);
            case "insurance_types" -> addUsageReferenceIfInUse(references, "client_insurance", "insurance_type", optionKey);
            case "treatment_modalities" -> addUsageReferenceIfInUse(references, "clients", "treatment_modality", optionKey);
            case "practice_settings" -> addUsageReferenceIfInUse(references, "practice_configuration", "config_key", optionKey);
            default -> log.debug("No configured usage check for category: {}", categoryKey);
        }

        return references;
    }

    private void addUsageReferenceIfInUse(
            List<SystemOptionUsageResponse.UsageReference> references,
            String table,
            String column,
            String value
    ) {
        long count = countExactMatches(table, column, value);
        if (count > 0) {
            references.add(SystemOptionUsageResponse.UsageReference.builder()
                    .table(table)
                    .column(column)
                    .count(count)
                    .build());
        }
    }

    private long countExactMatches(String table, String column, String value) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :value");
        query.setParameter("value", value);
        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    private <E extends Enum<E>> void addEnumUsageReferenceIfInUse(
            List<SystemOptionUsageResponse.UsageReference> references,
            String table,
            String column,
            Class<E> enumType,
            SystemOption option
    ) {
        E constant = resolveEnumConstant(enumType, option);
        if (constant == null) {
            return;
        }
        addUsageReferenceIfInUse(references, table, column, constant.name());
    }

    private SystemOptionUsageResponse toOptionUsageResponse(SystemOption option) {
        List<SystemOptionUsageResponse.UsageReference> references = collectOptionUsage(option);
        long total = references.stream().mapToLong(SystemOptionUsageResponse.UsageReference::getCount).sum();

        return SystemOptionUsageResponse.builder()
                .optionId(option.getId())
                .optionKey(option.getOptionKey())
                .categoryKey(option.getCategory().getCategoryKey())
                .inUse(total > 0)
                .totalReferences(total)
                .references(references)
                .build();
    }

    private <E extends Enum<E>> E resolveEnumConstant(Class<E> enumType, SystemOption option) {
        String optionKey = option.getOptionKey();
        String optionLabel = option.getOptionLabel();
        String normalizedKeyName = normalizeAsEnumName(optionKey);
        String normalizedLabelName = normalizeAsEnumName(optionLabel);

        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(optionKey)
                    || constant.name().equalsIgnoreCase(optionLabel)
                    || constant.name().equalsIgnoreCase(normalizedKeyName)
                    || constant.name().equalsIgnoreCase(normalizedLabelName)
                    || matchesEnumMethodValue(constant, "getValue", optionKey, optionLabel)
                    || matchesEnumMethodValue(constant, "getDisplayName", optionKey, optionLabel)) {
                return constant;
            }
        }
        return null;
    }

    private boolean matchesEnumMethodValue(Enum<?> constant, String methodName, String optionKey, String optionLabel) {
        try {
            Object value = constant.getClass().getMethod(methodName).invoke(constant);
            if (!(value instanceof String textValue)) {
                return false;
            }
            return textValue.equalsIgnoreCase(optionKey) || textValue.equalsIgnoreCase(optionLabel);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private String normalizeAsEnumName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_");
        return normalized.toUpperCase();
    }

    /**
     * Validate option key format (alphanumeric, underscores, hyphens only)
     */
    private void validateOptionKey(String optionKey) {
        if (!StringUtils.hasText(optionKey)) {
            throw new BadRequestException("Option key is required");
        }
        if (optionKey.length() > 100) {
            throw new BadRequestException("Option key cannot exceed 100 characters");
        }
        if (!optionKey.matches("^[a-z0-9_-]+$")) {
            throw new BadRequestException("Option key must contain only lowercase letters, numbers, underscores, and hyphens");
        }
    }

    /**
     * Validate category key format (alphanumeric, underscores, hyphens only)
     */
    private void validateCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            throw new BadRequestException("Category key is required");
        }
        if (categoryKey.length() > 100) {
            throw new BadRequestException("Category key cannot exceed 100 characters");
        }
        if (!categoryKey.matches("^[a-z0-9_-]+$")) {
            throw new BadRequestException("Category key must contain only lowercase letters, numbers, underscores, and hyphens");
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private OptionCategoryResponse toCategoryResponse(OptionCategory category) {
        return OptionCategoryResponse.builder()
                .id(category.getId())
                .categoryKey(category.getCategoryKey())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .isSystem(category.getIsSystem())
                .isActive(category.getIsActive())
                .ownershipType(SystemOptionPolicy.ownershipOf(category.getCategoryKey()).name())
                .optionSource(OptionCategorySourceType.SYSTEM_OPTIONS.name())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .options(Collections.emptyList())
                .build();
    }

    private OptionCategoryResponse toCategoryResponseWithOptions(OptionCategory category, boolean includeInactive) {
        OptionCategoryResponse response = toCategoryResponse(category);

        if (category.getOptions() != null) {
            List<SystemOptionResponse> options = category.getOptions().stream()
                    .filter(opt -> includeInactive || opt.getIsActive())
                    .sorted((a, b) -> {
                        int sortCompare = Integer.compare(
                                a.getSortOrder() != null ? a.getSortOrder() : 0,
                                b.getSortOrder() != null ? b.getSortOrder() : 0
                        );
                        if (sortCompare != 0) return sortCompare;
                        return a.getOptionLabel().compareToIgnoreCase(b.getOptionLabel());
                    })
                    .map(opt -> toOptionResponse(opt, category))
                    .collect(Collectors.toList());
            response.setOptions(options);
        } else {
            response.setOptions(Collections.emptyList());
        }
        
        return response;
    }

    private SystemOptionResponse toOptionResponse(SystemOption option) {
        return toOptionResponse(option, option.getCategory());
    }

    private SystemOptionResponse toOptionResponse(SystemOption option, OptionCategory category) {
        return SystemOptionResponse.builder()
                .id(option.getId())
                .categoryId(category != null ? category.getId() : null)
                .categoryKey(category != null ? category.getCategoryKey() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .optionKey(option.getOptionKey())
                .optionLabel(option.getOptionLabel())
                .sortOrder(option.getSortOrder())
                .isDefault(option.getIsDefault())
                .isSystem(option.getIsSystem())
                .isActive(option.getIsActive())
                .price(option.getPrice())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }

    private void assertAdmin(AuthPrincipal principal, String message) {
        permissionChecker.requireConsentAdminModuleAccess(principal, message);
    }

    private void validateReorderRequest(List<SystemOption> categoryOptions, ReorderCategoryOptionsRequest request) {
        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new BadRequestException("Options list is required");
        }

        if (categoryOptions.isEmpty()) {
            throw new BadRequestException("Category has no options to reorder");
        }

        Set<Long> requestedIds = new HashSet<>();
        Set<Integer> requestedSortOrders = new HashSet<>();
        for (ReorderCategoryOptionsRequest.OptionOrderItem item : request.getOptions()) {
            if (!requestedIds.add(item.getOptionId())) {
                throw new BadRequestException("Duplicate option ID in request: " + item.getOptionId());
            }
            if (!requestedSortOrders.add(item.getSortOrder())) {
                throw new BadRequestException("Duplicate sort order in request: " + item.getSortOrder());
            }
        }

        Set<Long> categoryOptionIds = categoryOptions.stream()
                .map(SystemOption::getId)
                .collect(Collectors.toSet());

        if (requestedIds.size() != categoryOptionIds.size()) {
            throw new BadRequestException("Request must include all category options exactly once");
        }

        Set<Long> missingIds = new HashSet<>(categoryOptionIds);
        missingIds.removeAll(requestedIds);
        if (!missingIds.isEmpty()) {
            throw new BadRequestException("Missing option IDs for category: " + missingIds);
        }

        Set<Long> unknownIds = new HashSet<>(requestedIds);
        unknownIds.removeAll(categoryOptionIds);
        if (!unknownIds.isEmpty()) {
            throw new BadRequestException("Options do not belong to the category: " + unknownIds);
        }
    }

    @SuppressWarnings("unchecked")
    private void enforceOwnershipPolicyForOptionMutation(OptionCategory category, String optionKey, String optionLabel) {
        String categoryKey = category.getCategoryKey();
        if (!SystemOptionPolicy.isWorkflowStrict(categoryKey)) {
            return;
        }
        Class<? extends Enum<?>> enumClass = SystemOptionPolicy.enumBindingFor(categoryKey);
        if (enumClass == null) {
            throw new ForbiddenException("Workflow-strict category does not allow custom options");
        }

        String key = optionKey != null ? optionKey.trim() : "";
        String label = optionLabel != null ? optionLabel.trim() : "";
        boolean matches = false;
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(key)
                    || constant.name().equalsIgnoreCase(label)
                    || matchesEnumMethodValue(constant, "getValue", key, label)
                    || matchesEnumMethodValue(constant, "getDisplayName", key, label)) {
                matches = true;
                break;
            }
        }
        if (!matches) {
            throw new BadRequestException("Option is incompatible with workflow-strict category: " + categoryKey);
        }
    }
}




