package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assessment analytics and statistics")
public class AssessmentAnalyticsResponse {

    @Schema(description = "Total number of assignments")
    private Long totalAssignments;

    @Schema(description = "Number of completed assignments")
    private Long completedAssignments;

    @Schema(description = "Number of pending assignments")
    private Long pendingAssignments;

    @Schema(description = "Number of in-progress assignments")
    private Long inProgressAssignments;

    @Schema(description = "Average completion time in days")
    private Double averageCompletionTimeDays;

    @Schema(description = "Completion rate percentage")
    private BigDecimal completionRate;

    @Schema(description = "Average score across all completed assessments")
    private BigDecimal averageScore;

    @Schema(description = "Score distribution by range")
    private Map<String, Long> scoreDistribution;

    @Schema(description = "Assignments by status")
    private Map<String, Long> assignmentsByStatus;

    @Schema(description = "Assignments by template")
    private Map<String, Long> assignmentsByTemplate;

    @Schema(description = "Completion trends over time")
    private List<CompletionTrend> completionTrends;

    @Schema(description = "Top performing templates")
    private List<TemplatePerformance> topTemplates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletionTrend {
        @Schema(description = "Date")
        private LocalDate date;
        @Schema(description = "Number of completions")
        private Long completions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplatePerformance {
        @Schema(description = "Template ID")
        private Long templateId;
        @Schema(description = "Template name")
        private String templateName;
        @Schema(description = "Number of assignments")
        private Long assignmentCount;
        @Schema(description = "Average score")
        private BigDecimal averageScore;
        @Schema(description = "Completion rate")
        private BigDecimal completionRate;
    }
}
