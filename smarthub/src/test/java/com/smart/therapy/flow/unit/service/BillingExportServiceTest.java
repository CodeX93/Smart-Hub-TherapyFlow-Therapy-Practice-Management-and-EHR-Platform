package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.subscription.entity.BillingExportJob;
import com.smart.therapy.flow.subscription.enums.BillingExportJobStatus;
import com.smart.therapy.flow.subscription.enums.BillingExportType;
import com.smart.therapy.flow.subscription.repository.BillingExportJobRepository;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.service.BillingExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingExportService Unit Tests")
class BillingExportServiceTest {

    @Mock
    private BillingExportJobRepository exportJobRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrgSubscriptionRepository orgSubscriptionRepository;
    @Mock
    private S3Client s3Client;

    private BillingExportService service;

    @BeforeEach
    void setUp() {
        service = spy(new BillingExportService(
                exportJobRepository,
                invoiceRepository,
                orgSubscriptionRepository,
                new ObjectMapper(),
                s3Client
        ));
        ReflectionTestUtils.setField(service, "tokenExpiryHours", 24L);
    }

    @Test
    @DisplayName("Should store hashed download token and return raw token once on create")
    void shouldStoreHashedTokenAndReturnRawTokenOnCreate() {
        doReturn(CompletableFuture.completedFuture(null)).when(service).processAsync(anyLong());
        when(exportJobRepository.save(any(BillingExportJob.class))).thenAnswer(invocation -> {
            BillingExportJob job = invocation.getArgument(0);
            job.setId(101L);
            return job;
        });

        BillingExportService.CreatedExportJob created = service.createJob(
                1L,
                BillingExportType.BILLING_INVOICES_CSV,
                Map.of("status", "paid")
        );

        String rawToken = created.downloadToken();
        BillingExportJob saved = created.job();
        assertThat(rawToken).isNotBlank();
        assertThat(saved.getDownloadToken()).isNotBlank();
        assertThat(saved.getDownloadToken()).isNotEqualTo(rawToken);
        assertThat(saved.getDownloadToken()).isEqualTo(sha256(rawToken));
    }

    @Test
    @DisplayName("Should allow download when provided raw token matches stored hash")
    void shouldAllowDownloadWithRawTokenAgainstHash() {
        String rawToken = "test_raw_token";
        BillingExportJob job = completedJob(55L, sha256(rawToken), Instant.now().plusSeconds(3600));
        when(exportJobRepository.findById(55L)).thenReturn(java.util.Optional.of(job));
        when(exportJobRepository.consumeDownloadToken(eq(55L), anyString(), eq(BillingExportJobStatus.COMPLETED), any(Instant.class)))
                .thenReturn(1);

        BillingExportService.DownloadTokenValidationResult resolved = service.validateAndConsumeDownloadToken(55L, rawToken);

        assertThat(resolved).isNotNull();
        assertThat(resolved.state()).isEqualTo(BillingExportService.DownloadTokenState.VALID);
        assertThat(resolved.job()).isNotNull();
        assertThat(resolved.job().getId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("Should reject download with invalid token")
    void shouldRejectInvalidDownloadToken() {
        BillingExportJob job = completedJob(77L, sha256("expected"), Instant.now().plusSeconds(3600));
        when(exportJobRepository.findById(77L)).thenReturn(java.util.Optional.of(job));

        BillingExportService.DownloadTokenValidationResult resolved = service.validateAndConsumeDownloadToken(77L, "wrong");

        assertThat(resolved.state()).isEqualTo(BillingExportService.DownloadTokenState.INVALID_TOKEN);
    }

    private static BillingExportJob completedJob(Long id, String storedToken, Instant expiresAt) {
        BillingExportJob job = new BillingExportJob();
        job.setId(id);
        job.setDownloadToken(storedToken);
        job.setTokenExpiresAt(expiresAt);
        job.setStatus(BillingExportJobStatus.COMPLETED);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        job.setTokenConsumed(false);
        return job;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash test token", e);
        }
    }
}
