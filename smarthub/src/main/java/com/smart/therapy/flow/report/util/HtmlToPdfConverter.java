package com.smart.therapy.flow.report.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;

@Slf4j
public final class HtmlToPdfConverter {

    private HtmlToPdfConverter() {
    }

    public static byte[] toPdfBytes(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(normalizeHtmlForPdf(html), null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to render HTML to PDF", ex);
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    /**
     * openhtmltopdf parses input as XML. Editor HTML often contains {@code &nbsp;}
     * and unclosed {@code <br>} tags that are valid in browsers but break PDF rendering.
     */
    public static String normalizeHtmlForPdf(String html) {
        if (!StringUtils.hasText(html)) {
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head><body></body></html>";
        }
        String withoutDoctype = html.replaceFirst("(?is)<!DOCTYPE[^>]*>", "").trim();
        Document document = Jsoup.parse(withoutDoctype);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return "<!DOCTYPE html>\n" + document.outerHtml();
    }
}
