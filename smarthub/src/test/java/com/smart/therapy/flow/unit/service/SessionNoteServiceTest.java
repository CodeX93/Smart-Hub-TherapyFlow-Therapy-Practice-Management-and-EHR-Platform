package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.dto.CreateSessionNoteRequest;
import com.smart.therapy.flow.session.dto.SessionNoteResponse;
import com.smart.therapy.flow.session.dto.UpdateSessionNoteRequest;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.SessionNoteService;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SessionNoteService Unit Tests")
class SessionNoteServiceTest {

    @Mock
    private SessionNoteRepository sessionNoteRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupervisorAssignmentRepository supervisorAssignmentRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private com.smart.therapy.flow.common.security.CaseloadScopeService caseloadScopeService;

    @Mock
    private com.smart.therapy.flow.system.service.SystemOptionResolverService systemOptionResolverService;

    @Mock
    private com.smart.therapy.flow.common.service.TimezoneService timezoneService;

    @Mock
    private com.smart.therapy.flow.client.util.ClientServiceEligibilityMessages clientServiceEligibilityMessages;

    @Mock
    private com.smart.therapy.flow.system.repository.PracticeConfigurationRepository practiceConfigurationRepository;

    @Mock
    private com.smart.therapy.flow.user.repository.UserProfileRepository userProfileRepository;

    @Mock
    private com.smart.therapy.flow.session.repository.SessionNoteAmendmentRepository sessionNoteAmendmentRepository;

