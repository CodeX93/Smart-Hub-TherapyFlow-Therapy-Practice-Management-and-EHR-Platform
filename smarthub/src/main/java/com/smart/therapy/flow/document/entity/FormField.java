package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_fields", indexes = {
    @Index(name = "idx_form_field_template_version", columnList = "template_version_id"),
    @Index(name = "idx_form_field_section", columnList = "section_id"),
    @Index(name = "idx_form_field_type", columnList = "field_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormField extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_version_id", nullable = false)
  private FormTemplateVersion templateVersion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "section_id", nullable = false)
  private FormSection section;

  @Column(name = "field_name", nullable = false, length = 100)
  private String fieldName; // Internal field name/key

  @Column(name = "field_label", nullable = false, columnDefinition = "TEXT")
  private String fieldLabel;

  @Enumerated(EnumType.STRING)
  @Column(name = "field_type", nullable = false, length = 50)
  private FieldType fieldType; // 'text', 'textarea', 'select', 'checkbox', 'radio', 'date', 'signature', etc.

  @Column(name = "is_required", nullable = false)
  @Builder.Default
  private Boolean isRequired = false;

  @Column(name = "default_value", columnDefinition = "TEXT")
  private String defaultValue;

  @Column(name = "validation_rules", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String validationRules; // JSONB for structured validation rules

  @Column(name = "help_text", columnDefinition = "TEXT")
  private String helpText;

  @Column(length = 255)
  private String placeholder;

  @Column(name = "is_repeatable", nullable = false)
  @Builder.Default
  private Boolean isRepeatable = false; // Allow multiple entries for this field

  @Column(name = "scoring_formula", columnDefinition = "TEXT")
  private String scoringFormula; // Formula for calculating score (for assessments)

  @Column(name = "max_score")
  private Integer maxScore; // Maximum possible score for this field

  @Column(name = "auto_populate", length = 50)
  private String autoPopulate; // Auto-populate source (client_name, client_dob, etc.)

  @Column(name = "conditional_display", columnDefinition = "TEXT")
  private String conditionalDisplay; // Conditional display rules (JSON)

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<FormFieldOption> options = new ArrayList<>(); // Normalized options

  @OneToMany(mappedBy = "field", cascade = CascadeType.ALL)
  @Builder.Default
  private List<FormAssignmentField> assignmentFields = new ArrayList<>(); // Assignment snapshots
}
