package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
import com.smart.therapy.flow.superadmin.dto.BulkFeatureKeyRequest;
import com.smart.therapy.flow.superadmin.dto.RolloutRuleRequest;
import com.smart.therapy.flow.superadmin.dto.RolloutRuleResponse;
import com.smart.therapy.flow.superadmin.dto.UpsertRolloutsRequest;
import com.smart.therapy.flow.superadmin.entity.FeatureRolloutRule;
import com.smart.therapy.flow.superadmin.service.SuperAdminGlobalRolloutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/rollouts")
@RequiredArgsConstructor
@Tag(name = "Global Feature Rollouts", description = "Global feature rollout rules")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminGlobalRolloutController {

    private final SuperAdminGlobalRolloutService globalRolloutService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List global rollout rules", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<RolloutRuleResponse>> listGlobalRollouts() {
        List<RolloutRuleResponse> body = globalRolloutService.listGlobalRollouts().stream()
                .map(SuperAdminGlobalRolloutController::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get a single global rollout rule", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<RolloutRuleResponse> getGlobalRollout(
            @Parameter(example = "123")
            @PathVariable Long ruleId
    ) {
        FeatureRolloutRule rule = globalRolloutService.getGlobalRollout(ruleId);
        return ResponseEntity.ok(toResponse(rule));
    }

    @GetMapping("/{ruleId}/history")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "View history for a global rollout rule", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AuditLogEntryResponse>> getGlobalRolloutHistory(
            @Parameter(example = "123")
            @PathVariable Long ruleId,
            @Parameter(example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "50")
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(globalRolloutService.listGlobalRolloutRuleHistory(ruleId, page, size));
    }

    @GetMapping("/history")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "View global rollout edit history", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AuditLogEntryResponse>> listGlobalRolloutHistory(
            @Parameter(example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "50")
            @RequestParam(defaultValue = "50") int size,
            @Parameter(example = "CLIENT_PORTAL")
            @RequestParam(required = false) String featureKey
    ) {
        return ResponseEntity.ok(globalRolloutService.listGlobalRolloutHistory(page, size, featureKey));
    }

    @PostMapping
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Upsert global rollout rules", security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Enable Client Portal Globally",
                                    value = "{\"rules\":[{\"featureKey\":\"CLIENT_PORTAL\",\"enabled\":true,\"usageLimit\":null,\"startAt\":\"2026-04-03T00:00:00Z\",\"endAt\":null}]}"
                            ))
            ))
    public ResponseEntity<List<RolloutRuleResponse>> upsertGlobalRollouts(
            @Valid @RequestBody UpsertRolloutsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SuperAdminGlobalRolloutService.RolloutRuleInput> inputs = (request != null && request.getRules() != null)
                ? request.getRules().stream().map(SuperAdminGlobalRolloutController::toInput).toList()
                : List.of();

        List<RolloutRuleResponse> body = globalRolloutService
                .upsertGlobalRollouts(inputs, principal != null ? principal.getAuthId() : null)
                .stream()
                .map(SuperAdminGlobalRolloutController::toResponse)
                .toList();

        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{ruleId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Edit global rollout rule", security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Disable With End Window",
                                    value = "{\"featureKey\":\"CLIENT_PORTAL\",\"enabled\":false,\"usageLimit\":null,\"startAt\":\"2026-04-04T00:00:00Z\",\"endAt\":\"2026-05-01T00:00:00Z\"}"
                            ))
            ))
    public ResponseEntity<RolloutRuleResponse> updateGlobalRollout(
            @Parameter(example = "123")
            @PathVariable Long ruleId,
            @Valid @RequestBody RolloutRuleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SuperAdminGlobalRolloutService.RolloutRuleInput input = toInput(request);
        FeatureRolloutRule saved = globalRolloutService.updateGlobalRollout(
                ruleId,
                input,
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete global rollout rule", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Void> deleteGlobalRollout(
            @Parameter(example = "123")
            @PathVariable Long ruleId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        globalRolloutService.deleteGlobalRollout(ruleId, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-disable")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Bulk disable global rollouts by feature key", security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Disable Multiple Features",
                                    value = "{\"featureKeys\":[\"CLIENT_PORTAL\",\"ADVANCED_BILLING\"]}"
                            ))
            ))
    public ResponseEntity<List<RolloutRuleResponse>> bulkDisableGlobalRollouts(
            @Valid @RequestBody BulkFeatureKeyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<RolloutRuleResponse> body = globalRolloutService
                .bulkDisableByFeatureKeys(request.getFeatureKeys(), principal != null ? principal.getAuthId() : null)
                .stream()
                .map(SuperAdminGlobalRolloutController::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/by-feature")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Bulk remove global rollouts by feature key", security = @SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Remove Multiple Features",
                                    value = "{\"featureKeys\":[\"CLIENT_PORTAL\",\"ADVANCED_BILLING\"]}"
                            ))
            ))
    public ResponseEntity<Map<String, Object>> bulkRemoveGlobalRollouts(
            @Valid @RequestBody BulkFeatureKeyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        long deleted = globalRolloutService.bulkRemoveByFeatureKeys(
                request.getFeatureKeys(),
                principal != null ? principal.getAuthId() : null
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        body.put("featureKeys", request.getFeatureKeys());
        return ResponseEntity.ok(body);
    }

    private static RolloutRuleResponse toResponse(FeatureRolloutRule rule) {
        RolloutRuleResponse r = new RolloutRuleResponse();
        r.setId(rule.getId());
        r.setOrganisationId(rule.getOrganisation() != null ? rule.getOrganisation().getId() : null);
        r.setScope(rule.getScope() != null ? rule.getScope().name() : null);
        r.setTargetId(rule.getTargetId());
        r.setTargetKey(rule.getTargetKey());
        r.setFeatureKey(rule.getFeatureKey());
        r.setEnabled(Boolean.TRUE.equals(rule.getEnabled()));
        r.setUsageLimit(rule.getUsageLimit());
        r.setStartAt(rule.getStartAt());
        r.setEndAt(rule.getEndAt());
        r.setUpdatedAt(rule.getUpdatedAt());
        return r;
    }

    private static SuperAdminGlobalRolloutService.RolloutRuleInput toInput(RolloutRuleRequest request) {
        return new SuperAdminGlobalRolloutService.RolloutRuleInput(
                request.getFeatureKey(),
                request.getEnabled(),
                request.getUsageLimit(),
                request.getStartAt(),
                request.getEndAt()
        );
    }
}
