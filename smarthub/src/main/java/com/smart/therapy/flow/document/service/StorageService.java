package com.smart.therapy.flow.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Interface for document storage operations.
 * Implementations can use Azure Blob Storage, local file system, or other storage backends.
 * Current implementation uses AWS S3 for file storage.
 * Previous implementations supported Azure Blob Storage and local file system.
 */
public interface StorageService {

    /**
     * Upload a file and return the storage path
     */
    String uploadFile(MultipartFile file, String clientId, String fileName) throws Exception;

    /**
     * Download a file as an InputStream
     */
    InputStream downloadFile(String storagePath) throws Exception;

    /**
     * Delete a file from storage
     */
    void deleteFile(String storagePath) throws Exception;

    /**
     * Check if a file exists
     */
    boolean fileExists(String storagePath);

    /**
     * Get file URL for direct access (if applicable)
     */
    String getFileUrl(String storagePath);
}

