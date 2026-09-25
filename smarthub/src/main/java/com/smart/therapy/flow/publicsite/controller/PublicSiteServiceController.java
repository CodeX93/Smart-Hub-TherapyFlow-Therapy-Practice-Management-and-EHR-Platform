package com.smart.therapy.flow.publicsite.controller;

import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.publicsite.dto.CreatePublicSiteServiceRequest;
import com.smart.therapy.flow.publicsite.dto.PublicSiteServiceResponse;
import com.smart.therapy.flow.publicsite.dto.UpdatePublicSiteServiceRequest;
import com.smart.therapy.flow.publicsite.service.PublicSiteServiceAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public-site/services")
@RequiredArgsConstructor
@Tag(name = "Public Site Services", description = "Marketing counseling services for the public site")
public class PublicSiteServiceController {

    private final PublicSiteServiceAdminService publicSiteServiceAdminService;

    @GetMapping
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @Operation(summary = "List all public site services")
    public ResponseEntity<List<PublicSiteServiceResponse>> list() {
        return ResponseEntity.ok(publicSiteServiceAdminService.listAll());
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Add a public site service")
    public ResponseEntity<PublicSiteServiceResponse> create(
            @Valid @RequestBody CreatePublicSiteServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicSiteServiceAdminService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Update or toggle a public site service")
    public ResponseEntity<PublicSiteServiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePublicSiteServiceRequest request) {
        return ResponseEntity.ok(publicSiteServiceAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Remove a public site service (not system)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        publicSiteServiceAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
