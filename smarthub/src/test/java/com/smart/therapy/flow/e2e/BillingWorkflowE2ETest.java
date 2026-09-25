package com.smart.therapy.flow.e2e;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Billing Workflow End-to-End Tests")
class BillingWorkflowE2ETest extends BaseTenantApiTest {

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

    @Test
    @DisplayName("Should complete billing workflow from session to invoice")
    void shouldCompleteBillingWorkflowFromSessionToInvoice() throws Exception {
        String created = mockMvc.perform(post("/api/v1/sessions/{id}/billing", session.getId())
                        .headers(createHeaders(authToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(session.getId()))
                .andExpect(jsonPath("$.totalAmount").value(100.0))
                .andReturn().getResponse().getContentAsString();
        long billingId = objectMapper.readTree(created).path("id").asLong();
        assertThat(billingId).isPositive();
        mockMvc.perform(get("/api/v1/sessions/{id}/billing", session.getId()).headers(createHeaders(authToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(billingId));
        mockMvc.perform(get("/api/v1/billing/billing").headers(createHeaders(authToken))
                        .param("clientId", client.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(billingId));
    }

    @Test
    @DisplayName("Should handle invoice retrieval workflow")
    void shouldHandleInvoiceRetrievalWorkflow() throws Exception {
        // Step 1: Get all invoices
        mockMvc.perform(get("/api/v1/billing/billing")
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Step 2: Get invoices for specific client
        mockMvc.perform(get("/api/v1/billing/billing")
                        .headers(createHeaders(authToken))
                        .param("clientId", client.getId().toString()))
                .andExpect(status().isOk());
    }
}

