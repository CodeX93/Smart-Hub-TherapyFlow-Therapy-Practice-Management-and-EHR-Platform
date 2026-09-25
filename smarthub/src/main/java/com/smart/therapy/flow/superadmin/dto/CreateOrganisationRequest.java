package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Request to create an organisation")
@Data
public class CreateOrganisationRequest {
@NotBlank
@Size(max = 255)
private String name;

@NotBlank
@Size(max = 100)
@Pattern(regexp = "[a-zA-Z0-9_-]+", message = "Slug must be alphanumeric, hyphen, underscore")
private String slug;

@Size(max = 20)
private String status;

@Size(max = 63)
@Pattern(regexp = "[a-zA-Z0-9-]*", message = "Subdomain alphanumeric and hyphen only")
private String subdomain;

@Size(max = 64)
private String timezone;

@Size(max = 100)
private String region;

@Size(max = 30)
private String dataResidency;

private Boolean provisionTenant;
}
