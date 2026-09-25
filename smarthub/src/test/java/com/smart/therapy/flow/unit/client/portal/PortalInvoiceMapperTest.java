package com.smart.therapy.flow.unit.client.portal;

import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.client.portal.util.PortalInvoiceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortalInvoiceMapper")
class PortalInvoiceMapperTest {

    @Test
    @DisplayName("Derives paid status from billing status")
    void derivesPaidStatus() {
        SessionBilling billing = SessionBilling.builder()
                .billingStatus(BillingStatus.PAID)
                .totalAmount(new BigDecimal("150.00"))
                .paidAmount(new BigDecimal("150.00"))
                .build();

        assertThat(PortalInvoiceMapper.derivePaymentStatus(billing, new BigDecimal("150.00"), new BigDecimal("150.00")))
                .isEqualTo("paid");
    }

    @Test
    @DisplayName("Derives partial status when paid amount is below total")
    void derivesPartialStatus() {
        SessionBilling billing = SessionBilling.builder()
                .billingStatus(BillingStatus.BILLED)
                .totalAmount(new BigDecimal("150.00"))
                .paidAmount(new BigDecimal("50.00"))
                .build();

        assertThat(PortalInvoiceMapper.derivePaymentStatus(billing, new BigDecimal("50.00"), new BigDecimal("150.00")))
                .isEqualTo("partial");
    }

    @Test
    @DisplayName("Derives unpaid status for billed invoice with no payments")
    void derivesUnpaidStatus() {
        SessionBilling billing = SessionBilling.builder()
                .billingStatus(BillingStatus.BILLED)
                .totalAmount(new BigDecimal("150.00"))
                .paidAmount(BigDecimal.ZERO)
                .build();

        assertThat(PortalInvoiceMapper.derivePaymentStatus(billing, BigDecimal.ZERO, new BigDecimal("150.00")))
                .isEqualTo("unpaid");
    }

    @Test
    @DisplayName("Derives partial status against amountDue after discount")
    void derivesPartialStatusAgainstAmountDue() {
        SessionBilling billing = SessionBilling.builder()
                .billingStatus(BillingStatus.BILLED)
                .totalAmount(new BigDecimal("75.00"))
                .discountAmount(new BigDecimal("15.00"))
                .paidAmount(new BigDecimal("30.00"))
                .build();

        BigDecimal amountDue = PortalInvoiceMapper.calculateAmountDue(billing);
        assertThat(amountDue).isEqualByComparingTo("60.00");
        assertThat(PortalInvoiceMapper.derivePaymentStatus(billing, new BigDecimal("30.00"), amountDue))
                .isEqualTo("partial");
    }
}
