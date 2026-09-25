package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.billing.dto.TenantSubscriptionDetailsResponse;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantSubscriptionQueryService {

    private final SubscriptionFeatureService subscriptionFeatureService;
    private final AppFeatureRepository appFeatureRepository;
    private final PlatformSubscriptionInvoiceService platformSubscriptionInvoiceService;

    @Transactional(readOnly = true)
    public TenantSubscriptionDetailsResponse getCurrentTenantSubscriptionDetails() {
        String schema = TenantContext.getSchemaName();
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null || schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_REQUIRED",
                    "Subscription endpoints are tenant-scoped and require a resolved organisation context.");
        }

        OrgSubscription subscription = subscriptionFeatureService.getCurrentSubscription(orgId);
        if (subscription == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND",
                    "No current subscription found for this organisation.");
        }

        Instant now = Instant.now();
        YearMonth period = YearMonth.from(now.atZone(ZoneOffset.UTC));
        Instant periodStart = monthStartUtc(period);
        Instant periodEnd = monthEndExclusiveUtc(period);

        List<TenantSubscriptionDetailsResponse.FeatureEntitlement> features = appFeatureRepository.findAll().stream()
                .filter(feature -> !Boolean.TRUE.equals(feature.getIsDeleted()))
                .sorted(Comparator.comparing(AppFeature::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(feature -> toFeatureEntitlement(orgId, feature, now, periodStart, periodEnd))
                .toList();

        TenantSubscriptionDetailsResponse.FeatureEntitlement auditExport = features.stream()
                .filter(feature -> SubscriptionFeatureService.FEATURE_AUDIT_EXPORT.equalsIgnoreCase(feature.getFeatureCode()))
                .findFirst()
                .orElse(null);

        Organisation organisation = subscription.getOrganisation();
        boolean providerBillingConfigured = SubscriptionPlanPriceResolver.isProviderManaged(
                subscription.getPlan(),
                subscription.getProviderCustomerId(),
                subscription.getProviderSubscriptionId());
        String billingMode = providerBillingConfigured ? "provider_managed" : "manual_or_unconfigured";

        String subStatus = subscription.getStatus() != null ? subscription.getStatus().trim().toLowerCase(Locale.ROOT) : "";
        String orgStatus = organisation != null && organisation.getStatus() != null
                ? organisation.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        boolean pastDue = "past_due".equals(subStatus) || "failed".equals(subStatus);
        boolean cancelled = "cancelled".equals(subStatus);
        boolean accessRestricted = pastDue || cancelled || Set.of("LOCKED", "ARCHIVED", "DELETED").contains(orgStatus);

        Instant currentPeriodEnd = subscription.getProviderCurrentPeriodEnd();
        if (currentPeriodEnd == null && subscription.getTrialEndAt() != null && subscription.isTrialing()) {
            currentPeriodEnd = subscription.getTrialEndAt();
        }

        Invoice openInvoice = platformSubscriptionInvoiceService.findFirstOpenInvoice(orgId);
        Long pendingInvoiceId = openInvoice != null ? openInvoice.getId() : null;
        boolean planHasStripePrice = StringUtils.hasText(
                SubscriptionPlanPriceResolver.resolveProviderPriceId(subscription.getPlan(), subscription.getBillingCycleAtTime()));
        String nextAction = resolveNextAction(subscription, providerBillingConfigured, planHasStripePrice, openInvoice, now);
        boolean canSelfServeRenew = planHasStripePrice && !"contact_support".equals(nextAction);

        TenantSubscriptionDetailsResponse response = new TenantSubscriptionDetailsResponse();
        response.setOrganisationId(orgId);
        response.setSubscriptionId(subscription.getId());
        response.setPlanCode(subscription.getPlan().getCode());
        response.setPlanName(subscription.getPlan().getName());
        response.setPlanStatus(subscription.getPlan().getStatus() != null ? subscription.getPlan().getStatus().name() : null);
        response.setSubscriptionStatus(subscription.getStatus());
        response.setBillingCycle(subscription.getBillingCycleAtTime());
        response.setPriceAtTime(subscription.getPriceAtTime());
        response.setStartAt(subscription.getStartAt());
        response.setEndAt(subscription.getEndAt());
        response.setTrialEndAt(subscription.getTrialEndAt());
        response.setTrialing(subscription.isTrialing());
        response.setUsagePeriod(period.toString());
        response.setFeatures(features);
        response.setAuditExportEnabled(auditExport != null && auditExport.isEnabled());
        response.setAuditExportLimit(auditExport != null ? auditExport.getUsageLimit() : null);
        response.setAuditExportUsage(auditExport != null ? auditExport.getCurrentUsage() : null);
        response.setBillingMode(billingMode);
        response.setProviderBillingConfigured(providerBillingConfigured);
        response.setAccessRestricted(accessRestricted);
        response.setPastDue(pastDue);
        response.setCancelled(cancelled);
        response.setCurrentPeriodEnd(currentPeriodEnd);
        response.setNextAction(nextAction);
        response.setPendingInvoiceId(pendingInvoiceId);
        response.setCanSelfServeRenew(canSelfServeRenew);
        return response;
    }

    private static String resolveNextAction(OrgSubscription subscription,
                                            boolean providerBillingConfigured,
                                            boolean planHasStripePrice,
                                            Invoice openInvoice,
                                            Instant now) {
        String status = subscription.getStatus() != null ? subscription.getStatus().trim().toLowerCase(Locale.ROOT) : "";

        if ("cancelled".equals(status)) {
            return providerBillingConfigured || planHasStripePrice ? "subscribe" : "contact_support";
        }

        if (!providerBillingConfigured) {
            if (pastDueOrExpiredTrial(subscription, status, now)) {
                return planHasStripePrice ? "subscribe" : "contact_support";
            }
            if (openInvoice != null && isOpenInvoice(openInvoice)) {
                return StringUtils.hasText(openInvoice.getProviderInvoiceId()) ? "pay_invoice" : "contact_support";
            }
            if (planHasStripePrice && ("trialing".equals(status) || "active".equals(status) || "past_due".equals(status))) {
                return "subscribe";
            }
            return "none";
        }

        if (openInvoice != null && isOpenInvoice(openInvoice)) {
            return "pay_invoice";
        }
        if ("past_due".equals(status)) {
            return "update_payment_method";
        }
        return "none";
    }

    private static boolean pastDueOrExpiredTrial(OrgSubscription subscription, String status, Instant now) {
        if ("past_due".equals(status) || "failed".equals(status)) {
            return true;
        }
        return "trialing".equals(status)
                && subscription.getTrialEndAt() != null
                && !subscription.getTrialEndAt().isAfter(now);
    }

    private static boolean isOpenInvoice(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.PENDING || invoice.getStatus() == InvoiceStatus.PAST_DUE;
    }

    private TenantSubscriptionDetailsResponse.FeatureEntitlement toFeatureEntitlement(
            Long orgId,
            AppFeature feature,
            Instant now,
            Instant periodStart,
            Instant periodEnd
    ) {
        String featureCode = feature.getCode();
        CoreFeature coreFeature = CoreFeature.fromCode(featureCode);

        boolean enabled = subscriptionFeatureService.isFeatureEnabled(orgId, featureCode, now);
        Integer usageLimit = subscriptionFeatureService.getEffectiveLimit(orgId, featureCode, periodStart);
        long currentUsage = subscriptionFeatureService.getCurrentUsageInPeriod(orgId, featureCode, periodStart, periodEnd);

        return new TenantSubscriptionDetailsResponse.FeatureEntitlement(
                featureCode,
                feature.getName(),
                feature.getDescription(),
                enabled,
                usageLimit,
                currentUsage,
                coreFeature != null,
                coreFeature != null ? coreFeature.getValueType().name() : null
        );
    }

    private static Instant monthStartUtc(YearMonth period) {
        return period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant monthEndExclusiveUtc(YearMonth period) {
        return period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
