package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.report.entity.ReportSupportingFile;
import com.smart.therapy.flow.report.repository.ReportSupportingFileRepository;
import com.smart.therapy.flow.report.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSupportingFileServiceAuditTest {

    @Mock private ReportSupportingFileRepository supportingFileRepository;
    @Mock private ReportDocumentExtractionService extractionService;
    @Mock private ReportTemplateService templateService;
    @Mock private StorageService storageService;
    @Mock private ClientReportAccessService accessService;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private ReportSupportingFileService supportingFileService;

    @Test
    void auditsSuccessfulSupportingFileDownloads() throws Exception {
        User actor = TestDataFactory.createTestTherapist();
        actor.setId(10L);
        AuthPrincipal principal = TestDataFactory.createAuthPrincipal(actor);
        Client client = TestDataFactory.createTestClientWithId(20L);
        ReportSupportingFile file = ReportSupportingFile.builder()
                .client(client)
                .fileBlobName("opaque-blob-key")
                .fileSize(123)
                .build();
        file.setId(30L);
        when(supportingFileRepository.findById(30L)).thenReturn(Optional.of(file));
        when(storageService.downloadFile("opaque-blob-key"))
                .thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        when(currentUserService.requireCurrentUser(principal)).thenReturn(actor);

        supportingFileService.downloadFile(30L, principal);

        verify(auditLogService).logClientReportAccess(
                eq(10L), eq(principal.getLoginIdentifier()), eq(null), eq(20L),
                eq("report_supporting_file_downloaded"), eq(null),
                eq("report-supporting-file-api"),
                eq(Map.of("fileId", 30L, "fileSizeBytes", 123)));
    }
}
