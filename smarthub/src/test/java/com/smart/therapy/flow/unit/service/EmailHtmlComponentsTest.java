package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailHtmlComponents Unit Tests")
class EmailHtmlComponentsTest {

    @Test
    @DisplayName("Primary button uses Clinical Clarity brand color")
    void primaryButtonUsesBrandColor() {
        String html = EmailHtmlComponents.primaryButton("Login to SmartHub", "https://example.com");
        assertThat(html).contains(EmailHtmlComponents.PRIMARY);
        assertThat(html).contains("Login to SmartHub");
        assertThat(html).contains("https://example.com");
        assertThat(html).doesNotContain("#0ea5e9");
    }

    @Test
    @DisplayName("Credential and OTP blocks render without client names")
    void credentialAndOtpBlocks() {
        String credentials = EmailHtmlComponents.credentialBlock(
                "Username", "admin@example.com", "Temporary Password", "Temp123!");
        assertThat(credentials).contains("admin@example.com");
        assertThat(credentials).contains("Temp123!");

        String otp = EmailHtmlComponents.otpBlock("482913");
        assertThat(otp).contains("482913");
        assertThat(otp).contains("Sign-in code");
    }

    @Test
    @DisplayName("Catalog CTA template keeps placeholders and brand color")
    void catalogCtaKeepsPlaceholders() {
        String button = EmailHtmlComponents.primaryButtonTemplate("View & Pay Bill", "{{paymentUrl}}");
        assertThat(button).contains("{{paymentUrl}}");
        assertThat(button).contains(EmailHtmlComponents.PRIMARY);
        assertThat(button).doesNotContain("#0ea5e9");
    }

    @Test
    @DisplayName("Session scheduled body matches Stitch card layout without hero badge")
    void sessionScheduledBodyMatchesStitch() {
        String html = EmailHtmlComponents.sessionScheduledEmailBody();
        assertThat(html).contains("New Session Scheduled");
        assertThat(html).contains("{{sessionDetailsHtml}}");
        assertThat(html).contains("View Session Details");
        assertThat(html).contains("{{sessionUrl}}");
        assertThat(html).contains("Add to Calendar");
        assertThat(html).doesNotContain("width:48px;height:48px");
        assertThat(html).doesNotContain("<svg");
        assertThat(html).doesNotContain("📅");
    }

    @Test
    @DisplayName("Session and invoice reminder bodies have no centered hero badges")
    void reminderBodiesHaveNoHeroBadges() {
        String sessionReminder = EmailHtmlComponents.sessionReminderEmailBody();
        assertThat(sessionReminder).contains("Session Reminder");
        assertThat(sessionReminder).doesNotContain("bell.png");
        assertThat(sessionReminder).doesNotContain("<svg");
        assertThat(sessionReminder).doesNotContain("📅");

        String invoiceReminder = EmailHtmlComponents.invoiceReminderEmailBody();
        assertThat(invoiceReminder).contains("Invoice Payment Reminder");
        assertThat(invoiceReminder).doesNotContain("bell.png");
        assertThat(invoiceReminder).doesNotContain("<svg");
        assertThat(invoiceReminder).doesNotContain("🧾");
    }

    @Test
    @DisplayName("Send-invoice email body is centered and embeds email-safe card")
    void invoiceSimpleEmailBodyIsCentered() {
        String card = EmailHtmlComponents.invoiceEmailDocument(
                "INV-1", "Aug 3, 2026", "Aug 4, 2026",
                "MindCare", "123 Main", "555-0100", "a@b.com", "example.com",
                "Client Name", "555-0101", "c@d.com",
                "Provider", "POL", "GRP",
                "Therapy", "90837", "200.00",
                EmailHtmlComponents.invoiceEmailTotalRow("Total Due", "$200.00", true)
                        + EmailHtmlComponents.invoiceEmailStatusRow("PENDING", "#3C4D58"),
                "Therapist", "RP", "12345");
        String html = EmailHtmlComponents.invoiceSimpleEmailBody("Fahad", card);
        assertThat(html).contains("Your Invoice");
        assertThat(html).contains("text-align:center");
        assertThat(html).contains("INV-1");
        assertThat(html).contains("Total Due");
        assertThat(html).contains("Status");
        assertThat(html).contains("Phone: 555-0100");
        assertThat(html).doesNotContain("<!DOCTYPE");
        assertThat(html).doesNotContain("Times New Roman");
        assertThat(html).doesNotContain("display: flex");
    }

    @Test
    @DisplayName("Invoice body includes MRN, service, session, and dual CTAs")
    void invoiceBodyIncludesMrnServiceSession() {
        String html = EmailHtmlComponents.invoiceReadyEmailBody();
        assertThat(html).contains("Your SmartHub Invoice is Ready");
        assertThat(html).contains("{{clientMrn}}");
        assertThat(html).contains("{{serviceName}}");
        assertThat(html).contains("{{sessionDateFormatted}}");
        assertThat(html).contains("{{invoiceNumber}}");
        assertThat(html).contains("Pay Invoice");
        assertThat(html).contains("View PDF");
        assertThat(html).contains("{{paymentUrl}}");
        assertThat(html).contains("{{invoiceUrl}}");
        assertThat(html).doesNotContain("receipt.png");
        assertThat(html).doesNotContain("<svg");
        assertThat(html).doesNotContain("#0ea5e9");
        assertThat(html).doesNotContain("🧾");
    }

