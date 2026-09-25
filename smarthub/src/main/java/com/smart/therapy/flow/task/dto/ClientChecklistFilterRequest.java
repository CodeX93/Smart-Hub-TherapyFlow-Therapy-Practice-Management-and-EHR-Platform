package com.smart.therapy.flow.task.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class ClientChecklistFilterRequest {
    private Long clientId; // Optional: filter by client
    private Long templateId; // Optional: filter by template
    private String category; // Optional: filter by checklist category (INTAKE, ASSESSMENT, ONGOING, DISCHARGE)
    private Boolean isCompleted; // Optional: filter by completion status
    private LocalDate completedDateFrom; // Optional: filter by completion date from
    private LocalDate completedDateTo; // Optional: filter by completion date to
    private LocalDate dueDateFrom; // Optional: filter by due date from
    private LocalDate dueDateTo; // Optional: filter by due date to
    private LocalDate createdDateFrom; // Optional: filter by creation date from
    private LocalDate createdDateTo; // Optional: filter by creation date to
}
