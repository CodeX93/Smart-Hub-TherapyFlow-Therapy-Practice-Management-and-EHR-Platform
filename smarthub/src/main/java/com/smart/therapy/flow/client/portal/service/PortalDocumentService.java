package com.smart.therapy.flow.client.portal.service;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.portal.util.PortalDocumentMapper;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentCategory;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalDocumentService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentResponse> getDocuments(
            AuthPrincipal principal,
            int page,
            int pageSize,
            String documentType,
            String category,
            String search,
            String ipAddress,
            String userAgent) {
        Client client = currentUserService.requireCurrentClient(principal);
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        DocumentType documentTypeFilter = parseEnumFilter(documentType, DocumentType.class, "documentType");
        DocumentCategory categoryFilter = parseEnumFilter(category, DocumentCategory.class, "category");
        String normalizedSearch = StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;

        List<Document> documents = documentRepository.findPortalDocuments(client.getId());
        List<Document> filtered = documents.stream()
                .filter(doc -> documentTypeFilter == null || doc.getDocumentType() == documentTypeFilter)
                .filter(doc -> categoryFilter == null || doc.getCategory() == categoryFilter)
                .filter(doc -> PortalDocumentMapper.matchesSearch(doc, normalizedSearch))
                .sorted(Comparator
                        .comparing(Document::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Document::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        List<DocumentResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(doc -> PortalDocumentMapper.toResponse(doc, client))
                .collect(Collectors.toList());

        recordAudit(client, "documents_viewed", ipAddress, userAgent,
                "Portal document list - page: " + safePage + ", count: " + items.size());

        return PaginatedResponse.of(items, filtered.size(), safePage, safePageSize);
    }

    private <E extends Enum<E>> E parseEnumFilter(String raw, Class<E> enumType, String label) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (E value : enumType.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(normalized)) {
                return value;
            }
        }
        throw new BadRequestException("Invalid " + label + ": " + raw);
    }

    private void recordAudit(Client client, String action, String ipAddress, String userAgent, String details) {
        if (client == null || client.getId() == null) {
            return;
        }
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .result("success")
                    .resourceType("document")
                    .username(HipaaAuditLabels.clientActor(client))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .hipaaRelevant(true)
                    .riskLevel("medium")
                    .timestamp(Instant.now())
                    .details(details)
                    .build();
            clientRepository.findById(client.getId()).ifPresent(auditLog::setClient);
            auditLogService.write(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit event for {}: {}", action, e.getMessage());
        }
    }
}
