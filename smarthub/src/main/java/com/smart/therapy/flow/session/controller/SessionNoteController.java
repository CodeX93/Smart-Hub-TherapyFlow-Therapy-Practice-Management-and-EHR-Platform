package com.smart.therapy.flow.session.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.session.dto.*;
import com.smart.therapy.flow.session.service.SessionNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/session-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Notes", description = "APIs for managing session notes and audio transcription")
public class SessionNoteController {

    private final SessionNoteService sessionNoteService;

    @GetMapping("/sessions/{sessionId}/notes")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<SessionNoteResponse>> getSessionNotes(
            @PathVariable("sessionId") Long sessionId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SessionNoteResponse> notes = sessionNoteService.getSessionNotesBySession(sessionId, principal);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/clients/{clientId}/session-notes")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<SessionNoteResponse>> getClientSessionNotes(
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SessionNoteResponse> notes = sessionNoteService.getSessionNotesByClient(clientId, principal);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<SessionNoteResponse> getSessionNote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SessionNoteResponse note = sessionNoteService.getSessionNote(id, principal);
        return ResponseEntity.ok(note);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Create session note",
            description = """
                    Create a new session note.
                    
                    **Required Fields:**
                    - `sessionId` (REQUIRED): ID of the session
                    - `clientId` (REQUIRED): ID of the client
                    - `therapistId` (REQUIRED): ID of the therapist
                    - `date` (REQUIRED): Date of the session (ISO 8601 format)
                    
                    **Optional Fields:**
                    - Clinical content: `sessionFocus`, `symptoms`, `shortTermGoals`, `intervention`, `progress`, `remarks`, `recommendations`
                    - Ratings (1-10): `clientRating`, `therapistRating`, `progressTowardGoals`, `moodBefore`, `moodAfter`
                    - Risk assessments (0-10): `riskSuicidalIdeation`, `riskSelfHarm`, `riskHomicidalIdeation`, `riskPsychosis`, `riskSubstanceUse`, `riskImpulsivity`, `riskAggression`, `riskTraumaSymptoms`, `riskNonAdherence`, `riskSupportSystem`
                    - Content state: `isDraft` (default: true), `isFinalized` (default: false)
                    - AI: `aiEnabled` (default: false), `customAiPrompt`
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Session note information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateSessionNoteRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Session Note",
                                    value = """
                                            {
                                              "sessionId": 1,
                                              "clientId": 123,
                                              "therapistId": 1,
                                              "date": "2025-12-25T14:00:00Z",
                                              "sessionFocus": "Anxiety management techniques",
                                              "symptoms": "Mild anxiety, improved mood",
                                              "intervention": "CBT techniques, mindfulness",
                                              "progress": "Client showed improvement",
                                              "clientRating": 7,
                                              "therapistRating": 8,
                                              "moodBefore": 4,
                                              "moodAfter": 7
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Session note created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionNoteResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<SessionNoteResponse> createSessionNote(
            @Valid @RequestBody CreateSessionNoteRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SessionNoteResponse note = sessionNoteService.createSessionNote(
                request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(note);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<SessionNoteResponse> updateSessionNote(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSessionNoteRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SessionNoteResponse note = sessionNoteService.updateSessionNote(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Void> deleteSessionNote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        sessionNoteService.deleteSessionNote(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<SessionNoteResponse> finalizeSessionNote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SessionNoteResponse note = sessionNoteService.finalizeSessionNote(
                id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(note);
    }

    @PostMapping("/{id}/unfinalize")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Reopen (unfinalize) session note",
            description = "Moves a finalized session note back to draft so it can be edited again. "
                    + "Same permission model as finalize: assigned therapist, supervising therapist, or admin. "
                    + "Matches ClientHub POST /api/session-notes/:id/unfinalize."
    )
    public ResponseEntity<SessionNoteResponse> unfinalizeSessionNote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SessionNoteResponse note = sessionNoteService.unfinalizeSessionNote(
                id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(note);
    }

    @PostMapping("/{id}/amendments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<SessionNoteAmendmentResponse> createAmendment(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateSessionNoteAmendmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SessionNoteAmendmentResponse amendment = sessionNoteService.createAmendment(
                id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(amendment);
    }

    @GetMapping("/{id}/amendments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<SessionNoteAmendmentResponse>> getAmendments(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SessionNoteAmendmentResponse> amendments = sessionNoteService.getAmendments(id, principal);
        return ResponseEntity.ok(amendments);
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Transcribe audio file",
            description = "Upload an audio file for transcription. Maximum file size is 25MB. Supported formats: audio/* and video/webm. Use multipart/form-data content type. Requires ADMIN, SUPERVISOR, or THERAPIST role JWT token.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TranscribeAudioResponse> transcribeAudio(
            @Parameter(description = "Optional session note ID to associate the transcription with")
            @RequestParam(value = "sessionNoteId", required = false) Long sessionNoteId,
            @Parameter(description = "The audio file to transcribe (multipart/form-data). Supported formats: audio/*, video/webm. Maximum size: 25MB", required = true)
            @RequestParam("audio") MultipartFile audioFile,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("No audio file uploaded");
        }
        String contentType = audioFile.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.equals("video/webm"))) {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("Only audio files are allowed");
        }
        if (audioFile.getSize() > 25 * 1024 * 1024) {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("Audio file size exceeds 25MB limit");
        }
        try {
            byte[] audioData = audioFile.getBytes();
            TranscribeAudioResponse response = sessionNoteService.transcribeAudio(
                    principal, sessionNoteId, audioData, audioFile.getOriginalFilename(),
                    HttpRequestUtil.getClientIp(httpRequest));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing audio transcription", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to process audio: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reprocess-audio")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Re-process audio transcription",
            description = "Re-processes stored audio file for a session note. Only available within retention period (30 days)."
    )
    public ResponseEntity<TranscribeAudioResponse> reprocessAudio(
            @PathVariable("id") Long sessionNoteId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            TranscribeAudioResponse response = sessionNoteService.reprocessAudioTranscription(
                    sessionNoteId, principal, HttpRequestUtil.getClientIp(httpRequest));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error re-processing audio transcription", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to re-process audio: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<String> getSessionNotePdf(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        String html = sessionNoteService.generatePdfHtml(id, principal);
        
        // Return HTML for browser PDF conversion
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(html);
    }

}
