package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubDocumentExecuteServiceTest {

    private final ClientHubDocumentExecuteService service =
            new ClientHubDocumentExecuteService(null, null, null, null, null);

    @Test
    void resolvesProvidedMimeType() {
        assertThat(service.resolveMimeType(document("application/pdf", "document.bin", "uploads/document.bin")))
                .isEqualTo("application/pdf");
    }

    @Test
    void infersMissingMimeTypeFromOriginalName() {
        assertThat(service.resolveMimeType(document(null, "document.pdf", "uploads/document")))
                .isEqualTo("application/pdf");
    }

    @Test
    void fallsBackWhenMimeTypeCannotBeInferred() {
        assertThat(service.resolveMimeType(document("", "document", "uploads/document")))
                .isEqualTo("application/octet-stream");
    }

    @Test
    void refusesToExecuteWithoutResolvedTargetOrganisation() {
        assertThatThrownBy(() -> service.execute(List.of(), new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }

    private SourceDocumentRecord document(String mimeType, String originalName, String fileName) {
        return new SourceDocumentRecord(
                "10",
                "100",
                null,
                fileName,
                originalName,
                1024,
                mimeType,
                "uploaded",
                false,
                0,
                false,
                false,
                null,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
