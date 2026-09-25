package com.smart.therapy.flow.document.controller;

import com.smart.therapy.flow.common.config.AppProperties;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.dto.SuccessResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import com.smart.therapy.flow.document.dto.DocumentSummaryResponse;
import com.smart.therapy.flow.document.dto.ReviewDocumentRequest;
import com.smart.therapy.flow.document.dto.ShareDocumentRequest;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Management", description = "APIs for managing client documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AppProperties appProperties;

    @GetMapping
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get all documents for a client",
            description = """
                    Retrieve all documents associated with a client.
                    
                    **Path Parameters:**
                    - `clientId` (REQUIRED): ID of the client
                    
                    **Query Parameters:**
                    - `page` (OPTIONAL, default: 1): Page number (1-based)
                    - `pageSize` (OPTIONAL, default: 25, max: 200): Items per page
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of documents retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = DocumentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PaginatedResponse<DocumentSummaryResponse>> getClientDocuments(
            @Parameter(description = "Client ID (REQUIRED)", required = true, example = "123")
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Page number (optional, default: 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page (optional, default: 25, max: 200)", example = "20")
            @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Filter by document type", example = "CONSENT")
            @RequestParam(required = false) String documentType,
            @Parameter(description = "Filter by category", example = "FORMS")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by review status", example = "PENDING")
            @RequestParam(required = false) ReviewStatus reviewStatus,
            @Parameter(description = "Filter by shared with client flag", example = "true")
            @RequestParam(required = false) Boolean shareWithClient,
            @Parameter(description = "Search by file name or description", example = "consent")
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int defaultPage = appProperties.getPagination().getDefaultPage();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        int maxPageSize = appProperties.getPagination().getMaxPageSize();
        int minPageSize = appProperties.getPagination().getMinPageSize();

        int safePage = Math.max(defaultPage, page);
        if (pageSize == null) {
            pageSize = defaultPageSize;
        }
        int safePageSize = Math.min(Math.max(pageSize, minPageSize), maxPageSize);

        PaginatedResponse<DocumentSummaryResponse> documents = documentService.getClientDocuments(
                clientId,
                documentType,
                category,
                reviewStatus,
                shareWithClient,
                search,
                safePage,
                safePageSize,
                principal
        );
        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Upload a document",
            description = """
                    Upload a document file for a client. Maximum file size is 50MB. Use multipart/form-data content type.
                    
                    **Path Parameters:**
                    - `clientId` (REQUIRED): ID of the client
                    
                    **Form Data Parameters:**
                    - `file` (REQUIRED): The file to upload (multipart/form-data)
                    - `documentType` (OPTIONAL): Document type (e.g., intake_form, consent, insurance_card, id_document, medical_record, prescription, lab_result, referral_letter)
                    - `category` (OPTIONAL): Document category (e.g., insurance, forms, id, medical, prescription, lab, referral)
                    - `description` (OPTIONAL): Document description
                    - `needsReview` (OPTIONAL, default: false): Whether the document needs review
                    - `shareWithClient` (OPTIONAL, default: false): Whether to share the document with the client in the portal
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Document uploaded successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = DocumentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or file too large"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Parameter(description = "Client ID (REQUIRED)", required = true, example = "123")
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "The file to upload (REQUIRED, multipart/form-data)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Document type (OPTIONAL)", example = "consent")
            @RequestParam(required = false) String documentType,
            @Parameter(description = "Document category (OPTIONAL)", example = "forms")
            @RequestParam(required = false) String category,
            @Parameter(description = "Document description (OPTIONAL)", example = "Signed consent form")
            @RequestParam(required = false) String description,
            @Parameter(description = "Whether document needs review (OPTIONAL, default: false)", example = "false")
            @RequestParam(required = false) Boolean needsReview,
            @Parameter(description = "Whether to share with client (OPTIONAL, default: false)", example = "true")
            @RequestParam(required = false) Boolean shareWithClient,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse document = documentService.uploadDocument(
                clientId, file, documentType, category, description, needsReview, shareWithClient,
                principal, HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.status(201).body(withDocumentUrls(document));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get document by ID",
            description = """
                    Retrieve a specific document by its ID.
                    
                    **Path Parameters:**
                    - `clientId` (REQUIRED): ID of the client
                    - `id` (REQUIRED): ID of the document
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Document retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = DocumentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Client ID (REQUIRED)", required = true, example = "123")
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Document ID (REQUIRED)", required = true, example = "456")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        DocumentResponse document = documentService.getDocument(id, principal);
        return ResponseEntity.ok(withDocumentUrls(document));
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<InputStreamResource> getDocumentFile(
            @PathVariable("clientId") Long clientId,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        try {
            InputStream fileStream = documentService.downloadDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest));
            
            // Get document metadata for content type and filename
            DocumentResponse document = documentService.getDocument(id, principal);
            
            return buildStreamingResponse(fileStream,
                    MediaType.parseMediaType(document.getMimeType()),
                    "attachment; filename=\"" + document.getOriginalName() + "\"");
        } catch (com.smart.therapy.flow.common.exception.BadRequestException |
                 com.smart.therapy.flow.common.exception.ResourceNotFoundException |
                 com.smart.therapy.flow.common.exception.ForbiddenException e) {
            // Re-throw so global exception handler can map to proper 400/403/404 response
            throw e;
        } catch (Exception e) {
            log.error("Error downloading document", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to download document: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<SuccessResponse> deleteDocument(
            @PathVariable("clientId") Long clientId,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        documentService.deleteDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(SuccessResponse.of("Document deleted successfully"));
    }

    @PatchMapping("/{id}/share")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Update document sharing settings",
            description = """
                    Update whether a document is shared with the client in the portal.
                    
                    **Path Parameters:**
                    - `clientId` (REQUIRED): ID of the client
                    - `id` (REQUIRED): ID of the document
                    
                    **Request Body:**
                    - `shareWithClient` (REQUIRED): Whether to share document with client (true/false)
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Document sharing settings",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ShareDocumentRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Share Document",
                                    value = """
                                            {
                                              "shareWithClient": true
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Document sharing updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<SuccessResponse> shareDocument(
            @Parameter(description = "Client ID (REQUIRED)", required = true, example = "123")
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Document ID (REQUIRED)", required = true, example = "456")
            @PathVariable("id") Long id,
            @Valid @RequestBody ShareDocumentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        documentService.shareDocument(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(SuccessResponse.of("Document sharing updated successfully"));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<DocumentResponse> previewDocument(
            @PathVariable("clientId") Long clientId,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            // Record audit event for preview
            documentService.viewDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest));
            // Return document metadata for preview
            DocumentResponse document = documentService.getDocument(id, principal);
            String mime = document.getMimeType() != null ? document.getMimeType().toLowerCase(java.util.Locale.ROOT) : "";
            // Preview hardening: validate storage readability before returning metadata.
            try (InputStream ignored = mime.contains("pdf")
                    ? documentService.viewPdfDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest))
                    : documentService.downloadDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest))) {
                // no-op
            }
            return ResponseEntity.ok(withDocumentUrls(document));
        } catch (com.smart.therapy.flow.common.exception.BadRequestException |
                 com.smart.therapy.flow.common.exception.ResourceNotFoundException |
                 com.smart.therapy.flow.common.exception.ForbiddenException e) {
            // Re-throw so global exception handler can map to proper 400/403/404 response
            throw e;
        } catch (Exception e) {
            log.error("Error previewing document", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to preview document: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Review a document",
            description = """
                    Review a document or move it between review stages.
                    
                    **Path Parameters:**
                    - `clientId` (REQUIRED): ID of the client
                    - `id` (REQUIRED): ID of the document
                    
                    **Request Body:**
                    - `reviewStatus` (REQUIRED): PENDING, THERAPIST_REVIEW, SUPERVISOR_REVIEW, APPROVED, or REJECTED
                    - `reviewNotes` (OPTIONAL): Notes about the review
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Document review information",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReviewDocumentRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Review Document",
                                    value = """
                                            {
                                              "reviewStatus": "approved",
                                              "reviewNotes": "Document verified and approved"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Document reviewed successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = DocumentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<DocumentResponse> reviewDocument(
            @Parameter(description = "Client ID (REQUIRED)", required = true, example = "123")
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Document ID (REQUIRED)", required = true, example = "456")
            @PathVariable("id") Long id,
            @Valid @RequestBody com.smart.therapy.flow.document.dto.ReviewDocumentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse document = documentService.reviewDocument(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(withDocumentUrls(document));
    }

    @GetMapping("/{id}/viewer")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<ByteArrayResource> viewDocumentInViewer(
            @PathVariable("clientId") Long clientId,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse document = documentService.getDocument(id, principal);

        try {
            InputStream fileStream = documentService.viewPdfDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest));
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (document.getMimeType() != null && !document.getMimeType().isBlank()) {
                mediaType = MediaType.parseMediaType(document.getMimeType());
            }
            String dispositionType = isInlineViewable(mediaType) ? "inline" : "attachment";
            byte[] content = readFully(fileStream);
            validatePdfPayloadIfNeeded(content, mediaType, document.getOriginalName());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + document.getOriginalName() + "\"")
                    .contentType(mediaType)
                    .contentLength(content.length)
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .header("Content-Security-Policy", "frame-ancestors 'self'")
                    .body(new ByteArrayResource(content));
        } catch (com.smart.therapy.flow.common.exception.BadRequestException | 
                 com.smart.therapy.flow.common.exception.ResourceNotFoundException e) {
            // Re-throw so global exception handler can return proper status code
            throw e;
        } catch (Exception e) {
            log.error("Error viewing document", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to view document: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/docx-viewer")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<Object> viewDocxDocument(
            @PathVariable("clientId") Long clientId,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            DocumentResponse document = documentService.getDocument(id, principal);
            
            String mimeType = document.getMimeType();
            String fileName = document.getFileName() != null ? document.getFileName().toLowerCase() : "";
            boolean isDocx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType) ||
                            fileName.endsWith(".docx") || fileName.endsWith(".doc");
            
            if (!isDocx) {
                throw new com.smart.therapy.flow.common.exception.BadRequestException("This endpoint only serves Word documents");
            }
            
            // TODO: Convert DOCX to HTML using a library like Apache POI or docx4j
            // For now, return a placeholder response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("html", "<p>DOCX to HTML conversion not yet implemented. Please download the file to view.</p>");
            response.put("messages", java.util.List.of("DOCX viewer requires additional library integration"));
            
            return ResponseEntity.ok(response);
        } catch (com.smart.therapy.flow.common.exception.BadRequestException |
                 com.smart.therapy.flow.common.exception.ResourceNotFoundException |
                 com.smart.therapy.flow.common.exception.ForbiddenException e) {
            // Re-throw so global exception handler can return proper status code (400/403/404)
            throw e;
        } catch (Exception e) {
            log.error("Error viewing DOCX document", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to view DOCX document: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable("clientId") Long clientId,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        try {
            InputStream fileStream = documentService.downloadDocument(id, principal, HttpRequestUtil.getClientIp(httpRequest));
            
            DocumentResponse document = documentService.getDocument(id, principal);
            
            return buildStreamingResponse(fileStream,
                    MediaType.parseMediaType(document.getMimeType()),
                    "attachment; filename=\"" + document.getOriginalName() + "\"");
        } catch (com.smart.therapy.flow.common.exception.BadRequestException |
                 com.smart.therapy.flow.common.exception.ResourceNotFoundException |
                 com.smart.therapy.flow.common.exception.ForbiddenException e) {
            // Re-throw so global exception handler can map to proper 400/403/404 response
            throw e;
        } catch (Exception e) {
            log.error("Error downloading document", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to download document: " + e.getMessage());
        }
    }

    private PaginatedResponse<DocumentResponse> withDocumentUrls(PaginatedResponse<DocumentResponse> response) {
        if (response == null || response.getItems() == null) {
            return response;
        }
        List<DocumentResponse> items = response.getItems().stream()
                .map(this::withDocumentUrls)
                .collect(Collectors.toList());
        return PaginatedResponse.of(items, response.getTotalCount(), response.getPage(), response.getPageSize());
    }

    private DocumentResponse withDocumentUrls(DocumentResponse document) {
        if (document == null || document.getClientId() == null || document.getId() == null) {
            return document;
        }

        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String pathBase = base + "/api/v1/clients/" + document.getClientId() + "/documents/" + document.getId();
        document.setPreviewUrl(pathBase + "/viewer");
        document.setDownloadUrl(pathBase + "/download");
        return document;
    }

    private ResponseEntity<InputStreamResource> buildStreamingResponse(
            InputStream fileStream,
            MediaType mediaType,
            String contentDisposition) {
        // ResourceHttpMessageConverter copies and closes the stream in this authenticated
        // request. Async redispatch would lose our stateless JWT security context.
        InputStreamResource body = new InputStreamResource(fileStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(mediaType)
                .body(body);
    }

    private boolean isInlineViewable(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        if ("image".equalsIgnoreCase(mediaType.getType())) {
            return true;
        }

        String mime = mediaType.toString().toLowerCase(Locale.ROOT);
        return "application/pdf".equals(mime)
                || "text/plain".equals(mime)
                || "text/html".equals(mime)
                || "text/csv".equals(mime)
                || "application/json".equals(mime)
                || "application/xml".equals(mime)
                || "text/xml".equals(mime)
                || "application/xhtml+xml".equals(mime)
                || mime.endsWith("+xml");
    }

    private byte[] readFully(InputStream fileStream) throws Exception {
        try (InputStream in = fileStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    private void validatePdfPayloadIfNeeded(byte[] content, MediaType mediaType, String originalName) {
        String mime = mediaType != null ? mediaType.toString().toLowerCase(Locale.ROOT) : "";
        boolean shouldBePdf = "application/pdf".equals(mime)
                || (originalName != null && originalName.toLowerCase(Locale.ROOT).endsWith(".pdf"));
        if (!shouldBePdf) {
            return;
        }
        if (content == null || content.length < 5) {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("Invalid or empty PDF content");
        }
        if (content[0] != '%' || content[1] != 'P' || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
            throw new com.smart.therapy.flow.common.exception.BadRequestException("Document content is not a valid PDF");
        }
    }

}

