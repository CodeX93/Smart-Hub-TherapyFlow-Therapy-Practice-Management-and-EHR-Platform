package com.smart.therapy.flow.task.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checklist_templates", indexes = {
        @Index(name = "idx_checklist_template_category", columnList = "category"),
        @Index(name = "idx_checklist_template_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ChecklistTemplate extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "client_type", length = 20)
    private String clientType; // Dynamic client type from system options

    @Column(length = 100)
    private String category;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user")
    private User createdByUser;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChecklistItem> items = new ArrayList<>();
}
