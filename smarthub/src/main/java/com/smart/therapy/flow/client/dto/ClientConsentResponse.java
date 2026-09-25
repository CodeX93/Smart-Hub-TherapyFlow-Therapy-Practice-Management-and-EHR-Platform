package com.smart.therapy.flow.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for client consent response (admin view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConsentResponse {
    private Long id;
    private String clientId;
    private String fullName;
    private String email;
    private Boolean hasPortalAccess;
    private List<ConsentInfo> consents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentInfo {
        private Long id;
        private String consentType;
        private Boolean granted;
        private Instant grantedAt;
        private Instant withdrawnAt;
        private String consentVersion;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
