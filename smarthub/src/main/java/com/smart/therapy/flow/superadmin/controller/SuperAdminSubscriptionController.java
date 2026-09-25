package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSubscriptionDetailsResponse;
import com.smart.therapy.flow.billing.service.PlatformSubscriptionBackfillService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSubscriptionBackfillResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSubscriptionInvoiceResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSubscriptionStatusResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSubscriptionUpdateRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/super-admin/organisations")
@RequiredArgsConstructor
@Tag(name = "Super Admin Subscriptions", description = "Strict organisation subscription endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminSubscriptionController {

    private final SuperAdminSubscriptionService superAdminSubscriptionService;
    private final PlatformSubscriptionBackfillService platformSubscriptionBackfillService;

    @GetMapping("/{id}/subscription")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get strict organisation subscription",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSubscriptionDetailsResponse> getSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(superAdminSubscriptionService.getStrictSubscription(id)));
    }

    @GetMapping("/{id}/subscription/status")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get organisation subscription status and enforcement flags",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSubscriptionStatusResponse> getSubscriptionStatus(@PathVariable Long id) {
        return ResponseEntity.ok(toStatusResponse(superAdminSubscriptionService.getSubscriptionStatus(id)));
    }

    @PutMapping("/{id}/subscription")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update strict organisation subscription",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSubscriptionDetailsResponse> updateSubscription(
            @PathVariable Long id,
            @RequestBody SuperAdminSubscriptionUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminSubscriptionService.UserLimitPatch patch = null;
        if (request != null && request.getUserLimits() != null) {
            patch = new SuperAdminSubscriptionService.UserLimitPatch(
                    request.getUserLimits().getTherapistLimit(),
                    request.getUserLimits().getSupervisorLimit(),
                    request.getUserLimits().getClientLimit()
            );
        }

        SuperAdminSubscriptionService.StrictSubscriptionView updated = superAdminSubscriptionService.updateStrictSubscription(
                id,
                request != null ? request.getPlan() : null,
                request != null ? request.getBillingCycle() : null,
                request != null ? request.getTrialDays() : null,
                request != null ? request.getEffectiveDate() : null,
                request != null ? request.getProrate() : null,
                patch,
                request != null ? request.getCreateRenewalInvoice() : null,
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/{id}/subscription/provision-stripe")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Provision Stripe customer and subscription for organisation",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSubscriptionDetailsResponse> provisionStripeSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        superAdminSubscriptionService.provisionStripeSubscription(id, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.ok(toResponse(superAdminSubscriptionService.getStrictSubscription(id)));
    }

    @PostMapping("/{id}/subscription/invoices")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create manual renewal invoice for manual-mode organisation",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSubscriptionInvoiceResponse> createManualRenewalInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var invoice = superAdminSubscriptionService.createManualRenewalInvoice(id, principal != null ? principal.getAuthId() : null);
        SuperAdminSubscriptionInvoiceResponse body = new SuperAdminSubscriptionInvoiceResponse();
        body.setInvoiceId(invoice.getId());
        body.setSubscriptionId(invoice.getSubscription() != null ? invoice.getSubscription().getId() : null);
        body.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        body.setAmount(invoice.getAmount());
        body.setOutstandingBalance(invoice.getOutstandingBalance());
        body.setDueDate(invoice.getDueDate());
        body.setBillingPeriodStart(invoice.getBillingPeriodStart());
        body.setBillingPeriodEnd(invoice.getBillingPeriodEnd());
        return ResponseEntity.status(201).body(body);
    }

    @PostMapping("/{id}/subscription/backfill-stripe")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Backfill Stripe provisioning and invoice sync for an organisation",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSubscriptionBackfillResponse> backfillStripeSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        PlatformSubscriptionBackfillService.BackfillResult result = platformSubscriptionBackfillService.backfillOrganisation(
                id, principal != null ? principal.getAuthId() : null);
        SuperAdminSubscriptionBackfillResponse body = new SuperAdminSubscriptionBackfillResponse();
        body.setSuccess(result.success());
        body.setNotFound(result.notFound());
        body.setAlreadyProvisioned(result.alreadyProvisioned());
        body.setProviderSubscriptionId(result.providerSubscriptionId());
        body.setInvoicesSynced(result.invoicesSynced());
        return ResponseEntity.ok(body);
    }

    private static SuperAdminSubscriptionDetailsResponse toResponse(SuperAdminSubscriptionService.StrictSubscriptionView view) {
        SuperAdminSubscriptionDetailsResponse body = new SuperAdminSubscriptionDetailsResponse();
        body.setOrganisationId(view.organisationId());
        body.setSubscriptionId(view.subscriptionId());
        body.setPlan(view.plan());
        body.setStatus(view.status());
        body.setBillingCycle(view.billingCycle());
        body.setPriceAtTime(view.priceAtTime());
        body.setStartAt(view.startAt());
        body.setEndAt(view.endAt());
        body.setTrialEndsAt(view.trialEndsAt());
        body.setProviderCustomerId(view.providerCustomerId());
        body.setProviderSubscriptionId(view.providerSubscriptionId());

        SuperAdminSubscriptionDetailsResponse.UserLimits limits = new SuperAdminSubscriptionDetailsResponse.UserLimits();
        limits.setTherapistLimit(view.therapistLimit());
        limits.setSupervisorLimit(view.supervisorLimit());
        limits.setClientLimit(view.clientLimit());
        body.setUserLimits(limits);

        SuperAdminSubscriptionDetailsResponse.UserUsage usage = new SuperAdminSubscriptionDetailsResponse.UserUsage();
        usage.setTherapistUsers(view.therapistUsers());
        usage.setSupervisorUsers(view.supervisorUsers());
        usage.setClientUsers(view.clientUsers());
        usage.setTotalUsers(view.therapistUsers() + view.supervisorUsers() + view.clientUsers());
        body.setUserUsage(usage);
        return body;
    }

    private static SuperAdminSubscriptionStatusResponse toStatusResponse(SuperAdminSubscriptionService.SubscriptionStatusView view) {
        SuperAdminSubscriptionStatusResponse body = new SuperAdminSubscriptionStatusResponse();
        body.setOrganisationId(view.organisationId());
        body.setSubscriptionId(view.subscriptionId());
        body.setPlan(view.plan());
        body.setStatus(view.status());
        body.setOrganisationStatus(view.organisationStatus());
        body.setStartAt(view.startAt());
        body.setEndAt(view.endAt());
        body.setTrialEndsAt(view.trialEndsAt());
        body.setCurrent(view.isCurrent());
        body.setTrialExpired(view.isTrialExpired());
        body.setPastDue(view.isPastDue());
        body.setCancelled(view.isCancelled());
        body.setAccessRestricted(view.isAccessRestricted());
        body.setProviderCustomerConfigured(view.providerCustomerConfigured());
        body.setProviderSubscriptionConfigured(view.providerSubscriptionConfigured());
        body.setProviderBillingConfigured(view.providerBillingConfigured());
        body.setBillingMode(view.billingMode());
        return body;
    }
}
