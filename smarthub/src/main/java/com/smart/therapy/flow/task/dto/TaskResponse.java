package com.smart.therapy.flow.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String titleKey;
    private String taskType;
    private String description;
    private Long commentCount;
    private String status;
    private String priority;
    private Instant dueDate;
    private Long clientId;
    private String clientName;
    private Long assignedToId;
    private String assignedToName;
    private Instant createdAt;
    private Instant updatedAt;
}
