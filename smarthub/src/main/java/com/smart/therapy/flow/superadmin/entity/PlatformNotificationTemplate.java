package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_notification_templates", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformNotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(name = "subject_template", nullable = false, length = 255)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "updated_by_auth_id")
    private Long updatedByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
