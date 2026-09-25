package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalConsentResponse {
    private Long id;
    private Long clientId;
    private String consentType;
    private String consentVersion;
    private Boolean granted;
    private Instant grantedAt;
    private Instant withdrawnAt;
    private String ipAddress;
    private String userAgent;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

