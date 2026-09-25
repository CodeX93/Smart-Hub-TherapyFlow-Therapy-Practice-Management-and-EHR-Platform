package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.common.validation.ValidFeatureCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Assign add-on request for an organisation")
@Data
public class UpsertOrgAddonRequest {
    @NotBlank
    @ValidFeatureCode
    private String featureCode;

    @Min(1)
    private Integer quantity;
}
