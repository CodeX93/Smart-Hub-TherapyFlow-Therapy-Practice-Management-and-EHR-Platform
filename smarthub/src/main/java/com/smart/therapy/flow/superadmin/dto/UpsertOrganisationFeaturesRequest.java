package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "Feature override upsert request")
@Data
public class UpsertOrganisationFeaturesRequest {
    private Map<String, FeatureValueRequest> features;
}
