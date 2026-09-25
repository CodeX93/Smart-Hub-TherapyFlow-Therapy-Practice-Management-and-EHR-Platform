package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PlatformApiKeyRepository extends JpaRepository<PlatformApiKey, Long> {
    Optional<PlatformApiKey> findByKeyPrefix(String keyPrefix);
    Optional<PlatformApiKey> findByKeyHash(String keyHash);
    boolean existsByKeyNameIgnoreCaseAndIsActiveTrue(String keyName);
    boolean existsByKeyNameIgnoreCase(String keyName);
    long countByIsActiveTrue();

    @Modifying
    @Query("update PlatformApiKey k set k.lastUsedAt = :usedAt where k.id = :id")
    int touchLastUsedAt(@Param("id") Long id, @Param("usedAt") Instant usedAt);
}
