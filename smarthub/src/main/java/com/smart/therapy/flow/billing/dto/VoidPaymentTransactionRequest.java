package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to void a payment transaction")
public class VoidPaymentTransactionRequest {

    @NotBlank(message = "Void reason is required")
    @Size(max = 1000, message = "Void reason must not exceed 1000 characters")
    @Schema(description = "Reason for voiding this payment transaction", example = "Duplicate manual entry")
    private String voidReason;
}
