package com.smart.therapy.flow.cms.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Triggers Azure Static Web Apps rebuild of therapyflow-website via GitHub repository_dispatch.
 * Mirrors Strapi's frontend-deploy.ts behaviour (debounced, optional).
 */
@Component
@Slf4j
public class CmsFrontendDeployTrigger {

    private static final String DISPATCH_EVENT = "strapi-content-published";
    private static final long DEBOUNCE_MS = 60_000L;

    @Value("${cms.frontend-deploy.enabled:true}")
    private boolean enabled;

    @Value("${cms.frontend-deploy.github-token:}")
    private String githubToken;

    @Value("${cms.frontend-deploy.landing-repo:MachineIntelligence6/therapyflow-website}")
    private String landingRepo;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cms-frontend-deploy");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> pending;

    public void scheduleLandingDeploy(String contentType) {
        if (!enabled) {
            log.info("[cms-deploy] Skipped: cms.frontend-deploy.enabled=false");
            return;
        }
        if (githubToken == null || githubToken.isBlank()) {
            log.warn("[cms-deploy] Skipped: cms.frontend-deploy.github-token is not configured");
            return;
        }

        ScheduledFuture<?> previous = pending;
        if (previous != null) {
            previous.cancel(false);
        }
        pending = scheduler.schedule(() -> dispatch(contentType), DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        log.info("[cms-deploy] Scheduled landing deploy in {}ms (contentType={})", DEBOUNCE_MS, contentType);
    }

    private void dispatch(String contentType) {
        if (!inFlight.compareAndSet(false, true)) {
            log.info("[cms-deploy] Already in progress; skipping duplicate");
            return;
        }
        try {
            String body = """
                    {
                      "event_type": "%s",
                      "client_payload": {
                        "source": "therapyflow-backend",
                        "target": "landing",
                        "contentType": "%s"
                      }
                    }
                    """.formatted(DISPATCH_EVENT, contentType == null ? "cms_landing_page" : contentType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + landingRepo + "/dispatches"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Content-Type", "application/json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[cms-deploy] Dispatched landing deploy to {} (status={})", landingRepo, response.statusCode());
            } else {
                log.warn("[cms-deploy] GitHub dispatch failed: status={} body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("[cms-deploy] Failed to dispatch landing deploy: {}", e.getMessage());
        } finally {
            inFlight.set(false);
        }
    }
}
