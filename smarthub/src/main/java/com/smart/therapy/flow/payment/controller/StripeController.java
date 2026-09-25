package com.smart.therapy.flow.payment.controller;

import com.smart.therapy.flow.payment.dto.StripeCheckoutResponse;
import com.smart.therapy.flow.payment.service.StripePaymentApplicationService;
import com.smart.therapy.flow.payment.service.StripeWebhookApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
@Tag(name = "Stripe Payment", description = "APIs for Stripe payment processing and webhooks")
public class StripeController {

    private final StripePaymentApplicationService stripePaymentApplicationService;
    private final StripeWebhookApplicationService stripeWebhookApplicationService;

    private String getSessionToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("portalSessionToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @Operation(summary = "Initiate Stripe payment for invoice", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<StripeCheckoutResponse> initiatePayment(
            @Parameter(description = "Invoice ID", required = true) @PathVariable("invoiceId") Long invoiceId,
            HttpServletRequest request
    ) {
        StripeCheckoutResponse response = stripePaymentApplicationService.initiatePayment(invoiceId, getSessionToken(request));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook/platform")
    @Operation(summary = "Handle platform Stripe webhook events")
    public ResponseEntity<Object> handlePlatformWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) throws IOException {
        return stripeWebhookApplicationService.handlePlatformWebhook(readRawBody(request), signature);
    }

    @PostMapping("/webhook/connect")
    @Operation(summary = "Handle Stripe Connect webhook events")
    public ResponseEntity<Object> handleConnectWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) throws IOException {
        return stripeWebhookApplicationService.handleConnectWebhook(readRawBody(request), signature);
    }

    @PostMapping("/webhook/tenant/{tenantKey}")
    @Operation(summary = "Handle tenant Stripe webhook events")
    public ResponseEntity<Object> handleTenantWebhook(
            @PathVariable("tenantKey") String tenantKey,
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) throws IOException {
        return stripeWebhookApplicationService.handleTenantWebhook(tenantKey, readRawBody(request), signature);
    }

    private static String readRawBody(HttpServletRequest request) throws IOException {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
