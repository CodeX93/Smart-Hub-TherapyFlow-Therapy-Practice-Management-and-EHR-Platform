package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Bulk feature-key request")
public class BulkFeatureKeyRequest {

    @NotEmpty
    @Schema(description = "Feature keys", example = "[\"CLIENT_PORTAL\",\"ADVANCED_BILLING\"]")
    private List<String> featureKeys;
}
