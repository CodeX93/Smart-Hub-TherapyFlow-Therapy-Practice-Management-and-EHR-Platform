package com.smart.therapy.flow.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientChecklistResponse {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long templateId;
    private String templateName;
    private Boolean isCompleted;
    private Instant completedAt;
    private Long completedById;
    private String completedByName;
    private String notes;
    private String description;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ClientChecklistItemResponse> items;
}

