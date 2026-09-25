package com.smart.therapy.flow.document.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.dto.DocumentSummaryResponse;
import com.smart.therapy.flow.document.dto.DocumentReviewDashboardResponse;
import com.smart.therapy.flow.document.dto.DocumentReviewSummaryResponse;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Review", description = "Review queue for documents across clients")
public class DocumentReviewController {

    private final DocumentService documentService;

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get document review queue",
            description = """
                    Retrieve documents awaiting review across your accessible clients.
                    
                    **Query Parameters:**
                    - `status` (OPTIONAL): PENDING, THERAPIST_REVIEW, SUPERVISOR_REVIEW, or OVERDUE
                    - `overdueOnly` (OPTIONAL): true/false (overrides status to return only overdue items)
                    - `clientId` (OPTIONAL): filter by client
                    - `page` (OPTIONAL): 0-based page index
                    - `pageSize` (OPTIONAL): page size (max 200)
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<DocumentSummaryResponse>> getReviewQueue(
            @Parameter(description = "Review status filter", example = "PENDING")
            @RequestParam(required = false) ReviewStatus status,
            @Parameter(description = "Return only overdue reviews", example = "false")
            @RequestParam(required = false) Boolean overdueOnly,
            @Parameter(description = "Overdue threshold in hours (e.g. 24)", example = "24")
            @RequestParam(required = false) Integer overdueHours,
            @Parameter(description = "Filter by client id", example = "123")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (max 200)", example = "50")
            @RequestParam(required = false) Integer pageSize,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<DocumentSummaryResponse> documents = documentService.getReviewQueue(
                status, overdueOnly, overdueHours, clientId, page, pageSize, principal);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/reviews/summary")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get review summary counts",
            description = """
                    Get summary counts for documents awaiting review across accessible clients.
                    
                    **Query Parameters:**
                    - `overdueHours` (OPTIONAL): Overdue threshold in hours (default uses reviewDueAt)
                    - `clientId` (OPTIONAL): filter by client
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<DocumentReviewSummaryResponse> getReviewSummary(
            @Parameter(description = "Overdue threshold in hours (e.g. 24)", example = "24")
            @RequestParam(required = false) Integer overdueHours,
            @Parameter(description = "Filter by client id", example = "123")
            @RequestParam(required = false) Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        DocumentReviewSummaryResponse summary = documentService.getReviewSummary(overdueHours, clientId, principal);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/reviews/dashboard")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get review dashboard (summary + list)",
            description = """
                    Get review summary counts and a filtered list in a single request.
                    
                    **Query Parameters:**
                    - `status` (OPTIONAL): PENDING, THERAPIST_REVIEW, SUPERVISOR_REVIEW, or OVERDUE
                    - `overdueOnly` (OPTIONAL): true/false
                    - `overdueHours` (OPTIONAL): Overdue threshold in hours (default uses reviewDueAt)
                    - `clientId` (OPTIONAL): filter by client
                    - `page` (OPTIONAL): 0-based page index
                    - `pageSize` (OPTIONAL): page size (max 200)
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<DocumentReviewDashboardResponse> getReviewDashboard(
            @Parameter(description = "Review status filter", example = "PENDING")
            @RequestParam(required = false) ReviewStatus status,
            @Parameter(description = "Return only overdue reviews", example = "false")
            @RequestParam(required = false) Boolean overdueOnly,
            @Parameter(description = "Overdue threshold in hours (e.g. 24)", example = "24")
            @RequestParam(required = false) Integer overdueHours,
            @Parameter(description = "Filter by client id", example = "123")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (max 200)", example = "50")
            @RequestParam(required = false) Integer pageSize,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        DocumentReviewSummaryResponse summary = documentService.getReviewSummary(overdueHours, clientId, principal);
        List<DocumentSummaryResponse> items = documentService.getReviewQueue(
                status, overdueOnly, overdueHours, clientId, page, pageSize, principal);

        return ResponseEntity.ok(DocumentReviewDashboardResponse.builder()
                .summary(summary)
                .items(items)
                .build());
    }
}

