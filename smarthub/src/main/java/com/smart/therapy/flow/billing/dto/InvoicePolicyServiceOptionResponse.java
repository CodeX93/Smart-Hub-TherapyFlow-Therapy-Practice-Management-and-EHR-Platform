package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service option for invoice policy scope dropdown")
public class InvoicePolicyServiceOptionResponse {

    @Schema(description = "Service ID. Null when optionKey is all.", example = "5")
    private Long serviceId;

    @Schema(description = "Option key. Use `all` for every service.", example = "all")
    private String optionKey;

    @Schema(description = "Display label", example = "All services")
    private String optionLabel;

    @Schema(description = "Service code when scoped to a specific service", example = "PSY-60")
    private String serviceCode;

    @Schema(description = "Base rate for the service, if applicable", example = "150.00")
    private BigDecimal baseRate;

    @Schema(description = "True when this option means all services")
    private Boolean allServices;
}
