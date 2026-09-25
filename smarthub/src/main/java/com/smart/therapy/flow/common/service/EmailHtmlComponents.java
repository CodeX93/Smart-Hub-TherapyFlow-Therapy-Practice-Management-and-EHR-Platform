package com.smart.therapy.flow.common.service;

/**
 * Shared Clinical Clarity email primitives for SmartHub transactional mail.
 * Inline styles + hosted PNG icons (Gmail/Outlook strip inline SVG).
 */
public final class EmailHtmlComponents {

    public static final String PRIMARY = "#3C4D58";
    public static final String CANVAS = "#F8FAFB";
    public static final String CARD_BORDER = "#E1E4E8";
    public static final String TEXT = "#1B1C1C";
    public static final String MUTED = "#43474B";
    public static final String LOGO_URL = "https://app.therapyflow.pro/smarthub-brain-icon-512.png";
    /** Hosted next to the logo on the app CDN (Gmail strips inline SVG). */
    public static final String ICON_BASE_URL = "https://app.therapyflow.pro/email-icons";
    public static final String PRODUCT_NAME = "SmartHub";

    /**
     * Shared &lt;style&gt; block for Apple Mail / iOS / modern clients.
     * Fluid tables + inline styles remain the Outlook fallback.
     */
    public static String clientStyles() {
        return "<style type=\"text/css\">"
                + "html,body{margin:0!important;padding:0!important;width:100%!important;-webkit-text-size-adjust:100%;"
                + "-ms-text-size-adjust:100%;}"
                + "table,td{border-collapse:collapse;mso-table-lspace:0;mso-table-rspace:0;}"
                + "img{border:0;outline:none;text-decoration:none;-ms-interpolation-mode:bicubic;}"
                + "a{word-break:break-word;}"
                + ".email-content{word-break:break-word;overflow-wrap:anywhere;}"
                + ".email-btn-link{max-width:100%;box-sizing:border-box;}"
                + "@media only screen and (max-width:620px){"
                + ".email-outer{padding:12px 6px!important;}"
                + ".email-card{width:100%!important;max-width:100%!important;border-radius:0!important;}"
                + ".email-header-pad{padding:16px 14px 10px 14px!important;}"
                + ".email-brand{font-size:17px!important;line-height:28px!important;}"
                + ".email-body-pad{padding:20px 14px!important;}"
                + ".email-footer-pad{padding:16px 14px!important;font-size:11px!important;line-height:16px!important;}"
                + ".email-h1{font-size:18px!important;line-height:24px!important;letter-spacing:-0.01em!important;}"
                + ".email-p{font-size:14px!important;line-height:20px!important;}"
                + ".email-kv-label{font-size:12px!important;line-height:16px!important;}"
                + ".email-kv-value{font-size:13px!important;line-height:18px!important;}"
                + ".email-icon-label{font-size:10px!important;}"
                + ".email-icon-value{font-size:13px!important;line-height:18px!important;}"
                + ".email-btn{margin:14px 0!important;}"
                + ".email-btn-link{display:block!important;width:100%!important;padding-left:14px!important;"
                + "padding-right:14px!important;box-sizing:border-box!important;text-align:center!important;"
                + "font-size:14px!important;min-height:42px!important;line-height:42px!important;}"
                + ".email-kv-label,.email-kv-value{display:block!important;width:100%!important;"
                + "box-sizing:border-box!important;text-align:left!important;}"
                + ".email-kv-label{padding:12px 14px 2px 14px!important;border-bottom:none!important;}"
                + ".email-kv-value{padding:2px 14px 12px 14px!important;}"
                + ".email-icon-cell{padding-left:10px!important;padding-right:6px!important;}"
                + ".email-icon-text{padding-right:10px!important;}"
                + ".email-otp{font-size:22px!important;letter-spacing:0.1em!important;}"
                + ".email-muted{font-size:11px!important;line-height:16px!important;}"
                + "}"
                + "</style>";
    }

    private EmailHtmlComponents() {
    }

    // --- Hosted PNG icons (email-safe; inline SVG is stripped by Gmail/Outlook) ---

    public static String iconCalendar() {
        return iconImg("calendar", 20);
    }

    public static String iconClock() {
        return iconImg("clock", 20);
    }

    public static String iconUser() {
        return iconImg("user", 20);
    }

    public static String iconMapPin() {
        return iconImg("map-pin", 20);
    }

    public static String iconHash() {
        return iconImg("hash", 20);
    }

    public static String iconBriefcase() {
        return iconImg("briefcase", 20);
    }

    public static String iconVideo() {
        return iconImg("video", 20);
    }

    public static String iconReceipt() {
        return iconImg("receipt", 20);
    }

    public static String iconCalendarCheck() {
        return iconImg("calendar-check", 20);
    }

    public static String iconMail() {
        return iconImg("mail", 20);
    }

    public static String iconLock() {
        return iconImg("lock", 20);
    }

    public static String iconBell() {
        return iconImg("bell", 20);
    }

    public static String iconShield() {
        return iconImg("shield", 20);
    }

    public static String iconCheckCircle() {
        return iconImg("check-circle", 20);
    }

    public static String iconAlertCircle() {
        return iconImg("alert-circle", 20);
    }

    public static String iconXCircle() {
        return iconImg("x-circle", 20);
    }

    private static String iconImg(String name, int sizePx) {
        return "<img src=\"" + ICON_BASE_URL + "/" + name + ".png\" width=\"" + sizePx + "\" height=\"" + sizePx
                + "\" alt=\"\" style=\"display:block;margin:0 auto;width:" + sizePx + "px;height:" + sizePx
                + "px;border:0;outline:none;line-height:0;\" />";
    }

    /**
     * Hero icon badges removed — Hostinger and other webmail clients mis-align the centered
     * square marks. Kept as a no-op so existing body builders stay compatible.
     */
    public static String heroBadge(String iconHtml) {
        return "";
    }

    private static String heroIcon(String iconHtml) {
        return heroBadge(iconHtml);
    }

    public static String heading(String text) {
        return "<h1 class=\"email-h1\" style=\"margin:0 0 16px 0;font-size:22px;font-weight:700;line-height:30px;letter-spacing:-0.02em;color:"
                + TEXT + ";\">" + text + "</h1>";
    }

    public static String headingCentered(String text) {
        return "<h1 class=\"email-h1\" style=\"margin:0 0 16px 0;font-size:22px;font-weight:700;line-height:30px;letter-spacing:-0.02em;color:"
                + PRIMARY + ";text-align:center;\">" + text + "</h1>";
    }

