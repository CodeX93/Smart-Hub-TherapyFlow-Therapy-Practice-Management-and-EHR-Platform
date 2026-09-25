package com.smart.therapy.flow.unit.client.portal;

import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.service.PortalDocumentService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalDocumentService")
class PortalDocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @InjectMocks
    private PortalDocumentService portalDocumentService;

    @Test
    @DisplayName("Returns paginated documents with preview and download URLs")
    void returnsPaginatedDocumentsWithUrls() {
        AuthPrincipal principal = TestDataFactory.createAuthPrincipalForClient(10L);
        Client client = TestDataFactory.createTestClient();
        client.setId(10L);

        Document document = Document.builder()
                .client(client)
                .fileName("clients/10/file.pdf")
                .originalName("file.pdf")
                .documentType(DocumentType.INSURANCE_CARD)
                .isSharedInPortal(true)
                .build();
        document.setId(7L);

        when(currentUserService.requireCurrentClient(principal)).thenReturn(client);
        when(documentRepository.findPortalDocuments(10L)).thenReturn(List.of(document));

        PaginatedResponse<DocumentResponse> page = portalDocumentService.getDocuments(
                principal, 1, 20, null, null, null, "127.0.0.1", "jest");

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getPreviewUrl()).isEqualTo("/api/v1/portal/documents/7/view");
        assertThat(page.getItems().get(0).getDownloadUrl()).isEqualTo("/api/v1/portal/documents/7/download");
    }
}