    @InjectMocks
    private SessionNoteService sessionNoteService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;
    private Client client;
    private Session session;
    private SessionNote sessionNote;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        when(permissionChecker.hasPermission(any(AuthPrincipal.class), eq("CLIENT_VIEW_OWN"))).thenReturn(true);
        when(caseloadScopeService.resolve(any(AuthPrincipal.class)))
                .thenReturn(new CaseloadScopeService.ResolvedCaseloadScope(
                        CaseloadScope.OWN, List.of(), therapist.getId()));
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.empty());

        client = TestDataFactory.createTestClient();
        client.setId(1L);
        client.setAssignedTherapist(therapist);

        session = Session.builder()
                .client(client)
                .therapist(therapist)
                .sessionDate(Instant.now())
                .status(com.smart.therapy.flow.session.enums.SessionStatus.COMPLETED.getValue())
                .build();
        session.setId(1L);

        sessionNote = SessionNote.builder()
                .session(session)
                .client(client)
                .therapist(therapist)
                .date(Instant.now())
                .sessionFocus("Anxiety management")
                .progress("Client showed improvement")
                .build();
        sessionNote.setId(1L);
    }

    @Test
    @DisplayName("Should create session note successfully")
    void shouldCreateSessionNoteSuccessfully() {
        // Arrange
        CreateSessionNoteRequest request = new CreateSessionNoteRequest();
        request.setSessionId(1L);
        request.setClientId(1L);
        request.setTherapistId(1L);
        request.setSessionFocus("Anxiety management");
        request.setProgress("Client showed improvement");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
        when(sessionNoteRepository.save(any(SessionNote.class))).thenReturn(sessionNote);

        // Act
        SessionNoteResponse response = sessionNoteService.createSessionNote(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(sessionRepository).findById(1L);
        verify(sessionNoteRepository).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when session not found")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Arrange
        CreateSessionNoteRequest request = new CreateSessionNoteRequest();
        request.setSessionId(999L);
        request.setClientId(1L);

        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionNoteService.createSessionNote(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");

        verify(sessionNoteRepository, never()).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user lacks access to session")
    void shouldThrowExceptionWhenUserLacksAccess() {
        // Arrange
        CreateSessionNoteRequest request = new CreateSessionNoteRequest();
        request.setSessionId(1L);
        request.setClientId(1L);

        User otherTherapist = TestDataFactory.createTestTherapist();
        otherTherapist.setId(999L);
        session.setTherapist(otherTherapist);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // Act & Assert
        assertThatThrownBy(() -> sessionNoteService.createSessionNote(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot create notes for this session");

        verify(sessionNoteRepository, never()).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when client ID does not match session")
    void shouldThrowExceptionWhenClientIdMismatch() {
        // Arrange
        CreateSessionNoteRequest request = new CreateSessionNoteRequest();
        request.setSessionId(1L);
        request.setClientId(999L); // Different client ID

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // Act & Assert
        assertThatThrownBy(() -> sessionNoteService.createSessionNote(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Client ID does not match session");

        verify(sessionNoteRepository, never()).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should get session note by ID successfully")
    void shouldGetSessionNoteByIdSuccessfully() {
        // Arrange
        Long noteId = 1L;
        when(sessionNoteRepository.findById(noteId)).thenReturn(Optional.of(sessionNote));

        // Act
        SessionNoteResponse response = sessionNoteService.getSessionNote(noteId, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(noteId);
        verify(sessionNoteRepository).findById(noteId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when note not found")
    void shouldThrowExceptionWhenNoteNotFound() {
        // Arrange
        Long noteId = 999L;
        when(sessionNoteRepository.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionNoteService.getSessionNote(noteId, therapistPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session note not found");

        verify(sessionNoteRepository).findById(noteId);
    }

    @Test
    @DisplayName("Should get session notes by session ID successfully")
    void shouldGetSessionNotesBySessionIdSuccessfully() {
        // Arrange
        Long sessionId = 1L;
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionNoteRepository.findBySessionId(sessionId)).thenReturn(List.of(sessionNote));

        // Act
        List<SessionNoteResponse> notes = sessionNoteService.getSessionNotesBySession(sessionId, therapistPrincipal);

        // Assert
        assertThat(notes).isNotNull();
        assertThat(notes).hasSize(1);
        verify(sessionRepository).findById(sessionId);
        verify(sessionNoteRepository).findBySessionId(sessionId);
    }

    @Test
    @DisplayName("Should get session notes by client ID successfully")
    void shouldGetSessionNotesByClientIdSuccessfully() {
        // Arrange
        Long clientId = 1L;
        when(sessionNoteRepository.findBySession_Client_Id(clientId)).thenReturn(List.of(sessionNote));
        when(sessionRepository.findByClientId(clientId)).thenReturn(List.of(session));

        // Act
        List<SessionNoteResponse> notes = sessionNoteService.getSessionNotesByClient(clientId, therapistPrincipal);

        // Assert
        assertThat(notes).isNotNull();
        verify(sessionNoteRepository).findBySession_Client_Id(clientId);
    }

    @Test
    @DisplayName("Should update session note successfully")
    void shouldUpdateSessionNoteSuccessfully() {
        // Arrange
        Long noteId = 1L;
        UpdateSessionNoteRequest request = new UpdateSessionNoteRequest();
        request.setSessionFocus("Updated focus");
        request.setProgress("Updated progress");

        when(sessionNoteRepository.findById(noteId)).thenReturn(Optional.of(sessionNote));
        when(sessionNoteRepository.save(any(SessionNote.class))).thenReturn(sessionNote);

        // Act
        SessionNoteResponse response = sessionNoteService.updateSessionNote(noteId, request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(sessionNoteRepository).findById(noteId);
        verify(sessionNoteRepository).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should reject edits to finalized session notes")
    void shouldRejectEditingFinalizedSessionNote() {
        sessionNote.setIsFinalized(true);
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        assertThatThrownBy(() -> sessionNoteService.updateSessionNote(
                1L, new UpdateSessionNoteRequest(), therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot edit a finalized session note");

        verify(sessionNoteRepository, never()).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should reject hard deletes of finalized session notes")
    void shouldRejectDeletingFinalizedSessionNote() {
        sessionNote.setIsFinalized(true);
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        assertThatThrownBy(() -> sessionNoteService.deleteSessionNote(
                1L, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete a finalized session note");

        verify(sessionNoteRepository, never()).delete(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should reopen finalized session note for assigned therapist")
    void shouldUnfinalizeSessionNoteSuccessfully() {
        sessionNote.setIsFinalized(true);
        sessionNote.setIsDraft(false);
        sessionNote.setFinalContent("Final clinical text");
        sessionNote.setFinalizedAt(Instant.parse("2026-09-01T12:00:00Z"));
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));
        when(sessionNoteRepository.save(any(SessionNote.class))).thenAnswer(inv -> inv.getArgument(0));

        jakarta.persistence.EntityManager entityManager = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query query = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn("true");
        org.springframework.test.util.ReflectionTestUtils.setField(sessionNoteService, "entityManager", entityManager);

        SessionNoteResponse response = sessionNoteService.unfinalizeSessionNote(
                1L, therapistPrincipal, "127.0.0.1");

        assertThat(response.getIsFinalized()).isFalse();
        assertThat(response.getIsDraft()).isTrue();
        assertThat(response.getDraftContent()).isEqualTo("Final clinical text");
        assertThat(response.getFinalContent()).isNull();
        assertThat(response.getFinalizedAt()).isNull();
        verify(entityManager).createNativeQuery(
                "SELECT set_config('app.allow_clinical_unfinalize', 'true', true)");
        verify(sessionNoteRepository).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should reopen finalized note when PUT sets isFinalized=false")
    void shouldUnfinalizeViaUpdateWhenIsFinalizedFalse() {
        sessionNote.setIsFinalized(true);
        sessionNote.setIsDraft(false);
        sessionNote.setFinalContent("Final clinical text");
        sessionNote.setFinalizedAt(Instant.parse("2026-09-01T12:00:00Z"));
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));
        when(sessionNoteRepository.save(any(SessionNote.class))).thenAnswer(inv -> inv.getArgument(0));

        jakarta.persistence.EntityManager entityManager = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query query = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn("true");
        org.springframework.test.util.ReflectionTestUtils.setField(sessionNoteService, "entityManager", entityManager);

        UpdateSessionNoteRequest request = new UpdateSessionNoteRequest();
        request.setIsFinalized(false);
        request.setIsDraft(true);

        SessionNoteResponse response = sessionNoteService.updateSessionNote(
                1L, request, therapistPrincipal, "127.0.0.1");

        assertThat(response.getIsFinalized()).isFalse();
        assertThat(response.getIsDraft()).isTrue();
        assertThat(response.getDraftContent()).isEqualTo("Final clinical text");
        verify(entityManager).createNativeQuery(
                "SELECT set_config('app.allow_clinical_unfinalize', 'true', true)");
    }

    @Test
    @DisplayName("Should reject reopen when session note is not finalized")
    void shouldRejectUnfinalizeWhenNotFinalized() {
        sessionNote.setIsFinalized(false);
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        assertThatThrownBy(() -> sessionNoteService.unfinalizeSessionNote(
                1L, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Session note is not finalized");

        verify(sessionNoteRepository, never()).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should reject reopen by therapist outside caseload")
    void shouldRejectUnfinalizeOutsideCaseload() {
        sessionNote.setIsFinalized(true);
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));
        when(caseloadScopeService.resolve(any(AuthPrincipal.class)))
                .thenReturn(new CaseloadScopeService.ResolvedCaseloadScope(
                        CaseloadScope.OWN, List.of(), 999L));

        assertThatThrownBy(() -> sessionNoteService.unfinalizeSessionNote(
                1L, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to reopen this session note");

        verify(sessionNoteRepository, never()).save(any(SessionNote.class));
    }

    @Test
    @DisplayName("Should audit successful PHI view of a session note")
    void shouldAuditSessionNoteView() {
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));
        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));

        sessionNoteService.getSessionNote(1L, therapistPrincipal);

        ArgumentCaptor<com.smart.therapy.flow.auth.entity.AuditLog> captor =
                ArgumentCaptor.forClass(com.smart.therapy.flow.auth.entity.AuditLog.class);
        verify(auditLogService).write(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("session_note_viewed");
        assertThat(captor.getValue().getHipaaRelevant()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> sessionNoteService.createSessionNote(null, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Request is required");
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateSessionNoteRequest request = new CreateSessionNoteRequest();
        request.setSessionId(1L);

        // Act & Assert
        assertThatThrownBy(() -> sessionNoteService.createSessionNote(request, null, "127.0.0.1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }

    // ------------------------------------------------------------------
    // Printable HTML export
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PDF prints the signed final note as the document body")
    void pdfShouldContainFinalNoteContent() {
        sessionNote.setFinalContent("Subjective:\nClient reported improved sleep onset.\nMorning fatigue persists.\n\nPlan:\nContinue weekly sessions.");
        sessionNote.setIsFinalized(true);
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("Client reported improved sleep onset.");
        assertThat(html).contains("Continue weekly sessions.");
        // Plain AI text keeps its line structure in print; section labels become
        // blocks, and line breaks inside a section survive.
        assertThat(html).contains("Client reported improved sleep onset.<br>Morning fatigue persists.");
    }

    @Test
    @DisplayName("PDF falls back to generated then draft content")
    void pdfShouldFallBackThroughContentSources() {
        sessionNote.setFinalContent(null);
        sessionNote.setGeneratedContent(null);
        sessionNote.setDraftContent("Draft only narrative of the session.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("Draft only narrative of the session.");
    }

    @Test
    @DisplayName("PDF without any note content still prints the structured fields")
    void pdfShouldPrintStructuredFieldsWhenNoFinalNote() {
        sessionNote.setFinalContent(null);
        sessionNote.setGeneratedContent(null);
        sessionNote.setDraftContent(null);
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        // Old notes predate final-note generation; their fields must still export.
        assertThat(html).contains("Session Focus:");
        assertThat(html).contains("Anxiety management");
        assertThat(html).contains("Client showed improvement");
    }

    @Test
    @DisplayName("PDF sanitizes rich-text note content before printing")
    void pdfShouldSanitizeRichTextContent() {
        sessionNote.setFinalContent("<p>Legitimate <strong>note</strong> text.</p><script>alert('x')</script>");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("Legitimate <strong>note</strong> text.");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("alert('x')");
    }

    @Test
    @DisplayName("PDF letterhead comes from the practice configuration")
    void pdfShouldUsePracticeConfigurationLetterhead() {
        var config = new com.smart.therapy.flow.system.entity.PracticeConfiguration();
        config.setPracticeName("Harbour Street Counselling");
        config.setPracticeAddress("12 Harbour Street, Toronto");
        config.setPracticePhone("+1 555 010 4477");
        config.setPracticeEmail("care@example.test");
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("Harbour Street Counselling");
        assertThat(html).contains("12 Harbour Street, Toronto");
        assertThat(html).doesNotContain("Therapy Practice");
    }


    @Test
    @DisplayName("PDF always carries the client identity fields")
    void pdfShouldCarryClientIdentityFields() {
        sessionNote.setFinalContent("Subjective:\nImproved sleep onset.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        // A typed note has no identity header of its own, so the record supplies it.
        assertThat(html).contains("<strong>Client ID:</strong>");
        assertThat(html).contains("<strong>Treatment Stage:</strong>");
        assertThat(html).contains("<strong>Duration:</strong>");
    }

    @Test
    @DisplayName("PDF does not duplicate an AI note's own identity header")
    void pdfShouldNotDuplicateIdentityHeader() {
        sessionNote.setFinalContent(
                "CLIENT INFORMATION\nName: Test Client\nClient ID: CL-1\nAge: 30\nGender: Male\n"
                        + "Treatment Stage: Intake\n\nSESSION INFORMATION\nDate: Sept 22, 2026\n"
                        + "Type: Psychotherapy\nDuration: 60 minutes\n\nSubjective:\nImproved sleep onset.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        // The header comes from the note text alone: exactly one Client ID label.
        int first = html.indexOf("Client ID:");
        assertThat(first).isGreaterThan(-1);
        assertThat(html.indexOf("Client ID:", first + 1)).isEqualTo(-1);
        // The stylesheet always carries the selector; the rendered block must not.
        assertThat(html).doesNotContain("content client-info");
    }

    @Test
    @DisplayName("PDF styles section labels in AI plain text")
    void pdfShouldStyleSectionLabelsInPlainText() {
        sessionNote.setFinalContent("Subjective:\nImproved sleep onset.\n\nPlan:\nContinue weekly sessions.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("<span class=\"section-label\">Subjective:</span>");
        assertThat(html).contains("<span class=\"section-label\">Plan:</span>");
    }


    @Test
    @DisplayName("PDF supplies clinical fields that a SOAP note text leaves out")
    void pdfShouldSupplyClinicalFieldsMissingFromNoteText() {
        sessionNote.setSymptoms("Residual morning fatigue.");
        sessionNote.setShortTermGoals("Consistent bedtime for two weeks.");
        sessionNote.setFinalContent("Subjective:\nImproved sleep onset.\n\nPlan:\nContinue weekly sessions.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        // The ClientHub report always shows these; a SOAP note text never contains them.
        assertThat(html).contains("<p class=\"section-label\">Session Focus:</p><p>Anxiety management</p>");
        assertThat(html).contains("<p class=\"section-label\">Symptoms:</p><p>Residual morning fatigue.</p>");
        assertThat(html).contains("<p class=\"section-label\">Short-Term Goals:</p>");
        assertThat(html).contains("<p class=\"section-label\">Progress:</p><p>Client showed improvement</p>");
        // ...ahead of the narrative, which still prints.
        assertThat(html.indexOf("Session Focus:")).isLessThan(html.indexOf("Improved sleep onset."));
    }

    @Test
    @DisplayName("PDF does not repeat a clinical field the note text already covers")
    void pdfShouldNotRepeatFieldsTheNoteTextCovers() {
        sessionNote.setFinalContent("Session Focus:\nAnxiety management and sleep.\n\nProgress:\nSteady.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        int first = html.indexOf("Session Focus:");
        assertThat(first).isGreaterThan(-1);
        assertThat(html.indexOf("Session Focus:", first + 1)).isEqualTo(-1);
        assertThat(html).doesNotContain("Client showed improvement");
    }

    @Test
    @DisplayName("PDF treats a heading without a colon as covering its field")
    void pdfShouldNotRepeatFieldsUnderColonlessHeadings() {
        // The shape of an AI note on production: headings on their own line.
        sessionNote.setSymptoms("Stress");
        sessionNote.setFinalContent("<p><strong>Session Focus</strong></p><p>The focus was stress.</p>"
                + "<h3>Symptoms</h3><p>The client reported stress.</p>"
                + "<p><strong>Progress Remarks</strong><br>Receptive and engaged.</p>");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).doesNotContain("<p class=\"section-label\">Session Focus:</p>");
        assertThat(html).doesNotContain("<p class=\"section-label\">Symptoms:</p>");
        // "Progress Remarks" covers the progress field as well.
        assertThat(html).doesNotContain("Client showed improvement");
    }

    @Test
    @DisplayName("PDF does not mistake a sentence for a section heading")
    void pdfShouldNotTreatSentenceAsHeading() {
        sessionNote.setSymptoms("Residual morning fatigue.");
        sessionNote.setFinalContent("Symptoms of stress were discussed.\nProgress was steady overall.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("<p class=\"section-label\">Symptoms:</p><p>Residual morning fatigue.</p>");
        assertThat(html).contains("<p class=\"section-label\">Progress:</p><p>Client showed improvement</p>");
    }

    @Test
    @DisplayName("PDF prints each amendment after the signature with its reason, author and time")
    void pdfShouldPrintAmendmentsAfterSignature() {
        sessionNote.setIsFinalized(true);
        sessionNote.setFinalizedAt(Instant.parse("2026-09-19T17:00:00Z"));
        sessionNote.setFinalContent("Subjective:\nImproved sleep onset.");
        com.smart.therapy.flow.session.entity.SessionNoteAmendment amendment =
                com.smart.therapy.flow.session.entity.SessionNoteAmendment.builder()
                        .sessionNote(sessionNote)
                        .reason("Medication <name> corrected")
                        .amendmentText("Sertraline 50 mg, not 100 mg.\nConfirmed with client.")
                        .createdByUser(therapist)
                        .signedAt(Instant.parse("2026-09-20T15:30:00Z"))
                        .build();
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));
        when(sessionNoteAmendmentRepository.findBySessionNoteId(1L)).thenReturn(List.of(amendment));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).contains("Amendments (1)");
        assertThat(html).contains("Medication &lt;name&gt; corrected");
        assertThat(html).contains("Sertraline 50 mg, not 100 mg.<br>Confirmed with client.");
        assertThat(html).contains(therapist.getFullName() + " · September 20, 2026");
        // After the signature block, before the footer.
        assertThat(html.indexOf("Amendments (1)")).isGreaterThan(html.indexOf("class=\"signature\""));
        assertThat(html.indexOf("Amendments (1)")).isLessThan(html.indexOf("class=\"footer\""));
    }

    @Test
    @DisplayName("PDF has no amendments section when there are none")
    void pdfShouldOmitAmendmentsWhenNone() {
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));
        when(sessionNoteAmendmentRepository.findBySessionNoteId(1L)).thenReturn(List.of());

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).doesNotContain("Amendments (");
    }

    @Test
    @DisplayName("PDF leaves out clinical fields that were never filled")
    void pdfShouldSkipEmptyClinicalFields() {
        sessionNote.setRecommendations(null);
        sessionNote.setIntervention("  ");
        sessionNote.setFinalContent("Subjective:\nImproved sleep onset.");
        when(sessionNoteRepository.findById(1L)).thenReturn(Optional.of(sessionNote));

        String html = sessionNoteService.generatePdfHtml(1L, therapistPrincipal);

        assertThat(html).doesNotContain("Recommendations:");
        assertThat(html).doesNotContain(">Intervention:<");
    }

}
