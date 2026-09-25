package com.smart.therapy.flow.security;

import com.smart.therapy.flow.billing.controller.BillingController;
import com.smart.therapy.flow.billing.dto.BillingStatisticsResponse;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.billing.service.TenantSubscriptionInvoiceService;
import com.smart.therapy.flow.billing.service.TenantSubscriptionQueryService;
import com.smart.therapy.flow.common.config.AppProperties;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(BillingCustomRoleSecurityTest.MethodSecurityTestConfig.class)
@ContextConfiguration(classes = BillingController.class)
@DisplayName("Billing custom role security")
class BillingCustomRoleSecurityTest extends BaseControllerContractTest {

    @MockBean
    private com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService fixtureStripePlatformSubscriptionService;


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;
    @MockBean
    private SubscriptionFeatureService subscriptionFeatureService;
    @MockBean
    private TenantSubscriptionQueryService tenantSubscriptionQueryService;
    @MockBean
    private TenantSubscriptionInvoiceService tenantSubscriptionInvoiceService;
    @MockBean
    private AppProperties appProperties;

    @BeforeEach
    void setUpMocks() {
        when(subscriptionFeatureService.isFeatureEnabled(any(), any(), any())).thenReturn(true);
        when(billingService.getBillingStatistics(any(AuthPrincipal.class))).thenReturn(
                BillingStatisticsResponse.builder()
                        .outstandingBalance(BigDecimal.ZERO)
                        .creditBalance(BigDecimal.ZERO)
                        .totalCollected(BigDecimal.ZERO)
                        .activeClients(0L)
                        .totalBillingRecords(0L)
                        .pendingRecords(0L)
                        .paidRecords(0L)
                        .partialRecords(0L)
                        .deniedRecords(0L)
                        .followUpRecords(0L)
                        .build());
    }

    @Test
    @DisplayName("Custom BILLING_ROLE with BILLING_VIEW can read statistics")
    @WithMockUser(authorities = {"ROLE_BILLING_ROLE", "BILLING_VIEW"})
    void customBillingRoleCanReadStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/billing/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User without billing permission cannot read statistics")
    @WithMockUser(authorities = {"ROLE_BILLING_ROLE"})
    void customBillingRoleWithoutPermissionIsBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/billing/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fixed ADMIN role with BILLING_VIEW can still read statistics")
    @WithMockUser(authorities = {"ROLE_ADMIN", "BILLING_VIEW"})
    void adminWithBillingViewCanReadStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/billing/statistics"))
                .andExpect(status().isOk());
    }

    @org.springframework.context.annotation.Configuration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig {
    }
}
