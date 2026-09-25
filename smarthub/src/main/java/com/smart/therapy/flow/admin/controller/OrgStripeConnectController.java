package com.smart.therapy.flow.admin.controller;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.payment.dto.OrgStripeConnectStatusResponse;
import com.smart.therapy.flow.payment.dto.StripeConnectOauthStartResponse;
import com.smart.therapy.flow.payment.dto.TenantStripeConfigResponse;
import com.smart.therapy.flow.payment.dto.TenantStripeConfigUpsertRequest;
import com.smart.therapy.flow.payment.service.OrgStripeConnectService;
import com.smart.therapy.flow.payment.service.StripeTenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/stripe-connect")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Org Stripe Connect", description = "Organization-admin Stripe Connect setup and management")
public class OrgStripeConnectController {

    private final OrgStripeConnectService orgStripeConnectService;
    private final StripeTenantConfigService stripeTenantConfigService;

    @GetMapping("/status")
    @Operation(summary = "Get Stripe Connect status", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    public ResponseEntity<OrgStripeConnectStatusResponse> getStatus() {
        return ResponseEntity.ok(orgStripeConnectService.getStatus(requireOrganisationId()));
    }

    @PostMapping("/oauth/start")
    @Operation(summary = "Start Stripe Connect OAuth", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    public ResponseEntity<StripeConnectOauthStartResponse> startOauth(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long actor = principal != null ? principal.getAuthId() : null;
        return ResponseEntity.ok(orgStripeConnectService.startOauth(requireOrganisationId(), actor));
    }

    @GetMapping("/oauth/callback")
    @Operation(summary = "Handle Stripe Connect OAuth callback and redirect to the staff frontend")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam("state") String state,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long actor = principal != null ? principal.getAuthId() : null;
        try {
            orgStripeConnectService.handleOauthCallbackByState(
                    state, code, error, errorDescription, actor);
        } catch (BadRequestException ex) {
            log.warn("Stripe Connect OAuth callback failed: {}", ex.getMessage());
        }
        return redirect(orgStripeConnectService.getOauthFrontendSuccessUrl());
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Stripe Connect capabilities/status", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    public ResponseEntity<OrgStripeConnectStatusResponse> refresh(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long actor = principal != null ? principal.getAuthId() : null;
        return ResponseEntity.ok(orgStripeConnectService.refresh(requireOrganisationId(), actor));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect Stripe Connect account", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    public ResponseEntity<OrgStripeConnectStatusResponse> disconnect(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long actor = principal != null ? principal.getAuthId() : null;
        return ResponseEntity.ok(orgStripeConnectService.disconnect(requireOrganisationId(), actor));
    }

    @GetMapping("/config")
    @Operation(summary = "Get tenant Stripe config", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    public ResponseEntity<TenantStripeConfigResponse> getTenantStripeConfig() {
        return ResponseEntity.ok(stripeTenantConfigService.getConfig(requireOrganisationId()));
    }

    @PutMapping("/config")
    @Operation(summary = "Update tenant Stripe config", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize(PermissionConstants.BILLING_MANAGE)
    public ResponseEntity<TenantStripeConfigResponse> upsertTenantStripeConfig(
            @RequestBody TenantStripeConfigUpsertRequest request
    ) {
        return ResponseEntity.ok(stripeTenantConfigService.upsertConfig(requireOrganisationId(), request));
    }

    private Long requireOrganisationId() {
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            throw new BadRequestException("Organization context not resolved");
        }
        return organisationId;
    }
}
