package com.smart.therapy.flow.document.controller;

import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.dto.CreateTagRequest;
import com.smart.therapy.flow.document.entity.LibraryTag;
import com.smart.therapy.flow.document.service.LibraryService;
import com.smart.therapy.flow.document.service.LibraryTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Library", description = "APIs for managing library entries and categories")
public class LibraryController {

        private final LibraryService libraryService;
        private final LibraryTagService libraryTagService;

        // ========== CATEGORY ENDPOINTS ==========

        @GetMapping("/categories")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get all library categories", description = """
                        Retrieve all library categories.

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryCategoryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<LibraryCategoryResponse>> getCategories() {
                List<LibraryCategoryResponse> categories = libraryService.getCategories();
                return ResponseEntity.ok(categories);
        }

        @GetMapping("/categories/{id}")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library category by ID", description = """
                        Retrieve a specific library category by its ID.

                        **Path Parameters:**
                        - `id` (REQUIRED): Category ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryCategoryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryCategoryResponse> getCategory(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Category ID", required = true, example = "1") @PathVariable("id") Long id) {
                LibraryCategoryResponse category = libraryService.getCategory(id);
                return ResponseEntity.ok(category);
        }

        @PostMapping("/categories")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Create library category", description = """
                        Create a new library category.

                        **Request Body:**
                        - See CreateLibraryCategoryRequest DTO for required/optional fields

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryCategoryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category name already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryCategoryResponse> createCategory(
                        @Valid @RequestBody CreateLibraryCategoryRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryCategoryResponse category = libraryService.createCategory(request, principal);
                return ResponseEntity.status(201).body(category);
        }

        @PutMapping("/categories/{id}")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Update library category", description = """
                        Update an existing library category.

                        **Path Parameters:**
                        - `id` (REQUIRED): Category ID

                        **Request Body:**
                        - See CreateLibraryCategoryRequest DTO for required/optional fields

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryCategoryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category name already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryCategoryResponse> updateCategory(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Category ID", required = true, example = "1") @PathVariable("id") Long id,
                        @Valid @RequestBody CreateLibraryCategoryRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryCategoryResponse category = libraryService.updateCategory(id, request, principal);
                return ResponseEntity.ok(category);
        }

        @DeleteMapping("/categories/{id}")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Delete library category", description = """
                        Delete a library category by ID.

                        **Path Parameters:**
                        - `id` (REQUIRED): Category ID

                        **Note:** This will soft-delete the category. Entries in this category will remain but the category will be marked as deleted.

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Category deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> deleteCategory(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Category ID", required = true, example = "1") @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                libraryService.deleteCategory(id, principal);
                return ResponseEntity.noContent().build();
        }

        // ========== ENTRY ENDPOINTS ==========

        @GetMapping("/entries")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library entries", description = """
                        Retrieve library entries, optionally filtered by category.

                        **Query Parameters:**
                        - `categoryId` (OPTIONAL): Filter entries by category ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entries retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<LibraryEntryResponse>> getEntries(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Category ID to filter by (OPTIONAL)", example = "1") @RequestParam(required = false) Long categoryId) {
                List<LibraryEntryResponse> entries = libraryService.getEntries(categoryId);
                return ResponseEntity.ok(entries);
        }

        @GetMapping("/entries/with-connections")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library entries with connected entries", description = """
                        Retrieve library entries (including usage count) along with their connected entries in one API call.

                        **Query Parameters:**
                        - `categoryId` (OPTIONAL): Filter base entries by category ID

                        **Response includes per entry:**
                        - Base entry details (`usageCount`, `categoryId`, `categoryName`, etc.)
                        - `connectedEntries` list (each with `categoryId` and `categoryName`)

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entries with connections retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryWithConnectionsResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<LibraryEntryWithConnectionsResponse>> getEntriesWithConnections(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Category ID to filter base entries by (OPTIONAL)", example = "1") @RequestParam(required = false) Long categoryId) {
                List<LibraryEntryWithConnectionsResponse> entries = libraryService.getEntriesWithConnections(categoryId);
                return ResponseEntity.ok(entries);
        }

        @GetMapping("/entries/{id}")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library entry by ID", description = """
                        Retrieve a specific library entry by its ID.

                        **Path Parameters:**
                        - `id` (REQUIRED): Entry ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entry retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryEntryResponse> getEntry(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID", required = true, example = "1") @PathVariable("id") Long id) {
                LibraryEntryResponse entry = libraryService.getEntry(id);
                return ResponseEntity.ok(entry);
        }

        @PostMapping("/entries")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Create library entry", description = """
                        Create a new library entry.

