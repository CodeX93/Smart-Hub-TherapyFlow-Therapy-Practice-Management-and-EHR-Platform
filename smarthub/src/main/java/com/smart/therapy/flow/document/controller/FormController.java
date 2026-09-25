package com.smart.therapy.flow.document.controller;

import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.service.FormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
@Slf4j
public class FormController {

    private final FormService formService;


    // ========== TEMPLATE ENDPOINTS ==========

    @GetMapping("/templates")
    @PreAuthorize(StaffAuthorizationExpressions.FORM_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get all form templates",
            description = "Retrieve all active form templates. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of form templates retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormTemplateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<FormTemplateResponse>> getTemplates(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType
    ) {
        String effectiveCategory = category != null ? category : categoryType;
        List<FormTemplateResponse> templates = formService.getTemplates(page, pageSize, search, effectiveCategory);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.FORM_READ_ACCESS)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form template by ID",
            description = "Retrieve a specific form template with all its fields. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Form template retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormTemplateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<FormTemplateResponse> getTemplate(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        FormTemplateResponse template = formService.getTemplate(id);
        return ResponseEntity.ok(template);
    }

    @PostMapping("/templates")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create form template",
            description = """
                    Create a new form template.
                    
                    **Required Fields:**
                    - `name` (REQUIRED): Form template name
                    - `category` (REQUIRED): Form category (consent, intake, release, agreement, safety, discharge, custom)
                    - `requiresSignature` (REQUIRED): Whether the form requires a signature
                    
                    **Optional Fields:**
                    - `description` (optional): Form template description
                    - `instructions` (optional): Instructions for filling out the form
                    - `isActive` (optional, default: true): Whether the template is active
                    - `isSystemTemplate` (optional, default: false): Whether this is a system template
                    - `sortOrder` (optional): Sort order for display
                    - `fields` (optional): List of form fields
                    
                    **Requires:** ADMIN role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Form template information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateFormTemplateRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Form Template",
                                    value = """
                                            {
                                              "name": "Intake Form",
                                              "description": "Initial intake form for new clients",
                                              "category": "intake",
                                              "instructions": "Please fill out all required fields",
                                              "requiresSignature": true,
                                              "isActive": true,
                                              "isSystemTemplate": false,
                                              "sortOrder": 1,
                                              "fields": [
                                                {
                                                  "fieldType": "text",
                                                  "label": "Full Name",
                                                  "isRequired": true,
                                                  "placeholder": "Enter your full name",
                                                  "sortOrder": 1
                                                },
                                                {
                                                  "fieldType": "date",
                                                  "label": "Date of Birth",
                                                  "isRequired": true,
                                                  "sortOrder": 2
                                                },
                                                {
                                                  "fieldType": "email",
                                                  "label": "Email Address",
                                                  "isRequired": true,
                                                  "placeholder": "example@email.com",
                                                  "sortOrder": 3
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Form template created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormTemplateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormTemplateResponse> createTemplate(
            @Valid @RequestBody CreateFormTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormTemplateResponse template = formService.createTemplate(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(template);
    }

    @PatchMapping("/templates/{id}")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update form template",
            description = "Update an existing form template. Only non-deleted templates can be updated. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Form template updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormTemplateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormTemplateResponse> updateTemplate(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @RequestBody UpdateFormTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormTemplateResponse template = formService.updateTemplate(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete form template",
            description = "Soft delete a form template. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Template deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> deleteTemplate(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        formService.deleteTemplate(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates/{id}/fields")
    @PreAuthorize(PermissionConstants.FORM_VIEW)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get fields for a form template",
            description = "Retrieve all fields for the active version of a form template."
    )
    public ResponseEntity<List<FormFieldResponse>> getTemplateFields(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long templateId
    ) {
        List<FormFieldResponse> fields = formService.getTemplateFields(templateId);
        return ResponseEntity.ok(fields);
    }

    // ========== VERSION MANAGEMENT ENDPOINTS ==========

    @GetMapping("/templates/{templateId}/versions")
    @PreAuthorize(PermissionConstants.FORM_VIEW)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get all versions of a template",
            description = "Retrieve all versions of a form template. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<FormTemplateVersionResponse>> getTemplateVersions(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId) {
        List<FormTemplateVersionResponse> versions = formService.getTemplateVersions(templateId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/templates/{templateId}/versions/{versionNumber}")
    @PreAuthorize(PermissionConstants.FORM_VIEW)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get specific template version",
            description = "Retrieve a specific version of a form template with all sections and fields.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<FormTemplateVersionResponse> getTemplateVersion(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Version number", required = true, example = "1")
            @PathVariable("versionNumber") Long versionNumber) {
        FormTemplateVersionResponse version = formService.getTemplateVersion(templateId, versionNumber);
        return ResponseEntity.ok(version);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNumber}/activate")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Activate template version",
            description = "Activate a specific version of a template. Previous active version will be archived. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<FormTemplateVersionResponse> activateVersion(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Version number", required = true, example = "2")
            @PathVariable("versionNumber") Long versionNumber,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        FormTemplateVersionResponse version = formService.activateVersion(
                templateId, versionNumber, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(version);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNumber}/archive")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Archive template version",
            description = "Archive a specific version of a template. Active version cannot be archived. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<FormTemplateVersionResponse> archiveVersion(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("templateId") Long templateId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Version number", required = true, example = "1")
            @PathVariable("versionNumber") Long versionNumber,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        FormTemplateVersionResponse version = formService.archiveVersion(
                templateId, versionNumber, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(version);
    }

    // ========== SECTION MANAGEMENT ENDPOINTS ==========

    @PostMapping("/template-versions/{templateVersionId}/sections")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create form section",
            description = "Create a new section within a template version. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<FormSectionResponse> createSection(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template version ID", required = true, example = "1")
            @PathVariable("templateVersionId") Long templateVersionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Section data")
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        String name = request.get("name") != null ? request.get("name").toString() : null;
        String description = request.get("description") != null ? request.get("description").toString() : null;
        Integer sortOrder = request.get("sortOrder") != null ? 
                Integer.valueOf(request.get("sortOrder").toString()) : null;
        
        FormSectionResponse section = formService.createSection(
                templateVersionId, name, description, sortOrder, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(section);
    }

    @PatchMapping("/sections/{sectionId}")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update form section",
            description = "Update an existing section. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<FormSectionResponse> updateSection(
            @io.swagger.v3.oas.annotations.Parameter(description = "Section ID", required = true, example = "1")
            @PathVariable("sectionId") Long sectionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Section data to update")
            @RequestBody UpdateFormSectionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        FormSectionResponse section = formService.updateSection(
                sectionId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(section);
    }

    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete form section",
            description = "Delete a section. Sections with fields cannot be deleted. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteSection(
            @io.swagger.v3.oas.annotations.Parameter(description = "Section ID", required = true, example = "1")
            @PathVariable("sectionId") Long sectionId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        formService.deleteSection(sectionId, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    // ========== FIELD ENDPOINTS ==========

    @PostMapping("/fields")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create form field",
            description = "Create a new form field for an existing template. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Form field created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormFieldResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormFieldResponse> createField(
            @Valid @RequestBody CreateFormFieldRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormFieldResponse field = formService.createField(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(field);
    }

    @PostMapping("/templates/{id}/fields")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create form field for template",
            description = "Create a new form field attached to the active version of the specified template. Requires ADMIN role."
    )
    public ResponseEntity<FormFieldResponse> createFieldForTemplate(
            @io.swagger.v3.oas.annotations.Parameter(description = "Template ID", required = true, example = "1")
            @PathVariable("id") Long templateId,
            @Valid @RequestBody CreateFormFieldRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormFieldResponse field = formService.createFieldForTemplate(templateId, request, principal,
                HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(field);
    }

    @GetMapping("/fields/{id}")
    @PreAuthorize(PermissionConstants.FORM_VIEW)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form field by ID",
            description = "Retrieve a single non-deleted form field by id."
    )
    public ResponseEntity<FormFieldResponse> getField(
            @io.swagger.v3.oas.annotations.Parameter(description = "Field ID", required = true, example = "1")
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(formService.getField(id));
    }

    @PatchMapping("/fields/{id}")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update form field",
            description = "Update an existing form field. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Form field updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormFieldResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormFieldResponse> updateField(
            @io.swagger.v3.oas.annotations.Parameter(description = "Field ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @RequestBody UpdateFormFieldRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormFieldResponse field = formService.updateField(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(field);
    }

    @DeleteMapping("/fields/{id}")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete form field",
            description = "Delete a form field. Requires ADMIN role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Field deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> deleteField(
            @io.swagger.v3.oas.annotations.Parameter(description = "Field ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        formService.deleteField(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fields/reorder")
    @PreAuthorize(PermissionConstants.FORM_TEMPLATE_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Reorder form fields",
            description = "Reorder form fields by providing a list of field IDs in the desired order. Requires ADMIN role."
    )
    public ResponseEntity<Void> reorderFields(
            @Valid @RequestBody ReorderFormFieldsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        formService.reorderFields(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    // ========== ASSIGNMENT ENDPOINTS ==========

    @GetMapping("/assignments/client/{clientId}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form assignments for a client",
            description = "Retrieve all form assignments for a specific client. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of form assignments retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<FormAssignmentResponse>> getClientAssignments(
            @io.swagger.v3.oas.annotations.Parameter(description = "Client ID", required = true, example = "1")
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<FormAssignmentResponse> assignments = formService.getClientAssignments(clientId, principal);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "List form assignments",
            description = "List form assignments with optional filters for clientId and templateId."
    )
    public ResponseEntity<List<FormAssignmentResponse>> listAssignments(
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        FormAssignmentFilterRequest filter = new FormAssignmentFilterRequest();
        filter.setClientId(clientId);
        filter.setTemplateId(templateId);
        List<FormAssignmentResponse> assignments = formService.getAssignmentsWithFilters(filter, principal);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/clients/{clientId}/forms")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get client forms",
            description = "Alias endpoint to retrieve form assignments for a specific client."
    )
    public ResponseEntity<List<FormAssignmentResponse>> getClientForms(
            @io.swagger.v3.oas.annotations.Parameter(description = "Client ID", required = true, example = "1")
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<FormAssignmentResponse> assignments = formService.getClientAssignments(clientId, principal);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/assignments/filter")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form assignments with filters",
            description = """
                    Retrieve form assignments with advanced filtering options.
                    
                    **Filter Options:**
                    - `clientId` - Filter by client
                    - `templateId` - Filter by template
                    - `versionStatus` - Filter by template version status (DRAFT, ACTIVE, ARCHIVED)
                    - `assignmentStatus` - Filter by assignment completion status (ASSIGNED, IN_PROGRESS, SUBMITTED, COMPLETED, REVIEWED, CANCELLED)
                    - `assignmentDateFrom` - Filter assignments created from this date
                    - `assignmentDateTo` - Filter assignments created to this date
                    - `dueDateFrom` - Filter by due date from
                    - `dueDateTo` - Filter by due date to
                    
                    All filters are optional. Multiple filters can be combined.
                    
                    Requires ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of filtered form assignments retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<FormAssignmentResponse>> getAssignmentsWithFilters(
            @Valid @RequestBody FormAssignmentFilterRequest filter,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<FormAssignmentResponse> assignments = formService.getAssignmentsWithFilters(filter, principal);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form assignment by ID",
            description = "Retrieve a specific form assignment with responses and signatures. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Form assignment retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<FormAssignmentResponse> getAssignment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        FormAssignmentResponse assignment = formService.getAssignment(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(assignment);
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create form assignment",
            description = "Assign a form template to a client. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Form assignment created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template or client not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormAssignmentResponse> createAssignment(
            @Valid @RequestBody CreateFormAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormAssignmentResponse assignment = formService.createAssignment(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(assignment);
    }

    @PostMapping("/assignments/bulk")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Bulk assign forms",
            description = """
                    Assign a form template to multiple clients at once.
                    
                    **Request Body:**
                    - `templateId` (REQUIRED): Form template ID
                    - `clientIds` (REQUIRED): List of client IDs (at least one required)
                    - `dueDate` (optional): Due date for all assignments
                    - `instructions` (optional): Custom instructions for all assignments
                    
                    **Features:**
                    - Handles failures gracefully (continues with other clients)
                    - Skips deleted clients
                    - Returns list of successful assignments
                    
                    Requires ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Form assignments created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<FormAssignmentResponse>> bulkAssignForms(
            @Valid @RequestBody BulkAssignFormRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        List<FormAssignmentResponse> assignments = formService.bulkAssignForms(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(assignments);
    }

    @PostMapping("/assignments/multi-template")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Assign multiple templates to one client",
            description = """
                    Assign multiple form templates to a single client in one request.

                    **Request Body:**
                    - `clientId` (REQUIRED): Client ID
                    - `templateIds` (REQUIRED): List of template IDs (at least one required)
                    - `dueDate` (optional): Due date applied to all assignments
                    - `instructions` (optional): Instructions applied to all assignments

                    **Features:**
                    - Reuses single-assignment validation and snapshot logic
                    - Deduplicates template IDs in request order
                    - Continues when one template fails and returns successful assignments
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<FormAssignmentResponse>> assignMultipleTemplatesToClient(
            @Valid @RequestBody AssignMultipleFormsToClientRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        List<FormAssignmentResponse> assignments = formService.assignMultipleFormsToClient(
                request,
                principal,
                HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.status(201).body(assignments);
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete form assignment",
            description = "Delete a form assignment. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Assignment deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> deleteAssignment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        formService.deleteAssignment(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update form assignment",
            description = "Update basic properties of a form assignment such as due date, instructions, or status."
    )
    public ResponseEntity<FormAssignmentResponse> updateAssignment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateFormAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormAssignmentResponse assignment = formService.updateAssignment(id, request, principal,
                HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(assignment);
    }

    @PatchMapping("/assignments/{id}/status")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update form assignment status only",
            description = "Updates only the status of a form assignment."
    )
    public ResponseEntity<FormAssignmentResponse> updateAssignmentStatus(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateFormAssignmentStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormAssignmentResponse assignment = formService.updateAssignmentStatus(
                id,
                request.getStatus(),
                principal,
                HttpRequestUtil.getClientIp(httpRequest)
        );
        return ResponseEntity.ok(assignment);
    }

    // ========== RESPONSE ENDPOINTS ==========

    @PostMapping("/assignments/{id}/responses")
    @PreAuthorize("hasAnyAuthority('CONSENT_ADMIN_VIEW', 'CLIENT_PORTAL_ACCESS')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Submit form responses",
            description = "Submit responses for form fields in an assignment. Requires ADMIN, SUPERVISOR, THERAPIST, or CLIENT role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Form responses submitted successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormResponseDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions or access denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<FormResponseDto>> submitResponses(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long assignmentId,
            @Valid @RequestBody SubmitFormResponseRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        List<FormResponseDto> responses = formService.submitResponses(assignmentId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(responses);
    }

    @GetMapping("/assignments/{id}/responses")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form responses for an assignment",
            description = "Retrieve all form responses for a given assignment."
    )
    public ResponseEntity<List<FormResponseDto>> getAssignmentResponses(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long assignmentId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        List<FormResponseDto> responses = formService.getAssignmentResponses(assignmentId, principal,
                HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/responses/{id}")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update a single form response",
            description = "Update the value of a single form response."
    )
    public ResponseEntity<FormResponseDto> updateResponse(
            @io.swagger.v3.oas.annotations.Parameter(description = "Response ID", required = true, example = "1")
            @PathVariable("id") Long responseId,
            @Valid @RequestBody UpdateFormResponseRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormResponseDto response = formService.updateResponse(responseId, request, principal,
                HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    // ========== SIGNATURE ENDPOINTS ==========

    @PostMapping("/assignments/{id}/signature")
    @PreAuthorize("hasAnyAuthority('CONSENT_ADMIN_VIEW', 'CLIENT_PORTAL_ACCESS')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Submit form signature",
            description = "Submit a signature for a form assignment. Requires ADMIN, SUPERVISOR, THERAPIST, or CLIENT role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Form signature submitted successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormSignatureResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions or access denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormSignatureResponse> submitSignature(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long assignmentId,
            @Valid @RequestBody SubmitFormSignatureRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormSignatureResponse signature = formService.submitSignature(
                assignmentId, 
                request, 
                principal, 
                HttpRequestUtil.getClientIp(httpRequest), 
                HttpRequestUtil.getUserAgent(httpRequest)
        );
        return ResponseEntity.status(201).body(signature);
    }

    @PostMapping("/assignments/{id}/signatures")
    @PreAuthorize("hasAnyAuthority('CONSENT_ADMIN_VIEW', 'CLIENT_PORTAL_ACCESS')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Submit form signature (alias)",
            description = "Alias endpoint for submitting a signature for a form assignment."
    )
    public ResponseEntity<FormSignatureResponse> submitSignatureAlias(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long assignmentId,
            @Valid @RequestBody SubmitFormSignatureRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return submitSignature(assignmentId, request, principal, httpRequest);
    }

    @GetMapping("/assignments/{id}/signatures")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get form signatures for an assignment",
            description = "Retrieve all signatures associated with a form assignment."
    )
    public ResponseEntity<List<FormSignatureResponse>> getSignatures(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long assignmentId
    ) {
        List<FormSignatureResponse> signatures = formService.getSignatures(assignmentId);
        return ResponseEntity.ok(signatures);
    }

    // ========== REVIEW ENDPOINTS ==========

    @PatchMapping("/assignments/{id}/review")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Review form assignment",
            description = "Review and approve/reject a completed form assignment. Requires ADMIN, SUPERVISOR, or THERAPIST role.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Form assignment reviewed successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormAssignmentResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Assignment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<FormAssignmentResponse> reviewAssignment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Assignment ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody ReviewFormAssignmentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        FormAssignmentResponse assignment = formService.reviewAssignment(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(assignment);
    }
}


