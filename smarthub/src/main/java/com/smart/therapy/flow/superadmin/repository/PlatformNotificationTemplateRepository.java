package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformNotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformNotificationTemplateRepository extends JpaRepository<PlatformNotificationTemplate, Long> {
    Optional<PlatformNotificationTemplate> findByTemplateKey(String templateKey);
}
