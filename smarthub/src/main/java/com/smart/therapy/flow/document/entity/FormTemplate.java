package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.FromCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_templates", indexes = {
    @Index(name = "idx_form_template_category", columnList = "category"),
    @Index(name = "idx_form_template_active", columnList = "is_active"),
    @Index(name = "idx_form_template_created_by", columnList = "created_by_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormTemplate extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FromCategory category; // CONSENT, INTake, RELEASE, AGREEMENT, SAFETY, DISCHARGE, CUSTOM

    @Column(columnDefinition = "TEXT")
    private String instructions; // Instructions shown to client

    @Column(name = "requires_signature", nullable = false)
    @Builder.Default
    private Boolean requiresSignature = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system_template", nullable = false)
    @Builder.Default
    private Boolean isSystemTemplate = false; // Pre-built templates can't be deleted

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version ASC")
    @Builder.Default
    private List<FormTemplateVersion> versions = new ArrayList<>();
}


