package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.client.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientAddressRepository extends JpaRepository<ClientAddress, Long> {
    
    @Query("SELECT a FROM ClientAddress a WHERE a.client.id = :clientId ORDER BY a.isPrimary DESC, a.displayOrder, a.id")
    List<ClientAddress> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT a FROM ClientAddress a WHERE a.client.id = :clientId AND a.addressType = :addressType ORDER BY a.isPrimary DESC, a.displayOrder, a.id")
    List<ClientAddress> findByClientIdAndAddressType(@Param("clientId") Long clientId, @Param("addressType") AddressType addressType);
    
    @Query("SELECT a FROM ClientAddress a WHERE a.client.id = :clientId AND a.isPrimary = true")
    Optional<ClientAddress> findPrimaryByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT a FROM ClientAddress a WHERE a.client.id = :clientId AND a.isPrimary = true AND a.addressType = :addressType")
    Optional<ClientAddress> findPrimaryByClientIdAndAddressType(@Param("clientId") Long clientId, @Param("addressType") AddressType addressType);
    
    @Query("SELECT a FROM ClientAddress a WHERE a.client.id = :clientId AND a.isCurrent = true ORDER BY a.isPrimary DESC, a.displayOrder, a.id")
    List<ClientAddress> findCurrentByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT a FROM ClientAddress a WHERE a.client.id = :clientId AND a.isVerified = true ORDER BY a.isPrimary DESC, a.displayOrder, a.id")
    List<ClientAddress> findVerifiedByClientId(@Param("clientId") Long clientId);
}





