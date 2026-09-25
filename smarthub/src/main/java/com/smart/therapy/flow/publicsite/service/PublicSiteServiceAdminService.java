package com.smart.therapy.flow.publicsite.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.publicsite.dto.CreatePublicSiteServiceRequest;
import com.smart.therapy.flow.publicsite.dto.PublicSiteServiceResponse;
import com.smart.therapy.flow.publicsite.dto.UpdatePublicSiteServiceRequest;
import com.smart.therapy.flow.publicsite.entity.PublicSiteService;
import com.smart.therapy.flow.publicsite.repository.PublicSiteServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicSiteServiceAdminService {

    private final PublicSiteServiceRepository repository;

    @Transactional(readOnly = true)
    public List<PublicSiteServiceResponse> listAll() {
        return repository.findByIsDeletedFalseOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PublicSiteServiceResponse create(CreatePublicSiteServiceRequest request) {
        String name = request.getName().trim();
        String slug = uniqueSlug(slugify(name));
        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextDisplayOrder();
        PublicSiteService entity = PublicSiteService.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.getDescription()))
                .enabled(request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()))
                .displayOrder(order)
                .isSystem(false)
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30)
                .baseRate(request.getBaseRate() != null ? request.getBaseRate() : BigDecimal.ZERO)
                .build();
        return toResponse(repository.save(entity));
    }

    @Transactional
    public PublicSiteServiceResponse update(Long id, UpdatePublicSiteServiceRequest request) {
        PublicSiteService entity = repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public site service not found"));

        if (StringUtils.hasText(request.getName())) {
            String name = request.getName().trim();
            entity.setName(name);
            if (!Boolean.TRUE.equals(entity.getIsSystem())) {
                String desired = slugify(name);
                entity.setSlug(uniqueSlugForUpdate(entity, desired));
            }
        }
        if (request.getDescription() != null) {
            entity.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getDurationMinutes() != null) {
            entity.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getBaseRate() != null) {
            entity.setBaseRate(request.getBaseRate());
        }
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        PublicSiteService entity = repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public site service not found"));
        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new BadRequestException("System public services (e.g. Consultation) cannot be deleted");
        }
        entity.softDelete();
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public PublicSiteService requireEnabled(Long id) {
        PublicSiteService entity = repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public site service not found"));
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BadRequestException("Selected service is not enabled on the public site");
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public PublicSiteService requireEnabledBySlug(String slug) {
        PublicSiteService entity = repository.findBySlugAndIsDeletedFalse(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Public site service not found"));
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BadRequestException("Selected service is not enabled on the public site");
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public List<PublicSiteServiceResponse> listEnabled() {
        return repository.findByEnabledTrueAndIsDeletedFalseOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private int nextDisplayOrder() {
        return repository.findByIsDeletedFalseOrderByDisplayOrderAscNameAsc().stream()
                .mapToInt(s -> s.getDisplayOrder() != null ? s.getDisplayOrder() : 0)
                .max()
                .orElse(0) + 10;
    }

    private String uniqueSlugForUpdate(PublicSiteService entity, String desired) {
        if (desired.equals(entity.getSlug())) {
            return desired;
        }
        if (!repository.existsBySlugAndIsDeletedFalse(desired)) {
            return desired;
        }
        return uniqueSlug(desired);
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int i = 2;
        while (repository.existsBySlugAndIsDeletedFalse(candidate)) {
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }

    static String slugify(String name) {
        String slug = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!StringUtils.hasText(slug)) {
            slug = "service";
        }
        if (slug.length() > 90) {
            slug = slug.substring(0, 90).replaceAll("-+$", "");
        }
        return slug;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private PublicSiteServiceResponse toResponse(PublicSiteService entity) {
        return PublicSiteServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .enabled(entity.getEnabled())
                .displayOrder(entity.getDisplayOrder())
                .isSystem(entity.getIsSystem())
                .durationMinutes(entity.getDurationMinutes() != null ? entity.getDurationMinutes() : 30)
                .baseRate(entity.getBaseRate() != null ? entity.getBaseRate() : BigDecimal.ZERO)
                .build();
    }
}
