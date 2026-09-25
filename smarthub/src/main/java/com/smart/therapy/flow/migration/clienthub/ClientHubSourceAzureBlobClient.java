package com.smart.therapy.flow.migration.clienthub;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientHubSourceAzureBlobClient {

    private final ClientHubMigrationProperties properties;

    private volatile BlobContainerClient containerClient;

    public String resolveBlobName(String legacyDocumentId, String fileName, String originalName) {
        BlobContainerClient container = container();
        for (String candidate : candidateBlobNames(legacyDocumentId, fileName, originalName)) {
            BlobClient blob = container.getBlobClient(candidate);
            if (Boolean.TRUE.equals(blob.exists())) {
                return candidate;
            }
        }

        String prefix = "documents/" + legacyDocumentId + "-";
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);
        List<String> matches = new ArrayList<>();
        for (BlobItem item : container.listBlobs(options, null)) {
            if (item.getName() != null && item.getName().startsWith(prefix)) {
                matches.add(item.getName());
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        return pickBestMatch(matches, fileName, originalName);
    }

    public byte[] downloadBytes(String blobName) {
        if (blobName == null || blobName.isBlank()) {
            throw new IllegalArgumentException("blobName is required");
        }
        BlobClient blob = container().getBlobClient(blobName);
        if (!Boolean.TRUE.equals(blob.exists())) {
            throw new IllegalStateException("Source blob not found: " + blobName);
        }
        return blob.downloadContent().toBytes();
    }

    static List<String> candidateBlobNames(String legacyDocumentId, String fileName, String originalName) {
        Set<String> candidates = new LinkedHashSet<>();
        if (legacyDocumentId == null || legacyDocumentId.isBlank()) {
            return List.of();
        }
        addCandidate(candidates, legacyDocumentId, fileName);
        addCandidate(candidates, legacyDocumentId, originalName);
        return List.copyOf(candidates);
    }

    private static void addCandidate(Set<String> candidates, String legacyDocumentId, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        candidates.add("documents/" + legacyDocumentId + "-" + trimmed);
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0 && slash < trimmed.length() - 1) {
            candidates.add("documents/" + legacyDocumentId + "-" + trimmed.substring(slash + 1));
        }
    }

    static String pickBestMatch(List<String> matches, String fileName, String originalName) {
        String preferredFile = basename(fileName);
        String preferredOriginal = basename(originalName);
        for (String match : matches) {
            if (preferredFile != null && match.endsWith("-" + preferredFile)) {
                return match;
            }
        }
        for (String match : matches) {
            if (preferredOriginal != null && match.endsWith("-" + preferredOriginal)) {
                return match;
            }
        }
        return matches.get(0);
    }

    private static String basename(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 && slash < trimmed.length() - 1 ? trimmed.substring(slash + 1) : trimmed;
    }

    private BlobContainerClient container() {
        BlobContainerClient existing = containerClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (containerClient == null) {
                String connectionString = properties.getSourceAzureConnectionString();
                if (connectionString == null || connectionString.isBlank()) {
                    throw new IllegalStateException(
                            "clienthub.migration.source-azure-connection-string is required for document binaries");
                }
                String containerName = properties.getSourceAzureContainerName();
                if (containerName == null || containerName.isBlank()) {
                    containerName = "documents";
                }
                containerClient = new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient()
                        .getBlobContainerClient(containerName.trim());
                log.info("ClientHubAI source Azure blob client ready: container={}",
                        containerName.trim().toLowerCase(Locale.ROOT));
            }
            return containerClient;
        }
    }
}
