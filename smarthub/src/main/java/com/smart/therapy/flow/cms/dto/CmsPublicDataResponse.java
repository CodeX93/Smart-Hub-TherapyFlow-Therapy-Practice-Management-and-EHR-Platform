package com.smart.therapy.flow.cms.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Strapi-compatible public landing response wrapper: { "data": { ...fields } }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsPublicDataResponse {
    private JsonNode data;
}
