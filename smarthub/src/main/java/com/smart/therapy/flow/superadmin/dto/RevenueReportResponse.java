package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "Revenue report response")
@Data
public class RevenueReportResponse {
    private String period;
    private String groupBy;
    private BigDecimal mrr;
    private BigDecimal arr;
    private BigDecimal churnRate;
    private List<RevenueMovement> movements;

    @Data
    public static class RevenueMovement {
        private String key;
        private BigDecimal amount;
        private Long paidInvoices;
    }
}
