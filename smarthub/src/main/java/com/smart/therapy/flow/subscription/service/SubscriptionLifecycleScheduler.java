package com.smart.therapy.flow.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleScheduler {

    private final SubscriptionLifecycleService lifecycleService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    @Value("${subscription.lifecycle.lock-ttl-seconds:900}")
    private long lockTtlSeconds;
    @Value("${app.redis.fail-open:true}")
    private boolean redisFailOpen;
    @Value("${subscription.lifecycle.dunning.batch-size:50}")
    private int dunningBatchSize;
    @Value("${subscription.lifecycle.dunning.max-batches-per-run:10}")
    private int dunningMaxBatchesPerRun;

    @Scheduled(cron = "${subscription.lifecycle.trial-notice-cron:0 0 9 * * *}")
    public void notifyTrialExpiry() {
        runWithDistributedLock("subscription-lifecycle:trial-notice", () -> {
            int trialCount = lifecycleService.notifyTrialsExpiringSoon();
            int termCount = lifecycleService.notifySubscriptionsEndingSoon(1);
            if (trialCount > 0 || termCount > 0) {
                log.info("Expiry notices sent: trial={}, termEndingSoon={}", trialCount, termCount);
            }
        });
    }

    @Scheduled(cron = "${subscription.lifecycle.trial-expiry-cron:0 */30 * * * *}")
    public void expireTrials() {
        runWithDistributedLock("subscription-lifecycle:trial-expiry", () -> {
            int trialExpired = lifecycleService.expireTrialsAndMarkPastDue();
            int termEnded = lifecycleService.enforceEndedSubscriptions();
            if (trialExpired > 0 || termEnded > 0) {
                log.info("Subscription expiry processing: trialExpiredToPastDue={}, termEndedLocked={}", trialExpired, termEnded);
            }
        });
    }

    @Scheduled(cron = "${subscription.lifecycle.dunning-cron:0 */30 * * * *}")
    public void processDunning() {
        runWithDistributedLock("subscription-lifecycle:dunning", () -> {
            int totalProcessed = 0;
            int totalContention = 0;
            int boundedBatchSize = Math.max(1, Math.min(dunningBatchSize, 500));
            int boundedBatches = Math.max(1, Math.min(dunningMaxBatchesPerRun, 100));
            for (int i = 0; i < boundedBatches; i++) {
                SubscriptionLifecycleService.DunningBatchResult batch = lifecycleService.processPastDueDunningBatch(boundedBatchSize);
                totalProcessed += batch.processedCount();
                totalContention += batch.contentionCount();
                if (batch.scannedCount() < boundedBatchSize) {
                    break;
                }
            }
            if (totalProcessed > 0 || totalContention > 0) {
                log.info("Processed dunning: updated={}, lockDeferred={}", totalProcessed, totalContention);
            }
        });
    }

    @Scheduled(cron = "${subscription.lifecycle.usage-reset-cron:0 0 0 1 * *}")
    public void resetUsageCounters() {
        runWithDistributedLock("subscription-lifecycle:usage-reset", () -> {
            YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
            int resetRows = subscriptionFeatureService.resetUsageForMonth(currentMonth);
            log.info("Usage reset completed for {} rows in {}", resetRows, currentMonth);
        });
    }

    private void runWithDistributedLock(String lockKey, Runnable task) {
        if (redisTemplate.isEmpty()) {
            task.run();
            return;
        }
        RedisTemplate<String, Object> redis = redisTemplate.get();
        String owner = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, owner, Duration.ofSeconds(lockTtlSeconds));
            if (!Boolean.TRUE.equals(acquired)) {
                log.debug("Skipping job due to active lock: {}", lockKey);
                return;
            }
            try {
                task.run();
            } finally {
                try {
                    Object currentOwner = redis.opsForValue().get(lockKey);
                    if (owner.equals(currentOwner)) {
                        redis.delete(lockKey);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to release scheduler lock {} due to Redis unavailability: {}", lockKey, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            if (redisFailOpen) {
                // Redis unavailable: continue without distributed lock on single node best effort.
                log.warn("Redis unavailable for scheduler lock {}. Running without distributed lock: {}", lockKey, ex.getMessage());
                task.run();
                return;
            }
            throw ex;
        }
    }
}
