package com.smart.therapy.flow.unit.util;

import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubscriptionPlanPriceResolver tests")
class SubscriptionPlanPriceResolverTest {

    @Test
    @DisplayName("Resolves monthly and annual provider price ids")
    void shouldResolveProviderPriceIds() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setProviderPriceIdMonthly("price_monthly");
        plan.setProviderPriceIdAnnual("price_annual");
        plan.setBasePrice(new BigDecimal("99.00"));
        plan.setAnnualPrice(new BigDecimal("990.00"));

        assertThat(SubscriptionPlanPriceResolver.resolveProviderPriceId(plan, "monthly")).isEqualTo("price_monthly");
        assertThat(SubscriptionPlanPriceResolver.resolveProviderPriceId(plan, "annual")).isEqualTo("price_annual");
        assertThat(SubscriptionPlanPriceResolver.resolvePlanPrice(plan, "yearly")).isEqualByComparingTo("990.00");
    }

    @Test
    @DisplayName("Detects provider-managed subscriptions")
    void shouldDetectProviderManaged() {
        assertThat(SubscriptionPlanPriceResolver.isProviderManaged(null, "cus_1", "sub_1")).isTrue();
        assertThat(SubscriptionPlanPriceResolver.isProviderManaged(null, "cus_1", null)).isFalse();
    }
}
