package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@TenantScoped
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    
    Optional<IdempotencyKey> findByKey(String key);
    
    @Modifying
    @Query("UPDATE IdempotencyKey ik SET ik.clientId = :clientId WHERE ik.key = :key")
    void updateClientId(@Param("key") String key, @Param("clientId") Long clientId);
}




