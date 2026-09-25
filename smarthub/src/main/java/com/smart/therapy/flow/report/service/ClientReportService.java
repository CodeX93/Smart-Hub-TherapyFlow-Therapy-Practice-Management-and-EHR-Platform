package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.ai.service.ConsentPolicyService;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.exception.AiConsentRequiredException;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.report.dto.ClientReportDownloadResult;
import com.smart.therapy.flow.report.dto.ClientReportResponse;
import com.smart.therapy.flow.report.dto.GenerateClientReportRequest;
import com.smart.therapy.flow.report.dto.UpdateClientReportDraftRequest;
import com.smart.therapy.flow.report.entity.ClientReport;
import com.smart.therapy.flow.report.entity.ReportSupportingFile;
import com.smart.therapy.flow.report.entity.ReportTemplate;
import com.smart.therapy.flow.report.repository.ClientReportRepository;
import com.smart.therapy.flow.report.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientReportService {

    private final ClientReportRepository clientReportRepository;
    private final ReportTemplateService templateService;
    private final ClientReportAccessService accessService;
    private final ReportSupportingFileService supportingFileService;
    private final ClientReportDataAggregator dataAggregator;
    private final AiService aiService;
    private final ConsentPolicyService consentPolicyService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ClientReportExportService exportService;

    @Transactional(readOnly = true)
    public List<ClientReportResponse> listReports(Long clientId, AuthPrincipal requester) {
        accessService.requireClientAccess(clientId, requester);
        List<ClientReportResponse> reports = clientReportRepository.findByClient_IdOrderByGeneratedAtDesc(clientId).stream()
                .map(this::toListResponse)
                .toList();
        User actor = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                actor.getId(), requester.getLoginIdentifier(), null, clientId,
                "client_reports_viewed", null, "client-report-api",
                Map.of("resultCount", reports.size()));
        return reports;
    }

    @Transactional(readOnly = true)
    public ClientReportResponse getReport(Long reportId, AuthPrincipal requester) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        User actor = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                actor.getId(), requester.getLoginIdentifier(), reportId, report.getClient().getId(),
                "client_report_viewed", null, "client-report-api", Map.of());
        return toDetailResponse(report);
    }

    @Transactional
    public ClientReportResponse generateReport(Long clientId, GenerateClientReportRequest request,
            AuthPrincipal requester, String ipAddress, String userAgent) {
        if (request == null || request.getTemplateId() == null) {
            throw new BadRequestException("templateId is required");
        }

        Client client = accessService.requireClientAccess(clientId, requester);
        ReportTemplate template = templateService.requireActiveTemplate(request.getTemplateId());

        try {
            consentPolicyService.requireAiConsent(clientId);
        } catch (ForbiddenException ex) {
            User user = currentUserService.requireCurrentUser(requester);
            auditLogService.logClientReportAccess(
                    user.getId(), requester.getLoginIdentifier(), null, clientId,
                    "ai_processing_blocked", ipAddress, userAgent,
                    Map.of("reason", "consent_not_granted", "consentType", "AI_PROCESSING"));
            throw new AiConsentRequiredException(ex.getMessage());
        }

        boolean includeProfile = resolveFlag(
                request.getSources() != null ? request.getSources().getIncludeProfile() : null,
                template.getDefaultIncludeProfile(), true);
        boolean includeNotes = resolveFlag(
                request.getSources() != null ? request.getSources().getIncludeNotes() : null,
                template.getDefaultIncludeNotes(), true);
        boolean includeAssessments = resolveFlag(
                request.getSources() != null ? request.getSources().getIncludeAssessments() : null,
                template.getDefaultIncludeAssessments(), true);

        List<ReportSupportingFile> supportingFiles = supportingFileService.resolveSupportingFiles(
                clientId, request.getSupportingFileIds());

        ClientReportDataAggregator.AggregatedReportData data = dataAggregator.aggregate(
                client, template, includeProfile, includeNotes, includeAssessments, supportingFiles);

        String generatedHtml;
        if (!data.hasUsableSourceData()) {
            // No profile / notes / assessments / supporting files — do not call AI (avoids hallucinations).
            generatedHtml = dataAggregator.buildUnavailableNarrativeHtml(template);
            log.info(
                    "Client report generation skipped AI for client {} template {} (no source blocks)",
                    clientId, template.getId());
        } else {
            generatedHtml = aiService.generateClientReportFromTemplate(template, data);
        }
        // Assessment source off → force Assessment Findings section (do not infer from notes).
        generatedHtml = dataAggregator.enforceExcludedSourceSections(generatedHtml, data);
        // Exact profile + referral from client record (not AI); missing fields use "_".
        generatedHtml = dataAggregator.injectExactProfileAndReferralSections(generatedHtml, client);
        generatedHtml = HtmlSanitizer.sanitize(generatedHtml);

        User creator = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ClientReport report = ClientReport.builder()
                .client(client)
                .template(template)
                .templateName(template.getName())
                .generatedContent(generatedHtml)
                .isDraft(true)
                .isFinalized(false)
                .generatedAt(Instant.now())
                .createdByUser(creator)
                .build();

        ClientReport saved = clientReportRepository.save(report);

        auditLogService.logClientReportAccess(
                creator.getId(), requester.getLoginIdentifier(), saved.getId(), clientId,
                "client_report_generated", ipAddress, userAgent,
                Map.of("templateId", template.getId()));

        return toDetailResponse(saved);
    }

    @Transactional
    public ClientReportResponse updateDraft(Long reportId, UpdateClientReportDraftRequest request,
            AuthPrincipal requester, String ipAddress, String userAgent) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        if (Boolean.TRUE.equals(report.getIsFinalized())) {
            throw new BadRequestException("Cannot edit a finalized report");
        }
        String sanitized = HtmlSanitizer.sanitize(request != null ? request.getDraftContent() : null);
        report.updateDraft(sanitized);
        ClientReport saved = clientReportRepository.save(report);

        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                user.getId(), requester.getLoginIdentifier(), saved.getId(), saved.getClient().getId(),
                "client_report_draft_saved", ipAddress, userAgent, Map.of());

        return toDetailResponse(saved);
    }

    @Transactional
    public ClientReportResponse finalizeReport(Long reportId, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        if (Boolean.TRUE.equals(report.getIsFinalized())) {
            throw new BadRequestException("Report is already finalized");
        }
        User finalizedBy = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        report.finalizeReport(finalizedBy);
        ClientReport saved = clientReportRepository.save(report);

        auditLogService.logClientReportAccess(
                finalizedBy.getId(), requester.getLoginIdentifier(), saved.getId(), saved.getClient().getId(),
                "client_report_finalized", ipAddress, userAgent, Map.of());

        return toDetailResponse(saved);
    }

    @Transactional
    public ClientReportResponse unfinalizeReport(Long reportId, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        loadReportWithAccess(reportId, requester);
        throw new BadRequestException(
                "Finalized client reports are immutable; create an amendment instead");
    }

    @Transactional
    public void deleteReport(Long reportId, AuthPrincipal requester, String ipAddress, String userAgent) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        if (Boolean.TRUE.equals(report.getIsFinalized())) {
            throw new BadRequestException(
                    "Finalized client reports cannot be deleted; create an amendment instead");
        }
        Long clientId = report.getClient().getId();
        clientReportRepository.delete(report);

        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                user.getId(), requester.getLoginIdentifier(), reportId, clientId,
                "client_report_deleted", ipAddress, userAgent, Map.of());
    }

    @Transactional(readOnly = true)
    public ClientReportDownloadResult downloadPdf(Long reportId, AuthPrincipal requester,
            String ipAddress, String userAgent) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                user.getId(), requester.getLoginIdentifier(), reportId, report.getClient().getId(),
                "client_report_downloaded", ipAddress, userAgent, Map.of("format", "pdf"));

        String filename = exportService.resolvePdfFilename(report);
        try {
            return ClientReportDownloadResult.builder()
                    .content(exportService.buildPdf(report))
                    .contentType("application/pdf")
                    .filename(filename)
                    .build();
        } catch (Exception ex) {
            log.warn("Client report PDF rendering failed for report {}, falling back to printable HTML", reportId, ex);
            return ClientReportDownloadResult.builder()
                    .content(exportService.buildPrintableHtml(report).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .contentType("text/html;charset=utf-8")
                    .filename(filename.replace(".pdf", ".html"))
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public String generatePdfHtml(Long reportId, AuthPrincipal requester, String ipAddress, String userAgent) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                user.getId(), requester.getLoginIdentifier(), reportId, report.getClient().getId(),
                "client_report_downloaded", ipAddress, userAgent, Map.of("format", "html"));
        return exportService.buildPrintableHtml(report);
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocx(Long reportId, AuthPrincipal requester, String ipAddress, String userAgent)
            throws Exception {
        ClientReport report = loadReportWithAccess(reportId, requester);
        User user = currentUserService.requireCurrentUser(requester);
        auditLogService.logClientReportAccess(
                user.getId(), requester.getLoginIdentifier(), reportId, report.getClient().getId(),
                "client_report_downloaded", ipAddress, userAgent, Map.of("format", "docx"));
        return exportService.buildDocx(report);
    }

    @Transactional(readOnly = true)
    public String resolveDocxFilename(Long reportId, AuthPrincipal requester) {
        ClientReport report = loadReportWithAccess(reportId, requester);
        return exportService.resolveDocxFilename(report);
    }

    private ClientReport loadReportWithAccess(Long reportId, AuthPrincipal requester) {
        ClientReport report = clientReportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Client report not found"));
        accessService.requireClientAccess(report.getClient().getId(), requester);
        return report;
    }

    private boolean resolveFlag(Boolean requestValue, Boolean templateDefault, boolean fallback) {
        if (requestValue != null) {
            return requestValue;
        }
        if (templateDefault != null) {
            return templateDefault;
        }
        return fallback;
    }

    private ClientReportResponse toListResponse(ClientReport report) {
        String createdByName = report.getCreatedByUser() != null ? report.getCreatedByUser().getFullName() : null;
        return ClientReportResponse.builder()
                .id(report.getId())
                .clientId(report.getClient().getId())
                .templateId(report.getTemplate() != null ? report.getTemplate().getId() : null)
                .templateName(report.getTemplateName())
                .isDraft(report.getIsDraft())
                .isFinalized(report.getIsFinalized())
                .generatedAt(report.getGeneratedAt())
                .editedAt(report.getEditedAt())
                .finalizedAt(report.getFinalizedAt())
                .createdById(report.getCreatedByUser() != null ? report.getCreatedByUser().getId() : null)
                .createdByName(createdByName)
                .build();
    }

    private ClientReportResponse toDetailResponse(ClientReport report) {
        ClientReportResponse response = toListResponse(report);
        response.setGeneratedContent(report.getGeneratedContent());
        response.setDraftContent(report.getDraftContent());
        response.setFinalContent(report.getFinalContent());
        response.setFinalizedById(report.getFinalizedByUser() != null ? report.getFinalizedByUser().getId() : null);
        if (report.getTemplate() != null) {
            response.setTemplate(templateService.toResponse(report.getTemplate()));
        }
        return response;
    }
}
