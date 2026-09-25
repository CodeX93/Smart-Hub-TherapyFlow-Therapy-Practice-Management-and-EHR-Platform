package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.dto.BillingStatisticsResponse;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
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
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.organisation.service.TenantFeatureService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Statistics Unit Tests")
class BillingServiceStatisticsTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private SessionBillingRepository sessionBillingRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ClientSearchHelper clientSearchHelper;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private PermissionChecker permissionChecker;
    @Mock private CaseloadScopeService caseloadScopeService;
    @Mock private TenantFeatureService tenantFeatureService;
    @Mock private PracticeConfigurationService practiceConfigurationService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private InvoicePolicyService invoicePolicyService;
    @Mock private ClientInsuranceRepository clientInsuranceRepository;
    @Mock private BillingGuard billingGuard;
    @Mock private ClientReportAccessService clientReportAccessService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private BillingService billingService;

    private AuthPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        User admin = TestDataFactory.createTestAdmin();
        admin.setId(1L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);

        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(admin);
        when(caseloadScopeService.resolve(any())).thenReturn(
                new CaseloadScopeService.ResolvedCaseloadScope(CaseloadScope.ALL, List.of(), 1L));
        when(practiceConfigurationService.getPracticeConfiguration()).thenReturn(
                PracticeConfigurationResponse.builder().timezone("America/Toronto").build());
    }

    @Test
    @DisplayName("totalCollected includes paid and partial invoices and reports both counts")
    void getBillingStatistics_countsPaidAndPartialSeparately() {
        List<SessionBilling> rows = new ArrayList<>();
        // 6 fully paid → $400
        rows.add(billing(BillingStatus.PAID, "170.00", "170.00", "0.00", 101L));
        rows.add(billing(BillingStatus.PAID, "0.00", "0.00", "0.00", 102L));
        rows.add(billing(BillingStatus.PAID, "0.00", "0.00", "0.00", 103L));
        rows.add(billing(BillingStatus.PAID, "0.00", "0.00", "0.00", 104L));
        rows.add(billing(BillingStatus.PAID, "60.00", "60.00", "0.00", 105L));
        rows.add(billing(BillingStatus.PAID, "170.00", "170.00", "0.00", 106L));
        // 4 partial (BILLED with money collected) → $600
        rows.add(billing(BillingStatus.BILLED, "170.00", "160.00", "10.00", 201L));
        rows.add(billing(BillingStatus.BILLED, "170.00", "153.00", "17.00", 202L));
        rows.add(billing(BillingStatus.BILLED, "200.00", "143.50", "56.50", 203L));
        rows.add(billing(BillingStatus.BILLED, "200.00", "143.50", "56.50", 204L));
        // unpaid billed with $0 collected — must not count as partial
        rows.add(billing(BillingStatus.BILLED, "170.00", "0.00", "170.00", 301L));

        when(sessionBillingRepository.findAll(any(Specification.class))).thenReturn(rows);

        BillingStatisticsResponse stats = billingService.getBillingStatistics(
                adminPrincipal,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));

        assertThat(stats.getTotalCollected()).isEqualByComparingTo("1000.00");
        assertThat(stats.getPaidRecords()).isEqualTo(6L);
        assertThat(stats.getPartialRecords()).isEqualTo(4L);
        assertThat(stats.getTotalBillingRecords()).isEqualTo(11L);
    }

    @Test
    @DisplayName("unbounded admin stats use SQL aggregates including partialRecords")
    void getBillingStatistics_unboundedUsesAggregatePartialCount() {
        when(sessionBillingRepository.sumPositiveOutstandingAmountForDateRange(any(), any()))
                .thenReturn(new BigDecimal("19341.66"));
        when(sessionBillingRepository.sumCreditBalanceForDateRange(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(sessionBillingRepository.sumPaidAmountForDateRange(any(), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(sessionBillingRepository.countDistinctClientsForDateRange(any(), any())).thenReturn(69L);
        when(sessionBillingRepository.countForDateRange(any(), any())).thenReturn(94L);
        when(sessionBillingRepository.countByBillingStatusInForDateRange(any(), any(), any()))
                .thenReturn(0L);
        when(sessionBillingRepository.countPartiallyPaidForDateRange(any(), any(), any()))
                .thenReturn(4L);

        BillingStatisticsResponse stats = billingService.getBillingStatistics(adminPrincipal);

        assertThat(stats.getTotalCollected()).isEqualByComparingTo("1000.00");
        assertThat(stats.getPartialRecords()).isEqualTo(4L);
    }

    @Test
    void filteredStatisticsNeverUseUnfilteredAggregates() {
        when(sessionBillingRepository.findAll(any(Specification.class))).thenReturn(List.of());
        BillingStatisticsResponse stats = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                billingService, "getBillingStatistics", adminPrincipal,
                null, null, null, "partial", null, null, null, null,
                null, null, null, null, null);
        assertThat(stats.getTotalBillingRecords()).isZero();
        assertThat(stats.getTotalCollected()).isEqualByComparingTo("0");
        org.mockito.Mockito.verify(sessionBillingRepository).findAll(any(Specification.class));
        org.mockito.Mockito.verify(sessionBillingRepository, org.mockito.Mockito.never())
                .countForDateRange(any(), any());
    }

    private SessionBilling billing(
            BillingStatus status,
            String total,
            String paid,
            String outstanding,
            long clientId) {
        Client client = TestDataFactory.createTestClientWithId(clientId);
        User therapist = TestDataFactory.createTestTherapist();
        therapist.setId(9L);
        Session session = TestDataFactory.createTestSession(client, therapist);
        session.setId(clientId);
        session.setSessionDate(Instant.parse("2026-09-10T15:00:00Z"));

        SessionBilling row = SessionBilling.builder()
                .session(session)
                .serviceCode("Psy02")
                .units(1)
                .ratePerUnit(new BigDecimal(total))
                .subtotalAmount(new BigDecimal(total))
                .totalAmount(new BigDecimal(total))
                .paidAmount(new BigDecimal(paid))
                .clientPaidAmount(new BigDecimal(paid))
                .insurancePaidAmount(BigDecimal.ZERO)
                .outstandingAmount(new BigDecimal(outstanding))
                .billingStatus(status)
                .billingDate(LocalDate.of(2026, 9, 10))
                .insuranceCovered(false)
                .build();
        row.setId(clientId);
        return row;
    }
}
