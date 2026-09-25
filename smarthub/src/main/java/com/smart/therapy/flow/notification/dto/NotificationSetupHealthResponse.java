package com.smart.therapy.flow.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSetupHealthResponse {

    private String scope;
    private List<EventSetupHealth> events;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSetupHealth {
        private String eventType;
        private boolean hasAnyTrigger;
        private boolean hasActiveTrigger;
        private boolean hasInAppTemplate;
        private boolean hasActiveInAppTemplate;
        private boolean hasEmailTemplate;
        private boolean hasActiveEmailTemplate;
        private Integer triggerCount;
        private Integer activeTriggerCount;
    }
}
