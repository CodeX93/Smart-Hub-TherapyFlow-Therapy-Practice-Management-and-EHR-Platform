package com.smart.therapy.flow.unit.assessment;

import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentReport;
import com.smart.therapy.flow.assessment.entity.AssessmentTemplate;
import com.smart.therapy.flow.assessment.util.AssessmentReportDocxBuilder;
import com.smart.therapy.flow.assessment.util.AssessmentReportHtmlBuilder;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.report.util.HtmlToPdfConverter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentReportHtmlBuilderTest {

    @Test
    void formatReportContent_stripsSubjectPreambleAndConvertsSections() {
        String raw = """
                Assessment Report
                Client: Sample Client
                Assessment: Mental Health Assessment
                Date: September 16, 2026
                ---
                CLIENT INFORMATION:
                - Subject: Client - Age Range: N/A - Assessment: Mental Health Assessment - Total Score: 0.00
                ---
                SECTION: Referral
                The client was referred for psychotherapy.
                SECTION: Informed Consent
                Consent was reviewed with the client.
                """;

        String html = AssessmentReportHtmlBuilder.formatReportContent(raw);

        assertTrue(html.contains("<h2>REFERRAL</h2>"));
        assertTrue(html.contains("<h2>INFORMED CONSENT</h2>"));
        assertTrue(html.contains("The client was referred for psychotherapy."));
        assertFalse(html.contains("SECTION:"));
        assertFalse(html.toLowerCase().contains("subject:"));
        assertFalse(html.toLowerCase().contains("total score"));
        assertFalse(html.toLowerCase().contains("client: sample client"));
        assertFalse(html.contains("CLIENT INFORMATION:"));
    }

    @Test
    void build_matchesClientHubLayoutAndRendersPdfAndDocx() {
        Client client = Client.builder()
                .fullName("Syed Aqeel Haider")
                .clientId("CL-2026-0999")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender("male")
                .build();
        client.setId(99L);

        User clinician = User.builder().fullName("Eva Carlin").build();
        clinician.setId(7L);

        AssessmentTemplate template = AssessmentTemplate.builder()
                .name("Mental Health Assessment")
                .build();
        template.setId(3L);

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .client(client)
                .template(template)
                .assignedBy(clinician)
                .completedAt(Instant.parse("2026-09-16T15:00:00Z"))
                .build();
        assignment.setId(158L);

        AssessmentReport report = AssessmentReport.builder()
                .assignment(assignment)
                .isFinalized(true)
                .finalizedAt(Instant.parse("2026-09-16T16:00:00Z"))
                .finalContent("""
                        SECTION: Referral
                        The client was referred for a mental health assessment.
                        SECTION: Treatment Plan
                        1. Improve sleep quality.
                        2. Address anxiety.
                        """)
                .build();
        report.setId(12L);

        Map<String, String> practice = Map.of(
                "name", "Resilience Counseling Research & Consultation",
                "address", "111 Waterloo St Unit 406, London, ON N6B 2M4",
                "phone", "+1 (548)866-0366",
                "email", "mail@resiliencec.com",
                "website", "https://www.resiliencec.com");

        String html = AssessmentReportHtmlBuilder.build(
                assignment, report, practice, null, "Male");

        assertTrue(html.contains("Clinical Assessment Report"));
        assertTrue(html.contains("Finalized"));
        assertTrue(html.contains("CLIENT INFORMATION"));
        assertTrue(html.contains("Syed Aqeel Haider"));
        assertTrue(html.contains("CL-2026-0999"));
        assertTrue(html.contains("PERSONAL AND CONFIDENTIAL"));
        assertTrue(html.contains("www.resiliencec.com"));
        assertFalse(html.contains("https://www.resiliencec.com"));
        assertTrue(html.contains("+1 (548)866-0366"));
        assertTrue(html.contains("Digitally signed on"));
        assertTrue(html.contains("<h2>REFERRAL</h2>"));
        assertTrue(html.contains("<h2>TREATMENT PLAN</h2>"));
        assertTrue(html.contains("margin-top:30px")); // ClientHub signature/footer spacing
        assertTrue(html.contains("padding:20px 30px")); // ClientHub body padding

        byte[] pdf = assertDoesNotThrow(() -> HtmlToPdfConverter.toPdfBytes(html));
        assertTrue(pdf.length > 500);
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));

        byte[] docx = assertDoesNotThrow(() -> AssessmentReportDocxBuilder.build(
                assignment, report, practice, null, "Male"));
        assertTrue(docx.length > 500);
        assertTrue(docx[0] == 'P' && docx[1] == 'K');
        // PDF-matching Word chrome is present in document.xml
        String docXml = assertDoesNotThrow(() -> {
            try (java.util.zip.ZipInputStream zis =
                         new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(docx))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if ("word/document.xml".equals(entry.getName())) {
                        return new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
            return "";
        });
        assertTrue(docXml.contains("Clinical Assessment Report"));
        assertTrue(docXml.contains("CLIENT INFORMATION"));
        assertTrue(docXml.contains("PERSONAL AND CONFIDENTIAL"));
        assertTrue(docXml.contains("2563EB")); // blue header/signature border
        assertTrue(docXml.contains("FEF3C7")); // confidentiality banner fill
        assertTrue(docXml.contains("F3F4F6")); // client info card fill
    }
}
