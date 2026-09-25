package com.smart.therapy.flow.unit.client.portal;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.service.ClientPortalService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.document.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client portal document view/download access")
class ClientPortalDocumentAccessTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private StorageService storageService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private ClientPortalService clientPortalService;

    private Client client;
    private AuthPrincipal principal;

    @BeforeEach
    void setUp() {
        client = TestDataFactory.createTestClient();
        client.setId(10L);
        principal = TestDataFactory.createAuthPrincipalForClient(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));

        when(currentUserService.requireCurrentClient(principal)).thenReturn(client);
        when(clientRepository.findByIdWithTherapist(10L)).thenReturn(Optional.of(client));
        ReflectionTestUtils.setField(clientPortalService, "storageService", storageService);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        lenient().when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Client can view their own upload")
    void viewClientUpload() throws Exception {
        Document document = staffDocument(false);
        document.setUploadedBy(null);
        when(documentRepository.findByIdWithRelations(185L)).thenReturn(Optional.of(document));
        when(storageService.downloadFile(anyString()))
                .thenReturn(new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.UTF_8)));

        ClientPortalService.DocumentViewResult result =
                clientPortalService.viewDocument(185L, "127.0.0.1", "test");

        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Client can view staff upload shared in portal")
    void viewSharedStaffUpload() throws Exception {
        Document document = staffDocument(true);
        when(documentRepository.findByIdWithRelations(186L)).thenReturn(Optional.of(document));
        when(storageService.downloadFile(anyString()))
                .thenReturn(new ByteArrayInputStream("shared".getBytes(StandardCharsets.UTF_8)));

        ClientPortalService.DocumentViewResult result =
                clientPortalService.viewDocument(186L, "127.0.0.1", "test");

        assertThat(new String(result.getContent(), StandardCharsets.UTF_8)).isEqualTo("shared");
    }

    @Test
    @DisplayName("Client cannot view staff upload that is not shared")
    void rejectUnsharedStaffUpload() {
        Document document = staffDocument(false);
        when(documentRepository.findByIdWithRelations(187L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> clientPortalService.viewDocument(187L, "127.0.0.1", "test"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Download increments null-safe download count")
    void downloadIncrementsCount() throws Exception {
        Document document = staffDocument(false);
        document.setUploadedBy(null);
        document.setDownloadCount(null);
        when(documentRepository.findByIdWithRelations(188L)).thenReturn(Optional.of(document));
        when(storageService.downloadFile(anyString()))
                .thenReturn(new ByteArrayInputStream("file".getBytes(StandardCharsets.UTF_8)));

        clientPortalService.downloadDocument(188L, "127.0.0.1", "test");

        assertThat(document.getDownloadCount()).isEqualTo(1);
        verify(documentRepository).save(document);
    }

    private Document staffDocument(boolean sharedInPortal) {
        User therapist = TestDataFactory.createTestTherapist();
        therapist.setId(6L);
        Document document = Document.builder()
                .client(client)
                .uploadedBy(therapist)
                .fileName("clients/10/test.pdf")
                .originalName("test.pdf")
                .mimeType("application/pdf")
                .isSharedInPortal(sharedInPortal)
                .build();
        document.setId(185L);
        return document;
    }
}
