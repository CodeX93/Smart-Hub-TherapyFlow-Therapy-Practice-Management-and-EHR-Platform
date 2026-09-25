package com.smart.therapy.flow.document.dto;

import com.smart.therapy.flow.document.enums.ConnectionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class LibraryConnectionUpdateRequest {

    private Long fromEntryId;
    private Long toEntryId;
    private ConnectionType connectionType;

    @Min(value = 1, message = "Strength must be between 1 and 5")
    @Max(value = 5, message = "Strength must be between 1 and 5")
    private Integer strength;

    private String description;
    private Boolean active;
}

