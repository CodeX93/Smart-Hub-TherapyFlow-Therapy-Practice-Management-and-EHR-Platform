package com.smart.therapy.flow.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsMediaUploadResponse {
    private Long id;
    private String url;
    private String alternativeText;
    private Integer width;
    private Integer height;
    private String filename;
    private String mimeType;
    private Long sizeBytes;
}
