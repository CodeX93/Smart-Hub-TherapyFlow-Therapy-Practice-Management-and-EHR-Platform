package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.BillingNotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingNotificationTemplateRepository extends JpaRepository<BillingNotificationTemplate, Long> {

    Optional<BillingNotificationTemplate> findByEventKey(String eventKey);

    List<BillingNotificationTemplate> findByIsActiveTrueOrderByEventKeyAsc();
}
