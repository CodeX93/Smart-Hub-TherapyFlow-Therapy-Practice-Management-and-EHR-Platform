package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.billing.dto.TenantSubscriptionCheckoutResponse;
import com.smart.therapy.flow.billing.dto.TenantSubscriptionPortalResponse;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.BillingContact;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.repository.BillingContactRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePlatformSubscriptionService {

    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final OrganisationRepository organisationRepository;
    private final BillingContactRepository billingContactRepository;
    private final StripePlatformConfigService stripePlatformConfigService;
    private final PlatformAuditService platformAuditService;

    @Value("${app.frontend.subscription-success-url:https://app.therapyflow.pro/billing/subscription/success}")
    private String subscriptionSuccessUrl;

    @Value("${app.frontend.subscription-cancel-url:https://app.therapyflow.pro/billing/subscription}")
    private String subscriptionCancelUrl;

    @Transactional
    public TenantSubscriptionCheckoutResponse createCheckoutSession(Long organisationId) {
        OrgSubscription subscription = requireCurrentSubscription(organisationId);
        String priceId = requireProviderPriceId(subscription);
        String customerId = ensureStripeCustomer(subscription);

        RequestOptions requestOptions = platformRequestOptions();
        try {
            SessionCreateParams.SubscriptionData.Builder subscriptionData = SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("organisationId", String.valueOf(organisationId))
                    .putMetadata("subscriptionId", String.valueOf(subscription.getId()));
            if (subscription.isTrialing() && subscription.getTrialEndAt() != null
                    && subscription.getTrialEndAt().isAfter(Instant.now())) {
                subscriptionData.setTrialEnd(subscription.getTrialEndAt().getEpochSecond());
            }

            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(subscriptionSuccessUrl)
                    .setCancelUrl(subscriptionCancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("organisationId", String.valueOf(organisationId))
                    .putMetadata("subscriptionId", String.valueOf(subscription.getId()))
                    .setSubscriptionData(subscriptionData.build());

            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params.build(), requestOptions);
            return TenantSubscriptionCheckoutResponse.builder()
                    .checkoutUrl(session.getUrl())
                    .sessionId(session.getId())
                    .build();
        } catch (StripeException ex) {
            log.error("Stripe checkout session failed org={}: {}", organisationId, ex.getMessage());
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE",
                    "Stripe checkout session creation failed");
        }
    }

    @Transactional
    public TenantSubscriptionPortalResponse createBillingPortalSession(Long organisationId) {
        OrgSubscription subscription = requireCurrentSubscription(organisationId);
        if (!StringUtils.hasText(subscription.getProviderCustomerId())) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PROVIDER_CUSTOMER_NOT_CONFIGURED",
                    "Stripe customer is not configured for this organisation");
        }
        RequestOptions requestOptions = platformRequestOptions();
        try {
            com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(subscription.getProviderCustomerId())
                            .setReturnUrl(subscriptionCancelUrl)
                            .build(),
                    requestOptions);
            return TenantSubscriptionPortalResponse.builder()
                    .portalUrl(portalSession.getUrl())
                    .build();
        } catch (StripeException ex) {
            log.error("Stripe billing portal failed org={}: {}", organisationId, ex.getMessage());
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE",
                    "Stripe billing portal session creation failed");
        }
    }

    @Transactional
    public OrgSubscription provisionStripeSubscription(Long organisationId, Long actorAuthId) {
        OrgSubscription subscription = requireCurrentSubscription(organisationId);
        if (StringUtils.hasText(subscription.getProviderSubscriptionId())) {
            return subscription;
        }

        String priceId = requireProviderPriceId(subscription);
        String customerId = ensureStripeCustomer(subscription);
        RequestOptions requestOptions = platformRequestOptions();

        try {
            SubscriptionCreateParams.Builder params = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                    .putMetadata("organisationId", String.valueOf(organisationId))
                    .putMetadata("subscriptionId", String.valueOf(subscription.getId()));

            if (subscription.isTrialing() && subscription.getTrialEndAt() != null
                    && subscription.getTrialEndAt().isAfter(Instant.now())) {
                params.setTrialEnd(subscription.getTrialEndAt().getEpochSecond());
            }

            Subscription stripeSub = Subscription.create(params.build(), requestOptions);
            subscription.setProviderCustomerId(customerId);
            subscription.setProviderSubscriptionId(stripeSub.getId());
            applyStripePeriod(subscription, stripeSub);
            OrgSubscription saved = orgSubscriptionRepository.save(subscription);

            platformAuditService.log(actorAuthId, "STRIPE_SUBSCRIPTION_PROVISIONED", "Organisation",
                    String.valueOf(organisationId),
                    "providerSubscriptionId=" + stripeSub.getId() + ", providerCustomerId=" + customerId);
            return saved;
        } catch (StripeException ex) {
            log.error("Stripe subscription provision failed org={}: {}", organisationId, ex.getMessage());
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE",
                    "Stripe subscription provisioning failed");
        }
    }

    @Transactional
    public void persistProviderIdsFromCheckout(Long organisationId, String customerId, String providerSubscriptionId) {
        if (organisationId == null || !StringUtils.hasText(customerId) || !StringUtils.hasText(providerSubscriptionId)) {
            return;
        }
        OrgSubscription subscription = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
        if (subscription == null) {
            return;
        }
        subscription.setProviderCustomerId(customerId);
        subscription.setProviderSubscriptionId(providerSubscriptionId);
        orgSubscriptionRepository.save(subscription);
        syncStripeSubscriptionPeriod(subscription.getId(), providerSubscriptionId);
    }

    @Transactional
    public void syncStripeSubscriptionPeriod(Long subscriptionId, String providerSubscriptionId) {
        if (subscriptionId == null || !StringUtils.hasText(providerSubscriptionId)) {
            return;
        }
        OrgSubscription subscription = orgSubscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null) {
            return;
        }
        try {
            Subscription stripeSub = Subscription.retrieve(providerSubscriptionId, platformRequestOptions());
            applyStripePeriod(subscription, stripeSub);
            orgSubscriptionRepository.save(subscription);
        } catch (StripeException ex) {
            log.warn("Failed to sync Stripe subscription period subscriptionId={}: {}", subscriptionId, ex.getMessage());
        }
    }

    public void applyStripePeriod(OrgSubscription subscription, Subscription stripeSub) {
        if (subscription == null || stripeSub == null) {
            return;
        }
        Long periodEnd = stripeSub.getCurrentPeriodEnd();
        if (periodEnd != null && periodEnd > 0) {
            subscription.setProviderCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
        }
        String status = stripeSub.getStatus();
        if (StringUtils.hasText(status)) {
            if ("trialing".equalsIgnoreCase(status)) {
                subscription.setStatus("trialing");
            } else if ("active".equalsIgnoreCase(status)) {
                subscription.setStatus("active");
            } else if ("past_due".equalsIgnoreCase(status) || "unpaid".equalsIgnoreCase(status)) {
                subscription.setStatus("past_due");
            } else if ("canceled".equalsIgnoreCase(status)) {
                subscription.setStatus("cancelled");
            }
        }
    }

    private String ensureStripeCustomer(OrgSubscription subscription) {
        if (StringUtils.hasText(subscription.getProviderCustomerId())) {
            return subscription.getProviderCustomerId();
        }
        Organisation organisation = subscription.getOrganisation();
        Long organisationId = organisation != null ? organisation.getId() : null;
        String email = resolveBillingEmail(organisationId);
        String name = organisation != null ? organisation.getName() : "TherapyFlow Customer";

        Map<String, String> metadata = new HashMap<>();
        if (organisationId != null) {
            metadata.put("organisationId", String.valueOf(organisationId));
        }
        metadata.put("subscriptionId", String.valueOf(subscription.getId()));

        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .putAllMetadata(metadata)
                    .build();
            Customer customer = Customer.create(params, platformRequestOptions());
            subscription.setProviderCustomerId(customer.getId());
            orgSubscriptionRepository.save(subscription);
            return customer.getId();
        } catch (StripeException ex) {
            log.error("Stripe customer creation failed org={}: {}", organisationId, ex.getMessage());
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE",
                    "Stripe customer creation failed");
        }
    }

    private String resolveBillingEmail(Long organisationId) {
        if (organisationId != null) {
            List<BillingContact> contacts = billingContactRepository
                    .findByOrganisation_IdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtAsc(organisationId);
            for (BillingContact contact : contacts) {
                if (contact != null && StringUtils.hasText(contact.getEmail())) {
                    return contact.getEmail().trim();
                }
            }
        }
        Organisation org = organisationId != null ? organisationRepository.findById(organisationId).orElse(null) : null;
        if (org != null && StringUtils.hasText(org.getSupportEmail())) {
            return org.getSupportEmail().trim();
        }
        throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "BILLING_EMAIL_REQUIRED",
                "A billing contact email is required before Stripe provisioning");
    }

    private String requireProviderPriceId(OrgSubscription subscription) {
        String priceId = SubscriptionPlanPriceResolver.resolveProviderPriceId(
                subscription.getPlan(), subscription.getBillingCycleAtTime());
        if (!StringUtils.hasText(priceId)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PRICE_ID_MISSING",
                    "Stripe price id is not configured on the subscription plan. Set providerPriceId in super-admin plan catalog.");
        }
        return priceId;
    }

    private OrgSubscription requireCurrentSubscription(Long organisationId) {
        return orgSubscriptionRepository.findCurrentByOrganisationId(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND",
                        "No current subscription found for this organisation"));
    }

    private RequestOptions platformRequestOptions() {
        return RequestOptions.builder()
                .setApiKey(stripePlatformConfigService.requirePlatformSecretKey())
                .build();
    }
}
