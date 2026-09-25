package com.smart.therapy.flow.unit.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InvoicePolicyRequestDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void deserializesProductionPayload() throws Exception {
        String json = """
                {
                  "clientTypeKey": "Refugee",
                  "clientTypeLabel": "Refugee",
                  "appointmentStatusKey": "confirmed",
                  "appointmentStatusLabel": "Confirmed",
                  "enabled": true,
                  "priceType": "PERCENTAGE",
                  "invoicePrice": 45,
                  "policyName": "new",
                  "serviceId": null,
                  "effectiveFrom": "2026-07-06",
                  "effectiveTo": "2026-07-30",
                  "priority": null
                }
                """;

        InvoicePolicyRequest request = objectMapper.readValue(json, InvoicePolicyRequest.class);

        assertThat(request.getPolicyName()).isEqualTo("new");
        assertThat(request.getPriority()).isNull();
        assertThat(request.getPriceType().name()).isEqualTo("PERCENTAGE");
    }

    @Test
    void deserializesPolicyNameObjectShape() throws Exception {
        String json = """
                {
                  "clientTypeKey": "Refugee",
                  "clientTypeLabel": "Refugee",
                  "appointmentStatusKey": "confirmed",
                  "appointmentStatusLabel": "Confirmed",
                  "enabled": true,
                  "priceType": "PERCENTAGE",
                  "invoicePrice": 45,
                  "policyName": {"optionKey": "new", "optionLabel": "New"},
                  "serviceId": null,
                  "effectiveFrom": "2026-07-06",
                  "effectiveTo": "2026-07-30",
                  "priority": null
                }
                """;

        InvoicePolicyRequest request = objectMapper.readValue(json, InvoicePolicyRequest.class);
        assertThat(request.getPolicyName()).isEqualTo("new");
    }
}
