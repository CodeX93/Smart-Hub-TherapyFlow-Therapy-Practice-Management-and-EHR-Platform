package com.smart.therapy.flow.session.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.session.dto.CreateSessionNoteAiTemplateRequest;
import com.smart.therapy.flow.session.dto.SessionNoteAiTemplateResponse;
import com.smart.therapy.flow.session.dto.UpdateSessionNoteAiTemplateRequest;
import com.smart.therapy.flow.session.service.SessionNoteAiTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/session-note-ai-templates")
@RequiredArgsConstructor
@Tag(name = "Session Note AI Templates", description = "Per-therapist AI instruction templates for session note generation")
public class SessionNoteAiTemplateController {

    private static final String AI_TEMPLATE_ACCESS = "(" + RoleConstants.ROLE_ADMIN + " or "
            + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and "
            + PermissionConstants.AI_USE;

    private final SessionNoteAiTemplateService templateService;

    @GetMapping
    @PreAuthorize(AI_TEMPLATE_ACCESS)
    @Operation(summary = "List my session note AI templates",
            description = "Returns templates owned by the current user, most recently used first.")
    public ResponseEntity<List<SessionNoteAiTemplateResponse>> listTemplates(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(templateService.listTemplates(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AI_TEMPLATE_ACCESS)
    public ResponseEntity<SessionNoteAiTemplateResponse> getTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(templateService.getTemplate(id, principal));
    }

    @PostMapping
    @PreAuthorize(AI_TEMPLATE_ACCESS)
    @Operation(summary = "Create session note AI template",
            description = "Stores therapist custom instructions used by Generate Final Note.")
    public ResponseEntity<SessionNoteAiTemplateResponse> createTemplate(
            @Valid @RequestBody CreateSessionNoteAiTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        SessionNoteAiTemplateResponse created = templateService.createTemplate(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AI_TEMPLATE_ACCESS)
    public ResponseEntity<SessionNoteAiTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionNoteAiTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AI_TEMPLATE_ACCESS)
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        templateService.deleteTemplate(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-last-used")
    @PreAuthorize(AI_TEMPLATE_ACCESS)
    @Operation(summary = "Mark template as last used",
            description = "Updates lastUsedAt for dropdown ordering and replaces browser lastUsedTemplate behavior.")
    public ResponseEntity<SessionNoteAiTemplateResponse> markLastUsed(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(templateService.markLastUsed(id, principal));
    }
}
