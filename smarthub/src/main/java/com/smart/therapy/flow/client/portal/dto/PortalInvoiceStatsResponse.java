package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalInvoiceStatsResponse {
    private long totalInvoices;
    private BigDecimal totalBilled;
    private BigDecimal totalPaid;
}
