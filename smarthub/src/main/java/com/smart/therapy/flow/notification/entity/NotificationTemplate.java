package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notification_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "triggers")
public class NotificationTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    /** Domain event this template is bound to (e.g. session_completed). Distinct from channel {@link #type}. */
    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate; // Template with {{variables}}

    @Column(name = "action_url_template", length = 500)
    private String actionUrlTemplate;

    @Column(name = "action_label", length = 100)
    private String actionLabel;

    @Column(name = "recipient_roles", columnDefinition = "TEXT")
    private String recipientRoles; // JSON array of roles that should receive this

    @Column(columnDefinition = "TEXT")
    private String variables; // JSON describing available template variables

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false; // System templates cannot be deleted

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    @Builder.Default
    private List<NotificationTrigger> triggers = new ArrayList<>();
}
