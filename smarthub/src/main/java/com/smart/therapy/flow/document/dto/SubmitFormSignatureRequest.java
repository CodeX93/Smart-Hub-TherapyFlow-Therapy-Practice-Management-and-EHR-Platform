package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitFormSignatureRequest {
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotBlank(message = "Signature data is required")
    private String signatureData; // Base64 encoded signature image
    
    @NotBlank(message = "Signer name is required")
    private String signerName;
    
    @NotBlank(message = "Signer role is required")
    private String signerRole; // client, guardian, therapist
    
    @NotNull(message = "Agreed to terms must be specified")
    private Boolean agreedToTerms;
}

