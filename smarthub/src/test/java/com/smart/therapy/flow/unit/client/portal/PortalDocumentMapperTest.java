package com.smart.therapy.flow.unit.client.portal;

import com.smart.therapy.flow.document.enums.DocumentType;
import com.smart.therapy.flow.client.portal.util.PortalDocumentMapper;
import com.smart.therapy.flow.document.dto.DocumentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortalDocumentMapper")
class PortalDocumentMapperTest {

    @Test
    @DisplayName("Defaults missing document type to OTHER")
    void defaultsMissingDocumentType() {
        assertThat(PortalDocumentMapper.parseDocumentType(null)).isEqualTo(DocumentType.OTHER);
        assertThat(PortalDocumentMapper.parseDocumentType("")).isEqualTo(DocumentType.OTHER);
    }

    @Test
    @DisplayName("Parses snake_case aliases")
    void parsesSnakeCaseAliases() {
        assertThat(PortalDocumentMapper.parseDocumentType("insurance_card")).isEqualTo(DocumentType.INSURANCE_CARD);
        assertThat(PortalDocumentMapper.parseDocumentType("id_document")).isEqualTo(DocumentType.OTHER);
    }

    @Test
    @DisplayName("Adds relative portal preview and download URLs when no request context")
    void addsRelativePortalUrls() {
        DocumentResponse response = DocumentResponse.builder().id(42L).build();

        PortalDocumentMapper.enrichPortalUrls(response);

        assertThat(response.getPreviewUrl()).isEqualTo("/api/v1/portal/documents/42/view");
        assertThat(response.getDownloadUrl()).isEqualTo("/api/v1/portal/documents/42/download");
    }
}
