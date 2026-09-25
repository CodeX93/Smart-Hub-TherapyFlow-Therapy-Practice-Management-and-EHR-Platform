package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.BillingExportJobStatus;
import com.smart.therapy.flow.subscription.enums.BillingExportType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Billing export job response")
@Data
public class BillingExportJobResponse {
    private Long jobId;
    private BillingExportType exportType;
    private BillingExportJobStatus status;
    private String fileName;
    private String downloadToken;
    private Instant tokenExpiresAt;
    private String errorMessage;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
