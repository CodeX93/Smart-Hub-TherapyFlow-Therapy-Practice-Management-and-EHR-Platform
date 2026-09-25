package com.smart.therapy.flow.session.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.session.dto.SessionTranscriptStatusResponse;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/session-transcripts")
@RequiredArgsConstructor
@Tag(name = "Session Transcripts", description = "Transcript status and lifecycle visibility APIs")
public class SessionTranscriptController {

    private final SessionTranscriptService sessionTranscriptService;

    @GetMapping("/status")
    @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
    @Operation(summary = "List transcript pipeline statuses", description = "Returns transcript status records visible to the current staff user.")
    public ResponseEntity<List<SessionTranscriptStatusResponse>> getTranscriptStatuses(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(sessionTranscriptService.getTranscriptStatuses(principal));
    }
}
