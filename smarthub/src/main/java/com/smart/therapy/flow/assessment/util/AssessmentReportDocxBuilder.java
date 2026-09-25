package com.smart.therapy.flow.assessment.util;

import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentReport;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.user.entity.UserProfile;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
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
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Assessment Word export matching {@link AssessmentReportHtmlBuilder} / PDF layout.
 */
public final class AssessmentReportDocxBuilder {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US);
    private static final ZoneId PRACTICE_ZONE = ZoneId.of("America/New_York");
    /** Letter page with ~0.5" margins → content width. */
    private static final int CONTENT_WIDTH_DXA = 10800;
    private static final int LEFT_COL_DXA = 6200;
    private static final int RIGHT_COL_DXA = CONTENT_WIDTH_DXA - LEFT_COL_DXA;

    private AssessmentReportDocxBuilder() {
    }

    public static byte[] build(
            AssessmentAssignment assignment,
            AssessmentReport report,
            Map<String, String> practiceSettings,
            UserProfile therapistProfile,
            String genderLabel) throws IOException {
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
        String practiceAddress = valueOr(practiceSettings.get("address"), "").replace("\n", ", ").trim();
        String practicePhone = valueOr(practiceSettings.get("phone"), "N/A");
        String practiceEmail = valueOr(practiceSettings.get("email"), "N/A");
        String practiceWebsite = displayWebsite(valueOr(practiceSettings.get("website"), "N/A"));

        String rawContent = firstNonBlank(
                report.getFinalContent(),
                report.getDraftContent(),
                report.getGeneratedContent());
        String contentHtml = AssessmentReportHtmlBuilder.formatReportContent(rawContent);

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ensurePageSetup(document);

            addHeaderTable(document, clientName, finalized, practiceName, practiceAddress,
                    practicePhone, practiceEmail, practiceWebsite);
            spacer(document, 120);
            addConfidentialityBanner(document);
            spacer(document, 120);
            addClientInfoTable(document, clientName, clientId, dateOfBirth, gender, phone, email,
                    address, assessmentName, completionDate, clinicianName);
            spacer(document, 120);
            appendHtmlAsDocx(document, contentHtml);

            if (finalized && report.getFinalizedAt() != null && assignment.getAssignedBy() != null) {
                spacer(document, 200);
                addSignatureBlock(document,
                        assignment.getAssignedBy().getFullName(),
                        formatLicense(therapistProfile),
                        formatInstant(report.getFinalizedAt()));
            }

            spacer(document, 200);
            addFooter(document, practiceName, practicePhone, practiceEmail);

            document.write(out);
            return out.toByteArray();
        }
    }

    private static void addHeaderTable(
            XWPFDocument document,
            String clientName,
            boolean finalized,
            String practiceName,
            String practiceAddress,
            String practicePhone,
            String practiceEmail,
            String practiceWebsite) {
        XWPFTable table = document.createTable(1, 2);
        configureFixedTable(table, CONTENT_WIDTH_DXA);
        clearAllTableBorders(table);
        styleBorderBottom(table, "2563EB", 24);

        XWPFTableRow row = table.getRow(0);
        XWPFTableCell left = row.getCell(0);
        XWPFTableCell right = row.getCell(1);
        setCellWidth(left, LEFT_COL_DXA);
        setCellWidth(right, RIGHT_COL_DXA);
        setCellVerticalAlignTop(left);
        setCellVerticalAlignTop(right);
        clearCell(left);
        clearCell(right);

        String title = "Clinical Assessment Report" + (finalized ? " Finalized" : "");
        addCellParagraph(left, title, true, 18, "333333", ParagraphAlignment.LEFT, 40);
        addCellParagraph(left, clientName, false, 11, "6B7280", ParagraphAlignment.LEFT, 40);

        addCellParagraph(right, practiceName, true, 12, "1E40AF", ParagraphAlignment.RIGHT, 40);
        addCellParagraph(right, practiceAddress, false, 10, "4B5563", ParagraphAlignment.RIGHT, 20);
        addCellParagraph(right, "Phone: " + practicePhone, false, 10, "4B5563", ParagraphAlignment.RIGHT, 20);
        addCellParagraph(right, "Email: " + practiceEmail, false, 10, "4B5563", ParagraphAlignment.RIGHT, 20);
        addCellParagraph(right, "Website: " + practiceWebsite, false, 10, "4B5563", ParagraphAlignment.RIGHT, 40);
    }

    private static void addConfidentialityBanner(XWPFDocument document) {
        XWPFTable table = document.createTable(1, 1);
        configureFixedTable(table, CONTENT_WIDTH_DXA);
        clearAllTableBorders(table);
        styleBorderLeft(table, "F59E0B", 48);

        XWPFTableCell cell = table.getRow(0).getCell(0);
        setCellWidth(cell, CONTENT_WIDTH_DXA);
        setCellShading(cell, "FEF3C7");
        clearCell(cell);

        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(80);
        p.setSpacingAfter(80);
        XWPFRun run = styleRun(p.createRun(), true, 9, "92400E");
        run.setText("⚠ PERSONAL AND CONFIDENTIAL – Protected Health Information. "
                + "Unauthorized use or disclosure is prohibited under HIPAA.");
    }

    private static void addClientInfoTable(
            XWPFDocument document,
            String clientName,
            String clientId,
            String dateOfBirth,
            String gender,
            String phone,
            String email,
            String address,
            String assessmentName,
            String completionDate,
            String clinicianName) {
        XWPFTable table = document.createTable(1, 1);
        configureFixedTable(table, CONTENT_WIDTH_DXA);
        styleBoxBorder(table, "E2E8F0", 8);
        XWPFTableCell outer = table.getRow(0).getCell(0);
        setCellWidth(outer, CONTENT_WIDTH_DXA);
        setCellShading(outer, "F3F4F6");
        clearCell(outer);

        XWPFParagraph title = outer.addParagraph();
        title.setSpacingAfter(80);
        styleRun(title.createRun(), true, 13, "1E40AF").setText("CLIENT INFORMATION");
        addParagraphBottomBorder(title, "DBEAFE", 16);

        addInfoPair(outer, "CLIENT NAME", clientName, "CLIENT ID", clientId);
        addInfoPair(outer, "DATE OF BIRTH", dateOfBirth, "GENDER", gender);
        addInfoPair(outer, "PHONE NUMBER", phone, "EMAIL ADDRESS", email);
        addInfoPair(outer, "ADDRESS", address, null, null);
        addInfoPair(outer, "ASSESSMENT", assessmentName, "COMPLETION DATE", completionDate);
        addInfoPair(outer, "CLINICIAN", clinicianName, null, null);
    }

    private static void addInfoPair(
            XWPFTableCell parent,
            String leftLabel,
            String leftValue,
            String rightLabel,
            String rightValue) {
        XWPFParagraph labels = parent.addParagraph();
        labels.setSpacingBefore(60);
        labels.setSpacingAfter(0);
        setMidPageTab(labels);
        styleRun(labels.createRun(), true, 9, "64748B").setText(leftLabel);
        if (StringUtils.hasText(rightLabel)) {
            labels.createRun().setText("\t");
            styleRun(labels.createRun(), true, 9, "64748B").setText(rightLabel);
        }

        XWPFParagraph values = parent.addParagraph();
        values.setSpacingBefore(0);
        values.setSpacingAfter(60);
        setMidPageTab(values);
        styleRun(values.createRun(), false, 11, "1F2937").setText(valueOr(leftValue, "Not provided"));
        if (StringUtils.hasText(rightLabel)) {
            values.createRun().setText("\t");
            styleRun(values.createRun(), false, 11, "1F2937").setText(valueOr(rightValue, "Not provided"));
        }
    }

    private static void addSignatureBlock(
            XWPFDocument document,
            String clinicianName,
            String license,
            String signedDate) {
        XWPFTable table = document.createTable(1, 1);
        configureFixedTable(table, CONTENT_WIDTH_DXA);
        clearAllTableBorders(table);
        styleBorderTop(table, "2563EB", 24);

        XWPFTableCell cell = table.getRow(0).getCell(0);
        setCellWidth(cell, CONTENT_WIDTH_DXA);
        setCellShading(cell, "F9FAFB");
        clearCell(cell);

        addCellParagraph(cell, clinicianName, true, 12, "1F2937", ParagraphAlignment.LEFT, 40);
        if (StringUtils.hasText(license)) {
            addCellParagraph(cell, license, false, 10, "6B7280", ParagraphAlignment.LEFT, 20);
        }
        XWPFParagraph datePara = cell.addParagraph();
        datePara.setSpacingAfter(60);
        XWPFRun dateRun = styleRun(datePara.createRun(), false, 10, "9CA3AF");
        dateRun.setItalic(true);
        dateRun.setText("Digitally signed on " + signedDate);
    }

    private static void addFooter(
            XWPFDocument document,
            String practiceName,
            String practicePhone,
            String practiceEmail) {
        XWPFParagraph line = document.createParagraph();
        line.setSpacingBefore(80);
        line.setSpacingAfter(120);
        addParagraphBottomBorder(line, "E5E7EB", 8);

        XWPFParagraph footer = document.createParagraph();
        footer.setAlignment(ParagraphAlignment.CENTER);
        footer.setSpacingAfter(0);
        styleRun(footer.createRun(), false, 9, "9CA3AF")
                .setText(practiceName + " | " + practicePhone + " | " + practiceEmail);
    }

    private static void appendHtmlAsDocx(XWPFDocument document, String html) {
        if (!StringUtils.hasText(html)) {
            return;
        }
        Element body = Jsoup.parseBodyFragment(html).body();
        for (Node node : body.childNodes()) {
            appendDocxNode(document, node);
        }
    }

    private static void appendDocxNode(XWPFDocument document, Node node) {
        if (node instanceof TextNode textNode) {
            String text = textNode.text();
            if (!StringUtils.hasText(text) || text.isBlank()) {
                return;
            }
            XWPFParagraph p = document.createParagraph();
            p.setSpacingAfter(60);
            styleRun(p.createRun(), false, 11, "374151").setText(text.trim());
            return;
        }
        if (!(node instanceof Element element)) {
            return;
        }
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        switch (tag) {
            case "h2" -> {
                XWPFParagraph p = document.createParagraph();
                p.setSpacingBefore(160);
                p.setSpacingAfter(60);
                addParagraphBottomBorder(p, "E5E7EB", 8);
                styleRun(p.createRun(), true, 13, "1E40AF")
                        .setText(element.text().trim().toUpperCase(Locale.US));
            }
            case "h3" -> {
                XWPFParagraph p = document.createParagraph();
                p.setSpacingBefore(100);
                p.setSpacingAfter(40);
                styleRun(p.createRun(), true, 12, "2563EB").setText(element.text().trim());
            }
            case "p" -> {
                if (!StringUtils.hasText(element.text())) {
                    return;
                }
                XWPFParagraph p = document.createParagraph();
                p.setSpacingAfter(60);
                appendInlineRuns(p, element);
            }
            case "ul" -> {
                for (Element li : element.children()) {
                    if ("li".equalsIgnoreCase(li.tagName())) {
                        appendListItem(document, li, "• ");
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
            default -> {
                for (Node child : element.childNodes()) {
                    appendDocxNode(document, child);
                }
            }
        }
    }

    private static void appendListItem(XWPFDocument document, Element li, String prefix) {
        if (!StringUtils.hasText(li.text())) {
            return;
        }
        XWPFParagraph p = document.createParagraph();
        p.setSpacingBefore(40);
        p.setSpacingAfter(80);
        p.setIndentationLeft(200);
        styleRun(p.createRun(), false, 11, "374151").setText(prefix + li.text().trim());
    }

    private static void appendInlineRuns(XWPFParagraph paragraph, Element parent) {
        for (Node child : parent.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text();
                if (StringUtils.hasText(text)) {
                    styleRun(paragraph.createRun(), false, 11, "374151").setText(text);
                }
            } else if (child instanceof Element el) {
                String tag = el.tagName().toLowerCase(Locale.ROOT);
                if ("br".equals(tag)) {
                    paragraph.createRun().addBreak();
                    continue;
                }
                boolean bold = "strong".equals(tag) || "b".equals(tag);
                boolean italic = "em".equals(tag) || "i".equals(tag);
                XWPFRun run = styleRun(paragraph.createRun(), bold, 11, "374151");
                run.setItalic(italic);
                run.setText(el.text());
            }
        }
        if (!StringUtils.hasText(paragraph.getText()) && StringUtils.hasText(parent.text())) {
            styleRun(paragraph.createRun(), false, 11, "374151").setText(parent.text().trim());
        }
    }

    private static void addCellParagraph(
            XWPFTableCell cell,
            String text,
            boolean bold,
            int size,
            String color,
            ParagraphAlignment align,
            int after) {
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(align);
        p.setSpacingAfter(after);
        styleRun(p.createRun(), bold, size, color).setText(text != null ? text : "");
    }

    private static XWPFRun styleRun(XWPFRun run, boolean bold, int size, String color) {
        run.setBold(bold);
        run.setFontSize(size);
        run.setColor(color);
        run.setFontFamily("Helvetica");
        return run;
    }

    private static void spacer(XWPFDocument document, int afterTwips) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(afterTwips);
    }

    private static void ensurePageSetup(XWPFDocument document) {
        var body = document.getDocument().getBody();
        var sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        var pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pgSz.setW(BigInteger.valueOf(12240));
        pgSz.setH(BigInteger.valueOf(15840));
        var pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pgMar.setLeft(BigInteger.valueOf(720));
        pgMar.setRight(BigInteger.valueOf(720));
        pgMar.setTop(BigInteger.valueOf(720));
        pgMar.setBottom(BigInteger.valueOf(792));
    }

    private static void configureFixedTable(XWPFTable table, int totalWidthDxa) {
        table.setWidth(String.valueOf(totalWidthDxa));
        table.setWidthType(TableWidthType.DXA);
        var ctTbl = table.getCTTbl();
        var tblPr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();
        CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblW.setType(STTblWidth.DXA);
        tblW.setW(BigInteger.valueOf(totalWidthDxa));
        CTTblLayoutType layout = tblPr.isSetTblLayout() ? tblPr.getTblLayout() : tblPr.addNewTblLayout();
        layout.setType(STTblLayoutType.FIXED);
    }

    private static void setCellWidth(XWPFTableCell cell, int widthDxa) {
        cell.setWidth(String.valueOf(widthDxa));
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setType(STTblWidth.DXA);
        tcW.setW(BigInteger.valueOf(widthDxa));
    }

    private static void setCellVerticalAlignTop(XWPFTableCell cell) {
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);
    }

    private static void setCellShading(XWPFTableCell cell, String hexColor) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setFill(hexColor);
    }

    private static void clearCell(XWPFTableCell cell) {
        int count = cell.getParagraphs().size();
        for (int i = count - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
    }

    private static void clearAllTableBorders(XWPFTable table) {
        var borders = tableBorders(table);
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
    }

    private static org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders tableBorders(XWPFTable table) {
        var ctTbl = table.getCTTbl();
        var tblPr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();
        return tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    }

    private static void styleBorderBottom(XWPFTable table, String hexColor, int sizeEighths) {
        var borders = tableBorders(table);
        var bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(BigInteger.valueOf(sizeEighths));
        bottom.setColor(hexColor);
        bottom.setSpace(BigInteger.valueOf(4));
    }

    private static void styleBorderTop(XWPFTable table, String hexColor, int sizeEighths) {
        var borders = tableBorders(table);
        var top = borders.isSetTop() ? borders.getTop() : borders.addNewTop();
        top.setVal(STBorder.SINGLE);
        top.setSz(BigInteger.valueOf(sizeEighths));
        top.setColor(hexColor);
        top.setSpace(BigInteger.ZERO);
    }

    private static void styleBorderLeft(XWPFTable table, String hexColor, int sizeEighths) {
        var borders = tableBorders(table);
        var left = borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft();
        left.setVal(STBorder.SINGLE);
        left.setSz(BigInteger.valueOf(sizeEighths));
        left.setColor(hexColor);
        left.setSpace(BigInteger.ZERO);
    }

    private static void styleBoxBorder(XWPFTable table, String hexColor, int sizeEighths) {
        var borders = tableBorders(table);
        for (CTBorder side : new CTBorder[] {
                borders.isSetTop() ? borders.getTop() : borders.addNewTop(),
                borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(),
                borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(),
                borders.isSetRight() ? borders.getRight() : borders.addNewRight()
        }) {
            side.setVal(STBorder.SINGLE);
            side.setSz(BigInteger.valueOf(sizeEighths));
            side.setColor(hexColor);
            side.setSpace(BigInteger.ZERO);
        }
        if (borders.isSetInsideH()) {
            borders.getInsideH().setVal(STBorder.NONE);
        }
        if (borders.isSetInsideV()) {
            borders.getInsideV().setVal(STBorder.NONE);
        }
    }

    private static void addParagraphBottomBorder(XWPFParagraph paragraph, String hexColor, int sizeEighths) {
        var ctp = paragraph.getCTP();
        var pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        var pBdr = pPr.isSetPBdr() ? pPr.getPBdr() : pPr.addNewPBdr();
        var bottom = pBdr.isSetBottom() ? pBdr.getBottom() : pBdr.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(BigInteger.valueOf(sizeEighths));
        bottom.setColor(hexColor);
        bottom.setSpace(BigInteger.valueOf(4));
    }

    private static void setMidPageTab(XWPFParagraph paragraph) {
        var ctp = paragraph.getCTP();
        var pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        var tabs = pPr.isSetTabs() ? pPr.getTabs() : pPr.addNewTabs();
        while (tabs.sizeOfTabArray() > 0) {
            tabs.removeTab(0);
        }
        var tab = tabs.addNewTab();
        tab.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc.LEFT);
        tab.setPos(BigInteger.valueOf(CONTENT_WIDTH_DXA / 2L));
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
}
