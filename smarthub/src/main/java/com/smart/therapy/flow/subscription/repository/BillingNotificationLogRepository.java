package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.BillingNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BillingNotificationLogRepository extends JpaRepository<BillingNotificationLog, Long> {

    List<BillingNotificationLog> findTop200ByOrganisation_IdOrderByCreatedAtDesc(Long organisationId);

    boolean existsByOrganisation_IdAndEventKeyAndCreatedAtAfter(Long organisationId, String eventKey, Instant createdAt);
}
