package com.smart.therapy.flow.publicsite.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePublicSiteServiceRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private Boolean enabled;

    private Integer displayOrder;

    @Min(value = 5, message = "Duration must be at least 5 minutes")
    private Integer durationMinutes;

    @DecimalMin(value = "0.0", inclusive = true, message = "Base rate cannot be negative")
    private BigDecimal baseRate;
}
