package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "Invoice dispute request")
@Data
public class UpsertInvoiceDisputeRequest {
    private String externalCaseId;
    private String status;

    @DecimalMin(value = "0.01")
    private BigDecimal amountUsd;

    @NotBlank
    @Size(min = 5, max = 500)
    private String reason;
}
