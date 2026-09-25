package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.common.entity.PlatformPhiEncryptionBackfillJob;
import com.smart.therapy.flow.common.repository.PlatformPhiEncryptionBackfillJobRepository;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhiEncryptionBackfillScheduler oid-column exclusion")
class PhiEncryptionBackfillSchedulerTest {

    @Mock
    private PlatformPhiEncryptionBackfillJobRepository jobRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private TenantSchemaHealthService tenantSchemaHealthService;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PhiEncryptionBackfillScheduler scheduler;

    @Test
    @DisplayName("seedMissingJobs never seeds the oid column patient_consents.signature_data")
    void seedMissingJobsSkipsSignatureData() {
        Organisation org = new Organisation();
        org.setSchemaName("tenant_qa_alpha");
        when(organisationRepository.findAll()).thenReturn(List.of(org));
        when(tenantSchemaHealthService.schemaExists("tenant_qa_alpha")).thenReturn(true);
        when(jobRepository.findBySchemaNameAndTableNameAndColumnName(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        scheduler.seedMissingJobs();

        ArgumentCaptor<PlatformPhiEncryptionBackfillJob> saved =
                ArgumentCaptor.forClass(PlatformPhiEncryptionBackfillJob.class);
        verify(jobRepository, atLeastOnce()).save(saved.capture());
        List<PlatformPhiEncryptionBackfillJob> jobs = saved.getAllValues();
        assertThat(jobs)
                .noneMatch(job -> "patient_consents".equals(job.getTableName())
                        && "signature_data".equals(job.getColumnName()));
        assertThat(jobs)
                .anyMatch(job -> "patient_consents".equals(job.getTableName())
                        && "signed_by".equals(job.getColumnName()));
    }

    @Test
    @DisplayName("cancelExcludedColumnJobs sweeps leftover signature_data jobs, including FAILED ones")
    void cancelExcludedColumnJobsCancelsLeftoverSignatureDataJobs() {
        PlatformPhiEncryptionBackfillJob failedJob = job("FAILED");
        PlatformPhiEncryptionBackfillJob pendingJob = job("PENDING");
        PlatformPhiEncryptionBackfillJob completedJob = job("COMPLETED");
        when(jobRepository.findRunnableJobs()).thenReturn(List.of());
        when(jobRepository.findByTableNameAndColumnName("patient_consents", "signature_data"))
                .thenReturn(List.of(failedJob, pendingJob, completedJob));

        scheduler.cancelExcludedColumnJobs();

        assertThat(failedJob.getStatus()).isEqualTo("CANCELLED");
        assertThat(failedJob.getLastError()).contains("oid large object");
        assertThat(pendingJob.getStatus()).isEqualTo("CANCELLED");
        assertThat(completedJob.getStatus()).isEqualTo("COMPLETED");
        verify(jobRepository).save(failedJob);
        verify(jobRepository).save(pendingJob);
    }

    @Test
    @DisplayName("cancelExcludedColumnJobs still cancels runnable voice_transcription jobs")
    void cancelExcludedColumnJobsStillCancelsVoiceTranscription() {
        PlatformPhiEncryptionBackfillJob voiceJob = job("PENDING");
        voiceJob.setTableName("session_notes");
        voiceJob.setColumnName("voice_transcription");
        when(jobRepository.findRunnableJobs()).thenReturn(List.of(voiceJob));
        when(jobRepository.findByTableNameAndColumnName(any(), any())).thenReturn(List.of());

        scheduler.cancelExcludedColumnJobs();

        assertThat(voiceJob.getStatus()).isEqualTo("CANCELLED");
        verify(jobRepository).save(voiceJob);
    }

    private static PlatformPhiEncryptionBackfillJob job(String status) {
        return PlatformPhiEncryptionBackfillJob.builder()
                .schemaName("tenant_qa_alpha")
                .tableName("patient_consents")
                .columnName("signature_data")
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
