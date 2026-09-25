package com.smart.therapy.flow.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "billing_notification_templates", schema = "public", indexes = {
        @Index(name = "idx_billing_notification_templates_event", columnList = "event_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class BillingNotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 100)
    private String eventKey;

    @Column(name = "subject_template", nullable = false, length = 500)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
