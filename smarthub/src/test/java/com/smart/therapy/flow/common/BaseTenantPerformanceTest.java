package com.smart.therapy.flow.common;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Local MockMvc/database load checks; these do not measure network or production capacity. */
public abstract class BaseTenantPerformanceTest extends BaseTenantApiTest {
    @FunctionalInterface
    protected interface RequestAction {
        void run(int requestId) throws Exception;
    }

    protected void runConcurrent(int threads, int requests, int timeoutSeconds, RequestAction action) throws Exception {
        var executor = Executors.newFixedThreadPool(threads);
        var futures = new ArrayList<Future<?>>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        try {
            for (int i = 0; i < requests; i++) {
                final int requestId = i;
                futures.add(executor.submit(() -> {
                    action.run(requestId);
                    return null;
                }));
            }
            executor.shutdown();
            // Future.get propagates request exceptions and assertion errors to JUnit.
            for (var future : futures) {
                future.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).as("all request workers stopped").isTrue();
        }
    }
}
