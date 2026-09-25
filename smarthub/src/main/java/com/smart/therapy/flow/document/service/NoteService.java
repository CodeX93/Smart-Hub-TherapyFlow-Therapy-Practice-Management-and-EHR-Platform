package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.entity.Note;
import com.smart.therapy.flow.document.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoteService {

    private static final String RESOURCE_TYPE_NOTE = "note";

    private final NoteRepository noteRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<NoteResponse> getClientNotes(Long clientId, String noteType, Instant startDate, Instant endDate,
            AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify client exists
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<Note> notes = noteRepository.findByClientIdWithRelations(clientId).stream()
                .filter(note -> {
                    if (!org.springframework.util.StringUtils.hasText(noteType)) {
                        return true;
                    }
                    return note.getNoteType() != null
                            && note.getNoteType().name().equalsIgnoreCase(noteType.trim());
                })
                .filter(note -> startDate == null || (note.getEventDate() != null && !note.getEventDate().isBefore(startDate)))
                .filter(note -> endDate == null || (note.getEventDate() != null && !note.getEventDate().isAfter(endDate)))
                .sorted((a, b) -> {
                    Instant aDate = a.getEventDate();
                    Instant bDate = b.getEventDate();
                    if (aDate == null && bDate == null) {
                        return 0;
                    }
                    if (aDate == null) {
                        return 1;
                    }
                    if (bDate == null) {
                        return -1;
                    }
                    return bDate.compareTo(aDate);
                })
                .collect(Collectors.toList());

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), null, clientId,
                "notes_viewed", "api-request", "notes-api", "Viewed notes for client");

        return notes.stream()
                .map(this::toNoteResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoteResponse getNote(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), id, note.getClient().getId(),
                "note_viewed", "api-request", "notes-api", "Viewed note");

        return toNoteResponse(note);
    }

    @Transactional
    public NoteResponse createNote(CreateNoteRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify client exists
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Get author (current user)
        User author = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Build note
        Note note = Note.builder()
                .client(client)
                .createdByUser(author)
                .title(request.getTitle())
                .content(request.getContent())
                .noteType(request.getNoteType() != null
                        ? com.smart.therapy.flow.document.enums.NoteType.valueOf(request.getNoteType().toUpperCase())
                        : com.smart.therapy.flow.document.enums.NoteType.GENERAL)
                .eventDate(request.getEventDate() != null ? request.getEventDate() : Instant.now())
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
                .build();

        Note saved = noteRepository.save(note);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), saved.getId(), request.getClientId(),
                "note_created", ipAddress, "notes-api",
                "Created " + saved.getNoteType() + " note for client");

        return toNoteResponse(saved);
    }

    @Transactional
    public NoteResponse updateNote(Long id, UpdateNoteRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Note ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        User actor = currentUserService.requireCurrentUser(requester);
        User author = note.getCreatedByUser();
        if (actor.getId() == null || author == null || !actor.getId().equals(author.getId())) {
            throw new ForbiddenException("Not authorized to update a note authored by another user");
        }

        // Update fields
        if (request.getTitle() != null)
            note.setTitle(request.getTitle());
        if (request.getContent() != null)
            note.setContent(request.getContent());
        if (request.getNoteType() != null)
            note.setNoteType(
                    com.smart.therapy.flow.document.enums.NoteType.valueOf(request.getNoteType().toUpperCase()));
        if (request.getEventDate() != null)
            note.setEventDate(request.getEventDate());
        if (request.getIsPrivate() != null)
            note.setIsPrivate(request.getIsPrivate());

        Note saved = noteRepository.save(note);

        // Audit log
        recordAuditEvent(actor.getId(), requester.getLoginIdentifier(), id, note.getClient().getId(),
                "note_updated", ipAddress, "notes-api", "Updated note");

        return toNoteResponse(saved);
    }

    @Transactional
    public void deleteNote(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        Long clientId = note.getClient().getId();

        noteRepository.delete(note);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), id, clientId,
                "note_deleted", ipAddress, "notes-api", "Deleted note");
    }

    private NoteResponse toNoteResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .clientId(note.getClient().getId())
                .authorId(note.getCreatedByUser().getId())
                .authorName(note.getCreatedByUser().getFullName())
                .title(note.getTitle())
                .content(note.getContent())
                .noteType(note.getNoteType().name())
                .eventDate(note.getEventDate())
                .isPrivate(note.getIsPrivate())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    /**
     * Record audit event in a separate transaction to avoid Hibernate collection
     * loading issues.
     */
    private void recordAuditEvent(Long userId, String username, Long resourceId, Long clientId,
            String action, String ipAddress, String userAgent, String details) {
        auditService.recordAuditEventWithUserAndClient(userId, clientId, builder -> builder
                .action(action)
                .result("success")
                .resourceType(RESOURCE_TYPE_NOTE)
                .resourceId(resourceId != null ? resourceId.toString() : null)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .hipaaRelevant(true)
                .riskLevel("medium")
                .timestamp(Instant.now())
                .details(details));
    }
}
