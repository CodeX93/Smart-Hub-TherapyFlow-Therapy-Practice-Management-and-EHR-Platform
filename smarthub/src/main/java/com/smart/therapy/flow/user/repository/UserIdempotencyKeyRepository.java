package com.smart.therapy.flow.user.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.user.entity.UserIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@TenantScoped
public interface UserIdempotencyKeyRepository extends JpaRepository<UserIdempotencyKey, Long> {
    
    Optional<UserIdempotencyKey> findByKey(String key);
    
    @Modifying
    @Query("UPDATE UserIdempotencyKey uik SET uik.userId = :userId WHERE uik.key = :key")
    void updateUserId(@Param("key") String key, @Param("userId") Long userId);
}




