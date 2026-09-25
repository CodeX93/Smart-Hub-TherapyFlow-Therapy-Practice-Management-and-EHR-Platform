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
public class NotificationActionMetadataResponse {
    private String scope;
    private List<EntityActionDefinition> entities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityActionDefinition {
        private String relatedEntityType;
        private String actionUrlTemplate;
        private String defaultActionLabel;
        private String exampleActionUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeedResult {
        private int created;
        private int updated;
        private int skipped;
    }
}
