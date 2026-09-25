package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.DocumentTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientHubDocumentMigrationPlanner {

    DocumentMigrationPlan buildPlan(
            SourceDocumentInventory inventory,
            List<SourceDocumentRef> sourceDocuments,
            DocumentTargetState targetState) {
        int wouldCreate = 0;
        int wouldUpdateMapped = 0;
        int blocked = 0;
        int unmappedClients = 0;
        int unmappedUploaders = 0;
        int unmappedReviewers = 0;

        for (SourceDocumentRef source : sourceDocuments) {
            if (source.missingRequiredFields()) {
                blocked++;
                continue;
            }
            if (!targetState.mappedClientIds().contains(source.clientLegacyId())) {
                blocked++;
                unmappedClients++;
                continue;
            }
            if (source.uploadedByLegacyId() != null && !targetState.mappedUserIds().contains(source.uploadedByLegacyId())) {
                blocked++;
                unmappedUploaders++;
                continue;
            }
            if (source.reviewedByLegacyId() != null && !targetState.mappedUserIds().contains(source.reviewedByLegacyId())) {
                blocked++;
                unmappedReviewers++;
                continue;
            }
            if (targetState.mappedDocumentIds().contains(source.legacyDocumentPk())) {
                wouldUpdateMapped++;
            } else {
                wouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        addBlocker(blockers, inventory.missingClientRows(), "Documents missing client_id");
        addBlocker(blockers, inventory.blankFileNameRows(), "Documents missing file_name/storage path");
        addBlocker(blockers, inventory.blankOriginalNameRows(), "Documents missing original_name");
        addBlocker(blockers, inventory.missingFileSizeRows(), "Documents missing file_size");
        addBlocker(blockers, inventory.blankCategoryRows(), "Documents missing category");
        addBlocker(blockers, unmappedClients, "Documents reference unmapped clients");
        addBlocker(blockers, unmappedUploaders, "Documents reference unmapped uploaders");
        addBlocker(blockers, unmappedReviewers, "Documents reference unmapped reviewers");
        addWarning(warnings, inventory.blankMimeTypeRows(),
                "Documents missing mime_type; will infer from filename or use application/octet-stream");

        return new DocumentMigrationPlan(
                sourceDocuments.size(),
                wouldCreate,
                wouldUpdateMapped,
                blocked,
                inventory.uploadedByRows(),
                inventory.reviewedByRows(),
                inventory.sharedInPortalRows(),
                blockers,
                warnings);
    }

    private void addBlocker(List<String> blockers, long count, String label) {
        if (count > 0) {
            blockers.add(label + ": " + count);
        }
    }

    private void addWarning(List<String> warnings, long count, String label) {
        if (count > 0) {
            warnings.add(label + ": " + count);
        }
    }

    record DocumentMigrationPlan(
            int sourceDocuments,
            int wouldCreate,
            int wouldUpdateMapped,
            int blockedRows,
            long uploadedByRows,
            long reviewedByRows,
            long sharedInPortalRows,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return blockedRows > 0 || !blockers.isEmpty();
        }
    }
}
