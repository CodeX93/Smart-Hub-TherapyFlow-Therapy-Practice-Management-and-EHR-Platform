package com.smart.therapy.flow.unit.client.portal;

import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.billing.repository.PaymentRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.dto.PortalInvoiceStatsResponse;
import com.smart.therapy.flow.client.portal.service.PortalInvoiceService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.TimezoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalInvoiceService")
class PortalInvoiceServiceTest {

    @Mock
    private SessionBillingRepository sessionBillingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private TimezoneService timezoneService;
    @Mock
    private BillingService billingService;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private PortalInvoiceService portalInvoiceService;

    @Test
    void returnsZeroAmountsWhenCurrentClientHasNoInvoices() {
        AuthPrincipal principal = TestDataFactory.createAuthPrincipalForClient(10L);
        Client client = TestDataFactory.createTestClient();
        client.setId(10L);
        when(currentUserService.requireCurrentClient(principal)).thenReturn(client);
        when(sessionBillingRepository.sumAmountDueByClientId(10L)).thenReturn(null);
        when(sessionBillingRepository.sumPaidAmountByClientId(10L)).thenReturn(null);
        var stats = portalInvoiceService.getInvoiceStats(principal);
        assertThat(stats.getTotalInvoices()).isZero();
        assertThat(stats.getTotalBilled()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getTotalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Returns discounted invoice totals scoped to the current client")
    void returnsInvoiceStats() {
        AuthPrincipal principal = TestDataFactory.createAuthPrincipalForClient(10L);
        Client client = TestDataFactory.createTestClient();
        client.setId(10L);

        when(currentUserService.requireCurrentClient(principal)).thenReturn(client);
        when(sessionBillingRepository.countByClientId(10L)).thenReturn(4L);
        when(sessionBillingRepository.sumAmountDueByClientId(10L)).thenReturn(new BigDecimal("600.00"));
        when(sessionBillingRepository.sumPaidAmountByClientId(10L)).thenReturn(new BigDecimal("450.00"));

        PortalInvoiceStatsResponse stats = portalInvoiceService.getInvoiceStats(principal);

        assertThat(stats.getTotalInvoices()).isEqualTo(4L);
        assertThat(stats.getTotalBilled()).isEqualByComparingTo("600.00");
        assertThat(stats.getTotalPaid()).isEqualByComparingTo("450.00");
    }
}
