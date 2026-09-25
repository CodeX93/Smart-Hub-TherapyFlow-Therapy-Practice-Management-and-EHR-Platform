package com.smart.therapy.flow.billing.controller;

import com.smart.therapy.flow.billing.dto.InvoicePolicyRequest;
import com.smart.therapy.flow.billing.dto.InvoicePolicyResponse;
import com.smart.therapy.flow.billing.dto.InvoicePolicyServiceOptionResponse;
import com.smart.therapy.flow.billing.service.InvoicePolicyService;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.system.dto.SystemOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/invoice-policies")
@RequiredArgsConstructor
@Tag(name = "Invoice Policy", description = "Dynamic invoice policy configuration by client type and appointment status")
public class InvoicePolicyController {

    private final InvoicePolicyService invoicePolicyService;
    private final SubscriptionFeatureService subscriptionFeatureService;

    private void requireBillingModule() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !subscriptionFeatureService.isFeatureEnabled(orgId, SubscriptionFeatureService.FEATURE_BILLING_MODULE, null)) {
            throw new ForbiddenException("Billing is not included in your plan. Please upgrade to access billing and invoicing.");
        }
    }

    @GetMapping
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @Operation(
            summary = "List invoice policies",
            description = "Returns all configured invoice policies for the current tenant organisation.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policies fetched successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvoicePolicyResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden or billing feature not enabled")
    })
    public ResponseEntity<List<InvoicePolicyResponse>> listPolicies() {
        requireBillingModule();
        return ResponseEntity.ok(invoicePolicyService.listPolicies());
    }

    @GetMapping("/{id}")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @Operation(summary = "Get invoice policy by ID", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<InvoicePolicyResponse> getPolicy(
            @Parameter(description = "Invoice policy ID", required = true) @PathVariable("id") Long id) {
        requireBillingModule();
        return ResponseEntity.ok(invoicePolicyService.getPolicy(id));
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Create invoice policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<InvoicePolicyResponse> createPolicy(
            @Valid @RequestBody InvoicePolicyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        requireBillingModule();
        return ResponseEntity.status(201).body(
                invoicePolicyService.createPolicy(request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Update invoice policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<InvoicePolicyResponse> updatePolicy(
            @PathVariable("id") Long id,
            @Valid @RequestBody InvoicePolicyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        requireBillingModule();
        return ResponseEntity.ok(
                invoicePolicyService.updatePolicy(id, request, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Activate invoice policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<InvoicePolicyResponse> activatePolicy(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        requireBillingModule();
        return ResponseEntity.ok(
                invoicePolicyService.activatePolicy(id, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Deactivate invoice policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<InvoicePolicyResponse> deactivatePolicy(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        requireBillingModule();
        return ResponseEntity.ok(
                invoicePolicyService.deactivatePolicy(id, principal, HttpRequestUtil.getClientIp(httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Delete invoice policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> deletePolicy(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        requireBillingModule();
        invoicePolicyService.deletePolicy(id, principal, HttpRequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/options/client-types")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @Operation(summary = "Get client type options", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SystemOptionResponse>> getClientTypeOptions() {
        requireBillingModule();
        return ResponseEntity.ok(invoicePolicyService.getClientTypeOptions());
    }

    @GetMapping("/options/appointment-statuses")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @Operation(summary = "Get appointment status options", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SystemOptionResponse>> getAppointmentStatusOptions() {
        requireBillingModule();
        return ResponseEntity.ok(invoicePolicyService.getAppointmentStatusOptions());
    }

    @GetMapping("/options/services")
    @PreAuthorize(PermissionConstants.BILLING_READ_ACCESS)
    @Operation(
            summary = "Get service options for invoice policy scope",
            description = "Returns active billing services plus an `all` option (serviceId null) for every service.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<InvoicePolicyServiceOptionResponse>> getServiceOptions() {
        requireBillingModule();
        return ResponseEntity.ok(invoicePolicyService.getServiceOptions());
    }
}
