package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an option for a form field (e.g., for SELECT, RADIO, CHECKBOX fields).
 * Normalized from JSON string storage to proper relational structure.
 */
@Entity
@Table(name = "form_field_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormFieldOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private FormField field;

    @Column(nullable = false, length = 255)
    private String label; // Display label (e.g., "Yes", "No", "Option 1")

    @Column(nullable = false, length = 255)
    private String value; // Option value (e.g., "yes", "no", "option1")

    @Column
    private Integer score; // Optional score for assessment/scoring forms

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
