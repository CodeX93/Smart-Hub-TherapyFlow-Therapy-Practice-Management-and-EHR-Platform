package com.smart.therapy.flow.performance;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantPerformanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Performance Load Tests")
class LoadTest extends BaseTenantPerformanceTest {

    private User therapist;
    private String authToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");
    }

    @Test
    @DisplayName("Should handle concurrent client creation requests")
    void shouldHandleConcurrentClientCreationRequests() throws Exception {
        runConcurrent(10, 50, 30, requestId -> {
            CreateClientRequest request = new CreateClientRequest();
            request.setStatus("active");
            request.setFullName("Load test client " + requestId);
            request.setEmail(uniqueEmail("load"));
            mockMvc.perform(post("/api/v1/clients").headers(createHeaders(authToken))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        });
    }

    @Test
    @DisplayName("Should handle multiple concurrent read requests efficiently")
    void shouldHandleMultipleConcurrentReadRequestsEfficiently() throws Exception {
        Client client = persistClient(therapist);
        long start = System.nanoTime();
        runConcurrent(20, 200, 30, requestId ->
                mockMvc.perform(get("/api/v1/clients/{id}", client.getId()).headers(createHeaders(authToken)))
                        .andExpect(status().isOk()));
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(10000);
    }

    @Test
    @DisplayName("Should maintain performance under load")
    void shouldMaintainPerformanceUnderLoad() throws Exception {
        // Arrange
        Client client = persistClient(therapist);
        client = clientRepository.save(client);

        int numberOfRequests = 100;
        // Measure sustained reads in the same state when this class runs alone or
        // after other API suites. Keep warm-up fixed, and validate every response.
        long warmupStart = System.nanoTime();
        for (int i = 0; i < numberOfRequests; i++) {
            mockMvc.perform(get("/api/v1/clients/{id}", client.getId())
                            .headers(createHeaders(authToken)))
                    .andExpect(status().isOk());
        }
        System.out.printf("Client read warm-up: %d requests, %.2f ms average%n", numberOfRequests,
                (System.nanoTime() - warmupStart) / 1_000_000.0 / numberOfRequests);
        long[] requestNanos = new long[numberOfRequests];
        System.out.println("Measuring client reads");
        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < numberOfRequests; i++) {
            long requestStart = System.nanoTime();
            mockMvc.perform(get("/api/v1/clients/{id}", client.getId())
                            .headers(createHeaders(authToken)))
                    .andExpect(status().isOk());
            requestNanos[i] = System.nanoTime() - requestStart;
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageTime = (double) totalTime / numberOfRequests;

        // Keep the measured result in successful reports as well as failures.
        System.out.printf("Client reads: %d requests, %.2f ms average (limit: <100 ms)%n",
                numberOfRequests, averageTime);
        System.out.printf("Client read timing: first 10 %.2f ms, last 10 %.2f ms, median %.2f ms, max %.2f ms%n",
                Arrays.stream(requestNanos, 0, 10).average().orElseThrow() / 1_000_000,
                Arrays.stream(requestNanos, 90, 100).average().orElseThrow() / 1_000_000,
                Arrays.stream(requestNanos).sorted().skip(50).findFirst().orElseThrow() / 1_000_000.0,
                Arrays.stream(requestNanos).max().orElseThrow() / 1_000_000.0);

        // Assert
        assertThat(averageTime).isLessThan(100); // 100ms average
    }
}
