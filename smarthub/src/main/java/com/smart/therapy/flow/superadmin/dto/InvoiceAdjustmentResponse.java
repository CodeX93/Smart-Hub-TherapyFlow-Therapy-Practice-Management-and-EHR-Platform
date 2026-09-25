package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Invoice adjustment response")
@Data
public class InvoiceAdjustmentResponse {
private Long adjustmentId;
private Long invoiceId;
private String type;
private BigDecimal amount;
private String reason;
private String status;
private Instant createdAt;
}
