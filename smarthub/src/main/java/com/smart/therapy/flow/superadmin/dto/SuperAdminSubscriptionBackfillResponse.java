package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Stripe subscription backfill result for an organisation")
public class SuperAdminSubscriptionBackfillResponse {
    private boolean success;
    private boolean notFound;
    private boolean alreadyProvisioned;
    private String providerSubscriptionId;
    private int invoicesSynced;
}
