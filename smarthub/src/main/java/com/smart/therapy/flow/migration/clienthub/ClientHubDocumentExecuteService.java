package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentCategory;
import com.smart.therapy.flow.document.enums.DocumentScanStatus;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ClientHubDocumentExecuteService {

    // Azure DBs occasionally stall/time out on large transactions; keep batches smaller for reliability.
    private static final int BATCH_SIZE = 50;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public DocumentExecuteResult execute(List<SourceDocumentRecord> sourceDocuments, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for document execution");
        }

        DocumentExecuteResult total = DocumentExecuteResult.empty();
        int batchNumber = 0;
        for (int start = 0; start < sourceDocuments.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, sourceDocuments.size());
            int currentBatch = ++batchNumber;
            List<SourceDocumentRecord> batch = sourceDocuments.subList(start, end);
            DocumentExecuteResult batchResult = tenantTransactionExecutor.executeWrite(
                    target.organisationId(), target.schemaName(),
                    () -> executeDocumentBatch(batch, target));
            total = total.plus(batchResult);
            log.info("ClientHubAI document execute batch complete: batch={} batch_size={} processed={} total={}",
                    currentBatch, batch.size(), total.sourceDocuments(), sourceDocuments.size());
        }
        return total;
    }

    private DocumentExecuteResult executeDocumentBatch(
            List<SourceDocumentRecord> sourceDocuments,
            TargetInventory target) {
        Map<String, Long> clientMappings = loadMappings(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");
        Map<String, Long> documentMappings = loadMappings(target.organisationId(), "documents");

        int created = 0;
        int updated = 0;
        int mappedReruns = 0;
        for (SourceDocumentRecord source : sourceDocuments) {
            Optional<Long> mappedDocumentId = Optional.ofNullable(documentMappings.get(source.legacyDocumentPk()));
            if (mappedDocumentId.isPresent()) {
                updated++;
                mappedReruns++;
                continue;
            }

            Document document = new Document();
            applyFields(document, source, clientMappings, userMappings);
            Document saved = documentRepository.save(document);
            upsertLegacyMapping(target, source, saved.getId());
            created++;
        }

        return new DocumentExecuteResult(sourceDocuments.size(), created, updated, mappedReruns);
    }

    private void applyFields(
            Document document,
            SourceDocumentRecord source,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings) {
        document.setClient(clientRepository.getReferenceById(requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
        document.setUploadedBy(source.uploadedByLegacyId() == null
                ? null
                : userRepository.getReferenceById(requiredMapping(userMappings, source.uploadedByLegacyId(), "users")));
        document.setReviewedBy(source.reviewedByLegacyId() == null
                ? null
                : userRepository.getReferenceById(requiredMapping(userMappings, source.reviewedByLegacyId(), "users")));
        document.setFileName(source.fileName().trim());
        document.setOriginalName(source.originalName().trim());
        document.setFileSize(source.fileSize());
        document.setMimeType(trimTo(resolveMimeType(source), 100));
        document.setCategory(mapCategory(source.category()));
        document.setDocumentType(mapDocumentType(source.category()));
        document.setIsSharedInPortal(source.sharedInPortal());
        document.setDownloadCount(source.downloadCount() == null ? 0 : source.downloadCount());
        document.setNeedsReview(source.requiresTherapistReview() || source.requiresSupervisorReview());
        document.setReviewStatus(mapReviewStatus(source));
        document.setReviewedAt(source.reviewedAt());
        document.setDescription(trim(source.reviewNotes()));
        // Metadata-only import: keep download gated, but avoid perpetual PENDING confusion on list APIs.
        document.setScanStatus(DocumentScanStatus.PENDING);
        document.setScannedAt(null);
        document.setScanDetail("Imported metadata only; binary file not yet reconciled into TherapyFlow storage.");
        document.setContentChecksum(ClientHubIdentifier.sha256Hex(source.fileName() + "|" + source.fileSize()));
        document.setIsDeleted(false);
        document.setDeletedAt(null);
        document.setCreatedBy(0L);
        document.setUpdatedBy(0L);
        if (source.createdAt() != null) {
            document.setCreatedAt(source.createdAt());
        }
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(TargetInventory target, SourceDocumentRecord source, Long documentId) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', 'documents', ?, ?, 'documents', ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                source.legacyDocumentPk(),
                target.schemaName(),
                documentId,
                checksum(source));
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private DocumentCategory mapCategory(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "shared" -> DocumentCategory.SHARED;
            case "generated" -> DocumentCategory.GENERATED;
            case "forms", "form" -> DocumentCategory.FORMS;
            case "insurance" -> DocumentCategory.INSURANCE;
            default -> DocumentCategory.UPLOADED;
        };
    }

    private DocumentType mapDocumentType(String category) {
        String normalized = normalize(category);
        return switch (normalized) {
            case "forms", "form" -> DocumentType.INTAKE_FORM;
            case "insurance" -> DocumentType.INSURANCE_CARD;
            default -> DocumentType.OTHER;
        };
    }

    private ReviewStatus mapReviewStatus(SourceDocumentRecord source) {
        if (!source.requiresTherapistReview() && !source.requiresSupervisorReview() && source.reviewStatus() == null) {
            return null;
        }
        String normalized = normalize(source.reviewStatus());
        return switch (normalized) {
            case "approved" -> ReviewStatus.APPROVED;
            case "rejected" -> ReviewStatus.REJECTED;
            case "supervisorreview", "supervisor" -> ReviewStatus.SUPERVISOR_REVIEW;
            case "therapistreview", "therapist" -> ReviewStatus.THERAPIST_REVIEW;
            case "overdue" -> ReviewStatus.OVERDUE;
            default -> source.requiresSupervisorReview() ? ReviewStatus.SUPERVISOR_REVIEW
                    : source.requiresTherapistReview() ? ReviewStatus.THERAPIST_REVIEW
                    : ReviewStatus.PENDING;
        };
    }

    private String checksum(SourceDocumentRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyDocumentPk(),
                source.clientLegacyId(),
                source.fileName(),
                source.originalName(),
                String.valueOf(source.fileSize()),
                resolveMimeType(source),
                source.category()));
    }

    String resolveMimeType(SourceDocumentRecord source) {
        if (source.mimeType() != null && !source.mimeType().isBlank()) {
            return source.mimeType().trim();
        }
        String inferred = inferMimeType(source.originalName());
        if (inferred == null) {
            inferred = inferMimeType(source.fileName());
        }
        return inferred == null ? "application/octet-stream" : inferred;
    }

    private String inferMimeType(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return URLConnection.guessContentTypeFromName(fileName.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record DocumentExecuteResult(int sourceDocuments, int created, int updated, int mappedReruns) {

        static DocumentExecuteResult empty() {
            return new DocumentExecuteResult(0, 0, 0, 0);
        }

        DocumentExecuteResult plus(DocumentExecuteResult other) {
            return new DocumentExecuteResult(
                    sourceDocuments + other.sourceDocuments,
                    created + other.created,
                    updated + other.updated,
                    mappedReruns + other.mappedReruns);
        }
    }
}
