package com.smart.therapy.flow.task.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checklist_items", indexes = {
    @Index(name = "idx_checklist_item_template", columnList = "template_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ChecklistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    @Column(name = "item_text", nullable = false, columnDefinition = "TEXT")
    private String itemText;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CheckListCategory category;  // 'intake', 'assessment', 'ongoing', 'discharge'

    @Column(name = "item_order")
    private Integer itemOrder; // Order/sequence of items in the checklist

    @Column(name = "days_from_start")
    private Integer daysFromStart;
}
