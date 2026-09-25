package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.document.entity.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds notification event payloads.
 * <p>
 * HIPAA: client identity in notifications uses MRN only — never display name or email.
 */
@Component
public class NotificationPayloadFactory {

    /**
     * Puts identity fields used by notification templates.
     * Both {@code clientMrn} and {@code clientName} are set to the MRN so legacy
     * {@code {{clientName}}} templates do not leak PHI.
     */
    public static void putClientIdentity(Map<String, Object> data, Client client) {
        if (data == null || client == null) {
            return;
        }
        String mrn = client.getClientId();
        data.put("clientMrn", mrn);
        data.put("clientName", mrn);
        data.remove("fullName");
    }

    /**
     * Payload map for durable in-app storage / body rendering: prefer MRN where templates
     * still use {@code {{clientName}}}, and drop clear {@code fullName}.
     */
    public static Map<String, Object> forDurableInAppStorage(Map<String, Object> entityData) {
        if (entityData == null || entityData.isEmpty()) {
            return entityData == null ? Map.of() : entityData;
        }
        Map<String, Object> durable = new HashMap<>(entityData);
        Object mrn = durable.get("clientMrn");
        if (mrn != null && StringUtils.hasText(String.valueOf(mrn))) {
            durable.put("clientName", mrn);
            durable.remove("fullName");
        }
        Object uploadedBy = durable.get("uploadedByName");
        if (uploadedBy != null) {
            String label = String.valueOf(uploadedBy);
            boolean looksLikeEmail = label.contains("@");
            boolean looksLikeMrn = label.regionMatches(true, 0, "CL-", 0, 3);
            if (looksLikeEmail || (!looksLikeMrn && isClientUploader(durable))) {
                durable.put("uploadedByName", mrn != null ? String.valueOf(mrn) : "client");
            }
        }
        return durable;
    }

    private static boolean isClientUploader(Map<String, Object> durable) {
        Object uploadedById = durable.get("uploadedById");
        Object clientId = durable.get("clientId");
        return uploadedById != null && clientId != null && uploadedById.equals(clientId);
    }

    public Map<String, Object> documentUploaded(Document document, Long uploadedById, String uploadedByName) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", document.getId());
        data.put("clientId", document.getClient().getId());
        putClientIdentity(data, document.getClient());
        if (document.getClient() != null && document.getClient().getAssignedTherapist() != null) {
            data.put("therapistId", document.getClient().getAssignedTherapist().getId());
            data.put("therapistName", document.getClient().getAssignedTherapist().getFullName());
        } else {
            data.put("therapistName", "Unassigned");
        }
        data.put("fileName", document.getOriginalName());
        data.put("documentType", document.getDocumentType());
        data.put("uploadedById", uploadedById);
        // Client uploads: always MRN. Staff uploads: keep staff display name.
        boolean clientUploader = document.getClient() != null
                && uploadedById != null
                && uploadedById.equals(document.getClient().getId());
        String safeUploader = clientUploader
                ? HipaaAuditLabels.clientActor(document.getClient())
                : uploadedByName;
        data.put("uploadedByName", safeUploader);
        data.put("needsReview", document.getNeedsReview());
        data.put("reviewStatus", document.getReviewStatus());
        data.put("reviewDueAt", document.getReviewDueAt());
        return data;
    }
}
