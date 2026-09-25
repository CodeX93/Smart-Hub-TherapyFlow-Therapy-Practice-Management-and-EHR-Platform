package com.smart.therapy.flow.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSessionNoteAiTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 50, message = "Template name must be at most 50 characters")
    private String name;

    @NotBlank(message = "Custom instructions are required")
    @Size(max = 20000, message = "Custom instructions must be at most 20000 characters")
    private String instructions;
}
