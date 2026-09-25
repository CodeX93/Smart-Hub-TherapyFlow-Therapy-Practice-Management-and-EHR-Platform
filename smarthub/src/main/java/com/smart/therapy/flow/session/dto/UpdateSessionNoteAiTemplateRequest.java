package com.smart.therapy.flow.session.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSessionNoteAiTemplateRequest {

    @Size(max = 50, message = "Template name must be at most 50 characters")
    private String name;

    @Size(max = 20000, message = "Custom instructions must be at most 20000 characters")
    private String instructions;
}
