package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.document.dto.CreateNoteRequest;
import com.smart.therapy.flow.document.dto.NoteResponse;
import com.smart.therapy.flow.document.dto.UpdateNoteRequest;
import com.smart.therapy.flow.document.entity.Note;
import com.smart.therapy.flow.document.repository.NoteRepository;
import com.smart.therapy.flow.document.service.NoteService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService Unit Tests")
@SuppressWarnings("null") // Suppress null warnings from Mockito mocks
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NoteService noteService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;
    private Client client;
    private Note note;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        client = TestDataFactory.createTestClient();
        client.setId(1L);
        client.setAssignedTherapist(therapist);

        note = Note.builder()
                .client(client)
                .createdByUser(therapist) // Changed from .author()
                .title("Progress Note")
                .content("Client is making good progress")
                .noteType(com.smart.therapy.flow.document.enums.NoteType.CLINICAL) // Using enum
                .isPrivate(false)
                .build();
        note.setId(1L);
    }

    @Test
    @DisplayName("Should create note successfully")
    void shouldCreateNoteSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        CreateNoteRequest request = new CreateNoteRequest();
        request.setClientId(1L);
        request.setTitle("Progress Note");
        request.setContent("Client is making good progress");
        request.setNoteType("clinical");
        request.setEventDate(java.time.Instant.now());

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteResponse response = noteService.createNote(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Progress Note");
        verify(clientRepository).findById(1L);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when client not found")
    void shouldThrowExceptionWhenClientNotFound() {
        // Arrange
        CreateNoteRequest request = new CreateNoteRequest();
        request.setClientId(999L);
        request.setTitle("Test Note");

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.createNote(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Should get note by ID successfully")
    void shouldGetNoteByIdSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        Long noteId = 1L;
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        // Act
        NoteResponse response = noteService.getNote(noteId, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(noteId);
        assertThat(response.getTitle()).isEqualTo("Progress Note");
        verify(noteRepository).findById(noteId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when note not found")
    void shouldThrowExceptionWhenNoteNotFound() {
        // Arrange
        Long noteId = 999L;
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.getNote(noteId, therapistPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");

        verify(noteRepository).findById(noteId);
    }

    @Test
    @DisplayName("Should update note successfully")
    void shouldUpdateNoteSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        Long noteId = 1L;
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitle("Updated Note Title");
        request.setContent("Updated content");

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteResponse response = noteService.updateNote(noteId, request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response.getTitle()).isEqualTo("Updated Note Title");
        assertThat(response.getContent()).isEqualTo("Updated content");
        verify(noteRepository).findById(noteId);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when updating note not authored by user")
    void shouldThrowExceptionWhenUpdatingNoteNotAuthoredByUser() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        Long noteId = 1L;
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitle("Updated Title");

        User otherTherapist = TestDataFactory.createTestTherapist();
        otherTherapist.setId(999L);
        note.setCreatedByUser(otherTherapist);

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

        // Act & Assert
        assertThatThrownBy(() -> noteService.updateNote(noteId, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Not authorized");

        verify(noteRepository, never()).save(any(Note.class));
        assertThat(note.getTitle()).isEqualTo("Progress Note");
        verifyNoInteractions(auditService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void shouldRejectUpdateWithoutPersistedAuthor(boolean missingAuthor) {
        note.setCreatedByUser(missingAuthor ? null : new User());
        when(currentUserService.requireCurrentUser(therapistPrincipal)).thenReturn(therapist);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitle("Changed");
        assertThatThrownBy(() -> noteService.updateNote(1L, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(note.getTitle()).isEqualTo("Progress Note");
        verify(noteRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should get client notes successfully")
    void shouldGetClientNotesSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        Long clientId = 1L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(noteRepository.findByClientIdWithRelations(clientId)).thenReturn(List.of(note));

        // Act
        List<NoteResponse> notes = noteService.getClientNotes(clientId, null, null, null, therapistPrincipal);

        // Assert
        assertThat(notes).isNotNull();
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitle()).isEqualTo("Progress Note");
        verify(clientRepository).findById(clientId);
        verify(noteRepository).findByClientIdWithRelations(clientId);
    }

    @Test
    @DisplayName("Should delete note successfully")
    void shouldDeleteNoteSuccessfully() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        // Arrange
        Long noteId = 1L;
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        doNothing().when(noteRepository).delete(note);

        // Act
        noteService.deleteNote(noteId, therapistPrincipal, "127.0.0.1");

        // Assert
        verify(noteRepository).findById(noteId);
        verify(noteRepository).delete(note);
    }
}
