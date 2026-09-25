package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@TenantScoped
public interface ClientInsuranceRepository extends JpaRepository<ClientInsurance, Long> {
    
    @Query("SELECT i FROM ClientInsurance i WHERE i.client.id = :clientId")
    Optional<ClientInsurance> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT i FROM ClientInsurance i WHERE i.client.id = :clientId AND i.isActive = true")
    Optional<ClientInsurance> findActiveByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT COUNT(i) > 0 FROM ClientInsurance i WHERE i.policyNumber = :policyNumber AND i.insuranceProvider = :provider")
    boolean existsByPolicyNumberAndProvider(@Param("policyNumber") String policyNumber, @Param("provider") String provider);
    
    @Query("SELECT i FROM ClientInsurance i WHERE i.policyNumber = :policyNumber AND i.insuranceProvider = :provider")
    Optional<ClientInsurance> findByPolicyNumberAndProvider(@Param("policyNumber") String policyNumber, @Param("provider") String provider);
}





