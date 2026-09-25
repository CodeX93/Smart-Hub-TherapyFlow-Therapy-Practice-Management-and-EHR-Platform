package com.smart.therapy.flow.report.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;

public final class HtmlSanitizer {

    private static final Safelist ALLOWED = Safelist.none()
            .addTags("h2", "h3", "p", "strong", "ul", "ol", "li", "br")
            .addAttributes("h2", "id")
            .addAttributes("h3", "id");

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String cleaned = stripMarkdownFences(html);
        cleaned = Jsoup.clean(cleaned, "", ALLOWED, new Document.OutputSettings().prettyPrint(false));
        return cleaned.trim();
    }

    /**
     * Removes common LLM markdown wrappers such as {@code ```html ... ```}.
     */
    public static String stripMarkdownFences(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String trimmed = html.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        trimmed = trimmed.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
