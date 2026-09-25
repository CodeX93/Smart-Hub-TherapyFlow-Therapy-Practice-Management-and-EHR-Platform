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
public class NotificationEventCatalogResponse {

    private String scope;
    private List<EventDefinition> events;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDefinition {
        private String eventType;
        private List<String> defaultChannels;
        private boolean required;
        private boolean sessionHealthEvent;
    }
}
