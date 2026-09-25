package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.billing.entity.*;
import com.smart.therapy.flow.billing.enums.*;
import com.smart.therapy.flow.billing.repository.*;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.common.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class BillingCreditLedgerTest {
    @Test
    void refundableBalanceExcludesVoidedTransactions() {
        var payments = mock(PaymentRepository.class);
        var transactions = mock(PaymentTransactionRepository.class);
        var service = mock(BillingService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "paymentRepository", payments);
        ReflectionTestUtils.setField(service, "paymentTransactionRepository", transactions);
        when(payments.findBySessionBillingId(1L)).thenReturn(List.of(Payment.builder()
                .amount(new BigDecimal("100")).status(PaymentStatus.PAID).build()));
        when(transactions.findBySessionBillingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(PaymentTransaction.builder()
                .amount(new BigDecimal("100")).status(PaymentStatus.PAID).isVoided(true).build()));
        BigDecimal refundable = ReflectionTestUtils.invokeMethod(service, "calculateTotalPaidAmount", 1L);
        assertThat(refundable).isEqualByComparingTo("0");
    }

    @Test
    void transferProducesBalancedLedgerAndCannotSpendCreditTwice() {
        var bills = mock(SessionBillingRepository.class);
        var payments = mock(PaymentRepository.class);
        var transactions = mock(PaymentTransactionRepository.class);
        var service = mock(BillingService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "sessionBillingRepository", bills);
        ReflectionTestUtils.setField(service, "paymentRepository", payments);
        ReflectionTestUtils.setField(service, "paymentTransactionRepository", transactions);
        var client = TestDataFactory.createTestClientWithId(10L);
        var source = SessionBilling.builder().session(TestDataFactory.createTestSession()).totalAmount(new BigDecimal("100"))
                .paidAmount(new BigDecimal("130")).billingStatus(BillingStatus.PAID).build(); source.setId(1L); source.getSession().setClient(client);
        var target = SessionBilling.builder().session(TestDataFactory.createTestSession()).totalAmount(new BigDecimal("50"))
                .paidAmount(BigDecimal.ZERO).billingStatus(BillingStatus.PENDING).build(); target.setId(2L); target.getSession().setClient(client);
        var ledger = new ArrayList<PaymentTransaction>();
        ledger.add(PaymentTransaction.builder().payment(Payment.builder().sessionBilling(source).build())
                .amount(new BigDecimal("130")).status(PaymentStatus.PAID).build());
        when(bills.findByClientIdForUpdate(10L)).thenReturn(List.of(source, target));
        when(payments.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactions.save(any())).thenAnswer(i -> { var t = (PaymentTransaction)i.getArgument(0); ledger.add(t); return t; });
        when(transactions.findBySessionBillingIdOrderByCreatedAtDesc(anyLong())).thenAnswer(i -> ledger.stream()
                .filter(t -> t.getPayment().getSessionBilling().getId().equals(i.getArgument(0))).toList());
        ReflectionTestUtils.invokeMethod(service, "applyAvailableClientCredit", target);
        assertThat(source.getPaidAmount()).isEqualByComparingTo("100");
        assertThat(target.getPaidAmount()).isEqualByComparingTo("30");
        assertThat(target.getOutstandingAmount()).isEqualByComparingTo("20");
        assertThat(ledger).hasSize(3);
        assertThat(ledger.stream().map(PaymentTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("130");
        ReflectionTestUtils.invokeMethod(service, "applyAvailableClientCredit", target);
        assertThat(ledger).hasSize(3);
    }
}
