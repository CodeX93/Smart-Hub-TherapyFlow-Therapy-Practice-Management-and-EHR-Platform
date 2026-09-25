package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.superadmin.entity.PlatformBackupJob;
import com.smart.therapy.flow.superadmin.repository.PlatformBackupJobRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformPasswordResetJobRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformPurgeJobRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminNotificationService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOperationsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminOperationsBackupTest {

    @Mock OrganisationRepository organisationRepository;
    @Mock PlatformBackupJobRepository backupJobRepository;
    @Mock TenantDirectoryService tenantDirectoryService;
    @Mock TenantSchemaHealthService tenantSchemaHealthService;
    @Mock PlatformAuditService platformAuditService;
    @Mock UserOrganisationRepository userOrganisationRepository;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock InvoiceRepository invoiceRepository;
    @Mock PlatformPurgeJobRepository platformPurgeJobRepository;
    @Mock PlatformPasswordResetJobRepository platformPasswordResetJobRepository;
    @Mock Optional<RedisTemplate<String, Object>> redisTemplate;
    @Mock SuperAdminNotificationService superAdminNotificationService;
    @Mock AuthIdentityRepository authIdentityRepository;
    @Mock AuthIdentityService authIdentityService;
    @Mock AuthSessionService authSessionService;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @Mock TenantTransactionExecutor tenantTransactionExecutor;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SuperAdminOperationsService service;

    @Test
    void backupRequestFailsExplicitlyAsUnsupported() {
        assertThatThrownBy(() -> service.requestBackup(10L, 20L))
                .isInstanceOf(StoryApiException.class)
                .satisfies(error -> {
                    StoryApiException exception = (StoryApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
                    assertThat(exception.getCode()).isEqualTo("AZURE_BACKUP_UNSUPPORTED");
                    assertThat(exception.getMessage()).contains("Azure backup integration");
                });
    }

    @Test
    void queuedPlaceholderBackupIsMarkedFailedWithoutStorageLocation() {
        Organisation organisation = Organisation.builder()
                .id(10L)
                .schemaName("tenant_10")
                .backupStatus("PENDING")
                .backupLocation("backup://fake/location.dump")
                .build();
        PlatformBackupJob job = PlatformBackupJob.builder()
                .id(30L)
                .organisation(organisation)
                .status("queued")
                .storageLocation("backup://fake/location.dump")
                .build();
        when(backupJobRepository.findByStatusInOrderByCreatedAtAsc(List.of("queued")))
                .thenReturn(List.of(job));

        service.runBackupsNow();

        assertThat(job.getStatus()).isEqualTo("failed");
        assertThat(job.getStorageLocation()).isNull();
        assertThat(job.getErrorMessage()).contains("unsupported");
        assertThat(organisation.getBackupStatus()).isEqualTo("FAILED");
        assertThat(organisation.getBackupLocation()).isNull();
        verify(backupJobRepository).save(job);
        verify(organisationRepository).save(organisation);
    }
}
