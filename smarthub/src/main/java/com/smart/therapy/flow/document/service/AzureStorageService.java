package com.smart.therapy.flow.document.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.smart.therapy.flow.common.security.FileSignatureValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Azure Blob Storage implementation of StorageService.
 * When {@code azure.storage.encryption-scope} is set, every upload uses that
 * customer-managed encryption scope (CMK).
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "azure", matchIfMissing = false)
@Slf4j
public class AzureStorageService implements StorageService {

    private final BlobContainerClient containerClient;
    private final String encryptionScope;
    private final String containerName;

    public AzureStorageService(
            @Value("${azure.storage.connection-string}") String connectionString,
            // Prefer azure.storage.container-name; also accept AZURE_BLOB_CONTAINER_NAME directly
            // because that env name does not relax-bind to azure.storage.* on its own.
            @Value("${azure.storage.container-name:${AZURE_BLOB_CONTAINER_NAME:documents}}") String containerName,
            @Value("${azure.storage.encryption-scope:}") String encryptionScope) {
        this.containerName = containerName;
        this.encryptionScope = StringUtils.hasText(encryptionScope) ? encryptionScope.trim() : null;

        BlobServiceClientBuilder builder = new BlobServiceClientBuilder()
                .connectionString(connectionString);
        if (this.encryptionScope != null) {
            builder.encryptionScope(this.encryptionScope);
        }
        BlobServiceClient blobServiceClient = builder.buildClient();

        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);

        if (!this.containerClient.exists()) {
            this.containerClient.create();
            log.info("Created Azure Blob Storage container: {}", containerName);
        }

        log.info("AzureStorageService initialized with container: {}, encryptionScope={}",
                containerName, this.encryptionScope != null ? this.encryptionScope : "(account default)");
    }

    @Override
    public String uploadFile(MultipartFile file, String clientId, String fileName) throws Exception {
        FileSignatureValidator.validate(file);
        String sanitizedClientId = sanitizePath(clientId);
        String sanitizedFileName = sanitizeFileName(fileName);
        String uniqueFileName = UUID.randomUUID() + safeExtension(sanitizedFileName);

        String blobName = "clients/" + sanitizedClientId + "/" + uniqueFileName;

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType(file.getContentType() != null ? file.getContentType() : getContentType(fileName));

            // Encryption scope is set on BlobServiceClientBuilder when configured;
            // uploads inherit customer-managed key (CMK) from that client.
            blobClient.upload(BinaryData.fromStream(file.getInputStream(), file.getSize()), true);
            blobClient.setHttpHeaders(headers);

            log.info("File uploaded to private Azure Blob Storage (CMK scope enforced={})",
                    encryptionScope != null);

            return blobName;
        } catch (Exception e) {
            log.error("Azure Blob upload failed: errorType={}", e.getClass().getSimpleName());
            throw new RuntimeException("Failed to upload file to Azure Blob Storage", e);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) throws Exception {
        try {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);

            if (!blobClient.exists()) {
                log.warn("Requested Azure Blob file was not found");
                throw new RuntimeException("File not found");
            }

            return blobClient.openInputStream();
        } catch (Exception e) {
            log.error("Azure Blob download failed: errorType={}", e.getClass().getSimpleName());
            throw new RuntimeException("Failed to download file from Azure Blob Storage", e);
        }
    }

    @Override
    public void deleteFile(String storagePath) throws Exception {
        try {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);

            if (blobClient.exists()) {
                blobClient.delete();
                log.info("File deleted from private Azure Blob Storage");
            } else {
                log.warn("Azure Blob file was not found for deletion");
            }
        } catch (Exception e) {
            log.error("Azure Blob deletion failed: errorType={}", e.getClass().getSimpleName());
            throw new RuntimeException("Failed to delete file from Azure Blob Storage", e);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);
            return blobClient.exists();
        } catch (Exception e) {
            log.error("Azure Blob existence check failed: errorType={}", e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public String getFileUrl(String storagePath) {
        try {
            return "/api/documents/file/" + storagePath.substring(storagePath.lastIndexOf('/') + 1);
        } catch (Exception e) {
            log.error("Azure Blob URL generation failed: errorType={}", e.getClass().getSimpleName());
            return "/api/documents/file/" + storagePath.substring(storagePath.lastIndexOf('/') + 1);
        }
    }

    private String sanitizePath(String path) {
        return path.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String safeExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int separator = fileName.lastIndexOf('.');
        if (separator < 0 || separator == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(separator).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private String getContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}
