package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "System health panel response")
public class SuperAdminSystemHealthPanelResponse {

    @Schema(description = "Overall system state", example = "Operational")
    private String overallStatus;

    @Schema(description = "Open incident count")
    private long openIncidents;

    @Schema(description = "Service indicators")
    private List<ServiceIndicator> services;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Service health indicator")
    public static class ServiceIndicator {
        @Schema(description = "Service display name", example = "API Gateway")
        private String name;

        @Schema(description = "Service status", example = "Operational")
        private String status;
    }
}
