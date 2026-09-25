package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Form response data")
public class FormResponseDto {
    private Long id;
    private Long assignmentId;
    private Long assignmentFieldId; // Snapshot reference
    private Long fieldId; // Original field reference (for traceability)
    private String fieldLabel; // From snapshot
    private String fieldType; // From snapshot
    private String value;
    private Instant createdAt;
    private Instant updatedAt;
}

