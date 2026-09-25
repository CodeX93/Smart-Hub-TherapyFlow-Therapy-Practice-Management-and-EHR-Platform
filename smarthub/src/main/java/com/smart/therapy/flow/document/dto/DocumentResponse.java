package com.smart.therapy.flow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private Long clientId;
    private String clientName;
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String mimeType;
    private String documentType;
    private String category;
    private String description;
    private Long uploadedById;
    private String uploadedByName;
    private Instant uploadedAt;
    private Boolean needsReview;
    private String reviewStatus;
    private Instant reviewDueAt;
    private Long reviewedById;
    private String reviewedByName;
    private Instant reviewedAt;
    private Boolean shareWithClient;
    private String previewUrl;
    private String downloadUrl;
    private Instant createdAt;
    private Instant updatedAt;
}

