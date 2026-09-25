package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeatureCatalogUpdateRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    private String scope;

    private Boolean defaultEnabled;

    private Boolean deprecated;
}
