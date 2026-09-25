package com.smart.therapy.flow.client.dto;

import com.smart.therapy.flow.client.enums.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client address information")
public class ClientAddressResponse {

    @Schema(description = "Address ID", example = "1")
    private Long id;

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Type of address", example = "HOME")
    private AddressType addressType;

    @Schema(description = "Street address line 1", example = "123 Main Street")
    private String streetAddress1;

    @Schema(description = "Street address line 2", example = "Apt 4B")
    private String streetAddress2;

    @Schema(description = "City", example = "Toronto")
    private String city;

    @Schema(description = "State or province", example = "Ontario")
    private String stateProvince;

    @Schema(description = "Postal or ZIP code", example = "M5H 2N2")
    private String postalCode;

    @Schema(description = "Country", example = "Canada")
    private String country;

    @Schema(description = "Whether this is the primary address", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Valid from date", example = "2025-01-01")
    private LocalDate validFrom;

    @Schema(description = "Valid until date", example = "2025-12-31")
    private LocalDate validUntil;

    @Schema(description = "When this address was created")
    private Instant createdAt;

    @Schema(description = "When this address was last updated")
    private Instant updatedAt;
}

