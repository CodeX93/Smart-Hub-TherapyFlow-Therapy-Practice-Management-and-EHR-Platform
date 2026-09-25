package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeatureCatalogCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String key;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    private String scope;

    @NotNull
    private String type;

    @NotNull
    private Boolean defaultEnabled;
}
