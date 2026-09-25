package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Update this and all future sessions in a recurring series")
public class UpdateRecurringFutureRequest {

    @NotNull(message = "Anchor session ID is required")
    private Long anchorId;

    @NotNull(message = "New anchor session date is required")
    private Instant sessionDate;

    private Long roomId;
    private String notes;
    private Long serviceId;
    private Long therapistId;
    private String sessionType;
    private String sessionMode;
    private Boolean zoomEnabled;

    @Schema(defaultValue = "false")
    private Boolean ignoreConflicts = false;
}
