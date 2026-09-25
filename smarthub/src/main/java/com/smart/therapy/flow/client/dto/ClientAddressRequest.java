package com.smart.therapy.flow.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.smart.therapy.flow.client.enums.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a client address")
public class ClientAddressRequest {

    @NotNull(message = "Address type is required")
    @Schema(description = "Type of address (HOME, WORK, BILLING, TEMPORARY, etc.)", 
            example = "HOME", requiredMode = Schema.RequiredMode.REQUIRED)
    private AddressType addressType;

    @NotBlank(message = "Street address is required")
    @Size(max = 500, message = "Street address must not exceed 500 characters")
    @JsonAlias({"legacyAddress", "addressLegacy"})
    @Schema(description = "Street address line 1 (aliases: legacyAddress, addressLegacy)", example = "123 Main Street", requiredMode = Schema.RequiredMode.REQUIRED)
    private String streetAddress1;

    @Size(max = 500, message = "Street address line 2 must not exceed 500 characters")
    @Schema(description = "Street address line 2", example = "Apt 4B")
    private String streetAddress2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City", example = "Toronto", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Size(max = 100, message = "State/Province must not exceed 100 characters")
    @JsonAlias({"stateLegacy"})
    @Schema(description = "State or province (alias: stateLegacy)", example = "Ontario")
    private String stateProvince;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @JsonAlias({"zipCodeLegacy"})
    @Schema(description = "Postal or ZIP code (alias: zipCodeLegacy)", example = "M5H 2N2")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Schema(description = "Country", example = "Canada")
    private String country;

    @Schema(description = "Whether this is the primary address", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Valid from date (for temporary addresses)", example = "2025-01-01")
    private LocalDate validFrom;

    @Schema(description = "Valid until date (for temporary addresses)", example = "2025-12-31")
    private LocalDate validUntil;
}

