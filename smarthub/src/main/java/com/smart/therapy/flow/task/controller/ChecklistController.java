package com.smart.therapy.flow.task.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.task.dto.*;
import com.smart.therapy.flow.task.service.ChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smart.therapy.flow.common.security.PermissionConstants;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Checklist Management", description = "APIs for managing checklist templates and client checklists")
public class ChecklistController {

    private final ChecklistService checklistService;


    // ========== TEMPLATE ENDPOINTS ==========

    @GetMapping("/checklist-templates")
    @PreAuthorize("hasAnyAuthority('CONSENT_ADMIN_VIEW', 'CLIENT_VIEW_OWN', 'CLIENT_VIEW_TEAM', 'CLIENT_VIEW_ALL')")
    public ResponseEntity<List<ChecklistTemplateResponse>> getTemplates(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType
    ) {
        String effectiveCategory = category != null ? category : categoryType;
        List<ChecklistTemplateResponse> templates = checklistService.getTemplates(page, pageSize, search, effectiveCategory);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/checklist-templates/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<ChecklistTemplateResponse> getTemplate(@PathVariable("id") Long id) {
        ChecklistTemplateResponse template = checklistService.getTemplate(id);
        return ResponseEntity.ok(template);
    }

    @PostMapping("/checklist-templates")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Create checklist template",
            description = """
                    Create a new checklist template.
                    
                    **Request Body:**
                    - See CreateChecklistTemplateRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ChecklistTemplateResponse> createTemplate(
            @Valid @RequestBody CreateChecklistTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ChecklistTemplateResponse template = checklistService.createTemplate(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(template);
    }

    @PatchMapping("/checklist-templates/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Update checklist template",
            description = """
                    Update an existing checklist template.
                    
                    **Path Parameters:**
                    - `id` (REQUIRED): Template ID
                    
                    **Request Body:**
                    - See CreateChecklistTemplateRequest DTO (all fields optional for update)
                    - If `items` is provided, existing items will be replaced
                    
                    **Requires:** ADMIN role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Template updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ChecklistTemplateResponse> updateTemplate(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateChecklistTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ChecklistTemplateResponse template = checklistService.updateTemplate(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/checklist-templates/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Delete checklist template",
            description = """
                    Soft delete a checklist template.
                    
                    **Path Parameters:**
                    - `id` (REQUIRED): Template ID
                    
                    **Validation:**
                    - Cannot delete templates with active client assignments
                    
                    **Requires:** ADMIN role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Template deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot delete (has active assignments)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        checklistService.deleteTemplate(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/checklist-templates/{templateId}/items")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get checklist template items",
            description = "Retrieve all items for a checklist template.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Checklist items retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChecklistItemResponse.class),
                    examples = @ExampleObject(value = """
                            [
                              {
                                "id": 10,
                                "templateId": 1,
                                "title": "Consent Form Signed",
                                "description": "Obtain and upload signed consent form",
                                "category": "INTAKE",
                                "isRequired": true,
                                "itemOrder": 1,
                                "daysFromStart": 0,
                                "sortOrder": 0
                              }
                            ]
                            """)
            )
    )
    public ResponseEntity<List<ChecklistItemResponse>> getTemplateItems(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId
    ) {
        List<ChecklistItemResponse> items = checklistService.getTemplateItems(templateId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/checklist-templates/{templateId}/items/{itemId}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get checklist template item by ID",
            description = "Retrieve a single checklist item under a template.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Checklist item retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChecklistItemResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 10,
                              "templateId": 1,
                              "title": "Consent Form Signed",
                              "description": "Obtain and upload signed consent form",
                              "category": "INTAKE",
                              "isRequired": true,
                              "itemOrder": 1,
                              "daysFromStart": 0,
                              "sortOrder": 0
                            }
                            """)
            )
    )
    public ResponseEntity<ChecklistItemResponse> getTemplateItem(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @Parameter(description = "Item ID", required = true, example = "10")
            @PathVariable("itemId") Long itemId
    ) {
        ChecklistItemResponse item = checklistService.getTemplateItem(templateId, itemId);
        return ResponseEntity.ok(item);
    }

