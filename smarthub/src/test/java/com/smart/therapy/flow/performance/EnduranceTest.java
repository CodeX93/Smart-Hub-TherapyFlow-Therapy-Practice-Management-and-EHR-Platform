package com.smart.therapy.flow.performance;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantPerformanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Endurance Tests - Long-Running Stability")
class EnduranceTest extends BaseTenantPerformanceTest {

    private User therapist;
    private String authToken;
    private Client testClient;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");

        testClient = persistClient(therapist);
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("Should maintain performance over extended period")
    void shouldMaintainPerformanceOverExtendedPeriod() throws Exception {
        // Arrange
        int numberOfIterations = 100;
        List<Long> responseTimes = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Act - Run requests over extended period
        for (int i = 0; i < numberOfIterations; i++) {
            long iterationStart = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/v1/clients/{id}", testClient.getId())
                            .headers(createHeaders(authToken)))
                    .andExpect(status().isOk());

            long iterationEnd = System.currentTimeMillis();
            responseTimes.add(iterationEnd - iterationStart);

            // Small delay to simulate real-world usage
            Thread.sleep(10);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Calculate statistics
        double averageTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long maxTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        // Assert
        // System should maintain consistent performance
        assertThat(averageTime).isLessThan(200); // 200ms average
        assertThat(maxTime).isLessThan(1000); // 1 second max
        // Total time should be reasonable
        assertThat(totalTime).isLessThan(30000); // 30 seconds for 100 requests
    }

    @Test
    @DisplayName("Should not have memory leaks over time")
    void shouldNotHaveMemoryLeaksOverTime() throws Exception {
        // Arrange
        int numberOfRequests = 500;
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection before test
        System.gc();
        Thread.sleep(100);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Act - Make many requests
        for (int i = 0; i < numberOfRequests; i++) {
            mockMvc.perform(get("/api/v1/clients/{id}", testClient.getId())
                            .headers(createHeaders(authToken)))
                    .andExpect(status().isOk());

            // Periodically check memory
            if (i % 100 == 0) {
                System.gc();
                Thread.sleep(50);
            }
        }

        // Force garbage collection after test
        System.gc();
        Thread.sleep(100);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        // Assert
        // Memory increase should be reasonable (less than 50MB for 500 requests)
        // This is a rough check - actual memory leak detection requires more sophisticated tools
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // 50MB
    }

    @Test
    @DisplayName("Should maintain database connection stability")
    void shouldMaintainDatabaseConnectionStability() throws Exception {
        // Arrange
        int numberOfRequests = 200;

        // Act - Make many database requests
        for (int i = 0; i < numberOfRequests; i++) {
            mockMvc.perform(get("/api/v1/clients")
                            .headers(createHeaders(authToken))
                            .param("page", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk());
        }

        // Assert - If we get here without exceptions, connections are stable
        // This test verifies that connection pool doesn't exhaust
        assertThat(com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId()).isNull();
    }

    @Test
    @DisplayName("Should handle continuous load without degradation")
    void shouldHandleContinuousLoadWithoutDegradation() throws Exception {
        // Arrange
        int numberOfBatches = 10;
        int requestsPerBatch = 20;
        List<Double> batchAverages = new ArrayList<>();

        // Act - Run multiple batches and track performance
        for (int batch = 0; batch < numberOfBatches; batch++) {
            List<Long> batchTimes = new ArrayList<>();
            
            for (int i = 0; i < requestsPerBatch; i++) {
                long start = System.currentTimeMillis();
                try {
                    mockMvc.perform(get("/api/v1/clients/{id}", testClient.getId())
                                    .headers(createHeaders(authToken)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new AssertionError("Request failed during endurance measurement", e);
                }
                long end = System.currentTimeMillis();
                batchTimes.add(end - start);
            }

            double batchAverage = batchTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            batchAverages.add(batchAverage);
        }

        // Assert - Performance should not degrade significantly
        double firstBatch = batchAverages.get(0);
        double lastBatch = batchAverages.get(batchAverages.size() - 1);
        
        // Last batch should not be more than 2x slower than first batch
        assertThat(lastBatch).isLessThan(firstBatch * 2);
    }
}

