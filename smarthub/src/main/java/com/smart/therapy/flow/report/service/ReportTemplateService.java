package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.report.dto.CreateReportTemplateRequest;
import com.smart.therapy.flow.report.dto.ReportTemplateResponse;
import com.smart.therapy.flow.report.dto.UpdateReportTemplateRequest;
import com.smart.therapy.flow.report.entity.ReportTemplate;
import com.smart.therapy.flow.report.repository.ReportTemplateRepository;
import com.smart.therapy.flow.report.util.BytesMultipartFile;
import com.smart.therapy.flow.report.util.ReportJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportTemplateService {

    private static final int MAX_FILE_BYTES = 15 * 1024 * 1024;

    private final ReportTemplateRepository templateRepository;
    private final ReportDocumentExtractionService extractionService;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ReportTemplateResponse> listTemplates(boolean includeInactive) {
        List<ReportTemplate> templates = includeInactive
                ? templateRepository.findAllByOrderByNameAsc()
                : templateRepository.findByIsActiveTrueOrderByNameAsc();
        return templates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportTemplateResponse getTemplate(Long id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report template not found"));
        return toResponse(template);
    }

    @Transactional
    public ReportTemplateResponse createTemplate(CreateReportTemplateRequest request, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        Objects.requireNonNull(request, "Request is required");
        byte[] fileBytes = decodeBase64(request.getFileContent());
        if (fileBytes.length > MAX_FILE_BYTES) {
            throw new BadRequestException("File exceeds maximum size of 15 MB");
        }
        if (!extractionService.isSupportedTemplateType(request.getMimeType(), request.getOriginalName())) {
            throw new BadRequestException("Only .docx and .pdf templates are supported");
        }

        String structureText = extractionService.extractTemplateStructure(
                fileBytes, request.getMimeType(), request.getOriginalName());

        User creator = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReportTemplate template = ReportTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .aiInstructions(request.getAiInstructions())
                .originalName(request.getOriginalName())
                .mimeType(request.getMimeType())
                .fileSize(fileBytes.length)
                .structureText(structureText)
                .defaultIncludeProfile(request.getDefaultIncludeProfile() != null ? request.getDefaultIncludeProfile() : true)
                .defaultIncludeNotes(request.getDefaultIncludeNotes() != null ? request.getDefaultIncludeNotes() : true)
                .defaultIncludeAssessments(request.getDefaultIncludeAssessments() != null ? request.getDefaultIncludeAssessments() : true)
                .supportingFilesGuidance(request.getSupportingFilesGuidance())
                .supportingFilesExpected(Boolean.TRUE.equals(request.getSupportingFilesExpected()))
                .supportingFileTypesJson(ReportJsonUtil.toJsonArray(request.getSupportingFileTypes()))
                .isActive(true)
                .createdByUser(creator)
                .build();

        template = templateRepository.save(template);

        try {
            BytesMultipartFile multipart = new BytesMultipartFile(
                    fileBytes, request.getOriginalName(), request.getMimeType());
            String blobKey = storageService.uploadFile(multipart, "report-templates", request.getOriginalName());
            template.setFileBlobName(blobKey);
            try {
                template.setFileUrl(storageService.getFileUrl(blobKey));
            } catch (Exception urlEx) {
                log.warn("Could not generate file URL for template {}: {}", template.getId(), urlEx.getMessage());
            }
            template = templateRepository.save(template);
        } catch (Exception ex) {
            templateRepository.delete(template);
            throw new BadRequestException("Failed to store template file: " + ex.getMessage());
        }

        auditLogService.logReportTemplateAccess(
                creator.getId(), requester.getLoginIdentifier(), template.getId(),
                "report_template_created", ipAddress, userAgent,
                Map.of("name", template.getName()));

        return toResponse(template);
    }

    @Transactional
    public ReportTemplateResponse updateTemplate(Long id, UpdateReportTemplateRequest request, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report template not found"));

        if (StringUtils.hasText(request.getName())) {
            template.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getAiInstructions() != null) {
            template.setAiInstructions(request.getAiInstructions());
        }
        if (request.getStructureText() != null) {
            template.setStructureText(request.getStructureText());
        }
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        if (request.getDefaultIncludeProfile() != null) {
            template.setDefaultIncludeProfile(request.getDefaultIncludeProfile());
        }
        if (request.getDefaultIncludeNotes() != null) {
            template.setDefaultIncludeNotes(request.getDefaultIncludeNotes());
        }
        if (request.getDefaultIncludeAssessments() != null) {
            template.setDefaultIncludeAssessments(request.getDefaultIncludeAssessments());
        }
        if (request.getSupportingFilesGuidance() != null) {
            template.setSupportingFilesGuidance(request.getSupportingFilesGuidance());
        }
        if (request.getSupportingFilesExpected() != null) {
            template.setSupportingFilesExpected(request.getSupportingFilesExpected());
        }
        if (request.getSupportingFileTypes() != null) {
            template.setSupportingFileTypesJson(ReportJsonUtil.toJsonArray(request.getSupportingFileTypes()));
        }

        ReportTemplate saved = templateRepository.save(template);
        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logReportTemplateAccess(
                user.getId(), requester.getLoginIdentifier(), saved.getId(),
                "report_template_updated", ipAddress, userAgent, Map.of());

        return toResponse(saved);
    }

    @Transactional
    public void deleteTemplate(Long id, AuthPrincipal requester, String ipAddress, String userAgent) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report template not found"));

        if (StringUtils.hasText(template.getFileBlobName())) {
            try {
                storageService.deleteFile(template.getFileBlobName());
            } catch (Exception ex) {
                log.warn("Failed to delete template blob {}: {}", template.getFileBlobName(), ex.getMessage());
            }
        }

        templateRepository.delete(template);
        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logReportTemplateAccess(
                user.getId(), requester.getLoginIdentifier(), id,
                "report_template_deleted", ipAddress, userAgent, Map.of("name", template.getName()));
    }

    public ReportTemplate requireActiveTemplate(Long id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report template not found"));
        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new BadRequestException("Report template is not active");
        }
        return template;
    }

    ReportTemplateResponse toResponse(ReportTemplate template) {
        Long createdById = template.getCreatedByUser() != null ? template.getCreatedByUser().getId() : null;
        return ReportTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .aiInstructions(template.getAiInstructions())
                .originalName(template.getOriginalName())
                .mimeType(template.getMimeType())
                .fileSize(template.getFileSize())
                .structureText(template.getStructureText())
                .defaultIncludeProfile(template.getDefaultIncludeProfile())
                .defaultIncludeNotes(template.getDefaultIncludeNotes())
                .defaultIncludeAssessments(template.getDefaultIncludeAssessments())
                .supportingFilesGuidance(template.getSupportingFilesGuidance())
                .supportingFilesExpected(template.getSupportingFilesExpected())
                .supportingFileTypes(ReportJsonUtil.fromJsonArray(template.getSupportingFileTypesJson()))
                .isActive(template.getIsActive())
                .createdById(createdById)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private byte[] decodeBase64(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BadRequestException("File content is required");
        }
        String payload = raw;
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            payload = raw.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid base64 file content");
        }
    }
}
