package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "billing_notification_logs", schema = "public", indexes = {
        @Index(name = "idx_billing_notification_logs_org_event", columnList = "organisation_id, event_key, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class BillingNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
