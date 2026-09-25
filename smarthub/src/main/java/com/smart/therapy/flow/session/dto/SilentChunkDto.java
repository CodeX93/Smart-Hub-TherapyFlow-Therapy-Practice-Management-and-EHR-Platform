package com.smart.therapy.flow.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SilentChunkDto {
    @NotNull(message = "index is required")
    @Min(value = 0, message = "index must be >= 0")
    private Integer index;

    @NotNull(message = "durationSeconds is required")
    @Min(value = 0, message = "durationSeconds must be >= 0")
    private Double durationSeconds;
}
