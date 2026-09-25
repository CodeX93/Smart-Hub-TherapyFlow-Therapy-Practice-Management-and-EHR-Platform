package com.smart.therapy.flow.auth.dto;

import com.smart.therapy.flow.auth.enums.MfaMethod;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class MfaDtos {
    private MfaDtos() {}

    public record StatusResponse(
            boolean enabled,
            boolean enrollmentRequired,
            String enforcementMode,
            int unusedRecoveryCodes,
            String method,
            String maskedDestination,
            List<String> enrolledMethods,
            boolean smsAvailable,
            boolean emailAvailable) {}

    public record EnrollmentStartResponse(
            MfaMethod method,
            String secret,
            String provisioningUri,
            String maskedDestination,
            boolean codeSent,
            boolean smsAvailable,
            boolean emailAvailable,
            List<String> enrolledMethods) {}

    public record EnrollmentStartRequest(
            MfaMethod method,
            String phone) {}

    public record CodeRequest(@NotBlank String code) {}

    public record ConfirmResponse(
            boolean enabled,
            List<String> recoveryCodes,
            List<String> enrolledMethods,
            boolean canAddMoreMethods) {}

    public record RecoveryCodesResponse(List<String> recoveryCodes) {}

    public record LoginVerifyRequest(
            @NotBlank String mfaChallengeToken,
            @NotBlank String code,
            MfaMethod method,
            Boolean trustDevice,
            Boolean staySignedIn) {}

    public record RequiredEnrollmentStartRequest(
            @NotBlank String mfaChallengeToken,
            MfaMethod method,
            String phone) {}

    public record RequiredEnrollmentConfirmRequest(
            @NotBlank String mfaChallengeToken,
            @NotBlank String code,
            Boolean trustDevice,
            Boolean staySignedIn) {}

    public record SendLoginCodeRequest(
            @NotBlank String mfaChallengeToken,
            MfaMethod method) {}

    public record SendLoginCodeResponse(
            boolean sent,
            String method,
            String maskedDestination) {}

    public record LoginChallengeInfo(
            MfaMethod method,
            String maskedDestination,
            boolean smsAvailable,
            boolean emailAvailable,
            List<String> enrolledMethods) {}

    public record ChangeStartRequest(
            @NotBlank String currentCode,
            MfaMethod method,
            String phone) {}

    public record SendSettingsCodeResponse(
            boolean sent,
            String method,
            String maskedDestination) {}

    public record AddMethodStartRequest(
            MfaMethod method,
            String phone) {}

    public record StepUpResponse(
            String stepUpToken,
            long expiresInSeconds) {}
}
