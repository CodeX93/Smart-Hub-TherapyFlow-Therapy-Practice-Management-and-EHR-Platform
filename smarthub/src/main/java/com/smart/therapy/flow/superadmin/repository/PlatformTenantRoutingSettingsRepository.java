package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformTenantRoutingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformTenantRoutingSettingsRepository extends JpaRepository<PlatformTenantRoutingSettings, Long> {
    Optional<PlatformTenantRoutingSettings> findTopByOrderByIdAsc();
}

