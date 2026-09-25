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
@Schema(description = "Client-specific billing summary statistics")
public class ClientBillingStatsResponse {

    @Schema(description = "Client ID", example = "123")
    private Long clientId;

    @Schema(description = "Total billing records for this client", example = "24")
    private Long totalInvoices;

    @Schema(description = "Pending invoices count", example = "2")
    private Long pendingInvoices;

    @Schema(description = "Billed invoices count", example = "5")
    private Long billedInvoices;

    @Schema(description = "Paid invoices count", example = "15")
    private Long paidInvoices;

    @Schema(description = "Denied invoices count", example = "1")
    private Long deniedInvoices;

    @Schema(description = "Follow-up invoices count", example = "1")
    private Long followUpInvoices;

    @Schema(description = "Cancelled invoices count", example = "0")
    private Long cancelledInvoices;

    @Schema(description = "Total amount billed", example = "3400.00")
    private BigDecimal totalBilledAmount;

    @Schema(description = "Total discount applied across invoices", example = "150.00")
    private BigDecimal totalDiscountAmount;

    @Schema(description = "Total amount paid", example = "2800.00")
    private BigDecimal totalPaidAmount;

    @Schema(description = "Current due amount", example = "600.00")
    private BigDecimal dueAmount;

    @Schema(description = "Current credit amount from overpaid invoices", example = "138.00")
    private BigDecimal creditAmount;
}
