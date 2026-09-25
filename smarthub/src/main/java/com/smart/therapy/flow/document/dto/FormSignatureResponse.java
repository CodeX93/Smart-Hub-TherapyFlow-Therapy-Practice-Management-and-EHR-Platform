package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "Form signature response")
public class FormSignatureResponse {
    private Long id;
    private Long assignmentId;
    private String signatureData; // Base64 encoded
    private String signatureImageUrl; // Frontend-friendly render URL
    private String signerName;
    private String signerRole; // client, guardian, therapist
    private String ipAddress;
    private String userAgent;
    private Instant signedAt;
    private Boolean agreedToTerms;
    private Instant createdAt;
}