                        **Request Body:**
                        - See CreateLibraryEntryRequest DTO for required/optional fields

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Entry created successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Entry title already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryEntryResponse> createEntry(
                        @Valid @RequestBody CreateLibraryEntryRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryEntryResponse entry = libraryService.createEntry(request, principal);
                return ResponseEntity.status(201).body(entry);
        }

        @PostMapping({ "/entries/bulk", "/bulk-entries" })
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Bulk create library entries", description = """
                        Create multiple library entries in a single request.

                        **Request Body:**
                        - `categoryId` (optional when rows include `domain`)
                        - `entries`: array of `{ title, content, domain?, subdomain?, tags?, sortOrder? }`

                        **Response includes:**
                        - Total count of entries processed
                        - Success, skipped, and failure counts
                        - `categoriesCreated` when domain/subdomain categories are auto-created
                        - List of errors (if any)

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bulk import completed", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryBulkImportResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
public ResponseEntity<LibraryBulkImportResponse> bulkCreateEntries(
                        @Valid @RequestBody LibraryEntryBulkRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryBulkImportResponse response = libraryService.bulkCreateEntries(request, principal);
                return ResponseEntity.status(201).body(response);
        }

        @GetMapping("/entries/session-note/{field}")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library entries for session note field", description = """
                        Retrieve library entries and AI template options for a specific session note field.

                        **Path Parameters:**
                        - `field`: Session note field name (symptoms, intervention, shortTermGoals, progress, sessionFocus, recommendations, remarks)

                        **Returns:**
                        - `libraryEntries`: Entries from the library that match the field (via tags)
                        - `aiTemplateOptions`: Options from AI clinical templates (CBT, Trauma-Focused, Mindfulness)

