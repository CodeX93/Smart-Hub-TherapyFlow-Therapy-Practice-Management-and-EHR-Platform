package com.smart.therapy.flow.unit.report;

import com.smart.therapy.flow.report.util.HtmlSanitizer;
import com.smart.therapy.flow.report.util.HtmlToPdfConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlToPdfConverterTest {

    @Test
    void toPdfBytes_rendersSimpleClinicalHtml() {
        String html = """
                <!DOCTYPE html><html><head><meta charset="UTF-8"/></head>
                <body><h2>Referral Information</h2><p>Information not available.</p></body></html>
                """;
        byte[] pdf = HtmlToPdfConverter.toPdfBytes(html);
        assertTrue(pdf.length > 100);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }

    @Test
    void toPdfBytes_rendersEditedDraftWithBrTags() {
        String draft = """
                <h2>Progress Report</h2>
                <h3>Overall Clinical Progress</h3>
                <p><strong>need to know about nothing&nbsp;</strong><br></p>
                <p>Information not available.</p><p><br></p>
                """;
        String sanitized = HtmlSanitizer.sanitize(draft);
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head><body><div class=\"report-content\">"
                + sanitized + "</div></body></html>";
        byte[] pdf = HtmlToPdfConverter.toPdfBytes(html);
        assertTrue(pdf.length > 500, "PDF should contain rendered draft content");
    }

    @Test
    void normalizeHtmlForPdf_convertsEditorEntitiesToXmlSafeMarkup() {
        String normalized = HtmlToPdfConverter.normalizeHtmlForPdf(
                "<p><strong>text&nbsp;</strong><br></p>");
        assertFalse(normalized.contains("&nbsp;"));
        assertTrue(normalized.contains("<br />") || normalized.contains("<br/>"));
    }
}
