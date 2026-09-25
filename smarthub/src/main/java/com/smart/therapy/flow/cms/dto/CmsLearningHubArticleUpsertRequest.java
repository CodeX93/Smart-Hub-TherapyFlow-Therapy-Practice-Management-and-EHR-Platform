package com.smart.therapy.flow.cms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsLearningHubArticleUpsertRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String slug;

    @NotNull
    private JsonNode draftContent;
}
