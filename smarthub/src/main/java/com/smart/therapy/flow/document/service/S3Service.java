package com.smart.therapy.flow.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String key, byte[] file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(getContentType(key))
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file));
            logger.info("Successfully uploaded file with key: {}", key);
        } catch (Exception e) {
            logger.error("Error uploading file with key: {}", key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public void uploadFile(String key, InputStream inputStream, long contentLength) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(getContentType(key))
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            logger.info("Successfully uploaded file with key: {}", key);
        } catch (Exception e) {
            logger.error("Error uploading file with key: {}", key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public byte[] downloadFile(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            byte[] data = response.readAllBytes();
            logger.info("Successfully downloaded file with key: {}", key);
            return data;
        } catch (NoSuchKeyException e) {
            logger.warn("File not found with key: {}", key);
            throw new RuntimeException("File not found in S3", e);
        } catch (Exception e) {
            logger.error("Error downloading file with key: {}", key, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    public InputStream downloadFileAsStream(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            logger.warn("File not found with key: {}", key);
            throw new RuntimeException("File not found in S3", e);
        } catch (Exception e) {
            logger.error("Error downloading file stream with key: {}", key, e);
            throw new RuntimeException("Failed to download file stream from S3", e);
        }
    }

    public boolean fileExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Error checking if file exists with key: {}", key, e);
            throw new RuntimeException("Failed to check if file exists in S3", e);
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            logger.info("Successfully deleted file with key: {}", key);
        } catch (Exception e) {
            logger.error("Error deleting file with key: {}", key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    public String generatePresignedUrl(String key, Duration expiration) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .build();

            S3Presigner presigner = S3Presigner.create();
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            logger.error("Error generating presigned URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    public List<String> listFiles(String prefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error listing files with prefix: {}", prefix, e);
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }

    public long getFileSize(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            return response.contentLength();
        } catch (NoSuchKeyException e) {
            throw new RuntimeException("File not found in S3", e);
        } catch (Exception e) {
            logger.error("Error getting file size for key: {}", key, e);
            throw new RuntimeException("Failed to get file size from S3", e);
        }
    }

    private String getContentType(String key) {
        String extension = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
}
