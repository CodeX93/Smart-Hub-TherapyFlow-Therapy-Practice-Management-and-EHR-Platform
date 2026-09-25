package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Revenue analytics month row")
@Data
public class RevenueAnalyticsMonth {
private String month;
private BigDecimal mrr;
private BigDecimal arr;
private Long activeSubscriptions;
private Long endedSubscriptions;
private BigDecimal churnRatePct;
}
