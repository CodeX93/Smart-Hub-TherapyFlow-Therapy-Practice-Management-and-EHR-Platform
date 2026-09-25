package com.smart.therapy.flow.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ComplianceReportResponse {
    private Long templateId;
    private String templateName;
    private String category;
    private Long totalAssignments;
    private Long completedAssignments;
    private Long pendingAssignments;
    private Long overdueAssignments;
    private Double completionRate;
    private List<ClientComplianceStatus> clientStatuses;
    private Instant reportGeneratedAt;
    
    @Data
    @Builder
    public static class ClientComplianceStatus {
        private Long clientId;
        private String clientName;
        private Long checklistId;
        private Boolean isCompleted;
        private Instant completedAt;
        private LocalDate dueDate;
        private Boolean isOverdue;
        private Long completedItemsCount;
        private Long totalItemsCount;
    }
}
