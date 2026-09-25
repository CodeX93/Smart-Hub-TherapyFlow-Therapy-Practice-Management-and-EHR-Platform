package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.repository.PaymentRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.service.PortalInvoiceService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalInvoiceService Unit Tests")
class PortalInvoiceServiceTest {

    @Mock private SessionBillingRepository sessionBillingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private TimezoneService timezoneService;
    @Mock private BillingService billingService;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private PortalInvoiceService portalInvoiceService;

    @Test
    @DisplayName("Portal invoice receipt rejects invoice belonging to another client")
    void portalInvoiceRejectsOtherClientInvoice() {
        Client client = TestDataFactory.createTestClientWithId(5L);
        AuthPrincipal principal = TestDataFactory.createAuthPrincipalForClient(99L);

        Client otherClient = TestDataFactory.createTestClientWithId(99L);
        Session session = Session.builder().client(otherClient).build();
        SessionBilling billing = SessionBilling.builder().session(session).build();
        billing.setId(10L);

        when(currentUserService.requireCurrentClient(principal)).thenReturn(client);
        when(sessionBillingRepository.findById(10L)).thenReturn(Optional.of(billing));

        assertThatThrownBy(() -> portalInvoiceService.downloadInvoiceReceipt(
                principal, 10L, "127.0.0.1", "test-agent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice not found or access denied");
    }
}
