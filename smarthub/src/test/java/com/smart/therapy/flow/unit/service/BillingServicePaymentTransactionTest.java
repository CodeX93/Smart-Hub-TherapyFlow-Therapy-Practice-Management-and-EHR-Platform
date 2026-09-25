package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.dto.SessionBillingResponse;
import com.smart.therapy.flow.billing.dto.VoidPaymentTransactionRequest;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.PaymentTransaction;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
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
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.organisation.service.TenantFeatureService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService Payment Transaction Unit Tests")
class BillingServicePaymentTransactionTest {

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private SessionBillingRepository sessionBillingRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private TenantFeatureService tenantFeatureService;
    @Mock
    private PracticeConfigurationService practiceConfigurationService;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private InvoicePolicyService invoicePolicyService;

    @Mock
    private BillingGuard billingGuard;

    @Mock
    private ClientReportAccessService clientReportAccessService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BillingService billingService;

    private AuthPrincipal actorPrincipal;
    private User actor;
    private SessionBilling billing;
    private Payment payment;
    private PaymentTransaction transaction;

    @BeforeEach
    void setUp() {
        actor = TestDataFactory.createTestAdmin();
        actor.setId(77L);
        actorPrincipal = TestDataFactory.createAuthPrincipal(actor);

        Client client = TestDataFactory.createTestClientWithId(42L);
        Session session = TestDataFactory.createTestSession(client, actor);
        session.setId(25L);

        billing = SessionBilling.builder()
                .session(session)
                .serviceCode("90837")
                .units(1)
                .ratePerUnit(new BigDecimal("150.00"))
                .subtotalAmount(new BigDecimal("150.00"))
                .totalAmount(new BigDecimal("150.00"))
                .paidAmount(new BigDecimal("150.00"))
                .outstandingAmount(BigDecimal.ZERO)
                .billingStatus(BillingStatus.PAID)
                .insuranceCovered(false)
                .build();
        billing.setId(10L);

        payment = Payment.builder()
                .sessionBilling(billing)
                .amount(new BigDecimal("150.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentSource(PaymentSource.MANUAL)
                .status(PaymentStatus.PAID)
                .paymentDate(Instant.now())
                .reference("PMT-1")
                .build();
        payment.setId(88L);

        transaction = PaymentTransaction.builder()
                .id(99L)
                .payment(payment)
                .provider(PaymentSource.MANUAL)
                .transactionType(TransactionType.CHARGE)
                .amount(new BigDecimal("150.00"))
                .status(PaymentStatus.PAID)
                .build();
    }

    @Test
    @DisplayName("Should void payment transaction and recalculate billing totals")
    void shouldVoidPaymentTransactionAndRecalculateBillingTotals() {
        // Arrange
        VoidPaymentTransactionRequest request = VoidPaymentTransactionRequest.builder()
                .voidReason("Duplicate manual entry")
                .build();

        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(paymentTransactionRepository.findByIdAndBillingIdForUpdate(99L, 10L)).thenReturn(Optional.of(transaction));
        when(currentUserService.requireCurrentUser(actorPrincipal)).thenReturn(actor);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SessionBillingResponse response = billingService.voidPaymentTransaction(
                10L,
                99L,
                request,
                actorPrincipal,
                "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBillingStatus()).isEqualTo("pending");
        assertThat(response.getAmountDue()).isEqualByComparingTo("150.00");
        assertThat(response.getRemainingDue()).isEqualByComparingTo("150.00");
        assertThat(response.getClientPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getInsurancePaidAmount()).isEqualByComparingTo("0.00");

        assertThat(transaction.getIsVoided()).isTrue();
        assertThat(transaction.getVoidedBy()).isEqualTo(77L);
        assertThat(transaction.getVoidReason()).isEqualTo("Duplicate manual entry");
        assertThat(transaction.getVoidedAt()).isNotNull();
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.FAILED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getNotes()).contains("Voided transaction #99");

        verify(paymentTransactionRepository).save(transaction);
        verify(paymentRepository).save(payment);
        verify(sessionBillingRepository).save(billing);
    }

    @Test
    @DisplayName("Should reject void when transaction already voided")
    void shouldRejectVoidWhenTransactionAlreadyVoided() {
        // Arrange
        transaction.setIsVoided(true);
        VoidPaymentTransactionRequest request = VoidPaymentTransactionRequest.builder()
                .voidReason("Duplicate manual entry")
                .build();

        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(billing));
        when(paymentTransactionRepository.findByIdAndBillingIdForUpdate(99L, 10L)).thenReturn(Optional.of(transaction));

        // Act / Assert
        assertThatThrownBy(() -> billingService.voidPaymentTransaction(
                10L,
                99L,
                request,
                actorPrincipal,
                "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already voided");

        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(sessionBillingRepository, never()).save(any(SessionBilling.class));
    }
}
