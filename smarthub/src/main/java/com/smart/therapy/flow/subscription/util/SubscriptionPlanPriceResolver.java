package com.smart.therapy.flow.subscription.util;

import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

public final class SubscriptionPlanPriceResolver {

    private SubscriptionPlanPriceResolver() {
    }

    public static String resolveProviderPriceId(SubscriptionPlan plan, String billingCycle) {
        if (plan == null) {
            return null;
        }
        String cycle = billingCycle != null ? billingCycle.trim().toLowerCase(Locale.ROOT) : "";
        if ("yearly".equals(cycle) || "annual".equals(cycle)) {
            return normalizePriceId(plan.getProviderPriceIdAnnual());
        }
        return normalizePriceId(plan.getProviderPriceIdMonthly());
    }

    public static BigDecimal resolvePlanPrice(SubscriptionPlan plan, String billingCycle) {
        if (plan == null) {
            return null;
        }
        String cycle = billingCycle != null ? billingCycle.trim().toLowerCase(Locale.ROOT) : "";
        if (("yearly".equals(cycle) || "annual".equals(cycle)) && plan.getAnnualPrice() != null) {
            return plan.getAnnualPrice();
        }
        return plan.getBasePrice();
    }

    public static boolean isProviderManaged(SubscriptionPlan plan, String providerCustomerId, String providerSubscriptionId) {
        return StringUtils.hasText(providerCustomerId) && StringUtils.hasText(providerSubscriptionId);
    }

    private static String normalizePriceId(String priceId) {
        if (!StringUtils.hasText(priceId)) {
            return null;
        }
        return priceId.trim();
    }
}
