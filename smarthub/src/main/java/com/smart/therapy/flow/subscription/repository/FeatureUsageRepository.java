package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.FeatureUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureUsageRepository extends JpaRepository<FeatureUsage, Long> {

    @Query("SELECT u FROM FeatureUsage u WHERE u.subscription.id = :subId AND u.feature.id = :featureId AND u.periodStart = :periodStart AND u.targetKey IS NULL")
    Optional<FeatureUsage> findBySubscriptionIdAndFeatureIdAndPeriodStart(
        @Param("subId") Long subscriptionId,
        @Param("featureId") Long featureId,
        @Param("periodStart") java.time.Instant periodStart
    );

    @Query("SELECT u FROM FeatureUsage u WHERE u.subscription.id = :subId AND u.feature.id = :featureId AND u.periodStart = :periodStart AND u.targetKey = :targetKey")
    Optional<FeatureUsage> findBySubscriptionIdAndFeatureIdAndPeriodStartAndTargetKey(
            @Param("subId") Long subscriptionId,
            @Param("featureId") Long featureId,
            @Param("periodStart") java.time.Instant periodStart,
            @Param("targetKey") String targetKey
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE FeatureUsage u
        SET u.usageCount = u.usageCount + :delta
        WHERE u.subscription.id = :subId
          AND u.feature.id = :featureId
          AND u.periodStart = :periodStart
          AND u.targetKey IS NULL
    """)
    int incrementUsageCount(
            @Param("subId") Long subscriptionId,
            @Param("featureId") Long featureId,
            @Param("periodStart") java.time.Instant periodStart,
            @Param("delta") long delta
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE FeatureUsage u
        SET u.usageCount = u.usageCount + :delta
        WHERE u.subscription.id = :subId
          AND u.feature.id = :featureId
          AND u.periodStart = :periodStart
          AND u.targetKey = :targetKey
    """)
    int incrementUsageCountForTarget(
            @Param("subId") Long subscriptionId,
            @Param("featureId") Long featureId,
            @Param("periodStart") java.time.Instant periodStart,
            @Param("delta") long delta,
            @Param("targetKey") String targetKey
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE FeatureUsage u
        SET u.usageCount = u.usageCount + :delta
        WHERE u.subscription.id = :subId
          AND u.feature.id = :featureId
          AND u.periodStart = :periodStart
          AND u.targetKey IS NULL
          AND u.usageCount + :delta <= :maxLimit
    """)
    int incrementUsageCountIfBelowLimit(
            @Param("subId") Long subscriptionId,
            @Param("featureId") Long featureId,
            @Param("periodStart") Instant periodStart,
            @Param("delta") long delta,
            @Param("maxLimit") long maxLimit
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE FeatureUsage u
        SET u.usageCount = u.usageCount + :delta
        WHERE u.subscription.id = :subId
          AND u.feature.id = :featureId
          AND u.periodStart = :periodStart
          AND u.targetKey = :targetKey
          AND u.usageCount + :delta <= :maxLimit
    """)
    int incrementUsageCountIfBelowLimitForTarget(
            @Param("subId") Long subscriptionId,
            @Param("featureId") Long featureId,
            @Param("periodStart") Instant periodStart,
            @Param("delta") long delta,
            @Param("maxLimit") long maxLimit,
            @Param("targetKey") String targetKey
    );

    @Query("SELECT u FROM FeatureUsage u WHERE u.subscription.id = :subId AND u.periodStart = :periodStart")
    List<FeatureUsage> findBySubscriptionIdAndPeriodStart(
            @Param("subId") Long subscriptionId,
            @Param("periodStart") Instant periodStart
    );

    @Query("SELECT u FROM FeatureUsage u WHERE u.subscription.id = :subId AND u.periodStart = :periodStart AND u.targetKey IS NULL")
    List<FeatureUsage> findBySubscriptionIdAndPeriodStartAndTargetKeyIsNull(
            @Param("subId") Long subscriptionId,
            @Param("periodStart") Instant periodStart
    );

    @Query("SELECT u FROM FeatureUsage u WHERE u.subscription.id = :subId AND u.periodStart = :periodStart AND u.targetKey = :targetKey")
    List<FeatureUsage> findBySubscriptionIdAndPeriodStartAndTargetKey(
            @Param("subId") Long subscriptionId,
            @Param("periodStart") Instant periodStart,
            @Param("targetKey") String targetKey
    );

    @Query("SELECT u FROM FeatureUsage u WHERE u.subscription.id = :subId AND u.periodStart = :periodStart AND u.targetKey IS NOT NULL")
    List<FeatureUsage> findBySubscriptionIdAndPeriodStartAndTargetKeyIsNotNull(
            @Param("subId") Long subscriptionId,
            @Param("periodStart") Instant periodStart
    );

    @Modifying
    @Transactional
    @Query("UPDATE FeatureUsage u SET u.usageCount = 0 WHERE u.periodStart = :periodStart")
    int resetUsageByPeriodStart(@Param("periodStart") Instant periodStart);
}
