package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.PlanPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanPricingTierRepository extends JpaRepository<PlanPricingTier, Long> {
    List<PlanPricingTier> findByPlanIdOrderByMinTherapistsAsc(Long planId);
    void deleteByPlanId(Long planId);

    List<PlanPricingTier> findByPlanIdInOrderByPlanIdAscMinTherapistsAsc(List<Long> planIds);
}
