package com.smart.therapy.flow.report.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.report.dto.CreateReportTemplateRequest;
import com.smart.therapy.flow.report.dto.ReportTemplateResponse;
import com.smart.therapy.flow.report.dto.UpdateReportTemplateRequest;
import com.smart.therapy.flow.report.service.ReportTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
@Tag(name = "Report Templates", description = "Admin-managed AI client report templates")
public class ReportTemplateController {

    private static final String CLIENT_VIEW_ANY = PermissionConstants.CLIENT_VIEW_OWN + " or "
            + PermissionConstants.CLIENT_VIEW_TEAM + " or " + PermissionConstants.CLIENT_VIEW_ALL;

    private final ReportTemplateService templateService;

    @GetMapping
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<List<ReportTemplateResponse>> listTemplates(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (includeInactive) {
            // inactive list is admin-only — enforced below via separate auth on service call path
            // Controller-level: require USER_MANAGE for includeInactive
        }
        boolean canViewInactive = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> "USER_MANAGE".equals(a.getAuthority()));
        List<ReportTemplateResponse> templates = templateService.listTemplates(
                includeInactive && canViewInactive);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ReportTemplateResponse> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplate(id));
    }

    @PostMapping
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<ReportTemplateResponse> createTemplate(
            @Valid @RequestBody CreateReportTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        ReportTemplateResponse created = templateService.createTemplate(
                request, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<ReportTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @RequestBody UpdateReportTemplateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(templateService.updateTemplate(
                id, request, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        templateService.deleteTemplate(id, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.noContent().build();
    }
}
