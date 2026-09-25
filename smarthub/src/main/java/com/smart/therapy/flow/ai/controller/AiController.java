package com.smart.therapy.flow.ai.controller;

import com.smart.therapy.flow.ai.dto.*;
import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.session.service.SessionNoteAiTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "AI Services", description = "APIs for AI-powered features including session notes, templates, and clinical reports")
public class AiController {

    private final AiService aiService;
    private final SessionNoteAiTemplateService sessionNoteAiTemplateService;

    @PostMapping("/session-notes/template")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Generate session note template",
            description = """
                    Generate a session note template using AI.
                    
                    **Request Body:**
                    - See SessionNoteTemplateRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionNoteTemplateResponse> generateSessionNoteTemplate(
            @Valid @RequestBody SessionNoteTemplateRequest request
    ) {
        String content = aiService.generateSessionNoteTemplate(request);
        return ResponseEntity.ok(SessionNoteTemplateResponse.builder().generatedContent(content).build());
    }

    @PostMapping("/assistant/chat")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Chat with AI assistant",
            description = """
                    Have a conversation with the AI assistant.
                    
                    **Request Body:**
                    - See AssistantChatRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<AssistantChatResponse> chatWithAssistant(
            @Valid @RequestBody AssistantChatRequest request
    ) {
        String response = aiService.getAssistantResponse(request);
        return ResponseEntity.ok(AssistantChatResponse.builder().message(response).build());
    }

    @PostMapping("/generate-template")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<GenerateTemplateResponse> generateTemplate(
            @Valid @RequestBody GenerateTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        String instructions = resolveSessionNoteInstructions(request, principal);
        request.setCustomInstructions(instructions);
        if (request.getTemplateId() != null) {
            sessionNoteAiTemplateService.markLastUsed(request.getTemplateId(), principal);
        }
        String content = aiService.generateAITemplate(request);
        return ResponseEntity.ok(GenerateTemplateResponse.builder().generatedContent(content).build());
    }

    private String resolveSessionNoteInstructions(GenerateTemplateRequest request, AuthPrincipal principal) {
        if (StringUtils.hasText(request.getCustomInstructions())) {
            return request.getCustomInstructions().trim();
        }
        if (request.getTemplateId() != null) {
            return sessionNoteAiTemplateService.resolveInstructions(request.getTemplateId(), principal);
        }
        throw new BadRequestException("customInstructions or templateId is required");
    }

    @GetMapping("/templates")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<TemplatesResponse> getTemplates() {
        Map<String, Object> templates = aiService.getAllTemplates();
        return ResponseEntity.ok(TemplatesResponse.builder().templates(templates).build());
    }

    @PostMapping("/generate-from-template")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<GenerateFromTemplateResponse> generateFromTemplate(
            @Valid @RequestBody GenerateFromTemplateRequest request
    ) {
        String content = aiService.generateFromTemplate(request.getTemplateId(), request.getField(), request.getContext());
        return ResponseEntity.ok(GenerateFromTemplateResponse.builder().content(content).build());
    }

    @GetMapping("/field-options/{templateId}/{field}")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<FieldOptionsResponse> getFieldOptions(
            @PathVariable("templateId") String templateId,
            @PathVariable("field") String field
    ) {
        List<FieldOptionDto> options = aiService.getFieldOptions(templateId, field);
        return ResponseEntity.ok(FieldOptionsResponse.builder().options(options).build());
    }

    @PostMapping("/connected-suggestions")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<ConnectedSuggestionsResponse> getConnectedSuggestions(
            @Valid @RequestBody ConnectedSuggestionsRequest request
    ) {
        Map<String, List<String>> suggestions = aiService.getConnectedSuggestions(
                request.getTemplateId(), request.getSourceField(), request.getSourceValue(), request.getClientId()
        );
        return ResponseEntity.ok(ConnectedSuggestionsResponse.builder().suggestions(suggestions).build());
    }

    @PostMapping("/generate-suggestions")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<GenerateSuggestionsResponse> generateSuggestions(
            @Valid @RequestBody GenerateSuggestionsRequest request
    ) {
        List<String> suggestions = aiService.generateSmartSuggestions(request.getField(), request.getContext(), request.getClientId());
        return ResponseEntity.ok(GenerateSuggestionsResponse.builder().suggestions(suggestions).build());
    }

    @PostMapping("/generate-clinical-report")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Generate clinical report",
            description = """
                    Generate a clinical report using AI based on client data.
                    
                    **Request Body:**
                    - See GenerateClinicalReportRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<GenerateClinicalReportResponse> generateClinicalReport(
            @Valid @RequestBody GenerateClinicalReportRequest request
    ) {
        String report = aiService.generateClinicalReport(request);
        return ResponseEntity.ok(GenerateClinicalReportResponse.builder().report(report).build());
    }

    @PostMapping("/regenerate-content/{sessionNoteId}")
    @PreAuthorize("(" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and " + PermissionConstants.AI_USE)
    public ResponseEntity<SessionNoteTemplateResponse> regenerateContent(
            @PathVariable("sessionNoteId") Long sessionNoteId,
            @Valid @RequestBody(required = false) RegenerateContentRequest request
    ) {
        String customPrompt = request != null ? request.getCustomPrompt() : null;
        String content = aiService.regenerateContent(sessionNoteId, customPrompt);
        return ResponseEntity.ok(SessionNoteTemplateResponse.builder().generatedContent(content).build());
    }
}

