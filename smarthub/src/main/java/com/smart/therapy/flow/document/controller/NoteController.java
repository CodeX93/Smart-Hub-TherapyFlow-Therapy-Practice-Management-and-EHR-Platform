package com.smart.therapy.flow.document.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.service.NoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteController {

    private final NoteService noteService;

    @GetMapping("/clients/{clientId}/notes")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<NoteResponse>> getClientNotes(
            @PathVariable("clientId") Long clientId,
            @RequestParam(required = false) String noteType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<NoteResponse> notes = noteService.getClientNotes(clientId, noteType, startDate, endDate, principal);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<NoteResponse> getNote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        NoteResponse note = noteService.getNote(id, principal);
        return ResponseEntity.ok(note);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create client note",
            description = """
                    Create a new note for a client.
                    
                    **Required Fields:**
                    - `clientId` (REQUIRED): ID of the client
                    - `content` (REQUIRED): Note content
                    - `eventDate` (REQUIRED): Date of the event (ISO 8601 format)
                    
                    **Optional Fields:**
                    - `title` (optional): Note title
                    - `noteType` (optional): Type of note (call, email, note, general, clinical, supervisor)
                    - `isPrivate` (optional, default: false): Whether the note is private
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Note information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateNoteRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Note",
                                    value = """
                                            {
                                              "clientId": 123,
                                              "title": "Phone call with client",
                                              "content": "Client called to reschedule appointment. Expressed concern about upcoming session.",
                                              "noteType": "call",
                                              "eventDate": "2025-12-25T14:00:00Z",
                                              "isPrivate": false
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Note created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = NoteResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<NoteResponse> createNote(
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        NoteResponse note = noteService.createNote(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(note);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateNoteRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        NoteResponse note = noteService.updateNote(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Void> deleteNote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        noteService.deleteNote(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

}


