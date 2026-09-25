package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.billing.dto.*;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.repository.*;
import com.smart.therapy.flow.billing.service.*;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingMutationAccessTest {
    @Mock SessionBillingRepository sessionBillingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentTransactionRepository paymentTransactionRepository;
    @Mock BillingGuard billingGuard;
    @InjectMocks BillingService service;

    @ParameterizedTest
    @ValueSource(strings = {"payment-status", "edit", "refund", "billing-status"})
    void inaccessibleBillIsRejectedBeforeAnyPaymentReadOrWrite(String operation) {
        AuthPrincipal principal = TestDataFactory.createAuthPrincipal(TestDataFactory.createTestTherapist());
        var bill = SessionBilling.builder().build();
        when(sessionBillingRepository.findById(42L)).thenReturn(Optional.of(bill));
        when(sessionBillingRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(bill));
        doThrow(new ForbiddenException("Outside billing scope")).when(billingGuard).assertStaffBillingAccess(bill, principal);
        assertThatThrownBy(() -> {
            switch (operation) {
                case "payment-status" -> service.updatePaymentStatus(42L, "paid", null, principal, null);
                case "edit" -> service.editPayment(42L, 7L, new EditPaymentRequest(), principal, null);
                case "refund" -> service.refundPayment(42L, new RefundPaymentRequest(), principal, null);
                default -> service.changeBillingStatus(42L, new ChangeBillingStatusRequest(), principal, null);
            }
        }).isInstanceOf(ForbiddenException.class).hasMessage("Outside billing scope");
        verifyNoInteractions(paymentRepository, paymentTransactionRepository);
        verify(sessionBillingRepository, never()).save(any());
    }
}
