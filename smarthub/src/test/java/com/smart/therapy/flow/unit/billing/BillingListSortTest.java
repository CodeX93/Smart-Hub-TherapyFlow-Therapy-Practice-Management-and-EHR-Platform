package com.smart.therapy.flow.unit.billing;

import com.smart.therapy.flow.billing.controller.BillingController;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class BillingListSortTest {

    @Test
    void billingDateSort_usesSameDirectionIdTieBreak() {
        Sort sort = BillingController.resolveBillingListSort("billingDate", "desc");

        assertThat(sort.toList()).hasSize(2);
        assertThat(sort.getOrderFor("billingDate"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        // Newest-first lists: same-day rows follow id DESC (Nisreen → Gerson → Miguel).
        assertThat(sort.getOrderFor("id"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void sessionDateAlias_mapsToBillingDateWithMatchingIdDirection() {
        Sort sort = BillingController.resolveBillingListSort("sessionDate", "desc");

        assertThat(sort.getOrderFor("billingDate")).isNotNull();
        assertThat(sort.getOrderFor("id"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }
}
