package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refund history summary and entries for a billing record")
public class RefundHistoryResponse {

    @Schema(description = "Billing record ID", example = "123")
    private Long billingId;

    @Schema(description = "Total number of refund entries", example = "2")
    private Long refundCount;

    @Schema(description = "Total refunded amount", example = "75.00")
    private BigDecimal totalRefunded;

    @Schema(description = "Refund entries ordered by most recent first")
    private List<PaymentResponse> refunds;
}
