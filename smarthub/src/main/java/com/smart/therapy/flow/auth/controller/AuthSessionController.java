package com.smart.therapy.flow.auth.controller;

import com.smart.therapy.flow.auth.dto.AuthSessionDtos;
import com.smart.therapy.flow.auth.service.AuthSessionQueryService;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/sessions")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AuthSessionQueryService authSessionQueryService;

    @GetMapping
    public ResponseEntity<AuthSessionDtos.SessionListResponse> listSessions(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        AuthPrincipal authenticated = requirePrincipal(principal);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authSessionQueryService.listSessions(
                        authenticated.getAuthId(),
                        authorization,
                        HttpRequestUtil.getClientIp(request),
                        HttpRequestUtil.getUserAgent(request)));
    }

    @DeleteMapping("/others")
    public ResponseEntity<Void> revokeOtherSessions(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthPrincipal authenticated = requirePrincipal(principal);
        authSessionQueryService.revokeOtherSessions(authenticated.getAuthId(), authorization);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthPrincipal authenticated = requirePrincipal(principal);
        authSessionQueryService.revokeSession(authenticated.getAuthId(), sessionId, authorization);
        return ResponseEntity.noContent().build();
    }

    private static AuthPrincipal requirePrincipal(AuthPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }
}
