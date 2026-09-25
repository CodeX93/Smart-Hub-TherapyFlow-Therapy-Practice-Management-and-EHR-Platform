package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformApiKeyScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformApiKeyScopeRepository extends JpaRepository<PlatformApiKeyScope, Long> {
    List<PlatformApiKeyScope> findByApiKeyId(Long apiKeyId);
    void deleteByApiKeyId(Long apiKeyId);
}

