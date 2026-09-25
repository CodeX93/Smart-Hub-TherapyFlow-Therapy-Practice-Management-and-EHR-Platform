package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.OrganisationSsoAllowedDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganisationSsoAllowedDomainRepository extends JpaRepository<OrganisationSsoAllowedDomain, Long> {

    List<OrganisationSsoAllowedDomain> findByOrganisationIdOrderByDomainAsc(Long organisationId);

    void deleteByOrganisationId(Long organisationId);
}

