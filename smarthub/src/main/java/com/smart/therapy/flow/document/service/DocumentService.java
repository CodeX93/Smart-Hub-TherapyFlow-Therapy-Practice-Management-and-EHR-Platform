package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.FileSignatureValidator;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.dto.DocumentSummaryResponse;
import com.smart.therapy.flow.document.dto.DocumentReviewSummaryResponse;
import com.smart.therapy.flow.document.dto.ShareDocumentRequest;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentCategory;
import com.smart.therapy.flow.document.enums.DocumentScanStatus;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.audit.service.AuditEventFactory;
import com.smart.therapy.flow.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private static final String RESOURCE_TYPE_DOCUMENT = "document";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final ReviewQueueService reviewQueueService;
    private final DocumentReviewIntentMapper reviewIntentMapper;
    private final NotificationPayloadFactory notificationPayloadFactory;
    private final AuditEventFactory auditEventFactory;
    private final ClientReportAccessService clientReportAccessService;

    @Value("${document.review.default-days:7}")
    private long defaultReviewDays;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentSummaryResponse> getClientDocuments(Long clientId, AuthPrincipal requester) {
        return getClientDocuments(clientId, null, null, null, null, null, 1, 25, requester);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentSummaryResponse> getClientDocuments(
            Long clientId,
            String documentType,
            String category,
            ReviewStatus reviewStatus,
            Boolean shareWithClient,
            String search,
            int page,
            int pageSize,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateDocumentAccess(client, requester);

        DocumentType documentTypeFilter = parseEnumFilter(documentType, DocumentType.class, "documentType");
        DocumentCategory categoryFilter = parseEnumFilter(category, DocumentCategory.class, "category");
        String normalizedSearch = StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;

        List<Document> documents = documentRepository.findByClientIdWithRelations(clientId);
        List<Document> filtered = documents.stream()
                .filter(doc -> documentTypeFilter == null || doc.getDocumentType() == documentTypeFilter)
                .filter(doc -> categoryFilter == null || doc.getCategory() == categoryFilter)
                .filter(doc -> reviewStatus == null || resolveEffectiveReviewStatus(doc) == reviewStatus)
                .filter(doc -> shareWithClient == null || Objects.equals(doc.getIsSharedInPortal(), shareWithClient))
                .filter(doc -> !StringUtils.hasText(normalizedSearch) || matchesSearch(doc, normalizedSearch))
                .sorted(Comparator
                        .comparing(Document::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Document::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = filtered.size();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        List<DocumentSummaryResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toDocumentSummaryResponse)
                .collect(Collectors.toList());

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "document_list_viewed", null, clientId, null, true,
                "resultCount=" + items.size() + ",total=" + total + ",page=" + safePage + ",pageSize=" + safePageSize);

        return PaginatedResponse.of(items, total, safePage, safePageSize);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> getReviewQueue(
            ReviewStatus status,
            Boolean overdueOnly,
            Integer overdueHours,
            Long clientId,
            Integer page,
            Integer pageSize,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");

        if (!canViewAll && !canViewTeam && !canViewOwn) {
            throw new ForbiddenException("Insufficient permissions to view document review queue");
        }

        if (status == ReviewStatus.APPROVED || status == ReviewStatus.REJECTED) {
            throw new BadRequestException("Review queue only supports pending review statuses");
        }

        List<ReviewStatus> statuses = resolveReviewQueueStatuses(status);
        boolean filterOverdue = Boolean.TRUE.equals(overdueOnly) || status == ReviewStatus.OVERDUE;

        List<Document> documents = (clientId != null)
                ? documentRepository.findByNeedsReviewTrueAndReviewStatusInAndIsDeletedFalseAndClient_Id(statuses, clientId)
                : documentRepository.findByNeedsReviewTrueAndReviewStatusInAndIsDeletedFalse(statuses);

        documents = reviewQueueService.getScopedPendingQueue(requester, documents);

        if (filterOverdue) {
            documents = documents.stream()
                    .filter(doc -> isOverdue(doc, overdueHours))
                    .collect(Collectors.toList());
        }

        documents.sort((a, b) -> {
            Instant aCreated = a.getCreatedAt();
            Instant bCreated = b.getCreatedAt();
            if (aCreated == null && bCreated == null) {
                return 0;
            }
            if (aCreated == null) {
                return 1;
            }
            if (bCreated == null) {
                return -1;
            }
            return bCreated.compareTo(aCreated);
        });

        int safePage = Math.max(page != null ? page : 0, 0);
        int safeSize = Math.min(Math.max(pageSize != null ? pageSize : 50, 1), 200);
        int fromIndex = Math.min(safePage * safeSize, documents.size());
        int toIndex = Math.min(fromIndex + safeSize, documents.size());

        List<DocumentSummaryResponse> result = documents.subList(fromIndex, toIndex).stream()
                .map(this::toDocumentSummaryResponse)
                .collect(Collectors.toList());

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(),
                "document_review_queue_viewed", null, clientId, null, true,
                "resultCount=" + result.size() + ",page=" + safePage + ",pageSize=" + safeSize);

        return result;
    }

    @Transactional(readOnly = true)
    public DocumentReviewSummaryResponse getReviewSummary(
            Integer overdueHours,
            Long clientId,
            AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");

        if (!canViewAll && !canViewTeam && !canViewOwn) {
            throw new ForbiddenException("Insufficient permissions to view document review summary");
        }

        List<ReviewStatus> statuses = resolveReviewQueueStatuses(null);
        List<Document> documents = (clientId != null)
                ? documentRepository.findByNeedsReviewTrueAndReviewStatusInAndIsDeletedFalseAndClient_Id(statuses, clientId)
                : documentRepository.findByNeedsReviewTrueAndReviewStatusInAndIsDeletedFalse(statuses);

        documents = reviewQueueService.getScopedPendingQueue(requester, documents);

        long pending = documents.stream()
                .filter(doc -> doc.getReviewStatus() == ReviewStatus.PENDING)
                .count();
        long therapistReview = documents.stream()
                .filter(doc -> doc.getReviewStatus() == ReviewStatus.THERAPIST_REVIEW)
                .count();
        long supervisorReview = documents.stream()
                .filter(doc -> doc.getReviewStatus() == ReviewStatus.SUPERVISOR_REVIEW)
                .count();
        long overdue = documents.stream()
                .filter(doc -> isOverdue(doc, overdueHours))
                .count();

        long totalPending = pending + therapistReview + supervisorReview;

        return DocumentReviewSummaryResponse.builder()
                .totalPending(totalPending)
                .overdue(overdue)
                .therapistReview(therapistReview)
                .supervisorReview(supervisorReview)
                .pending(pending)
                .build();
    }

    @Transactional
    @CacheEvict(value = "documents", allEntries = true)
    public DocumentResponse uploadDocument(
            Long clientId,
            MultipartFile file,
            String documentType,
            String category,
            String description,
            Boolean needsReview,
            Boolean shareWithClient,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(file, "File is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 50MB");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateDocumentAccess(client, requester);

        User uploader = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            // Plan limit: DOCUMENT_UPLOAD_GB (tracked in bytes)
            Long orgId = com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId();
            if (orgId != null) {
                subscriptionFeatureService.consumeDocumentStorageOrThrow(orgId, file.getSize(), "Document upload");
            }

            byte[] fileBytes = file.getBytes();
            String contentChecksum = sha256Hex(fileBytes);
            Instant scannedAt = Instant.now();
            DocumentScanStatus scanStatus = DocumentScanStatus.PENDING;
            String scanDetail = null;
            String storagePath = null;

            try {
                FileSignatureValidator.validate(file);
                storagePath = storageService.uploadFile(file, String.valueOf(clientId),
                        UUID.randomUUID() + extractExtension(file.getOriginalFilename()));
                scanStatus = DocumentScanStatus.CLEAN;
            } catch (BadRequestException validationError) {
                scanStatus = DocumentScanStatus.QUARANTINED;
                scanDetail = validationError.getMessage();
            }

            Instant reviewDueAt = resolveReviewDueAt(needsReview);

            Document document = Document.builder()
                    .client(client)
                    .fileName(storagePath != null ? storagePath : "quarantined/" + contentChecksum)
                    .originalName(file.getOriginalFilename())
                    .fileSize((int) file.getSize())
                    .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .category(category != null ? DocumentCategory.valueOf(category.toUpperCase())
                            : DocumentCategory.UPLOADED)
                    .documentType(resolveDocumentType(documentType))
                    .description(description)
                    .uploadedBy(uploader)
                    .isSharedInPortal(shareWithClient != null && shareWithClient)
                    .needsReview(needsReview != null && needsReview)
                    .reviewStatus(needsReview != null && needsReview ? ReviewStatus.PENDING : null)
                    .reviewDueAt(reviewDueAt)
                    .scanStatus(scanStatus)
                    .scannedAt(scannedAt)
                    .scanDetail(scanDetail)
                    .contentChecksum(contentChecksum)
                    .build();

            Document saved = Objects.requireNonNull(documentRepository.save(document),
                    "Persisted document must not be null");
            Long savedId = requireDocumentId(saved);

            // Trigger notification
            if (notificationService != null) {
                try {
                    notificationService.processEventInNewTransaction(
                            NotificationEventCatalog.DOCUMENT_UPLOADED,
                            notificationPayloadFactory.documentUploaded(saved,
                                    saved.getUploadedBy() != null ? saved.getUploadedBy().getId() : null,
                                    saved.getUploadedBy() != null ? saved.getUploadedBy().getFullName() : null));
                    // Note: Document entity doesn't have needsReview field - can be handled via
                    // category or separate workflow
                } catch (Exception e) {
                    log.error("Failed to trigger document notification", e);
                }
            }

            // Audit log
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_uploaded", savedId, clientId, ipAddress, true);

            return toDocumentResponse(saved);

        } catch (Exception e) {
            log.error("Failed to upload document", e);
            throw new BadRequestException("Failed to upload document: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "documents", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey(#documentId)")
    public DocumentResponse getDocument(Long documentId, AuthPrincipal requester) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        validateDocumentAccess(document.getClient(), requester);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_metadata_viewed",
                documentId, document.getClient().getId(), null, true);
        return toDocumentResponse(document);
    }

    public InputStream viewPdfDocument(Long documentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        Client client = clientRepository.findById(document.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        validateDocumentAccess(client, requester);
        assertDownloadAllowed(document);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_viewed", documentId, client.getId(), ipAddress, true);

        // Get file stream from storage
        try {
            return storageService.downloadFile(document.getFileName());
        } catch (Exception e) {
            log.error("Failed to view PDF document", e);
            throw new BadRequestException("Failed to view document: " + e.getMessage());
        }
    }

    public InputStream downloadDocument(Long documentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        validateDocumentAccess(document.getClient(), requester);
        assertDownloadAllowed(document);

        try {
            // Audit log
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_downloaded", documentId, document.getClient().getId(),
                    ipAddress, true);

            // Note: Document entity doesn't have storagePath - use fileName which contains
            // the storage path
            return storageService.downloadFile(document.getFileName());
        } catch (Exception e) {
            log.error("Failed to download document", e);
            throw new BadRequestException("Failed to download document: " + e.getMessage());
        }
    }

    @Transactional
    @CacheEvict(value = "documents", allEntries = true)
    public void deleteDocument(Long documentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Find document including deleted (to check if already deleted)
        Document document = documentRepository.findByIdIncludingDeleted(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Check if already deleted
        if (Boolean.TRUE.equals(document.getIsDeleted())) {
            throw new BadRequestException("Document is already deleted");
        }

        validateDocumentAccess(document.getClient(), requester);

        try {
            // Delete from storage - use fileName which contains the storage path
            storageService.deleteFile(document.getFileName());

            // Soft delete from database (HIPAA/GDPR compliance)
            Long clientId = document.getClient().getId();
            document.setIsDeleted(true);
            document.setDeletedAt(Instant.now());
            documentRepository.save(document);
            documentRepository.flush();

            // Audit log
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_deleted", documentId, clientId, ipAddress, true);

        } catch (Exception e) {
            log.error("Failed to delete document", e);
            throw new BadRequestException("Failed to delete document: " + e.getMessage());
        }
    }

    @Transactional
    @CacheEvict(value = "documents", allEntries = true)
    public void shareDocument(Long documentId, ShareDocumentRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        validateDocumentAccess(document.getClient(), requester);

        document.setIsSharedInPortal(request.getShareWithClient() != null ? request.getShareWithClient() : false);
        documentRepository.save(document);

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_share_updated", documentId, document.getClient().getId(),
                ipAddress, true);
    }

    public void viewDocument(Long documentId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(requester, "Requester is required");

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        validateDocumentAccess(document.getClient(), requester);

        // Audit log for document viewing
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_viewed", documentId, document.getClient().getId(), ipAddress,
                true);
    }

    @Transactional
    @CacheEvict(value = "documents", allEntries = true)
    public DocumentResponse reviewDocument(Long documentId,
            com.smart.therapy.flow.document.dto.ReviewDocumentRequest request, AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(documentId, "Document id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // PBAC: Check permission to view documents
        if (!permissionChecker.hasPermission(requester,"FORM_VIEW") && 
            !permissionChecker.hasPermission(requester,"CLIENT_VIEW_OWN") &&
            !permissionChecker.hasPermission(requester,"CLIENT_VIEW_TEAM") &&
            !permissionChecker.hasPermission(requester,"CLIENT_VIEW_ALL")) {
            throw new ForbiddenException("Insufficient permissions to review documents");
        }

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        validateDocumentAccess(document.getClient(), requester);

        if (!Boolean.TRUE.equals(document.getNeedsReview())) {
            throw new BadRequestException("Document does not require review");
        }

        User reviewer = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReviewStatus nextStatus = reviewIntentMapper.resolveCanonicalStatus(request, document);
        if (nextStatus == null) {
            throw new BadRequestException("Review status is required");
        }
        if (nextStatus == ReviewStatus.OVERDUE) {
            throw new BadRequestException("Review status OVERDUE is system-derived and cannot be set directly");
        }

        if (nextStatus == ReviewStatus.APPROVED || nextStatus == ReviewStatus.REJECTED) {
            document.setReviewStatus(nextStatus);
            document.setReviewedAt(Instant.now());
            document.setReviewedBy(reviewer);
            document.setNeedsReview(false); // Mark as reviewed
        } else {
            // Move back to a review stage
            document.setReviewStatus(nextStatus);
            document.setNeedsReview(true);
            document.setReviewedAt(null);
            document.setReviewedBy(null);
            if (document.getReviewDueAt() == null) {
                document.setReviewDueAt(resolveReviewDueAt(true));
            }
        }

        Document updated = documentRepository.save(document);

        // Reload with all relationships to ensure they're available in the response
        updated = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found after update"));

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "document_reviewed", documentId, document.getClient().getId(), ipAddress,
                true);

        return toDocumentResponse(updated);
    }

    // Private helper methods

    /**
     * PBAC: Validate document access via shared CLIENT_VIEW_ALL / TEAM / OWN caseload rules.
     */
    private void validateDocumentAccess(Client client, AuthPrincipal requester) {
        clientReportAccessService.validateClientAccess(client, requester);
    }

    private Map<String, Object> buildDocumentEventData(Document document) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", document.getId());
        data.put("clientId", document.getClient().getId());
        NotificationPayloadFactory.putClientIdentity(data, document.getClient());
        data.put("fileName", document.getOriginalName());
        data.put("documentType", document.getDocumentType());
        data.put("uploadedById", document.getUploadedBy() != null ? document.getUploadedBy().getId() : null);
        data.put("uploadedByName", document.getUploadedBy() != null ? document.getUploadedBy().getFullName() : null);
        data.put("needsReview", document.getNeedsReview());
        data.put("reviewStatus", document.getReviewStatus());
        data.put("reviewDueAt", document.getReviewDueAt());
        return data;
    }

    private void recordAuditEvent(Long actorId, String action, Long documentId, Long clientId, String ipAddress,
            boolean hipaaRelevant) {
        recordAuditEvent(actorId, action, documentId, clientId, ipAddress, hipaaRelevant, null);
    }

    private void recordAuditEvent(Long actorId, String action, Long documentId, Long clientId, String ipAddress,
            boolean hipaaRelevant, String details) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_DOCUMENT, documentId, clientId, ipAddress,
                    hipaaRelevant, details);
        } catch (Exception e) {
            log.error("Failed to record audit event for document: {}", documentId, e);
        }
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .clientId(document.getClient().getId())
                .clientName(safeClientName(document.getClient()))
                .fileName(document.getFileName())
                .originalName(resolveDisplayName(document.getOriginalName(), document.getFileName()))
                .fileSize(document.getFileSize() != null ? document.getFileSize().longValue() : null)
                .mimeType(document.getMimeType())
                .documentType(document.getDocumentType() != null ? document.getDocumentType().name() : null)
                .category(document.getCategory() != null ? document.getCategory().name() : null)
                .description(resolveReadableText(document.getDescription()))
                .uploadedById(document.getUploadedBy() != null ? document.getUploadedBy().getId() : null)
                .uploadedByName(document.getUploadedBy() != null ? document.getUploadedBy().getFullName() : null)
                .uploadedAt(document.getCreatedAt()) // Use createdAt as uploadedAt
                .needsReview(document.getNeedsReview())
                .reviewStatus(resolveEffectiveReviewStatus(document) != null ? resolveEffectiveReviewStatus(document).name() : null)
                .reviewDueAt(document.getReviewDueAt())
                .reviewedById(document.getReviewedBy() != null ? document.getReviewedBy().getId() : null)
                .reviewedByName(document.getReviewedBy() != null ? document.getReviewedBy().getFullName() : null)
                .reviewedAt(document.getReviewedAt())
                .shareWithClient(document.getIsSharedInPortal())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private DocumentSummaryResponse toDocumentSummaryResponse(Document document) {
        return DocumentSummaryResponse.builder()
                .id(document.getId())
                .clientId(document.getClient().getId())
                .clientName(safeClientName(document.getClient()))
                .fileName(document.getFileName())
                .originalName(resolveDisplayName(document.getOriginalName(), document.getFileName()))
                .fileSize(document.getFileSize() != null ? document.getFileSize().longValue() : null)
                .mimeType(document.getMimeType())
                .documentType(document.getDocumentType() != null ? document.getDocumentType().name() : null)
                .category(document.getCategory() != null ? document.getCategory().name() : null)
                .uploadedById(document.getUploadedBy() != null ? document.getUploadedBy().getId() : null)
                .uploadedByName(document.getUploadedBy() != null ? document.getUploadedBy().getFullName() : null)
                .uploadedAt(document.getCreatedAt())
                .needsReview(document.getNeedsReview())
                .reviewStatus(resolveEffectiveReviewStatus(document) != null ? resolveEffectiveReviewStatus(document).name() : null)
                .reviewDueAt(document.getReviewDueAt())
                .reviewedById(document.getReviewedBy() != null ? document.getReviewedBy().getId() : null)
                .reviewedByName(document.getReviewedBy() != null ? document.getReviewedBy().getFullName() : null)
                .reviewedAt(document.getReviewedAt())
                .shareWithClient(document.getIsSharedInPortal())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * When PHI decrypt soft-fails, converters may return TFENC ciphertext. Never expose that to clients.
     */
    private static String resolveDisplayName(String originalName, String fileName) {
        String readable = resolveReadableText(originalName);
        if (StringUtils.hasText(readable)) {
            return readable;
        }
        return StringUtils.hasText(fileName) ? fileName : "Document";
    }

    private static String resolveReadableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("TFENC:") || (trimmed.startsWith("ENC(") && trimmed.endsWith(")"))) {
            return null;
        }
        return trimmed;
    }

    private static String safeClientName(Client client) {
        if (client == null) {
            return null;
        }
        return resolveReadableText(client.getFullName());
    }

    private void assertDownloadAllowed(Document document) {
        DocumentScanStatus status = document.getScanStatus();
        if (status == null || status == DocumentScanStatus.CLEAN) {
            return;
        }
        if (status == DocumentScanStatus.QUARANTINED) {
            throw new BadRequestException("Document is quarantined and cannot be downloaded");
        }
        if (status == DocumentScanStatus.PENDING) {
            throw new BadRequestException("Document scan is pending; download is not available yet");
        }
        throw new BadRequestException("Document failed security scan and cannot be downloaded");
    }

    private static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to compute file checksum");
        }
    }

    private Instant resolveReviewDueAt(Boolean needsReview) {
        if (!Boolean.TRUE.equals(needsReview)) {
            return null;
        }
        return Instant.now().plus(defaultReviewDays, ChronoUnit.DAYS);
    }

    private List<ReviewStatus> resolveReviewQueueStatuses(ReviewStatus status) {
        if (status == null || status == ReviewStatus.OVERDUE) {
            return List.of(ReviewStatus.PENDING, ReviewStatus.THERAPIST_REVIEW, ReviewStatus.SUPERVISOR_REVIEW);
        }
        return List.of(status);
    }

    private boolean isOverdue(Document document, Integer overdueHours) {
        if (document == null || !Boolean.TRUE.equals(document.getNeedsReview())) {
            return false;
        }
        Instant now = Instant.now();
        if (overdueHours != null && overdueHours > 0) {
            Instant cutoff = now.minus(overdueHours, ChronoUnit.HOURS);
            Instant createdAt = document.getCreatedAt();
            return createdAt != null && createdAt.isBefore(cutoff);
        }
        return document.getReviewDueAt() != null && document.getReviewDueAt().isBefore(now);
    }

    private ReviewStatus resolveEffectiveReviewStatus(Document document) {
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

    private Long requireDocumentId(Document document) {
        Objects.requireNonNull(document, "Document is required");
        return Objects.requireNonNull(document.getId(), "Document id must not be null");
    }

    private boolean matchesSearch(Document document, String normalizedSearch) {
        if (!StringUtils.hasText(normalizedSearch)) {
            return true;
        }

        return containsIgnoreCase(document.getOriginalName(), normalizedSearch)
                || containsIgnoreCase(document.getFileName(), normalizedSearch)
                || containsIgnoreCase(document.getDescription(), normalizedSearch);
    }

    private boolean containsIgnoreCase(String value, String normalizedSearch) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    private static String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        String ext = originalFilename.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");
        return ext.length() <= 16 ? ext : "";
    }

    private DocumentType resolveDocumentType(String rawDocumentType) {
        if (!StringUtils.hasText(rawDocumentType)) {
            return DocumentType.OTHER;
        }

        String normalized = rawDocumentType.trim().toUpperCase(Locale.ROOT);
        try {
            return DocumentType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            // Backward compatibility for legacy/client values not present in enum.
            switch (normalized) {
                case "CONSENT_FORM":
                    return DocumentType.CONSENT;
                case "ID_DOCUMENT":
                case "MEDICAL_RECORD":
                case "PRESCRIPTION":
                case "LAB_RESULT":
                case "REFERRAL_LETTER":
                    log.warn("Unsupported documentType '{}'; defaulting to OTHER", rawDocumentType);
                    return DocumentType.OTHER;
                default:
                    log.warn("Unknown documentType '{}'; defaulting to OTHER", rawDocumentType);
                    return DocumentType.OTHER;
            }
        }
    }

    private <E extends Enum<E>> E parseEnumFilter(String rawValue, Class<E> enumType, String fieldName) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + fieldName + " value: " + rawValue);
        }
    }

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility in business logic validation
    // For access control, always use principal.hasPermission() instead
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return permissionChecker.hasRole(principal, roleName);
    }

}




