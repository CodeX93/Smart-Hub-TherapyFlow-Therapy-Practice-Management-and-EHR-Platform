package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalLoginResponse {
    private PortalClientResponse client;
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // Token expiration in seconds
    private Boolean mfaRequired;
    private Boolean mfaEnrollmentRequired;
    private String mfaChallengeToken;
    private String mfaMethod;
    private String mfaMaskedDestination;
    private java.util.List<String> mfaMethods;
    private Boolean mfaSmsAvailable;
    private Boolean mfaEmailAvailable;
    private String deviceTrustToken;
    @Builder.Default
    private Boolean mfaSkippedTrustedDevice = false;
    @Builder.Default
    private Boolean staySignedIn = false;
    private String message;
}

