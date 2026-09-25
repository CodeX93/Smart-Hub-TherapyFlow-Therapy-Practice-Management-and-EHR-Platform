package com.smart.therapy.flow.publicsite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSiteServiceResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Boolean enabled;
    private Integer displayOrder;
    private Boolean isSystem;
    private Integer durationMinutes;
    private BigDecimal baseRate;
}
