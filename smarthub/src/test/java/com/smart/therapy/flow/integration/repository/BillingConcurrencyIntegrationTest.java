package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.common.*;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.billing.entity.*;
import com.smart.therapy.flow.billing.enums.*;
import com.smart.therapy.flow.billing.repository.*;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BillingConcurrencyIntegrationTest extends BaseTenantRepositoryTest {
    @Autowired TestEntityManager fixture;
    @PersistenceContext EntityManager em;
    @Autowired SessionBillingRepository bills;
    @Autowired SessionRepository sessions;
    @Autowired PaymentRepository payments;
    @Autowired PaymentTransactionRepository transactions;
    @Autowired PlatformTransactionManager manager;

    private com.smart.therapy.flow.auth.entity.User therapist;
    @org.junit.jupiter.api.BeforeEach
    void identity() { therapist = persistTherapist(); }

    @Test
    void concurrentCreditAndCreationPreserveSingleSpendAndSingleBill() throws Exception {
        Long organisation = TenantContext.getOrganisationId();
        var client = fixture.persistAndFlush(TestDataFactory.createTestClient(therapist));
        var catalog = fixture.persistAndFlush(com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("concurrent").serviceName("Concurrent fixture").baseRate(BigDecimal.TEN).duration(60).build());
        var sourceSession = TestDataFactory.createTestSession(client, therapist); sourceSession.setService(catalog); fixture.persistAndFlush(sourceSession);
        var a = TestDataFactory.createTestSession(client, therapist); a.setService(catalog); fixture.persistAndFlush(a);
        var b = TestDataFactory.createTestSession(client, therapist); b.setService(catalog); fixture.persistAndFlush(b);
        var c = TestDataFactory.createTestSession(client, therapist); c.setService(catalog); fixture.persistAndFlush(c);
        var source = fixture.persistAndFlush(bill(sourceSession, "100", "130"));
        var first = fixture.persistAndFlush(bill(a, "50", "0"));
        var second = fixture.persistAndFlush(bill(b, "50", "0"));
        var payment = fixture.persistAndFlush(Payment.builder().sessionBilling(source).amount(new BigDecimal("130"))
                .paymentMethod(PaymentMethod.CASH).paymentSource(PaymentSource.MANUAL).status(PaymentStatus.PAID).build());
        fixture.persistAndFlush(PaymentTransaction.builder().payment(payment).amount(new BigDecimal("130"))
                .provider(PaymentSource.MANUAL).transactionType(TransactionType.CHARGE).status(PaymentStatus.PAID).build());
        Long sourceId = source.getId(), firstId = first.getId(), secondId = second.getId(), sessionId = c.getId();
        TestTransaction.flagForCommit(); TestTransaction.end();
        var service = mock(BillingService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "sessionBillingRepository", bills);
        ReflectionTestUtils.setField(service, "paymentRepository", payments);
        ReflectionTestUtils.setField(service, "paymentTransactionRepository", transactions);
        var pool = Executors.newFixedThreadPool(2);
        try {
            overlap(pool, organisation,
                () -> ReflectionTestUtils.invokeMethod(service, "applyAvailableClientCredit", bills.findById(firstId).orElseThrow()),
                () -> ReflectionTestUtils.invokeMethod(service, "applyAvailableClientCredit", bills.findById(secondId).orElseThrow()));
            inTransaction(organisation, () -> {
                assertThat(bills.findById(sourceId).orElseThrow().getPaidAmount()).isEqualByComparingTo("100");
                assertThat(bills.findById(firstId).orElseThrow().getPaidAmount()
                        .add(bills.findById(secondId).orElseThrow().getPaidAmount())).isEqualByComparingTo("30");
                assertThat(transactions.findBySessionBillingIdOrderByCreatedAtDesc(sourceId)).hasSize(2);
            });
            overlap(pool, organisation, () -> {
                var locked = sessions.findByIdForBilling(sessionId).orElseThrow();
                assertThat(bills.findBySessionId(sessionId)).isEmpty();
                bills.saveAndFlush(bill(locked, "50", "0"));
            }, () -> {
                sessions.findByIdForBilling(sessionId).orElseThrow();
                assertThat(bills.findBySessionId(sessionId)).isPresent();
            });
        } finally { pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); TenantContext.clear(); }
    }

    private SessionBilling bill(Session session, String total, String paid) {
        return SessionBilling.builder().session(session).serviceCode("concurrent").ratePerUnit(new BigDecimal(total))
                .subtotalAmount(new BigDecimal(total)).totalAmount(new BigDecimal(total)).paidAmount(new BigDecimal(paid))
                .outstandingAmount(new BigDecimal(total).subtract(new BigDecimal(paid)).max(BigDecimal.ZERO))
                .billingStatus(new BigDecimal(paid).signum() > 0 ? BillingStatus.PAID : BillingStatus.PENDING).build();
    }

    private void inTransaction(Long organisation, Runnable work) {
        TenantContext.setSchemaName(SCHEMA); TenantContext.setOrganisationId(organisation);
        try { new TransactionTemplate(manager).executeWithoutResult(status -> {work.run(); em.flush();}); }
        finally {TenantContext.clear();}
    }

    private void overlap(ExecutorService pool, Long organisation, Runnable first, Runnable second) throws Exception {
        var held = new CountDownLatch(1); var release = new CountDownLatch(1); var started = new CountDownLatch(1);
        Future<?> one = pool.submit(() -> inTransaction(organisation, () -> {
            first.run(); em.flush(); held.countDown();
            try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Lock holder timed out"); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
        }));
        try {
            assertThat(held.await(15, TimeUnit.SECONDS)).isTrue();
            Future<?> two = pool.submit(() -> inTransaction(organisation, () -> {started.countDown(); second.run();}));
            assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
            try { two.get(300, TimeUnit.MILLISECONDS); throw new AssertionError("Second transaction did not wait for lock"); }
            catch (TimeoutException expected) { /* Held until first transaction commits. */ }
            release.countDown(); one.get(15, TimeUnit.SECONDS); two.get(15, TimeUnit.SECONDS);
        } finally {release.countDown();}
    }
}
