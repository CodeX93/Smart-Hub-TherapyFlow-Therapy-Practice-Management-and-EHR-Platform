package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.report.entity.ClientReport;
import com.smart.therapy.flow.report.util.HtmlSanitizer;
import com.smart.therapy.flow.report.util.HtmlToPdfConverter;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientReportExportService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern UNSAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._-]+");
    /** Letter page content width with ~1" margins (twips / DXA). */
    private static final int PAGE_CONTENT_WIDTH_DXA = 9360;
    private static final int HALF_PAGE_WIDTH_DXA = PAGE_CONTENT_WIDTH_DXA / 2;

    private final PracticeConfigurationService practiceConfigurationService;
    private final UserProfileRepository userProfileRepository;
    private final SystemOptionResolverService systemOptionResolverService;

    public String buildPrintableHtml(ClientReport report) {
        PracticeConfigurationResponse practice = practiceConfigurationService.getPracticeConfiguration();
        ZoneId zoneId = resolveZoneId(practice);
        Client client = report.getClient();
        User author = report.getCreatedByUser();
        Optional<UserProfile> authorProfile = author != null
                ? userProfileRepository.findByUserId(author.getId())
                : Optional.empty();

        String templateName = StringUtils.hasText(report.getTemplateName())
                ? report.getTemplateName() : "Client Report";
        String clientName = client != null && StringUtils.hasText(client.getFullName())
                ? client.getFullName() : "Client";
        String content = HtmlSanitizer.sanitize(report.resolveEditorContent());
        if (!StringUtils.hasText(content)) {
            content = "<p>No content</p>";
        }

        String generatedDate = formatInstant(report.getGeneratedAt(), zoneId,
                LocalDate.now(zoneId).format(DISPLAY_DATE));
        String finalizedDate = formatInstant(report.getFinalizedAt(), zoneId, null);
        String clientDob = client != null && client.getDateOfBirth() != null
                ? client.getDateOfBirth().format(DISPLAY_DATE) : "Not provided";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>");
        html.append("body { font-family: Helvetica, Arial, sans-serif; padding: 20px 30px; line-height: 1.5; color: #333; }");
        html.append(".header-table { width: 100%; border-bottom: 3px solid #2563eb; margin-bottom: 15px; padding-bottom: 12px; }");
        html.append(".header-title { color: #1e40af; font-size: 22px; margin: 0 0 4px 0; font-weight: 700; }");
        html.append(".header-subtitle { margin: 5px 0; color: #6b7280; }");
        html.append(".practice-name { font-weight: 600; color: #1e40af; font-size: 16px; margin-bottom: 8px; }");
        html.append(".practice-meta { color: #4b5563; font-size: 13px; margin: 4px 0; text-align: right; }");
        html.append(".status-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; margin-left: 8px; background: #dcfce7; color: #166534; }");
        html.append(".confidentiality-banner { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 10px 15px; margin: 12px 0; text-align: center; font-size: 12px; font-weight: 600; color: #92400e; letter-spacing: 0.5px; text-transform: uppercase; }");
        html.append(".client-info-section { background-color: #f3f4f6; border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px 15px; margin: 12px 0 15px 0; }");
        html.append(".client-info-title { font-size: 16px; font-weight: 700; color: #1e40af; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #dbeafe; }");
        html.append(".info-table { width: 100%; border-collapse: collapse; }");
        html.append(".info-table td { width: 50%; vertical-align: top; padding: 6px 8px 10px 0; }");
        html.append(".info-label { font-weight: 600; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }");
        html.append(".info-value { font-size: 14px; color: #1f2937; }");
        html.append(".report-content { margin-top: 8px; font-size: 14px; line-height: 1.6; }");
        html.append(".report-content h2 { color: #1e40af; font-size: 17px; margin: 15px 0 6px 0; font-weight: 700; border-bottom: 1px solid #e5e7eb; padding-bottom: 4px; }");
        html.append(".report-content h3 { color: #2563eb; font-size: 15px; margin: 10px 0 5px 0; font-weight: 600; }");
        html.append(".report-content p { margin: 5px 0; line-height: 1.6; color: #374151; }");
        html.append(".report-content strong { color: #1f2937; font-weight: 600; }");
        html.append(".report-content ul, .report-content ol { margin: 5px 0 5px 20px; padding: 0; }");
        html.append(".report-content li { margin: 3px 0; color: #374151; }");
        html.append(".signature-section { margin-top: 30px; padding: 15px 20px; border-top: 2px solid #2563eb; background-color: #f9fafb; }");
        html.append(".signature-name { font-weight: 600; color: #1f2937; font-size: 15px; margin-bottom: 2px; }");
        html.append(".signature-title { color: #6b7280; font-size: 13px; margin-bottom: 4px; }");
        html.append(".signature-date { color: #9ca3af; font-size: 12px; font-style: italic; }");
        html.append("</style></head><body>");

        html.append("<table class=\"header-table\"><tr>");
        html.append("<td style=\"vertical-align:top;\">");
        html.append("<h1 class=\"header-title\">").append(escapeHtml(templateName));
        if (Boolean.TRUE.equals(report.getIsFinalized())) {
            html.append("<span class=\"status-badge\">Finalized</span>");
        }
        html.append("</h1><p class=\"header-subtitle\">").append(escapeHtml(clientName)).append("</p></td>");
        html.append("<td style=\"vertical-align:top;\">");
        html.append("<div class=\"practice-name\">").append(escapeHtml(valueOr(practice.getPracticeName(), "Practice"))).append("</div>");
        html.append("<div class=\"practice-meta\">").append(escapeHtml(valueOr(practice.getPracticeAddress(), "")).replace("\n", "<br/>")).append("</div>");
        html.append("<div class=\"practice-meta\">Phone: ").append(escapeHtml(valueOr(practice.getPracticePhone(), "N/A"))).append("</div>");
        html.append("<div class=\"practice-meta\">Email: ").append(escapeHtml(valueOr(practice.getPracticeEmail(), "N/A"))).append("</div>");
        html.append("<div class=\"practice-meta\">Website: ").append(escapeHtml(valueOr(practice.getPracticeWebsite(), "N/A"))).append("</div>");
        html.append("</td></tr></table>");

        html.append("<div class=\"confidentiality-banner\">");
        html.append("PERSONAL AND CONFIDENTIAL - Protected Health Information. Unauthorized use or disclosure is prohibited under HIPAA.");
        html.append("</div>");

        html.append("<div class=\"client-info-section\"><div class=\"client-info-title\">CLIENT INFORMATION</div>");
        html.append("<table class=\"info-table\"><tr>");
        html.append(infoCell("Client Name", clientName));
        html.append(infoCell("Client ID", client != null ? valueOr(client.getClientId(), "Not provided") : "Not provided"));
        html.append("</tr><tr>");
        html.append(infoCell("Date of Birth", clientDob));
        html.append(infoCell("Gender", client != null && client.getGender() != null
                ? systemOptionResolverService.resolveOptionLabel(SystemOptionCategories.GENDER, client.getGender()) : "Not specified"));
        html.append("</tr><tr>");
        html.append(infoCell("Report Type", templateName));
        html.append(infoCell("Generated", generatedDate));
        html.append("</tr><tr>");
        html.append(infoCell("Prepared By", formatPreparedBy(author)));
        html.append("<td></td>");
        html.append("</tr></table></div>");

        html.append("<div class=\"report-content\">").append(content).append("</div>");

        if (Boolean.TRUE.equals(report.getIsFinalized()) && StringUtils.hasText(finalizedDate)) {
            html.append("<div class=\"signature-section\">");
            if (author != null && StringUtils.hasText(author.getSignatureImage())) {
                html.append("<img src=\"").append(escapeHtml(author.getSignatureImage()))
                        .append("\" alt=\"Signature\" style=\"max-width:200px;max-height:60px;\"/>");
            }
            if (author != null) {
                html.append("<div class=\"signature-name\">").append(escapeHtml(author.getFullName())).append("</div>");
            }
            String licenseLine = formatLicenseLine(author, authorProfile.orElse(null));
            if (StringUtils.hasText(licenseLine)) {
                html.append("<div class=\"signature-title\">").append(escapeHtml(licenseLine)).append("</div>");
            }
            html.append("<div class=\"signature-date\">Digitally signed on ").append(escapeHtml(finalizedDate)).append("</div>");
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    public byte[] buildPdf(ClientReport report) {
        return HtmlToPdfConverter.toPdfBytes(buildPrintableHtml(report));
    }

    public String resolvePdfFilename(ClientReport report) {
        String mrnPart = resolveMrnFileSegment(report);
        ZoneId zoneId = resolveZoneId(practiceConfigurationService.getPracticeConfiguration());
        Instant generatedAt = report.getGeneratedAt() != null ? report.getGeneratedAt() : Instant.now();
        String datePart = FILE_DATE.format(generatedAt.atZone(zoneId));
        return mrnPart + "-" + datePart + ".pdf";
    }

    public String resolveDocxFilename(ClientReport report) {
        return resolvePdfFilename(report).replace(".pdf", ".docx");
    }

    /**
     * Filename segment from MRN ({@code client.clientId}), e.g. {@code CL-2026-0001}.
     * Falls back to numeric client PK when MRN is unavailable.
     */
    private String resolveMrnFileSegment(ClientReport report) {
        Client client = report != null ? report.getClient() : null;
        if (client != null && StringUtils.hasText(client.getClientId())) {
            String cleaned = UNSAFE_FILENAME.matcher(client.getClientId().trim()).replaceAll("-");
            cleaned = cleaned.replaceAll("-+", "-").replaceAll("^-|-$", "");
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }
        }
        if (client != null && client.getId() != null) {
            return "client-" + client.getId();
        }
        return "client";
    }

    public byte[] buildDocx(ClientReport report) throws Exception {
        // Match PDF package: same printable document model, rendered to Word layout.
        PracticeConfigurationResponse practice = practiceConfigurationService.getPracticeConfiguration();
        ZoneId zoneId = resolveZoneId(practice);
        Client client = report.getClient();
        User author = report.getCreatedByUser();
        Optional<UserProfile> authorProfile = author != null
                ? userProfileRepository.findByUserId(author.getId())
                : Optional.empty();

        String templateName = StringUtils.hasText(report.getTemplateName())
                ? report.getTemplateName() : "Client Report";
        String clientName = client != null && StringUtils.hasText(client.getFullName())
                ? client.getFullName() : "Client";
        String content = HtmlSanitizer.sanitize(report.resolveEditorContent());
        if (!StringUtils.hasText(content)) {
            content = "<p>No content</p>";
        }

        String generatedDate = formatInstant(report.getGeneratedAt(), zoneId,
                LocalDate.now(zoneId).format(DISPLAY_DATE));
        String finalizedDate = formatInstant(report.getFinalizedAt(), zoneId, null);
        String clientDob = client != null && client.getDateOfBirth() != null
                ? client.getDateOfBirth().format(DISPLAY_DATE) : "Not provided";
        String clientIdVal = client != null ? valueOr(client.getClientId(), "Not provided") : "Not provided";
        String genderVal = client != null && client.getGender() != null
                ? systemOptionResolverService.resolveOptionLabel(SystemOptionCategories.GENDER, client.getGender())
                : "Not specified";
        if (!StringUtils.hasText(genderVal)) {
            genderVal = "Not specified";
        }

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ensurePageSetup(document);

            // Header without tables (tables collapse to 1-char width in Word/Pages).
            // Row 1: title | practice name
            addTwoColumnRow(
                    document,
                    templateName + (Boolean.TRUE.equals(report.getIsFinalized()) ? "  Finalized" : ""),
                    valueOr(practice.getPracticeName(), "Practice"),
                    true,
                    18,
                    "1E40AF",
                    12,
                    "1E40AF",
                    true);
            // Row 2: client name | address / first contact line
            String addressLine = valueOr(practice.getPracticeAddress(), "").replace("\n", ", ").trim();
            addTwoColumnRow(
                    document,
                    clientName,
                    addressLine,
                    false,
                    11,
                    "6B7280",
                    10,
                    "4B5563",
                    false);
            addTwoColumnRow(
                    document,
                    "",
                    "Phone: " + valueOr(practice.getPracticePhone(), "N/A"),
                    false,
                    11,
                    "6B7280",
                    10,
                    "4B5563",
                    false);
            addTwoColumnRow(
                    document,
                    "",
                    "Email: " + valueOr(practice.getPracticeEmail(), "N/A"),
                    false,
                    11,
                    "6B7280",
                    10,
                    "4B5563",
                    false);
            XWPFParagraph websiteRow = addTwoColumnRow(
                    document,
                    "",
                    "Website: " + valueOr(practice.getPracticeWebsite(), "N/A"),
                    false,
                    11,
                    "6B7280",
                    10,
                    "4B5563",
                    false);
            addBottomBorder(websiteRow, "2563EB", 24);

            spacer(document, 160);

            // Confidentiality banner
            XWPFParagraph banner = document.createParagraph();
            banner.setAlignment(ParagraphAlignment.CENTER);
            banner.setSpacingAfter(200);
            XWPFRun bannerRun = banner.createRun();
            bannerRun.setBold(true);
            bannerRun.setFontSize(9);
            bannerRun.setColor("92400E");
            bannerRun.setFontFamily("Helvetica");
            bannerRun.setText(
                    "PERSONAL AND CONFIDENTIAL - Protected Health Information. Unauthorized use or disclosure is prohibited under HIPAA.");

            // Client information — full-width stacked pairs (same fields as PDF, no tables)
            XWPFParagraph infoTitle = document.createParagraph();
            infoTitle.setSpacingAfter(120);
            XWPFRun infoTitleRun = infoTitle.createRun();
            infoTitleRun.setBold(true);
            infoTitleRun.setFontSize(13);
            infoTitleRun.setColor("1E40AF");
            infoTitleRun.setFontFamily("Helvetica");
            infoTitleRun.setText("CLIENT INFORMATION");

            addInfoFieldPair(document, "CLIENT NAME", clientName, "CLIENT ID", clientIdVal);
            addInfoFieldPair(document, "DATE OF BIRTH", clientDob, "GENDER", genderVal);
            addInfoFieldPair(document, "REPORT TYPE", templateName, "GENERATED", generatedDate);
            addInfoFieldPair(document, "PREPARED BY", formatPreparedBy(author), "", "");

            spacer(document, 200);

            // Report body — same HTML content as PDF .report-content
            appendHtmlAsDocx(document, content);

            // Signature (finalized only) — mirrors PDF signature block
            if (Boolean.TRUE.equals(report.getIsFinalized()) && StringUtils.hasText(finalizedDate)) {
                spacer(document, 280);
                if (author != null && StringUtils.hasText(author.getFullName())) {
                    XWPFParagraph sn = document.createParagraph();
                    XWPFRun snr = sn.createRun();
                    snr.setBold(true);
                    snr.setFontSize(12);
                    snr.setColor("1F2937");
                    snr.setFontFamily("Helvetica");
                    snr.setText(author.getFullName());
                }
                String licenseLine = formatLicenseLine(author, authorProfile.orElse(null));
                if (StringUtils.hasText(licenseLine)) {
                    XWPFParagraph st = document.createParagraph();
                    XWPFRun str = st.createRun();
                    str.setFontSize(10);
                    str.setColor("6B7280");
                    str.setFontFamily("Helvetica");
                    str.setText(licenseLine);
                }
                XWPFParagraph sd = document.createParagraph();
                XWPFRun sdr = sd.createRun();
                sdr.setItalic(true);
                sdr.setFontSize(10);
                sdr.setColor("9CA3AF");
                sdr.setFontFamily("Helvetica");
                sdr.setText("Digitally signed on " + finalizedDate);
            }

            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Dual-column layout using a mid-page tab stop (reliable in Word + Pages; avoids collapsed tables).
     */
    private XWPFParagraph addTwoColumnRow(
            XWPFDocument document,
            String left,
            String right,
            boolean leftBold,
            int leftSize,
            String leftColor,
            int rightSize,
            String rightColor,
            boolean rightBold) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(40);
        p.setAlignment(ParagraphAlignment.LEFT);
        setMidPageTab(p);

        if (StringUtils.hasText(left)) {
            XWPFRun leftRun = p.createRun();
            leftRun.setBold(leftBold);
            leftRun.setFontSize(leftSize);
            leftRun.setColor(leftColor);
            leftRun.setFontFamily("Helvetica");
            leftRun.setText(left);
        }

        if (StringUtils.hasText(right)) {
            XWPFRun tabRun = p.createRun();
            tabRun.setFontFamily("Helvetica");
            tabRun.setText("\t");
            XWPFRun rightRun = p.createRun();
            rightRun.setBold(rightBold);
            rightRun.setFontSize(rightSize);
            rightRun.setColor(rightColor);
            rightRun.setFontFamily("Helvetica");
            rightRun.setText(right);
        }
        return p;
    }

    private void addInfoFieldPair(
            XWPFDocument document,
            String leftLabel,
            String leftValue,
            String rightLabel,
            String rightValue) {
        // Labels row
        XWPFParagraph labels = document.createParagraph();
        labels.setSpacingAfter(20);
        setMidPageTab(labels);
        XWPFRun ll = labels.createRun();
        ll.setBold(true);
        ll.setFontSize(9);
        ll.setColor("64748B");
        ll.setFontFamily("Helvetica");
        ll.setText(leftLabel != null ? leftLabel : "");
        if (StringUtils.hasText(rightLabel)) {
            labels.createRun().setText("\t");
            XWPFRun rl = labels.createRun();
            rl.setBold(true);
            rl.setFontSize(9);
            rl.setColor("64748B");
            rl.setFontFamily("Helvetica");
            rl.setText(rightLabel);
        }

        // Values row
        XWPFParagraph values = document.createParagraph();
        values.setSpacingAfter(120);
        setMidPageTab(values);
        XWPFRun lv = values.createRun();
        lv.setFontSize(11);
        lv.setColor("1F2937");
        lv.setFontFamily("Helvetica");
        lv.setText(valueOr(leftValue, "Not provided"));
        if (StringUtils.hasText(rightLabel)) {
            values.createRun().setText("\t");
            XWPFRun rv = values.createRun();
            rv.setFontSize(11);
            rv.setColor("1F2937");
            rv.setFontFamily("Helvetica");
            rv.setText(valueOr(rightValue, "Not provided"));
        }
    }

    private void setMidPageTab(XWPFParagraph paragraph) {
        var ctp = paragraph.getCTP();
        var pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        var tabs = pPr.isSetTabs() ? pPr.getTabs() : pPr.addNewTabs();
        while (tabs.sizeOfTabArray() > 0) {
            tabs.removeTab(0);
        }
        var tab = tabs.addNewTab();
        tab.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc.LEFT);
        tab.setPos(BigInteger.valueOf(HALF_PAGE_WIDTH_DXA));
    }

    private void addBottomBorder(XWPFParagraph paragraph, String hexColor, int sizeEighths) {
        try {
            var ctp = paragraph.getCTP();
            var pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
            var pBdr = pPr.isSetPBdr() ? pPr.getPBdr() : pPr.addNewPBdr();
            var bottom = pBdr.isSetBottom() ? pBdr.getBottom() : pBdr.addNewBottom();
            bottom.setVal(STBorder.SINGLE);
            bottom.setSz(BigInteger.valueOf(sizeEighths));
            bottom.setColor(hexColor);
            bottom.setSpace(BigInteger.valueOf(8));
        } catch (Exception ex) {
            log.debug("Could not add paragraph bottom border: {}", ex.getMessage());
        }
    }

    private void ensurePageSetup(XWPFDocument document) {
        try {
            var body = document.getDocument().getBody();
            var sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            var pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
            // US Letter
            pgSz.setW(BigInteger.valueOf(12240));
            pgSz.setH(BigInteger.valueOf(15840));
            var pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
            pgMar.setLeft(BigInteger.valueOf(1440));
            pgMar.setRight(BigInteger.valueOf(1440));
            pgMar.setTop(BigInteger.valueOf(1440));
            pgMar.setBottom(BigInteger.valueOf(1440));
        } catch (Exception ex) {
            log.debug("Could not set Word page setup: {}", ex.getMessage());
        }
    }

    private void appendHtmlAsDocx(XWPFDocument document, String html) {
        if (!StringUtils.hasText(html)) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun run = p.createRun();
            run.setFontFamily("Helvetica");
            run.setFontSize(11);
            run.setColor("374151");
            run.setText("No content");
            return;
        }
        Element body = Jsoup.parseBodyFragment(html).body();
        for (Node node : body.childNodes()) {
            appendDocxNode(document, node, null);
        }
    }

    private void appendDocxNode(XWPFDocument document, Node node, String listPrefix) {
        if (node instanceof TextNode textNode) {
            String text = textNode.text();
            if (!StringUtils.hasText(text) || text.isBlank()) {
                return;
            }
            XWPFParagraph p = document.createParagraph();
            p.setSpacingAfter(80);
            XWPFRun run = p.createRun();
            styleBodyRun(run, false, 11, "374151");
            run.setText((listPrefix != null ? listPrefix : "") + text.trim());
            return;
        }
        if (!(node instanceof Element element)) {
            return;
        }
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        switch (tag) {
            case "h2" -> {
                XWPFParagraph p = document.createParagraph();
                p.setSpacingBefore(200);
                p.setSpacingAfter(80);
                XWPFRun run = p.createRun();
                styleBodyRun(run, true, 13, "1E40AF");
                run.setText(element.text().trim());
            }
            case "h3" -> {
                XWPFParagraph p = document.createParagraph();
                p.setSpacingBefore(140);
                p.setSpacingAfter(60);
                XWPFRun run = p.createRun();
                styleBodyRun(run, true, 12, "2563EB");
                run.setText(element.text().trim());
            }
            case "p" -> {
                if (!StringUtils.hasText(element.text())) {
                    return;
                }
                XWPFParagraph p = document.createParagraph();
                p.setSpacingAfter(80);
                p.setAlignment(ParagraphAlignment.LEFT);
                appendInlineRuns(p, element, listPrefix);
            }
            case "ul" -> {
                int i = 0;
                for (Element li : element.children()) {
                    if ("li".equalsIgnoreCase(li.tagName())) {
                        appendListItem(document, li, "• ");
                        i++;
                    }
                }
            }
            case "ol" -> {
                int index = 1;
                for (Element li : element.children()) {
                    if ("li".equalsIgnoreCase(li.tagName())) {
                        appendListItem(document, li, index + ". ");
                        index++;
                    }
                }
            }
            case "br" -> document.createParagraph();
            case "strong", "b", "em", "i", "span" -> {
                // Treated as block container if top-level
                if (!StringUtils.hasText(element.text())) {
                    return;
                }
                XWPFParagraph p = document.createParagraph();
                p.setSpacingAfter(80);
                appendInlineRuns(p, element, listPrefix);
            }
            default -> {
                for (Node child : element.childNodes()) {
                    appendDocxNode(document, child, listPrefix);
                }
            }
        }
    }

    private void appendListItem(XWPFDocument document, Element li, String prefix) {
        if (!StringUtils.hasText(li.text())) {
            return;
        }
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(40);
        p.setIndentationLeft(360);
        appendInlineRuns(p, li, prefix);
    }

    private void appendInlineRuns(XWPFParagraph paragraph, Element parent, String optionalPrefix) {
        if (StringUtils.hasText(optionalPrefix)) {
            XWPFRun prefixRun = paragraph.createRun();
            styleBodyRun(prefixRun, false, 11, "374151");
            prefixRun.setText(optionalPrefix);
        }
        appendInlineNodes(paragraph, parent);
        if (!StringUtils.hasText(paragraph.getText()) && StringUtils.hasText(parent.text())) {
            // Fallback if only nested containers
            XWPFRun run = paragraph.createRun();
            styleBodyRun(run, false, 11, "374151");
            run.setText(parent.text().trim());
        }
    }

    private void appendInlineNodes(XWPFParagraph paragraph, Element parent) {
        for (Node child : parent.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text();
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                XWPFRun run = paragraph.createRun();
                styleBodyRun(run, false, 11, "374151");
                run.setText(text);
            } else if (child instanceof Element el) {
                String tag = el.tagName().toLowerCase(Locale.ROOT);
                if ("br".equals(tag)) {
                    paragraph.createRun().addBreak();
                    continue;
                }
                boolean bold = "strong".equals(tag) || "b".equals(tag);
                boolean italic = "em".equals(tag) || "i".equals(tag);
                if (bold || italic || "span".equals(tag)) {
                    XWPFRun run = paragraph.createRun();
                    styleBodyRun(run, bold, 11, bold ? "1F2937" : "374151");
                    run.setItalic(italic);
                    run.setText(el.text());
                } else {
                    appendInlineNodes(paragraph, el);
                }
            }
        }
    }

    private void styleBodyRun(XWPFRun run, boolean bold, int size, String color) {
        run.setBold(bold);
        run.setFontSize(size);
        run.setColor(color);
        run.setFontFamily("Helvetica");
    }

    private void setInfoCell(XWPFTableCell cell, String label, String value) {
        clearCell(cell);
        XWPFParagraph labelP = cell.addParagraph();
        labelP.setSpacingAfter(40);
        XWPFRun lr = labelP.createRun();
        lr.setBold(true);
        lr.setFontSize(9);
        lr.setColor("64748B");
        lr.setFontFamily("Helvetica");
        lr.setText(label);

        XWPFParagraph valueP = cell.addParagraph();
        valueP.setSpacingAfter(80);
        XWPFRun vr = valueP.createRun();
        vr.setFontSize(11);
        vr.setColor("1F2937");
        vr.setFontFamily("Helvetica");
        vr.setText(valueOr(value, "Not provided"));
    }

    private void addRightMeta(XWPFTableCell cell, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun run = p.createRun();
        run.setFontSize(10);
        run.setColor("4B5563");
        run.setFontFamily("Helvetica");
        run.setText(text.trim());
    }

    private void spacer(XWPFDocument document, int afterTwips) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(afterTwips);
    }

    private void clearCell(XWPFTableCell cell) {
        // Remove default empty paragraph to avoid extra blank lines
        int count = cell.getParagraphs().size();
        for (int i = count - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
    }

    /**
     * Fixed table layout with explicit DXA widths — without this Word often collapses
     * auto-fit columns so text stacks one character per line (matches broken DOCX header/info tables).
     */
    private void configureFixedTable(XWPFTable table, int totalWidthDxa) {
        table.setWidth(String.valueOf(totalWidthDxa));
        var ctTbl = table.getCTTbl();
        var tblPr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();

        CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblW.setType(STTblWidth.DXA);
        tblW.setW(BigInteger.valueOf(totalWidthDxa));

        CTTblLayoutType layout = tblPr.isSetTblLayout() ? tblPr.getTblLayout() : tblPr.addNewTblLayout();
        layout.setType(STTblLayoutType.FIXED);
    }

    private void setCellWidth(XWPFTableCell cell, int widthDxa) {
        cell.setWidth(String.valueOf(widthDxa));
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setType(STTblWidth.DXA);
        tcW.setW(BigInteger.valueOf(widthDxa));
    }

    private void setCellVerticalAlignTop(XWPFTableCell cell) {
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);
    }

    private void setCellShading(XWPFTableCell cell, String hexColor) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setFill(hexColor);
    }

    private void clearAllTableBorders(XWPFTable table) {
        try {
            var ctTbl = table.getCTTbl();
            var tblPr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();
            var borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
            for (CTBorder side : new CTBorder[] {
                    borders.isSetTop() ? borders.getTop() : borders.addNewTop(),
                    borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(),
                    borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(),
                    borders.isSetRight() ? borders.getRight() : borders.addNewRight(),
                    borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(),
                    borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV()
            }) {
                side.setVal(STBorder.NONE);
                side.setSz(BigInteger.ZERO);
                side.setColor("FFFFFF");
            }
        } catch (Exception ex) {
            log.debug("Could not clear Word info table borders: {}", ex.getMessage());
        }
    }

    private void styleBorderBottom(XWPFTable table, String hexColor, int sizeEighths) {
        try {
            var ctTbl = table.getCTTbl();
            var tblPr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();
            var borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
            var bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
            bottom.setVal(STBorder.SINGLE);
            bottom.setSz(BigInteger.valueOf(sizeEighths));
            bottom.setColor(hexColor);
            // Hide other borders for a cleaner header underline look
            for (CTBorder side : new CTBorder[] {
                    borders.isSetTop() ? borders.getTop() : borders.addNewTop(),
                    borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(),
                    borders.isSetRight() ? borders.getRight() : borders.addNewRight(),
                    borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(),
                    borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV()
            }) {
                side.setVal(STBorder.NONE);
            }
        } catch (Exception ex) {
            log.debug("Could not style Word header table border: {}", ex.getMessage());
        }
    }

    private String infoCell(String label, String value) {
        return "<td><div class=\"info-label\">" + escapeHtml(label)
                + "</div><div class=\"info-value\">" + escapeHtml(value) + "</div></td>";
    }

    private String formatPreparedBy(User author) {
        if (author == null) {
            return "Not provided";
        }
        if (StringUtils.hasText(author.getTitle())) {
            return author.getFullName() + ", " + author.getTitle();
        }
        return author.getFullName();
    }

    private String formatLicenseLine(User author, UserProfile profile) {
        if (profile != null && StringUtils.hasText(profile.getLicenseType())) {
            String line = profile.getLicenseType();
            if (StringUtils.hasText(profile.getLicenseNumber())) {
                line += " #" + profile.getLicenseNumber();
            }
            return line;
        }
        return author != null ? valueOr(author.getTitle(), "") : "";
    }

    private ZoneId resolveZoneId(PracticeConfigurationResponse practice) {
        if (practice != null && StringUtils.hasText(practice.getTimezone())) {
            try {
                return ZoneId.of(practice.getTimezone());
            } catch (Exception ignored) {
                log.debug("Invalid practice timezone {}, using UTC", practice.getTimezone());
            }
        }
        return ZoneId.of("UTC");
    }

    private String formatInstant(Instant instant, ZoneId zoneId, String fallback) {
        if (instant == null) {
            return fallback != null ? fallback : "N/A";
        }
        return DISPLAY_DATE.format(instant.atZone(zoneId));
    }

    private String valueOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
