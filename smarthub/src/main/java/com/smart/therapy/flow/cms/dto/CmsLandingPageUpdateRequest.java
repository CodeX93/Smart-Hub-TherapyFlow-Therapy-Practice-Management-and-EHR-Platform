package com.smart.therapy.flow.cms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsLandingPageUpdateRequest {
    @NotNull
    private JsonNode draftContent;
}
