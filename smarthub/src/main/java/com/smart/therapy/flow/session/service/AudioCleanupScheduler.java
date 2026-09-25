package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.session.entity.AudioFile;
import com.smart.therapy.flow.session.repository.AudioFileRepository;
import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled job for cleaning up expired audio files.
 * Implements 30-day retention policy for HIPAA compliance.
 * 
 * Runs daily at 2 AM to delete expired audio files.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AudioCleanupScheduler {

    private final AudioFileRepository audioFileRepository;
    private final AudioStorageService audioStorageService;
    private final TenantExecutionService tenantExecutionService;
    private final TransactionTemplate transactionTemplate;

    @Value("${audio.cleanup.enabled:true}")
    private Boolean cleanupEnabled;

    @Value("${audio.cleanup.batch-size:100}")
    private Integer batchSize;

    /**
     * Cleanup expired audio files daily at 2 AM
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "${audio.cleanup.cron:0 0 2 * * ?}")
    public void cleanupExpiredAudioFiles() {
        tenantExecutionService.runForEachActiveTenant("audio-cleanup", tenant ->
                transactionTemplate.execute(status -> {
                    cleanupExpiredAudioFilesForTenant();
                    return null;
                })
        );
    }

    @Transactional
    protected void cleanupExpiredAudioFilesForTenant() {
        if (!cleanupEnabled) {
            log.debug("Audio cleanup is disabled");
            return;
        }

        log.info("Starting audio file cleanup job");
        Instant now = Instant.now();
        
        try {
            // Find expired files
            List<AudioFile> expiredFiles = audioFileRepository.findExpiredFiles(now);
            log.info("Found {} expired audio files to delete", expiredFiles.size());

            int deletedCount = 0;
            int errorCount = 0;

            for (AudioFile audioFile : expiredFiles) {
                try {
                    // Delete from storage
                    audioStorageService.deleteAudioFile(audioFile.getStoragePath());
                    
                    // Mark as deleted in database
                    audioFile.markAsExpired();
                    audioFileRepository.save(audioFile);
                    
                    deletedCount++;
                    
                    if (deletedCount % batchSize == 0) {
                        log.info("Deleted {} audio files so far...", deletedCount);
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to delete audio file: id={}, path={}", 
                            audioFile.getId(), audioFile.getStoragePath(), e);
                    // Continue with next file
                }
            }

            log.info("Audio cleanup completed: deleted={}, errors={}, total={}", 
                    deletedCount, errorCount, expiredFiles.size());
        } catch (Exception e) {
            log.error("Audio cleanup job failed", e);
        }
    }

    /**
     * Notification job: Find files expiring soon (within 7 days)
     * Can be used to send notifications to therapists
     */
    @Scheduled(cron = "${audio.cleanup.notification-cron:0 0 9 * * ?}") // Daily at 9 AM
    public void notifyExpiringAudioFiles() {
        tenantExecutionService.runForEachActiveTenant("audio-expiry-notify", tenant ->
                transactionTemplate.execute(status -> {
                    notifyExpiringAudioFilesForTenant();
                    return null;
                })
        );
    }

    @Transactional(readOnly = true)
    protected void notifyExpiringAudioFilesForTenant() {
        if (!cleanupEnabled) {
            return;
        }

        Instant now = Instant.now();
        Instant sevenDaysFromNow = now.plusSeconds(7 * 24 * 60 * 60);

        List<AudioFile> expiringFiles = audioFileRepository.findExpiringSoon(now, sevenDaysFromNow);
        
        if (!expiringFiles.isEmpty()) {
            log.info("Found {} audio files expiring within 7 days", expiringFiles.size());
            // TODO: Send notifications to therapists
            // notificationService.notifyExpiringAudioFiles(expiringFiles);
        }
    }
}
