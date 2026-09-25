package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalServiceResponse {
    private Long id;
    private String serviceCode;
    private String serviceName;
    private String description;
    private Integer duration;
    private BigDecimal baseRate;
    // private String category;
    // category field removed - not needed in portal response
}

