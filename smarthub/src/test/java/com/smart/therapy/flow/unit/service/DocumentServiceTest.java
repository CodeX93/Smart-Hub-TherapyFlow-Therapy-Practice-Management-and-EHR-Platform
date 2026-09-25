package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.dto.DocumentSummaryResponse;
import com.smart.therapy.flow.document.dto.ReviewDocumentRequest;
import com.smart.therapy.flow.document.dto.ShareDocumentRequest;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentScanStatus;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.document.service.DocumentReviewIntentMapper;
import com.smart.therapy.flow.document.service.DocumentService;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
@SuppressWarnings("null") // Suppress null warnings from Mockito mocks
class DocumentServiceTest {

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private ClientRepository clientRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private AuditLogRepository auditLogRepository;

        @Mock
        private CurrentUserService currentUserService;

        @Mock
        private StorageService storageService;

        @Mock
        private NotificationService notificationService;

        @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @Mock
    private ClientReportAccessService clientReportAccessService;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private DocumentReviewIntentMapper reviewIntentMapper = new DocumentReviewIntentMapper();

    @InjectMocks
        private DocumentService documentService;

        private AuthPrincipal therapistPrincipal;
        private User therapist;
        private Client client;
        private Document document;
        private MultipartFile multipartFile;

        @BeforeEach
        void setUp() {
                therapist = TestDataFactory.createTestTherapist();
                therapist.setId(1L);
                therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST", "CLIENT_VIEW_OWN");

                client = TestDataFactory.createTestClient();
                client.setId(1L);
                client.setAssignedTherapist(therapist);

                document = Document.builder()
                                .client(client)
                                .fileName("test.pdf")
                                .originalName("test.pdf")
                                .fileSize(1024)
                                .mimeType("application/pdf")
                                .isSharedInPortal(false)
                                .documentType(com.smart.therapy.flow.document.enums.DocumentType.INTAKE_FORM) // Using
                                                                                                              // enum
                                .description("Test document description")
                                .needsReview(false)
                                .reviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.PENDING) // Using enum
                                .build();

