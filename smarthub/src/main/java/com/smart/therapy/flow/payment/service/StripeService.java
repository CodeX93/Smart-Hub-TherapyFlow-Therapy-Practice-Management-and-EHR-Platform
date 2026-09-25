package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.audit.support.AuditEventDraft;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.audit.BillingAuditActions;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.payment.dto.StripeCheckoutResponse;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    @Value("${app.frontend.client-portal-invoice-success-url:http://localhost:3000/portal/invoices?payment=success&session_id={CHECKOUT_SESSION_ID}}")
    private String clientPortalInvoiceSuccessUrl;

    @Value("${app.frontend.client-portal-invoice-cancel-url:http://localhost:3000/portal/invoices?payment=cancelled}")
    private String clientPortalInvoiceCancelUrl;

    private final SessionBillingRepository billingRepository;
    private final AuditLogService auditLogService;
    private final ClientRepository clientRepository;
    private final com.smart.therapy.flow.billing.repository.PaymentRepository paymentRepository;
    private final com.smart.therapy.flow.billing.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final StripePlatformConfigService stripePlatformConfigService;
    private final StripeTenantConfigService stripeTenantConfigService;
    private final OrgStripeConnectService orgStripeConnectService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final BillingService billingService;

    // Not @Transactional: Session.create is an external HTTP call and must not hold a DB connection.
    @CircuitBreaker(name = "stripe", fallbackMethod = "createCheckoutSessionFallback")
    @Retry(name = "stripe")
    @TimeLimiter(name = "stripe")
    public CompletableFuture<StripeCheckoutResponse> createCheckoutSessionAsync(Long invoiceId, Long clientId,
                                                                                 String invoiceAmount,
                                                                                 String serviceName, String serviceCode,
                                                                                 String sessionType, String sessionDate,
                                                                                 String clientEmail) {
        return CompletableFuture.supplyAsync(() -> createCheckoutSession(invoiceId, clientId, invoiceAmount,
                serviceName, serviceCode, sessionType, sessionDate, clientEmail));
    }

    // Not @Transactional: Stripe HTTP must run outside any held JDBC connection / open TX.
    @CircuitBreaker(name = "stripe", fallbackMethod = "createCheckoutSessionFallback")
    @Retry(name = "stripe")
    public StripeCheckoutResponse createCheckoutSession(Long invoiceId, Long clientId, String invoiceAmount,
                                                        String serviceName, String serviceCode, String sessionType,
                                                        String sessionDate, String clientEmail) {
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            throw new BadRequestException("Organization context is required for invoice payment");
        }
        requireStripePaymentsEnabled(organisationId);
        String connectedAccountId = orgStripeConnectService.requireReadyConnectAccountId(organisationId);
        return createCheckoutSession(invoiceId, clientId, invoiceAmount, serviceName, serviceCode, sessionType,
                sessionDate, clientEmail, stripePlatformConfigService.requirePlatformSecretKey(), connectedAccountId, organisationId);
    }

    // Not @Transactional: Stripe HTTP must run outside any held JDBC connection / open TX.
    @CircuitBreaker(name = "stripe", fallbackMethod = "createCheckoutSessionFallback")
    @Retry(name = "stripe")
    public StripeCheckoutResponse createCheckoutSession(Long invoiceId, Long clientId, String invoiceAmount,
                                                        String serviceName, String serviceCode, String sessionType,
                                                        String sessionDate, String clientEmail,
                                                        String connectedAccountId) {
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            throw new BadRequestException("Organization context is required for invoice payment");
        }
        return createCheckoutSession(invoiceId, clientId, invoiceAmount, serviceName, serviceCode, sessionType,
                sessionDate, clientEmail, stripePlatformConfigService.requirePlatformSecretKey(), connectedAccountId, organisationId);
    }

    private StripeCheckoutResponse createCheckoutSession(Long invoiceId, Long clientId, String invoiceAmount,
                                                         String serviceName, String serviceCode, String sessionType,
                                                         String sessionDate, String clientEmail,
                                                         String apiKey, String connectedAccountId, Long organisationId) {
        Objects.requireNonNull(invoiceId, "Invoice ID is required");
        Objects.requireNonNull(clientId, "Client ID is required");

        if (!StringUtils.hasText(apiKey)) {
            throw new BadRequestException("Stripe API key is not configured");
        }
        try {
            log.info("Creating Stripe checkout session: invoiceId={}, clientId={}, connectedAccount={}, correlationId={}",
                    invoiceId, clientId, connectedAccountId, MDC.get("correlationId"));
            long amountCents = Math.round(Double.parseDouble(invoiceAmount) * 100);

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(clientPortalInvoiceSuccessUrl)
                    .setCancelUrl(clientPortalInvoiceCancelUrl)
                    .setClientReferenceId(invoiceId.toString())
                    .putMetadata("invoiceId", invoiceId.toString())
                    .putMetadata("clientId", clientId.toString())
                    .putMetadata("organisationId", String.valueOf(organisationId))
                    .putMetadata("portalPayment", "true");
            if (StringUtils.hasText(connectedAccountId)) {
                paramsBuilder.putMetadata("connectedAccountId", connectedAccountId);
            }

            if (clientEmail != null && !clientEmail.isEmpty()) {
                paramsBuilder.setCustomerEmail(clientEmail);
            }

            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(amountCents)
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName((serviceName != null ? serviceName : serviceCode) + " - "
                                                            + sessionType)
                                                    .setDescription("Session on " + sessionDate)
                                                    .build())
                                    .build())
                    .build();
            paramsBuilder.addLineItem(lineItem);

            RequestOptions.RequestOptionsBuilder optionsBuilder = RequestOptions.builder()
                    .setApiKey(apiKey)
                    .setConnectTimeout(10_000)
                    .setReadTimeout(30_000);
            if (StringUtils.hasText(connectedAccountId)) {
                optionsBuilder.setStripeAccount(connectedAccountId);
            }
            Session session = Session.create(paramsBuilder.build(), optionsBuilder.build());

            // Success audit is recorded by the portal/caller with request IP/UA.
            // Do not persist audit here — a blocked audit insert must not delay the checkout URL.
            return StripeCheckoutResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            log.error("Stripe API error creating checkout session: invoiceId={}, correlationId={}",
                    invoiceId, MDC.get("correlationId"), e);
            recordAuditEvent(clientId,
                    BillingAuditActions.PAYMENT_INITIATED, "stripe-api", "stripe-sdk", "failure",
                    "Stripe API error: " + e.getMessage(), invoiceId, invoiceAmount, null);
            throw new BadRequestException("Failed to create payment session: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating Stripe checkout session: invoiceId={}, correlationId={}",
                    invoiceId, MDC.get("correlationId"), e);
            recordAuditEvent(clientId,
                    BillingAuditActions.PAYMENT_INITIATED, "stripe-api", "stripe-sdk", "failure",
                    "Stripe connection error: " + e.getMessage(), invoiceId, invoiceAmount, null);
            throw new BadRequestException(
                    "Unable to reach Stripe to start checkout. Check network/DNS access to api.stripe.com and try again.");
        }
    }

    public StripeCheckoutResponse createCheckoutSessionFallback(Long invoiceId, Long clientId, String invoiceAmount,
                                                                String serviceName, String serviceCode,
                                                                String sessionType, String sessionDate,
                                                                String clientEmail, Exception ex) {
        log.error("Stripe circuit breaker fallback triggered: invoiceId={}, correlationId={}",
                invoiceId, MDC.get("correlationId"), ex);
        throw new BadRequestException("Payment service temporarily unavailable. Please try again later.");
    }

    private void requireStripePaymentsEnabled(Long organisationId) {
        if (!subscriptionFeatureService.isFeatureEnabled(
                organisationId,
                SubscriptionFeatureService.FEATURE_STRIPE_PAYMENTS,
                null)) {
            throw new ForbiddenException("Stripe payments are not included in your plan. Please upgrade to enable online payments.");
        }
    }

    @Transactional
    public void handleConnectedAccountWebhookEvent(Map<String, Object> eventData, String eventType, String connectedAccountId) {
        if (!"checkout.session.completed".equals(eventType)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = (Map<String, Object>) eventData.get("object");
        if (sessionData == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) sessionData.get("metadata");
        if (metadata == null) {
            log.error("Missing metadata in Stripe webhook event");
            return;
        }

        String invoiceIdStr = metadata.get("invoiceId") != null ? String.valueOf(metadata.get("invoiceId")) : null;
        String clientIdStr = metadata.get("clientId") != null ? String.valueOf(metadata.get("clientId")) : null;
        String organisationIdStr = metadata.get("organisationId") != null ? String.valueOf(metadata.get("organisationId")) : null;

        String metadataConnectedAccountId = metadata.get("connectedAccountId") != null
                ? String.valueOf(metadata.get("connectedAccountId")) : null;

        if (!StringUtils.hasText(invoiceIdStr) || !StringUtils.hasText(clientIdStr)) {
            log.error("Missing invoice/client metadata in Stripe webhook event");
            return;
        }

        Long tenantOrganisationId = TenantContext.getOrganisationId();
        if (tenantOrganisationId != null && StringUtils.hasText(organisationIdStr)
                && !Objects.equals(tenantOrganisationId, Long.parseLong(organisationIdStr))) {
            log.error("Stripe webhook organisation mismatch expected={} metadata={}", tenantOrganisationId, organisationIdStr);
            throw new BadRequestException("Stripe webhook organisation metadata mismatch");
        }

        Long invoiceId = Long.parseLong(invoiceIdStr);
        Long clientId = Long.parseLong(clientIdStr);
        Long organisationId = StringUtils.hasText(organisationIdStr) ? Long.parseLong(organisationIdStr) : tenantOrganisationId;

        Long amountTotal = sessionData.get("amount_total") instanceof Number
                ? ((Number) sessionData.get("amount_total")).longValue()
                : null;
        String paymentIntent = sessionData.get("payment_intent") != null ? String.valueOf(sessionData.get("payment_intent")) : null;
        String checkoutSessionId = sessionData.get("id") != null ? String.valueOf(sessionData.get("id")) : null;

        java.math.BigDecimal paymentAmountDecimal = java.math.BigDecimal.valueOf(
                amountTotal != null ? amountTotal / 100.0 : 0
        );

        billingService.applyStripePortalPayment(
                invoiceId,
                clientId,
                organisationId,
                paymentAmountDecimal,
                paymentIntent,
                checkoutSessionId,
                connectedAccountId,
                metadataConnectedAccountId);

        // HIPAA: store client MRN as actor — never customer email from Stripe.
        recordAuditEvent(clientId,
                BillingAuditActions.PAYMENT_COMPLETED, "stripe-webhook", "stripe-webhook", "success",
                "Stripe payment webhook processing", invoiceId,
                amountTotal != null ? String.valueOf(amountTotal / 100.0) : "0",
                checkoutSessionId);
        recordAuditEvent(clientId,
                BillingAuditActions.STRIPE_WEBHOOK_PROCESSED, "stripe-webhook", "stripe-webhook", "success",
                "checkout.session.completed", invoiceId,
                amountTotal != null ? String.valueOf(amountTotal / 100.0) : "0",
                checkoutSessionId);
    }

    private void recordAuditEvent(Long clientId, String action, String ipAddress,
                                  String userAgent, String result, String details, Long invoiceId,
                                  String amount, String stripeSessionId) {
        try {
            auditLogService.record(AuditEventDraft.of(action, "billing")
                    .resourceId(invoiceId)
                    .clientId(clientId)
                    .username(resolveClientMrnForAudit(clientId))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .hipaaRelevant(true)
                    .riskLevel("high")
                    .result(result)
                    .details(details + (stripeSessionId != null ? ", stripeSessionId: " + stripeSessionId : "")
                            + (clientId != null ? ", clientId: " + clientId : "")));
        } catch (Exception e) {
            log.error("Failed to record audit event for Stripe payment: {}", invoiceId, e);
        }
    }

    /**
     * HIPAA: client payment actors must be labeled by MRN only (never email).
     */
    private String resolveClientMrnForAudit(Long clientId) {
        if (clientId == null) {
            return HipaaAuditLabels.clientActorFallback();
        }
        try {
            return clientRepository.findById(clientId)
                    .map(HipaaAuditLabels::clientActor)
                    .filter(StringUtils::hasText)
                    .orElse("client-" + clientId);
        } catch (Exception e) {
            log.debug("Could not resolve MRN for payment audit clientId={}: {}", clientId, e.getMessage());
            return "client-" + clientId;
        }
    }
}
