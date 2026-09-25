package com.smart.therapy.flow.assessment.controller;

import com.smart.therapy.flow.assessment.dto.*;
import com.smart.therapy.flow.assessment.service.AssessmentService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/templates")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    public ResponseEntity<List<AssessmentTemplateResponse>> getTemplates() {
        List<AssessmentTemplateResponse> templates = assessmentService.getTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    public ResponseEntity<AssessmentTemplateResponse> getTemplate(@PathVariable("id") Long id) {
        AssessmentTemplateResponse template = assessmentService.getTemplate(id);
        return ResponseEntity.ok(template);
    }

    @GetMapping("/templates/{id}/sections")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get template sections",
            description = "Retrieves all sections (with questions) for a specific assessment template."
    )
    public ResponseEntity<List<AssessmentSectionResponse>> getTemplateSections(@PathVariable("id") Long id) {
        List<AssessmentSectionResponse> sections = assessmentService.getTemplateSections(id);
        return ResponseEntity.ok(sections);
    }

    @PostMapping("/templates")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create assessment template",
            description = """
                    Create a new assessment template.
                    
                    **Required Fields:**
                    - `name` (REQUIRED): Assessment template name
                    
                    **Optional Fields:**
                    - `description` (optional): Assessment template description
                    - `category` (optional): Assessment category
                    - `isStandardized` (optional, default: false): Whether this is a standardized assessment
                    - `version` (optional, default: 1): Template version
                    - `sections` (optional): List of assessment sections/questions
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Assessment template information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateAssessmentTemplateRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Assessment Template",
                                    value = """
                                            {
                                              "name": "PHQ-9 Depression Assessment",
                                              "description": "Patient Health Questionnaire for depression screening",
                                              "category": "Mental Health",
                                              "isStandardized": true,
                                              "version": 1
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Assessment template created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AssessmentTemplateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<AssessmentTemplateResponse> createTemplate(
            @Valid @RequestBody CreateAssessmentTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentTemplateResponse template = assessmentService.createTemplate(request, principal);
        return ResponseEntity.status(201).body(template);
    }

    @GetMapping("/clients/{clientId}/assessments")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    public ResponseEntity<List<AssessmentAssignmentResponse>> getClientAssessments(
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentAssignmentResponse> assignments = assessmentService.getClientAssessments(clientId, principal);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/clients/{clientId}/assessments")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    public ResponseEntity<AssessmentAssignmentResponse> assignAssessment(
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody CreateAssessmentAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        AssessmentAssignmentResponse assignment = assessmentService.assignAssessment(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(assignment);
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    public ResponseEntity<AssessmentAssignmentResponse> getAssignment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentAssignmentResponse assignment = assessmentService.getAssignment(id, principal);
        return ResponseEntity.ok(assignment);
    }

    @PostMapping("/assignments/{id}/responses")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    public ResponseEntity<AssessmentAssignmentResponse> submitResponses(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubmitAssessmentResponseRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        // Ensure assignment ID matches path
        request.setAssignmentId(id);
        AssessmentAssignmentResponse assignment = assessmentService.submitResponses(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(assignment);
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        assessmentService.deleteAssignment(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments/{id}/responses/batch")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Batch save assessment responses",
            description = "Save multiple responses at once for better performance. Auto-updates status from pending to client_in_progress."
    )
    public ResponseEntity<AssessmentAssignmentResponse> submitBatchResponses(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubmitAssessmentResponseRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        request.setAssignmentId(id);
        AssessmentAssignmentResponse assignment = assessmentService.submitBatchResponses(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/assignments/{assignmentId}/report")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get assessment report",
            description = "Retrieves the assessment report for an assignment. Returns finalized content if available, otherwise draft or generated content."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.entity.AssessmentReport> getReport(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        com.smart.therapy.flow.assessment.entity.AssessmentReport report = assessmentService.getReport(assignmentId);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/assignments/{assignmentId}/generate-report")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Generate AI assessment report",
            description = "Generates an AI-powered assessment report from responses. Requires AI processing consent from the client. Updates status to waiting_for_therapist."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.entity.AssessmentReport> generateReport(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.assessment.entity.AssessmentReport report = assessmentService.generateReport(
                assignmentId, 
                principal, 
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)
        );
        return ResponseEntity.status(201).body(report);
    }

    @PutMapping("/assignments/{assignmentId}/report")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update report draft",
            description = "Updates the draft content of an assessment report. Cannot edit finalized reports."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.entity.AssessmentReport> updateReportDraft(
            @PathVariable("assignmentId") Long assignmentId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        String draftContent = request.get("draftContent");
        if (draftContent == null) {
            return ResponseEntity.badRequest().build();
        }
        com.smart.therapy.flow.assessment.entity.AssessmentReport report = assessmentService.updateReportDraft(
                assignmentId,
                draftContent,
                principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)
        );
        return ResponseEntity.ok(report);
    }

    @PostMapping("/assignments/{assignmentId}/report/finalize")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Finalize assessment report",
            description = "Finalizes an assessment report, locking it from further edits. Only assigned therapist, supervisor, or admin can finalize. Updates assignment status to completed."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.entity.AssessmentReport> finalizeReport(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.assessment.entity.AssessmentReport report = assessmentService.finalizeReport(
                assignmentId,
                principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)
        );
        return ResponseEntity.ok(report);
    }

    @PostMapping("/assignments/{assignmentId}/report/unfinalize")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Unfinalize assessment report",
            description = "Unfinalizes a finalized report, allowing edits again. Only assigned therapist, supervisor, or admin can unfinalize. Updates assignment status back to therapist_completed."
    )
    public ResponseEntity<com.smart.therapy.flow.assessment.entity.AssessmentReport> unfinalizeReport(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        com.smart.therapy.flow.assessment.entity.AssessmentReport report = assessmentService.unfinalizeReport(
                assignmentId,
                principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)
        );
        return ResponseEntity.ok(report);
    }

    @PostMapping("/transcribe")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Transcribe assessment audio",
            description = "Transcribes audio for assessment responses using OpenAI Whisper. Requires AI processing consent if assignmentId is provided."
    )
    public ResponseEntity<Map<String, String>> transcribeAudio(
            @RequestParam("audio") org.springframework.web.multipart.MultipartFile audioFile,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            byte[] audioData = audioFile.getBytes();
            String fileName = audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "audio.webm";
            String transcription = assessmentService.transcribeAudio(
                    audioData,
                    fileName,
                    assignmentId,
                    principal,
                    HttpRequestUtil.getClientIp(httpRequest),
                    HttpRequestUtil.getUserAgent(httpRequest)
            );
            return ResponseEntity.ok(Map.of("transcription", transcription));
        } catch (Exception e) {
            log.error("Failed to transcribe audio for assessment", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/assignments/{assignmentId}/download/pdf")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Download assessment report as PDF",
            description = "Generates HTML for PDF printing. Browser will handle PDF conversion via print dialog."
    )
    public ResponseEntity<String> downloadAssessmentPdf(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            String html = assessmentService.generateAssessmentPdfHtml(
                    assignmentId, 
                    principal, 
                    HttpRequestUtil.getClientIp(httpRequest)
            );
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/html")
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                    .header(org.springframework.http.HttpHeaders.EXPIRES, "0")
                    .body(html);
        } catch (Exception e) {
            log.error("Failed to generate PDF HTML for assessment assignment {}", assignmentId, e);
            return ResponseEntity.status(500).body("<html><body><h1>Error generating PDF</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    @GetMapping("/assignments/{assignmentId}/download/docx")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Download assessment report as Word document",
            description = "Generates a Word document (.docx) of the assessment report."
    )
    public ResponseEntity<org.springframework.core.io.Resource> downloadAssessmentDocx(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            byte[] docxBytes = assessmentService.downloadAssessmentDocx(assignmentId, principal, HttpRequestUtil.getClientIp(httpRequest));
            org.springframework.core.io.ByteArrayResource resource = 
                    new org.springframework.core.io.ByteArrayResource(docxBytes);
            
            String filename = String.format("assessment-report-%d.docx", assignmentId);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to generate DOCX for assessment assignment {}", assignmentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    // ========== TEMPLATE UPDATE/DELETE ==========

    @PutMapping("/templates/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update assessment template",
            description = "Updates an existing assessment template. Cannot update templates with active assignments."
    )
    public ResponseEntity<AssessmentTemplateResponse> updateTemplate(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAssessmentTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentTemplateResponse template = assessmentService.updateTemplate(id, request, principal);
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete assessment template",
            description = "Deletes an assessment template. Cannot delete templates with existing assignments."
    )
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        assessmentService.deleteTemplate(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== SECTION CRUD ==========

    @PostMapping("/templates/{templateId}/sections")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create assessment section",
            description = "Creates a new section in an assessment template."
    )
    public ResponseEntity<AssessmentSectionResponse> createSection(
            @PathVariable("templateId") Long templateId,
            @Valid @RequestBody AssessmentSectionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentSectionResponse section = assessmentService.createSection(templateId, request, principal);
        return ResponseEntity.status(201).body(section);
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update assessment section",
            description = "Updates an existing assessment section. Cannot update sections with existing responses."
    )
    public ResponseEntity<AssessmentSectionResponse> updateSection(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAssessmentSectionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentSectionResponse section = assessmentService.updateSection(id, request, principal);
        return ResponseEntity.ok(section);
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete assessment section",
            description = "Deletes an assessment section. Cannot delete sections with existing responses."
    )
    public ResponseEntity<Void> deleteSection(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        assessmentService.deleteSection(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== QUESTION CRUD ==========

    @PostMapping("/sections/{sectionId}/questions")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create assessment question",
            description = "Creates a new question in an assessment section."
    )
    public ResponseEntity<AssessmentQuestionResponse> createQuestion(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody AssessmentQuestionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentQuestionResponse question = assessmentService.createQuestion(sectionId, request, principal);
        return ResponseEntity.status(201).body(question);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update assessment question",
            description = "Updates an existing assessment question. Cannot update questions with existing responses."
    )
    public ResponseEntity<AssessmentQuestionResponse> updateQuestion(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAssessmentQuestionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentQuestionResponse question = assessmentService.updateQuestion(id, request, principal);
        return ResponseEntity.ok(question);
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete assessment question",
            description = "Deletes an assessment question. Cannot delete questions with existing responses."
    )
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        assessmentService.deleteQuestion(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== RECALCULATE SCORES ==========

    @PostMapping("/assignments/{id}/recalculate-scores")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Recalculate assessment scores",
            description = "Recalculates the total score for an assessment assignment based on all responses."
    )
    public ResponseEntity<AssessmentAssignmentResponse> recalculateScores(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentAssignmentResponse assignment = assessmentService.recalculateScores(id, principal);
        return ResponseEntity.ok(assignment);
    }

    // ========== STATUS TRANSITIONS ==========

    @PutMapping("/assignments/{id}/status")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update assessment status",
            description = "Updates the status of an assessment assignment. Validates status transitions."
    )
    public ResponseEntity<AssessmentAssignmentResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAssessmentStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentAssignmentResponse assignment = assessmentService.updateStatus(id, request, principal);
        return ResponseEntity.ok(assignment);
    }

    // ========== SERVER-SIDE PDF GENERATION ==========

    @GetMapping("/assignments/{assignmentId}/download/pdf-file")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Download assessment report as PDF file",
            description = "Generates an actual PDF file (not HTML) of the assessment report using server-side PDF generation."
    )
    public ResponseEntity<org.springframework.core.io.Resource> downloadAssessmentPdfFile(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            byte[] pdfBytes = assessmentService.generateAssessmentPdf(assignmentId, principal);
            org.springframework.core.io.ByteArrayResource resource = 
                    new org.springframework.core.io.ByteArrayResource(pdfBytes);
            
            String filename = String.format("assessment-report-%d.pdf", assignmentId);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to generate PDF for assessment assignment {}", assignmentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    // ========== BULK OPERATIONS ==========

    @PostMapping("/templates/{templateId}/sections/bulk")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Bulk create sections",
            description = "Creates multiple sections at once for better performance."
    )
    public ResponseEntity<List<AssessmentSectionResponse>> bulkCreateSections(
            @PathVariable("templateId") Long templateId,
            @Valid @RequestBody BulkSectionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentSectionResponse> sections = assessmentService.bulkCreateSections(templateId, request, principal);
        return ResponseEntity.status(201).body(sections);
    }

    @PostMapping("/sections/{sectionId}/questions/bulk")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Bulk create questions",
            description = "Creates multiple questions at once for better performance."
    )
    public ResponseEntity<List<AssessmentQuestionResponse>> bulkCreateQuestions(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody BulkQuestionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentQuestionResponse> questions = assessmentService.bulkCreateQuestions(sectionId, request, principal);
        return ResponseEntity.status(201).body(questions);
    }

    @DeleteMapping("/sections/bulk")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Bulk delete sections",
            description = "Deletes multiple sections at once. Sections with existing responses cannot be deleted."
    )
    public ResponseEntity<Map<String, Object>> bulkDeleteSections(
            @Valid @RequestBody BulkDeleteRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Map<String, Object> result = assessmentService.bulkDeleteSections(request, principal);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/questions/bulk")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Bulk delete questions",
            description = "Deletes multiple questions at once. Questions with existing responses cannot be deleted."
    )
    public ResponseEntity<Map<String, Object>> bulkDeleteQuestions(
            @Valid @RequestBody BulkDeleteRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Map<String, Object> result = assessmentService.bulkDeleteQuestions(request, principal);
        return ResponseEntity.ok(result);
    }

    // ========== QUESTION OPTIONS ==========

    @GetMapping("/questions/{questionId}/options")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get assessment question options",
            description = "Retrieves all options for a given assessment question."
    )
    public ResponseEntity<List<AssessmentQuestionOptionResponse>> getQuestionOptions(
            @PathVariable("questionId") Long questionId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentQuestionOptionResponse> options = assessmentService.getQuestionOptions(questionId, principal);
        return ResponseEntity.ok(options);
    }

    @DeleteMapping("/questions/{questionId}/options")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete all options for a question",
            description = "Deletes all options associated with a given assessment question."
    )
    public ResponseEntity<Void> deleteQuestionOptions(
            @PathVariable("questionId") Long questionId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        assessmentService.deleteQuestionOptions(questionId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/question-options")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create assessment question option",
            description = "Creates a single option for an assessment question."
    )
    public ResponseEntity<AssessmentQuestionOptionResponse> createQuestionOption(
            @Valid @RequestBody CreateAssessmentQuestionOptionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentQuestionOptionResponse option = assessmentService.createQuestionOption(request, principal);
        return ResponseEntity.status(201).body(option);
    }

    @PostMapping("/question-options/bulk")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Bulk create assessment question options",
            description = "Creates multiple options for a given assessment question."
    )
    public ResponseEntity<List<AssessmentQuestionOptionResponse>> bulkCreateQuestionOptions(
            @Valid @RequestBody BulkAssessmentQuestionOptionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentQuestionOptionResponse> options = assessmentService.bulkCreateQuestionOptions(request, principal);
        return ResponseEntity.status(201).body(options);
    }

    @PatchMapping("/question-options/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update assessment question option",
            description = "Updates an existing assessment question option."
    )
    public ResponseEntity<AssessmentQuestionOptionResponse> updateQuestionOption(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAssessmentQuestionOptionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentQuestionOptionResponse option = assessmentService.updateQuestionOption(id, request, principal);
        return ResponseEntity.ok(option);
    }

    @DeleteMapping("/question-options/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete assessment question option",
            description = "Deletes a single assessment question option by ID."
    )
    public ResponseEntity<Void> deleteQuestionOption(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        assessmentService.deleteQuestionOption(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== TEMPLATE VERSIONING ==========

    @PostMapping("/templates/{id}/versions")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create template version",
            description = """
                    Creates a new version of an existing template with all sections and questions copied.
                    
                    **Path Parameters:**
                    - `id` (REQUIRED): Template ID to version
                    
                    **Query Parameters:**
                    - `version` (optional): Specific version number. If not provided, auto-increments from current version.
                    
                    **Requires:** ADMIN role.
                    """
    )
    public ResponseEntity<AssessmentTemplateResponse> createTemplateVersion(
            @PathVariable("id") Long id,
            @RequestParam(value = "version", required = false) String version,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AssessmentTemplateResponse template = assessmentService.createTemplateVersion(id, version, principal);
        return ResponseEntity.status(201).body(template);
    }

    @GetMapping("/templates/{id}/versions")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get template version history",
            description = """
                    Retrieves all versions of a template, sorted by version number (newest first).
                    
                    **Path Parameters:**
                    - `id` (REQUIRED): Template ID (any version)
                    
                    **Returns:** List of all template versions with their version numbers.
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """
    )
    public ResponseEntity<List<AssessmentTemplateResponse>> getTemplateVersions(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentTemplateResponse> versions = assessmentService.getTemplateVersions(id);
        return ResponseEntity.ok(versions);
    }

    // ========== ASSESSMENT ANALYTICS ==========

    @GetMapping("/analytics")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get assessment analytics",
            description = "Retrieves comprehensive analytics and statistics for assessments. Can be filtered by template, client, or date range."
    )
    public ResponseEntity<AssessmentAnalyticsResponse> getAnalytics(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate,
            @RequestParam(value = "endDate", required = false) java.time.LocalDate endDate
    ) {
        AssessmentAnalyticsResponse analytics = assessmentService.getAnalytics(templateId, clientId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    // ========== REMINDER SYSTEM ==========

    @PostMapping("/assignments/{id}/reminders")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create assessment reminder",
            description = "Creates a reminder for an assessment assignment. Can send email and in-app notifications."
    )
    public ResponseEntity<Void> createReminder(
            @PathVariable("id") Long id,
            @Valid @RequestBody AssessmentReminderRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        request.setAssignmentId(id);
        assessmentService.createAssessmentReminder(request, principal);
        return ResponseEntity.status(201).build();
    }

    // ========== ASSIGNMENT LISTING & CREATION ==========

    @GetMapping("/assignments")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "List assessment assignments",
            description = "Lists assessment assignments with optional filters for template, client, status, date range, search, and pagination."
    )
    public ResponseEntity<PaginatedResponse<AssessmentAssignmentResponse>> listAssignments(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) java.time.Instant from,
            @RequestParam(value = "to", required = false) java.time.Instant to,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize
    ) {
        PaginatedResponse<AssessmentAssignmentResponse> assignments = assessmentService.getAssignments(
                templateId, clientId, status, from, to, search, page, pageSize);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/assignments")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create assessment assignment",
            description = "Creates a new assessment assignment using a request body containing template and client information."
    )
    public ResponseEntity<AssessmentAssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssessmentAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        AssessmentAssignmentResponse assignment = assessmentService.assignAssessment(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(assignment);
    }

    // ========== RESPONSES ROOT ENDPOINTS ==========

    @GetMapping("/assignments/{assignmentId}/responses")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get assessment responses for an assignment",
            description = "Retrieves all responses for a given assessment assignment."
    )
    public ResponseEntity<List<AssessmentResponseDto>> getAssignmentResponses(
            @PathVariable("assignmentId") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<AssessmentResponseDto> responses = assessmentService.getAssignmentResponses(assignmentId, principal);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/responses")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_ASSIGN_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Submit assessment responses (root endpoint)",
            description = "Submits assessment responses using a request body containing assignmentId and responses."
    )
    public ResponseEntity<AssessmentAssignmentResponse> submitResponsesRoot(
            @Valid @RequestBody SubmitAssessmentResponseRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        AssessmentAssignmentResponse assignment = assessmentService.submitResponses(
                request,
                principal,
                HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(assignment);
    }

    // ========== EXPORT OPTIONS ==========

    @GetMapping("/export/csv")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Export assessment data as CSV",
            description = "Exports assessment assignment data as CSV file. Can be filtered by template, client, or date range."
    )
    public ResponseEntity<org.springframework.core.io.Resource> exportAsCsv(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate,
            @RequestParam(value = "endDate", required = false) java.time.LocalDate endDate
    ) {
        byte[] csvBytes = assessmentService.exportAssessmentDataAsCsv(templateId, clientId, startDate, endDate);
        org.springframework.core.io.ByteArrayResource resource = 
                new org.springframework.core.io.ByteArrayResource(csvBytes);
        
        String filename = String.format("assessment-export-%s.csv", 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/export/excel")
    @PreAuthorize(StaffAuthorizationExpressions.ASSESSMENT_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Export assessment data as Excel",
            description = "Exports assessment assignment data as Excel file (.xlsx). Can be filtered by template, client, or date range."
    )
    public ResponseEntity<org.springframework.core.io.Resource> exportAsExcel(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate,
            @RequestParam(value = "endDate", required = false) java.time.LocalDate endDate
    ) {
        byte[] excelBytes = assessmentService.exportAssessmentDataAsExcel(templateId, clientId, startDate, endDate);
        org.springframework.core.io.ByteArrayResource resource = 
                new org.springframework.core.io.ByteArrayResource(excelBytes);
        
        String filename = String.format("assessment-export-%s.xlsx", 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

}


