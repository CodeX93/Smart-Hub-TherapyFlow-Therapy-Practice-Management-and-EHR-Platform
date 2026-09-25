package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.FeaturePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeaturePricingRepository extends JpaRepository<FeaturePricing, Long> {

    Optional<FeaturePricing> findByFeatureId(Long featureId);
}
