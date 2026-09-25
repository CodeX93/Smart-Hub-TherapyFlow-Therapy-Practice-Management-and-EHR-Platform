package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.TenantFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantFeatureRepository extends JpaRepository<TenantFeature, Long> {

    Optional<TenantFeature> findByOrganisationIdAndFeatureKey(Long organisationId, String featureKey);

    List<TenantFeature> findByOrganisationId(Long organisationId);

    boolean existsByOrganisationIdAndFeatureKey(Long organisationId, String featureKey);
}
