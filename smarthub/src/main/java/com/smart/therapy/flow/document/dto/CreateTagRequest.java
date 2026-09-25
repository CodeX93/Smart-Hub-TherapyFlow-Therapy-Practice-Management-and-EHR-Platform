package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new library tag
 */
@Data
public class CreateTagRequest {
    
    @NotBlank(message = "Tag name is required")
    @Size(min = 1, max = 255, message = "Tag name must be between 1 and 255 characters")
    private String name;
}