    @Test
    @DisplayName("Client welcome has no centered hero badge")
    void clientWelcomeHasNoHeroBadge() {
        String html = EmailHtmlComponents.clientWelcomeEmailBody("Alex", "");
        assertThat(html).contains("Welcome to SmartHub");
        assertThat(html).doesNotContain("mail.png");
        assertThat(html).doesNotContain("<svg");
        assertThat(html).doesNotContain("👋");
    }

    @Test
    @DisplayName("Google Calendar URL uses TEMPLATE action and UTC dates")
    void googleCalendarUrlBuildsTemplateLink() {
        java.time.Instant start = java.time.Instant.parse("2026-08-04T11:00:00Z");
        java.time.Instant end = java.time.Instant.parse("2026-08-04T11:45:00Z");
        String url = EmailHtmlComponents.googleCalendarUrl(
                "Individual Therapy (CL-1)", start, end, "Provider: Jane", "Room A");
        assertThat(url).startsWith("https://calendar.google.com/calendar/render?action=TEMPLATE");
        assertThat(url).contains("dates=20260804T110000Z/20260804T114500Z");
        assertThat(url).contains("text=");
        assertThat(url).contains("location=");
    }

    @Test
    @DisplayName("Hero badge is disabled (no centered square icon boxes)")
    void heroBadgeIsDisabled() {
        String html = EmailHtmlComponents.heroBadge(EmailHtmlComponents.iconCheckCircle());
        assertThat(html).isEmpty();
        assertThat(EmailHtmlComponents.sessionScheduledEmailBody())
                .doesNotContain("width:48px;height:48px")
                .doesNotContain("border-radius:12px");
        assertThat(EmailHtmlComponents.invoiceReadyEmailBody())
                .doesNotContain("receipt.png");
    }

    @Test
    @DisplayName("Checklist item completed has clear copy, no hero icon, checklist template label")
    void checklistItemCompletedIsClearWithoutHeroIcon() {
        String html = EmailHtmlComponents.checklistItemCompletedEmailBody();
        assertThat(html).contains("Checklist Item Completed");
        assertThat(html).contains("Checklist item <strong>{{itemTitle}}</strong> has been marked complete");
        assertThat(html).contains("Checklist Template");
        assertThat(html).doesNotContain("on checklist");
        assertThat(html).doesNotContain("was completed for client");
        assertThat(html).doesNotContain("check-circle.png");
        assertThat(html).contains("{{checklistName}}");
        assertThat(html).contains("{{itemTitle}}");
        assertThat(html).contains("{{clientMrn}}");
    }

    @Test
    @DisplayName("Assessment and checklist bodies include client MRN")
    void assessmentAndChecklistIncludeMrn() {
        String assessment = EmailHtmlComponents.assessmentAssignedEmailBody(
                "Alex", "MRN-1042", "PHQ-9", "Oct 31, 2024", "https://example.com");
        assertThat(assessment).contains("Client MRN");
        assertThat(assessment).contains("MRN-1042");

        String checklist = EmailHtmlComponents.checklistAssignedEmailBody();
        assertThat(checklist).contains("{{clientMrn}}");
        assertThat(checklist).contains("Client MRN");
        assertThat(checklist).contains("{{checklistName}}");
        assertThat(checklist).contains("{{therapistName}}");
        assertThat(checklist).contains("Assigned Therapist");
    }

    @Test
    @DisplayName("Form assigned email includes form, client MRN, and therapist")
    void formAssignedIncludesFormMrnAndTherapist() {
        String html = EmailHtmlComponents.formAssignedEmailBody();
        assertThat(html).contains("Form Assigned");
        assertThat(html).contains("{{templateName}}");
        assertThat(html).contains("{{clientMrn}}");
        assertThat(html).contains("{{therapistName}}");
        assertThat(html).contains("Assigned Therapist");
    }

    @Test
    @DisplayName("Task and document staff emails include concrete detail rows")
    void taskAndDocumentEmailsAreNotGeneric() {
        assertThat(EmailHtmlComponents.taskAssignedEmailBody())
                .contains("{{title}}")
                .contains("{{clientMrn}}")
                .contains("{{assignedToName}}");
        assertThat(EmailHtmlComponents.documentUploadedEmailBody())
                .contains("{{fileName}}")
                .contains("{{clientMrn}}")
                .contains("{{therapistName}}");
        assertThat(EmailHtmlComponents.clientAssignedEmailBody())
                .contains("{{clientMrn}}")
                .contains("{{therapistName}}");
    }

    @Test
    @DisplayName("Organisation onboarding email names the organisation")
    void organisationOnboardingMentionsOrgName() {
        String html = EmailHtmlComponents.organisationOnboardingEmailBody(
                "Sam", "Acme Clinic", "sam@acme.clinic", "Temp123!", "https://example.com/login");
        assertThat(html).contains("Acme Clinic");
        assertThat(html).contains("successfully onboarded");
        assertThat(html).contains("Temp123!");
        assertThat(html).doesNotContain("shield.png");
        assertThat(html).doesNotContain("<svg");
    }

    @Test
    @DisplayName("Branded document includes mobile media query and viewport")
    void brandedDocumentIsMobileReady() {
        String html = EmailHtmlComponents.brandedDocument("Test", "Clinic", "<p>Hello</p>");
        assertThat(html).contains("viewport");
        assertThat(html).contains("@media only screen and (max-width:620px)");
        assertThat(html).contains("font-size:18px!important");
        assertThat(html).contains("font-size:14px!important");
        assertThat(html).contains("email-btn-link");
        assertThat(html).contains("email-body-pad");
        assertThat(html).contains("max-width:600px");
        assertThat(html).contains("width:100%");
    }
}
