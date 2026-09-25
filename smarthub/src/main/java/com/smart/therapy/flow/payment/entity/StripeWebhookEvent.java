package com.smart.therapy.flow.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "stripe_webhook_events", schema = "public", indexes = {
        @Index(name = "idx_stripe_webhook_events_processed_at", columnList = "processed_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_stripe_webhook_events_event_context", columnNames = {"event_id", "context_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @Column(name = "context_key", nullable = false, length = 255)
    private String contextKey;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
