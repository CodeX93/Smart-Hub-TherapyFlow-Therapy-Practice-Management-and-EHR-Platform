package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByNameIgnoreCase(String name);
    Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<SubscriptionPlan> findAllByOrderByBasePriceAsc();
    List<SubscriptionPlan> findAllByStatusOrderByBasePriceAsc(SubscriptionPlanStatus status);
}
