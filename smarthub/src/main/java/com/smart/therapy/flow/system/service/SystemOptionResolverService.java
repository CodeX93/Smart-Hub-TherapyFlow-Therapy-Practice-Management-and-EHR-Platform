package com.smart.therapy.flow.system.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.dto.SystemOptionResponse;
import com.smart.therapy.flow.system.entity.OptionCategory;
import com.smart.therapy.flow.system.entity.SystemOption;
import com.smart.therapy.flow.system.enums.OptionCategoryOwnershipType;
import com.smart.therapy.flow.system.enums.OptionCategorySourceType;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemOptionResolverService {

    private final OptionCategoryRepository categoryRepository;
    private final SystemOptionRepository optionRepository;

    public OptionCategoryResponse resolveCategoryWithOptions(String categoryKey) {
        Objects.requireNonNull(categoryKey, "Category key is required");
        Optional<OptionCategory> categoryOpt = findCategory(categoryKey);
        Class<? extends Enum<?>> enumBinding = SystemOptionPolicy.enumBindingFor(categoryKey);
        OptionCategoryOwnershipType ownership = SystemOptionPolicy.ownershipOf(categoryKey);

        if (categoryOpt.isPresent()) {
            OptionCategory category = categoryOpt.get();
            List<SystemOption> dbOptions = optionRepository.findByCategoryId(category.getId()).stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsActive()))
                    .sorted(Comparator.comparing((SystemOption o) -> o.getSortOrder() != null ? o.getSortOrder() : 0)
                            .thenComparing(SystemOption::getOptionLabel, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (!dbOptions.isEmpty()) {
                return OptionCategoryResponse.builder()
                        .id(category.getId())
                        .categoryKey(category.getCategoryKey())
                        .categoryName(category.getCategoryName())
                        .description(category.getDescription())
                        .isSystem(category.getIsSystem())
                        .isActive(category.getIsActive())
                        .ownershipType(ownership.name())
                        .optionSource(OptionCategorySourceType.SYSTEM_OPTIONS.name())
                        .createdAt(category.getCreatedAt())
                        .updatedAt(category.getUpdatedAt())
                        .options(dbOptions.stream().map(this::toOptionResponse).toList())
                        .build();
            }
        }

        if (enumBinding != null) {
            String displayName = toTitleCase(categoryKey.replace('_', ' '));
            OptionCategory category = categoryOpt.orElse(null);
            return OptionCategoryResponse.builder()
                    .id(category != null ? category.getId() : null)
                    .categoryKey(categoryKey)
                    .categoryName(category != null && StringUtils.hasText(category.getCategoryName())
                            ? category.getCategoryName() : displayName)
                    .description(category != null ? category.getDescription() : null)
                    .isSystem(true)
                    .isActive(true)
                    .ownershipType(ownership.name())
                    .optionSource(OptionCategorySourceType.ENUM_VIRTUAL.name())
                    .createdAt(category != null ? category.getCreatedAt() : null)
                    .updatedAt(category != null ? category.getUpdatedAt() : null)
                    .options(buildVirtualOptions(categoryKey, enumBinding))
                    .build();
        }

        OptionCategory category = categoryOpt.orElseThrow(
                () -> new ResourceNotFoundException("Option category not found: " + categoryKey));
        return OptionCategoryResponse.builder()
                .id(category.getId())
                .categoryKey(category.getCategoryKey())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .isSystem(category.getIsSystem())
                .isActive(category.getIsActive())
                .ownershipType(ownership.name())
                .optionSource(OptionCategorySourceType.SYSTEM_OPTIONS.name())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .options(new ArrayList<>())
                .build();
    }

    public <E extends Enum<E>> E resolveEnumValue(String categoryKey, String input, Class<E> enumClass) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String normalizedInput = input.trim();
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalizedInput)) {
                return constant;
            }
            if (matchesMethodValue(constant, "getValue", normalizedInput)
                    || matchesMethodValue(constant, "getDisplayName", normalizedInput)) {
                return constant;
            }
        }

        Optional<OptionCategory> category = findCategory(categoryKey);
        if (category.isPresent()) {
            List<SystemOption> options = optionRepository.findByCategoryId(category.get().getId());
            Optional<SystemOption> match = options.stream()
                    .filter(opt -> opt.getOptionKey().equalsIgnoreCase(normalizedInput)
                            || opt.getOptionLabel().equalsIgnoreCase(normalizedInput))
                    .findFirst();
            if (match.isPresent()) {
                String optionKey = match.get().getOptionKey();
                for (E constant : enumClass.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(optionKey)
                            || matchesMethodValue(constant, "getValue", optionKey)
                            || matchesMethodValue(constant, "getDisplayName", optionKey)) {
                        return constant;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve user input (option key or label) to the canonical option key for a category.
     * Falls back to enum name normalization when the category has an enum binding.
     */
    public String resolveOptionKey(String categoryKey, String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String normalizedInput = SystemOptionKeyMatcher.normalize(input);
        OptionCategoryResponse category = resolveCategoryWithOptions(categoryKey);
        if (category.getOptions() != null) {
            for (SystemOptionResponse option : category.getOptions()) {
                String optionKey = option.getOptionKey();
                String optionLabel = option.getOptionLabel();
                if (SystemOptionKeyMatcher.normalize(optionKey).equals(normalizedInput)
                        || (StringUtils.hasText(optionLabel)
                            && (SystemOptionKeyMatcher.normalize(optionLabel).equals(normalizedInput)
                                || optionLabel.equalsIgnoreCase(input.trim())))) {
                    // Always persist the canonical lowercase/underscore form.
                    return SystemOptionKeyMatcher.normalize(optionKey);
                }
            }
        }

        Class<? extends Enum<?>> enumBinding = SystemOptionPolicy.enumBindingFor(categoryKey);
        if (enumBinding != null) {
            for (Enum<?> constant : enumBinding.getEnumConstants()) {
                if (SystemOptionKeyMatcher.normalize(constant.name()).equals(normalizedInput)
                        || matchesMethodValue(constant, "getValue", input.trim())
                        || matchesMethodValue(constant, "getDisplayName", input.trim())) {
                    return normalizeKey(constant.name());
                }
            }
        }
        return null;
    }

    public String resolveOptionLabel(String categoryKey, String optionKey) {
        if (!StringUtils.hasText(optionKey)) {
            return null;
        }
        OptionCategoryResponse category = resolveCategoryWithOptions(categoryKey);
        if (category.getOptions() != null) {
            for (SystemOptionResponse option : category.getOptions()) {
                if (option.getOptionKey().equalsIgnoreCase(optionKey.trim())) {
                    return option.getOptionLabel();
                }
            }
        }
        return toTitleCase(optionKey.trim().replace('_', ' '));
    }

    public String requireOptionKey(String categoryKey, String input) {
        String key = resolveOptionKey(categoryKey, input);
        if (key == null) {
            throw new BadRequestException("Invalid value for " + categoryKey + ": '" + input + "'");
        }
        return key;
    }

    public String parseOptionKey(String categoryKey, String input, String fallbackKey) {
        if (!StringUtils.hasText(input)) {
            if (StringUtils.hasText(fallbackKey)) {
                return fallbackKey;
            }
            String defaultKey = resolveDefaultOptionKey(categoryKey);
            return defaultKey != null ? defaultKey : fallbackKey;
        }
        String key = resolveOptionKey(categoryKey, input);
        if (key != null) {
            return key;
        }
        if (StringUtils.hasText(fallbackKey)) {
            return fallbackKey;
        }
        String defaultKey = resolveDefaultOptionKey(categoryKey);
        return defaultKey != null ? defaultKey : input.trim().toLowerCase(Locale.ROOT);
    }

    public String resolveDefaultOptionKey(String categoryKey) {
        OptionCategoryResponse category = resolveCategoryWithOptions(categoryKey);
        if (category.getOptions() != null) {
            Optional<SystemOptionResponse> defaultOption = category.getOptions().stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsDefault()))
                    .findFirst();
            if (defaultOption.isPresent()) {
                return defaultOption.get().getOptionKey();
            }
            if (!category.getOptions().isEmpty()) {
                return category.getOptions().get(0).getOptionKey();
            }
        }
        return null;
    }

    private Optional<OptionCategory> findCategory(String categoryKey) {
        for (String lookupKey : SystemOptionCategories.categoryLookupKeys(categoryKey)) {
            Optional<OptionCategory> category = categoryRepository.findByCategoryKey(lookupKey);
            if (category.isPresent()) {
                return category;
            }
        }
        return Optional.empty();
    }

    private List<SystemOptionResponse> buildVirtualOptions(String categoryKey, Class<? extends Enum<?>> enumBinding) {
        Enum<?>[] constants = enumBinding.getEnumConstants();
        List<SystemOptionResponse> options = new ArrayList<>(constants.length);
        int sort = 0;
        for (Enum<?> constant : constants) {
            options.add(SystemOptionResponse.builder()
                    .id(null)
                    .categoryId(null)
                    .categoryKey(categoryKey)
                    .categoryName(toTitleCase(categoryKey.replace('_', ' ')))
                    .optionKey(normalizeKey(constant.name()))
                    .optionLabel(resolveBestLabel(constant))
                    .sortOrder(sort++)
                    .isDefault(false)
                    .isSystem(true)
                    .isActive(true)
                    .price(java.math.BigDecimal.ZERO)
                    .build());
        }
        return options;
    }

    private SystemOptionResponse toOptionResponse(SystemOption option) {
        OptionCategory category = option.getCategory();
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

    private String resolveBestLabel(Enum<?> constant) {
        String displayName = invokeMethod(constant, "getDisplayName");
        if (StringUtils.hasText(displayName)) {
            return displayName;
        }
        String value = invokeMethod(constant, "getValue");
        if (StringUtils.hasText(value)) {
            return value;
        }
        return toTitleCase(constant.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    private String invokeMethod(Enum<?> constant, String methodName) {
        try {
            Object value = constant.getClass().getMethod(methodName).invoke(constant);
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private boolean matchesMethodValue(Enum<?> constant, String methodName, String input) {
        String value = invokeMethod(constant, methodName);
        return value != null && value.equalsIgnoreCase(input);
    }

    private String normalizeKey(String enumName) {
        return enumName.toLowerCase(Locale.ROOT);
    }

    private String toTitleCase(String input) {
        return java.util.Arrays.stream(input.split("\\s+"))
                .filter(StringUtils::hasText)
                .map(token -> token.substring(0, 1).toUpperCase(Locale.ROOT)
                        + token.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }
}
