package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubBillingMigrationPlanner.BillingMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.BillingTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePaymentTransactionRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubBillingMigrationPlannerTest {

    private final ClientHubBillingMigrationPlanner planner = new ClientHubBillingMigrationPlanner();

    @Test
    void countsBillingAndPaymentCreatesAndMappedUpdates() {
        BillingMigrationPlan plan = planner.buildPlan(
                inventory(2, 0, 0, 0, 0, 2, 0, 0, 0),
                List.of(
                        billing("10", "100"),
                        billing("11", "101")),
                List.of(
                        payment("20", "10"),
                        payment("21", "11")),
                new BillingTargetState(Set.of("100", "101"), Set.of("11"), Set.of("21")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.billingsWouldCreate()).isEqualTo(1);
        assertThat(plan.billingsWouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.paymentsWouldCreate()).isEqualTo(1);
        assertThat(plan.paymentsWouldUpdateMapped()).isEqualTo(1);
    }

    @Test
    void blocksInvalidRowsAndUnmappedDependencies() {
        BillingMigrationPlan plan = planner.buildPlan(
                inventory(2, 1, 1, 1, 1, 2, 1, 1, 1),
                List.of(
                        new SourceBillingRef("10", null, "", null, null),
                        billing("11", "missing-session")),
                List.of(
                        new SourcePaymentTransactionRef("20", null, "", null),
                        payment("21", "missing-billing")),
                new BillingTargetState(Set.of(), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.billingsBlocked()).isEqualTo(1);
        assertThat(plan.paymentsBlocked()).isEqualTo(1);
        assertThat(plan.blockers())
                .contains("Billing rows missing session_id: 1")
                .contains("Billing rows missing service_code: 1")
                .contains("Billing rows missing rate_per_unit: 1")
                .contains("Billing rows missing total_amount: 1")
                .contains("Payment transaction rows missing session_billing_id: 1")
                .contains("Payment transaction rows missing source: 1")
                .contains("Payment transaction rows missing amount: 1");
        assertThat(plan.warnings())
                .contains("Billing rows skipped because session is not mapped yet: 1")
                .contains("Payment transactions skipped because billing is not mapped yet: 1");
    }

    @Test
    void skipsUnmappedLiveRowsWithoutBlockingExecute() {
        BillingMigrationPlan plan = planner.buildPlan(
                inventory(2, 0, 0, 0, 0, 2, 0, 0, 0),
                List.of(
                        billing("10", "100"),
                        billing("11", "missing-session")),
                List.of(
                        payment("20", "10"),
                        payment("21", "11")),
                new BillingTargetState(Set.of("100"), Set.of(), Set.of()));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.billingsWouldCreate()).isEqualTo(1);
        assertThat(plan.paymentsWouldCreate()).isEqualTo(1);
        assertThat(plan.warnings())
                .contains("Billing rows skipped because session is not mapped yet: 1")
                .contains("Payment transactions skipped because billing is not mapped yet: 1");
    }

    @Test
    void keepsOneBillingPerSessionAndSkipsDuplicates() {
        BillingMigrationPlan plan = planner.buildPlan(
                inventory(3, 0, 0, 0, 0, 2, 0, 0, 0),
                List.of(
                        billing("10", "100"),
                        billing("11", "100"),
                        billing("12", "101")),
                List.of(
                        payment("20", "10"),
                        payment("21", "11")),
                new BillingTargetState(Set.of("100", "101"), Set.of(), Set.of()));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.billingsWouldCreate()).isEqualTo(2);
        assertThat(plan.paymentsWouldCreate()).isEqualTo(1);
        assertThat(plan.warnings())
                .contains("Billing rows skipped because TherapyFlow allows only one billing per session: 1")
                .contains("Payment transactions skipped because billing is not mapped yet: 1");
    }

    private SourceBillingInventory inventory(
            long billings,
            long missingSession,
            long blankServiceCode,
            long missingRate,
            long missingTotal,
            long payments,
            long paymentMissingBilling,
            long paymentBlankSource,
            long paymentMissingAmount) {
        return new SourceBillingInventory(
                billings,
                missingSession,
                blankServiceCode,
                missingRate,
                missingTotal,
                payments,
                paymentMissingBilling,
                paymentBlankSource,
                paymentMissingAmount);
    }

    private SourceBillingRef billing(String legacyBillingPk, String sessionLegacyId) {
        return new SourceBillingRef(
                legacyBillingPk,
                sessionLegacyId,
                "90834",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"));
    }

    private SourcePaymentTransactionRef payment(String legacyPaymentTransactionPk, String billingLegacyId) {
        return new SourcePaymentTransactionRef(
                legacyPaymentTransactionPk,
                billingLegacyId,
                "client",
                new BigDecimal("25.00"));
    }
}