    public static String paragraph(String text) {
        return "<p class=\"email-p\" style=\"margin:0 0 16px 0;font-size:16px;line-height:24px;color:" + MUTED + ";\">"
                + text + "</p>";
    }

    public static String paragraphCentered(String text) {
        return "<p class=\"email-p\" style=\"margin:0 0 16px 0;font-size:16px;line-height:24px;color:" + MUTED
                + ";text-align:center;\">" + text + "</p>";
    }

    public static String mutedNote(String text) {
        return "<p class=\"email-p email-muted\" style=\"margin:16px 0 0 0;font-size:12px;line-height:18px;color:"
                + MUTED + ";\">" + text + "</p>";
    }

    public static String primaryButton(String label, String href) {
        return primaryButton(label, href, PRIMARY);
    }

    public static String primaryButton(String label, String href, String brandColor) {
        String color = (brandColor != null && !brandColor.isBlank()) ? brandColor : PRIMARY;
        return "<div class=\"email-btn\" style=\"margin:24px 0;text-align:center;\">"
                + "<a class=\"email-btn-link\" href=\"" + href
                + "\" style=\"display:inline-block;min-height:44px;line-height:44px;"
                + "padding:0 28px;background-color:" + color + ";color:#ffffff;text-decoration:none;"
                + "border-radius:4px;font-size:16px;font-weight:600;max-width:100%;box-sizing:border-box;\">"
                + label + "</a>"
                + "</div>";
    }

    public static String primaryButtonTemplate(String label, String hrefPlaceholder) {
        return "<div class=\"email-btn\" style=\"margin:24px 0;text-align:center;\">"
                + "<a class=\"email-btn-link\" href=\"" + hrefPlaceholder
                + "\" style=\"display:inline-block;min-height:44px;line-height:44px;"
                + "padding:0 28px;background-color:" + PRIMARY + ";color:#ffffff;text-decoration:none;"
                + "border-radius:4px;font-size:16px;font-weight:600;max-width:100%;box-sizing:border-box;\">"
                + label + "</a>"
                + "</div>";
    }

    public static String fullWidthPrimaryButtonTemplate(String label, String hrefPlaceholder) {
        return "<div class=\"email-btn\" style=\"margin:16px 0 0 0;\">"
                + "<a class=\"email-btn-link\" href=\"" + hrefPlaceholder
                + "\" style=\"display:block;width:100%;box-sizing:border-box;"
                + "text-align:center;min-height:48px;line-height:48px;padding:0 16px;background-color:"
                + PRIMARY + ";color:#ffffff;text-decoration:none;border-radius:8px;font-size:16px;font-weight:600;\">"
                + label + "</a></div>";
    }

    public static String fullWidthOutlineButtonTemplate(String label, String hrefPlaceholder) {
        return "<div class=\"email-btn\" style=\"margin:12px 0 0 0;\">"
                + "<a class=\"email-btn-link\" href=\"" + hrefPlaceholder
                + "\" style=\"display:block;width:100%;box-sizing:border-box;"
                + "text-align:center;min-height:48px;line-height:46px;padding:0 16px;background-color:#ffffff;"
                + "color:" + PRIMARY + ";text-decoration:none;border:1.5px solid " + PRIMARY
                + ";border-radius:8px;font-size:16px;font-weight:600;\">"
                + label + "</a></div>";
    }

    public static String textLinkWithIcon(String iconHtml, String label, String hrefPlaceholder) {
        String smallIcon = iconHtml
                .replace("width=\"20\"", "width=\"16\"")
                .replace("height=\"20\"", "height=\"16\"")
                .replace("width:20px", "width:16px")
                .replace("height:20px", "height:16px");
        return "<div style=\"text-align:center;margin:8px 0 0 0;\">"
                + "<a href=\"" + hrefPlaceholder + "\" style=\"display:inline-block;font-size:14px;color:"
                + PRIMARY + ";text-decoration:underline;\">"
                + "<span style=\"display:inline-block;vertical-align:middle;margin-right:6px;line-height:0;\">"
                + smallIcon
                + "</span>"
                + "<span style=\"vertical-align:middle;\">" + label + "</span></a></div>";
    }

