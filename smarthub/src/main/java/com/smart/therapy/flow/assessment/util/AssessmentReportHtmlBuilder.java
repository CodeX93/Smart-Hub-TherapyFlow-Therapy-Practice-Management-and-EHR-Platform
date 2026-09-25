package com.smart.therapy.flow.assessment.util;

import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentReport;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.report.util.HtmlSanitizer;
import com.smart.therapy.flow.user.entity.UserProfile;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clinical assessment PDF HTML — ported from ClientHub {@code assessment-report-pdf.ts}.
 * openhtmltopdf cannot layout flex/grid, so those are expressed as tables with the same CSS values.
 */
public final class AssessmentReportHtmlBuilder {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US);
    private static final ZoneId PRACTICE_ZONE = ZoneId.of("America/New_York");
    private static final Pattern SECTION_HEADING =
            Pattern.compile("(?im)^\\s*(?:SECTION\\s*:\\s*)?([A-Z][A-Za-z0-9 /()&'-]{2,80})\\s*$");
    private static final Pattern HAS_HTML_TAG = Pattern.compile("(?s).*<[a-zA-Z][^>]*>.*");
    private static final Pattern EMPTY_P =
            Pattern.compile("(?i)<p\\b[^>]*>(?:\\s|&nbsp;|<br\\s*/?>)*</p>");

    private AssessmentReportHtmlBuilder() {
    }

    public static String build(
            AssessmentAssignment assignment,
            AssessmentReport report,
            Map<String, String> practiceSettings,
            UserProfile therapistProfile,
            String genderLabel) {
        Client client = assignment.getClient();
        boolean finalized = Boolean.TRUE.equals(report.getIsFinalized());

        String clientName = valueOr(client != null ? client.getFullName() : null, "Client");
        String clientId = valueOr(client != null ? client.getClientId() : null, "Not provided");
        String dateOfBirth = formatDate(client != null ? client.getDateOfBirth() : null);
        String gender = valueOr(genderLabel, client != null ? client.getGender() : null, "Not specified");
        String phone = valueOr(client != null ? client.getPrimaryPhone() : null, "Not provided");
        String email = valueOr(client != null ? client.getPrimaryEmail() : null, "Not provided");
        String address = formatAddress(client != null ? client.getPrimaryAddress() : null);
        String assessmentName = assignment.getTemplate() != null
                ? valueOr(assignment.getTemplate().getName(), "Assessment")
                : "Assessment";
        Instant completionInstant = assignment.getCompletedAt() != null
                ? assignment.getCompletedAt()
                : (report.getFinalizedAt() != null ? report.getFinalizedAt() : assignment.getAssignedDate());
        String completionDate = completionInstant != null ? formatInstant(completionInstant) : "Not completed";
        String clinicianName = assignment.getAssignedBy() != null
                ? valueOr(assignment.getAssignedBy().getFullName(), "Not assigned")
                : "Not assigned";
        String clinicianTitle = assignment.getAssignedBy() != null
                ? valueOr(assignment.getAssignedBy().getTitle(), null)
                : null;
        if (StringUtils.hasText(clinicianTitle)) {
            clinicianName = clinicianName + ", " + clinicianTitle.trim();
        }

        String practiceName = valueOr(practiceSettings.get("name"), "Practice");
        String practiceAddress = valueOr(practiceSettings.get("address"), "");
        String practicePhone = valueOr(practiceSettings.get("phone"), "N/A");
        String practiceEmail = valueOr(practiceSettings.get("email"), "N/A");
        String practiceWebsite = displayWebsite(valueOr(practiceSettings.get("website"), "N/A"));

        String rawContent = firstNonBlank(
                report.getFinalContent(),
                report.getDraftContent(),
                report.getGeneratedContent());
        String contentHtml = formatReportContent(rawContent);

        // CSS values copied from ClientHub server/pdf/assessment-report-pdf.ts
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>")
                .append(escape(assessmentName)).append(" - ").append(escape(clientName))
                .append("</title><style>");
        html.append("body{font-family:Helvetica,Arial,sans-serif;padding:20px 30px;line-height:1.5;color:#333;}");
        html.append(".header-table{width:100%;border-collapse:collapse;border-bottom:3px solid #3C4D58;");
        html.append("padding-bottom:12px;margin-bottom:15px;}");
        html.append(".header-left h1{color:#333;font-size:24px;margin:0;font-weight:700;line-height:1.2;}");
        html.append(".header-subtitle{margin:5px 0;color:#6b7280;}");
        html.append(".status-badge{display:inline-block;font-size:12px;padding:4px 12px;border-radius:12px;");
        html.append("margin-left:10px;font-weight:600;vertical-align:middle;background:#E0EAED;color:#3C4D58;}");
        html.append(".practice-name{font-weight:600;color:#3C4D58;font-size:16px;margin-bottom:8px;text-align:right;}");
        html.append(".practice-meta{color:#4b5563;font-size:13px;margin:4px 0;text-align:right;}");
        html.append(".confidentiality-banner{background-color:#fef3c7;border-left:4px solid #f59e0b;");
        html.append("padding:10px 15px;margin:12px 0;text-align:center;font-size:12px;font-weight:600;");
        html.append("color:#92400e;letter-spacing:0.5px;text-transform:uppercase;}");
        html.append(".client-info-section{background-color:#f3f4f6;border:1px solid #e2e8f0;");
        html.append("border-radius:6px;padding:12px 15px;margin:12px 0 15px 0;}");
        html.append(".client-info-title{font-size:16px;font-weight:700;color:#3C4D58;margin-bottom:15px;");
        html.append("padding-bottom:8px;border-bottom:2px solid #E0EAED;}");
        html.append(".info-table{width:100%;border-collapse:collapse;}");
        html.append(".info-table td{width:50%;vertical-align:top;padding:0 12px 12px 0;}");
        html.append(".info-label{font-weight:600;color:#64748b;font-size:12px;text-transform:uppercase;");
        html.append("letter-spacing:0.5px;margin-bottom:4px;}");
        html.append(".info-value{font-size:14px;color:#1f2937;}");
        html.append(".report-content{margin-top:8px;font-size:14px;line-height:1.6;}");
        html.append(".report-content h1{color:#3C4D58;font-size:19px;margin:20px 0 8px 0;padding-top:15px;");
        html.append("font-weight:700;border-top:3px solid #3C4D58;border-bottom:2px solid #E0EAED;padding-bottom:5px;}");
        html.append(".report-content h1:first-child{margin-top:10px;padding-top:0;border-top:none;}");
        html.append(".report-content h2{color:#3C4D58;font-size:17px;margin:15px 0 6px 0;padding-top:10px;");
        html.append("font-weight:700;border-top:2px solid #e5e7eb;border-bottom:1px solid #e5e7eb;padding-bottom:4px;}");
        html.append(".report-content h2:first-child{margin-top:8px;padding-top:0;border-top:none;}");
        html.append(".report-content h3{color:#3C4D58;font-size:15px;margin:10px 0 5px 0;font-weight:600;}");
        html.append(".report-content h4{color:#4B5563;font-size:14px;margin:8px 0 4px 0;font-weight:600;}");
        html.append(".report-content p{margin:5px 0;line-height:1.6;color:#374151;}");
        html.append(".report-content strong{color:#1f2937;font-weight:600;}");
        html.append(".report-content ul,.report-content ol{margin:5px 0 5px 20px;padding:0;}");
        html.append(".report-content li{margin:3px 0;color:#374151;}");
        html.append(".signature-section{margin-top:30px;padding:15px 20px;border-top:2px solid #3C4D58;");
        html.append("background-color:#f9fafb;}");
        html.append(".signature-name{font-weight:600;color:#1f2937;font-size:15px;margin-bottom:2px;}");
        html.append(".signature-title{color:#6b7280;font-size:13px;margin-bottom:4px;}");
        html.append(".signature-date{color:#9ca3af;font-size:12px;font-style:italic;}");
        html.append(".footer{margin-top:30px;padding-top:15px;border-top:1px solid #e5e7eb;");
        html.append("text-align:center;font-size:11px;color:#9ca3af;}");
        html.append("</style></head><body>");

        // Header (ClientHub .header flex → table)
        html.append("<table class=\"header-table\"><tr>");
        html.append("<td class=\"header-left\" style=\"vertical-align:top;width:58%;\">");
        html.append("<h1>Clinical Assessment Report");
        if (finalized) {
            html.append(" <span class=\"status-badge\">Finalized</span>");
        }
        html.append("</h1>");
        html.append("<p class=\"header-subtitle\">").append(escape(clientName)).append("</p>");
        html.append("</td><td style=\"vertical-align:top;width:42%;\">");
        html.append("<div class=\"practice-name\">").append(escape(practiceName)).append("</div>");
        html.append("<div class=\"practice-meta\">")
                .append(escape(practiceAddress).replace("\n", "<br/>")).append("</div>");
        html.append("<div class=\"practice-meta\">Phone: ").append(escape(practicePhone)).append("</div>");
        html.append("<div class=\"practice-meta\">Email: ").append(escape(practiceEmail)).append("</div>");
        html.append("<div class=\"practice-meta\">Website: ").append(escape(practiceWebsite)).append("</div>");
        html.append("</td></tr></table>");

        html.append("<div class=\"confidentiality-banner\">");
        html.append("⚠ PERSONAL AND CONFIDENTIAL – Protected Health Information. ");
        html.append("Unauthorized use or disclosure is prohibited under HIPAA.");
        html.append("</div>");

        // Client info (ClientHub .info-grid → table)
        html.append("<div class=\"client-info-section\">");
        html.append("<div class=\"client-info-title\">CLIENT INFORMATION</div>");
        html.append("<table class=\"info-table\">");
        html.append("<tr>").append(infoCell("Client Name", clientName))
                .append(infoCell("Client ID", clientId)).append("</tr>");
        html.append("<tr>").append(infoCell("Date of Birth", dateOfBirth))
                .append(infoCell("Gender", gender)).append("</tr>");
        html.append("<tr>").append(infoCell("Phone Number", phone))
                .append(infoCell("Email Address", email)).append("</tr>");
        html.append("<tr><td colspan=\"2\">").append(infoCellInner("Address", address)).append("</td></tr>");
        html.append("<tr>").append(infoCell("Assessment", assessmentName))
                .append(infoCell("Completion Date", completionDate)).append("</tr>");
        html.append("<tr>").append(infoCell("Clinician", clinicianName))
                .append("<td></td></tr>");
        html.append("</table></div>");

        html.append("<div class=\"report-content\">").append(contentHtml).append("</div>");

        if (finalized && report.getFinalizedAt() != null && assignment.getAssignedBy() != null) {
            html.append("<div class=\"signature-section\">");
            if (StringUtils.hasText(assignment.getAssignedBy().getSignatureImage())) {
                html.append("<img src=\"").append(escape(assignment.getAssignedBy().getSignatureImage()))
                        .append("\" alt=\"Signature\" style=\"max-width:200px;max-height:60px;\"/>");
            }
            html.append("<div class=\"signature-name\">")
                    .append(escape(assignment.getAssignedBy().getFullName())).append("</div>");
            String license = formatLicense(therapistProfile);
            if (StringUtils.hasText(license)) {
                html.append("<div class=\"signature-title\">").append(escape(license)).append("</div>");
            }
            html.append("<div class=\"signature-date\">Digitally signed on ")
                    .append(escape(formatInstant(report.getFinalizedAt()))).append("</div>");
            html.append("</div>");
        }

        html.append("<div class=\"footer\"><p>")
                .append(escape(practiceName)).append(" | ")
                .append(escape(practicePhone)).append(" | ")
                .append(escape(practiceEmail)).append("</p></div>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Port of ClientHub content handling + TherapyFlow plain-text {@code SECTION:} conversion.
     * Strips AI preamble (Subject / Age Range / Total Score) that ClientHub never included.
     */
    public static String formatReportContent(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return "<p></p>";
        }
        String trimmed = rawContent.trim();
        if (HAS_HTML_TAG.matcher(trimmed).matches()) {
            return HtmlSanitizer.sanitize(normalizeReportHtml(trimmed));
        }

        String[] lines = trimmed.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder html = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        boolean skippedPreamble = false;

        for (String line : lines) {
            String cleaned = line.trim();
            if (!skippedPreamble && isPreambleLine(cleaned)) {
                continue;
            }
            skippedPreamble = true;
            if (isPreambleLine(cleaned) || isSeparatorLine(cleaned)) {
                flushParagraph(html, paragraph);
                continue;
            }

            Matcher sectionMatcher = SECTION_HEADING.matcher(cleaned);
            boolean looksLikeSection = cleaned.regionMatches(true, 0, "SECTION:", 0, 8)
                    || (cleaned.equals(cleaned.toUpperCase(Locale.US))
                    && cleaned.length() >= 3
                    && cleaned.length() <= 60
                    && cleaned.chars().noneMatch(Character::isDigit)
                    && !cleaned.contains("."));

            if (looksLikeSection && sectionMatcher.matches()) {
                flushParagraph(html, paragraph);
                String title = sectionMatcher.group(1).trim();
                if (title.regionMatches(true, 0, "SECTION:", 0, 8)) {
                    title = title.substring(8).trim();
                }
                // ClientHub section headings in sample PDFs are uppercase
                html.append("<h2>").append(escape(title.toUpperCase(Locale.US))).append("</h2>");
                continue;
            }

            if (cleaned.isEmpty()) {
                flushParagraph(html, paragraph);
                continue;
            }

            if (looksLikeNumberedItem(cleaned)) {
                flushParagraph(html, paragraph);
                html.append("<p>").append(escape(cleaned)).append("</p>");
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(cleaned);
        }
        flushParagraph(html, paragraph);
        splitInlineNumberedItems(html);

        String sanitized = HtmlSanitizer.sanitize(normalizeReportHtml(html.toString()));
        return StringUtils.hasText(sanitized) ? sanitized : "<p></p>";
    }

    /** Port of ClientHub {@code normalizeReportHtml}. */
    public static String normalizeReportHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String out = html;
        out = out.replaceAll("(?i)(</h[1-6]>)\\s*(?:" + EMPTY_P.pattern() + "\\s*)+", "$1");
        out = out.replaceAll("(?i)(?:" + EMPTY_P.pattern() + "\\s*){2,}", "<p><br/></p>");
        out = out.replaceAll("(?i)^\\s*(?:" + EMPTY_P.pattern() + "\\s*)+", "");
        return out;
    }

    private static boolean isSeparatorLine(String line) {
        return line.equals("---") || line.equals("***") || line.equals("___");
    }

    private static boolean looksLikeNumberedItem(String line) {
        return line.matches("^\\d+\\.\\s+.+");
    }

    private static void splitInlineNumberedItems(StringBuilder html) {
        String source = html.toString();
        Pattern paragraph = Pattern.compile("(?s)<p>(\\d+\\.\\s+.*?)</p>");
        Matcher pMatcher = paragraph.matcher(source);
        StringBuffer rewritten = new StringBuffer();
        boolean changed = false;
        while (pMatcher.find()) {
            String body = pMatcher.group(1);
            String[] parts = body.split("(?=\\s\\d+\\.\\s+)");
            if (parts.length < 2) {
                pMatcher.appendReplacement(rewritten, Matcher.quoteReplacement(pMatcher.group()));
                continue;
            }
            changed = true;
            StringBuilder replacement = new StringBuilder();
            for (String part : parts) {
                String item = part.trim();
                if (!item.isEmpty()) {
                    replacement.append("<p>").append(item).append("</p>");
                }
            }
            pMatcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement.toString()));
        }
        pMatcher.appendTail(rewritten);
        if (changed) {
            html.setLength(0);
            html.append(rewritten);
        }
    }

    private static boolean isPreambleLine(String line) {
        if (!StringUtils.hasText(line)) {
            return true;
        }
        if (isSeparatorLine(line)) {
            return true;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.equals("assessment report")
                || lower.equals("clinical assessment report")
                || lower.equals("report:")
                || lower.equals("client information")
                || lower.equals("client information:")
                || lower.startsWith("client:")
                || lower.startsWith("assessment:")
                || lower.startsWith("date:")
                || lower.startsWith("total score:")
                || lower.startsWith("- subject:")
                || lower.startsWith("- age range:")
                || lower.startsWith("- assessment:")
                || lower.startsWith("- total score:")
                || lower.startsWith("subject:")
                || lower.startsWith("age range:")) {
            return true;
        }
        // Combined AI meta line: "- Subject: Client - Age Range: N/A - Assessment: ... - Total Score: 0.00"
        return lower.contains("subject:")
                && (lower.contains("age range:") || lower.contains("total score:"));
    }

    private static void flushParagraph(StringBuilder html, StringBuilder paragraph) {
        if (paragraph.length() == 0) {
            return;
        }
        html.append("<p>").append(escape(paragraph.toString().trim())).append("</p>");
        paragraph.setLength(0);
    }

    private static String infoCell(String label, String value) {
        return "<td>" + infoCellInner(label, value) + "</td>";
    }

    private static String infoCellInner(String label, String value) {
        return "<div class=\"info-label\">" + escape(label) + "</div>"
                + "<div class=\"info-value\">" + escape(value) + "</div>";
    }

    private static String formatLicense(UserProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getLicenseType())) {
            return null;
        }
        String license = profile.getLicenseType().trim();
        if (StringUtils.hasText(profile.getLicenseNumber())) {
            license += " #" + profile.getLicenseNumber().trim();
        }
        return license;
    }

    private static String formatAddress(ClientAddress address) {
        if (address == null) {
            return "Not provided";
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(address.getStreetAddress1())) {
            parts.add(address.getStreetAddress1().trim());
        }
        if (StringUtils.hasText(address.getStreetAddress2())) {
            parts.add(address.getStreetAddress2().trim());
        }
        String cityLine = joinNonBlank(", ",
                address.getCity(),
                address.getStateProvince(),
                address.getPostalCode());
        if (StringUtils.hasText(cityLine)) {
            parts.add(cityLine);
        }
        if (StringUtils.hasText(address.getCountry())) {
            parts.add(address.getCountry().trim());
        }
        return parts.isEmpty() ? "Not provided" : String.join(", ", parts);
    }

    private static String joinNonBlank(String delimiter, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(delimiter, parts);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "Not provided" : DISPLAY_DATE.format(date);
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return "Not provided";
        }
        return DISPLAY_DATE.format(instant.atZone(PRACTICE_ZONE));
    }

    private static String displayWebsite(String website) {
        if (!StringUtils.hasText(website) || "N/A".equalsIgnoreCase(website)) {
            return website;
        }
        String cleaned = website.trim();
        if (cleaned.regionMatches(true, 0, "https://", 0, 8)) {
            cleaned = cleaned.substring(8);
        } else if (cleaned.regionMatches(true, 0, "http://", 0, 7)) {
            cleaned = cleaned.substring(7);
        }
        return cleaned;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private static String valueOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String valueOr(String preferred, String secondary, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        if (StringUtils.hasText(secondary)) {
            return secondary.trim();
        }
        return fallback;
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
