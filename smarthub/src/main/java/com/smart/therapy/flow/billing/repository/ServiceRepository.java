package com.smart.therapy.flow.billing.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.billing.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findByServiceCode(String serviceCode);
    List<Service> findByIsActive(Boolean isActive);
    List<Service> findByPublicSiteEnabledTrueAndIsActiveTrue();

    @Modifying
    @Query("UPDATE Service s SET s.therapistVisible = :visible")
    int updateAllTherapistVisible(boolean visible);
}





