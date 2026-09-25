package com.smart.therapy.flow.system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSystemOptionRequest {
    private Long categoryId;
    private String optionKey;
    private String optionLabel;
    private Integer sortOrder;
    private Boolean isDefault;
    private Boolean isActive;
    private BigDecimal price;
}
