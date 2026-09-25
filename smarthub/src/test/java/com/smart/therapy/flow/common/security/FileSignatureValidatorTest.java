package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSignatureValidatorTest {

    @Test
    void acceptsPdfWithMatchingSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "record.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII)
        );

        assertThatCode(() -> FileSignatureValidator.validate(file))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsExecutableRenamedAsPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "record.pdf",
                "application/pdf",
                new byte[]{0x4D, 0x5A, 0x10, 0x00}
        );

        assertThatThrownBy(() -> FileSignatureValidator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void acceptsOfficeArchiveOnlyForMatchingDocumentFamily() throws Exception {
        byte[] wordArchive = officeArchive("word/document.xml");
        MockMultipartFile document = new MockMultipartFile(
                "file",
                "letter.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                wordArchive
        );
        MockMultipartFile spreadsheet = new MockMultipartFile(
                "file",
                "letter.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                wordArchive
        );

        assertThatCode(() -> FileSignatureValidator.validate(document))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> FileSignatureValidator.validate(spreadsheet))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsBinaryPayloadDeclaredAsText() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                new byte[]{'n', 'o', 't', 'e', 0, 'x'}
        );

        assertThatThrownBy(() -> FileSignatureValidator.validate(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsUnknownOctetStreamPayload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.bin",
                "application/octet-stream",
                new byte[]{0x01, 0x02, 0x03, 0x04}
        );

        assertThatThrownBy(() -> FileSignatureValidator.validate(file))
                .isInstanceOf(BadRequestException.class);
    }

    private byte[] officeArchive(String documentEntry) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(documentEntry));
            zip.write("<document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
