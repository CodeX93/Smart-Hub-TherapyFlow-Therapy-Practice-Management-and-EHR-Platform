package com.smart.therapy.flow.auth.controller;

import com.smart.therapy.flow.auth.dto.AuthDeviceDtos;
import com.smart.therapy.flow.auth.service.AuthKnownDeviceService;
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
@RequestMapping("/api/v1/auth/devices")
@RequiredArgsConstructor
public class AuthDeviceController {

    private final AuthKnownDeviceService authKnownDeviceService;

    @GetMapping
    public ResponseEntity<AuthDeviceDtos.DeviceListResponse> listDevices(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "X-Device-Trust-Token", required = false) String deviceTrustToken,
            HttpServletRequest request) {
        AuthPrincipal authenticated = requirePrincipal(principal);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authKnownDeviceService.listDevices(
                        authenticated.getAuthId(),
                        deviceTrustToken,
                        HttpRequestUtil.getUserAgent(request)));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> revokeDevice(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long deviceId) {
        authKnownDeviceService.revokeDevice(requirePrincipal(principal).getAuthId(), deviceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAllTrustedDevices(
            @AuthenticationPrincipal AuthPrincipal principal) {
        authKnownDeviceService.revokeAllTrustedDevices(requirePrincipal(principal).getAuthId());
        return ResponseEntity.noContent().build();
    }

    private static AuthPrincipal requirePrincipal(AuthPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }
}
