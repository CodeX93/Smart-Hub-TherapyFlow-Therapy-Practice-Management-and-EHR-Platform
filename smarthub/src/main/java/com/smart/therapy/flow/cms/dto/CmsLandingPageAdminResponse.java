package com.smart.therapy.flow.cms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsLandingPageAdminResponse {
    private Long id;
    private JsonNode draftContent;
    private JsonNode publishedContent;
    private Instant publishedAt;
    private Instant updatedAt;
    private Long updatedByAuthId;
    private boolean hasUnpublishedChanges;
}
