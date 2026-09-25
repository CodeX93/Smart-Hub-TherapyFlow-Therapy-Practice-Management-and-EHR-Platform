package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgFeatureOverrideRepository extends JpaRepository<OrgFeatureOverride, Long> {

    List<OrgFeatureOverride> findByOrganisationId(Long organisationId);

    Optional<OrgFeatureOverride> findByOrganisationIdAndFeatureKey(Long organisationId, String featureKey);

    void deleteByOrganisationId(Long organisationId);

    long countByFeatureKeyIgnoreCase(String featureKey);
}
