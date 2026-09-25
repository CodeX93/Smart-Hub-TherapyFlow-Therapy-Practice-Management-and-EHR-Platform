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
@Schema(description = "Billing statistics and dashboard data")
public class BillingStatisticsResponse {

    @Schema(description = "Total outstanding balance (pending + partial payments)", example = "5000.00")
    private BigDecimal outstandingBalance;

    @Schema(description = "Total customer credit from overpaid invoices", example = "138.00")
    private BigDecimal creditBalance;

    @Schema(description = "Total amount collected (paid billing records)", example = "25000.00")
    private BigDecimal totalCollected;

    @Schema(description = "Number of active clients with billing records", example = "45")
    private Long activeClients;

    @Schema(description = "Total number of billing records", example = "120")
    private Long totalBillingRecords;

    @Schema(description = "Number of pending billing records", example = "15")
    private Long pendingRecords;

    @Schema(description = "Number of paid billing records", example = "85")
    private Long paidRecords;

    @Schema(description = "Number of partially paid billing records (payment received, balance remaining)", example = "4")
    private Long partialRecords;

    @Schema(description = "Number of denied billing records", example = "5")
    private Long deniedRecords;

    @Schema(description = "Number of records requiring follow-up", example = "10")
    private Long followUpRecords;
}
