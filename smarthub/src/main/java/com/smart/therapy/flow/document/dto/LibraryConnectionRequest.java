package com.smart.therapy.flow.document.dto;

import com.smart.therapy.flow.document.enums.ConnectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LibraryConnectionRequest {

    @NotNull(message = "From entry id is required")
    private Long fromEntryId;

    @NotNull(message = "To entry id is required")
    private Long toEntryId;

    @Schema(
            description = "Type of connection between entries",
            allowableValues = { "Related", "Reference", "Derived", "Supplement", "Other" },
            example = "Related")
    private ConnectionType connectionType = ConnectionType.RELATED;

    @Min(value = 1, message = "Strength must be between 1 and 5")
    @Max(value = 5, message = "Strength must be between 1 and 5")
    private Integer strength = 4;

    private String description;
}