    /**
     * Google Calendar "create event" deep link.
     * {@code start}/{@code end} should be UTC instants.
     */
    public static String googleCalendarUrl(
            String title,
            java.time.Instant start,
            java.time.Instant end,
            String details,
            String location) {
        if (start == null) {
            return "https://calendar.google.com/calendar/r";
        }
        java.time.Instant safeEnd = end != null ? end : start.plus(java.time.Duration.ofMinutes(45));
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(java.time.ZoneOffset.UTC);
        String dates = fmt.format(start) + "/" + fmt.format(safeEnd);
        StringBuilder url = new StringBuilder("https://calendar.google.com/calendar/render?action=TEMPLATE");
        url.append("&text=").append(urlEncode(title != null ? title : "Therapy Session"));
        url.append("&dates=").append(dates);
        if (hasText(details)) {
            url.append("&details=").append(urlEncode(details));
        }
        if (hasText(location)) {
            url.append("&location=").append(urlEncode(stripHtml(location)));
        }
        return url.toString();
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static String stripHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    public static String detailCard(String innerHtml) {
        return "<div style=\"background-color:#ffffff;padding:20px;border-radius:8px;margin:24px 0;"
                + "border:1px solid " + CARD_BORDER + ";overflow:hidden;\">" + innerHtml + "</div>";
    }

    public static String detailCardFlush(String innerHtml) {
        return "<div style=\"background-color:#ffffff;padding:0;border-radius:8px;margin:24px 0;"
                + "border:1px solid " + CARD_BORDER + ";overflow:hidden;\">" + innerHtml + "</div>";
    }

    public static String detailRow(String label, String value) {
        return "<p style=\"margin:4px 0;font-size:14px;line-height:22px;color:" + MUTED + ";\">"
                + "<strong style=\"color:" + TEXT + ";\">" + label + ":</strong> " + value + "</p>";
    }

    public static String detailRowTemplate(String label, String valuePlaceholder) {
        return detailRow(label, valuePlaceholder);
    }

    public static String iconDetailRow(String svgIcon, String label, String value, boolean last) {
        String border = last ? "" : "border-bottom:1px solid " + CARD_BORDER + ";";
        return "<tr>"
                + "<td class=\"email-icon-cell\" style=\"padding:16px 12px 16px 20px;" + border
                + "vertical-align:middle;width:40px;\">"
                + svgIcon + "</td>"
                + "<td class=\"email-icon-text\" style=\"padding:16px 20px 16px 4px;" + border
                + "vertical-align:middle;\">"
                + "<div class=\"email-icon-label\" style=\"font-size:11px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;"
                + "color:#73787B;margin:0 0 4px 0;\">" + label + "</div>"
                + "<div class=\"email-icon-value\" style=\"font-size:15px;line-height:22px;color:" + PRIMARY
                + ";word-break:break-word;\">" + value + "</div>"
                + "</td></tr>";
    }

    public static String invoiceKvRow(String label, String value, boolean last) {
        String border = last ? "" : "border-bottom:1px solid " + CARD_BORDER + ";";
        return "<tr>"
                + "<td class=\"email-kv-label\" style=\"padding:14px 20px;" + border + "font-size:14px;color:" + MUTED
                + ";text-align:left;\">" + label + "</td>"
                + "<td class=\"email-kv-value\" style=\"padding:14px 20px;" + border
                + "font-size:14px;font-weight:600;color:"
                + TEXT + ";text-align:right;word-break:break-word;\">" + value + "</td></tr>";
    }

    public static String kvTable(String... rowsHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        for (String row : rowsHtml) {
            if (row != null && !row.isBlank()) {
                sb.append(row);
            }
        }
        sb.append("</table>");
        return detailCardFlush(sb.toString());
    }

    public static String sessionDetailsCardHtml(
            String dateValue,
            String timeValue,
            String clientMrnValue,
            String providerValue,
            String locationValue,
            String serviceValue) {
        StringBuilder rows = new StringBuilder();
        rows.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        if (hasText(dateValue)) {
            rows.append(iconDetailRow(iconCalendar(), "Date", dateValue, false));
        }
        if (hasText(timeValue)) {
            rows.append(iconDetailRow(iconClock(), "Time", timeValue, false));
        }
        if (hasText(clientMrnValue)) {
            rows.append(iconDetailRow(iconHash(), "Client MRN", clientMrnValue, false));
        }
        if (hasText(providerValue)) {
            rows.append(iconDetailRow(iconUser(), "Provider", providerValue, false));
        }
        if (hasText(locationValue)) {
            boolean last = !hasText(serviceValue);
            String locLower = locationValue.toLowerCase();
            String locIcon = locLower.contains("telehealth")
                    || locLower.contains("video")
                    || locLower.contains("zoom")
                    || locLower.contains("online")
                    ? iconVideo()
                    : iconMapPin();
            rows.append(iconDetailRow(locIcon, "Location", locationValue, last));
        }
        if (hasText(serviceValue)) {
            rows.append(iconDetailRow(iconBriefcase(), "Service", serviceValue, true));
        }
        rows.append("</table>");
        return detailCardFlush(rows.toString());
    }

    public static String sessionScheduledEmailBody() {
        return heroIcon(iconCalendarCheck())
                + heading("New Session Scheduled")
                + paragraph("Your upcoming session has been confirmed. Please find the details below."
                        + " For virtual sessions, the Zoom meeting link and join button appear with the session details.")
                + "{{sessionDetailsHtml}}"
                + primaryButtonTemplate("View Session Details", "{{sessionUrl}}")
                + textLinkWithIcon(iconCalendarCheck(), "Add to Calendar", "{{calendarUrl}}");
    }

    public static String sessionReminderEmailBody() {
        return heroIcon(iconBell())
                + heading("Session Reminder")
                + paragraph("Reminder for an upcoming session with client <strong>{{clientMrn}}</strong>:")
                + "{{sessionDetailsHtml}}"
                + primaryButtonTemplate("View Session Details", "{{sessionUrl}}")
                + textLinkWithIcon(iconCalendarCheck(), "Add to Calendar", "{{calendarUrl}}");
    }

    public static String sessionConfirmationEmailBody() {
        return heroIcon(iconCalendarCheck())
                + heading("Session Confirmation")
                + paragraph("Your session has been confirmed. Please find the details below."
                        + " For virtual sessions, use the Zoom meeting link in the details to join at the scheduled time.")
                + "{{sessionDetailsHtml}}"
                + primaryButtonTemplate("View Session Details", "{{sessionUrl}}")
                + textLinkWithIcon(iconCalendarCheck(), "Add to Calendar", "{{calendarUrl}}");
    }

    public static String sessionRescheduledEmailBody() {
        return heroIcon(iconCalendar())
                + heading("Session Rescheduled")
                + paragraph("A session for client <strong>{{clientMrn}}</strong> has been rescheduled. Updated details:")
                + "{{sessionDetailsHtml}}"
                + primaryButtonTemplate("View Session Details", "{{sessionUrl}}");
    }

    public static String zoomMeetingCard(String joinUrl, String password) {
        if (!hasText(joinUrl)) {
            return "";
        }
        String passwordRow = hasText(password)
                ? detailRow("Password", escape(password))
                : "";
        return detailCard(
                "<div style=\"margin:0 0 12px 0;\">" + iconVideo() + "</div>"
                        + "<p style=\"margin:0 0 8px 0;font-size:14px;font-weight:600;color:" + TEXT
                        + ";\">Zoom Meeting Details</p>"
                        + detailRow("Join URL",
                                "<a href=\"" + escape(joinUrl) + "\" style=\"color:" + PRIMARY
                                        + ";word-break:break-all;\">" + escape(joinUrl) + "</a>")
                        + passwordRow)
                + primaryButton("Join Zoom Meeting", joinUrl);
    }

    /**
     * Direct (non-catalog) session confirmation / reminder / cancellation for clients and therapists.
     */
    public static String sessionPartyEmailBody(
            String heroSvg,
            String title,
            String greetingName,
            String introHtml,
            String date,
            String timeWithTimezone,
            String durationMinutes,
            String sessionType,
            String location,
            String zoomHtml,
            String followUpHtml,
            String contactNoteHtml) {
        String serviceLabel = hasText(sessionType)
                ? sessionType + (hasText(durationMinutes) ? " (" + durationMinutes + " min)" : "")
                : (hasText(durationMinutes) ? durationMinutes + " min" : null);
        return heroIcon(heroSvg)
                + heading(title)
                + paragraph("Hi " + greetingName + ",")
                + paragraph(introHtml)
                + sessionDetailsCardHtml(date, timeWithTimezone, null, null, location, serviceLabel)
                + (hasText(zoomHtml) ? zoomHtml : "")
                + (hasText(followUpHtml) ? paragraph(followUpHtml) : "")
                + (hasText(contactNoteHtml) ? mutedNote(contactNoteHtml) : "");
    }

    public static String recurringSeriesConfirmationEmailBody(
            String greetingName,
            String therapistName,
            String serviceName,
            int sessionCount,
            String datesListItemsHtml) {
        return recurringSeriesConfirmationEmailBody(
                greetingName, therapistName, serviceName, sessionCount, datesListItemsHtml, "America/New_York");
    }

    public static String recurringSeriesConfirmationEmailBody(
            String greetingName,
            String therapistName,
            String serviceName,
            int sessionCount,
            String datesListItemsHtml,
            String timezoneId) {
        String zoneLabel = hasText(timezoneId) ? timezoneId : "UTC";
        return heroIcon(iconCalendarCheck())
                + heading("Recurring Appointment Series Confirmed")
                + paragraph("Hi " + escape(greetingName) + ",")
                + paragraph("Your recurring therapy sessions with <strong>" + escape(therapistName)
                        + "</strong> have been scheduled.")
                + kvTable(
                        invoiceKvRow("Service", escape(serviceName), false),
                        invoiceKvRow("Total sessions", String.valueOf(sessionCount), true))
                + "<ul style=\"margin:0 0 16px 0;padding-left:20px;line-height:1.8;color:" + MUTED + ";\">"
                + datesListItemsHtml
                + "</ul>"
                + paragraph("You will receive a reminder before each individual session.")
                + mutedNote("All times shown in practice timezone (" + escape(zoneLabel) + ").");
    }

    public static String otpEmailBody(String code) {
        return heroIcon(iconLock())
                + heading("Your sign-in code")
                + paragraph("Use this code to finish signing in to SmartHub. It expires in 5 minutes.")
                + otpBlock(code)
                + mutedNote("If you did not request this, you can ignore this email.");
    }

    public static String assessmentAssignedEmailBody(
            String greetingName, String clientMrn, String templateName, String dueDate, String portalUrl) {
        return heroIcon(iconBriefcase())
                + heading("New assessment assigned")
                + paragraph("Dear " + greetingName + ",")
                + paragraph("A new assessment has been assigned to you:")
                + kvTable(
                        invoiceKvRow("Client MRN", hasText(clientMrn) ? clientMrn : "N/A", false),
                        invoiceKvRow("Assessment", templateName, false),
                        invoiceKvRow("Due Date", dueDate, true))
                + paragraph("Please complete this assessment at your earliest convenience.")
                + primaryButton("Complete Assessment", portalUrl);
    }

    public static String assessmentCompletedEmailBody(
            String greetingName, String clientMrn, String templateName, String score) {
        return heroIcon(iconCheckCircle())
                + heading("Assessment completed")
                + paragraph("Dear " + greetingName + ",")
                + paragraph("Thank you for completing the assessment:")
                + kvTable(
                        invoiceKvRow("Client MRN", hasText(clientMrn) ? clientMrn : "N/A", false),
                        invoiceKvRow("Assessment", templateName, false),
                        invoiceKvRow("Score", score, true))
                + paragraph(
                        "Your therapist will review your responses and discuss them with you in your next session.");
    }

    public static String assessmentReminderEmailBody(
            String greetingName, String clientMrn, String templateName, String dueDate, String portalUrl) {
        return heroIcon(iconBell())
                + heading("Assessment reminder")
                + paragraph("Dear " + greetingName + ",")
                + paragraph("This is a reminder that you have a pending assessment:")
                + kvTable(
                        invoiceKvRow("Client MRN", hasText(clientMrn) ? clientMrn : "N/A", false),
                        invoiceKvRow("Assessment", templateName, false),
                        invoiceKvRow("Due Date", dueDate, true))
                + paragraph("Please complete this assessment before the due date.")
                + primaryButton("Complete Assessment", portalUrl);
    }

    public static String checklistAssignedEmailBody() {
        return heroIcon(iconBriefcase())
                + heading("Checklist Assigned")
                + paragraph(
                        "The checklist <strong>{{checklistName}}</strong> has been assigned for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Checklist", "{{checklistName}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String formAssignedEmailBody() {
        return heroIcon(iconBriefcase())
                + heading("Form Assigned")
                + paragraph(
                        "The form <strong>{{templateName}}</strong> has been assigned for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Form", "{{templateName}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String formCompletedEmailBody() {
        return heroIcon(iconCheckCircle())
                + heading("Form Completed")
                + paragraph(
                        "The form <strong>{{templateName}}</strong> has been completed for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Form", "{{templateName}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String checklistCompletedEmailBody() {
        return heroIcon(iconCheckCircle())
                + heading("Checklist Completed")
                + paragraph(
                        "The checklist <strong>{{checklistName}}</strong> has been completed for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Checklist", "{{checklistName}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String checklistItemCompletedEmailBody() {
        return headingCentered("Checklist Item Completed")
                + paragraphCentered(
                        "Checklist item <strong>{{itemTitle}}</strong> has been marked complete for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Checklist Template", "{{checklistName}}", false),
                        invoiceKvRow("Item", "{{itemTitle}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String taskAssignedEmailBody() {
        return heroIcon(iconBriefcase())
                + heading("Task Assigned")
                + paragraph(
                        "Task <strong>{{title}}</strong> has been assigned for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Task", "{{title}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned To", "{{assignedToName}}", false),
                        invoiceKvRow("Priority", "{{priority}}", false),
                        invoiceKvRow("Due", "{{dueDateFormatted}}", false),
                        invoiceKvRow("Client Therapist", "{{therapistName}}", true));
    }

    public static String taskOverdueEmailBody() {
        return heroIcon(iconAlertCircle())
                + heading("Task Overdue")
                + paragraph(
                        "Task <strong>{{title}}</strong> for client <strong>{{clientMrn}}</strong> is overdue.")
                + kvTable(
                        invoiceKvRow("Task", "{{title}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned To", "{{assignedToName}}", false),
                        invoiceKvRow("Priority", "{{priority}}", false),
                        invoiceKvRow("Due", "{{dueDateFormatted}}", false),
                        invoiceKvRow("Client Therapist", "{{therapistName}}", true));
    }

    public static String taskCommentEmailBody() {
        return heroIcon(iconBell())
                + heading("Task Comment Added")
                + paragraph(
                        "<strong>{{authorName}}</strong> commented on task <strong>{{title}}</strong> "
                                + "for client <strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Task", "{{title}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Author", "{{authorName}}", false),
                        invoiceKvRow("Comment", "{{commentText}}", false),
                        invoiceKvRow("Assigned To", "{{assignedToName}}", true));
    }

    public static String documentUploadedEmailBody() {
        return heroIcon(iconBriefcase())
                + heading("Document Uploaded")
                + paragraph(
                        "A document was uploaded for client <strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("File", "{{fileName}}", false),
                        invoiceKvRow("Type", "{{documentType}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Uploaded By", "{{uploadedByName}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String clientAssignedEmailBody() {
        return heroIcon(iconUser())
                + heading("Client Assigned")
                + paragraph(
                        "Client <strong>{{clientMrn}}</strong> has been assigned to a therapist.")
                + kvTable(
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", false),
                        invoiceKvRow("Status", "{{status}}", false),
                        invoiceKvRow("Stage", "{{stage}}", true));
    }

    public static String assessmentAssignedStaffEmailBody() {
        return heroIcon(iconBriefcase())
                + heading("Assessment Assigned")
                + paragraph(
                        "Assessment <strong>{{templateName}}</strong> has been assigned for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Assessment", "{{templateName}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", false),
                        invoiceKvRow("Assigned By", "{{assignedByName}}", false),
                        invoiceKvRow("Due", "{{dueDateFormatted}}", true));
    }

    public static String assessmentCompletedStaffEmailBody() {
        return heroIcon(iconCheckCircle())
                + heading("Assessment Completed")
                + paragraph(
                        "Assessment <strong>{{templateName}}</strong> has been completed for client "
                                + "<strong>{{clientMrn}}</strong>.")
                + kvTable(
                        invoiceKvRow("Assessment", "{{templateName}}", false),
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Score", "{{totalScore}}", false),
                        invoiceKvRow("Assigned Therapist", "{{therapistName}}", true));
    }

    public static String sessionNoteEmailBody(String headingText, String introHtml) {
        return heroIcon(iconBriefcase())
                + heading(headingText)
                + paragraph(introHtml)
                + kvTable(
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Therapist", "{{therapistName}}", false),
                        invoiceKvRow("Session Date", "{{sessionDateFormatted}}", true));
    }

    /**
     * New organisation admin onboarding email — includes organisation name and first-login credentials.
     */
    public static String organisationOnboardingEmailBody(
            String adminName,
            String organisationName,
            String username,
            String temporaryPassword,
            String loginUrl) {
        String org = hasText(organisationName) ? organisationName : "your organisation";
        return heroIcon(iconShield())
                + heading("Welcome to SmartHub")
                + paragraph("Hi " + escape(adminName) + ",")
                + paragraph("Your organisation <strong>" + escape(org)
                        + "</strong> has been successfully onboarded onto SmartHub.")
                + paragraph(
                        "Use the credentials below to sign in as the organisation administrator and complete setup.")
                + kvTable(
                        invoiceKvRow("Organisation", escape(org), false),
                        invoiceKvRow("Admin email", escape(username), true))
                + credentialBlock("Username", username, "Temporary Password", temporaryPassword)
                + securityNote(
                        "You will be prompted to change your password immediately after your first login. "
                                + "Do not share these credentials.")
                + primaryButton("Login to SmartHub", loginUrl)
                + linkFallback(escape(loginUrl))
                + mutedNote("If you did not expect this email, contact SmartHub support right away.");
    }

    public static String invoiceSimpleEmailBody(String greetingName, String invoiceHtml) {
        return heroIcon(iconReceipt())
                + headingCentered("Your Invoice")
                + paragraphCentered("Hi " + greetingName + ",")
                + paragraphCentered("Please find your invoice details below.")
                + (invoiceHtml != null ? invoiceHtml : "")
                + paragraphCentered("If you have any questions, please contact your therapist.");
    }

    /**
     * Email-safe invoice document (tables + inline styles). Used by send-invoice email only —
     * not the printable PDF HTML.
     */
    public static String invoiceEmailDocument(
            String invoiceNumber,
            String billingDate,
            String serviceDate,
            String practiceName,
            String practiceAddress,
            String practicePhone,
            String practiceEmail,
            String practiceWebsite,
            String clientName,
            String clientPhone,
            String clientEmail,
            String insuranceProvider,
            String insurancePolicy,
            String insuranceGroup,
            String serviceLabel,
            String cptCode,
            String serviceAmountFormatted,
            String totalsRowsHtml,
            String therapistName,
            String licenseName,
            String licenseNumber) {

        String font = "font-family:Inter,Helvetica,Arial,sans-serif;";
        StringBuilder practiceLines = new StringBuilder();
        if (hasText(practiceAddress)) {
            practiceLines.append("<p style=\"margin:0 0 4px 0;font-size:13px;line-height:18px;color:")
                    .append(MUTED).append(";\">").append(practiceAddress).append("</p>");
        }
        appendLabeledLine(practiceLines, "Phone", practicePhone);
        appendLabeledLine(practiceLines, "Email", practiceEmail);
        appendLabeledLine(practiceLines, "Website", practiceWebsite);

        StringBuilder billTo = new StringBuilder();
        billTo.append("<p style=\"margin:0 0 4px 0;font-size:14px;line-height:20px;color:").append(TEXT)
                .append(";font-weight:600;\">").append(nullToEmpty(clientName)).append("</p>");
        if (hasText(clientPhone)) {
            billTo.append("<p style=\"margin:0 0 2px 0;font-size:13px;line-height:18px;color:").append(MUTED)
                    .append(";\">").append(clientPhone).append("</p>");
        }
        if (hasText(clientEmail)) {
            billTo.append("<p style=\"margin:0;font-size:13px;line-height:18px;color:").append(MUTED)
                    .append(";\">").append(clientEmail).append("</p>");
        }

        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"width:100%;max-width:100%;background-color:#ffffff;border:1px solid " + CARD_BORDER
                + ";border-radius:8px;overflow:hidden;margin:8px 0 24px 0;\">"
                // Header: INVOICE + practice
                + "<tr><td style=\"padding:20px 20px 16px 20px;" + font + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + "<tr>"
                + "<td valign=\"top\" style=\"width:50%;padding-right:12px;\">"
                + "<div style=\"margin:0 0 10px 0;font-size:20px;font-weight:700;letter-spacing:0.04em;"
                + "text-transform:uppercase;color:" + PRIMARY + ";" + font + "\">Invoice</div>"
                + metaLine("Invoice #", nullToEmpty(invoiceNumber))
                + metaLine("Date", nullToEmpty(billingDate))
                + metaLine("Service Date", nullToEmpty(serviceDate))
                + "</td>"
                + "<td valign=\"top\" style=\"width:50%;text-align:right;\">"
                + "<div style=\"margin:0 0 8px 0;font-size:15px;font-weight:700;color:" + TEXT + ";" + font + "\">"
                + nullToEmpty(practiceName) + "</div>"
                + practiceLines
                + "</td>"
                + "</tr></table>"
                + "</td></tr>"
                // Bill to / insurance
                + "<tr><td style=\"padding:0 20px 16px 20px;" + font + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + "<tr>"
                + "<td valign=\"top\" style=\"width:50%;padding:14px 12px 14px 0;border-top:1px solid " + CARD_BORDER + ";\">"
                + sectionLabel("Bill To")
                + billTo
                + "</td>"
                + "<td valign=\"top\" style=\"width:50%;padding:14px 0 14px 12px;border-top:1px solid " + CARD_BORDER + ";\">"
                + sectionLabel("Insurance Info")
                + metaLine("Provider", nullToEmpty(insuranceProvider))
                + metaLine("Policy", nullToEmpty(insurancePolicy))
                + metaLine("Group", nullToEmpty(insuranceGroup))
                + "</td>"
                + "</tr></table>"
                + "</td></tr>"
                // Line items
                + "<tr><td style=\"padding:0 20px 8px 20px;" + font + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"width:100%;border-collapse:collapse;\">"
                + "<tr style=\"background-color:" + CANVAS + ";\">"
                + thCell("Service", false)
                + thCell("CPT Code", false)
                + thCell("Date", false)
                + thCell("Amount", true)
                + "</tr>"
                + "<tr>"
                + tdCell(nullToEmpty(serviceLabel), false)
                + tdCell(nullToEmpty(cptCode), false)
                + tdCell(nullToEmpty(serviceDate), false)
                + tdCell("$" + nullToEmpty(serviceAmountFormatted), true)
                + "</tr>"
                + "</table>"
                + "</td></tr>"
                // Totals
                + "<tr><td style=\"padding:8px 20px 16px 20px;" + font + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + "<tr><td style=\"width:35%;\">&nbsp;</td><td style=\"width:65%;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + (totalsRowsHtml != null ? totalsRowsHtml : "")
                + "</table>"
                + "</td></tr></table>"
                + "</td></tr>"
                // Provider box
                + "<tr><td style=\"padding:0 20px 20px 20px;" + font + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"width:100%;background-color:" + CANVAS + ";border:1px solid " + CARD_BORDER
                + ";border-radius:8px;\">"
                + "<tr><td style=\"padding:16px 16px 14px 16px;\">"
                + "<div style=\"margin:0 0 10px 0;font-size:13px;font-weight:700;color:" + TEXT + ";" + font
                + "\">Provider Information for Insurance Reimbursement</div>"
                + metaLine("Provider Name", nullToEmpty(therapistName))
                + metaLine("License Name", nullToEmpty(licenseName))
                + metaLine("License Number", nullToEmpty(licenseNumber))
                + "</td></tr></table>"
                + "</td></tr>"
                + "</table>";
    }

    public static String invoiceEmailTotalRow(String label, String value, boolean emphasize) {
        String labelStyle = emphasize
                ? "padding:10px 0 4px 0;font-size:15px;font-weight:700;color:" + TEXT + ";border-top:2px solid "
                        + PRIMARY + ";"
                : "padding:4px 0;font-size:13px;color:" + MUTED + ";";
        String valueStyle = emphasize
                ? "padding:10px 0 4px 0;font-size:15px;font-weight:700;color:" + TEXT
                        + ";text-align:right;border-top:2px solid " + PRIMARY + ";"
                : "padding:4px 0;font-size:13px;color:" + TEXT + ";text-align:right;font-weight:600;";
        return "<tr>"
                + "<td style=\"" + labelStyle + "\">" + label + "</td>"
                + "<td style=\"" + valueStyle + "\">" + value + "</td>"
                + "</tr>";
    }

    public static String invoiceEmailStatusRow(String statusLabel, String statusColor) {
        String color = hasText(statusColor) ? statusColor : PRIMARY;
        return "<tr>"
                + "<td style=\"padding:8px 0 0 0;font-size:13px;color:" + MUTED + ";\">Status</td>"
                + "<td style=\"padding:8px 0 0 0;font-size:13px;font-weight:700;text-align:right;color:"
                + color + ";\">" + nullToEmpty(statusLabel) + "</td>"
                + "</tr>";
    }

    private static void appendLabeledLine(StringBuilder sb, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        sb.append("<p style=\"margin:0 0 2px 0;font-size:13px;line-height:18px;color:").append(MUTED)
                .append(";\">").append(label).append(": ").append(value).append("</p>");
    }

    private static String sectionLabel(String text) {
        return "<div style=\"margin:0 0 8px 0;font-size:11px;font-weight:700;letter-spacing:0.06em;"
                + "text-transform:uppercase;color:" + PRIMARY + ";\">" + text + "</div>";
    }

    private static String metaLine(String label, String value) {
        return "<p style=\"margin:0 0 4px 0;font-size:13px;line-height:18px;color:" + MUTED + ";\">"
                + "<span style=\"color:" + MUTED + ";\">" + label + ": </span>"
                + "<span style=\"color:" + TEXT + ";font-weight:600;\">" + value + "</span></p>";
    }

    private static String thCell(String text, boolean right) {
        return "<th style=\"padding:10px 8px;font-size:11px;font-weight:700;letter-spacing:0.04em;"
                + "text-transform:uppercase;color:" + MUTED + ";text-align:"
                + (right ? "right" : "left") + ";border-bottom:1px solid " + CARD_BORDER + ";\">"
                + text + "</th>";
    }

    private static String tdCell(String text, boolean right) {
        return "<td style=\"padding:12px 8px;font-size:13px;line-height:18px;color:" + TEXT + ";text-align:"
                + (right ? "right" : "left") + ";border-bottom:1px solid " + CARD_BORDER + ";\">"
                + text + "</td>";
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    public static String sessionCancelledEmailBody() {
        return heroIcon(iconXCircle())
                + heading("Session Cancelled")
                + paragraph("The session for client <strong>{{clientMrn}}</strong> scheduled for "
                        + "<strong>{{sessionDateFormatted}}</strong> has been cancelled.");
    }

    public static String invoiceReadyEmailBody() {
        return heroIcon(iconReceipt())
                + headingCentered("Your SmartHub Invoice is Ready")
                + paragraphCentered(
                        "A new invoice has been generated for your recent SmartHub services. Please review the details below.")
                + kvTable(
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Service", "{{serviceName}}", false),
                        invoiceKvRow("Session", "{{sessionDateFormatted}}", false),
                        invoiceKvRow("Invoice #", "{{invoiceNumber}}", false),
                        invoiceKvRow("Due Date", "{{dueDateFormatted}}", false),
                        invoiceKvRow("Amount Due", "{{amountDue}}", true))
                + fullWidthPrimaryButtonTemplate("Pay Invoice", "{{paymentUrl}}")
                + fullWidthOutlineButtonTemplate("View PDF", "{{invoiceUrl}}");
    }

    public static String invoiceReminderEmailBody() {
        return heroIcon(iconBell())
                + headingCentered("Invoice Payment Reminder")
                + paragraphCentered(
                        "This is a reminder that a bill for client <strong>{{clientMrn}}</strong> is due soon.")
                + kvTable(
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Service", "{{serviceName}}", false),
                        invoiceKvRow("Session", "{{sessionDateFormatted}}", false),
                        invoiceKvRow("Invoice #", "{{invoiceNumber}}", false),
                        invoiceKvRow("Amount Due", "{{amountDue}}", false),
                        invoiceKvRow("Due Date", "{{dueDateFormatted}}", true))
                + fullWidthPrimaryButtonTemplate("Pay Now", "{{paymentUrl}}")
                + fullWidthOutlineButtonTemplate("View PDF", "{{invoiceUrl}}");
    }

    public static String paymentReceivedEmailBody() {
        return heroIcon(iconCheckCircle())
                + headingCentered("Payment Received")
                + paragraphCentered(
                        "A payment for client <strong>{{clientMrn}}</strong> was successfully received.")
                + kvTable(
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Paid Amount", "{{paidAmount}}", false),
                        invoiceKvRow("Outstanding Balance", "{{amountDue}}", true));
    }

    public static String paymentFailedEmailBody() {
        return heroIcon(iconAlertCircle())
                + headingCentered("Payment Failed")
                + paragraphCentered(
                        "A recent payment attempt for client <strong>{{clientMrn}}</strong> has failed.")
                + kvTable(
                        invoiceKvRow("Client MRN", "{{clientMrn}}", false),
                        invoiceKvRow("Amount Due", "{{amountDue}}", true))
                + fullWidthPrimaryButtonTemplate("Retry Payment", "{{paymentUrl}}");
    }

    public static String clientWelcomeEmailBody(String greetingName, String therapistSectionHtml) {
        return heroIcon(iconMail())
                + heading("Welcome to SmartHub")
                + paragraph("Hi " + greetingName + ",")
                + paragraph(
                        "We're excited to welcome you to SmartHub. Your client profile has been created and you're now part of our care system.")
                + (therapistSectionHtml != null ? therapistSectionHtml : "")
                + paragraph("What's next:")
                + "<ul style=\"margin:0 0 16px 0;padding-left:20px;line-height:1.8;color:" + MUTED + ";\">"
                + "<li>Your therapist will reach out to schedule your first session</li>"
                + "<li>If portal access is enabled, you'll receive a separate activation email</li>"
                + "<li>You can access your information and appointments through the client portal</li>"
                + "</ul>"
                + mutedNote("If you have any questions, please contact your therapist or our support team.");
    }

    public static String staffWelcomeWithCredentialsEmailBody(
            String greetingName, String username, String temporaryPassword, String loginUrl) {
        return heroIcon(iconShield())
                + heading("Welcome to your SmartHub workspace")
                + paragraph("Hi " + greetingName + ",")
                + paragraph(
                        "Your SmartHub account has been created. Use the credentials below to log in for the first time.")
                + credentialBlock("Username", username, "Temporary Password", temporaryPassword)
                + securityNote("You will be prompted to change your password immediately after your first login.")
                + primaryButton("Login to SmartHub", loginUrl)
                + linkFallback(escape(loginUrl));
    }

    public static String staffWelcomeNoPasswordEmailBody(String greetingName, String username, String loginUrl) {
        return heroIcon(iconShield())
                + heading("Welcome to SmartHub")
                + paragraph("Hi " + greetingName + ",")
                + paragraph("Your SmartHub account has been created.")
                + detailCard(detailRow("Username", escape(username)))
                + securityNote(
                        "For security, passwords are never sent by email. Use your assigned password or the forgot-password flow if needed.")
                + primaryButton("Login to SmartHub", loginUrl)
                + linkFallback(escape(loginUrl));
    }

    public static String portalActivationEmailBody(String greetingName, String activationUrl) {
        return heroIcon(iconShield())
                + heading("Activate your SmartHub portal")
                + paragraph("Hi " + greetingName + ",")
                + paragraph(
                        "Your therapist has enabled portal access for you. Click the button below to activate your account and set your password.")
                + primaryButton("Activate My Account", activationUrl)
                + linkFallback(escape(activationUrl))
                + mutedNote("If you didn't request portal access, please contact your therapist.");
    }

    public static String passwordResetEmailBody(String greetingName, String resetUrl, boolean staff) {
        return heroIcon(iconLock())
                + heading("Reset your password")
                + paragraph("Hi " + greetingName + ",")
                + paragraph(staff
                        ? "We received a request to reset your SmartHub staff account password. Click the button below to set a new password."
                        : "We received a request to reset your SmartHub Client Portal password. Click the button below to set a new password.")
                + primaryButton("Reset My Password", resetUrl)
                + linkFallback(escape(resetUrl))
                + mutedNote(
                        "If you didn't request a password reset, you can safely ignore this email. Your password will not be changed.");
    }

    public static String appointmentConfirmationEmailBody(
            String greetingName,
            String date,
            String time,
            String duration,
            String sessionType,
            String location) {
        return heroIcon(iconCalendarCheck())
                + heading("Appointment Confirmation")
                + paragraph("Hi " + greetingName + ",")
                + paragraph("Your appointment has been confirmed:")
                + sessionDetailsCardHtml(date, time, null, null, location,
                        hasText(sessionType) ? sessionType + (hasText(duration) ? " (" + duration + " min)" : "") : null)
                + paragraph("You can view and manage your appointments in your client portal.");
    }

    public static String credentialBlock(String usernameLabel, String username, String passwordLabel, String password) {
        return detailCard(
                detailRow(usernameLabel, escape(username))
                        + (password != null
                                ? detailRow(passwordLabel, "<code style=\"background:" + CANVAS
                                        + ";padding:2px 6px;border-radius:3px;font-family:monospace;\">"
                                        + escape(password) + "</code>")
                                : ""));
    }

    public static String securityNote(String text) {
        return "<div style=\"background-color:#F5F3F0;border-left:4px solid " + PRIMARY
                + ";padding:12px 16px;margin:20px 0;border-radius:0 4px 4px 0;\">"
                + "<p style=\"margin:0;font-size:14px;line-height:20px;color:" + TEXT + ";\">"
                + "<strong>Security note:</strong> " + text + "</p></div>";
    }

    public static String otpBlock(String code) {
        return "<div style=\"text-align:center;margin:28px 0;padding:24px;background-color:" + CANVAS
                + ";border:1px solid " + CARD_BORDER + ";border-radius:8px;\">"
                + "<p style=\"margin:0 0 8px 0;font-size:12px;font-weight:600;letter-spacing:0.05em;"
                + "text-transform:uppercase;color:" + MUTED + ";\">Sign-in code</p>"
                + "<p class=\"email-otp\" style=\"margin:0;font-size:32px;font-weight:700;letter-spacing:0.2em;color:"
                + PRIMARY + ";font-family:Inter,Helvetica,Arial,sans-serif;\">"
                + escape(code) + "</p></div>";
    }

    /**
     * Full branded document wrapper used by previews and kept in sync with EmailService shell.
     */
    public static String brandedDocument(String subject, String tenantName, String contentHtml) {
        String safeSubject = escape(subject);
        String safeTenant = escape(tenantName != null ? tenantName : "Acme Clinic");
        return "<!DOCTYPE html>\n<html>\n<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"
                + "<title>" + safeSubject + "</title>\n"
                + clientStyles() + "\n"
                + "</head>\n"
                + "<body style=\"margin:0;padding:0;background-color:" + CANVAS
                + ";font-family:Inter,Helvetica,Arial,sans-serif;line-height:1.5;color:" + TEXT + ";\">\n"
                + "<table role=\"presentation\" class=\"email-outer\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"background-color:" + CANVAS + ";padding:32px 16px;\">\n"
                + "<tr><td align=\"center\">\n"
                + "<table role=\"presentation\" class=\"email-card\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"max-width:600px;width:100%;background-color:#ffffff;border:1px solid " + CARD_BORDER
                + ";border-radius:8px;overflow:hidden;\">\n"
                + "<tr><td class=\"email-header-pad\" align=\"center\" style=\"padding:24px 24px 16px 24px;background-color:#ffffff;\">\n"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" style=\"margin:0 auto;\">\n"
                + "<tr>\n"
                + "<td style=\"vertical-align:middle;padding-right:10px;\">"
                + "<img src=\"" + LOGO_URL + "\" alt=\"" + PRODUCT_NAME
                + "\" width=\"36\" height=\"36\" style=\"display:block;width:36px;height:36px;border:0;\" />"
                + "</td>\n"
                + "<td style=\"vertical-align:middle;\">"
                + "<span class=\"email-brand\" style=\"font-family:Inter,Helvetica,Arial,sans-serif;font-size:20px;font-weight:600;"
                + "letter-spacing:-0.01em;color:" + PRIMARY + ";line-height:36px;\">" + PRODUCT_NAME + "</span>"
                + "</td>\n"
                + "</tr></table>\n"
                + "</td></tr>\n"
                + "<tr><td style=\"height:4px;background-color:" + PRIMARY + ";font-size:0;line-height:0;\">&nbsp;</td></tr>\n"
                + "<tr><td class=\"email-body-pad email-content\" style=\"padding:32px 24px;font-size:15px;color:"
                + MUTED + ";\">\n"
                + contentHtml + "\n"
                + "</td></tr>\n"
                + "<tr><td class=\"email-footer-pad\" style=\"padding:24px;font-size:12px;line-height:18px;color:#9AA0A6;"
                + "text-align:center;background-color:" + CANVAS + ";border-top:1px solid " + CARD_BORDER
                + ";\">\n"
                + "<p style=\"margin:0 0 12px 0;color:#9AA0A6;\">This secure notification was sent via <strong style=\"color:#9AA0A6;\">"
                + PRODUCT_NAME + "</strong> on behalf of <strong style=\"color:#9AA0A6;\">"
                + safeTenant + "</strong>.</p>\n"
                + "<p style=\"margin:0;color:#9AA0A6;\"><strong style=\"color:#9AA0A6;\">STRICT CONFIDENTIALITY &amp; HIPAA NOTICE:</strong> This email and any "
                + "attachments are highly confidential, privileged, and protected by HIPAA audit laws and data "
                + "protection regulations. If you are not the intended recipient, any dissemination, distribution, "
                + "or copying is strictly prohibited. Please delete all copies and notify the sender immediately.</p>\n"
                + "</td></tr>\n"
                + "</table>\n"
                + "</td></tr></table>\n"
                + "</body>\n</html>";
    }

    public static String statusChip(String label, String background, String textColor) {
        return "<span style=\"display:inline-block;padding:4px 12px;border-radius:12px;font-size:12px;"
                + "font-weight:600;background-color:" + background + ";color:" + textColor + ";\">"
                + label + "</span>";
    }

    public static String linkFallback(String url) {
        return mutedNote("Or copy and paste this link into your browser:<br>"
                + "<span style=\"word-break:break-all;color:" + PRIMARY + ";\">" + url + "</span>");
    }

    /** Generic event email: heading + intro + flush KV table + optional CTA. */
    public static String standardEventEmailBody(
            String headingText,
            String introHtml,
            String kvTableHtml,
            String buttonLabel,
            String buttonHrefPlaceholder) {
        StringBuilder sb = new StringBuilder();
        sb.append(heading(headingText));
        sb.append(paragraph(introHtml));
        if (hasText(kvTableHtml)) {
            sb.append(kvTableHtml);
        }
        if (hasText(buttonLabel) && hasText(buttonHrefPlaceholder)) {
            sb.append(primaryButtonTemplate(buttonLabel, buttonHrefPlaceholder));
        }
        return sb.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