    @PostMapping("/checklist-templates/{templateId}/items")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Create checklist item",
            description = "Create a single checklist item under a checklist template.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateChecklistItemRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "Consent Form Signed",
                              "description": "Obtain and upload signed consent form",
                              "category": "intake",
                              "isRequired": true,
                              "itemOrder": 1,
                              "daysFromStart": 0,
                              "sortOrder": 0
                            }
                            """)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Checklist item created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChecklistItemResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 10,
                              "templateId": 1,
                              "title": "Consent Form Signed",
                              "description": "Obtain and upload signed consent form",
                              "category": "INTAKE",
                              "isRequired": true,
                              "itemOrder": 1,
                              "daysFromStart": 0,
                              "sortOrder": 0
                            }
                            """)
            )
    )
    public ResponseEntity<ChecklistItemResponse> addTemplateItem(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @Valid @RequestBody CreateChecklistItemRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ChecklistItemResponse item = checklistService.addItemToTemplate(
                templateId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(item);
    }

    @PatchMapping("/checklist-templates/{templateId}/items/{itemId}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Update checklist item",
            description = "Update a single checklist item under a checklist template.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateChecklistTemplateItemRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "Consent Form (Updated)",
                              "description": "Signed consent form must be uploaded before first session",
                              "category": "intake",
                              "isRequired": true,
                              "itemOrder": 1,
                              "daysFromStart": 0,
                              "sortOrder": 0
                            }
                            """)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Checklist item updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChecklistItemResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 10,
                              "templateId": 1,
                              "title": "Consent Form (Updated)",
                              "description": "Signed consent form must be uploaded before first session",
                              "category": "INTAKE",
                              "isRequired": true,
                              "itemOrder": 1,
                              "daysFromStart": 0,
                              "sortOrder": 0
                            }
                            """)
            )
    )
    public ResponseEntity<ChecklistItemResponse> updateTemplateItem(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @Parameter(description = "Item ID", required = true, example = "10")
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody UpdateChecklistTemplateItemRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ChecklistItemResponse item = checklistService.updateTemplateItem(
                templateId, itemId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/checklist-templates/{templateId}/items/{itemId}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @Operation(
            summary = "Delete checklist item",
            description = "Soft delete a single checklist item under a checklist template.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Checklist item deleted")
    public ResponseEntity<Void> deleteTemplateItem(
            @Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @Parameter(description = "Item ID", required = true, example = "10")
            @PathVariable("itemId") Long itemId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        checklistService.deleteTemplateItem(templateId, itemId, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    // ========== CLIENT CHECKLIST ENDPOINTS ==========

    @GetMapping("/clients/{clientId}/checklists")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get client checklists",
            description = "Retrieve all checklists for a specific client. Supports optional query parameters for filtering.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<ClientChecklistResponse>> getClientChecklists(
            @Parameter(description = "Client ID", required = true) @PathVariable("clientId") Long clientId,
            @Parameter(description = "Optional: Filter by template ID") @RequestParam(required = false) Long templateId,
            @Parameter(description = "Optional: Filter by category (INTAKE, ASSESSMENT, ONGOING, DISCHARGE)") @RequestParam(required = false) String category,
            @Parameter(description = "Optional: Filter by completion status") @RequestParam(required = false) Boolean isCompleted
    ) {
        // If any filter parameters provided, use filtered method
        if (templateId != null || category != null || isCompleted != null) {
            ClientChecklistFilterRequest filter = new ClientChecklistFilterRequest();
            filter.setClientId(clientId);
            filter.setTemplateId(templateId);
            filter.setCategory(category);
            filter.setIsCompleted(isCompleted);
            List<ClientChecklistResponse> checklists = checklistService.getClientChecklistsWithFilters(filter);
            return ResponseEntity.ok(checklists);
        }
        
        // Otherwise use simple method
        List<ClientChecklistResponse> checklists = checklistService.getClientChecklists(clientId);
        return ResponseEntity.ok(checklists);
    }

    @PostMapping("/checklists/filter")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Get checklists with advanced filters",
            description = """
                    Retrieve client checklists with advanced filtering options.
                    
                    **Filter Options:**
                    - `clientId` - Filter by client
                    - `templateId` - Filter by template
                    - `category` - Filter by checklist category (INTAKE, ASSESSMENT, ONGOING, DISCHARGE)
                    - `isCompleted` - Filter by completion status
                    - `completedDateFrom` - Filter by completion date from
                    - `completedDateTo` - Filter by completion date to
                    - `dueDateFrom` - Filter by due date from
                    - `dueDateTo` - Filter by due date to
                    - `createdDateFrom` - Filter by creation date from
                    - `createdDateTo` - Filter by creation date to
                    
                    All filters are optional. Multiple filters can be combined.
                    
                    Requires ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of filtered checklists retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<ClientChecklistResponse>> getChecklistsWithFilters(
            @Valid @RequestBody ClientChecklistFilterRequest filter) {
        List<ClientChecklistResponse> checklists = checklistService.getClientChecklistsWithFilters(filter);
        return ResponseEntity.ok(checklists);
    }

    @GetMapping("/client-checklists/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<ClientChecklistResponse> getClientChecklist(@PathVariable("id") Long id) {
        ClientChecklistResponse checklist = checklistService.getClientChecklist(id);
        return ResponseEntity.ok(checklist);
    }

    @PostMapping("/clients/{clientId}/checklists")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Assign checklist to client",
            description = """
                    Assign a checklist template to a client.
                    
                    **Path Parameters:**
                    - `clientId` (REQUIRED): ID of the client
                    
                    **Request Body:**
                    - See AssignChecklistRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientChecklistResponse> assignChecklist(
            @Parameter(description = "Client ID (REQUIRED)", required = true, example = "123")
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody AssignChecklistRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ClientChecklistResponse checklist = checklistService.assignChecklistToClient(clientId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(checklist);
    }

    @PostMapping("/bulk-assign")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @Operation(
            summary = "Bulk assign checklist",
            description = "Assign a checklist template to multiple clients at once.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<ClientChecklistResponse>> bulkAssignChecklist(
            @Valid @RequestBody BulkAssignChecklistRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        List<ClientChecklistResponse> responses = checklistService.bulkAssignChecklist(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(responses);
    }

    // ========== CLIENT CHECKLIST ITEM ENDPOINTS ==========

    @GetMapping("/client-checklist-items/{clientChecklistId}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<List<ClientChecklistItemResponse>> getClientChecklistItems(@PathVariable("clientChecklistId") Long clientChecklistId) {
        List<ClientChecklistItemResponse> items = checklistService.getClientChecklistItems(clientChecklistId);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/client-checklist-items/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    public ResponseEntity<ClientChecklistItemResponse> updateClientChecklistItem(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ClientChecklistItemResponse item = checklistService.updateClientChecklistItem(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(item);
    }

    // ========== REPORTING ENDPOINTS ==========

    @GetMapping("/compliance-report/{templateId}")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
    @Operation(
            summary = "Get compliance report",
            description = "Get a compliance report for a checklist template showing completion status, overdue items, and client statuses.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ComplianceReportResponse> getComplianceReport(
            @Parameter(description = "Template ID", required = true) @PathVariable("templateId") Long templateId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ComplianceReportResponse report = checklistService.getComplianceReport(templateId, principal);
        return ResponseEntity.ok(report);
    }
}


