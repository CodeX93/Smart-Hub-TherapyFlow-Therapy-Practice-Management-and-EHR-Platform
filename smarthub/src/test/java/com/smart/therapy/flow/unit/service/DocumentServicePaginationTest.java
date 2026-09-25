package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.document.dto.DocumentSummaryResponse;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.document.service.DocumentService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServicePaginationTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private ClientReportAccessService clientReportAccessService;

    @InjectMocks
    private DocumentService documentService;

    private AuthPrincipal therapistPrincipal;
    private Client client;

    @BeforeEach
    void setUp() {
        User therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        client = TestDataFactory.createTestClient();
        client.setId(10L);
        client.setAssignedTherapist(therapist);
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
    }

    @Test
    void shouldReturnDifferentPagesForClientDocuments() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));

        Instant now = Instant.parse("2026-06-12T10:00:00Z");
        List<Document> documents = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> {
                    Document doc = Document.builder()
                            .client(client)
                            .fileName("file-" + i + ".pdf")
                            .originalName("file-" + i + ".pdf")
                            .fileSize(100)
                            .mimeType("application/pdf")
                            .documentType(DocumentType.OTHER)
                            .needsReview(false)
                            .reviewStatus(ReviewStatus.PENDING)
                            .build();
                    doc.setId((long) i);
                    doc.setCreatedAt(now.minusSeconds(i));
                    return doc;
                })
                .toList();
        when(documentRepository.findByClientIdWithRelations(10L)).thenReturn(documents);

        PaginatedResponse<DocumentSummaryResponse> page1 = documentService.getClientDocuments(
                10L, null, null, null, null, null, 1, 20, therapistPrincipal);
        PaginatedResponse<DocumentSummaryResponse> page2 = documentService.getClientDocuments(
                10L, null, null, null, null, null, 2, 20, therapistPrincipal);

        assertThat(page1.getTotalCount()).isEqualTo(25);
        assertThat(page1.getPage()).isEqualTo(1);
        assertThat(page1.getPageSize()).isEqualTo(20);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getItems()).hasSize(20);
        assertThat(page1.getItems().get(0).getId()).isEqualTo(1L);

        assertThat(page2.getItems()).hasSize(5);
        assertThat(page2.getItems().get(0).getId()).isEqualTo(21L);
        assertThat(page1.getItems().get(0).getId())
                .isNotEqualTo(page2.getItems().get(0).getId());
    }
}
