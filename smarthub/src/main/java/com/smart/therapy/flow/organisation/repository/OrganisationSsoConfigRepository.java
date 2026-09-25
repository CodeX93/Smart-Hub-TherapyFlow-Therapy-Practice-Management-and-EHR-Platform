package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.OrganisationSsoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganisationSsoConfigRepository extends JpaRepository<OrganisationSsoConfig, Long> {

    Optional<OrganisationSsoConfig> findByOrganisationIdAndProviderAndIsEnabledTrue(Long organisationId, String provider);

    List<OrganisationSsoConfig> findByOrganisationIdAndIsEnabledTrue(Long organisationId);
}
