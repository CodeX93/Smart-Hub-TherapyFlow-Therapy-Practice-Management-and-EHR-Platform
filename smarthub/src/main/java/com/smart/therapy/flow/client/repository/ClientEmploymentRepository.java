package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientEmployment;
import com.smart.therapy.flow.client.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientEmploymentRepository extends JpaRepository<ClientEmployment, Long> {
    
    @Query("SELECT e FROM ClientEmployment e WHERE e.client.id = :clientId")
    Optional<ClientEmployment> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT e FROM ClientEmployment e WHERE e.employmentStatus = :status")
    List<ClientEmployment> findByEmploymentStatus(@Param("status") EmploymentStatus status);
    
    @Query("SELECT e FROM ClientEmployment e WHERE e.employerName = :employerName")
    List<ClientEmployment> findByEmployerName(@Param("employerName") String employerName);
    
    @Query("SELECT e FROM ClientEmployment e WHERE e.financialHardship = true")
    List<ClientEmployment> findClientsWithFinancialHardship();
    
    @Query("SELECT e FROM ClientEmployment e WHERE e.eligibleForSlidingScale = true")
    List<ClientEmployment> findEligibleForSlidingScale();
    
    @Query("SELECT e FROM ClientEmployment e WHERE e.veteranStatus = true")
    List<ClientEmployment> findVeterans();
}





