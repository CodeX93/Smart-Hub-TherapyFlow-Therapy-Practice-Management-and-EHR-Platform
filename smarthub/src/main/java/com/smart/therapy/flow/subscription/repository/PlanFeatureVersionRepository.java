package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.PlanFeatureVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanFeatureVersionRepository extends JpaRepository<PlanFeatureVersion, Long> {

    @Query("SELECT pf FROM PlanFeatureVersion pf WHERE pf.plan.id = :planId AND pf.effectiveFrom <= :at AND (pf.effectiveTo IS NULL OR pf.effectiveTo > :at)")
    List<PlanFeatureVersion> findByPlanIdEffectiveAt(@Param("planId") Long planId, @Param("at") Instant at);

    @Query("SELECT pf FROM PlanFeatureVersion pf WHERE pf.plan.id = :planId AND pf.feature.id = :featureId AND pf.effectiveFrom <= :at AND (pf.effectiveTo IS NULL OR pf.effectiveTo > :at)")
    Optional<PlanFeatureVersion> findCurrentByPlanAndFeature(@Param("planId") Long planId,
                                                              @Param("featureId") Long featureId,
                                                              @Param("at") Instant at);

    List<PlanFeatureVersion> findByPlanIdAndEffectiveToIsNull(Long planId);

    List<PlanFeatureVersion> findByPlanIdInAndEffectiveToIsNull(List<Long> planIds);

    void deleteByPlanId(Long planId);

    @Query("SELECT COUNT(pf) FROM PlanFeatureVersion pf WHERE pf.feature.id = :featureId")
    long countByFeatureId(@Param("featureId") Long featureId);
}
