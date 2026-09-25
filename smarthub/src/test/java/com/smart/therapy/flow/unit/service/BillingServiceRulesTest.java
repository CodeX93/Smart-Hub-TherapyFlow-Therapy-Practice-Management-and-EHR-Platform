package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.dto.ApplyDiscountRequest;
import com.smart.therapy.flow.billing.dto.RecordPaymentRequest;
import com.smart.therapy.flow.billing.dto.SessionBillingResponse;
import com.smart.therapy.flow.billing.dto.VoidPaymentTransactionRequest;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.PaymentTransaction;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.enums.DiscountType;
import com.smart.therapy.flow.billing.enums.PaymentMethod;
import com.smart.therapy.flow.billing.enums.PaymentSource;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.billing.enums.TransactionType;
import com.smart.therapy.flow.billing.repository.PaymentRepository;
import com.smart.therapy.flow.billing.repository.PaymentTransactionRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingGuard;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.billing.service.InvoicePolicyService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.organisation.service.TenantFeatureService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Rules Unit Tests")
class BillingServiceRulesTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private SessionBillingRepository sessionBillingRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private PermissionChecker permissionChecker;
    @Mock private TenantFeatureService tenantFeatureService;
    @Mock private PracticeConfigurationService practiceConfigurationService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private InvoicePolicyService invoicePolicyService;
    @Mock private ClientInsuranceRepository clientInsuranceRepository;

    @Mock
    private BillingGuard billingGuard;

    @InjectMocks
    private BillingService billingService;

    private AuthPrincipal adminPrincipal;
    private User admin;
    private SessionBilling billing;
    private Session session;
    private Client client;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
        admin = TestDataFactory.createTestAdmin();
        admin.setId(10L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);
        when(currentUserService.requireCurrentUser(adminPrincipal)).thenReturn(admin);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(permissionChecker.hasPermission(adminPrincipal, "BILLING_MANAGE")).thenReturn(true);
        when(tenantFeatureService.isAdvancedBillingEnabled(1L)).thenReturn(true);
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        client = TestDataFactory.createTestClientWithId(42L);
        session = TestDataFactory.createTestSession(client, admin);
        session.setId(25L);

        billing = SessionBilling.builder()
                .session(session)
                .serviceCode("90837")
                .units(1)
                .ratePerUnit(new BigDecimal("100.00"))
                .subtotalAmount(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .paidAmount(BigDecimal.ZERO)
                .clientPaidAmount(BigDecimal.ZERO)
                .insurancePaidAmount(BigDecimal.ZERO)
                .outstandingAmount(new BigDecimal("100.00"))
                .billingStatus(BillingStatus.PENDING)
                .insuranceCovered(false)
                .build();
        billing.setId(10L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Cumulative payment records delta and sets billed status for partial pay")
    void cumulativePaymentPartialSetsBilled() {
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(10L)).thenAnswer(inv ->
                List.of(PaymentTransaction.builder()
                        .amount(new BigDecimal("40.00"))
                        .status(PaymentStatus.PAID)
                        .isVoided(false)
                        .payment(Payment.builder()
                                .paymentMethod(PaymentMethod.CASH)
                                .paymentSource(PaymentSource.MANUAL)
                                .build())
                        .build()));

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setClientId(client.getId());
        request.setPaymentAmount(new BigDecimal("40.00"));
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setPaymentSide("client");

        SessionBillingResponse response = billingService.recordPayment(10L, request, adminPrincipal, "127.0.0.1");

        assertThat(response.getBillingStatus()).isEqualTo("billed");
        assertThat(response.getClientPaidAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("Cumulative payment reaching amount due sets paid status")
    void cumulativePaymentFullSetsPaid() {
        billing.setClientPaidAmount(new BigDecimal("60.00"));
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(10L)).thenAnswer(inv ->
                List.of(
                        PaymentTransaction.builder()
                                .amount(new BigDecimal("60.00"))
                                .status(PaymentStatus.PAID)
                                .isVoided(false)
                                .payment(Payment.builder().paymentMethod(PaymentMethod.CASH).paymentSource(PaymentSource.MANUAL).build())
                                .build(),
                        PaymentTransaction.builder()
                                .amount(new BigDecimal("40.00"))
                                .status(PaymentStatus.PAID)
                                .isVoided(false)
                                .payment(Payment.builder().paymentMethod(PaymentMethod.CASH).paymentSource(PaymentSource.MANUAL).build())
                                .build()));

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setClientId(client.getId());
        request.setPaymentAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setPaymentSide("client");

        SessionBillingResponse response = billingService.recordPayment(10L, request, adminPrincipal, "127.0.0.1");

        assertThat(response.getBillingStatus()).isEqualTo("paid");
    }

    @Test
    @DisplayName("Discount recalculates amount due from outstanding")
    void discountRecalculatesAmountDue() {
        when(sessionBillingRepository.findById(10L)).thenReturn(Optional.of(billing));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplyDiscountRequest request = new ApplyDiscountRequest();
        request.setDiscountType("percentage");
        request.setDiscountValue(new BigDecimal("10"));

        SessionBillingResponse response = billingService.applyDiscount(10L, request, adminPrincipal, "127.0.0.1");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(response.getAmountDue()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("100% discount marks invoice paid with zero outstanding")
    void fullDiscountMarksPaid() {
        when(sessionBillingRepository.findById(10L)).thenReturn(Optional.of(billing));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplyDiscountRequest request = new ApplyDiscountRequest();
        request.setDiscountType("percentage");
        request.setDiscountValue(new BigDecimal("100"));

        SessionBillingResponse response = billingService.applyDiscount(10L, request, adminPrincipal, "127.0.0.1");

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getAmountDue()).isEqualByComparingTo("0.00");
        assertThat(response.getRemainingDue()).isEqualByComparingTo("0.00");
        assertThat(response.getBillingStatus()).isEqualTo("paid");
        assertThat(response.getPaymentStatus()).isEqualTo("paid");
    }

    @Test
    @DisplayName("Discount percentage applies to outstanding after partial payment")
    void discountUsesOutstandingNotTotal() {
        billing.setPaidAmount(new BigDecimal("40.00"));
        billing.setClientPaidAmount(new BigDecimal("40.00"));
        billing.setOutstandingAmount(new BigDecimal("60.00"));
        when(sessionBillingRepository.findById(10L)).thenReturn(Optional.of(billing));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplyDiscountRequest request = new ApplyDiscountRequest();
        request.setDiscountType("percentage");
        request.setDiscountValue(new BigDecimal("50"));

        SessionBillingResponse response = billingService.applyDiscount(10L, request, adminPrincipal, "127.0.0.1");

        // 50% of outstanding $60 = $30 off (not 50% of $100 total)
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("30.00");
        assertThat(response.getAmountDue()).isEqualByComparingTo("70.00");
        assertThat(response.getRemainingDue()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Second discount apply is rejected")
    void rejectsSecondDiscount() {
        billing.setDiscountType(DiscountType.PERCENTAGE);
        billing.setDiscountValue(new BigDecimal("10.00"));
        billing.setDiscountAmount(new BigDecimal("10.00"));
        when(sessionBillingRepository.findById(10L)).thenReturn(Optional.of(billing));

        ApplyDiscountRequest request = new ApplyDiscountRequest();
        request.setDiscountType("percentage");
        request.setDiscountValue(new BigDecimal("5"));

        assertThatThrownBy(() -> billingService.applyDiscount(10L, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been applied");
    }

    @Test
    @DisplayName("Void recomputes totals from non-voided transactions")
    void voidRecomputesTotals() {
        billing.setPaidAmount(new BigDecimal("100.00"));
        billing.setBillingStatus(BillingStatus.PAID);

        Payment payment = Payment.builder()
                .sessionBilling(billing)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentSource(PaymentSource.MANUAL)
                .status(PaymentStatus.PAID)
                .paymentDate(Instant.now())
                .build();
        payment.setId(88L);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(99L)
                .payment(payment)
                .provider(PaymentSource.MANUAL)
                .transactionType(TransactionType.CHARGE)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PAID)
                .isVoided(false)
                .build();

        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(paymentTransactionRepository.findByIdAndBillingIdForUpdate(99L, 10L)).thenReturn(Optional.of(transaction));
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        VoidPaymentTransactionRequest request = VoidPaymentTransactionRequest.builder()
                .voidReason("Duplicate entry")
                .build();

        SessionBillingResponse response = billingService.voidPaymentTransaction(10L, 99L, request, adminPrincipal, "127.0.0.1");

        assertThat(response.getBillingStatus()).isEqualTo("pending");
        assertThat(response.getRemainingDue()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Verified Stripe webhook applies portal payment and marks paid")
    void stripeWebhookAppliesPortalPayment() {
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(sessionBillingRepository.findSessionIdByBillingId(10L)).thenReturn(Optional.of(session.getId()));
        when(sessionRepository.findClientIdBySessionId(session.getId())).thenReturn(Optional.of(client.getId()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.existsByProviderIntentId("pi_new")).thenReturn(false);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(10L)).thenAnswer(inv ->
                List.of(PaymentTransaction.builder()
                        .amount(new BigDecimal("100.00"))
                        .status(PaymentStatus.PAID)
                        .isVoided(false)
                        .payment(Payment.builder()
                                .paymentMethod(PaymentMethod.CREDIT_CARD)
                                .paymentSource(PaymentSource.STRIPE)
                                .build())
                        .build()));

        billingService.applyStripePortalPayment(
                10L, client.getId(), 1L, new BigDecimal("100.00"),
                "pi_new", "cs_test", "acct_tenant", "acct_tenant");

        assertThat(billing.getBillingStatus()).isEqualTo(BillingStatus.PAID);
    }

    @Test
    @DisplayName("Duplicate Stripe payment intent is ignored safely")
    void duplicateStripePaymentIntentIgnored() {
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(sessionBillingRepository.findSessionIdByBillingId(10L)).thenReturn(Optional.of(session.getId()));
        when(sessionRepository.findClientIdBySessionId(session.getId())).thenReturn(Optional.of(client.getId()));
        when(paymentTransactionRepository.existsByProviderIntentId("pi_dup")).thenReturn(true);

        billingService.applyStripePortalPayment(
                10L, client.getId(), 1L, new BigDecimal("100.00"),
                "pi_dup", "cs_test", "acct_tenant", "acct_tenant");

        verify(paymentRepository, never()).save(any());
        assertThat(billing.getBillingStatus()).isEqualTo(BillingStatus.PENDING);
    }

    @Test
    @DisplayName("Stripe webhook connected account mismatch is rejected")
    void stripeWebhookAccountMismatchRejected() {
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(sessionBillingRepository.findSessionIdByBillingId(10L)).thenReturn(Optional.of(session.getId()));
        when(sessionRepository.findClientIdBySessionId(session.getId())).thenReturn(Optional.of(client.getId()));
        org.mockito.Mockito.doThrow(new BadRequestException("Stripe webhook connected account mismatch"))
                .when(billingGuard).assertConnectedAccountMatches("acct_a", "acct_b");

        assertThatThrownBy(() -> billingService.applyStripePortalPayment(
                10L, client.getId(), 1L, new BigDecimal("100.00"),
                "pi_mismatch", "cs_test", "acct_a", "acct_b"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("connected account mismatch");
    }

    @Test
    @DisplayName("Stale expectedPreviousForSource is rejected with conflict")
    void staleExpectedPreviousForSourceRejected() {
        billing.setClientPaidAmount(new BigDecimal("40.00"));
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setClientId(client.getId());
        request.setPaymentAmount(new BigDecimal("50.00"));
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setPaymentSide("client");
        request.setExpectedPreviousForSource(new BigDecimal("0.00"));

        assertThatThrownBy(() -> billingService.recordPayment(10L, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Payment totals changed");
    }

    @Test
    @DisplayName("Payment without invoice ownership is rejected")
    void paymentWithoutInvoiceOwnershipRejected() {
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        org.mockito.Mockito.doThrow(new ForbiddenException("Invoice does not belong to the specified client"))
                .when(billingGuard).assertClientOwnsInvoice(billing, 999L);

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setClientId(999L);
        request.setPaymentAmount(new BigDecimal("50.00"));
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setPaymentSide("client");

        assertThatThrownBy(() -> billingService.recordPayment(10L, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }
}
