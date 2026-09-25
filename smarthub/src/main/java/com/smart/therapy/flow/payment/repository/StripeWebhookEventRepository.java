package com.smart.therapy.flow.payment.repository;

import com.smart.therapy.flow.payment.entity.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {

    boolean existsByEventIdAndContextKey(String eventId, String contextKey);

    long deleteByEventIdAndContextKey(String eventId, String contextKey);
}




