package com.smart.therapy.flow.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookApplicationService {

    private static final String WEBHOOK_CONTEXT_PLATFORM = "platform";

    private final StripeSubscriptionWebhookService stripeSubscriptionWebhookService;
    private final StripeWebhookEventService stripeWebhookEventService;
    private final StripePlatformConfigService stripePlatformConfigService;
    private final StripeTenantConfigService stripeTenantConfigService;
    private final OrgStripeConnectService orgStripeConnectService;
    private final StripeService stripeService;
    private final PlatformAuditService platformAuditService;
    private final ObjectMapper objectMapper;

    public ResponseEntity<Object> handlePlatformWebhook(String payload, String signature) {
        return handleWebhookInternal(payload, signature, WEBHOOK_CONTEXT_PLATFORM,
                stripePlatformConfigService.requirePlatformWebhookSecret(), true);
    }

    public ResponseEntity<Object> handleConnectWebhook(String payload, String signature) {
        return handleWebhookInternal(payload, signature, "connect",
                stripePlatformConfigService.requireConnectWebhookSecret(), false);
    }

    public ResponseEntity<Object> handleTenantWebhook(String tenantKey, String payload, String signature) {
        Organisation org = stripeTenantConfigService.requireBySlugOrSubdomain(tenantKey);
        return handleTenantWebhookInternal(org, payload, signature);
    }

    private ResponseEntity<Object> handleWebhookInternal(String payload, String signature, String defaultContext,
                                                         String secret, boolean platformWebhook) {
        String claimedEventId = null;
        String claimedContextKey = null;
        try {
            if (!StringUtils.hasText(signature)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing Stripe-Signature header"));
            }
            Event event = Webhook.constructEvent(payload, signature, secret);
            String eventId = event.getId();
            String eventType = event.getType();
            if (!StringUtils.hasText(eventId) || !StringUtils.hasText(eventType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook event envelope"));
            }

            String contextKey = resolveContextKey(defaultContext, event);
            boolean claimed = stripeWebhookEventService.tryClaimEvent(eventId, eventType, contextKey);
            if (!claimed) {
                return ResponseEntity.ok(Map.of("status", "duplicate_ignored"));
            }
            claimedEventId = eventId;
            claimedContextKey = contextKey;

            Map<String, Object> payloadMap = objectMapper.readValue(payload, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = payloadMap.get("data") instanceof Map
                    ? (Map<String, Object>) payloadMap.get("data")
                    : null;
            if (eventData == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook payload"));
            }

            if (platformWebhook) {
                boolean lifecycleHandled = stripeSubscriptionWebhookService.handleLifecycleEvent(eventType, eventData, eventId);
                if (!lifecycleHandled) {
                    log.debug("Ignoring non-lifecycle platform webhook event type={}", eventType);
                }
                platformAuditService.log(null, "STRIPE_PLATFORM_WEBHOOK_" + sanitize(eventType),
                        "StripeWebhook", eventId, "context=" + contextKey);
            } else {
                if (eventType.startsWith("account.") || eventType.startsWith("capability.")) {
                    orgStripeConnectService.handleConnectAccountEvent(eventType, event.getAccount(), eventData);
                } else {
                    String connectAccountId = event.getAccount();
                    if (!StringUtils.hasText(connectAccountId)) {
                        log.warn("Connect webhook event without account context: type={}", eventType);
                    } else {
                        orgStripeConnectService.runInTenantContextForAccount(connectAccountId, () ->
                                stripeService.handleConnectedAccountWebhookEvent(eventData, eventType, connectAccountId)
                        );
                    }
                }
                platformAuditService.log(null, "STRIPE_CONNECT_WEBHOOK_" + sanitize(eventType),
                        "StripeWebhook", eventId, "context=" + contextKey);
            }
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook due to invalid signature");
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Stripe signature"));
        } catch (Exception e) {
            if (claimedEventId != null && claimedContextKey != null) {
                stripeWebhookEventService.releaseClaim(claimedEventId, claimedContextKey);
            }
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(500).body(Map.of("error", "Webhook processing failed"));
        }
    }

    private String resolveContextKey(String defaultContext, Event event) {
        if (WEBHOOK_CONTEXT_PLATFORM.equals(defaultContext)) {
            return WEBHOOK_CONTEXT_PLATFORM;
        }
        if (StringUtils.hasText(event.getAccount())) {
            return "connect:" + event.getAccount();
        }
        return defaultContext;
    }

    private static String sanitize(String eventType) {
        return eventType == null ? "UNKNOWN" : eventType.toUpperCase().replace('.', '_');
    }

    private ResponseEntity<Object> handleTenantWebhookInternal(Organisation org, String payload, String signature) {
        String claimedEventId = null;
        String claimedContextKey = null;
        String contextKey = "tenant:" + org.getId();
        try {
            if (!StringUtils.hasText(signature)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing Stripe-Signature header"));
            }
            String webhookSecret = stripeTenantConfigService.requireTenantWebhookSecret(org.getId());
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            String eventId = event.getId();
            String eventType = event.getType();
            if (!StringUtils.hasText(eventId) || !StringUtils.hasText(eventType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook event envelope"));
            }

            boolean claimed = stripeWebhookEventService.tryClaimEvent(eventId, eventType, contextKey);
            if (!claimed) {
                return ResponseEntity.ok(Map.of("status", "duplicate_ignored"));
            }
            claimedEventId = eventId;
            claimedContextKey = contextKey;

            Map<String, Object> payloadMap = objectMapper.readValue(payload, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = payloadMap.get("data") instanceof Map
                    ? (Map<String, Object>) payloadMap.get("data")
                    : null;
            if (eventData == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook payload"));
            }

            boolean lifecycleHandled = stripeSubscriptionWebhookService.handleLifecycleEvent(eventType, eventData, eventId);
            if (!lifecycleHandled) {
                orgStripeConnectService.runInTenantContextForOrganisation(org.getId(), () ->
                        stripeService.handleConnectedAccountWebhookEvent(eventData, eventType, null)
                );
            }

            platformAuditService.log(null, "STRIPE_TENANT_WEBHOOK_" + sanitize(eventType),
                    "StripeWebhook", eventId, "context=" + contextKey + ",orgId=" + org.getId());
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (SignatureVerificationException e) {
            log.warn("Rejected tenant Stripe webhook due to invalid signature: tenant={}", org.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Stripe signature"));
        } catch (Exception e) {
            if (claimedEventId != null && claimedContextKey != null) {
                stripeWebhookEventService.releaseClaim(claimedEventId, claimedContextKey);
            }
            log.error("Error processing tenant Stripe webhook tenant={}", org.getId(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Webhook processing failed"));
        }
    }
}
