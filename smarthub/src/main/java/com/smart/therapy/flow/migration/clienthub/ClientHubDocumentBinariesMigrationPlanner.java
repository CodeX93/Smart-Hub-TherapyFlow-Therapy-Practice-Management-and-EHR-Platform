package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.document.enums.DocumentScanStatus;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.DocumentBinariesTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.MappedDocumentBinaryRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ClientHubDocumentBinariesMigrationPlanner {

    DocumentBinariesMigrationPlan buildPlan(DocumentBinariesTargetState targetState) {
        int wouldCopy = 0;
        int alreadyClean = 0;
        int missingBlob = 0;
        int blocked = 0;

        for (MappedDocumentBinaryRef doc : targetState.mappedDocuments()) {
            if (doc.targetDocumentId() == null
                    || doc.targetClientId() == null
                    || doc.fileName() == null
                    || doc.fileName().isBlank()) {
                blocked++;
                continue;
            }
            DocumentScanStatus status = parseScanStatus(doc.scanStatus());
            if (status == DocumentScanStatus.CLEAN) {
                alreadyClean++;
                continue;
            }
            // Dry-run does not probe Azure; execute records missing_blob as skipped.
            wouldCopy++;
        }

        List<String> blockers = new ArrayList<>();
        if (blocked > 0) {
            blockers.add("Mapped documents missing target row / client / fileName: " + blocked);
        }

        List<String> warnings = new ArrayList<>();
        if (wouldCopy > 0) {
            warnings.add("Document binary Azure existence is probed only during execute; "
                    + "missing blobs are counted as skippedMissingBlob then");
        }

        return new DocumentBinariesMigrationPlan(
                targetState.mappedDocuments().size(),
                wouldCopy,
                alreadyClean,
                missingBlob,
                blocked,
                blockers,
                warnings);
    }

    static DocumentScanStatus parseScanStatus(String value) {
        if (value == null || value.isBlank()) {
            return DocumentScanStatus.PENDING;
        }
        try {
            return DocumentScanStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DocumentScanStatus.PENDING;
        }
    }

    record DocumentBinariesMigrationPlan(
            int mappedDocuments,
            int wouldCopy,
            int alreadyClean,
            int missingBlob,
            int blockedRows,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return blockedRows > 0 || !blockers.isEmpty();
        }
    }
}
