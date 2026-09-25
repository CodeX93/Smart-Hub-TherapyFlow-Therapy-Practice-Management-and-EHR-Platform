package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.organisation.entity.OrgStripeAccount;
import com.smart.therapy.flow.organisation.entity.OrgStripeOnboardingStatus;
import com.smart.therapy.flow.organisation.repository.OrgStripeAccountRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.payment.service.OrgStripeConnectService;
import com.smart.therapy.flow.payment.service.StripePlatformConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgStripeConnectService Unit Tests")
class OrgStripeConnectServiceTest {

    @Mock private OrgStripeAccountRepository orgStripeAccountRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private StripePlatformConfigService stripePlatformConfigService;
    @Mock private PlatformAuditService platformAuditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OrgStripeConnectService orgStripeConnectService;

    @Test
    @DisplayName("Portal payment requires configured Stripe Connect account")
    void portalPaymentRequiresConfiguredAccount() {
        when(orgStripeAccountRepository.findByOrganisationId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orgStripeConnectService.requireReadyConnectAccountId(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    @DisplayName("Portal payment requires completed onboarding")
    void portalPaymentRequiresCompletedOnboarding() {
        OrgStripeAccount account = OrgStripeAccount.builder()
                .organisationId(1L)
                .connectAccountId(null)
                .onboardingStatus(OrgStripeOnboardingStatus.PENDING)
                .build();
        when(orgStripeAccountRepository.findByOrganisationId(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> orgStripeConnectService.requireReadyConnectAccountId(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("onboarding is incomplete");
    }

    @Test
    @DisplayName("Portal payment requires charges enabled")
    void portalPaymentRequiresChargesEnabled() {
        OrgStripeAccount account = OrgStripeAccount.builder()
                .organisationId(1L)
                .connectAccountId("acct_ready")
                .chargesEnabled(false)
                .build();
        when(orgStripeAccountRepository.findByOrganisationId(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> orgStripeConnectService.requireReadyConnectAccountId(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Charges are not enabled");
    }

    @Test
    @DisplayName("Ready Connect account id is returned when onboarding complete")
    void readyConnectAccountIdReturned() {
        OrgStripeAccount account = OrgStripeAccount.builder()
                .organisationId(1L)
                .connectAccountId("acct_ready")
                .chargesEnabled(true)
                .build();
        when(orgStripeAccountRepository.findByOrganisationId(1L)).thenReturn(Optional.of(account));

        String accountId = orgStripeConnectService.requireReadyConnectAccountId(1L);

        assertThat(accountId).isEqualTo("acct_ready");
    }
}
