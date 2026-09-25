package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.TenantSchemaVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSchemaVersionRepository extends JpaRepository<TenantSchemaVersion, Long> {

    Optional<TenantSchemaVersion> findByOrganisationId(Long organisationId);
}
