package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_action_metadata",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_action_metadata_entity_type", columnNames = {"related_entity_type"})
        },
        indexes = {
                @Index(name = "idx_notification_action_metadata_sort", columnList = "sort_order"),
                @Index(name = "idx_notification_action_metadata_active", columnList = "is_active")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationActionMetadata extends BaseEntity {

    @Column(name = "related_entity_type", nullable = false, length = 50)
    private String relatedEntityType;

    @Column(name = "action_url_template", nullable = false, length = 500)
    private String actionUrlTemplate;

    @Column(name = "default_action_label", nullable = false, length = 100)
    private String defaultActionLabel;

    @Column(name = "example_action_url", length = 500)
    private String exampleActionUrl;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

