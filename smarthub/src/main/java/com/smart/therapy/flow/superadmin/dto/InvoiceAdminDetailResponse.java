package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Subscription invoice detail for super-admin (ledger row + adjustments/disputes)")
@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceAdminDetailResponse extends InvoiceAdminResponse {
    private List<InvoiceAdjustmentResponse> adjustments = new ArrayList<>();
    private List<InvoiceDisputeResponse> disputes = new ArrayList<>();
}
