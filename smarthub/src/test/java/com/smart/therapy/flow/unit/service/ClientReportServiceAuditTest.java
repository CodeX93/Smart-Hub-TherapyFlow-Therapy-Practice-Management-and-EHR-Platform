package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.ai.service.ConsentPolicyService;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.report.entity.ClientReport;
import com.smart.therapy.flow.report.repository.ClientReportRepository;
import com.smart.therapy.flow.report.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientReportServiceAuditTest {

    @Mock private ClientReportRepository clientReportRepository;
    @Mock private ReportTemplateService templateService;
    @Mock private ClientReportAccessService accessService;
    @Mock private ReportSupportingFileService supportingFileService;
    @Mock private ClientReportDataAggregator dataAggregator;
    @Mock private AiService aiService;
    @Mock private ConsentPolicyService consentPolicyService;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditLogService auditLogService;
    @Mock private ClientReportExportService exportService;

    @InjectMocks private ClientReportService clientReportService;

    @Test
    void auditsSuccessfulReportViews() {
        User actor = TestDataFactory.createTestTherapist();
        actor.setId(10L);
        AuthPrincipal principal = TestDataFactory.createAuthPrincipal(actor);
        Client client = TestDataFactory.createTestClientWithId(20L);
        ClientReport report = ClientReport.builder().client(client).isDraft(true).isFinalized(false).build();
        report.setId(30L);
        when(clientReportRepository.findByIdWithRelations(30L)).thenReturn(Optional.of(report));
        when(currentUserService.requireCurrentUser(principal)).thenReturn(actor);

        clientReportService.getReport(30L, principal);

        verify(auditLogService).logClientReportAccess(
                eq(10L), eq(principal.getLoginIdentifier()), eq(30L), eq(20L),
                eq("client_report_viewed"), eq(null), eq("client-report-api"), eq(java.util.Map.of()));
    }
}
