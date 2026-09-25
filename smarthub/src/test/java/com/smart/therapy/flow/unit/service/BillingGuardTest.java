package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.service.BillingGuard;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.session.entity.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingGuard Unit Tests")
class BillingGuardTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private com.smart.therapy.flow.common.security.CaseloadScopeService caseloadScopeService;

    @InjectMocks
    private BillingGuard billingGuard;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = com.smart.therapy.flow.common.security.CaseloadScope.class,
            names = {"NONE", "OWN", "TEAM", "TEAM_AND_OWN"})
    void customRoleCannotManageBillOutsideResolvedScope(com.smart.therapy.flow.common.security.CaseloadScope scope) {
        var principal = com.smart.therapy.flow.common.TestDataFactory.createAuthPrincipal(
                com.smart.therapy.flow.common.TestDataFactory.createTestTherapist());
        var other = com.smart.therapy.flow.common.TestDataFactory.createTestTherapist(); other.setId(99L);
        var client = Client.builder().assignedTherapist(other).build();
        var bill = SessionBilling.builder().session(Session.builder().client(client).therapist(other).build()).build();
        org.mockito.Mockito.lenient().when(caseloadScopeService.resolve(principal)).thenReturn(
                new com.smart.therapy.flow.common.security.CaseloadScopeService.ResolvedCaseloadScope(scope, java.util.List.of(2L), 1L));
        assertThatThrownBy(() -> billingGuard.assertStaffBillingAccess(bill, principal)).isInstanceOf(ForbiddenException.class);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = com.smart.therapy.flow.common.security.CaseloadScope.class,
            names = {"ALL", "OWN", "TEAM", "TEAM_AND_OWN"})
    void permitsBillInsideResolvedScope(com.smart.therapy.flow.common.security.CaseloadScope scope) {
        var principal = com.smart.therapy.flow.common.TestDataFactory.createAuthPrincipal(
                com.smart.therapy.flow.common.TestDataFactory.createTestTherapist());
        var assigned = com.smart.therapy.flow.common.TestDataFactory.createTestTherapist(); assigned.setId(2L);
        var conducted = com.smart.therapy.flow.common.TestDataFactory.createTestTherapist(); conducted.setId(1L);
        var bill = SessionBilling.builder().session(Session.builder()
                .client(Client.builder().assignedTherapist(assigned).build()).therapist(conducted).build()).build();
        when(caseloadScopeService.resolve(principal)).thenReturn(
                new com.smart.therapy.flow.common.security.CaseloadScopeService.ResolvedCaseloadScope(scope, java.util.List.of(2L), 1L));
        org.assertj.core.api.Assertions.assertThatCode(() -> billingGuard.assertStaffBillingAccess(bill, principal)).doesNotThrowAnyException();
    }

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should reject billing without tenant context")
    void shouldRejectBillingWithoutTenant() {
        TenantContext.clear();
        assertThatThrownBy(() -> billingGuard.requireTenantContext())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("Should reject billing without session")
    void shouldRejectBillingWithoutSession() {
        assertThatThrownBy(() -> billingGuard.requireSessionForBilling(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session is required");
    }

    @Test
    @DisplayName("Should reject negative payment delta")
    void shouldRejectNegativePaymentDelta() {
        assertThatThrownBy(() -> billingGuard.rejectNegativePaymentDelta(java.math.BigDecimal.valueOf(-5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid payment amount");
    }

    @Test
    @DisplayName("Should reject invoice already paid for portal pay")
    void shouldRejectAlreadyPaidInvoice() {
        SessionBilling billing = SessionBilling.builder().billingStatus(BillingStatus.PAID).build();
        assertThatThrownBy(() -> billingGuard.assertInvoiceNotPaid(billing))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("Should reject tenant organisation mismatch")
    void shouldRejectTenantOrganisationMismatch() {
        TenantContext.setOrganisationId(10L);
        assertThatThrownBy(() -> billingGuard.assertTenantOrganisationMatches(99L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("another organisation");
    }

    @Test
    @DisplayName("Should reject connected account mismatch")
    void shouldRejectConnectedAccountMismatch() {
        assertThatThrownBy(() -> billingGuard.assertConnectedAccountMatches("acct_a", "acct_b"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("connected account mismatch");
    }

    @Test
    @DisplayName("Should reject payment delta that exceeds outstanding balance")
    void shouldRejectPaymentExceedingOutstanding() {
        assertThatThrownBy(() -> billingGuard.rejectPaymentExceedingOutstanding(
                        java.math.BigDecimal.valueOf(150.00),
                        java.math.BigDecimal.valueOf(100.00)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds the outstanding balance")
                .hasMessageContaining("$150.00")
                .hasMessageContaining("$100.00");
    }

    @Test
    @DisplayName("Should allow payment delta equal to outstanding balance")
    void shouldAllowPaymentEqualToOutstanding() {
        billingGuard.rejectPaymentExceedingOutstanding(
                java.math.BigDecimal.valueOf(100.00),
                java.math.BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Should reject client invoice ownership mismatch")
    void shouldRejectClientOwnershipMismatch() {
        Client client = Client.builder().build();
        client.setId(5L);
        Session session = Session.builder().client(client).build();
        SessionBilling billing = SessionBilling.builder().session(session).build();

        assertThatThrownBy(() -> billingGuard.assertClientOwnsInvoice(billing, 99L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }
}
