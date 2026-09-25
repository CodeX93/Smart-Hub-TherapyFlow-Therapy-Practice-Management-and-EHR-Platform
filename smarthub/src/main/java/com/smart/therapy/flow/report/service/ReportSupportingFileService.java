package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.report.dto.ReportSupportingFileResponse;
import com.smart.therapy.flow.report.dto.UploadSupportingFileRequest;
import com.smart.therapy.flow.report.entity.ReportSupportingFile;
import com.smart.therapy.flow.report.entity.ReportTemplate;
import com.smart.therapy.flow.report.repository.ReportSupportingFileRepository;
import com.smart.therapy.flow.report.util.BytesMultipartFile;
import com.smart.therapy.flow.report.util.ReportJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportSupportingFileService {

    private static final int MAX_FILE_BYTES = 15 * 1024 * 1024;

    private final ReportSupportingFileRepository supportingFileRepository;
    private final ReportDocumentExtractionService extractionService;
    private final ReportTemplateService templateService;
    private final StorageService storageService;
    private final ClientReportAccessService accessService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ReportSupportingFileResponse> listFiles(Long clientId, AuthPrincipal requester) {
        accessService.requireClientAccess(clientId, requester);
        List<ReportSupportingFileResponse> files = supportingFileRepository.findByClient_IdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toSafeResponse)
                .collect(Collectors.toList());
        User actor = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                actor.getId(), requester.getLoginIdentifier(), null, clientId,
                "report_supporting_files_viewed", null, "report-supporting-file-api",
                Map.of("resultCount", files.size()));
        return files;
    }

    @Transactional
    public ReportSupportingFileResponse uploadFile(Long clientId, UploadSupportingFileRequest request,
            AuthPrincipal requester, String ipAddress, String userAgent) {
        Client client = accessService.requireClientAccess(clientId, requester);
        byte[] fileBytes = decodeBase64(request.getFileContent());
        if (fileBytes.length > MAX_FILE_BYTES) {
            throw new BadRequestException("File exceeds maximum size of 15 MB");
        }
        if (!extractionService.isSupportedSupportingType(request.getMimeType(), request.getOriginalName())) {
            throw new BadRequestException("Only .docx, .pdf, and .txt files are supported");
        }

        if (StringUtils.hasText(request.getDocumentType())) {
            if (request.getTemplateId() == null) {
                throw new BadRequestException("templateId is required when documentType is set");
            }
            ReportTemplate template = templateService.requireActiveTemplate(request.getTemplateId());
            List<String> allowed = ReportJsonUtil.fromJsonArray(template.getSupportingFileTypesJson());
            if (!allowed.contains(request.getDocumentType())) {
                throw new BadRequestException("documentType is not allowed for this template");
            }
        }

        String extractedText = extractionService.extractDocumentText(
                fileBytes, request.getMimeType(), request.getOriginalName());

        User creator = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReportSupportingFile file = ReportSupportingFile.builder()
                .client(client)
                .originalName(request.getOriginalName())
                .mimeType(request.getMimeType())
                .fileSize(fileBytes.length)
                .documentType(request.getDocumentType())
                .extractedText(extractedText)
                .createdByUser(creator)
                .build();
        file = supportingFileRepository.save(file);

        try {
            BytesMultipartFile multipart = new BytesMultipartFile(
                    fileBytes, request.getOriginalName(), request.getMimeType());
            String blobKey = storageService.uploadFile(multipart, String.valueOf(clientId), request.getOriginalName());
            file.setFileBlobName(blobKey);
            try {
                file.setFileUrl(storageService.getFileUrl(blobKey));
            } catch (Exception urlEx) {
                log.warn("Could not generate file URL for supporting file {}: {}", file.getId(), urlEx.getMessage());
            }
            file = supportingFileRepository.save(file);
        } catch (Exception ex) {
            log.warn("Blob upload failed for supporting file {}, keeping DB row: {}", file.getId(), ex.getMessage());
        }

        auditLogService.logClientReportAccess(
                creator.getId(), requester.getLoginIdentifier(), null, clientId,
                "report_supporting_file_uploaded", ipAddress, userAgent,
                Map.of("fileId", file.getId(), "fileSizeBytes", file.getFileSize()));

        return toSafeResponse(file);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId, AuthPrincipal requester) throws Exception {
        ReportSupportingFile file = supportingFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Supporting file not found"));
        accessService.requireClientAccess(file.getClient().getId(), requester);
        if (!StringUtils.hasText(file.getFileBlobName())) {
            throw new ResourceNotFoundException("File content is not available");
        }
        InputStream stream = storageService.downloadFile(file.getFileBlobName());
        User actor = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                actor.getId(), requester.getLoginIdentifier(), null, file.getClient().getId(),
                "report_supporting_file_downloaded", null, "report-supporting-file-api",
                Map.of("fileId", file.getId(), "fileSizeBytes", file.getFileSize()));
        return new InputStreamResource(stream);
    }

    public String resolveDownloadFilename(Long fileId) {
        return supportingFileRepository.findById(fileId)
                .map(ReportSupportingFile::getOriginalName)
                .orElse("supporting-file");
    }

    @Transactional
    public void deleteFile(Long fileId, AuthPrincipal requester, String ipAddress, String userAgent) {
        ReportSupportingFile file = supportingFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Supporting file not found"));
        Long clientId = file.getClient().getId();
        accessService.requireClientAccess(clientId, requester);

        if (StringUtils.hasText(file.getFileBlobName())) {
            try {
                storageService.deleteFile(file.getFileBlobName());
            } catch (Exception ex) {
                log.warn("Failed to delete supporting file blob {}: {}", file.getFileBlobName(), ex.getMessage());
            }
        }

        supportingFileRepository.delete(file);
        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                user.getId(), requester.getLoginIdentifier(), null, clientId,
                "report_supporting_file_deleted", ipAddress, userAgent,
                Map.of("fileId", fileId));
    }

    @Transactional(readOnly = true)
    public List<ReportSupportingFile> resolveSupportingFiles(Long clientId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return fileIds.stream()
                .map(id -> supportingFileRepository.findByIdAndClient_Id(id, clientId).orElse(null))
                .filter(Objects::nonNull)
                .filter(f -> StringUtils.hasText(f.getExtractedText()))
                .collect(Collectors.toList());
    }

    private ReportSupportingFileResponse toSafeResponse(ReportSupportingFile file) {
        return ReportSupportingFileResponse.builder()
                .id(file.getId())
                .clientId(file.getClient().getId())
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .documentType(file.getDocumentType())
                .createdById(file.getCreatedByUser() != null ? file.getCreatedByUser().getId() : null)
                .createdAt(file.getCreatedAt())
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
