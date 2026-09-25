package com.smart.therapy.flow.subscription.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.subscription.entity.BillingExportJob;
import com.smart.therapy.flow.subscription.enums.BillingExportJobStatus;
import com.smart.therapy.flow.subscription.enums.BillingExportType;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.BillingExportJobRepository;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingExportService {

    private final BillingExportJobRepository exportJobRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final ObjectMapper objectMapper;
    private final S3Client s3Client;

    @Value("${app.storage.local.path:./uploads}")
    private String localStoragePath;

    @Value("${app.storage.type:s3}")
    private String storageType;

    @Value("${aws.s3.bucket:}")
    private String s3Bucket;

    @Value("${billing.exports.token-expiry-hours:24}")
    private long tokenExpiryHours;

    public record CreatedExportJob(BillingExportJob job, String downloadToken) {}

    public enum DownloadTokenState {
        VALID,
        JOB_NOT_FOUND,
        INVALID_TOKEN,
        TOKEN_EXPIRED,
        TOKEN_CONSUMED,
        EXPORT_NOT_READY
    }

    public record DownloadTokenValidationResult(DownloadTokenState state, BillingExportJob job) {}

    @Transactional
    public CreatedExportJob createJob(Long requestedByAuthId, BillingExportType exportType, Map<String, Object> payload) {
        if (exportType == null) {
            throw new IllegalArgumentException("Unsupported exportType: null");
        }
        Instant now = Instant.now();
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        BillingExportJob job = new BillingExportJob();
        job.setExportType(exportType);
        job.setStatus(BillingExportJobStatus.QUEUED);
        job.setRequestedByAuthId(requestedByAuthId);
        try {
            job.setRequestPayload(payload != null ? objectMapper.writeValueAsString(payload) : "{}");
        } catch (Exception e) {
            job.setRequestPayload("{}");
        }
        job.setDownloadToken(hashToken(rawToken));
        job.setTokenExpiresAt(now.plusSeconds(tokenExpiryHours * 3600));
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job = exportJobRepository.save(job);
        processAsync(job.getId());
        return new CreatedExportJob(job, rawToken);
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processAsync(Long jobId) {
        BillingExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            job.setStatus(BillingExportJobStatus.IN_PROGRESS);
            job.setUpdatedAt(Instant.now());
            exportJobRepository.save(job);

            Map<String, Object> payload = parsePayload(job.getRequestPayload());
            String csv = switch (job.getExportType()) {
                case BILLING_INVOICES_CSV -> buildInvoicesCsv(payload);
                case REVENUE_ANALYTICS_CSV -> buildRevenueAnalyticsCsv(payload);
                default -> throw new IllegalArgumentException("Unsupported export type: " + job.getExportType());
            };

            String fileName = "billing-export-" + job.getId() + "-" + System.currentTimeMillis() + ".csv";
            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

            String artifactPath;
            if ("s3".equalsIgnoreCase(storageType) && s3Bucket != null && !s3Bucket.isBlank()) {
                String key = "exports/" + fileName;
                PutObjectRequest put = PutObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(key)
                        .contentType("text/csv")
                        .build();
                s3Client.putObject(put, RequestBody.fromBytes(bytes));
                artifactPath = "s3://" + s3Bucket + "/" + key;
            } else {
                Path exportsDir = Path.of(localStoragePath, "exports");
                Files.createDirectories(exportsDir);
                Path out = exportsDir.resolve(fileName);
                Files.write(out, bytes);
                artifactPath = out.toAbsolutePath().toString();
            }

            job.setFileName(fileName);
            job.setFilePath(artifactPath);
            job.setStatus(BillingExportJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            exportJobRepository.save(job);
        } catch (Exception e) {
            log.error("Billing export job failed id={}", jobId, e);
            job.setStatus(BillingExportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setUpdatedAt(Instant.now());
            exportJobRepository.save(job);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public BillingExportJob getJob(Long jobId) {
        return exportJobRepository.findById(jobId).orElse(null);
    }

    @Transactional(readOnly = true)
    public DownloadTokenValidationResult validateAndConsumeDownloadToken(Long jobId, String token) {
        if (token == null || token.isBlank()) {
            return new DownloadTokenValidationResult(DownloadTokenState.INVALID_TOKEN, null);
        }
        BillingExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new DownloadTokenValidationResult(DownloadTokenState.JOB_NOT_FOUND, null);
        }
        String stored = job.getDownloadToken();
        String hashedProvided = hashToken(token);
        if (!constantTimeEquals(stored, hashedProvided)) {
            return new DownloadTokenValidationResult(DownloadTokenState.INVALID_TOKEN, null);
        }
        if (Boolean.TRUE.equals(job.getTokenConsumed())) {
            return new DownloadTokenValidationResult(DownloadTokenState.TOKEN_CONSUMED, null);
        }
        Instant now = Instant.now();
        if (job.getTokenExpiresAt() == null || job.getTokenExpiresAt().isBefore(now)) {
            return new DownloadTokenValidationResult(DownloadTokenState.TOKEN_EXPIRED, null);
        }
        if (job.getStatus() != BillingExportJobStatus.COMPLETED) {
            return new DownloadTokenValidationResult(DownloadTokenState.EXPORT_NOT_READY, null);
        }

        int updated = exportJobRepository.consumeDownloadToken(
                job.getId(),
                hashedProvided,
                BillingExportJobStatus.COMPLETED,
                now
        );
        if (updated == 1) {
            BillingExportJob consumedJob = exportJobRepository.findById(job.getId()).orElse(job);
            return new DownloadTokenValidationResult(DownloadTokenState.VALID, consumedJob);
        }

        BillingExportJob refreshed = exportJobRepository.findById(job.getId()).orElse(null);
        if (refreshed == null) {
            return new DownloadTokenValidationResult(DownloadTokenState.JOB_NOT_FOUND, null);
        }
        if (Boolean.TRUE.equals(refreshed.getTokenConsumed())) {
            return new DownloadTokenValidationResult(DownloadTokenState.TOKEN_CONSUMED, null);
        }
        if (refreshed.getTokenExpiresAt() == null || refreshed.getTokenExpiresAt().isBefore(now)) {
            return new DownloadTokenValidationResult(DownloadTokenState.TOKEN_EXPIRED, null);
        }
        if (refreshed.getStatus() != BillingExportJobStatus.COMPLETED) {
            return new DownloadTokenValidationResult(DownloadTokenState.EXPORT_NOT_READY, null);
        }
        return new DownloadTokenValidationResult(DownloadTokenState.INVALID_TOKEN, null);
    }

    @Transactional(readOnly = true)
    public byte[] readArtifact(BillingExportJob job) throws Exception {
        if (job == null || job.getFilePath() == null || job.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Missing export artifact path");
        }
        String path = job.getFilePath();
        if (path.startsWith("s3://")) {
            String noPrefix = path.substring("s3://".length());
            int slash = noPrefix.indexOf('/');
            if (slash <= 0) {
                throw new IllegalArgumentException("Invalid s3 artifact path");
            }
            String bucket = noPrefix.substring(0, slash);
            String key = noPrefix.substring(slash + 1);
            GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
            try (ResponseInputStream<?> in = s3Client.getObject(get)) {
                return in.readAllBytes();
            }
        }
        Path local = Path.of(path);
        return Files.readAllBytes(local);
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            if (payload == null || payload.isBlank()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String buildInvoicesCsv(Map<String, Object> payload) {
        Long organisationId = parseLong(payload.get("organisationId"));
        InvoiceStatus status = parseInvoiceStatus(payload.get("status"));

        List<Invoice> invoices = invoiceRepository.search(
                organisationId,
                status,
                PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("invoiceId,organisationId,subscriptionId,status,amount,dueDate,billingPeriodStart,billingPeriodEnd,createdAt\n");
        for (Invoice i : invoices) {
            csv.append(i.getId()).append(",")
                    .append(i.getSubscription().getOrganisation().getId()).append(",")
                    .append(i.getSubscription().getId()).append(",")
                    .append(escapeCsv(i.getStatus() != null ? i.getStatus().name() : null)).append(",")
                    .append(i.getAmount()).append(",")
                    .append(i.getDueDate()).append(",")
                    .append(i.getBillingPeriodStart()).append(",")
                    .append(i.getBillingPeriodEnd()).append(",")
                    .append(i.getCreatedAt())
                    .append("\n");
        }
        return csv.toString();
    }

    private String buildRevenueAnalyticsCsv(Map<String, Object> payload) {
        Integer months = parseInt(payload.get("months"));
        int window = Math.max(1, Math.min(months != null ? months : 12, 36));
        StringBuilder csv = new StringBuilder();
        csv.append("month,mrr,arr,activeSubscriptions,endedSubscriptions,churnRatePct\n");

        for (int i = window - 1; i >= 0; i--) {
            LocalDate monthStartDate = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(i);
            Instant monthStart = monthStartDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant monthEnd = monthStartDate.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            List<Invoice> monthInvoices = invoiceRepository.findByCreatedRange(monthStart, monthEnd);
            BigDecimal mrr = monthInvoices.stream()
                    .filter(inv -> InvoiceStatus.PAID.equals(inv.getStatus()))
                    .map(inv -> inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

            long activeStart = orgSubscriptionRepository.countActiveAt(monthStart);
            long endedThisMonth = orgSubscriptionRepository.countEndedBetween(monthStart, monthEnd);
            BigDecimal churn = activeStart > 0
                    ? BigDecimal.valueOf((endedThisMonth * 100.0) / activeStart).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            csv.append(monthStartDate).append(",")
                    .append(mrr).append(",")
                    .append(arr).append(",")
                    .append(activeStart).append(",")
                    .append(endedThisMonth).append(",")
                    .append(churn).append("\n");
        }
        return csv.toString();
    }

    private static Long parseLong(Object v) {
        try {
            return v != null ? Long.valueOf(String.valueOf(v)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInt(Object v) {
        try {
            return v != null ? Integer.valueOf(String.valueOf(v)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static InvoiceStatus parseInvoiceStatus(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.isBlank()) {
            return null;
        }
        if ("VOID".equals(normalized)) {
            return InvoiceStatus.VOID;
        }
        try {
            return InvoiceStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String escapeCsv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash export token", e);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
