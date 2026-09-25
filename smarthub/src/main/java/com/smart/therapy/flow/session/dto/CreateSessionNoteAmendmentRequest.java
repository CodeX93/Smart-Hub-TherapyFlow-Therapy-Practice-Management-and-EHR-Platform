package com.smart.therapy.flow.session.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSessionNoteAmendmentRequest {

    @NotBlank(message = "Amendment text is required")
    private String amendmentText;

    @NotBlank(message = "Reason is required")
    private String reason;
}
