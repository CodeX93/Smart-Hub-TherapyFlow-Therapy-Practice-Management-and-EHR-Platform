package com.smart.therapy.flow.unit.report;

import com.smart.therapy.flow.report.entity.ReportTemplate;
import com.smart.therapy.flow.report.service.ClientReportDataAggregator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientReportEmptySourcesTest {

    @Test
    void hasUsableSourceData_falseWhenAllBlocksEmpty() {
        ClientReportDataAggregator.AggregatedReportData empty =
                ClientReportDataAggregator.AggregatedReportData.builder().build();
        assertFalse(empty.hasUsableSourceData());

        ClientReportDataAggregator.AggregatedReportData withNotes =
                ClientReportDataAggregator.AggregatedReportData.builder()
                        .notesBlock("SESSION NOTES:\n- hello")
                        .build();
        assertTrue(withNotes.hasUsableSourceData());
    }

    @Test
    void buildUnavailableNarrativeHtml_usesTemplateHeadingsWithoutCallingProse() {
        ClientReportDataAggregator aggregator = new ClientReportDataAggregator(
                null, null, null, null, null, null, null, null);

        ReportTemplate template = ReportTemplate.builder()
                .structureText("""
                        Psychotherapy Progress Report Template and AI Prompt Guide

                        Client Information

                        Client Name, Date of Birth, Age, Gender, Report Date, Clinician Name and Credentials.

                        Referral Information

                        Referral source, referral date, and reason for referral.

                        Brief Background Summary

                        Concise summary of relevant history.

                        Clinical Presentation

                        Summary of presenting symptoms.

                        Assessment Findings and Initial Treatment Goals

                        Summarize assessment findings.

                        Treatment Summary and Interventions

                        State total sessions.

                        Overall Clinical Progress

                        Review session notes.

                        Recommendations and Future Treatment Focus

                        Clinical rationale for continued treatment.
                        """)
                .build();

        String html = aggregator.buildUnavailableNarrativeHtml(template);

        assertTrue(html.contains("<h2>Brief Background Summary</h2>"));
        assertTrue(html.contains("<h2>Clinical Presentation</h2>"));
        assertTrue(html.contains("<h2>Assessment Findings and Initial Treatment Goals</h2>"));
        assertTrue(html.contains("<h2>Treatment Summary and Interventions</h2>"));
        assertTrue(html.contains("<h2>Overall Clinical Progress</h2>"));
        assertTrue(html.contains("<h2>Recommendations and Future Treatment Focus</h2>"));
        assertFalse(html.contains("Client Information"));
        assertFalse(html.contains("Referral Information"));
        assertTrue(html.contains("<p>Information not available.</p>"));
        // No fabricated session narrative
        assertFalse(html.toLowerCase().contains("psychotherapy sessions between"));
    }

    @Test
    void enforceExcludedSourceSections_forcesNaWhenAssessmentsDisabled() {
        ClientReportDataAggregator aggregator = new ClientReportDataAggregator(
                null, null, null, null, null, null, null, null);

        String html = """
                <h2>Brief Background Summary</h2><p>OK</p>
                <h2>Assessment Findings and Initial Treatment Goals</h2>
                <p>Made-up PTSD findings from notes.</p>
                <h2>Treatment Summary and Interventions</h2><p>OK</p>
                """;
        ClientReportDataAggregator.AggregatedReportData data =
                ClientReportDataAggregator.AggregatedReportData.builder()
                        .includeAssessments(false)
                        .assessmentsBlock(null)
                        .build();

        String out = aggregator.enforceExcludedSourceSections(html, data);
        assertTrue(out.contains("Information not available."));
        assertFalse(out.contains("Made-up PTSD findings"));
    }

    @Test
    void enforceExcludedSourceSections_replacesNaWhenAssessmentsEnabledWithData() {
        ClientReportDataAggregator aggregator = new ClientReportDataAggregator(
                null, null, null, null, null, null, null, null);

        String html = """
                <h2>Assessment Findings and Initial Treatment Goals</h2>
                <p>Information not available.</p>
                <h2>Treatment Summary and Interventions</h2><p>Sessions ok</p>
                """;
        String assessments = """
                ASSESSMENTS (use for Assessment Findings and Initial Treatment Goals):
                --- Assessment: Initial Clinical Assessment ---
                - Status: completed
                - Assigned: 2026-07-01
                - Completed: 2026-07-02
                - Total score: 18
                - Clinician notes: Elevated anxiety; goals include grounding skill building.
                """;
        ClientReportDataAggregator.AggregatedReportData data =
                ClientReportDataAggregator.AggregatedReportData.builder()
                        .includeAssessments(true)
                        .assessmentsBlock(assessments)
                        .build();

        String out = aggregator.enforceExcludedSourceSections(html, data);
        assertTrue(out.contains("Initial Clinical Assessment"));
        assertTrue(out.contains("Elevated anxiety") || out.contains("Status: completed"));
        assertTrue(out.contains("Treatment Summary and Interventions"));
    }
}

