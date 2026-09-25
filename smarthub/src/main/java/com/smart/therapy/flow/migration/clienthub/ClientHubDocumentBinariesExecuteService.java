package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.DocumentScanStatus;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.document.service.StorageService;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.MappedDocumentBinaryRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientHubDocumentBinariesExecuteService {

    private static final int BATCH_SIZE = 8;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final DocumentRepository documentRepository;
    private final ClientHubSourceAzureBlobClient sourceAzureBlobClient;
    private final ObjectProvider<StorageService> storageServiceProvider;

    public DocumentBinariesExecuteResult execute(
            List<MappedDocumentBinaryRef> mappedDocuments,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required for document binaries execution");
        }
        StorageService storageService = storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            throw new IllegalStateException(
                    "StorageService bean is required for document binaries "
                            + "(set app.storage.type=azure and target container therapy-flow-private-container)");
        }

        int copied = 0;
        int alreadyClean = 0;
        int skippedMissingBlob = 0;
        int skippedBlocked = 0;
        int failed = 0;

        alreadyClean += (int) mappedDocuments.stream()
                .filter(doc -> ClientHubDocumentBinariesMigrationPlanner.parseScanStatus(doc.scanStatus())
                        == DocumentScanStatus.CLEAN)
                .count();

        List<MappedDocumentBinaryRef> pending = mappedDocuments.stream()
                .filter(doc -> ClientHubDocumentBinariesMigrationPlanner.parseScanStatus(doc.scanStatus())
                        != DocumentScanStatus.CLEAN)
                .toList();

        int batchNumber = 0;
        for (int start = 0; start < pending.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, pending.size());
            int currentBatch = ++batchNumber;
            List<MappedDocumentBinaryRef> batch = pending.subList(start, end);
            for (MappedDocumentBinaryRef ref : batch) {
                ItemResult itemResult = copyOneWithRetry(ref, storageService, target);
                switch (itemResult) {
                    case COPIED -> copied++;
                    case ALREADY_CLEAN -> alreadyClean++;
                    case MISSING_BLOB -> skippedMissingBlob++;
                    case BLOCKED -> skippedBlocked++;
                    case FAILED -> failed++;
                }
            }
            log.info("ClientHubAI document binaries batch complete: batch={} batch_size={} processed={}/{}",
                    currentBatch, batch.size(), end, pending.size());
        }

        return new DocumentBinariesExecuteResult(
                mappedDocuments.size(),
                copied,
                alreadyClean,
                skippedMissingBlob,
                skippedBlocked,
                failed);
    }

    private ItemResult copyOneWithRetry(
            MappedDocumentBinaryRef ref,
            StorageService storageService,
            TargetInventory target) {
        RuntimeException lastTransient = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return copyOne(ref, storageService, target);
            } catch (RuntimeException ex) {
                if (!isTransientFailure(ex) || attempt >= 3) {
                    if (isTransientFailure(ex)) {
                        throw ex;
                    }
                    log.warn("ClientHubAI document binary failed: legacy_id={} cause={}",
                            ref.legacyDocumentPk(), ex.getMessage());
                    return ItemResult.FAILED;
                }
                lastTransient = ex;
                log.warn("ClientHubAI document binary transient failure (legacy_id={} attempt={}/3); retrying. cause={}",
                        ref.legacyDocumentPk(), attempt, ex.getMessage());
                try {
                    Thread.sleep(3_000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw lastTransient;
    }

    private ItemResult copyOne(
            MappedDocumentBinaryRef ref,
            StorageService storageService,
            TargetInventory target) {
        if (ref.targetDocumentId() == null
                || ref.targetClientId() == null
                || ref.legacyDocumentPk() == null
                || ref.legacyDocumentPk().isBlank()
                || ref.fileName() == null
                || ref.fileName().isBlank()) {
            return ItemResult.BLOCKED;
        }

        DocumentSnapshot snapshot = tenantTransactionExecutor.executeReadOnly(
                target.organisationId(),
                target.schemaName(),
                () -> {
                    Document document = documentRepository.findById(ref.targetDocumentId()).orElse(null);
                    if (document == null) {
                        return null;
                    }
                    if (document.getScanStatus() == DocumentScanStatus.CLEAN) {
                        return new DocumentSnapshot(true, null, null, null, null);
                    }
                    return new DocumentSnapshot(
                            false,
                            document.getFileName(),
                            document.getOriginalName(),
                            document.getMimeType(),
                            document.getClient() != null ? document.getClient().getId() : ref.targetClientId());
                });
        if (snapshot == null) {
            return ItemResult.BLOCKED;
        }
        if (snapshot.alreadyClean()) {
            return ItemResult.ALREADY_CLEAN;
        }

        String logicalFileName = snapshot.fileName() != null ? snapshot.fileName() : ref.fileName();
        String originalName = snapshot.originalName() != null && !snapshot.originalName().isBlank()
                ? snapshot.originalName()
                : basename(logicalFileName);
        String mimeType = snapshot.mimeType() != null && !snapshot.mimeType().isBlank()
                ? snapshot.mimeType()
                : (ref.mimeType() != null && !ref.mimeType().isBlank()
                ? ref.mimeType()
                : "application/octet-stream");
        Long clientId = snapshot.clientId() != null ? snapshot.clientId() : ref.targetClientId();

        String blobName;
        try {
            blobName = sourceAzureBlobClient.resolveBlobName(
                    ref.legacyDocumentPk(),
                    logicalFileName,
                    originalName);
        } catch (RuntimeException ex) {
            if (isTransientFailure(ex)) {
                throw ex;
            }
            log.warn("ClientHubAI document binary blob resolve failed: legacy_id={} cause={}",
                    ref.legacyDocumentPk(), ex.getMessage());
            return ItemResult.MISSING_BLOB;
        }
        if (blobName == null) {
            log.warn("ClientHubAI document binary missing source blob: legacy_id={}", ref.legacyDocumentPk());
            return ItemResult.MISSING_BLOB;
        }

        byte[] bytes = sourceAzureBlobClient.downloadBytes(blobName);

        try {
            MultipartFile multipart = new ByteArrayMultipartFile("file", originalName, mimeType, bytes);
            String storageKey = storageService.uploadFile(
                    multipart,
                    String.valueOf(clientId),
                    originalName);
            String checksum = sha256Hex(bytes);
            int size = bytes.length;

            return tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                Document document = documentRepository.findById(ref.targetDocumentId()).orElse(null);
                if (document == null) {
                    return ItemResult.BLOCKED;
                }
                if (document.getScanStatus() == DocumentScanStatus.CLEAN) {
                    return ItemResult.ALREADY_CLEAN;
                }
                document.setFileName(storageKey);
                document.setScanStatus(DocumentScanStatus.CLEAN);
                document.setScannedAt(Instant.now());
                document.setScanDetail("Binary reconciled from ClientHubAI Azure blob");
                document.setContentChecksum(checksum);
                document.setFileSize(size);
                document.setUpdatedBy(0L);
                documentRepository.save(document);
                return ItemResult.COPIED;
            });
        } catch (Exception ex) {
            if (isTransientFailure(ex)) {
                throw (ex instanceof RuntimeException runtime) ? runtime : new RuntimeException(ex);
            }
            log.warn("ClientHubAI document binary upload/update failed: legacy_id={} cause={}",
                    ref.legacyDocumentPk(), ex.getMessage());
            return ItemResult.FAILED;
        }
    }

    private record DocumentSnapshot(
            boolean alreadyClean,
            String fileName,
            String originalName,
            String mimeType,
            Long clientId) {
    }

    private boolean isTransientFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataAccessResourceFailureException
                    || current instanceof org.springframework.dao.TransientDataAccessException
                    || current instanceof java.net.SocketException
                    || current instanceof java.net.SocketTimeoutException
                    || current instanceof java.io.IOException
                    || (current.getMessage() != null && (
                    current.getMessage().contains("I/O error")
                            || current.getMessage().contains("Connection is closed")
                            || current.getMessage().contains("This connection has been closed")
                            || current.getMessage().toLowerCase(Locale.ROOT).contains("timeout")
                            || current.getMessage().toLowerCase(Locale.ROOT).contains("temporarily")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String basename(String value) {
        if (value == null || value.isBlank()) {
            return "document.bin";
        }
        String trimmed = value.trim();
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 && slash < trimmed.length() - 1 ? trimmed.substring(slash + 1) : trimmed;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute SHA-256", ex);
        }
    }

    private enum ItemResult {
        COPIED,
        ALREADY_CLEAN,
        MISSING_BLOB,
        BLOCKED,
        FAILED
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }

    record DocumentBinariesExecuteResult(
            int mappedDocuments,
            int copied,
            int alreadyClean,
            int skippedMissingBlob,
            int skippedBlocked,
            int failed) {
    }
}
