package com.smart.therapy.flow.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = false)
@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local.path:./uploads}")
    private String storageBasePath;

    @Override
    public String uploadFile(MultipartFile file, String clientId, String fileName) throws Exception {
        String sanitizedClientId = sanitizePath(clientId);
        Path clientDir = Paths.get(storageBasePath, "clients", sanitizedClientId);
        Files.createDirectories(clientDir);

        String uniqueFileName = UUID.randomUUID().toString() + safeExtension(fileName);
        Path targetPath = clientDir.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Opaque path — no original filename in storage key
        log.info("File uploaded for client folder (opaque key)");
        return targetPath.toString().replace("\\", "/");
    }

    @Override
    public InputStream downloadFile(String storagePath) throws Exception {
        Path filePath = Paths.get(storagePath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + storagePath);
        }
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public void deleteFile(String storagePath) throws Exception {
        Path filePath = Paths.get(storagePath);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("File deleted: {}", storagePath);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        return Files.exists(Paths.get(storagePath));
    }

    @Override
    public String getFileUrl(String storagePath) {
        // For local storage, return relative path or construct URL
        return "/api/documents/file/" + Paths.get(storagePath).getFileName().toString();
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
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");
        return ext.length() <= 16 ? ext : "";
    }
}

