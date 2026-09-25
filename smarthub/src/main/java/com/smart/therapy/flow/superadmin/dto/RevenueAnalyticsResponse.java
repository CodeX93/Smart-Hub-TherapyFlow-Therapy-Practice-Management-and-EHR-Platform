package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Schema(description = "Revenue analytics response")
@Data
public class RevenueAnalyticsResponse {
private Integer months;
private Instant generatedAt;
private List<RevenueAnalyticsMonth> rows;
}
