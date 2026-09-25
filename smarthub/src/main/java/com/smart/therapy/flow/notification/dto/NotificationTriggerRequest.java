package com.smart.therapy.flow.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationTriggerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 10000, message = "Description cannot exceed 10000 characters")
    private String description;

    @NotBlank(message = "Event type is required")
    @Size(max = 50, message = "Event type cannot exceed 50 characters")
    private String eventType;

    private com.smart.therapy.flow.notification.enums.EntityType entityType;

    @Size(max = 10000, message = "Condition rules cannot exceed 10000 characters")
    private String conditionRules;

    @Size(max = 10000, message = "Recipient rules cannot exceed 10000 characters")
    private String recipientRules;

    @Size(max = 20, message = "Priority cannot exceed 20 characters")
    @Pattern(regexp = "(?i)^(LOW|MEDIUM|HIGH|URGENT)$",
            message = "Invalid priority. Allowed values: LOW, MEDIUM, HIGH, URGENT")
    private String priority;

    private Boolean isScheduled;

    @Min(value = 0, message = "Schedule offset minutes must be 0 or greater")
    @Max(value = 525600, message = "Schedule offset minutes cannot exceed 525600 (1 year)")
    private Integer scheduleOffsetMinutes;

    @Min(value = 1, message = "Batch window minutes must be at least 1")
    @Max(value = 1440, message = "Batch window minutes cannot exceed 1440 (24 hours)")
    private Integer batchWindowMinutes;

    @Min(value = 1, message = "Max batch size must be at least 1")
    @Max(value = 1000, message = "Max batch size cannot exceed 1000")
    private Integer maxBatchSize;

    private Boolean isActive;
}
