package com.smart.therapy.flow.unit.report;

import com.smart.therapy.flow.report.util.HtmlSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HtmlSanitizerTest {

    @Test
    void stripMarkdownFences_removesHtmlCodeBlockWrapper() {
        String input = "```html\n<h2>Referral Information</h2>\n<p>Information not available.</p>\n```";
        String result = HtmlSanitizer.stripMarkdownFences(input);
        assertEquals("<h2>Referral Information</h2>\n<p>Information not available.</p>", result);
    }

    @Test
    void sanitize_removesFencesAndKeepsAllowedTags() {
        String input = "```html\n<h2>Progress</h2><script>alert(1)</script><p>Done</p>\n```";
        String result = HtmlSanitizer.sanitize(input);
        assertEquals("<h2>Progress</h2><p>Done</p>", result);
        assertFalse(result.contains("script"));
        assertFalse(result.contains("```"));
    }
}
