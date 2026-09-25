package com.smart.therapy.flow.billing.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.billing.enums.InvoicePolicyPriceType;
import com.smart.therapy.flow.common.jackson.LenientStringDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoicePolicyRequest {

    @NotBlank(message = "Client type key is required")
    @Schema(description = "Client type option key, or `all` for every client type", example = "refugee")
    private String clientTypeKey;

    @NotBlank(message = "Client type label is required")
    @Schema(description = "Client type display label (use `All client types` when key is all)", example = "Refugee")
    private String clientTypeLabel;

    @NotBlank(message = "Appointment status key is required")
    @Schema(description = "Session/appointment status option key, or `all` for every status", example = "completed")
    private String appointmentStatusKey;

    @NotBlank(message = "Appointment status label is required")
    @Schema(description = "Status display label (use `All session statuses` when key is all)", example = "Completed")
    private String appointmentStatusLabel;

    @NotNull(message = "Enabled is required")
    private Boolean enabled;

    @NotNull(message = "Price type is required")
    private InvoicePolicyPriceType priceType;

    @NotNull(message = "Invoice price is required")
    @DecimalMin(value = "0.00", message = "Invoice price must be greater than or equal to 0")
    private BigDecimal invoicePrice;

    @Schema(description = "Optional display name for this policy rule")
    @JsonDeserialize(using = LenientStringDeserializer.class)
    private String policyName;

    @Schema(description = "Optional service scope. Null means all services. Ignored when serviceScopeKey is `all`.")
    private Long serviceId;

    @Schema(description = "Optional service scope key. Use `all` for every service (same as serviceId null).", example = "all")
    @JsonDeserialize(using = LenientStringDeserializer.class)
    private String serviceScopeKey;

    @Schema(description = "First date this policy is effective (inclusive)")
    private java.time.LocalDate effectiveFrom;

    @Schema(description = "Last date this policy is effective (inclusive)")
    private java.time.LocalDate effectiveTo;

    @Schema(description = "Higher priority policies win when multiple rules match", example = "10")
    private Integer priority;
}

