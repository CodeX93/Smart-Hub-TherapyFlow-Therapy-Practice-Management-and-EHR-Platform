package com.smart.therapy.flow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminIntegrationHealthResponse {
    private Long organisationId;
    private Instant generatedAt;
    private StripeHealth stripe;
    private ZoomHealth zoom;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StripeHealth {
        private Boolean connected;
        private String onboardingStatus;
        private Boolean chargesEnabled;
        private Boolean payoutsEnabled;
        private Boolean detailsSubmitted;
        private String connectAccountId;
        private Instant lastSyncedAt;
        private String disabledReason;
        private Boolean healthy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoomHealth {
        private Integer totalTherapists;
        private Integer configuredTherapists;
        private Integer activeTherapists;
        private Integer healthyTherapists;
        private LocalDate date;
        private Long serviceId;
        private String sessionType;
        private String timezone;
        private List<TherapistZoomStatus> therapists;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TherapistZoomStatus {
        private Long therapistId;
        private String therapistName;
        private Boolean therapistActive;
        private Boolean zoomConfigured;
        private Boolean zoomActive;
        private Boolean zoomHealthy;
        private Instant zoomLastUpdatedAt;
        private Integer availableSlotsCount;
        private List<SlotItem> availableSlots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotItem {
        private Instant time;
        private String timezone;
        private String localTime;
    }
}
