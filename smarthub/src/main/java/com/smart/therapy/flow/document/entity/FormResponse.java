package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "form_responses", indexes = {
        @Index(name = "idx_form_response_assignment", columnList = "assignment_id"),
        @Index(name = "idx_form_response_assignment_field", columnList = "assignment_field_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormResponse extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private FormAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_field_id", nullable = false)
    private FormAssignmentField assignmentField; // References snapshot, not live field

    /**
     * Form response value (plain text or JSON string depending on field type), encrypted at rest.
     * Stored as TEXT because {@link com.smart.therapy.flow.common.converter.EncryptedStringConverter}
     * produces opaque ciphertext, which is not valid PostgreSQL JSON.
     */
    @Column(name = "response_value", columnDefinition = "TEXT")
    @Convert(converter = com.smart.therapy.flow.common.converter.EncryptedStringConverter.class)
    private String responseValue;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
