package com.smart.therapy.flow.api;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BillingController API Tests")
class BillingControllerApiTest extends BaseTenantApiTest {

    @Autowired
    private SessionRepository sessionRepository;

    private User therapist;
    private Client client;
    private Session session;
    private String authToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");

        client = persistClient(therapist);
        client = clientRepository.save(client);

        session = Session.builder()
                .client(client)
                .service(fixtureService)
                .therapist(therapist)
                .sessionDate(Instant.now().minusSeconds(86400))
                .duration(60)
                .status(com.smart.therapy.flow.session.enums.SessionStatus.COMPLETED.getValue())
                .sessionType(com.smart.therapy.flow.session.enums.SessionType.IN_PERSON.getValue())
                .clinicalSessionType("psychotherapy")
                .build();
        session = sessionRepository.save(session);
    }

    @Autowired
    private com.smart.therapy.flow.billing.repository.SessionBillingRepository billingRepository;
    @Autowired
    private com.smart.therapy.flow.billing.repository.PaymentRepository paymentRepository;

    @Test
    void paymentAndBillingStatusAreIndependentThroughHttp() throws Exception {
        var bill = billingRepository.saveAndFlush(com.smart.therapy.flow.billing.entity.SessionBilling.builder()
                .session(session).serviceCode(fixtureService.getServiceCode())
                .ratePerUnit(new java.math.BigDecimal("100"))
                .subtotalAmount(new java.math.BigDecimal("100"))
                .totalAmount(new java.math.BigDecimal("100"))
                .outstandingAmount(new java.math.BigDecimal("100"))
                .billingStatus(com.smart.therapy.flow.billing.enums.BillingStatus.PENDING).build());
        paymentRepository.saveAndFlush(com.smart.therapy.flow.billing.entity.Payment.builder()
                .sessionBilling(bill).amount(java.math.BigDecimal.TEN)
                .paymentMethod(com.smart.therapy.flow.billing.enums.PaymentMethod.CASH)
                .status(com.smart.therapy.flow.billing.enums.PaymentStatus.FAILED).build());
        for (String paymentState : java.util.List.of("pending", "failed")) {
            mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                            .param("clientId", client.getId().toString())
                            .param("status", "pending").param("paymentStatus", paymentState))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(bill.getId().intValue())));
        }
        mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                        .param("clientId", client.getId().toString())
                        .param("status", "pending").param("paymentStatus", "paid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
        mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                        .param("clientId", client.getId().toString())
                        .param("status", "denied").param("paymentStatus", "failed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void invalidFiltersAreRejectedRatherThanIgnored() throws Exception {
        for (String parameter : java.util.List.of("clientType", "sessionType", "paymentMethod", "paymentStatus", "status")) {
            mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                            .param(parameter, "not-a-valid-filter"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                        .param("startDate", "2026-09-20").param("endDate", "2026-09-10"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                        .param("minAmount", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                        .param("minAmount", "200").param("maxAmount", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get services list via API")
    void shouldGetServicesListViaApi() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/billing/services")
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should get invoices for client via API")
    void shouldGetInvoicesForClientViaApi() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/billing/billing")
                        .headers(createHeaders(authToken))
                        .param("clientId", client.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should return 403 when accessing billing API without authentication")
    void shouldReturn403WhenAccessingBillingApiWithoutAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/billing/services")
                        .headers(createHeaders()))
                .andExpect(status().isForbidden());
    }
}

