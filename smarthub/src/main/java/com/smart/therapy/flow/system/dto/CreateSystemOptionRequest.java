package com.smart.therapy.flow.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSystemOptionRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    @NotBlank(message = "Option key is required")
    private String optionKey;
    
    @NotBlank(message = "Option label is required")
    private String optionLabel;
    
    private Integer sortOrder;
    
    private Boolean isDefault;
    
    private Boolean isSystem;
    
    private Boolean isActive;
    
    private BigDecimal price;
}

