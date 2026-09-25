package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubDocumentBinariesMigrationPlanner.DocumentBinariesMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.DocumentBinariesTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.MappedDocumentBinaryRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHubDocumentBinariesMigrationPlannerTest {

    private final ClientHubDocumentBinariesMigrationPlanner planner =
            new ClientHubDocumentBinariesMigrationPlanner();

    @Test
    void countsWouldCopyAlreadyCleanAndBlocked() {
        DocumentBinariesMigrationPlan plan = planner.buildPlan(new DocumentBinariesTargetState(List.of(
                mapped("1", 101L, "file.pdf", "PENDING", 11L),
                mapped("2", 102L, "file2.pdf", "CLEAN", 12L),
                mapped("3", 103L, null, "PENDING", 13L),
                mapped("4", null, "x.pdf", "PENDING", 14L))));

        assertThat(plan.mappedDocuments()).isEqualTo(4);
        assertThat(plan.wouldCopy()).isEqualTo(1);
        assertThat(plan.alreadyClean()).isEqualTo(1);
        assertThat(plan.missingBlob()).isZero();
        assertThat(plan.blockedRows()).isEqualTo(2);
        assertThat(plan.blocked()).isTrue();
        assertThat(plan.warnings()).isNotEmpty();
    }

    @Test
    void buildsCandidateBlobNames() {
        assertThat(ClientHubSourceAzureBlobClient.candidateBlobNames("42", "a.pdf", "orig.pdf"))
                .containsExactly("documents/42-a.pdf", "documents/42-orig.pdf");
        assertThat(ClientHubSourceAzureBlobClient.pickBestMatch(
                List.of("documents/42-other.pdf", "documents/42-a.pdf"), "a.pdf", "orig.pdf"))
                .isEqualTo("documents/42-a.pdf");
    }

    private MappedDocumentBinaryRef mapped(
            String legacyId, Long targetId, String fileName, String scanStatus, Long clientId) {
        return new MappedDocumentBinaryRef(
                legacyId, targetId, fileName, fileName, scanStatus, clientId, "application/pdf");
    }
}
