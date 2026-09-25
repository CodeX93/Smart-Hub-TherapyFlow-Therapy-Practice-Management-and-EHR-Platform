package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generates docs/email-templates-preview.html with every SmartHub email body,
 * wrapped in the branded mobile-responsive shell. Open in a browser and use
 * Desktop / Mobile toggles to verify layout.
 */
@DisplayName("Email template preview generator")
class EmailTemplatePreviewGeneratorTest {

    private static final Path OUTPUT = Path.of("docs/email-templates-preview.html");

    @Test
    @DisplayName("Writes all-template mobile/desktop preview HTML")
    void writesAllTemplatesPreview() throws Exception {
        String sessionDetails = EmailHtmlComponents.sessionDetailsCardHtml(
                "Tuesday, Oct 24, 2024",
                "10:00 AM - 11:00 AM ET",
                "MRN-1042",
                "Jordan Lee",
                "SmartHub Video Link",
                "Individual Therapy");

        Map<String, String> bodies = new LinkedHashMap<>();
        bodies.put("Session Scheduled", fill(EmailHtmlComponents.sessionScheduledEmailBody(), sessionDetails));
        bodies.put("Session Reminder", fill(EmailHtmlComponents.sessionReminderEmailBody(), sessionDetails));
        bodies.put("Session Confirmation", fill(EmailHtmlComponents.sessionConfirmationEmailBody(), sessionDetails));
        bodies.put("Session Rescheduled", fill(EmailHtmlComponents.sessionRescheduledEmailBody(), sessionDetails));
        bodies.put("Session Cancelled", EmailHtmlComponents.sessionCancelledEmailBody()
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{sessionDateFormatted}}", "Tuesday, Oct 24, 2024 10:00 AM ET"));
        bodies.put("Recurring Series", EmailHtmlComponents.recurringSeriesConfirmationEmailBody(
                "Alex", "Jordan Lee", "Individual Therapy", 4,
                "<li>Tue, Oct 24, 2024 at 10:00 AM ET</li>"
                        + "<li>Tue, Oct 31, 2024 at 10:00 AM ET</li>"
                        + "<li>Tue, Nov 7, 2024 at 10:00 AM ET</li>"
                        + "<li>Tue, Nov 14, 2024 at 10:00 AM ET</li>"));
        bodies.put("Invoice Ready", fillInvoice(EmailHtmlComponents.invoiceReadyEmailBody()));
        bodies.put("Invoice Reminder", fillInvoice(EmailHtmlComponents.invoiceReminderEmailBody()));
        bodies.put("Payment Received", fillInvoice(EmailHtmlComponents.paymentReceivedEmailBody())
                .replace("{{paidAmount}}", "$150.00"));
        bodies.put("Payment Failed", fillInvoice(EmailHtmlComponents.paymentFailedEmailBody()));
        bodies.put("Client Welcome", EmailHtmlComponents.clientWelcomeEmailBody("Alex",
                EmailHtmlComponents.paragraph(
                        "You have been assigned to <strong>Jordan Lee</strong>, who will be your primary therapist.")));
        bodies.put("Portal Activation",
                EmailHtmlComponents.portalActivationEmailBody("Alex", "https://app.therapyflow.pro/activate/demo"));
        bodies.put("Staff Welcome", EmailHtmlComponents.staffWelcomeWithCredentialsEmailBody(
                "Sam", "sam@clinic.example", "TempPass!23", "https://app.therapyflow.pro/login"));
        bodies.put("Password Reset",
                EmailHtmlComponents.passwordResetEmailBody("Alex", "https://app.therapyflow.pro/reset/demo", false));
        bodies.put("OTP Sign-in", EmailHtmlComponents.otpEmailBody("482913"));
        bodies.put("Assessment Assigned", EmailHtmlComponents.assessmentAssignedEmailBody(
                "Alex", "MRN-1042", "PHQ-9", "Oct 31, 2024", "https://app.therapyflow.pro/portal/assessments"));
        bodies.put("Assessment Reminder", EmailHtmlComponents.assessmentReminderEmailBody(
                "Alex", "MRN-1042", "PHQ-9", "Oct 31, 2024", "https://app.therapyflow.pro/portal/assessments"));
        bodies.put("Assessment Completed",
                EmailHtmlComponents.assessmentCompletedEmailBody("Alex", "MRN-1042", "PHQ-9", "12"));
        bodies.put("Checklist Assigned", EmailHtmlComponents.checklistAssignedEmailBody()
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{checklistName}}", "Intake Packet")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Checklist Completed", EmailHtmlComponents.checklistCompletedEmailBody()
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{checklistName}}", "Intake Packet")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Checklist Item Completed", EmailHtmlComponents.checklistItemCompletedEmailBody()
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{checklistName}}", "Intake Packet")
                .replace("{{itemTitle}}", "Sign consent form")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Form Assigned", EmailHtmlComponents.formAssignedEmailBody()
                .replace("{{templateName}}", "Informed Consent")
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Form Completed", EmailHtmlComponents.formCompletedEmailBody()
                .replace("{{templateName}}", "Informed Consent")
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Task Assigned", EmailHtmlComponents.taskAssignedEmailBody()
                .replace("{{title}}", "Complete intake docs")
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{assignedToName}}", "Sam Rivera")
                .replace("{{priority}}", "HIGH")
                .replace("{{dueDateFormatted}}", "Aug 10, 2026 5:00 PM UTC")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Document Uploaded", EmailHtmlComponents.documentUploadedEmailBody()
                .replace("{{fileName}}", "insurance-card.pdf")
                .replace("{{documentType}}", "INSURANCE")
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{uploadedByName}}", "Sam Rivera")
                .replace("{{therapistName}}", "Jordan Lee"));
        bodies.put("Client Assigned", EmailHtmlComponents.clientAssignedEmailBody()
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{therapistName}}", "Jordan Lee")
                .replace("{{status}}", "ACTIVE")
                .replace("{{stage}}", "INTAKE"));
        bodies.put("Organisation Onboarding", EmailHtmlComponents.organisationOnboardingEmailBody(
                "Sam Rivera", "Acme Clinic", "sam@acme.clinic", "TempPass!23",
                "https://app.therapyflow.pro/login"));
        bodies.put("Appointment Confirmation", EmailHtmlComponents.appointmentConfirmationEmailBody(
                "Alex", "Tuesday, Oct 24, 2024", "10:00 AM ET", "50", "Individual Therapy", "Online"));
        bodies.put("Org User Created", EmailHtmlComponents.standardEventEmailBody(
                "Organisation User Created",
                "A new user account has been created.",
                EmailHtmlComponents.kvTable(
                        EmailHtmlComponents.invoiceKvRow("Name", "Sam Rivera", false),
                        EmailHtmlComponents.invoiceKvRow("Email", "sam@clinic.example", false),
                        EmailHtmlComponents.invoiceKvRow("Created by", "Admin", true)),
                null, null));

        StringBuilder page = new StringBuilder();
        page.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>SmartHub Email Templates Preview</title>
                  <style>
                    :root { color-scheme: light; }
                    * { box-sizing: border-box; }
                    body { margin: 0; font-family: Inter, Helvetica, Arial, sans-serif; background: #E8ECF0; color: #1B1C1C; }
                    .toolbar {
                      position: sticky; top: 0; z-index: 20; display: flex; flex-wrap: wrap; gap: 12px; align-items: center;
                      justify-content: space-between; padding: 12px 16px; background: #3C4D58; color: #fff;
                    }
                    .toolbar h1 { margin: 0; font-size: 15px; font-weight: 600; }
                    .toolbar .controls { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
                    .toolbar button, .toolbar select {
                      border: 1px solid rgba(255,255,255,.35); background: transparent; color: #fff;
                      border-radius: 6px; padding: 8px 12px; font-size: 13px; cursor: pointer;
                    }
                    .toolbar button.active { background: #fff; color: #3C4D58; border-color: #fff; font-weight: 600; }
                    .toolbar select { background: #2f3d46; }
                    .layout { display: grid; grid-template-columns: 240px 1fr; min-height: calc(100vh - 56px); }
                    @media (max-width: 860px) { .layout { grid-template-columns: 1fr; } .nav { max-height: 180px; overflow: auto; } }
                    .nav { background: #F8FAFB; border-right: 1px solid #E1E4E8; padding: 12px; }
                    .nav a {
                      display: block; padding: 8px 10px; border-radius: 6px; color: #3C4D58; text-decoration: none;
                      font-size: 13px; margin-bottom: 2px;
                    }
                    .nav a:hover, .nav a.active { background: #E1E4E8; }
                    .stage-wrap { padding: 24px 16px 48px; display: flex; justify-content: center; }
                    .stage {
                      width: 100%; max-width: 680px; transition: max-width .2s ease;
                      background: transparent;
                    }
                    body.mobile-view .stage { max-width: 390px; }
                    body.mobile-view .stage-frame {
                      border: 10px solid #1B1C1C; border-radius: 28px; overflow: hidden;
                      box-shadow: 0 8px 24px rgba(0,0,0,.18);
                    }
                    .stage-frame iframe {
                      display: block; width: 100%; border: 0; background: #F8FAFB; min-height: 720px;
                    }
                    .hint { margin: 0 0 12px; font-size: 12px; color: #43474B; text-align: center; }
                  </style>
                </head>
                <body>
                  <div class="toolbar">
                    <h1>SmartHub email templates</h1>
                    <div class="controls">
                      <button type="button" id="btn-desktop" class="active">Desktop</button>
                      <button type="button" id="btn-mobile">Mobile (390px)</button>
                      <label for="template-select" style="font-size:12px;opacity:.85;">Template</label>
                      <select id="template-select"></select>
                    </div>
                  </div>
                  <div class="layout">
                    <nav class="nav" id="nav"></nav>
                    <main class="stage-wrap">
                      <div class="stage">
                        <p class="hint" id="hint">Desktop preview · fluid to 600px card</p>
                        <div class="stage-frame">
                          <iframe id="preview" title="Email preview"></iframe>
                        </div>
                      </div>
                    </main>
                  </div>
                  <script>
                    const templates = {
                """);

        boolean first = true;
        for (Map.Entry<String, String> entry : bodies.entrySet()) {
            String doc = EmailHtmlComponents.brandedDocument(entry.getKey(), "Acme Clinic", entry.getValue());
            // Use local logo when previewing from docs/
            doc = doc.replace(EmailHtmlComponents.LOGO_URL, "smarthub-brain-icon-512.png");
            if (!first) {
                page.append(",\n");
            }
            first = false;
            page.append("                      ")
                    .append(jsonKey(entry.getKey()))
                    .append(": ")
                    .append(jsonString(doc));
        }

        page.append("""

                    };
                    const names = Object.keys(templates);
                    const nav = document.getElementById('nav');
                    const select = document.getElementById('template-select');
                    const frame = document.getElementById('preview');
                    const hint = document.getElementById('hint');
                    let current = names[0];

                    function show(name) {
                      current = name;
                      frame.srcdoc = templates[name];
                      [...nav.querySelectorAll('a')].forEach(a => a.classList.toggle('active', a.dataset.name === name));
                      select.value = name;
                    }

                    names.forEach(name => {
                      const a = document.createElement('a');
                      a.href = '#';
                      a.dataset.name = name;
                      a.textContent = name;
                      a.addEventListener('click', (e) => { e.preventDefault(); show(name); });
                      nav.appendChild(a);
                      const opt = document.createElement('option');
                      opt.value = name;
                      opt.textContent = name;
                      select.appendChild(opt);
                    });

                    select.addEventListener('change', () => show(select.value));
                    document.getElementById('btn-desktop').addEventListener('click', () => {
                      document.body.classList.remove('mobile-view');
                      document.getElementById('btn-desktop').classList.add('active');
                      document.getElementById('btn-mobile').classList.remove('active');
                      hint.textContent = 'Desktop preview · fluid to 600px card';
                      show(current);
                    });
                    document.getElementById('btn-mobile').addEventListener('click', () => {
                      document.body.classList.add('mobile-view');
                      document.getElementById('btn-mobile').classList.add('active');
                      document.getElementById('btn-desktop').classList.remove('active');
                      hint.textContent = 'Mobile preview · 390px viewport (iPhone-class)';
                      show(current);
                    });

                    show(current);
                  </script>
                </body>
                </html>
                """);

        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, page.toString(), StandardCharsets.UTF_8);

        String written = Files.readString(OUTPUT);
        assertThat(written).contains("Session Reminder");
        assertThat(written).contains("Invoice Reminder");
        assertThat(written).contains("Client Welcome");
        assertThat(written).contains("email-btn-link");
        assertThat(written).contains("@media only screen and (max-width:620px)");
        assertThat(written).doesNotContain("📅");
        assertThat(written).doesNotContain("🧾");
        assertThat(OUTPUT.toFile().length()).isGreaterThan(10_000);
    }

    private static String fill(String template, String sessionDetails) {
        return template
                .replace("{{sessionDetailsHtml}}", sessionDetails)
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{sessionUrl}}", "https://app.therapyflow.pro/therapist/scheduling")
                .replace("{{calendarUrl}}",
                        "https://calendar.google.com/calendar/render?action=TEMPLATE"
                                + "&text=Individual%20Psychotherapy%2045%20min%20(CL-2026-0001)"
                                + "&dates=20260804T110000Z/20260804T114500Z"
                                + "&details=SmartHub%20session"
                                + "&location=Therapy%20Room");
    }

    private static String fillInvoice(String template) {
        return template
                .replace("{{clientMrn}}", "MRN-1042")
                .replace("{{serviceName}}", "Individual Therapy")
                .replace("{{sessionDateFormatted}}", "Oct 24, 2024 10:00 AM UTC")
                .replace("{{invoiceNumber}}", "INV-MRN-1042-881")
                .replace("{{dueDateFormatted}}", "Oct 31, 2024")
                .replace("{{amountDue}}", "$150.00")
                .replace("{{paymentUrl}}", "https://app.therapyflow.pro/pay/1")
                .replace("{{invoiceUrl}}", "https://app.therapyflow.pro/invoices/1.pdf");
    }

    private static String jsonKey(String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static String jsonString(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("</", "<\\/");
        return "\"" + escaped + "\"";
    }
}