                        **Field Mappings:**
                        - `symptoms` / `symptom` → Symptoms library entries + AI symptomsOptions
                        - `intervention` / `interventions` → Intervention library entries + AI interventionOptions
                        - `sessionFocus` / `session_focus` → AI sessionFocusOptions
                        - `shortTermGoals` / `goals` → Goals library entries
                        - `progress` → AI progressOptions
                        - `recommendations` → AI recommendationsOptions
                        - `remarks` → Remarks library entries

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entries retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionNoteFieldEntriesResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<SessionNoteFieldEntriesResponse> getSessionNoteFieldEntries(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Session note field name (e.g., symptoms, intervention, shortTermGoals)", required = true, example = "symptoms") @PathVariable("field") String field) {
                SessionNoteFieldEntriesResponse response = libraryService.getSessionNoteFieldEntries(field);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/connections")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library entry connections", description = """
                        Retrieve connections between library entries.

                        **Query Parameters:**
                        - `entryId` (OPTIONAL): Filter connections by entry ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connections retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryConnectionResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<LibraryConnectionResponse>> getConnections(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID to filter connections (OPTIONAL)", example = "1") @RequestParam(value = "entryId", required = false) Long entryId) {
                List<LibraryConnectionResponse> connections = libraryService.getConnections(entryId);
                return ResponseEntity.ok(connections);
        }

        @GetMapping("/entries/{id}/connected")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get connected entries for a library entry", description = """
                        Retrieve all entries connected to a specific library entry.

                        **Path Parameters:**
                        - `id` (REQUIRED): Entry ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connected entries retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryConnectedEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<LibraryConnectedEntryResponse>> getConnectedEntries(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID", required = true, example = "1") @PathVariable("id") Long entryId) {
                List<LibraryConnectedEntryResponse> connections = libraryService.getConnectedEntries(entryId);
                return ResponseEntity.ok(connections);
        }

        @PostMapping("/entries/connected-bulk")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get connected entries for multiple library entries", description = """
                        Retrieve connected entries for multiple library entries in a single request.

                        **Request Body:**
                        - Array of entry IDs

                        **Response:**
                        - Map of entry ID to list of connected entries

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connected entries retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json")),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Map<Long, List<LibraryConnectedEntryResponse>>> getConnectedEntriesBulk(
                        @Valid @RequestBody LibraryConnectedEntriesBulkRequest request) {
                Map<Long, List<LibraryConnectedEntryResponse>> response = libraryService
                                .getConnectedEntriesBulk(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/connections")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Create library entry connection", description = """
                        Create a connection between two library entries.

                        **Request Body:**
                        - `fromEntryId`: Source entry ID
                        - `toEntryId`: Target entry ID
                        - `connectionType`: Type of connection ("Related", "Reference", "Derived", "Supplement", "Other")
                        - `strength`: Connection strength (1-5)
                        - `description`: Optional description

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Connection created successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryConnectionResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryConnectionResponse> createConnection(
                        @Valid @RequestBody LibraryConnectionRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryConnectionResponse response = libraryService.createConnection(request, principal);
                return ResponseEntity.status(201).body(response);
        }

        @PostMapping("/connections/batch")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Create multiple library entry connections", description = """
                        Create multiple connections between library entries in a single request.

                        **Request Body:**
                        - Array of connection requests

                        **Response includes:**
                        - Total count of connections processed
                        - Success count
                        - Failure count
                        - List of created connections
                        - List of errors (if any)

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Batch connections created", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryConnectionBatchResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryConnectionBatchResponse> createConnectionsBatch(
                        @Valid @RequestBody LibraryConnectionBatchRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryConnectionBatchResponse response = libraryService.createConnectionsBatch(request, principal);
                return ResponseEntity.status(201).body(response);
        }

        @PutMapping("/connections/{id}")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Update library entry connection", description = """
                        Update an existing connection between library entries.

                        **Path Parameters:**
                        - `id` (REQUIRED): Connection ID

                        **Request Body:**
                        - See LibraryConnectionUpdateRequest DTO for updatable fields

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connection updated successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryConnectionResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Connection not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryConnectionResponse> updateConnection(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Connection ID", required = true, example = "1") @PathVariable("id") Long id,
                        @Valid @RequestBody LibraryConnectionUpdateRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryConnectionResponse response = libraryService.updateConnection(id, request, principal);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/connections/{id}")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Delete library entry connection", description = """
                        Delete a connection between library entries.

                        **Path Parameters:**
                        - `id` (REQUIRED): Connection ID

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Connection deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Connection not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> deleteConnection(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Connection ID", required = true, example = "1") @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                libraryService.deleteConnection(id, principal);
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/entries/{entryId}/connections")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Delete all connections for a library entry", description = """
                        Delete all connections associated with a specific library entry.

                        **Path Parameters:**
                        - `entryId` (REQUIRED): Entry ID

                        **Note:** This will delete all connections where this entry is either the source or target.

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Connections deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> deleteConnectionsForEntry(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID", required = true, example = "1") @PathVariable("entryId") Long entryId,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                libraryService.deleteConnectionsForEntry(entryId, principal);
                return ResponseEntity.noContent().build();
        }

        @PutMapping("/entries/{id}")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Update library entry", description = """
                        Update an existing library entry.

                        **Path Parameters:**
                        - `id` (REQUIRED): Entry ID

                        **Request Body:**
                        - See CreateLibraryEntryRequest DTO for updatable fields

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Entry updated successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Entry title already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryEntryResponse> updateEntry(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID", required = true, example = "1") @PathVariable("id") Long id,
                        @Valid @RequestBody CreateLibraryEntryRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryEntryResponse entry = libraryService.updateEntry(id, request, principal);
                return ResponseEntity.ok(entry);
        }

        @DeleteMapping("/entries/{id}")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Delete library entry", description = """
                        Delete a library entry by ID.

                        **Path Parameters:**
                        - `id` (REQUIRED): Entry ID

                        **Note:** This will soft-delete the entry. Connections to this entry will remain but the entry will be marked as deleted.

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Entry deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> deleteEntry(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID", required = true, example = "1") @PathVariable("id") Long id,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                libraryService.deleteEntry(id, principal);
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/entries/bulk")
        @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Bulk delete library entries", description = """
                        Delete multiple library entries in a single request.

                        **Request Body:**
                        - `entryIds`: List of library entry IDs to delete

                        **Response includes:**
                        - Total count processed
                        - Deleted count
                        - Failed count
                        - List of per-entry errors (if any)

                        **Requires:** ADMIN or SUPERVISOR role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bulk delete completed", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryBulkDeleteResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryBulkDeleteResponse> bulkDeleteEntries(
                        @Valid @RequestBody LibraryBulkDeleteRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal) {
                LibraryBulkDeleteResponse response = libraryService.bulkDeleteEntries(request, principal);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/search")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Search library entries", description = """
                        Search library entries by query string.

                        **Query Parameters:**
                        - `q` (REQUIRED): Search query string (searches in title, content, tags, and category name)
                        - `categoryId` (OPTIONAL): Filter by category ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing required query parameter"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<LibraryEntryResponse>> searchEntries(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Search query (REQUIRED)", required = true, example = "anxiety") @RequestParam("q") String query,
                        @io.swagger.v3.oas.annotations.Parameter(description = "Category ID to filter by (OPTIONAL)", example = "1") @RequestParam(required = false) Long categoryId) {
                List<LibraryEntryResponse> entries = libraryService.searchEntries(query, categoryId);
                return ResponseEntity.ok(entries);
        }

        @PostMapping("/entries/{id}/increment-usage")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Increment library entry usage count", description = """
                        Increment the usage counter for a specific library entry.

                        This is typically called when a therapist/admin/supervisor views or uses
                        a library entry in treatment planning or documentation.

                        **Path Parameters:**
                        - `id` (REQUIRED): Entry ID

                        **Response Body:** Returns the updated library entry, including the new `usageCount`.

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usage count incremented successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = LibraryEntryResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Library entry not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<LibraryEntryResponse> incrementUsage(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Entry ID", required = true, example = "1") @PathVariable("id") Long id) {
                LibraryEntryResponse updated = libraryService.incrementUsage(id);
                return ResponseEntity.ok(updated);
        }

        // ========== TAG MANAGEMENT ENDPOINTS ==========

        @GetMapping("/tags")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get all library tags with usage statistics", description = """
                        Retrieve all library tags with usage counts.

                        **Returns:**
                        - Tag ID
                        - Tag name
                        - Usage count (number of entries using this tag)
                        - Created/updated timestamps

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tags retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TagResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<List<TagResponse>> getAllTags() {
                List<TagResponse> tags = libraryTagService.getAllTagsWithStats();
                return ResponseEntity.ok(tags);
        }

        @GetMapping("/tags/{id}")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Get library tag by ID with statistics", description = """
                        Retrieve a specific library tag with usage count.

                        **Path Parameters:**
                        - `id` (REQUIRED): Tag ID

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tag retrieved successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TagResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<TagResponse> getTag(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Tag ID", required = true, example = "1") @PathVariable("id") Long id) {
                TagResponse tag = libraryTagService.getTagWithStats(id);
                return ResponseEntity.ok(tag);
        }

        @PostMapping("/tags")
        @PreAuthorize(PermissionConstants.USER_MANAGE)
        @io.swagger.v3.oas.annotations.Operation(summary = "Create library tag manually (Admin only)", description = """
                        Manually create a new library tag.

                        **Note:** Tags are usually auto-created when used in library entries,
                        but this endpoint allows admins to pre-create tags.

                        **Request Body:**
                        - `name`: Tag name (required, 1-255 characters)

                        **Requires:** ADMIN role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tag created successfully", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TagResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tag name already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
                LibraryTag tag = libraryTagService.findOrCreateTag(request.getName());
                TagResponse response = libraryTagService.getTagWithStats(tag.getId());
                return ResponseEntity.status(201).body(response);
        }

        @DeleteMapping("/tags/{id}")
        @PreAuthorize(PermissionConstants.USER_MANAGE)
        @io.swagger.v3.oas.annotations.Operation(summary = "Delete unused library tag (Admin only)", description = """
                        Delete a library tag if it's not being used by any entries.

                        **Path Parameters:**
                        - `id` (REQUIRED): Tag ID

                        **Note:** This will fail if the tag is still in use by any library entries.
                        Remove the tag from all entries first before deleting.

                        **Requires:** ADMIN role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Tag deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tag is still in use"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> deleteTag(
                        @io.swagger.v3.oas.annotations.Parameter(description = "Tag ID", required = true, example = "1") @PathVariable("id") Long id) {
                boolean deleted = libraryTagService.deleteUnusedTag(id);
                if (!deleted) {
                        throw new com.smart.therapy.flow.common.exception.BadRequestException(
                                        "Cannot delete tag - still in use by library entries");
                }
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/entries/tags/bulk")
        @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
        @io.swagger.v3.oas.annotations.Operation(summary = "Bulk add/remove tags from library entries", description = """
                        Add or remove tags from multiple library entries in a single operation.

                        **Request Body:**
                        - `entryIds`: List of library entry IDs (required)
                        - `tagsToAdd`: List of tag names to add (optional)
                        - `tagsToRemove`: List of tag names to remove (optional)

                        **Example:**
                        ```json
                        {
                          "entryIds": [1, 2, 3, 4],
                          "tagsToAdd": ["featured", "recommended"],
                          "tagsToRemove": ["draft"]
                        }
                        ```

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                        """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Bulk tag operation completed successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid data"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<Void> bulkTagOperation(@Valid @RequestBody BulkTagRequest request) {
                // Add tags
                if (request.getTagsToAdd() != null && !request.getTagsToAdd().isEmpty()) {
                        libraryTagService.addTagsToEntries(request.getEntryIds(), request.getTagsToAdd());
                }

                // Remove tags
                if (request.getTagsToRemove() != null && !request.getTagsToRemove().isEmpty()) {
                        libraryTagService.removeTagsFromEntries(request.getEntryIds(), request.getTagsToRemove());
                }

                return ResponseEntity.noContent().build();
        }
}

