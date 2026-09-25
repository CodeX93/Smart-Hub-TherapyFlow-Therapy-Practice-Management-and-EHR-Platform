package com.smart.therapy.flow.system.controller;

import com.smart.therapy.flow.system.dto.*;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.system.service.SystemOptionService;
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
@RequestMapping("/api/v1/system-options")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "System Options", description = "APIs for managing system options and categories")
public class SystemOptionController {

    private final SystemOptionService systemOptionService;

    // ========== CATEGORY ENDPOINTS ==========

    @GetMapping("/categories")
    @PreAuthorize("authenticated")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get all option categories",
            description = """
                    Retrieve all system option categories. Requires any authenticated JWT token.

                    **Query Parameters:**
                    - `includeInactive` (OPTIONAL, default false): also list inactive options,
                      for the management screen; consumers should keep the active-only default
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<OptionCategoryResponse>> getCategories(
            @RequestParam(name = "includeInactive", required = false, defaultValue = "false") boolean includeInactive
    ) {
        List<OptionCategoryResponse> categories = systemOptionService.getCategories(includeInactive);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("authenticated")
    public ResponseEntity<OptionCategoryResponse> getCategory(
            @PathVariable("id") Long id,
            @RequestParam(name = "includeInactive", required = false, defaultValue = "false") boolean includeInactive
    ) {
        OptionCategoryResponse category = systemOptionService.getCategory(id, includeInactive);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/categories/{id}/usage")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<OptionCategoryUsageResponse> getCategoryUsage(@PathVariable("id") Long id) {
        OptionCategoryUsageResponse usage = systemOptionService.getCategoryUsage(id);
        return ResponseEntity.ok(usage);
    }

    @PostMapping("/categories")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create option category",
            description = """
                    Create a new system option category.
                    
                    **Request Body:**
                    - See CreateOptionCategoryRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN role JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<OptionCategoryResponse> createCategory(
            @Valid @RequestBody CreateOptionCategoryRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        OptionCategoryResponse category = systemOptionService.createCategory(request, principal);
        return ResponseEntity.status(201).body(category);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<OptionCategoryResponse> updateCategory(
            @PathVariable("id") Long id,
            @RequestBody UpdateOptionCategoryRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        OptionCategoryResponse category = systemOptionService.updateCategory(id, request, principal);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<Void> deleteCategory(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        systemOptionService.deleteCategory(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/categories/{id}/options/order")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<List<SystemOptionResponse>> reorderCategoryOptions(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReorderCategoryOptionsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SystemOptionResponse> options = systemOptionService.reorderCategoryOptions(id, request, principal);
        return ResponseEntity.ok(options);
    }

    // ========== OPTION ENDPOINTS ==========

    @GetMapping
    @PreAuthorize("authenticated")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Get system options",
            description = """
                    Retrieve system options, optionally filtered by category.
                    
                    **Query Parameters:**
                    - `categoryId` (OPTIONAL): Filter options by category ID
                    
                    **Requires:** any authenticated JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<SystemOptionResponse>> getOptions(
            @io.swagger.v3.oas.annotations.Parameter(description = "Category ID to filter by (OPTIONAL)", example = "1")
            @RequestParam(required = false) Long categoryId
    ) {
        List<SystemOptionResponse> options = systemOptionService.getOptions(categoryId);
        return ResponseEntity.ok(options);
    }

    @GetMapping("/by-category/{categoryKey}")
    @PreAuthorize("authenticated")
    public ResponseEntity<List<SystemOptionResponse>> getOptionsByCategoryKey(
            @PathVariable("categoryKey") String categoryKey
    ) {
        List<SystemOptionResponse> options = systemOptionService.getOptionsByCategoryKey(categoryKey);
        return ResponseEntity.ok(options);
    }

    @GetMapping("/{id}")
    @PreAuthorize("authenticated")
    public ResponseEntity<SystemOptionResponse> getOption(@PathVariable("id") Long id) {
        SystemOptionResponse option = systemOptionService.getOption(id);
        return ResponseEntity.ok(option);
    }

    @GetMapping("/{id}/usage")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<SystemOptionUsageResponse> getOptionUsage(@PathVariable("id") Long id) {
        SystemOptionUsageResponse usage = systemOptionService.getOptionUsage(id);
        return ResponseEntity.ok(usage);
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create system option",
            description = """
                    Create a new system option.
                    
                    **Request Body:**
                    - See CreateSystemOptionRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN role JWT token.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SystemOptionResponse> createOption(
            @Valid @RequestBody CreateSystemOptionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SystemOptionResponse option = systemOptionService.createOption(request, principal);
        return ResponseEntity.status(201).body(option);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<SystemOptionResponse> updateOption(
            @PathVariable("id") Long id,
            @RequestBody UpdateSystemOptionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SystemOptionResponse option = systemOptionService.updateOption(id, request, principal);
        return ResponseEntity.ok(option);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<Void> deleteOption(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        systemOptionService.deleteOption(id, principal);
        return ResponseEntity.noContent().build();
    }
}


