package com.smart.therapy.flow.client.controller;

import com.smart.therapy.flow.client.dto.*;
import com.smart.therapy.flow.client.enums.*;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.client.service.ClientSmsLogService;
import com.smart.therapy.flow.client.service.DuplicateDetectionService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.session.dto.SessionTranscriptStatusResponse;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clients", description = "🟡 Therapist, Admin & Supervisor - Client management endpoints")
public class ClientController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.smart.therapy.flow.common.config.AppProperties appProperties;

    private final ClientService clientService;
    private final ClientSmsLogService clientSmsLogService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final SessionTranscriptService sessionTranscriptService;

    @GetMapping
    @PreAuthorize(
            PermissionConstants.CLIENT_VIEW_OWN + " or " +
            PermissionConstants.CLIENT_VIEW_TEAM + " or " +
            PermissionConstants.CLIENT_VIEW_ALL
    )
    @Operation(
            summary = "Get clients (paginated)",
            description = """
                    Get a paginated list of clients with filtering and sorting options.
                    
                    **Query Parameters:**
                    
                    **Optional (all have defaults):**
                    - `page`: Page number (default: 1)
                    - `pageSize`: Items per page (default: 25, max: 200)
                    - `sortBy`: Field to sort by (default: "createdAt")
                    - `sortOrder`: Sort direction - "asc" or "desc" (default: "desc")
                    
                    **Optional Filters:**
                    - `search`: Search by full name, first/last name token, email, phone, or MRN (exact match)
                    - `status`: Filter by status (active, inactive, pending, discharged)
                    - `stage`: Filter by stage (intake, active, maintenance, closed)
                    - `therapistId`: Filter by assigned therapist ID
                    - `clientType`: Filter by type (individual, group, family)
                    - `hasPortalAccess`: Filter by portal access (true/false)
                    - `hasPendingTasks`: Filter by pending tasks (true/false)
                    - `hasNoSessions`: Filter clients with no sessions (true/false)
                    - `needsFollowUp`: Filter clients needing follow-up (true/false)
                    - `unassigned`: Filter unassigned clients (true/false)
                    - `checklistTemplateId`: Filter clients with the given checklist template assigned
                    - `reportTemplateId`: Filter clients with a report generated from the given report template
                    
                    **Example Request:**
                    ```
                    GET /api/v1/clients?page=1&pageSize=25&status=active&therapistId=1&sortBy=fullName&sortOrder=asc
                    ```
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<ClientSummaryResponse>> getClients(
            @Parameter(description = "Page number (optional, default: 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page (optional, default: 25, max: 200)", example = "25")
            @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Search by full name, first/last name, email, phone, or MRN (exact match)", example = "John")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status (optional). Valid values: Active, Inactive, Pending, Discharged", example = "Active")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by stage (optional). Valid values: Intake, Assessment, Active Treatment, Maintenance, Discharge", example = "Intake")
            @RequestParam(required = false) String stage,
            @Parameter(description = "Filter by therapist ID (optional)", example = "1")
            @RequestParam(required = false) Long therapistId,
            @Parameter(description = "Filter by client type (optional). Valid values: Individual, Couple, Family, Group", example = "Individual")
            @RequestParam(required = false) String clientType,
            @Parameter(description = "Filter by portal access (optional)", example = "true")
            @RequestParam(required = false) Boolean hasPortalAccess,
            @Parameter(description = "Filter by pending tasks (optional)", example = "true")
            @RequestParam(required = false) Boolean hasPendingTasks,
            @Parameter(description = "Filter clients with no sessions (optional)", example = "true")
            @RequestParam(required = false) Boolean hasNoSessions,
            @Parameter(description = "Filter clients needing follow-up (optional)", example = "true")
            @RequestParam(required = false) Boolean needsFollowUp,
            @Parameter(description = "Filter unassigned clients (optional)", example = "true")
            @RequestParam(required = false) Boolean unassigned,
            @Parameter(
                    description = "When true, include unassigned clients together with therapistId / own caseload (optional)",
                    example = "true")
            @RequestParam(required = false) Boolean includeUnassigned,
            @Parameter(description = "Filter clients with this checklist template assigned (optional)", example = "3")
            @RequestParam(required = false) Long checklistTemplateId,
            @Parameter(description = "Filter clients with a report for this report template (optional)", example = "2")
            @RequestParam(required = false) Long reportTemplateId,
            @Parameter(description = "Field to sort by (optional, default: 'createdAt'). Valid values: createdAt, updatedAt, fullName, status, stage, clientId, dateOfBirth, lastSessionDate", example = "fullName")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort order - 'asc' or 'desc' (optional, default: 'desc')", example = "asc")
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        // Validate and sanitize pagination
        int safePage = Math.max(1, page);
        // int safePageSize = Math.min(Math.max(pageSize, 1), 200);
        if (pageSize == null) {
            pageSize = appProperties.getPagination().getDefaultPageSize();
        }
        int maxPageSize = appProperties.getPagination().getMaxPageSize();
        int minPageSize = appProperties.getPagination().getMinPageSize();
        int defaultPageSize = appProperties.getPagination().getDefaultPageSize();
        int safePageSize = Math.min(Math.max(pageSize, minPageSize), maxPageSize);
        
        // Convert string inputs to enums at controller boundary (CRITICAL: single conversion point)
        ClientFilter.ClientFilterBuilder filterBuilder = ClientFilter.builder()
                .search(StringUtils.hasText(search) ? search.trim() : null);
        
        if (StringUtils.hasText(status)) {
            filterBuilder.status(status.trim());
        }
        
        // Stage filter
        if (StringUtils.hasText(stage)) {
            filterBuilder.stage(stage.trim());
        }
        
        if (StringUtils.hasText(clientType)) {
            filterBuilder.clientType(clientType.trim());
        }
        
        filterBuilder
                .therapistId(therapistId)
                .hasPortalAccess(hasPortalAccess)
                .hasPendingTasks(hasPendingTasks)
                .hasNoSessions(hasNoSessions)
                .needsFollowUp(needsFollowUp)
                .unassigned(unassigned)
                .includeUnassigned(includeUnassigned)
                .checklistTemplateId(checklistTemplateId)
                .reportTemplateId(reportTemplateId);
        
        ClientFilter filter = filterBuilder.build();
        
        // Convert sortBy string to enum and build Pageable
        ClientSortField sortField;
        try {
            sortField = ClientSortField.fromValue(sortBy);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid sortBy value: " + sortBy + ". Valid values: " + 
                String.join(", ", java.util.Arrays.stream(ClientSortField.values())
                    .map(ClientSortField::getApiField)
                    .toList()));
        }
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortField.getEntityField());
        
        // If sorting by fullName, add secondary sort by id for consistent ordering
        if (sortField == ClientSortField.FULL_NAME) {
            sort = sort.and(Sort.by(direction, "id"));
        }
        
        Pageable pageable = PageRequest.of(safePage - 1, safePageSize, sort);
        
        // Call service with typed filter and pageable
        PaginatedResponse<ClientSummaryResponse> response = clientService.getClients(filter, pageable, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client statistics",
            description = "Get aggregated statistics about clients. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientStatsResponse> getClientStats(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(clientService.getClientStats(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client by ID",
            description = "Get detailed information about a specific client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientResponse> getClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request
    ) {
        ClientResponse client = clientService.getClient(id, principal);
        return ResponseEntity.ok(client);
    }

    @PostMapping
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_CREATE_ACCESS)
    @Operation(
            summary = "Create a new client",
            description = """
                    Create a new client record in the system.
                    
                    **Required Fields:**
                    - `fullName` (REQUIRED): Client's full name
                    - `status` (REQUIRED): Client status - active, inactive, pending, or discharged
                    
                    **Optional Fields:**
                    - Contact: `email`, `phone`, `streetAddress1`, `streetAddress2`, `city`, `province`, `postalCode`, `country`
                    - Demographics: `dateOfBirth`, `gender`, `maritalStatus`, `preferredLanguage`, `pronouns`
                    - Assignment: `assignedTherapistId`, `stage`, `clientType`
                    - Insurance: `insuranceProvider`, `policyNumber`, `groupNumber`, `insurancePhone`, `copayAmount`, `deductible`
                    - Emergency Contact: `emergencyContactName`, `emergencyContactPhone`, `emergencyContactRelationship`
                    - Referral: `referrerName`, `referralDate`, `referenceNumber`, `clientSource`
                    - Portal: `hasPortalAccess` (default: false), `portalEmail`, `emailNotifications` (default: true)
                    - Additional: `notes`, `serviceType`, `serviceFrequency`
                    - Compatibility aliases: `timezone`, `legacyAddress`/`addressLegacy`, `stateLegacy`, `zipCodeLegacy`,
                      `emergencyContactLegacy`, `startDate`, `legacyReferral`, `referringPersonName`, `referralType`,
                      `employmentStatus`, `educationLevel`, `numberOfDependents`/`dependents`, `priority`,
                      `dueDate`/`followUpDate`, `generalNotes`, `followUpNotes`, `referralNotes`
                    
                    **Request Body Example:**
                    ```json
                    {
                      "fullName": "John Doe",
                      "email": "john.doe@example.com",
                      "phone": "+1-555-123-4567",
                      "dateOfBirth": "1990-05-15",
                      "gender": "Male",
                      "status": "active",
                      "stage": "intake",
                      "clientType": "individual",
                      "assignedTherapistId": 1,
                      "streetAddress1": "123 Main Street",
                      "city": "Toronto",
                      "province": "Ontario",
                      "postalCode": "M5H 2N2",
                      "country": "Canada",
                      "hasPortalAccess": false,
                      "emailNotifications": true
                    }
                    ```
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Client information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateClientRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Complete Client",
                                    value = """
                                            {
                                              "fullName": "John Doe",
                                              "email": "john.doe@example.com",
                                              "phone": "+1-555-123-4567",
                                              "dateOfBirth": "1990-05-15",
                                              "gender": "Male",
                                              "maritalStatus": "Single",
                                              "preferredLanguage": "English",
                                              "pronouns": "he/him",
                                              "status": "active",
                                              "stage": "intake",
                                              "clientType": "individual",
                                              "assignedTherapistId": 1,
                                              "streetAddress1": "123 Main Street",
                                              "city": "Toronto",
                                              "province": "Ontario",
                                              "postalCode": "M5H 2N2",
                                              "country": "Canada",
                                              "addressLegacy": "123 Main Street, Toronto, Ontario",
                                              "stateLegacy": "Ontario",
                                              "zipCodeLegacy": "M5H 2N2",
                                              "emergencyContactName": "Jane Doe",
                                              "emergencyContactPhone": "+1-555-987-6543",
                                              "emergencyContactRelationship": "Spouse",
                                              "emergencyContactLegacy": "Jane Doe (Spouse) +1-555-987-6543",
                                              "insuranceProvider": "Blue Cross",
                                              "policyNumber": "POL123456789",
                                              "copayAmount": 25.00,
                                              "referringPersonName": "Dr. Smith",
                                              "legacyReferral": "Website",
                                              "referralType": "External",
                                              "referralNotes": "Referred for anxiety intake",
                                              "employmentStatus": "EMPLOYED_FULL_TIME",
                                              "educationLevel": "BACHELOR",
                                              "numberOfDependents": 2,
                                              "hasPortalAccess": true,
                                              "portalEmail": "maria.garcia@example.com",
                                              "emailNotifications": true,
                                              "timezone": "America/New_York",
                                              "startDate": "2026-04-23",
                                              "priority": "High",
                                              "dueDate": "2026-05-01",
                                              "followUpNotes": "Call after first session",
                                              "generalNotes": "Prefers morning appointments"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Client created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ClientResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody CreateClientRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        // ClientResponse client = clientService.createClient(request, principal, getClientIp(httpRequest));
        ClientResponse client = clientService.createClient(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(201).body(client);
    }

    @PutMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Update client",
            description = """
                    Update an existing client record. **All fields are optional** - only include the fields you want to update.
                    
                    **Close / reopen file:**
                    - Close file: `{ "status": "inactive" }` — sets status to inactive and auto-sets stage to `closed` when stage is omitted. Does **not** delete data or cancel existing sessions.
                    - Reopen file (admin only): `{ "status": "active" }` — reactivates the client; stage is unchanged unless explicitly provided.
                    - Writes `file_closed`, `file_reopened`, and `stage_change` entries to client history as applicable.
                    
                    **Note:** Only admins can change therapist assignments or reopen a closed file.
                    
                    **Optional Fields (include only what you want to update):**
                    - Contact information (fullName, email, phone, address)
                    - Demographics (dateOfBirth, gender, maritalStatus, preferredLanguage, pronouns)
                    - Assignment (assignedTherapistId, stage, clientType, status)
                    - Insurance information
                    - Emergency contact
                    - Portal access settings
                    - Notes and service details
                    - Compatibility aliases: `timezone`, `legacyAddress`/`addressLegacy`, `stateLegacy`, `zipCodeLegacy`,
                      `emergencyContactLegacy`, `startDate`, `legacyReferral`, `referringPersonName`, `referralType`,
                      `employmentStatus`, `educationLevel`, `numberOfDependents`/`dependents`, `priority`,
                      `dueDate`/`followUpDate`, `generalNotes`, `followUpNotes`, `referralNotes`
                    
                    **Request Body Example (updating only name and email):**
                    ```json
                    {
                      "fullName": "John Smith",
                      "email": "john.smith@example.com"
                    }
                    ```
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Client fields to update (all optional)",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UpdateClientRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Partial Update",
                                    value = """
                                            {
                                              "fullName": "John Smith",
                                              "email": "john.smith@example.com",
                                              "phone": "+1-555-999-8888",
                                              "timezone": "America/New_York",
                                              "dueDate": "2026-05-01",
                                              "priority": "Medium",
                                              "generalNotes": "Updated from intake form"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Client updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ClientResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ClientResponse> updateClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateClientRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ClientResponse client = clientService.updateClient(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(client);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Partially update client (PATCH)",
            description = """
                    Partially update an existing client record using PATCH semantics. 
                    Only include the fields you want to update - all other fields remain unchanged.
                    
                    **Note:** Only admins can change therapist assignments.
                    
                    **Optional Fields (include only what you want to update):**
                    - Contact information (fullName, email, phone, address)
                    - Demographics (dateOfBirth, gender, maritalStatus, preferredLanguage, pronouns)
                    - Assignment (assignedTherapistId, stage, clientType, status)
                    - Insurance information
                    - Emergency contact
                    - Portal access settings
                    - Notes and service details
                    - Compatibility aliases: `timezone`, `legacyAddress`/`addressLegacy`, `stateLegacy`, `zipCodeLegacy`,
                      `emergencyContactLegacy`, `startDate`, `legacyReferral`, `referringPersonName`, `referralType`,
                      `employmentStatus`, `educationLevel`, `numberOfDependents`/`dependents`, `priority`,
                      `dueDate`/`followUpDate`, `generalNotes`, `followUpNotes`, `referralNotes`
                    
                    **Request Body Example (updating only name):**
                    ```json
                    {
                      "fullName": "John Smith"
                    }
                    ```
                    
                    **Difference from PUT:**
                    - PATCH is semantically for partial updates (this endpoint)
                    - PUT can also do partial updates but is semantically for full replacement
                    - Both endpoints work the same way - use PATCH for clarity
                    
                    **Requires:** THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Client fields to update (all optional)",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UpdateClientRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Partial Update",
                                    value = """
                                            {
                                              "fullName": "John Smith",
                                              "followUpNotes": "Follow-up in one week"
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Client updated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ClientResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ClientResponse> patchClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateClientRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        // PATCH uses the same service method as PUT - both support partial updates
        ClientResponse client = clientService.updateClient(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(client);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.CLIENT_DELETE)
    @Operation(
            summary = "Delete client",
            description = "Permanently delete a client record. Requires CLIENT_DELETE permission.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        clientService.deleteClient(id, principal, HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize(PermissionConstants.CLIENT_DELETE)
    @Operation(
            summary = "Restore soft-deleted client",
            description = """
                    Restore a previously soft-deleted client record.
                    
                    This operation will:
                    - Verify the client is currently soft-deleted
                    - Ensure restoring will not violate uniqueness constraints for email, portalEmail, or clientId
                    - Mark the client as active again (isDeleted = false)
                    
                    **Requires:** ADMIN role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Client restored successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ClientResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Restored Client",
                                    value = """
                                            {
                                              "id": 2,
                                              "clientId": "CL-2025-0002",
                                              "fullName": "Maria Garcia",
                                              "email": "maria.garcia@example.com",
                                              "phone": "+1-555-222-3333",
                                              "status": "active",
                                              "stage": "intake",
                                              "clientType": "individual",
                                              "assignedTherapistId": 3,
                                              "hasPortalAccess": true,
                                              "portalEmail": "maria.portal@example.com",
                                              "createdAt": "2025-01-15T10:30:00Z",
                                              "updatedAt": "2025-02-01T12:00:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Client is not deleted and cannot be restored"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (Admin only)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Client not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Restoring would violate uniqueness constraints (email, portalEmail, or clientId already in use)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ClientResponse> restoreClient(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ClientResponse client = clientService.restoreClient(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{id}/portal-access")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Update client portal access",
            description = "Enable or disable portal access for a client and set portal email. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientResponse> updatePortalAccess(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody PortalAccessRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ClientResponse client = clientService.updatePortalAccess(id, request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(client);
    }

    @PostMapping("/{id}/send-portal-activation")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Send portal activation email",
            description = "Send portal activation email to client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<com.smart.therapy.flow.common.dto.SuccessResponse> sendPortalActivation(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        clientService.sendPortalActivation(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(com.smart.therapy.flow.common.dto.SuccessResponse.of("Portal activation email sent successfully"));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client history",
            description = "Get the complete history/audit trail for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<ClientHistoryResponse>> getClientHistory(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<ClientHistoryResponse> response = clientService.getClientHistoryTimeline(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/email-history")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client email history",
            description = "Get email/communication history for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientHistoryListResponse> getClientEmailHistory(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientHistoryListResponse response = clientService.getClientEmailHistory(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/stage-durations")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client stage durations",
            description = "Get the duration spent in each stage for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientStageDurationsResponse> getStageDurations(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientStageDurationsResponse durations = clientService.getStageDurations(id, principal);
        return ResponseEntity.ok(durations);
    }

    @GetMapping("/{clientId}/sessions")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client sessions",
            description = "Get all sessions for a specific client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<com.smart.therapy.flow.session.dto.SessionResponse>> getClientSessions(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<com.smart.therapy.flow.session.dto.SessionResponse> sessions = clientService.getClientSessions(clientId, principal);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{clientId}/sessions/summary")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client session summary cards",
            description = "Get aggregate session counts for client session KPI cards (total, completed, scheduled, missed/cancelled, conflicts). Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientSessionSummaryResponse> getClientSessionSummary(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientSessionSummaryResponse summary = clientService.getClientSessionSummary(clientId, principal);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{clientId}/session-conflicts")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client session conflicts",
            description = "Get session scheduling conflicts for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientSessionConflictsResponse> getClientSessionConflicts(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientSessionConflictsResponse conflicts = clientService.getClientSessionConflicts(clientId, principal);
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("/{clientId}/session-transcripts/status")
    @PreAuthorize(StaffAuthorizationExpressions.SESSION_READ)
    @Operation(
            summary = "Get client transcript upload statuses",
            description = "Get transcript pipeline status records for a specific client.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<SessionTranscriptStatusResponse>> getClientSessionTranscriptStatuses(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(sessionTranscriptService.getClientTranscriptStatuses(clientId, principal));
    }

    @PostMapping("/bulk-upload")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_CREATE)
    @Operation(
            summary = "Bulk upload clients",
            description = "Upload multiple clients at once from a JSON array. Returns success/failure counts. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BulkUploadResponse> bulkUploadClients(
            @Valid @RequestBody BulkUploadRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        BulkUploadResponse response = clientService.bulkUploadClients(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        // Return 207 (Multi-Status) if partial success, 400 if all failed, 200 if all succeeded
        if (response.getFailed() > 0 && response.getSuccessful() == 0) {
            return ResponseEntity.badRequest().body(response);
        } else if (response.getFailed() > 0) {
            return ResponseEntity.status(207).body(response); // 207 Multi-Status for partial success
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-update-stage")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_EDIT)
    @Operation(
            summary = "Bulk update client stage",
            description = "Update the stage for multiple clients at once. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BulkOperationResponse> bulkUpdateStage(
            @Valid @RequestBody BulkUpdateStageRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        BulkOperationResponse response = clientService.bulkUpdateStage(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-reassign-therapist")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_EDIT)
    @Operation(
            summary = "Bulk reassign therapist",
            description = "Reassign multiple clients to one or more therapists. Can distribute clients evenly or assign all to one therapist. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BulkOperationResponse> bulkReassignTherapist(
            @Valid @RequestBody BulkReassignTherapistRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        BulkOperationResponse response = clientService.bulkReassignTherapist(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-portal-access")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Bulk update portal access",
            description = "Enable or disable portal access for multiple clients at once. Requires ADMIN role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BulkOperationResponse> bulkUpdatePortalAccess(
            @Valid @RequestBody BulkPortalAccessRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        BulkOperationResponse response = clientService.bulkUpdatePortalAccess(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-update-status")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_EDIT)
    @Operation(
            summary = "Bulk update client status",
            description = "Update the status for multiple clients at once. Valid statuses: active, inactive, pending, discharged. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BulkOperationResponse> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        BulkOperationResponse response = clientService.bulkUpdateStatus(request, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EXPORT_ACCESS)
    @Operation(
            summary = "Export clients to CSV",
            description = "Export all clients to CSV format for download. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<String> exportClients(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        String csv = clientService.exportClientsToCsv(principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"clients_export.csv\"")
                .body(csv);
    }

    // ========== DUPLICATE DETECTION ENDPOINTS ==========

    @GetMapping("/duplicates")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_READ)
    @Operation(
            summary = "Detect duplicate clients",
            description = "Detect potential duplicate client records based on name, email, and phone matching. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<DuplicatesResponse> detectDuplicates(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        DuplicatesResponse duplicates = duplicateDetectionService.detectDuplicates(principal);
        return ResponseEntity.ok(duplicates);
    }

    @PostMapping("/{id}/mark-duplicate")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_EDIT)
    @Operation(
            summary = "Mark client as duplicate",
            description = "Mark a client as a duplicate of another client. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Object> markDuplicate(
            @Parameter(description = "Client ID to mark as duplicate", required = true)
            @PathVariable("id") Long clientId,
            @Valid @RequestBody MarkDuplicateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        duplicateDetectionService.markDuplicate(clientId, request, principal);
        return ResponseEntity.ok(new Object() {
            public final String message = "Client marked as duplicate successfully";
        });
    }

    @PostMapping("/{id}/unmark-duplicate")
    @PreAuthorize(StaffAuthorizationExpressions.ADMIN_SUPERVISOR_CLIENT_EDIT)
    @Operation(
            summary = "Unmark client as duplicate",
            description = "Remove duplicate marking from a client record. Requires ADMIN or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Object> unmarkDuplicate(
            @Parameter(description = "Client ID to unmark", required = true)
            @PathVariable("id") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        duplicateDetectionService.unmarkDuplicate(clientId, principal);
        return ResponseEntity.ok(new Object() {
            public final String message = "Client unmarked as duplicate successfully";
        });
    }

    // ========== NORMALIZED ENTITY ENDPOINTS ==========
    // Note: These endpoints are for granular management of normalized entities.
    // The existing POST /api/v1/clients and PUT /api/v1/clients/{id} endpoints
    // continue to work and handle all fields in one request, creating/updating
    // normalized entities automatically via ClientService.

    // ========== CONTACTS ==========

    @GetMapping("/{clientId}/contacts")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get all contacts for a client",
            description = "Get all contact information (emails, phones, emergency contacts) for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<ClientContactResponse>> getClientContacts(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        // Note: ClientService.createClient and updateClient already handle contacts
        // This endpoint is for viewing/managing contacts separately
        List<ClientContactResponse> contacts = clientService.getClientContacts(clientId, principal);
        return ResponseEntity.ok(contacts);
    }

    @PostMapping("/{clientId}/contacts")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Add a new contact for a client",
            description = "Add a new contact (email, phone, emergency contact) for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientContactResponse> createClientContact(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody ClientContactRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientContactResponse contact = clientService.createClientContact(clientId, request, principal);
        return ResponseEntity.status(201).body(contact);
    }

    @PutMapping("/{clientId}/contacts/{contactId}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Update a client contact",
            description = "Update an existing contact for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientContactResponse> updateClientContact(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Contact ID", required = true)
            @PathVariable("contactId") Long contactId,
            @Valid @RequestBody ClientContactRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientContactResponse contact = clientService.updateClientContact(clientId, contactId, request, principal);
        return ResponseEntity.ok(contact);
    }

    @DeleteMapping("/{clientId}/contacts/{contactId}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Delete a client contact",
            description = "Delete a contact for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteClientContact(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Contact ID", required = true)
            @PathVariable("contactId") Long contactId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        clientService.deleteClientContact(clientId, contactId, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== ADDRESSES ==========

    @GetMapping("/{clientId}/addresses")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get all addresses for a client",
            description = "Get all addresses (home, work, billing, temporary) for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<ClientAddressResponse>> getClientAddresses(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<ClientAddressResponse> addresses = clientService.getClientAddresses(clientId, principal);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/{clientId}/addresses")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Add a new address for a client",
            description = """
                    Add a new address (home, work, billing, temporary) for a client.
                    
                    Compatibility aliases supported in request body:
                    - `legacyAddress` or `addressLegacy` -> `streetAddress1`
                    - `stateLegacy` -> `stateProvince`
                    - `zipCodeLegacy` -> `postalCode`
                    
                    Requires THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientAddressResponse> createClientAddress(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody ClientAddressRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientAddressResponse address = clientService.createClientAddress(clientId, request, principal);
        return ResponseEntity.status(201).body(address);
    }

    @PutMapping("/{clientId}/addresses/{addressId}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Update a client address",
            description = """
                    Update an existing address for a client.
                    
                    Compatibility aliases supported in request body:
                    - `legacyAddress` or `addressLegacy` -> `streetAddress1`
                    - `stateLegacy` -> `stateProvince`
                    - `zipCodeLegacy` -> `postalCode`
                    
                    Requires THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientAddressResponse> updateClientAddress(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Address ID", required = true)
            @PathVariable("addressId") Long addressId,
            @Valid @RequestBody ClientAddressRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientAddressResponse address = clientService.updateClientAddress(clientId, addressId, request, principal);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{clientId}/addresses/{addressId}")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Delete a client address",
            description = "Delete an address for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteClientAddress(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Parameter(description = "Address ID", required = true)
            @PathVariable("addressId") Long addressId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        clientService.deleteClientAddress(clientId, addressId, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== INSURANCE ==========

    @GetMapping("/{clientId}/insurance")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client insurance information",
            description = "Get insurance information for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientInsuranceResponse> getClientInsurance(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientInsuranceResponse insurance = clientService.getClientInsurance(clientId, principal);
        return ResponseEntity.ok(insurance);
    }

    @PutMapping("/{clientId}/insurance")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Create or update client insurance",
            description = "Create or update insurance information for a client. If insurance exists, it will be updated; otherwise, it will be created. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientInsuranceResponse> upsertClientInsurance(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody ClientInsuranceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientInsuranceResponse insurance = clientService.upsertClientInsurance(clientId, request, principal);
        return ResponseEntity.ok(insurance);
    }

    @DeleteMapping("/{clientId}/insurance")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Delete client insurance",
            description = "Delete insurance information for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteClientInsurance(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        clientService.deleteClientInsurance(clientId, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== REFERRAL ==========

    @GetMapping("/{clientId}/referral")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client referral information",
            description = "Get referral information for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientReferralResponse> getClientReferral(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientReferralResponse referral = clientService.getClientReferral(clientId, principal);
        return ResponseEntity.ok(referral);
    }

    @PutMapping("/{clientId}/referral")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Create or update client referral",
            description = """
                    Create or update referral information for a client.
                    If referral exists, it will be updated; otherwise, it will be created.
                    
                    Compatibility aliases supported in request body:
                    - `referringPersonName` -> `referrerName`
                    - `legacyReferral` -> `clientSource`
                    
                    Requires THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientReferralResponse> upsertClientReferral(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody ClientReferralRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientReferralResponse referral = clientService.upsertClientReferral(clientId, request, principal);
        return ResponseEntity.ok(referral);
    }

    @DeleteMapping("/{clientId}/referral")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Delete client referral",
            description = "Delete referral information for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteClientReferral(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        clientService.deleteClientReferral(clientId, principal);
        return ResponseEntity.noContent().build();
    }

    // ========== EMPLOYMENT ==========

    @GetMapping("/{clientId}/employment")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_READ)
    @Operation(
            summary = "Get client employment information",
            description = "Get employment and socioeconomic information for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientEmploymentResponse> getClientEmployment(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientEmploymentResponse employment = clientService.getClientEmployment(clientId, principal);
        return ResponseEntity.ok(employment);
    }

    @PutMapping("/{clientId}/employment")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Create or update client employment",
            description = """
                    Create or update employment and socioeconomic information for a client.
                    If employment info exists, it will be updated; otherwise, it will be created.
                    
                    Compatibility alias supported in request body:
                    - `numberOfDependents` -> `dependents`
                    
                    Requires THERAPIST, ADMIN, or SUPERVISOR role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ClientEmploymentResponse> upsertClientEmployment(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @Valid @RequestBody ClientEmploymentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        ClientEmploymentResponse employment = clientService.upsertClientEmployment(clientId, request, principal);
        return ResponseEntity.ok(employment);
    }

    @DeleteMapping("/{clientId}/employment")
    @PreAuthorize(StaffAuthorizationExpressions.CLIENT_EDIT_ACCESS)
    @Operation(
            summary = "Delete client employment",
            description = "Delete employment and socioeconomic information for a client. Requires THERAPIST, ADMIN, or SUPERVISOR role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Void> deleteClientEmployment(
            @Parameter(description = "Client ID", required = true)
            @PathVariable("clientId") Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        clientService.deleteClientEmployment(clientId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{clientId}/sms-log")
    @PreAuthorize(
            PermissionConstants.CLIENT_VIEW_OWN + " or " +
            PermissionConstants.CLIENT_VIEW_TEAM + " or " +
            PermissionConstants.CLIENT_VIEW_ALL
    )
    @Operation(
            summary = "Get client SMS delivery log",
            description = "Returns SMS notification audit history for a client from audit logs.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<PaginatedResponse<ClientSmsLogResponse>> getClientSmsLog(
            @PathVariable("clientId") Long clientId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "25") int pageSize,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), appProperties.getPagination().getMaxPageSize());
        Pageable pageable = PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        var result = clientSmsLogService.getSmsLog(clientId, from, to, pageable, principal);
        return ResponseEntity.ok(PaginatedResponse.of(
                result.getContent(),
                result.getTotalElements(),
                safePage,
                safePageSize));
    }

    @GetMapping(value = "/{clientId}/sms-log/export", produces = "text/csv")
    @PreAuthorize(
            PermissionConstants.CLIENT_VIEW_OWN + " or " +
            PermissionConstants.CLIENT_VIEW_TEAM + " or " +
            PermissionConstants.CLIENT_VIEW_ALL
    )
    @Operation(
            summary = "Export client SMS delivery log",
            description = "Exports SMS notification audit history for a client as CSV.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<String> exportClientSmsLog(
            @PathVariable("clientId") Long clientId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        String csv = clientSmsLogService.exportSmsLogCsv(clientId, from, to, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + clientSmsLogService.buildExportFileName(clientId) + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

}
