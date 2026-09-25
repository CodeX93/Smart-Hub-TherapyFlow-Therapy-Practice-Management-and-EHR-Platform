package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of a form field at the time of assignment.
 * Preserves the exact field definition (label, type, options) that was shown to the client,
 * ensuring historical accuracy even if the template is later modified.
 */
@Entity
@Table(name = "form_assignment_fields", indexes = {
    @Index(name = "idx_form_assignment_field_assignment", columnList = "assignment_id"),
    @Index(name = "idx_form_assignment_field_field", columnList = "field_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormAssignmentField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private FormAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private FormField field; // Reference to original field for traceability

    @Column(name = "field_label", columnDefinition = "TEXT")
    private String fieldLabel; // Snapshot of field label at assignment time

    @Column(name = "field_type", length = 50)
    private String fieldType; // Snapshot of field type at assignment time

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String options; // Snapshot of field options (JSONB) at assignment time

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "auto_populate", length = 50)
    private String autoPopulate; // Snapshot of auto-populate source at assignment time

    @Column(name = "conditional_display", columnDefinition = "TEXT")
    private String conditionalDisplay; // Snapshot of conditional display rules at assignment time

    @OneToMany(mappedBy = "assignmentField", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<FormResponse> responses = new ArrayList<>();
}
