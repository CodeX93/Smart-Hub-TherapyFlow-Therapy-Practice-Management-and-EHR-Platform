package com.smart.therapy.flow.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private Long id;
    private String serviceCode;
    private String serviceName;
    private String description;
    private Integer durationInMinutes;
    private BigDecimal baseRate;
    private Boolean isActive;
    private Boolean therapistVisible;
    private Boolean clientPortalVisible;
    private Boolean publicSiteEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}

