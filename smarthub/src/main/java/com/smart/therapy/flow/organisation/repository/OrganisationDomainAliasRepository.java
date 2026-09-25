package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.OrganisationDomainAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganisationDomainAliasRepository extends JpaRepository<OrganisationDomainAlias, Long> {

    List<OrganisationDomainAlias> findAll();
}
