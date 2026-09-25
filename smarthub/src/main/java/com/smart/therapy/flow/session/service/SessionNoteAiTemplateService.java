package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.session.dto.CreateSessionNoteAiTemplateRequest;
import com.smart.therapy.flow.session.dto.SessionNoteAiTemplateResponse;
import com.smart.therapy.flow.session.dto.UpdateSessionNoteAiTemplateRequest;
import com.smart.therapy.flow.session.entity.SessionNoteAiTemplate;
import com.smart.therapy.flow.session.repository.SessionNoteAiTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionNoteAiTemplateService {

    private final SessionNoteAiTemplateRepository templateRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<SessionNoteAiTemplateResponse> listTemplates(AuthPrincipal requester) {
        Long ownerId = resolveOwnerId(requester);
        return templateRepository.findByCreatedByAndIsDeletedFalseOrderByLastUsedAtDescUpdatedAtDesc(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionNoteAiTemplateResponse getTemplate(Long id, AuthPrincipal requester) {
        return toResponse(loadOwnedTemplate(id, requester));
    }

    @Transactional(readOnly = true)
    public String resolveInstructions(Long templateId, AuthPrincipal requester) {
        SessionNoteAiTemplate template = loadOwnedTemplate(templateId, requester);
        return template.getInstructions();
    }

    @Transactional
    public SessionNoteAiTemplateResponse createTemplate(CreateSessionNoteAiTemplateRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        currentUserService.requireCurrentUser(requester);

        String name = normalizeRequired(request.getName(), "Template name");
        String instructions = normalizeRequired(request.getInstructions(), "Custom instructions");

        SessionNoteAiTemplate template = SessionNoteAiTemplate.builder()
                .name(name)
                .instructions(instructions)
                .lastUsedAt(Instant.now())
                .build();

        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public SessionNoteAiTemplateResponse updateTemplate(Long id, UpdateSessionNoteAiTemplateRequest request,
            AuthPrincipal requester) {
        Objects.requireNonNull(request, "Request is required");
        SessionNoteAiTemplate template = loadOwnedTemplate(id, requester);

        if (StringUtils.hasText(request.getName())) {
            template.setName(normalizeRequired(request.getName(), "Template name"));
        }
        if (request.getInstructions() != null) {
            if (!StringUtils.hasText(request.getInstructions())) {
                throw new BadRequestException("Custom instructions cannot be empty");
            }
            template.setInstructions(request.getInstructions().trim());
        }

        template.setUpdatedBy(resolveOwnerId(requester));
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long id, AuthPrincipal requester) {
        SessionNoteAiTemplate template = loadOwnedTemplate(id, requester);
        template.softDelete();
        template.setUpdatedBy(resolveOwnerId(requester));
        templateRepository.save(template);
    }

    @Transactional
    public SessionNoteAiTemplateResponse markLastUsed(Long id, AuthPrincipal requester) {
        SessionNoteAiTemplate template = loadOwnedTemplate(id, requester);
        template.setLastUsedAt(Instant.now());
        template.setUpdatedBy(resolveOwnerId(requester));
        return toResponse(templateRepository.save(template));
    }

    private SessionNoteAiTemplate loadOwnedTemplate(Long id, AuthPrincipal requester) {
        Long ownerId = resolveOwnerId(requester);
        return templateRepository.findByIdAndCreatedByAndIsDeletedFalse(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Session note AI template not found"));
    }

    /**
     * {@link com.smart.therapy.flow.common.entity.BaseEntity#getCreatedBy()} stores AuthIdentity id
     * (see {@link com.smart.therapy.flow.common.config.JpaAuditingConfig}), not {@link User#getId()}.
     */
    private Long resolveOwnerId(AuthPrincipal requester) {
        currentUserService.requireCurrentUser(requester);
        Long authId = requester.getAuthId();
        if (authId == null) {
            throw new com.smart.therapy.flow.common.exception.ForbiddenException("Staff user not found");
        }
        return authId;
    }

    private String normalizeRequired(String value, String fieldLabel) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(fieldLabel + " is required");
        }
        String trimmed = value.trim();
        if ("Template name".equals(fieldLabel) && trimmed.length() > 50) {
            throw new BadRequestException("Template name must be at most 50 characters");
        }
        return trimmed;
    }

    private SessionNoteAiTemplateResponse toResponse(SessionNoteAiTemplate template) {
        return SessionNoteAiTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .instructions(template.getInstructions())
                .lastUsedAt(template.getLastUsedAt())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
