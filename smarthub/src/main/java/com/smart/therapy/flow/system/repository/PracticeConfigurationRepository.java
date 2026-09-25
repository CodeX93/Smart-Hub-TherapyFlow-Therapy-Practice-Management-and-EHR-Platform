package com.smart.therapy.flow.system.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.system.entity.PracticeConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@TenantScoped
public interface PracticeConfigurationRepository extends JpaRepository<PracticeConfiguration, Long> {
    
    /**
     * Get the practice configuration (singleton pattern - only one record should exist)
     * @return Optional practice configuration
     */
    Optional<PracticeConfiguration> findFirstByOrderByIdAsc();
}




