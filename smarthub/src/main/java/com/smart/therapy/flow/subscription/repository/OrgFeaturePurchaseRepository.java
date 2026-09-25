package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.OrgFeaturePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrgFeaturePurchaseRepository extends JpaRepository<OrgFeaturePurchase, Long> {

    @Query("SELECT p FROM OrgFeaturePurchase p WHERE p.subscription.id = :subId AND p.startAt <= :at AND (p.endAt IS NULL OR p.endAt > :at)")
    List<OrgFeaturePurchase> findActiveBySubscriptionIdAt(@Param("subId") Long subscriptionId, @Param("at") Instant at);

    List<OrgFeaturePurchase> findBySubscriptionIdOrderByStartAtDesc(Long subscriptionId);

    @Query("SELECT COUNT(p) FROM OrgFeaturePurchase p WHERE p.feature.id = :featureId")
    long countByFeatureId(@Param("featureId") Long featureId);

    @Query("""
            SELECT COUNT(p)
            FROM OrgFeaturePurchase p
            WHERE p.feature.id = :featureId
              AND (p.endAt IS NULL OR p.endAt > :at)
            """)
    long countActiveByFeatureId(@Param("featureId") Long featureId, @Param("at") Instant at);
}