                document.setId(1L);
                multipartFile = mock(MultipartFile.class);

        }

        @Test
        @DisplayName("Should upload document successfully")
        void shouldUploadDocumentSuccessfully() throws Exception {
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
                // A synthetic PDF header exercises the real signature validator.
                multipartFile = new MockMultipartFile("file", "test.pdf", "application/pdf",
                        "%PDF-1.4\nSynthetic fixture\n%%EOF".getBytes(StandardCharsets.US_ASCII));
                // Arrange
                String storagePath = "documents/1/test.pdf";
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
                when(storageService.uploadFile(any(MultipartFile.class), anyString(), anyString()))
                                .thenReturn(storagePath);
                when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                        Document saved = inv.getArgument(0);
                        saved.setId(1L);
                        return saved;
                });

                // Act
                DocumentResponse response = documentService.uploadDocument(
                                1L, multipartFile, "INTAKE_FORM", "uploaded", "Test document description", false, false,
                                therapistPrincipal, "127.0.0.1");

                // Assert
                assertThat(response).isNotNull();
                verify(clientRepository).findById(1L);
                verify(storageService).uploadFile(any(MultipartFile.class), anyString(), anyString());
                verify(documentRepository).save(argThat(saved -> saved.getScanStatus() == DocumentScanStatus.CLEAN));
        }

        @Test
        @DisplayName("Should upload document with documentType and description")
        void shouldUploadDocumentWithDocumentTypeAndDescription() throws Exception {
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
                // A synthetic PDF header exercises the real signature validator.
                multipartFile = new MockMultipartFile("file", "test.pdf", "application/pdf",
                        "%PDF-1.4\nSynthetic fixture\n%%EOF".getBytes(StandardCharsets.US_ASCII));
                // Arrange
                String storagePath = "documents/1/test.pdf";

                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
                when(storageService.uploadFile(any(MultipartFile.class), anyString(), anyString()))
                                .thenReturn(storagePath);
                when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                        Document saved = inv.getArgument(0);
                        saved.setId(1L);
                        return saved;
                });

                // Act
                DocumentResponse response = documentService.uploadDocument(
                                1L, multipartFile, "CONSENT", "uploaded", "Client progress note for session", true,
                                false,
                                therapistPrincipal, "127.0.0.1");

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getDocumentType()).isEqualTo("CONSENT");
                assertThat(response.getDescription()).isEqualTo("Client progress note for session");
                assertThat(response.getNeedsReview()).isTrue();
                verify(documentRepository).save(argThat(saved -> saved.getScanStatus() == DocumentScanStatus.CLEAN));
        }

        @Test
        @DisplayName("Should throw BadRequestException when file is empty")
        void shouldThrowExceptionWhenFileIsEmpty() {
                when(multipartFile.isEmpty()).thenReturn(false);
                // Arrange
                when(multipartFile.isEmpty()).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> documentService.uploadDocument(
                                1L, multipartFile, "pdf", "uploaded", null, false, false,
                                therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("File is empty");

                verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when file size exceeds limit")
        void shouldThrowExceptionWhenFileSizeExceedsLimit() {
                when(multipartFile.getSize()).thenReturn(1024L);
                when(multipartFile.isEmpty()).thenReturn(false);
                // Arrange
                when(multipartFile.getSize()).thenReturn(60 * 1024 * 1024L); // 60MB

                // Act & Assert
                assertThatThrownBy(() -> documentService.uploadDocument(
                                1L, multipartFile, "pdf", "uploaded", null, false, false,
                                therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("File size exceeds maximum");

                verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client not found")
        void shouldThrowExceptionWhenClientNotFound() {
                when(multipartFile.getSize()).thenReturn(1024L);
                when(multipartFile.isEmpty()).thenReturn(false);
                // Arrange
                when(clientRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> documentService.uploadDocument(
                                999L, multipartFile, "pdf", "uploaded", null, false, false,
                                therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Client not found");

                verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should get client documents successfully")
        void shouldGetClientDocumentsSuccessfully() {
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
                // Arrange
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(documentRepository.findByClientIdWithRelations(1L)).thenReturn(List.of(document));

                // Act
                com.smart.therapy.flow.common.dto.PaginatedResponse<DocumentSummaryResponse> documents =
                        documentService.getClientDocuments(1L, therapistPrincipal);

                // Assert
                assertThat(documents).isNotNull();
                assertThat(documents.getItems()).hasSize(1);
                verify(clientRepository).findById(1L);
                verify(documentRepository).findByClientIdWithRelations(1L);
        }

        @Test
        @DisplayName("Should share document with client successfully")
        void shouldShareDocumentWithClientSuccessfully() {
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
                // Arrange
                ShareDocumentRequest request = new ShareDocumentRequest();
                request.setShareWithClient(true);

                when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
                when(documentRepository.save(any(Document.class))).thenReturn(document);

                // Act
                documentService.shareDocument(1L, request, therapistPrincipal, "127.0.0.1");

                // Assert
                verify(documentRepository).findById(1L);
                verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
                // Arrange
                ShareDocumentRequest request = new ShareDocumentRequest();
                when(documentRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> documentService.shareDocument(999L, request, therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Document not found");

                verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should delete document successfully")
        void shouldDeleteDocumentSuccessfully() throws Exception {
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
                // Arrange
                when(documentRepository.findByIdIncludingDeleted(1L)).thenReturn(Optional.of(document));
                doNothing().when(storageService).deleteFile(anyString());

                // Act
                documentService.deleteDocument(1L, therapistPrincipal, "127.0.0.1");

                // Assert
                verify(documentRepository).findByIdIncludingDeleted(1L);
                verify(storageService).deleteFile(anyString());
                verify(documentRepository).save(document);
                verify(documentRepository, never()).delete(any(Document.class));
                assertThat(document.getIsDeleted()).isTrue();
                assertThat(document.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should review document successfully")
        void shouldReviewDocumentSuccessfully() {
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);

                // Arrange
                Document documentNeedingReview = Document.builder()
                                .client(client)
                                .fileName("test.pdf")
                                .originalName("test.pdf")
                                .fileSize(1024)
                                .mimeType("application/pdf")
                                .isSharedInPortal(false)
                                .needsReview(true)
                                .reviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.PENDING) // Using enum
                                .build();
                documentNeedingReview.setId(1L);

                ReviewDocumentRequest request = new ReviewDocumentRequest();
                request.setReviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.APPROVED);
                request.setReviewNotes("Document looks good");

                when(documentRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(documentNeedingReview));
                when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
                when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                DocumentResponse response = documentService.reviewDocument(1L, request, therapistPrincipal,
                                "127.0.0.1");

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getReviewStatus()).isEqualTo("APPROVED");
                assertThat(documentNeedingReview.getReviewedBy()).isSameAs(therapist);
                assertThat(documentNeedingReview.getReviewedAt()).isNotNull();
                assertThat(documentNeedingReview.getNeedsReview()).isFalse();
                verify(documentRepository, times(2)).findByIdWithRelations(1L);
                verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when document does not need review")
        void shouldThrowExceptionWhenDocumentDoesNotNeedReview() {
                // Arrange
                Document reviewedDocument = Document.builder()
                                .client(client)
                                .fileName("test.pdf")
                                .originalName("test.pdf")
                                .fileSize(1024)
                                .mimeType("application/pdf")
                                .isSharedInPortal(false)
                                .needsReview(false)
                                .reviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.APPROVED) // Using enum
                                .build();
                reviewedDocument.setId(1L);

                ReviewDocumentRequest request = new ReviewDocumentRequest();
                request.setReviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.APPROVED);

                when(documentRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(reviewedDocument));

                // Act & Assert
                assertThatThrownBy(() -> documentService.reviewDocument(1L, request, therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Document does not require review");

                verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when review status is missing")
        void shouldThrowExceptionWhenReviewStatusIsMissing() {
                when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
                when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);

                // Arrange
                Document documentNeedingReview = Document.builder()
                                .client(client)
                                .fileName("test.pdf")
                                .originalName("test.pdf")
                                .fileSize(1024)
                                .mimeType("application/pdf")
                                .isSharedInPortal(false)
                                .needsReview(true)
                                .reviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.PENDING) // Using enum
                                .build();
                documentNeedingReview.setId(1L);

                ReviewDocumentRequest request = new ReviewDocumentRequest();
                request.setReviewStatus(null);

                when(documentRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(documentNeedingReview));

                // Act & Assert
                assertThatThrownBy(() -> documentService.reviewDocument(1L, request, therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Either reviewStatus or action is required");

                verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw ForbiddenException when non-authorized user reviews document")
        void shouldThrowExceptionWhenNonAuthorizedUserReviewsDocument() {
                // Arrange
                // Create a user without therapist/supervisor/admin roles (e.g., client role)
                com.smart.therapy.flow.auth.entity.AuthIdentity authRegular = com.smart.therapy.flow.common.TestDataFactory.createTestAuthIdentity("regular@example.com", "$2a$10$encryptedPasswordHash");
                User regularUser = User.builder()
                                .email("regular@example.com")
                                .fullName("Regular User")
                                .authIdentity(authRegular)
                                .isActive(true)
                                .build();
                regularUser.setId(3L);
                AuthPrincipal regularUserPrincipal = TestDataFactory.createAuthPrincipal(regularUser);
                Document documentNeedingReview = Document.builder()
                                .client(client)
                                .fileName("test.pdf")
                                .originalName("test.pdf")
                                .fileSize(1024)
                                .mimeType("application/pdf")
                                .isSharedInPortal(false)
                                .needsReview(true)
                                .reviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.PENDING) // Using enum
                                .build();
                documentNeedingReview.setId(1L);

                ReviewDocumentRequest request = new ReviewDocumentRequest();
                request.setReviewStatus(com.smart.therapy.flow.document.enums.ReviewStatus.APPROVED);

                // Act & Assert
                assertThatThrownBy(() -> documentService.reviewDocument(1L, request, regularUserPrincipal, "127.0.0.1"))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("Insufficient permissions to review documents");

                verify(documentRepository, never()).save(any(Document.class));
        }
}
