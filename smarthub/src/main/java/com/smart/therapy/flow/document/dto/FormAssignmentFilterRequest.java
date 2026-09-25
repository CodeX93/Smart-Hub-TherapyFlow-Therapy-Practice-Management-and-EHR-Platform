package com.smart.therapy.flow.document.dto;

import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import com.smart.therapy.flow.document.enums.Status;
import lombok.Data;

import java.time.Instant;

@Data
public class FormAssignmentFilterRequest {
    private Long clientId; // Optional: filter by client
    private Long templateId; // Optional: filter by template
    private FormTemplateVersionStatus versionStatus; // Optional: filter by template version status (DRAFT, ACTIVE, ARCHIVED)
    private Status assignmentStatus; // Optional: filter by assignment completion status
    private Instant assignmentDateFrom; // Optional: filter assignments created from this date
    private Instant assignmentDateTo; // Optional: filter assignments created to this date
    private Instant dueDateFrom; // Optional: filter by due date from
    private Instant dueDateTo; // Optional: filter by due date to
}
