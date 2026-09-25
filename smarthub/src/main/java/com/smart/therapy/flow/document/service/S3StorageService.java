package com.smart.therapy.flow.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * S3 implementation of StorageService.
 * Stores files in AWS S3 bucket.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = false)
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.presigned-url-expiration-hours:24}")
    private int presignedUrlExpirationHours;

    @Value("${aws.s3.sse-kms-key-id:}")
    private String sseKmsKeyId;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(MultipartFile file, String clientId, String fileName) throws Exception {
        String sanitizedClientId = sanitizePath(clientId);
        String sanitizedFileName = sanitizeFileName(fileName);
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFileName;
        
        // S3 key format: clients/{clientId}/{uniqueFileName}
        String s3Key = "clients/" + sanitizedClientId + "/" + uniqueFileName;
        
        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType() != null ? file.getContentType() : getContentType(fileName))
                    .contentLength(file.getSize())
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS);
            if (sseKmsKeyId != null && !sseKmsKeyId.isBlank()) {
                requestBuilder.ssekmsKeyId(sseKmsKeyId.trim());
            }
            PutObjectRequest request = requestBuilder.build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("File uploaded to S3 with SSE-KMS: bucket={}, key={}", bucketName, s3Key);
            
            // Return the S3 key as the storage path
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: key={}", s3Key, e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) throws Exception {
        // storagePath is the S3 key
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.warn("File not found in S3: key={}", storagePath);
            throw new RuntimeException("File not found: " + storagePath, e);
        } catch (Exception e) {
            log.error("Failed to download file from S3: key={}", storagePath, e);
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String storagePath) throws Exception {
        // storagePath is the S3 key
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            s3Client.deleteObject(request);
            log.info("File deleted from S3: key={}", storagePath);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: key={}", storagePath, e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        // storagePath is the S3 key
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking if file exists in S3: key={}", storagePath, e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String storagePath) {
        // Generate a presigned URL for direct access
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(storagePath)
                            .build())
                    .build();

            S3Presigner presigner = S3Presigner.create();
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", storagePath, e);
            // Return a fallback URL format
            return "/api/documents/file/" + storagePath.substring(storagePath.lastIndexOf('/') + 1);
        }
    }

    private String sanitizePath(String path) {
        return path.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
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

