package com.smart.therapy.flow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private Long expiresIn;
    /** Tenant schema bound into the JWT (e.g. tenant_42). "public" for platform-only sessions. */
    private String tenantSchema;
    /** Organisation id bound into the JWT; null for platform-only sessions. */
    private Long organisationId;
    /** Organisation slug for frontend routing (when tenant-bound). */
    private String organisationSlug;
    @Builder.Default
    private Boolean passwordChangeRequired = false;
    private String changePasswordToken;
    @Builder.Default
    private Boolean mfaRequired = false;
    private String mfaChallengeToken;
    @Builder.Default
    private Boolean mfaEnrollmentRequired = false;
    private String mfaMethod;
    private String mfaMaskedDestination;
    private List<String> mfaMethods;
    @Builder.Default
    private Boolean mfaSmsAvailable = false;
    @Builder.Default
    private Boolean mfaEmailAvailable = false;
    /** Opaque token for the client to store; present only when trust was newly granted. */
    private String deviceTrustToken;
    /** True when MFA was skipped because a valid trusted device token was presented. */
    @Builder.Default
    private Boolean mfaSkippedTrustedDevice = false;
    @Builder.Default
    private Boolean staySignedIn = false;
    private String message;
    /** Present after first MFA enrollment confirm when recovery codes were generated. */
    private List<String> mfaRecoveryCodes;
    /** Enrolled MFA methods after required-enrollment confirm. */
    private List<String> enrolledMethods;
    @Builder.Default
    private Boolean canAddMoreMethods = false;
    @Builder.Default
    private Boolean statusSuccess = false;
}

