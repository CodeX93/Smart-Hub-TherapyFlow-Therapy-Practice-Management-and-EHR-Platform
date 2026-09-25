package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.dto.ApplyDiscountRequest;
import com.smart.therapy.flow.billing.dto.CreateServiceRequest;
import com.smart.therapy.flow.billing.dto.CreateSessionBillingRequest;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRateResult;
import com.smart.therapy.flow.billing.dto.ServiceResponse;
import com.smart.therapy.flow.billing.dto.SessionBillingResponse;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.PaymentTransaction;
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
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.service.TenantFeatureService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Unit Tests")
class BillingServiceTest {

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
    private ClientInsuranceRepository clientInsuranceRepository;

    @Mock
    private BillingGuard billingGuard;

    @InjectMocks
    private BillingService billingService;

    private AuthPrincipal adminPrincipal;
    private com.smart.therapy.flow.billing.entity.Service service;
    private Session session;
    private SessionBilling sessionBilling;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
        User admin = TestDataFactory.createTestAdmin();
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(admin);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantFeatureService.isAdvancedBillingEnabled(1L)).thenReturn(true);
        when(permissionChecker.hasPermission(any(), eq("BILLING_MANAGE"))).thenReturn(true);
        when(invoicePolicyService.resolveBillingRate(any(), any(), any(), any())).thenReturn(
                InvoicePolicyRateResult.builder()
                        .ratePerUnit(new BigDecimal("150.00"))
                        .policyApplied(false)
                        .build());
        when(sessionBillingRepository.findBySessionId(any())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(com.smart.therapy.flow.billing.entity.Service.class))).thenAnswer(inv -> {
            com.smart.therapy.flow.billing.entity.Service saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });

        service = com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("90837")
                .serviceName("Psychotherapy 60 min")
                .baseRate(new BigDecimal("150.00"))
                .duration(60)
                .isActive(true)
                .therapistVisible(true)
                .clientPortalVisible(true)
                .build();
        service.setId(1L);

        session = TestDataFactory.createTestSession();
        session.setId(1L);
        session.setStatus("completed");
        session.setSessionDate(Instant.now().minusSeconds(60));

        sessionBilling = SessionBilling.builder()
                .session(session)
                .serviceCode("90837")
                .totalAmount(new BigDecimal("150.00"))
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void billingResponsesIncludeClientReferenceAndHandleMissingReferral() {
        var referral = new com.smart.therapy.flow.client.entity.ClientReferral();
        referral.setReferenceNumber("REF-123");
        session.setClient(new com.smart.therapy.flow.client.entity.Client());
        session.getClient().setReferral(referral);
        SessionBilling record = new SessionBilling();
        record.setSession(session);
        record.setTotalAmount(new BigDecimal("100.00"));
        for (String method : List.of("toSessionBillingResponse", "toBillingHistoryResponse")) {
            Object response = org.springframework.test.util.ReflectionTestUtils.invokeMethod(billingService, method, record);
            assertThat(org.springframework.test.util.ReflectionTestUtils.getField(response, "clientReferenceNumber"))
                    .isEqualTo("REF-123");
        }
        session.getClient().setReferral(null);
        for (String method : List.of("toSessionBillingResponse", "toBillingHistoryResponse")) {
            Object response = org.springframework.test.util.ReflectionTestUtils.invokeMethod(billingService, method, record);
            assertThat(org.springframework.test.util.ReflectionTestUtils.getField(response, "clientReferenceNumber")).isNull();
        }
    }

    @Test
    @DisplayName("Should create service successfully")
    void shouldCreateServiceSuccessfully() {
        // Arrange
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceCode("90837");
        request.setServiceName("Psychotherapy 60 min");
        request.setBaseRate(new BigDecimal("150.00"));
        request.setDurationInMinutes(60);
        request.setIsActive(true);

        when(serviceRepository.findByServiceCode("90837")).thenReturn(Optional.empty());
        when(serviceRepository.save(any(com.smart.therapy.flow.billing.entity.Service.class)))
                .thenReturn(service);
        when(auditLogRepository.save(any())).thenReturn(null);

        // Act
        ServiceResponse response = billingService.createService(request, adminPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getServiceCode()).isEqualTo("90837");
        assertThat(response.getServiceName()).isEqualTo("Psychotherapy 60 min");
        verify(serviceRepository).findByServiceCode("90837");
        verify(serviceRepository).save(any(com.smart.therapy.flow.billing.entity.Service.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when service code already exists")
    void shouldThrowExceptionWhenServiceCodeExists() {
        // Arrange
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceCode("90837");
        request.setServiceName("Existing service");

        when(serviceRepository.findByServiceCode("90837")).thenReturn(Optional.of(service));

        // Act & Assert
        assertThatThrownBy(() -> billingService.createService(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Service code already exists");

        verify(serviceRepository, never()).save(any(com.smart.therapy.flow.billing.entity.Service.class));
    }

    @Test
    @DisplayName("Should get service by ID successfully")
    void shouldGetServiceByIdSuccessfully() {
        // Arrange
        Long serviceId = 1L;
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

        // Act
        ServiceResponse response = billingService.getService(serviceId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(serviceId);
        assertThat(response.getServiceCode()).isEqualTo("90837");
        verify(serviceRepository).findById(serviceId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when service not found")
    void shouldThrowExceptionWhenServiceNotFound() {
        // Arrange
        Long serviceId = 999L;
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> billingService.getService(serviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");

        verify(serviceRepository).findById(serviceId);
    }

    @Test
    @DisplayName("Should get all services")
    void shouldGetAllServices() {
        // Arrange
        when(serviceRepository.findAll()).thenReturn(List.of(service));

        // Act
        List<ServiceResponse> services = billingService.getServices(false, null, null);

        // Assert
        assertThat(services).isNotNull();
        assertThat(services).hasSize(1);
        assertThat(services.get(0).getServiceCode()).isEqualTo("90837");
    }

    @Test
    @DisplayName("Should get only active services")
    void shouldGetOnlyActiveServices() {
        // Arrange
        when(serviceRepository.findAll()).thenReturn(List.of(service));

        // Act
        List<ServiceResponse> services = billingService.getServices(true, null, null);

        // Assert
        assertThat(services).isNotNull();
        assertThat(services).hasSize(1);
        verify(serviceRepository).findAll();
        verify(serviceRepository, never()).findByIsActive(true);
    }

    @Test
    @DisplayName("Should create session billing successfully")
    void shouldCreateSessionBillingSuccessfully() {
        // Arrange
        CreateSessionBillingRequest request = new CreateSessionBillingRequest();
        request.setSessionId(1L);
        request.setServiceCode("90837");
        request.setUnitRate(new BigDecimal("150.00"));

        when(sessionRepository.findByIdForBilling(1L)).thenReturn(Optional.of(session));
        when(serviceRepository.findByServiceCode("90837")).thenReturn(Optional.of(service));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> {
            SessionBilling saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        SessionBillingResponse response = billingService.createSessionBilling(request, adminPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(1L);
        assertThat(response.getServiceCode()).isEqualTo("90837");
        verify(sessionRepository).findByIdForBilling(1L);
        verify(sessionBillingRepository).save(any(SessionBilling.class));
    }

    @Test
    @DisplayName("Should derive billing and policy dates in the Administration timezone")
    void shouldUsePracticeTimezoneForBillingDate() {
        CreateSessionBillingRequest request = new CreateSessionBillingRequest();
        request.setSessionId(1L);
        request.setServiceCode("90837");
        request.setUnitRate(new BigDecimal("150.00"));
        request.setBillingDate(Instant.parse("2026-09-11T02:30:00Z"));

        when(practiceConfigurationService.getPracticeConfiguration()).thenReturn(
                PracticeConfigurationResponse.builder().timezone("America/Toronto").build());
        when(sessionRepository.findByIdForBilling(1L)).thenReturn(Optional.of(session));
        when(serviceRepository.findByServiceCode("90837")).thenReturn(Optional.of(service));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenAnswer(inv -> {
            SessionBilling saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        billingService.createSessionBilling(request, adminPrincipal, "127.0.0.1");

        verify(invoicePolicyService).resolveBillingRate(
                eq(session),
                eq(new BigDecimal("150.00")),
                eq(1L),
                eq(LocalDate.of(2026, 9, 10)));
        verify(sessionBillingRepository).save(argThat(saved ->
                LocalDate.of(2026, 9, 10).equals(saved.getBillingDate())));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when session not found for billing")
    void shouldThrowExceptionWhenSessionNotFoundForBilling() {
        // Arrange
        CreateSessionBillingRequest request = new CreateSessionBillingRequest();
        request.setSessionId(999L);

        when(sessionRepository.findByIdForBilling(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> billingService.createSessionBilling(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");

        verify(sessionBillingRepository, never()).save(any(SessionBilling.class));
    }

    @Test
    @DisplayName("Should apply discount successfully")
    void shouldApplyDiscountSuccessfully() {
        // Arrange
        Long billingId = 1L;
        ApplyDiscountRequest request = new ApplyDiscountRequest();
        request.setDiscountAmount(new BigDecimal("25.00"));
        request.setDiscountType("percentage");
        request.setDiscountValue(new BigDecimal("10.00"));

        sessionBilling.setTotalAmount(new BigDecimal("150.00"));
        sessionBilling.setDiscountAmount(BigDecimal.ZERO);
        sessionBilling.setSession(session);
        sessionBilling.setSubtotalAmount(new BigDecimal("150.00"));
        sessionBilling.setRatePerUnit(new BigDecimal("150.00"));
        sessionBilling.setUnits(1);
        sessionBilling.setBillingStatus(com.smart.therapy.flow.billing.enums.BillingStatus.PENDING);

        when(sessionBillingRepository.findById(billingId)).thenReturn(Optional.of(sessionBilling));
        when(sessionBillingRepository.save(any(SessionBilling.class))).thenReturn(sessionBilling);
        when(auditLogRepository.save(any())).thenReturn(null);

        // Act
        SessionBillingResponse response = billingService.applyDiscount(billingId, request, adminPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(sessionBillingRepository).findById(billingId);
        verify(sessionBillingRepository).save(any(SessionBilling.class));
    }

    @Test
    @DisplayName("Should reject discount percentage above configured maximum")
    void shouldRejectDiscountPercentageAboveMaximum() {
        Long billingId = 1L;
        ApplyDiscountRequest request = new ApplyDiscountRequest();
        request.setDiscountType("percentage");
        request.setDiscountValue(new BigDecimal("150.00"));

        sessionBilling.setTotalAmount(new BigDecimal("150.00"));

        when(sessionBillingRepository.findById(billingId)).thenReturn(Optional.of(sessionBilling));

        assertThatThrownBy(() -> billingService.applyDiscount(billingId, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Discount percentage cannot exceed");

        verify(sessionBillingRepository, never()).save(any(SessionBilling.class));
    }

    @Test
    @DisplayName("Stripe payments write linked ledger entries, reconcile balances and ignore a partial-payment replay")
    void stripePaymentsReconcileBalancesWithoutDuplicatingReplayedIntent() {
        session.setClient(TestDataFactory.createTestClientWithId(42L));
        sessionBilling.setId(10L);
        sessionBilling.setBillingStatus(BillingStatus.PENDING);
        sessionBilling.setPaidAmount(BigDecimal.ZERO);
        sessionBilling.setOutstandingAmount(new BigDecimal("150.00"));
        Long clientId = session.getClient().getId();
        when(sessionBillingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sessionBilling));
        when(sessionBillingRepository.findSessionIdByBillingId(10L)).thenReturn(Optional.of(session.getId()));
        when(sessionRepository.findClientIdBySessionId(session.getId())).thenReturn(Optional.of(clientId));

        // Reconciliation reads only transactions actually submitted to save, not a fabricated paid ledger.
        List<Payment> payments = new ArrayList<>();
        List<PaymentTransaction> transactions = new ArrayList<>();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(100L + payments.size());
            payments.add(payment);
            return payment;
        });
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            transactions.add(transaction);
            return transaction;
        });
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(10L))
                .thenAnswer(invocation -> List.copyOf(transactions));
        when(paymentTransactionRepository.existsByProviderIntentId(anyString()))
                .thenAnswer(invocation -> transactions.stream()
                        .anyMatch(transaction -> invocation.getArgument(0).equals(transaction.getProviderIntentId())));

        billingService.applyStripePortalPayment(10L, clientId, 1L, new BigDecimal("60.00"),
                "pi_qa_partial", "cs_qa_partial", "acct_qa", "acct_qa");

        assertThat(payments).singleElement().satisfies(payment -> {
            assertThat(payment.getSessionBilling()).isSameAs(sessionBilling);
            assertThat(payment.getAmount()).isEqualByComparingTo("60.00");
            assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(payment.getPaymentSource()).isEqualTo(PaymentSource.STRIPE);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getReference()).isEqualTo("pi_qa_partial");
            assertThat(payment.getPaymentDate()).isNotNull();
        });
        assertThat(transactions).singleElement().satisfies(transaction -> {
            assertThat(transaction.getPayment()).isSameAs(payments.get(0));
            assertThat(transaction.getProvider()).isEqualTo(PaymentSource.STRIPE);
            assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.CHARGE);
            assertThat(transaction.getAmount()).isEqualByComparingTo("60.00");
            assertThat(transaction.getProviderIntentId()).isEqualTo("pi_qa_partial");
            assertThat(transaction.getConnectedAccountId()).isEqualTo("acct_qa");
            assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(transaction.getIsVoided()).isFalse();
        });
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(sessionBilling.getClientPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(sessionBilling.getInsurancePaidAmount()).isEqualByComparingTo("0.00");
        assertThat(sessionBilling.getOutstandingAmount()).isEqualByComparingTo("90.00");
        assertThat(sessionBilling.getBillingStatus()).isEqualTo(BillingStatus.BILLED);

        // Replay while still partially paid so the intent guard, not the already-paid guard, is exercised.
        billingService.applyStripePortalPayment(10L, clientId, 1L, new BigDecimal("60.00"),
                "pi_qa_partial", "cs_qa_replay", "acct_qa", "acct_qa");
        assertThat(payments).hasSize(1);
        assertThat(transactions).hasSize(1);
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(sessionBilling.getOutstandingAmount()).isEqualByComparingTo("90.00");
        assertThat(sessionBilling.getStripeCheckoutSessionId()).isEqualTo("cs_qa_partial");
        verify(sessionBillingRepository, times(1)).save(sessionBilling);

        billingService.applyStripePortalPayment(10L, clientId, 1L, new BigDecimal("90.00"),
                "pi_qa_remainder", "cs_qa_remainder", "acct_qa", "acct_qa");
        assertThat(payments).hasSize(2);
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(1).getPayment()).isSameAs(payments.get(1));
        assertThat(transactions.get(1).getAmount()).isEqualByComparingTo("90.00");
        assertThat(transactions.get(1).getProviderIntentId()).isEqualTo("pi_qa_remainder");
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("150.00");
        assertThat(sessionBilling.getClientPaidAmount()).isEqualByComparingTo("150.00");
        assertThat(sessionBilling.getInsurancePaidAmount()).isEqualByComparingTo("0.00");
        assertThat(sessionBilling.getOutstandingAmount()).isEqualByComparingTo("0.00");
        assertThat(sessionBilling.getBillingStatus()).isEqualTo(BillingStatus.PAID);
        assertThat(sessionBilling.getStripePaymentIntentId()).isEqualTo("pi_qa_remainder");
        assertThat(sessionBilling.getStripeCheckoutSessionId()).isEqualTo("cs_qa_remainder");
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(paymentTransactionRepository, times(2)).save(any(PaymentTransaction.class));
        verify(sessionBillingRepository, times(2)).save(sessionBilling);
    }

    @Test
    @DisplayName("Attaching Stripe checkout keeps the invoice unpaid and creates no payment entries")
    void attachingStripeCheckoutDoesNotRecordPayment() {
        session.setClient(TestDataFactory.createTestClientWithId(42L));
        sessionBilling.setId(10L);
        sessionBilling.setBillingStatus(BillingStatus.PENDING);
        sessionBilling.setPaidAmount(BigDecimal.ZERO);
        sessionBilling.setOutstandingAmount(new BigDecimal("150.00"));
        when(sessionBillingRepository.findById(10L)).thenReturn(Optional.of(sessionBilling));

        billingService.attachStripeCheckoutSession(10L, session.getClient().getId(), "cs_qa_unpaid");

        verify(sessionBillingRepository).save(sessionBilling);
        assertThat(sessionBilling.getStripeCheckoutSessionId()).isEqualTo("cs_qa_unpaid");
        assertThat(sessionBilling.getStripePaymentIntentId()).isNull();
        assertThat(sessionBilling.getBillingStatus()).isEqualTo(BillingStatus.PENDING);
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(sessionBilling.getOutstandingAmount()).isEqualByComparingTo("150.00");
        verifyNoInteractions(paymentRepository, paymentTransactionRepository);
    }

    @Test
    @DisplayName("Billing transaction history exposes Stripe references, failure and void details from payment transactions")
    void transactionHistoryMapsStripePaymentDetails() {
        Instant createdAt = Instant.parse("2026-09-01T12:00:00Z");
        Instant voidedAt = createdAt.plusSeconds(60);
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(51L)
                .provider(PaymentSource.STRIPE)
                .transactionType(TransactionType.CHARGE)
                .amount(new BigDecimal("60.00"))
                .providerIntentId("pi_qa_history")
                .providerChargeId("ch_qa_history")
                .providerCustomerId("cus_qa_history")
                .providerPaymentMethodId("pm_qa_history")
                .status(PaymentStatus.FAILED)
                .failureReason("Synthetic card decline")
                .isVoided(true)
                .voidReason("Synthetic reconciliation")
                .voidedAt(voidedAt)
                .createdAt(createdAt)
                .build();
        when(sessionBillingRepository.existsById(10L)).thenReturn(true);
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(transaction));

        assertThat(billingService.getTransactionsForBilling(10L)).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(51L);
            assertThat(response.getProvider()).isEqualTo("stripe");
            assertThat(response.getTransactionType()).isEqualTo("charge");
            assertThat(response.getAmount()).isEqualByComparingTo("60.00");
            assertThat(response.getProviderIntentId()).isEqualTo("pi_qa_history");
            assertThat(response.getProviderChargeId()).isEqualTo("ch_qa_history");
            assertThat(response.getProviderCustomerId()).isEqualTo("cus_qa_history");
            assertThat(response.getProviderPaymentMethodId()).isEqualTo("pm_qa_history");
            assertThat(response.getStatus()).isEqualTo("failed");
            assertThat(response.getFailureReason()).isEqualTo("Synthetic card decline");
            assertThat(response.getVoided()).isTrue();
            assertThat(response.getVoidReason()).isEqualTo("Synthetic reconciliation");
            assertThat(response.getVoidedAt()).isEqualTo(voidedAt);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        });
        verify(paymentTransactionRepository).findBySessionBillingIdOrderByCreatedAtDesc(10L);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invoice preview shows DENIED after status change even with zero payments")
    void invoicePreviewShowsDeniedStatus() {
        com.smart.therapy.flow.client.entity.Client client = TestDataFactory.createTestClientWithId(10L);
        session.setClient(client);
        sessionBilling.setId(1L);
        sessionBilling.setBillingStatus(com.smart.therapy.flow.billing.enums.BillingStatus.DENIED);
        sessionBilling.setPaidAmount(BigDecimal.ZERO);
        sessionBilling.setOutstandingAmount(new BigDecimal("150.00"));
        sessionBilling.setSubtotalAmount(new BigDecimal("150.00"));
        sessionBilling.setRatePerUnit(new BigDecimal("150.00"));
        sessionBilling.setUnits(1);

        when(sessionBillingRepository.findById(1L)).thenReturn(Optional.of(sessionBilling));
        when(practiceConfigurationService.getPracticeConfiguration()).thenReturn(
                com.smart.therapy.flow.system.dto.PracticeConfigurationResponse.builder()
                        .practiceName("Test Practice")
                        .practiceAddress("1 Main St")
                        .practicePhone("555-0000")
                        .practiceEmail("practice@test.com")
                        .practiceWebsite("https://test.com")
                        .build());

        String html = billingService.getInvoicePreview(1L, adminPrincipal);

        assertThat(html).contains("DENIED");
        assertThat(html).contains("Status:");
    }

    @Test
    @DisplayName("Mark as Paid cannot manufacture money without a ledger payment")
    void markAsPaidRequiresRecordedPayment() {
        sessionBilling.setId(1L);
        sessionBilling.setPaidAmount(BigDecimal.ZERO);
        sessionBilling.setOutstandingAmount(new BigDecimal("150.00"));
        when(sessionBillingRepository.findById(1L)).thenReturn(Optional.of(sessionBilling));
        when(sessionBillingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sessionBilling));
        var request = new com.smart.therapy.flow.billing.dto.ChangeBillingStatusRequest();
        request.setBillingStatus("paid");
        assertThatThrownBy(() -> billingService.changeBillingStatus(1L, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Record a payment");
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("0");
        verify(sessionBillingRepository, never()).save(any());
    }

    @Test
    void legacyPaidStatusCannotManufactureMoney() {
        sessionBilling.setId(1L);
        sessionBilling.setPaidAmount(BigDecimal.ZERO);
        when(sessionBillingRepository.findById(1L)).thenReturn(Optional.of(sessionBilling));
        when(sessionBillingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sessionBilling));
        assertThatThrownBy(() -> billingService.updatePaymentStatus(1L, "paid", null, adminPrincipal, null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Record a payment");
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("0");
    }

    @Test
    void editingManualPaymentUpdatesItsLedgerTransaction() {
        sessionBilling.setId(1L);
        when(sessionBillingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sessionBilling));
        var payment = Payment.builder().sessionBilling(sessionBilling).amount(new BigDecimal("50"))
                .paymentSource(PaymentSource.MANUAL).paymentMethod(PaymentMethod.CASH).status(PaymentStatus.PAID).build();
        payment.setId(7L);
        var txn = PaymentTransaction.builder().payment(payment).amount(new BigDecimal("50"))
                .provider(PaymentSource.MANUAL).transactionType(TransactionType.CHARGE).status(PaymentStatus.PAID).build();
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(txn));
        when(paymentTransactionRepository.findBySessionBillingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(txn));
        var request = new com.smart.therapy.flow.billing.dto.EditPaymentRequest(); request.setAmount(new BigDecimal("60"));
        billingService.editPayment(1L, 7L, request, adminPrincipal, null);
        assertThat(txn.getAmount()).isEqualByComparingTo("60");
        assertThat(sessionBilling.getPaidAmount()).isEqualByComparingTo("60");
        assertThat(sessionBilling.getOutstandingAmount()).isEqualByComparingTo("90");
    }


    @Test
    @DisplayName("Client invoice PDF is generated from the same admin invoice HTML layout")
    void clientInvoicePdfUsesAdminHtmlLayout() {
        com.smart.therapy.flow.client.entity.Client client = TestDataFactory.createTestClientWithId(10L);
        session.setClient(client);
        sessionBilling.setId(1L);
        sessionBilling.setBillingStatus(com.smart.therapy.flow.billing.enums.BillingStatus.PAID);
        sessionBilling.setPaidAmount(new BigDecimal("150.00"));
        sessionBilling.setOutstandingAmount(BigDecimal.ZERO);
        sessionBilling.setSubtotalAmount(new BigDecimal("150.00"));
        sessionBilling.setRatePerUnit(new BigDecimal("150.00"));
        sessionBilling.setUnits(1);

        when(sessionBillingRepository.findById(1L)).thenReturn(Optional.of(sessionBilling));
        when(practiceConfigurationService.getPracticeConfiguration()).thenReturn(
                com.smart.therapy.flow.system.dto.PracticeConfigurationResponse.builder()
                        .practiceName("Test Practice")
                        .practiceAddress("1 Main St")
                        .practicePhone("555-0000")
                        .practiceEmail("practice@test.com")
                        .practiceWebsite("https://test.com")
                        .build());

        byte[] pdf = billingService.getInvoicePdfForClientPortal(1L, 10L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
        assertThat(new String(pdf, 0, Math.min(pdf.length, 8), java.nio.charset.StandardCharsets.ISO_8859_1))
                .startsWith("%PDF");
    }
}
