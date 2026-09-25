package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformIntegrationConfigRepository extends JpaRepository<PlatformIntegrationConfig, Long> {
    Optional<PlatformIntegrationConfig> findByIntegrationKey(String integrationKey);
}
