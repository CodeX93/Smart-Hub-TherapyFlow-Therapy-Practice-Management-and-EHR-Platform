package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Invoice dispute response")
@Data
public class InvoiceDisputeResponse {
private Long disputeId;
private Long invoiceId;
private String externalCaseId;
private String status;
private BigDecimal amountUsd;
private String reason;
private Instant openedAt;
private Instant resolvedAt;
}
