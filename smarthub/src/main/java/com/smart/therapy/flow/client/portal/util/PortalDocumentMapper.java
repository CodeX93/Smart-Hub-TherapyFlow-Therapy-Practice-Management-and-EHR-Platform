package com.smart.therapy.flow.client.portal.util;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

public final class PortalDocumentMapper {

    private static final Map<String, DocumentType> DOCUMENT_TYPE_ALIASES = Map.of(
            "insurance_card", DocumentType.INSURANCE_CARD,
            "intake_form", DocumentType.INTAKE_FORM,
            "consent_form", DocumentType.CONSENT,
            "consent", DocumentType.CONSENT,
            "id_document", DocumentType.OTHER,
            "medical_record", DocumentType.OTHER,
            "prescription", DocumentType.OTHER,
            "lab_result", DocumentType.OTHER,
            "referral_letter", DocumentType.OTHER);

    private PortalDocumentMapper() {
    }

    public static DocumentType parseDocumentType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DocumentType.OTHER;
        }
        String trimmed = raw.trim();
        String aliasKey = trimmed.toLowerCase(Locale.ROOT);
        DocumentType alias = DOCUMENT_TYPE_ALIASES.get(aliasKey);
        if (alias != null) {
            return alias;
        }
        try {
            return DocumentType.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DocumentType.OTHER;
        }
    }

    public static DocumentResponse toResponse(Document doc, Client client) {
        DocumentResponse.DocumentResponseBuilder builder = DocumentResponse.builder()
                .id(doc.getId())
                .clientId(doc.getClient().getId())
                .clientName(readableOrNull(doc.getClient().getFullName()))
                .fileName(doc.getFileName())
                .originalName(displayName(doc.getOriginalName(), doc.getFileName()))
                .fileSize(doc.getFileSize() != null ? doc.getFileSize().longValue() : null)
                .mimeType(doc.getMimeType())
                .documentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : null)
                .category(doc.getCategory() != null ? doc.getCategory().name() : null)
                .description(readableOrNull(doc.getDescription()))
                .shareWithClient(doc.getIsSharedInPortal())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt());

        if (doc.getUploadedBy() == null) {
            builder.uploadedById(client.getId()).uploadedByName(readableOrNull(client.getFullName()));
        } else {
            builder.uploadedById(doc.getUploadedBy().getId())
                    .uploadedByName(doc.getUploadedBy().getFullName());
        }

        builder.uploadedAt(doc.getCreatedAt());
        ReviewStatus effectiveStatus = resolveEffectiveReviewStatus(doc);
        builder.needsReview(doc.getNeedsReview())
                .reviewStatus(effectiveStatus != null ? effectiveStatus.name() : null)
                .reviewDueAt(doc.getReviewDueAt())
                .reviewedById(doc.getReviewedBy() != null ? doc.getReviewedBy().getId() : null)
                .reviewedByName(doc.getReviewedBy() != null ? doc.getReviewedBy().getFullName() : null)
                .reviewedAt(doc.getReviewedAt());

        return enrichPortalUrls(builder.build());
    }

    public static DocumentResponse enrichPortalUrls(DocumentResponse document) {
        if (document == null || document.getId() == null) {
            return document;
        }
        String relativeBase = "/api/v1/portal/documents/" + document.getId();
        String previewUrl = relativeBase + "/view";
        String downloadUrl = relativeBase + "/download";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            String absoluteBase = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            previewUrl = absoluteBase + previewUrl;
            downloadUrl = absoluteBase + downloadUrl;
        }
        document.setPreviewUrl(previewUrl);
        document.setDownloadUrl(downloadUrl);
        return document;
    }

    public static boolean matchesSearch(Document doc, String normalizedSearch) {
        if (!StringUtils.hasText(normalizedSearch)) {
            return true;
        }
        String needle = normalizedSearch.toLowerCase(Locale.ROOT);
        return contains(doc.getOriginalName(), needle)
                || contains(doc.getDescription(), needle)
                || contains(doc.getDocumentType() != null ? doc.getDocumentType().name() : null, needle)
                || contains(doc.getCategory() != null ? doc.getCategory().name() : null, needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String displayName(String originalName, String fileName) {
        String readable = readableOrNull(originalName);
        if (StringUtils.hasText(readable)) {
            return readable;
        }
        return StringUtils.hasText(fileName) ? fileName : "Document";
    }

    private static String readableOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("TFENC:") || (trimmed.startsWith("ENC(") && trimmed.endsWith(")"))) {
            return null;
        }
        return trimmed;
    }

    private static ReviewStatus resolveEffectiveReviewStatus(Document document) {
        if (document == null) {
            return null;
        }
        ReviewStatus status = document.getReviewStatus();
        if (status == null) {
            return null;
        }
        if (status == ReviewStatus.APPROVED || status == ReviewStatus.REJECTED) {
            return status;
        }
        if (Boolean.TRUE.equals(document.getNeedsReview())
                && document.getReviewDueAt() != null
                && document.getReviewDueAt().isBefore(Instant.now())) {
            return ReviewStatus.OVERDUE;
        }
        return status;
    }
}
