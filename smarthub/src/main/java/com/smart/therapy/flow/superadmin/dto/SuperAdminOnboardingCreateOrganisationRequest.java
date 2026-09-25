package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminOnboardingCreateOrganisationRequest {
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,48}[a-z0-9]$")
    private String slug;

    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$")
    private String subdomain;

    @NotBlank
    @Email
    private String primaryAdminEmail;

    @NotBlank
    @Size(min = 1, max = 50)
    private String primaryAdminFirstName;

    @NotBlank
    @Size(min = 1, max = 50)
    private String primaryAdminLastName;

    @NotBlank
    private String plan;

    @NotBlank
    private String billingCycle;

    private Integer trialDays;

    @NotBlank
    private String timezone;

    @NotBlank
    private String region;

    @NotBlank
    private String dataResidency;

    private Boolean provisionTenant = true;

    private Boolean provisionStripeSubscription = false;
}
