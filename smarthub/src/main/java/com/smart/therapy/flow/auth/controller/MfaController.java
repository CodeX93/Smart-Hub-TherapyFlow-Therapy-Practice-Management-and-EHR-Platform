package com.smart.therapy.flow.auth.controller;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.MfaService;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
public class MfaController {
    private final MfaService mfaService;
    private final AuthenticationService authenticationService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/verify-login")
    public ResponseEntity<JwtAuthenticationResponse> verifyLogin(
            @Valid @RequestBody MfaDtos.LoginVerifyRequest request,
            HttpServletRequest httpRequest) {
        return noStore(authenticationService.verifyMfaLogin(
                request.mfaChallengeToken(),
                request.code(),
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest),
                request.method(),
                request.trustDevice(),
                request.staySignedIn()));
    }

    @PostMapping("/send-login-code")
    public ResponseEntity<MfaDtos.SendLoginCodeResponse> sendLoginCode(
            @Valid @RequestBody MfaDtos.SendLoginCodeRequest request) {
        return noStore(mfaService.sendLoginCode(request.mfaChallengeToken(), request.method()));
    }

    @PostMapping("/required-enrollment")
    public ResponseEntity<MfaDtos.EnrollmentStartResponse> startRequiredEnrollment(
            @Valid @RequestBody MfaDtos.RequiredEnrollmentStartRequest request) {
        MfaMethod method = request.method() != null ? request.method() : MfaMethod.TOTP;
        return noStore(mfaService.startRequiredEnrollment(
                request.mfaChallengeToken(), method, request.phone()));
    }

    @PostMapping("/required-enrollment/confirm")
    public ResponseEntity<JwtAuthenticationResponse> confirmRequiredEnrollment(
            @Valid @RequestBody MfaDtos.RequiredEnrollmentConfirmRequest request,
            HttpServletRequest httpRequest) {
        return noStore(authenticationService.completeRequiredMfaEnrollment(
                request.mfaChallengeToken(),
                request.code(),
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest),
                request.trustDevice(),
                request.staySignedIn()));
    }

    @GetMapping("/status")
    public ResponseEntity<MfaDtos.StatusResponse> status(
            @AuthenticationPrincipal AuthPrincipal principal) {
        AuthPrincipal authenticated = requirePrincipal(principal);
        return ResponseEntity.ok(mfaService.status(
                authenticated.getAuthId(), authenticated.getAuthorities()));
    }

    @PostMapping("/enrollment")
    public ResponseEntity<MfaDtos.EnrollmentStartResponse> startEnrollment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody(required = false) MfaDtos.EnrollmentStartRequest request) {
        AuthPrincipal authenticated = requirePrincipal(principal);
        MfaMethod method = request != null && request.method() != null
                ? request.method() : MfaMethod.TOTP;
        String phone = request != null ? request.phone() : null;
        return noStore(mfaService.startEnrollment(
                authenticated.getAuthId(), authenticated.getLoginIdentifier(), method, phone, null));
    }

    @PostMapping("/enrollment/confirm")
    public ResponseEntity<MfaDtos.ConfirmResponse> confirmEnrollment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request) {
        return noStore(mfaService.confirmEnrollment(
                requirePrincipal(principal).getAuthId(), request.code()));
    }

    @DeleteMapping
    public ResponseEntity<Void> disable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request) {
        mfaService.disable(requirePrincipal(principal).getAuthId(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send-settings-code")
    public ResponseEntity<MfaDtos.SendSettingsCodeResponse> sendSettingsCode(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return noStore(mfaService.sendSettingsCode(requirePrincipal(principal).getAuthId()));
    }

    @PostMapping("/change/start")
    public ResponseEntity<MfaDtos.EnrollmentStartResponse> startChange(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.ChangeStartRequest request) {
        MfaMethod method = request.method() != null ? request.method() : MfaMethod.TOTP;
        return noStore(mfaService.startChange(
                requirePrincipal(principal).getAuthId(),
                request.currentCode(),
                method,
                request.phone()));
    }

    @PostMapping("/change/confirm")
    public ResponseEntity<MfaDtos.ConfirmResponse> confirmChange(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return noStore(mfaService.confirmChange(
                requirePrincipal(principal).getAuthId(),
                request.code(),
                extractJti(authorization)));
    }

    @PostMapping("/recovery-codes")
    public ResponseEntity<MfaDtos.RecoveryCodesResponse> regenerateRecoveryCodes(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request) {
        return noStore(mfaService.regenerateRecoveryCodes(
                requirePrincipal(principal).getAuthId(), request.code()));
    }

    @PostMapping("/step-up")
    public ResponseEntity<MfaDtos.StepUpResponse> stepUp(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MfaDtos.CodeRequest request) {
        return noStore(mfaService.createStepUpToken(
                requirePrincipal(principal).getAuthId(), request.code()));
    }

    private String extractJti(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            return tokenProvider.getJtiFromToken(authorization.substring(7).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static AuthPrincipal requirePrincipal(AuthPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
