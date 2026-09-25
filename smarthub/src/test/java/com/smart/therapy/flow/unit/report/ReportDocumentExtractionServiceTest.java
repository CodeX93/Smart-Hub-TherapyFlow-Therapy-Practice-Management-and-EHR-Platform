package com.smart.therapy.flow.unit.report;

import com.smart.therapy.flow.report.service.ReportDocumentExtractionService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ReportDocumentExtractionServiceTest {

    private final ReportDocumentExtractionService service = new ReportDocumentExtractionService();

    @Test
    void extractTxt_normalizesWhitespace() {
        byte[] bytes = "  Hello   world \n\n section ".getBytes(StandardCharsets.UTF_8);
        String text = service.extractDocumentText(bytes, "text/plain", "note.txt");
        assertEquals("Hello world section", text);
    }

    @Test
    void rejectsEmptyTxt() {
        byte[] bytes = "   ".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> service.extractDocumentText(bytes, "text/plain", "empty.txt"));
    }

    @Test
    void supportsTemplateTypes() {
        assertTrue(service.isSupportedTemplateType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "report.docx"));
        assertTrue(service.isSupportedTemplateType("application/pdf", "report.pdf"));
        assertFalse(service.isSupportedTemplateType("text/plain", "report.txt"));
    }
}
