package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import com.smart.therapy.flow.session.repository.SessionTranscriptChunkRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Clears expired READY transcript PHI after the configured retention window.
 * Disabled by default; enable only when retention policy is approved for production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptRetentionScheduler {

    private final SessionTranscriptRepository sessionTranscriptRepository;
    private final SessionTranscriptChunkRepository sessionTranscriptChunkRepository;
    private final TenantExecutionService tenantExecutionService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.transcripts.cleanup.enabled:false}")
    private boolean cleanupEnabled;

    @Value("${app.transcripts.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.transcripts.cleanup.cron:0 30 3 * * ?}")
    public void cleanupExpiredTranscripts() {
        if (!cleanupEnabled) {
            return;
        }
        tenantExecutionService.runForEachActiveTenant("transcript-retention-cleanup", tenant ->
                transactionTemplate.execute(status -> {
                    cleanupExpiredTranscriptsForTenant();
                    return null;
                })
        );
    }

    protected void cleanupExpiredTranscriptsForTenant() {
        Instant now = Instant.now();
        Instant fallbackCutoff = now.minus(retentionDays, ChronoUnit.DAYS);

        var expired = sessionTranscriptRepository.findAllActiveOrderByUpdatedAtDesc().stream()
                .filter(t -> t.getStatus() == SessionTranscriptStatus.READY)
                .filter(t -> {
                    if (t.getExpiresAt() != null) {
                        return t.getExpiresAt().isBefore(now);
                    }
                    Instant reference = t.getFinalizedAt() != null ? t.getFinalizedAt() : t.getUpdatedAt();
                    return reference != null && reference.isBefore(fallbackCutoff);
                })
                .toList();

        if (expired.isEmpty()) {
            return;
        }

        int cleared = 0;
        for (var transcript : expired) {
            transcript.setFinalTranscript(null);
            transcript.setRawContent(null);
            sessionTranscriptRepository.save(transcript);
            sessionTranscriptChunkRepository.clearChunkTextByTranscriptId(transcript.getId());
            cleared++;
        }
        log.info("Transcript retention cleanup cleared PHI for {} expired READY transcripts", cleared);
    }
}
