package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Client session feedback request")
public class RateSessionRequest {
    @NotNull
    @Min(0)
    @Max(10)
    @Schema(description = "Client rating score (0-10)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rating;

    @Schema(description = "Optional feedback comments")
    private String comment;
}
