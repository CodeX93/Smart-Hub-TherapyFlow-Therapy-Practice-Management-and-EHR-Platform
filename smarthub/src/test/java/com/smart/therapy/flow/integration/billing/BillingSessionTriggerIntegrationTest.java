package com.smart.therapy.flow.integration.billing;

import com.smart.therapy.flow.billing.dto.CreateSessionBillingRequest;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRateResult;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.billing.service.InvoicePolicyService;
import com.smart.therapy.flow.common.BaseIntegrationTest;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Billing Session Trigger Integration Tests")
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.default_schema=tenant_test",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BillingSessionTriggerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionBillingRepository sessionBillingRepository;
    @Autowired
    private BillingService billingService;
    @Autowired
    private ServiceRepository serviceRepository;

    @MockBean
    private InvoicePolicyService invoicePolicyService;

    @Autowired
    private com.smart.therapy.flow.organisation.service.TenantSystemOptionSeedService optionSeedService;

    @Autowired
    private com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository featureOverrides;

    @MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    private AuthPrincipal principal;
    private Session session;

    @BeforeEach
    void setUpBilling() {
        TenantContext.clear();
        Organisation org = organisationRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Organisation organisation = TestDataFactory.createTestOrganisation();
                    organisation.setId(null);
                    organisation.setSchemaName("tenant_test");
                    return organisationRepository.save(organisation);
                });
        TenantContext.setOrganisationId(org.getId());
        if (featureOverrides.findByOrganisationIdAndFeatureKey(org.getId(), "BILLING_MODULE").isEmpty()) {
            featureOverrides.save(com.smart.therapy.flow.subscription.entity.OrgFeatureOverride.builder()
                    .organisation(org).featureKey("BILLING_MODULE").enabled(true).build());
        }
        TenantContext.setSchemaName(org.getSchemaName() != null ? org.getSchemaName() : "tenant_test");
        optionSeedService.seedDefaults(org.getId(), TenantContext.getSchemaName());

        var admin = TestDataFactory.createTestAdmin();
        admin.getAuthIdentity().setId(null);
        String runId = java.util.UUID.randomUUID().toString();
        admin.setEmail("admin-" + runId + "@example.com");
        admin.getAuthIdentity().setLoginIdentifier(admin.getEmail());
        admin.getAuthIdentity().setNormalisedLoginIdentifier(admin.getEmail());
        admin.getAuthIdentity().setOrganisation(org);
        admin.setAuthIdentity(authIdentityRepository.save(admin.getAuthIdentity()));
        principal = TestDataFactory.createAuthPrincipal(userRepository.save(admin),
                "ROLE_ADMIN", "SESSION_EDIT", "SESSION_VIEW", "BILLING_MANAGE");
        var therapistFixture = TestDataFactory.createTestTherapist();
        therapistFixture.getAuthIdentity().setId(null);
        therapistFixture.setEmail("therapist-" + runId + "@example.com");
        therapistFixture.getAuthIdentity().setLoginIdentifier(therapistFixture.getEmail());
        therapistFixture.getAuthIdentity().setNormalisedLoginIdentifier(therapistFixture.getEmail());
        therapistFixture.getAuthIdentity().setOrganisation(org);
        therapistFixture.setAuthIdentity(authIdentityRepository.save(therapistFixture.getAuthIdentity()));
        var therapist = userRepository.save(therapistFixture);
        var clientFixture = TestDataFactory.createTestClient(therapist);
        clientFixture.setClientId("QA-" + runId.substring(0, 8));
        var client = clientRepository.save(clientFixture);
        var service = serviceRepository.save(com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("QA-" + runId.substring(0, 8))
                .serviceName("Therapy")
                .baseRate(new BigDecimal("100.00"))
                .duration(60)
                .isActive(true)
                .build());
        session = sessionRepository.save(Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .sessionDate(Instant.now().minusSeconds(60))
                .duration(60)
                .status(SessionStatus.SCHEDULED.getValue())
                .build());
        when(invoicePolicyService.resolveBillingRate(any(), any(), any(), any())).thenReturn(
                InvoicePolicyRateResult.builder()
                        .ratePerUnit(new BigDecimal("100.00"))
                        .policyApplied(false)
                        .build());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Completed session creates billing once")
    void completedSessionCreatesBilling() {
        sessionService.updateSessionStatus(session.getId(), SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1");

        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isPresent();
    }

    @Test
    @DisplayName("No-show session creates billing once")
    void noShowSessionCreatesBilling() {
        sessionService.updateSessionStatus(session.getId(), SessionStatus.NO_SHOW.getValue(), principal, "127.0.0.1");

        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isPresent();
    }

    @Test
    @DisplayName("Duplicate status update does not duplicate billing")
    void duplicateStatusUpdateDoesNotDuplicateBilling() {
        sessionService.updateSessionStatus(session.getId(), SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1");
        assertThatThrownBy(() -> sessionService.updateSessionStatus(
                session.getId(), SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class);

        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isPresent();
        assertThat(sessionBillingRepository.findAll().stream().filter(b -> b.getSession().getId().equals(session.getId())).count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Cancelled session does not create billing")
    void cancelledSessionDoesNotCreateBilling() {
        sessionService.updateSessionStatus(session.getId(), SessionStatus.CANCELLED.getValue(), principal, "127.0.0.1");

        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "no-show"})
    void futureOutcomeIsRejectedWithoutChangingStatusOrCreatingBill(String outcome) {
        session.setSessionDate(Instant.now().plusSeconds(3600));
        sessionRepository.saveAndFlush(session);
        assertThatThrownBy(() -> sessionService.updateSessionStatus(session.getId(), outcome, principal, "127.0.0.1"))
                .hasMessageContaining("scheduled time");
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus()).isEqualTo("scheduled");
        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "no-show"})
    void billingFailureDurablyRestoresScheduled(String outcome) {
        when(invoicePolicyService.resolveBillingRate(any(), any(), any(), any()))
                .thenThrow(new com.smart.therapy.flow.common.exception.BadRequestException("QA billing failure"));
        assertThatThrownBy(() -> sessionService.updateSessionStatus(session.getId(), outcome, principal, "127.0.0.1"))
                .hasMessageContaining("remains scheduled");
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus()).isEqualTo("scheduled");
        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isEmpty();
    }

    @Test
    void billedSessionCannotBeCancelled() {
        sessionService.updateSessionStatus(session.getId(), "completed", principal, "127.0.0.1");
        assertThatThrownBy(() -> sessionService.updateSessionStatus(session.getId(), "cancelled", principal, "127.0.0.1"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus()).isEqualTo("completed");
        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isPresent();
    }

    @Test
    void httpCompletionReturnsSuccessAndBillRemainsReadable() throws Exception {
        Long organisationId = TenantContext.getOrganisationId();
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal);
        // The real filters and authorization checks run with the synthetic test principal.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/sessions/{id}/status", session.getId())
                        .with(auth).contentType("application/json").content("{\"status\":\"completed\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("completed"));
        TenantContext.setSchemaName("tenant_test");
        TenantContext.setOrganisationId(organisationId);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/billing/sessions/{id}/billing", session.getId()).with(auth))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"scheduled", "cancelled"})
    void manualBillingRejectsIneligibleStatus(String status) {
        session.setStatus(status);
        sessionRepository.saveAndFlush(session);
        CreateSessionBillingRequest request = new CreateSessionBillingRequest();
        request.setSessionId(session.getId());
        assertThatThrownBy(() -> billingService.createSessionBilling(request, principal, "127.0.0.1"))
                .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class);
        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "no_show"})
    void manualBillingAcceptsEligibleHistoricalOutcomeOnlyOnce(String status) {
        session.setStatus(status);
        sessionRepository.saveAndFlush(session);
        CreateSessionBillingRequest request = new CreateSessionBillingRequest();
        request.setSessionId(session.getId());
        assertThat(billingService.createSessionBilling(request, principal, "127.0.0.1").getTotalAmount())
                .isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> billingService.createSessionBilling(request, principal, "127.0.0.1"))
                .hasMessageContaining("already exists");
    }

    @Test
    void completedSessionSupportsPartialThenFullPaymentWithoutDoubleCounting() {
        sessionService.updateSessionStatus(session.getId(), "completed", principal, "127.0.0.1");
        Long billingId = sessionBillingRepository.findBySessionId(session.getId()).orElseThrow().getId();
        var request = new com.smart.therapy.flow.billing.dto.RecordPaymentRequest();
        request.setPaymentMethod(com.smart.therapy.flow.billing.enums.PaymentMethod.CASH);
        request.setPaymentSide("client");
        request.setPaymentAmount(new BigDecimal("40.00"));
        assertThat(billingService.recordPayment(billingId, request, principal, "127.0.0.1").getRemainingDue())
                .isEqualByComparingTo("60.00");
        // The endpoint accepts cumulative totals; repeating 40 must not add another 40.
        assertThat(billingService.recordPayment(billingId, request, principal, "127.0.0.1").getRemainingDue())
                .isEqualByComparingTo("60.00");
        request.setPaymentAmount(new BigDecimal("100.00"));
        var paid = billingService.recordPayment(billingId, request, principal, "127.0.0.1");
        assertThat(paid.getRemainingDue()).isEqualByComparingTo("0.00");
        assertThat(paid.getClientPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(paid.getPaymentStatus()).isEqualToIgnoringCase("paid");
    }

    @Test
    @DisplayName("Rescheduling session does not create billing")
    void reschedulingSessionDoesNotCreateBilling() {
        sessionService.updateSessionStatus(session.getId(), SessionStatus.RESCHEDULING.getValue(), principal, "127.0.0.1");

        assertThat(sessionBillingRepository.findBySessionId(session.getId())).isEmpty();
    }

    @Test
    @DisplayName("Manual billing creation requires tenant context")
    void manualBillingRequiresTenant() {
        CreateSessionBillingRequest request = new CreateSessionBillingRequest();
        request.setSessionId(session.getId());

        Long previousOrg = TenantContext.getOrganisationId();
        TenantContext.clear();
        try {
            assertThatThrownBy(() -> billingService.createSessionBilling(request, principal, "127.0.0.1"))
                    .isInstanceOf(com.smart.therapy.flow.common.exception.BadRequestException.class);
        } finally {
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate paymentJdbc;

    @Test
    void splitPaymentsPersistOnlyDeltasAndReplayCreatesNoLedgerRows() {
        Long id = paymentEdgeBill();
        var split = new com.smart.therapy.flow.billing.dto.RecordSplitPaymentRequest();
        split.setClientLeg(paymentLeg("20.00", com.smart.therapy.flow.billing.enums.PaymentMethod.CASH));
        split.setInsuranceLeg(paymentLeg("30.00", com.smart.therapy.flow.billing.enums.PaymentMethod.INSURANCE));
        billingService.recordSplitPayment(id, split, principal, "127.0.0.1");
        assertPaymentLedger(id, "20.00", "30.00", "50.00", "0.00", 2, "BILLED");
        billingService.recordSplitPayment(id, split, principal, "127.0.0.1");
        assertPaymentLedger(id, "20.00", "30.00", "50.00", "0.00", 2, "BILLED");
        split.getInsuranceLeg().setAmount(new BigDecimal("80.00"));
        billingService.recordSplitPayment(id, split, principal, "127.0.0.1");
        assertPaymentLedger(id, "20.00", "80.00", "0.00", "0.00", 3, "PAID");
        assertThat(paymentJdbc.queryForList("SELECT amount FROM tenant_test.payments WHERE session_billing_id=? ORDER BY amount", BigDecimal.class, id))
                .containsExactly(new BigDecimal("20.00"), new BigDecimal("30.00"), new BigDecimal("50.00"));
    }

    @Test
    void removingAndReapplyingDiscountNeverCreatesPayments() {
        Long id = paymentEdgeBill();
        billingService.recordPayment(id, cumulativePayment("40.00", "client"), principal, "127.0.0.1");
        discount(id, "percentage", "10.00");
        assertPaymentLedger(id, "40.00", "0.00", "54.00", "6.00", 1, "BILLED");
        discount(id, "none", null);
        assertPaymentLedger(id, "40.00", "0.00", "60.00", "0.00", 1, "BILLED");
        discount(id, "fixed", "15.00");
        assertPaymentLedger(id, "40.00", "0.00", "45.00", "15.00", 1, "BILLED");
        discount(id, "none", null);
        assertPaymentLedger(id, "40.00", "0.00", "60.00", "0.00", 1, "BILLED");
    }

    @Test
    void removingFullOutstandingDiscountReopensBalanceWithoutManufacturingPayment() {
        Long id = paymentEdgeBill();
        billingService.recordPayment(id, cumulativePayment("40.00", "client"), principal, "127.0.0.1");
        discount(id, "percentage", "100.00");
        assertPaymentLedger(id, "40.00", "0.00", "0.00", "60.00", 1, "PAID");
        discount(id, "none", null);
        assertPaymentLedger(id, "40.00", "0.00", "60.00", "0.00", 1, "BILLED");
    }

    @Test
    void concurrentIdenticalCumulativePaymentsCreateOnePayment() throws Exception {
        Long id = paymentEdgeBill();
        var results = concurrently(
                () -> billingService.recordPayment(id, cumulativePayment("40.00", "client"), principal, "127.0.0.1"),
                () -> billingService.recordPayment(id, cumulativePayment("40.00", "client"), principal, "127.0.0.1"));
        assertThat(results).allMatch(java.util.Objects::isNull);
        assertPaymentLedger(id, "40.00", "0.00", "60.00", "0.00", 1, "BILLED");
    }

    @Test
    void concurrentClientAndInsurancePaymentsPreserveBothLedgerLegs() throws Exception {
        Long id = paymentEdgeBill();
        var results = concurrently(
                () -> billingService.recordPayment(id, cumulativePayment("40.00", "client"), principal, "127.0.0.1"),
                () -> billingService.recordPayment(id, cumulativePayment("60.00", "insurance"), principal, "127.0.0.1"));
        assertThat(results).allMatch(java.util.Objects::isNull);
        assertPaymentLedger(id, "40.00", "60.00", "0.00", "0.00", 2, "PAID");
    }

    @Test
    void concurrentStaleEditsRejectOneWriterWithoutExtraPayment() throws Exception {
        Long id = paymentEdgeBill();
        var first = cumulativePayment("40.00", "client");
        var second = cumulativePayment("60.00", "client");
        first.setExpectedPreviousForSource(BigDecimal.ZERO);
        second.setExpectedPreviousForSource(BigDecimal.ZERO);
        var results = concurrently(
                () -> billingService.recordPayment(id, first, principal, "127.0.0.1"),
                () -> billingService.recordPayment(id, second, principal, "127.0.0.1"));
        assertThat(results.stream().filter(java.util.Objects::isNull).count()).isEqualTo(1);
        assertThat(results.stream().filter(java.util.Objects::nonNull).toList()).singleElement()
                .isInstanceOf(com.smart.therapy.flow.common.exception.ConflictException.class);
        boolean firstWon = results.get(0) == null;
        assertPaymentLedger(id, firstWon ? "40.00" : "60.00", "0.00", firstWon ? "60.00" : "40.00", "0.00", 1, "BILLED");
    }

    private Long paymentEdgeBill() {
        Long orgId = TenantContext.getOrganisationId();
        if (featureOverrides.findByOrganisationIdAndFeatureKey(orgId, "ADVANCED_BILLING").isEmpty()) {
            featureOverrides.save(com.smart.therapy.flow.subscription.entity.OrgFeatureOverride.builder()
                    .organisation(organisationRepository.findById(orgId).orElseThrow())
                    .featureKey("ADVANCED_BILLING").enabled(true).build());
        }
        sessionService.updateSessionStatus(session.getId(), "completed", principal, "127.0.0.1");
        return sessionBillingRepository.findBySessionId(session.getId()).orElseThrow().getId();
    }

    private com.smart.therapy.flow.billing.dto.RecordPaymentRequest cumulativePayment(String amount, String side) {
        var request = new com.smart.therapy.flow.billing.dto.RecordPaymentRequest();
        request.setPaymentAmount(new BigDecimal(amount));
        request.setPaymentSide(side);
        request.setPaymentMethod("insurance".equals(side)
                ? com.smart.therapy.flow.billing.enums.PaymentMethod.INSURANCE
                : com.smart.therapy.flow.billing.enums.PaymentMethod.CASH);
        return request;
    }

    private com.smart.therapy.flow.billing.dto.SplitPaymentLegRequest paymentLeg(
            String amount, com.smart.therapy.flow.billing.enums.PaymentMethod method) {
        var leg = new com.smart.therapy.flow.billing.dto.SplitPaymentLegRequest();
        leg.setAmount(new BigDecimal(amount));
        leg.setPaymentMethod(method);
        return leg;
    }

    private void discount(Long id, String type, String value) {
        var request = new com.smart.therapy.flow.billing.dto.ApplyDiscountRequest();
        request.setDiscountType(type);
        request.setDiscountValue(value == null ? null : new BigDecimal(value));
        billingService.applyDiscount(id, request, principal, "127.0.0.1");
    }

    // Independent reads after service transaction commits verify durable totals and both ledger tables.
    private void assertPaymentLedger(Long id, String client, String insurance, String outstanding,
                                     String discount, int entries, String status) {
        var bill = sessionBillingRepository.findById(id).orElseThrow();
        BigDecimal paid = new BigDecimal(client).add(new BigDecimal(insurance));
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(bill.getClientPaidAmount()).isEqualByComparingTo(client);
        assertThat(bill.getInsurancePaidAmount()).isEqualByComparingTo(insurance);
        assertThat(bill.getPaidAmount()).isEqualByComparingTo(paid);
        assertThat(bill.getOutstandingAmount()).isEqualByComparingTo(outstanding);
        assertThat(bill.getDiscountAmount() == null ? BigDecimal.ZERO : bill.getDiscountAmount()).isEqualByComparingTo(discount);
        assertThat(bill.getBillingStatus().name()).isEqualTo(status);
        assertThat(paymentJdbc.queryForObject("SELECT count(*) FROM tenant_test.payments WHERE session_billing_id=?", Integer.class, id)).isEqualTo(entries);
        assertThat(paymentJdbc.queryForObject("SELECT coalesce(sum(amount),0) FROM tenant_test.payments WHERE session_billing_id=?", BigDecimal.class, id)).isEqualByComparingTo(paid);
        assertThat(paymentJdbc.queryForObject("SELECT count(*) FROM tenant_test.payment_transactions t JOIN tenant_test.payments p ON p.id=t.payment_id WHERE p.session_billing_id=?", Integer.class, id)).isEqualTo(entries);
        assertThat(paymentJdbc.queryForObject("SELECT coalesce(sum(t.amount),0) FROM tenant_test.payment_transactions t JOIN tenant_test.payments p ON p.id=t.payment_id WHERE p.session_billing_id=?", BigDecimal.class, id)).isEqualByComparingTo(paid);
        assertThat(paymentJdbc.queryForObject("SELECT coalesce(sum(amount),0) FROM tenant_test.payments WHERE session_billing_id=? AND payment_source='INSURANCE_PORTAL'", BigDecimal.class, id)).isEqualByComparingTo(insurance);
    }

    private java.util.List<RuntimeException> concurrently(Runnable first, Runnable second) throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        Long orgId = TenantContext.getOrganisationId();
        String schema = TenantContext.getSchemaName();
        java.util.List<java.util.concurrent.Future<RuntimeException>> futures = new java.util.ArrayList<>();
        try {
            for (Runnable work : java.util.List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    TenantContext.setOrganisationId(orgId);
                    TenantContext.setSchemaName(schema);
                    try {
                        ready.countDown();
                        if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Payment start barrier timed out");
                        try { work.run(); return null; }
                        catch (RuntimeException failure) { return failure; }
                    } finally { TenantContext.clear(); }
                }));
            }
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            java.util.List<RuntimeException> results = new java.util.ArrayList<>();
            for (var future : futures) results.add(future.get(30, java.util.concurrent.TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            futures.forEach(future -> future.cancel(true));
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }
}
