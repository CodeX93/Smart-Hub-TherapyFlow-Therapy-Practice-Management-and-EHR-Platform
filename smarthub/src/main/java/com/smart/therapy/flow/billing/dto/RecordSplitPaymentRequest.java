package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RecordSplitPaymentRequest {

    @Schema(description = "Client-side payment leg")
    private SplitPaymentLegRequest clientLeg;

    @Schema(description = "Insurance-side payment leg")
    private SplitPaymentLegRequest insuranceLeg;

    @Schema(description = "Shared notes applied to both legs", example = "Client paid today and insurance paid after EOB")
    private String notes;

    @Schema(description = "Allow recording payment for a zero-balance bill (requires overrideReason)", example = "false")
    private Boolean allowZeroBillOverpayment;

    @Schema(description = "Mandatory reason when overriding zero-balance guard", example = "Advance deposit for upcoming sessions")
    private String overrideReason;
}

