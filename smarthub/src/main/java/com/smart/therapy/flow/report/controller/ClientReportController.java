package com.smart.therapy.flow.report.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.report.dto.*;
import com.smart.therapy.flow.report.service.ClientReportService;
import com.smart.therapy.flow.report.service.ReportSupportingFileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Client Reports", description = "AI-generated client reports and supporting files")
public class ClientReportController {

    private static final String CLIENT_VIEW_ANY = PermissionConstants.CLIENT_VIEW_OWN + " or "
            + PermissionConstants.CLIENT_VIEW_TEAM + " or " + PermissionConstants.CLIENT_VIEW_ALL;

    private final ClientReportService clientReportService;
    private final ReportSupportingFileService supportingFileService;

    @GetMapping("/api/v1/clients/{clientId}/reports")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<List<ClientReportResponse>> listReports(
            @PathVariable Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(clientReportService.listReports(clientId, principal));
    }

    @PostMapping("/api/v1/clients/{clientId}/reports/generate")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ClientReportResponse> generateReport(
            @PathVariable Long clientId,
            @RequestBody GenerateClientReportRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        ClientReportResponse created = clientReportService.generateReport(
                clientId, request, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/v1/clients/{clientId}/supporting-files")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<List<ReportSupportingFileResponse>> listSupportingFiles(
            @PathVariable Long clientId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(supportingFileService.listFiles(clientId, principal));
    }

    @PostMapping("/api/v1/clients/{clientId}/supporting-files")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ReportSupportingFileResponse> uploadSupportingFile(
            @PathVariable Long clientId,
            @Valid @RequestBody UploadSupportingFileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        ReportSupportingFileResponse created = supportingFileService.uploadFile(
                clientId, request, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/v1/reports/{reportId}")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ClientReportResponse> getReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(clientReportService.getReport(reportId, principal));
    }

    @PutMapping("/api/v1/reports/{reportId}")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ClientReportResponse> updateDraft(
            @PathVariable Long reportId,
            @RequestBody UpdateClientReportDraftRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(clientReportService.updateDraft(
                reportId, request, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)));
    }

    @PostMapping("/api/v1/reports/{reportId}/finalize")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ClientReportResponse> finalizeReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(clientReportService.finalizeReport(
                reportId, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)));
    }

    @PostMapping("/api/v1/reports/{reportId}/unfinalize")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<ClientReportResponse> unfinalizeReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(clientReportService.unfinalizeReport(
                reportId, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest)));
    }

    @DeleteMapping("/api/v1/reports/{reportId}")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        clientReportService.deleteReport(reportId, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/reports/{reportId}/download/pdf")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        ClientReportDownloadResult download = clientReportService.downloadPdf(
                reportId, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        contentDispositionAttachment(download.getFilename()))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .body(download.getContent());
    }

    @GetMapping("/api/v1/reports/{reportId}/download/docx")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<Resource> downloadDocx(
            @PathVariable Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) throws Exception {
        byte[] docx = clientReportService.downloadDocx(
                reportId, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        String filename = clientReportService.resolveDocxFilename(reportId, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(filename))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(new org.springframework.core.io.ByteArrayResource(docx));
    }

    @GetMapping("/api/v1/supporting-files/{fileId}/download")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<Resource> downloadSupportingFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthPrincipal principal) throws Exception {
        Resource resource = supportingFileService.downloadFile(fileId, principal);
        String filename = supportingFileService.resolveDownloadFilename(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/api/v1/supporting-files/{fileId}")
    @PreAuthorize(CLIENT_VIEW_ANY)
    public ResponseEntity<Void> deleteSupportingFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        supportingFileService.deleteFile(fileId, principal,
                HttpRequestUtil.getClientIp(httpRequest),
                HttpRequestUtil.getUserAgent(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private static String contentDispositionAttachment(String filename) {
        String safe = filename != null ? filename.replace("\"", "") : "download";
        return "attachment; filename=\"" + safe + "\"";
    }
}
