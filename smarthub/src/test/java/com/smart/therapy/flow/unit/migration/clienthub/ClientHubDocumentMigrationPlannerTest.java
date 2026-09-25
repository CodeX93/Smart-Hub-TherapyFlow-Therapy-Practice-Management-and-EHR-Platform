package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubDocumentMigrationPlanner.DocumentMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.DocumentTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubDocumentMigrationPlannerTest {

    private final ClientHubDocumentMigrationPlanner planner = new ClientHubDocumentMigrationPlanner();

    @Test
    void countsCreatesAndMappedUpdates() {
        DocumentMigrationPlan plan = planner.buildPlan(
                inventory(2, 0, 0, 0, 0, 0, 0, 1, 1, 1),
                List.of(
                        document("10", "100", "200", null),
                        document("11", "101", null, "201")),
                new DocumentTargetState(Set.of("100", "101"), Set.of("200", "201"), Set.of("11")));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.wouldUpdateMapped()).isEqualTo(1);
        assertThat(plan.uploadedByRows()).isEqualTo(1);
        assertThat(plan.reviewedByRows()).isEqualTo(1);
        assertThat(plan.sharedInPortalRows()).isEqualTo(1);
    }

    @Test
    void blocksMissingFieldsAndUnmappedDependencies() {
        DocumentMigrationPlan plan = planner.buildPlan(
                inventory(4, 1, 1, 1, 1, 1, 1, 2, 2, 0),
                List.of(
                        new SourceDocumentRef("10", null, null, null, "", "", null, "", ""),
                        document("11", "missing-client", null, null),
                        document("12", "100", "missing-uploader", null),
                        document("13", "100", null, "missing-reviewer")),
                new DocumentTargetState(Set.of("100"), Set.of(), Set.of()));

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockedRows()).isEqualTo(4);
        assertThat(plan.blockers())
                .contains("Documents missing client_id: 1")
                .contains("Documents missing file_name/storage path: 1")
                .contains("Documents missing original_name: 1")
                .contains("Documents missing file_size: 1")
                .contains("Documents missing category: 1")
                .contains("Documents reference unmapped clients: 1")
                .contains("Documents reference unmapped uploaders: 1")
                .contains("Documents reference unmapped reviewers: 1");
        assertThat(plan.warnings())
                .contains("Documents missing mime_type; will infer from filename or use application/octet-stream: 1");
    }

    @Test
    void treatsMissingMimeTypeAsFallbackWarningOnly() {
        DocumentMigrationPlan plan = planner.buildPlan(
                inventory(1, 0, 0, 0, 0, 1, 0, 0, 0, 0),
                List.of(new SourceDocumentRef(
                        "10",
                        "100",
                        null,
                        null,
                        "uploads/10.pdf",
                        "document-10.pdf",
                        1024,
                        "",
                        "uploaded")),
                new DocumentTargetState(Set.of("100"), Set.of(), Set.of()));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.wouldCreate()).isEqualTo(1);
        assertThat(plan.warnings())
                .contains("Documents missing mime_type; will infer from filename or use application/octet-stream: 1");
    }

    private SourceDocumentInventory inventory(
            long documents,
            long missingClient,
            long blankFileName,
            long blankOriginalName,
            long missingFileSize,
            long blankMimeType,
            long blankCategory,
            long uploadedByRows,
            long reviewedByRows,
            long sharedRows) {
        return new SourceDocumentInventory(
                documents,
                missingClient,
                blankFileName,
                blankOriginalName,
                missingFileSize,
                blankMimeType,
                blankCategory,
                uploadedByRows,
                reviewedByRows,
                sharedRows);
    }

    private SourceDocumentRef document(String legacyPk, String clientId, String uploadedById, String reviewedById) {
        return new SourceDocumentRef(
                legacyPk,
                clientId,
                uploadedById,
                reviewedById,
                "uploads/" + legacyPk + ".pdf",
                "document-" + legacyPk + ".pdf",
                1024,
                "application/pdf",
                "uploaded");
    }
}
