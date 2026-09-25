package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminApiKeyResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminCreateApiKeyRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminIntegrationResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminIntegrationTestResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminIntegrationUpsertRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformApiKey;
import com.smart.therapy.flow.superadmin.service.SuperAdminIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin Integrations", description = "Global integrations and API key management")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminIntegrationController {

    private final SuperAdminIntegrationService integrationService;
    private final PlatformAuditService platformAuditService;

    @GetMapping("/integrations")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List integrations", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminIntegrationResponse>> listIntegrations() {
        return ResponseEntity.ok(integrationService.listIntegrations());
    }

    @GetMapping("/integrations/{integrationKey}")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get integration config", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminIntegrationResponse> getIntegration(@PathVariable String integrationKey) {
        return ResponseEntity.ok(integrationService.getIntegration(integrationKey));
    }

    @PutMapping("/integrations/{integrationKey}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert integration config", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> upsertIntegration(
            @PathVariable String integrationKey,
            @Valid @RequestBody SuperAdminIntegrationUpsertRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminIntegrationResponse saved = integrationService.upsertIntegration(
                integrationKey,
                request.getEnabled(),
                request.getClientId(),
                request.getPublishableKey(),
                request.getSecret(),
                request.getConnectClientSecret(),
                request.getConnectWebhookSecret(),
                request.getPlatformWebhookSecret(),
                request.getWebhookUrl(),
                principal != null ? principal.getAuthId() : null
        );
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "INTEGRATION_UPDATED",
                "PlatformIntegrationConfig",
                saved.getKey(),
                "enabled=" + saved.getEnabled()
        );
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/integrations/{integrationKey}/test")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Test integration connectivity", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminIntegrationTestResponse> testIntegration(@PathVariable String integrationKey) {
        SuperAdminIntegrationTestResponse result = integrationService.testIntegration(integrationKey);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api-keys")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List platform API keys", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminApiKeyResponse>> listApiKeys() {
        List<SuperAdminApiKeyResponse> body = integrationService.listApiKeys().stream()
                .map(k -> new SuperAdminApiKeyResponse(
                        k.getId(),
                        k.getKeyName(),
                        k.getKeyPrefix(),
                        integrationService.resolveApiKeyScopes(k),
                        k.getIsActive(),
                        k.getExpiresAt(),
                        k.getCreatedAt(),
                        k.getLastUsedAt(),
                        k.getRevokedAt()
                ))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/api-keys")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create platform API key", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createApiKey(
            @Valid @RequestBody SuperAdminCreateApiKeyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminIntegrationService.CreatedApiKey created = integrationService.createApiKey(
                request.getKeyName(),
                request.getScopes(),
                request.getExpiresAt(),
                principal != null ? principal.getAuthId() : null
        );
        PlatformApiKey key = created.saved();
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "API_KEY_CREATED",
                "PlatformApiKey",
                String.valueOf(key.getId()),
                "name=" + key.getKeyName()
        );
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("id", key.getId());
        body.put("name", key.getKeyName());
        body.put("keyPrefix", key.getKeyPrefix());
        body.put("apiKey", created.rawKey());
        body.put("expiresAt", key.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/api-keys/{keyId}/rotate")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Rotate platform API key", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> rotateApiKey(
            @PathVariable Long keyId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminIntegrationService.CreatedApiKey created = integrationService.rotateApiKey(
                keyId,
                principal != null ? principal.getAuthId() : null
        );
        PlatformApiKey key = created.saved();
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "API_KEY_ROTATED",
                "PlatformApiKey",
                String.valueOf(key.getId()),
                "name=" + key.getKeyName()
        );
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("id", key.getId());
        body.put("name", key.getKeyName());
        body.put("keyPrefix", key.getKeyPrefix());
        body.put("apiKey", created.rawKey());
        body.put("expiresAt", key.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/api-keys/{keyId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Revoke platform API key", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> revokeApiKey(
            @PathVariable Long keyId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        integrationService.revokeApiKey(keyId);
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "API_KEY_REVOKED",
                "PlatformApiKey",
                String.valueOf(keyId),
                ""
        );
        return ResponseEntity.ok(Map.of("success", true));
    }

}
