package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.NotificationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface NotificationTriggerRepository extends JpaRepository<NotificationTrigger, Long> {
    List<NotificationTrigger> findByEventType(String eventType);
    List<NotificationTrigger> findByIsActive(Boolean isActive);
}





