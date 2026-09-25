package com.smart.therapy.flow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Invoice credit/refund request")
@Data
public class InvoiceAdjustmentRequest {
    @NotNull
    @DecimalMin(value = "0.01")
    @JsonAlias("amount")
    private BigDecimal amountUsd;

    @NotBlank
    @Size(min = 5, max = 500)
    private String reason;
}
