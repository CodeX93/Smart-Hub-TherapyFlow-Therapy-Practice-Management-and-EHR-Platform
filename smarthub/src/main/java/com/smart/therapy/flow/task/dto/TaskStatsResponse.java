package com.smart.therapy.flow.task.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskStatsResponse {

    private Long totalTasks;
    private Long pendingTasks;
    private Long inProgressTasks;
    private Long completedTasks;
    private Long overdueTasks;
    private Long needsAttentionTasks;
    private Long highPriorityTasks;
    private Long urgentTasks;
}

