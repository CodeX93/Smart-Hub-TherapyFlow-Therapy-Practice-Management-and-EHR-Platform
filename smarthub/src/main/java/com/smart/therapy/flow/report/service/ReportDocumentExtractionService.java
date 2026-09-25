package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ReportDocumentExtractionService {

    private static final int MAX_TEMPLATE_CHARS = 30_000;
    private static final int MAX_DOCUMENT_CHARS = 30_000;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String extractTemplateStructure(byte[] fileBytes, String mimeType, String originalName) {
        String text = extractText(fileBytes, mimeType, originalName);
        return normalizeAndCap(text, MAX_TEMPLATE_CHARS, "Template structure extraction produced no text");
    }

    public String extractDocumentText(byte[] fileBytes, String mimeType, String originalName) {
        String text = extractText(fileBytes, mimeType, originalName);
        return normalizeAndCap(text, MAX_DOCUMENT_CHARS, "Document text extraction produced no text");
    }

    public boolean isSupportedTemplateType(String mimeType, String originalName) {
        return isDocx(mimeType, originalName) || isPdf(mimeType, originalName);
    }

    public boolean isSupportedSupportingType(String mimeType, String originalName) {
        return isDocx(mimeType, originalName) || isPdf(mimeType, originalName) || isTxt(mimeType, originalName);
    }

    private String extractText(byte[] fileBytes, String mimeType, String originalName) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("File is empty");
        }
        try {
            if (isDocx(mimeType, originalName)) {
                return extractDocx(fileBytes);
            }
            if (isPdf(mimeType, originalName)) {
                return extractPdf(fileBytes);
            }
            if (isTxt(mimeType, originalName)) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            }
            throw new BadRequestException("Unsupported file type");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to extract text from {}: {}", originalName, ex.getMessage());
            throw new BadRequestException("Failed to extract text from file");
        }
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = paragraph.getText();
                if (StringUtils.hasText(text)) {
                    sb.append(text.trim()).append('\n');
                }
            }
            return sb.toString();
        }
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument doc = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String normalizeAndCap(String text, int maxChars, String emptyMessage) {
        if (!StringUtils.hasText(text)) {
            throw new BadRequestException(emptyMessage);
        }
        String normalized = WHITESPACE.matcher(text.trim()).replaceAll(" ");
        if (normalized.length() > maxChars) {
            normalized = normalized.substring(0, maxChars);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestException(emptyMessage);
        }
        return normalized;
    }

    private boolean isDocx(String mimeType, String originalName) {
        String lower = safeLower(originalName);
        return (mimeType != null && mimeType.contains("wordprocessingml"))
                || lower.endsWith(".docx");
    }

    private boolean isPdf(String mimeType, String originalName) {
        String lower = safeLower(originalName);
        return "application/pdf".equalsIgnoreCase(mimeType) || lower.endsWith(".pdf");
    }

    private boolean isTxt(String mimeType, String originalName) {
        String lower = safeLower(originalName);
        return "text/plain".equalsIgnoreCase(mimeType) || lower.endsWith(".txt");
    }

    private String safeLower(String name) {
        return name != null ? name.toLowerCase(Locale.ROOT) : "";
    }
}
