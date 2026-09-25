package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.service.EmailHtmlComponents;
import com.smart.therapy.flow.common.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @Test
    void welcomeEmailEscapesClientAndTherapistMarkup() {
        String html = ReflectionTestUtils.invokeMethod(emailService, "buildWelcomeEmailTemplate",
                "<script>alert('XSS')</script>", "<img src=x onerror=alert(1)>");
        assertThat(html).contains("&lt;script&gt;", "&lt;img")
                .doesNotContain("<script>", "<img src=x");
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@therapyflow.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Should send email successfully")
    void shouldSendEmailSuccessfully() throws Exception {
        String to = "client@example.com";
        String subject = "Test Subject";
        String htmlBody = "<html><body>Test Email</body></html>";

        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendEmail(to, subject, htmlBody);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should not send email when recipient is empty")
    void shouldNotSendEmailWhenRecipientIsEmpty() {
        emailService.sendEmail("", "Test Subject", "<html><body>Test Email</body></html>");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should not send email when recipient is null")
    void shouldNotSendEmailWhenRecipientIsNull() {
        emailService.sendEmail(null, "Test Subject", "<html><body>Test Email</body></html>");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception when messaging fails")
    void shouldThrowExceptionWhenMessagingFails() {
        doThrow(new RuntimeException("Failed to send")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("client@example.com", "Test Subject", "<html><body>Test</body></html>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send");
    }

    @Test
    @DisplayName("Fragment emails should use SmartHub branded shell with centered logo")
    void fragmentEmailsShouldUseSmartHubShell() {
        String branded = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildBrandedHtml",
                "Session Scheduled",
                "Acme Clinic",
                "<p>Body</p>",
                EmailHtmlComponents.PRIMARY);

        assertThat(branded).contains(EmailHtmlComponents.PRODUCT_NAME);
        assertThat(branded).contains(EmailHtmlComponents.LOGO_URL);
        assertThat(branded).contains("smarthub-brain-icon-512.png");
        assertThat(branded).contains("vertical-align:middle");
        assertThat(branded).contains("padding-right:10px");
        assertThat(branded).contains("SmartHub");
        assertThat(branded).contains("Acme Clinic");
        assertThat(branded).doesNotContain("TherapyFlow");
        assertThat(branded).contains(EmailHtmlComponents.PRIMARY);
    }

    @Test
    @DisplayName("Primary CTA helper uses SmartHub brand color")
    void primaryCtaUsesBrandColor() {
        String button = EmailHtmlComponents.primaryButtonTemplate("Pay Now", "{{paymentUrl}}");
        assertThat(button).contains(EmailHtmlComponents.PRIMARY);
        assertThat(button).contains("{{paymentUrl}}");
        assertThat(button).doesNotContain("#0ea5e9");
        assertThat(button).doesNotContain("#2563eb");
    }

    @Test
    @DisplayName("Should send activation email successfully")
    void shouldSendActivationEmailSuccessfully() {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        emailService.sendActivationEmail("client@example.com", "John Doe", "test-token-123");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Activation email link should use frontend base URL, not backend base URL")
    void activationEmailShouldUseFrontendBaseUrl() {
        ReflectionTestUtils.setField(emailService, "staffLoginUrl",
                "https://app.therapyflow.pro/auth/therapist/login");

        String activationUrl = ReflectionTestUtils.invokeMethod(
                emailService, "buildClientActivationUrl", "abc123");

        assertThat(activationUrl).isEqualTo("https://app.therapyflow.pro/portal/activate/abc123");
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void shouldSendPasswordResetEmailSuccessfully() {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        emailService.sendPasswordResetEmail("client@example.com", "John Doe", "reset-token-123");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send appointment confirmation email successfully")
    void shouldSendAppointmentConfirmationEmailSuccessfully() {
        Map<String, String> appointmentDetails = new HashMap<>();
        appointmentDetails.put("date", "2024-12-15");
        appointmentDetails.put("time", "10:00 AM");
        appointmentDetails.put("therapist", "Dr. Smith");

        doNothing().when(mailSender).send(any(MimeMessage.class));
        emailService.sendAppointmentConfirmationEmail("client@example.com", "John Doe", appointmentDetails);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send invoice email successfully")
    void shouldSendInvoiceEmailSuccessfully() {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        emailService.sendInvoiceEmail("client@example.com", "John Doe", "<div>Invoice Content</div>");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Staff welcome credentials template should include credential card and SmartHub CTA")
    void staffWelcomeUsesClinicalClarityLayout() {
        String html = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildUserWelcomeEmailTemplate",
                "Admin User",
                "admin@example.com",
                "TempPass123",
                "https://app.therapyflow.pro/auth/therapist/login");

        assertThat(html).contains("Welcome to your SmartHub workspace");
        assertThat(html).contains("admin@example.com");
        assertThat(html).contains("TempPass123");
        assertThat(html).contains(EmailHtmlComponents.PRIMARY);
        assertThat(html).contains("Login to SmartHub");
        assertThat(html).doesNotContain("#2563eb");
    }
}
