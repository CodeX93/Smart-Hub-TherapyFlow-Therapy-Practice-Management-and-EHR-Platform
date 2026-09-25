package com.smart.therapy.flow.performance;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.common.BaseTenantPerformanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Stress Tests - System Breaking Points")
class StressTest extends BaseTenantPerformanceTest {

    private User therapist;
    private String authToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");
    }

    @Test
    @DisplayName("Should handle high concurrent load without crashing")
    void shouldHandleHighConcurrentLoadWithoutCrashing() throws Exception {
        runConcurrent(50, 500, 60, requestId ->
                mockMvc.perform(get("/api/v1/clients").headers(createHeaders(authToken))
                                .param("page", "1").param("pageSize", "10"))
                        .andExpect(status().isOk()));
    }

    @Test
    @DisplayName("Should handle burst traffic spikes")
    void shouldHandleBurstTrafficSpikes() throws Exception {
        runConcurrent(100, 100, 30, requestId ->
                mockMvc.perform(get("/api/v1/clients").headers(createHeaders(authToken)))
                        .andExpect(status().isOk()));
    }

    @Test
    @DisplayName("Should maintain response time under stress")
    void shouldMaintainResponseTimeUnderStress() throws Exception {
        // Arrange
        int numberOfRequests = 200;
        long[] responseTimes = new long[numberOfRequests];

        // Act
        for (int i = 0; i < numberOfRequests; i++) {
            long startTime = System.currentTimeMillis();
            try {
                mockMvc.perform(get("/api/v1/clients")
                                .headers(createHeaders(authToken))
                                .param("page", "0")
                                .param("pageSize", "10"))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new AssertionError("Request failed during stress measurement", e);
            }
            long endTime = System.currentTimeMillis();
            responseTimes[i] = endTime - startTime;
        }

        // Calculate statistics
        long totalTime = 0;
        long maxTime = 0;
        for (long time : responseTimes) {
            totalTime += time;
            maxTime = Math.max(maxTime, time);
        }
        double averageTime = (double) totalTime / numberOfRequests;

        // Assert
        // Average response time should be reasonable even under stress
        assertThat(averageTime).isLessThan(500); // 500ms average
        // Max response time should not be excessive
        assertThat(maxTime).isLessThan(2000); // 2 seconds max
    }

    @Test
    @DisplayName("Should handle resource exhaustion gracefully")
    void shouldHandleResourceExhaustionGracefully() throws Exception {
        var mrns = java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
        runConcurrent(100, 1000, 120, requestId -> {
            CreateClientRequest request = new CreateClientRequest();
            request.setStatus("active");
            request.setFullName("Stress test client " + requestId);
            request.setEmail(uniqueEmail("stress"));
            String response = mockMvc.perform(post("/api/v1/clients").headers(createHeaders(authToken))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String mrn = objectMapper.readTree(response).path("clientId").asText();
            assertThat(mrn).isNotBlank();
            assertThat(mrns.add(mrn)).as("each created client has a distinct MRN").isTrue();
        });
        assertThat(mrns).hasSize(1000);
        mockMvc.perform(get("/api/v1/clients").headers(createHeaders(authToken)))
                .andExpect(status().isOk());
    }
}
