package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByName(String name);
    List<NotificationTemplate> findByNameStartingWith(String prefix);
    List<NotificationTemplate> findByIsActive(Boolean isActive);
    List<NotificationTemplate> findByType(String type);
    List<NotificationTemplate> findByEventTypeIgnoreCase(String eventType);
}
