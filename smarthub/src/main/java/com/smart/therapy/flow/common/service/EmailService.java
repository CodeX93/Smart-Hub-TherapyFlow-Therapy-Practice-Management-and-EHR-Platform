package com.smart.therapy.flow.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired(required = false)
    private EmailProviderService emailProviderService;

    @Autowired(required = false)
    private TimezoneService timezoneService;

    @Autowired(required = false)
    private OrganisationRepository organisationRepository;

    @Value("${spring.mail.from:noreply@therapyflow.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend.staff-login-url:${APP_FRONTEND_STAFF_LOGIN_URL:https://app.therapyflow.pro/auth/therapist/login}}")
    private String staffLoginUrl;

    public void sendEmail(String to, String subject, String htmlBody) {
        if (!StringUtils.hasText(to)) {
            log.warn("Cannot send email: recipient email is empty, correlationId={}", MDC.get("correlationId"));
            return;
        }

        String tenantName = EmailHtmlComponents.PRODUCT_NAME;
        String brandColor = EmailHtmlComponents.PRIMARY;
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && organisationRepository != null) {
            var orgOpt = organisationRepository.findById(orgId);
            if (orgOpt.isPresent()) {
                com.smart.therapy.flow.organisation.entity.Organisation org = orgOpt.get();
                if (StringUtils.hasText(org.getName())) {
                    tenantName = org.getName();
                }
                if (StringUtils.hasText(org.getBrandPrimaryColor())) {
                    brandColor = org.getBrandPrimaryColor();
                }
            }
        }
        
        String brandedHtml = htmlBody;
        if (htmlBody != null && !htmlBody.trim().toLowerCase().startsWith("<!doctype html") && !htmlBody.trim().toLowerCase().startsWith("<html")) {
            brandedHtml = buildBrandedHtml(subject, tenantName, htmlBody, brandColor);
        }

        // Use EmailProviderService if available (SparkPost or SES), otherwise fall back
        // to JavaMailSender
        if (emailProviderService != null) {
            try {
                emailProviderService.sendEmail(to, subject, brandedHtml);
                return;
            } catch (Exception e) {
                log.warn("EmailProviderService failed, falling back to JavaMailSender: correlationId={}",
                        MDC.get("correlationId"), e);
                // Fall through to JavaMailSender
            }
        }

        // Fallback to JavaMailSender
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(brandedHtml, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent successfully via JavaMailSender: to={}, correlationId={}",
                    maskRecipient(to), MDC.get("correlationId"));
        } catch (MessagingException e) {
            log.error("Failed to send email: to={}, correlationId={}", maskRecipient(to),
                    MDC.get("correlationId"), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String htmlBody) {
        try {
            sendEmail(to, subject, htmlBody);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async email sending failed: to={}, correlationId={}", maskRecipient(to), MDC.get("correlationId"), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void sendActivationEmail(String to, String clientName, String activationToken) {
        String activationUrl = buildClientActivationUrl(activationToken);
        String htmlBody = buildActivationEmailTemplate(clientName, activationUrl);
        sendEmail(to, "Activate Your SmartHub Portal Account", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendActivationEmailAsync(String to, String clientName, String activationToken) {
        sendActivationEmail(to, clientName, activationToken);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send welcome email to newly created client.
     * Uses SES (or configured email provider) via EmailService.sendEmail().
     * 
     * @param to Client email address
     * @param clientName Client's full name
     * @param therapistName Assigned therapist's name (optional)
     */
    public void sendWelcomeEmail(String to, String clientName, String therapistName) {
        String htmlBody = buildWelcomeEmailTemplate(clientName, therapistName);
        sendEmail(to, "Welcome to SmartHub", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendWelcomeEmailAsync(String to, String clientName, String therapistName) {
        sendWelcomeEmail(to, clientName, therapistName);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send organisation onboarding welcome email with credentials and organisation name.
     */
    public void sendOrganisationOnboardingEmail(
            String to,
            String adminName,
            String organisationName,
            String username,
            String temporaryPassword) {
        String loginUrl = staffLoginUrl;
        String htmlBody = EmailHtmlComponents.organisationOnboardingEmailBody(
                adminName, organisationName, username, temporaryPassword, loginUrl);
        String subject = "Welcome to SmartHub — " + (organisationName != null ? organisationName : "Your organisation");
        sendEmail(to, subject, htmlBody);
    }

    /**
     * Send welcome email to newly created user with credentials.
     * Uses SES (or configured email provider) via EmailService.sendEmail().
     * 
     * @param to User email address
     * @param userName User's full name
     * @param username Username for login
     * @param temporaryPassword Temporary password (will be shown in email)
     */
    public void sendUserWelcomeEmail(String to, String userName, String username, String temporaryPassword) {
        String loginUrl = staffLoginUrl;
        String htmlBody = buildUserWelcomeEmailTemplate(userName, username, temporaryPassword, loginUrl);
        sendEmail(to, "Welcome to SmartHub - Your Account Details", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendUserWelcomeEmailAsync(String to, String userName, 
            String username, String temporaryPassword) {
        sendUserWelcomeEmail(to, userName, username, temporaryPassword);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send welcome email to newly created user without sharing credentials.
     * This is the preferred onboarding path for security.
     */
    public void sendUserWelcomeEmailNoPassword(String to, String userName, String username) {
        String loginUrl = staffLoginUrl;
        String htmlBody = buildUserWelcomeEmailNoPasswordTemplate(userName, username, loginUrl);
        sendEmail(to, "Welcome to SmartHub", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendUserWelcomeEmailNoPasswordAsync(String to, String userName, String username) {
        sendUserWelcomeEmailNoPassword(to, userName, username);
        return CompletableFuture.completedFuture(null);
    }

    public void sendPasswordResetEmail(String to, String clientName, String resetToken) {
        String resetUrl = buildClientSetNewPasswordUrl(resetToken);
        String htmlBody = buildPasswordResetEmailTemplate(clientName, resetUrl);
        sendEmail(to, "Reset Your SmartHub Portal Password", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendPasswordResetEmailAsync(String to, String clientName, String resetToken) {
        sendPasswordResetEmail(to, clientName, resetToken);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send password reset email to staff (admin/therapist/supervisor).
     * Link points to frontend set-new-password page; user then calls
     * POST /api/v1/auth/reset-password with token and new password.
     */
    public void sendStaffPasswordResetEmail(String to, String staffName, String resetToken) {
        String resetUrl = buildStaffSetNewPasswordUrl(resetToken);
        String htmlBody = buildStaffPasswordResetEmailTemplate(staffName, resetUrl);
        sendEmail(to, "Reset Your SmartHub Password", htmlBody);
    }

    public void sendNewDeviceLoginAlert(
            String to,
            String userName,
            String deviceLabel,
            String ipAddress,
            String userAgent,
            Instant at
    ) {
        String when = at != null ? at.toString() : Instant.now().toString();
        String safeName = userName != null ? userName : to;
        String htmlBody = EmailHtmlComponents.heroBadge(EmailHtmlComponents.iconShield())
                + EmailHtmlComponents.heading("New sign-in to SmartHub")
                + EmailHtmlComponents.paragraph("Hi " + escapeHtml(safeName) + ",")
                + EmailHtmlComponents.paragraph(
                        "We noticed a sign-in from a device we have not seen on your account before.")
                + EmailHtmlComponents.detailCard(
                        EmailHtmlComponents.detailRow("Device", escapeHtml(deviceLabel))
                                + EmailHtmlComponents.detailRow("IP address", escapeHtml(ipAddress))
                                + EmailHtmlComponents.detailRow("Time (UTC)", escapeHtml(when)))
                + EmailHtmlComponents.mutedNote("Browser details: " + escapeHtml(truncate(userAgent, 180)))
                + EmailHtmlComponents.paragraph(
                        "If this was you, no action is needed. If you do not recognize this sign-in, "
                                + "change your password and review your MFA method in Security settings.");
        sendEmail(to, "New sign-in to your SmartHub account", htmlBody);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendStaffPasswordResetEmailAsync(String to, String staffName, String resetToken) {
        sendStaffPasswordResetEmail(to, staffName, resetToken);
        return CompletableFuture.completedFuture(null);
    }

    private String buildStaffSetNewPasswordUrl(String resetToken) {
        String frontendBaseUrl = resolveFrontendBaseUrlFromStaffLoginUrl();
        try {
            return UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                    .path("/auth/staff/set-new-password")
                    .queryParam("token", resetToken)
                    .build()
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid frontend base URL '{}' for staff reset link; using string fallback", frontendBaseUrl);
            return frontendBaseUrl + "/auth/staff/set-new-password?token=" + resetToken;
        }
    }

    private String buildClientSetNewPasswordUrl(String resetToken) {
        String frontendBaseUrl = resolveFrontendBaseUrlFromStaffLoginUrl();
        try {
            return UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                    .path("/auth/set-new-password")
                    .queryParam("token", resetToken)
                    .build()
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid frontend base URL '{}' for client reset link; using string fallback", frontendBaseUrl);
            return frontendBaseUrl + "/auth/set-new-password?token=" + resetToken;
        }
    }

    private String buildClientActivationUrl(String activationToken) {
        String frontendBaseUrl = resolveFrontendBaseUrlFromStaffLoginUrl();
        try {
            return UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                    .path("/portal/activate/" + activationToken)
                    .build()
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid frontend base URL '{}' for client activation link; using string fallback", frontendBaseUrl);
            return frontendBaseUrl + "/portal/activate/" + activationToken;
        }
    }

    private String resolveFrontendBaseUrlFromStaffLoginUrl() {
        String candidate = StringUtils.hasText(staffLoginUrl) ? staffLoginUrl : baseUrl;
        try {
            URI uri = URI.create(candidate);
            if (StringUtils.hasText(uri.getScheme()) && StringUtils.hasText(uri.getAuthority())) {
                return uri.getScheme() + "://" + uri.getAuthority();
            }
        } catch (Exception ex) {
            log.warn("Invalid staff frontend URL config '{}', using fallback base URL", staffLoginUrl);
        }
        return baseUrl;
    }

    public void sendAppointmentConfirmationEmail(String to, String clientName, Map<String, String> appointmentDetails) {
        String htmlBody = buildAppointmentConfirmationTemplate(clientName, appointmentDetails);
        sendEmail(to, "Appointment Confirmation", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendAppointmentConfirmationEmailAsync(String to, String clientName,
            Map<String, String> appointmentDetails) {
        sendAppointmentConfirmationEmail(to, clientName, appointmentDetails);
        return CompletableFuture.completedFuture(null);
    }

    public void sendInvoiceEmail(String to, String clientName, String invoiceHtml) {
        String htmlBody = buildInvoiceEmailTemplate(clientName, invoiceHtml);
        sendEmail(to, "Your Invoice", htmlBody);
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendInvoiceEmailAsync(String to, String clientName, String invoiceHtml) {
        sendInvoiceEmail(to, clientName, invoiceHtml);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send session confirmation email to client or therapist.
     * Time is displayed in Administration practice timezone.
     * Includes Zoom join link and password if zoomEnabled=true.
     */
    public void sendSessionConfirmationEmail(Object recipient, Object session, Object otherParty) {
        try {
            // Determine recipient type and extract information
            String recipientEmail = null;
            String recipientName = null;
            boolean isClient = false;

            if (recipient instanceof com.smart.therapy.flow.client.entity.Client) {
                com.smart.therapy.flow.client.entity.Client client = (com.smart.therapy.flow.client.entity.Client) recipient;
                recipientEmail = client.getPrimaryEmail(); // Use normalized entity
                recipientName = client.getFullName();
                isClient = true;
            } else if (recipient instanceof com.smart.therapy.flow.auth.entity.User) {
                com.smart.therapy.flow.auth.entity.User user = (com.smart.therapy.flow.auth.entity.User) recipient;
                recipientEmail = user.getEmail();
                recipientName = user.getFullName();
            }

            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("Cannot send session confirmation email: recipient email is empty");
                return;
            }

            // Extract session information
            com.smart.therapy.flow.session.entity.Session sessionEntity = (com.smart.therapy.flow.session.entity.Session) session;

            // Extract other party information
            String otherPartyName = null;
            if (otherParty instanceof com.smart.therapy.flow.client.entity.Client) {
                otherPartyName = ((com.smart.therapy.flow.client.entity.Client) otherParty).getFullName();
            } else if (otherParty instanceof com.smart.therapy.flow.auth.entity.User) {
                otherPartyName = ((com.smart.therapy.flow.auth.entity.User) otherParty).getFullName();
            }

            java.time.ZoneId practiceTimezone = resolvePracticeTimezone();
            java.time.ZonedDateTime zonedDateTime = sessionEntity.getSessionDate().atZone(practiceTimezone);
            String sessionDate = zonedDateTime
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            String sessionTime = zonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a z"));
            String timezoneName = practiceTimezone.getId();

            // Get session details
            String sessionMode = sessionEntity.getSessionType() != null ? sessionEntity.getSessionType() : "N/A";
            String sessionType = sessionEntity.getClinicalSessionType() != null
                    ? sessionEntity.getClinicalSessionType()
                    : (sessionEntity.getService() != null
                            ? (sessionEntity.getService().getCategory() != null
                                    ? sessionEntity.getService().getCategory()
                                    : sessionEntity.getService().getServiceName())
                            : "N/A");
            Integer duration = sessionEntity.getDuration() != null ? sessionEntity.getDuration() : 60;

            // Determine location
            String location = "Office";
            // Get Zoom integration
            SessionIntegration zoomIntegration = getZoomIntegration(sessionEntity);
            if ("online".equalsIgnoreCase(sessionMode) || zoomIntegration != null) {
                location = "Online";
            }

            // Build email template
            String htmlBody = buildSessionConfirmationEmailTemplate(
                    recipientName,
                    otherPartyName,
                    sessionDate,
                    sessionTime,
                    timezoneName,
                    duration,
                    sessionType,
                    location,
                    zoomIntegration != null,
                    zoomIntegration != null ? zoomIntegration.getJoinUrl() : null,
                    zoomIntegration != null ? zoomIntegration.getPassword() : null,
                    isClient);

            String subject = isClient
                    ? "Appointment Confirmation - Therapy Session"
                    : "New Session Scheduled - " + (otherPartyName != null ? otherPartyName : "Client");

            sendEmail(recipientEmail, subject, htmlBody);
            log.info("Session confirmation email sent: to={}, sessionId={}, isClient={}, timezone={}",
                    maskRecipient(recipientEmail), sessionEntity.getId(), isClient, timezoneName);
        } catch (Exception e) {
            log.error("Failed to send session confirmation email", e);
            throw new RuntimeException("Failed to send session confirmation email", e);
        }
    }

    /**
     * Send one combined confirmation for a recurring session series.
     */
    public void sendRecurringSeriesConfirmationEmail(
            com.smart.therapy.flow.client.entity.Client client,
            java.util.List<com.smart.therapy.flow.session.entity.Session> sessions,
            com.smart.therapy.flow.auth.entity.User therapist) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String clientEmail = client.getPrimaryEmail();
            if (clientEmail == null || clientEmail.isBlank()) {
                log.warn("Cannot send series confirmation: client email is empty");
                return;
            }

            java.time.ZoneId timezone = resolvePracticeTimezone();
            java.time.format.DateTimeFormatter dateFormatter =
                    java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");
            java.time.format.DateTimeFormatter timeFormatter =
                    java.time.format.DateTimeFormatter.ofPattern("h:mm a z");

            StringBuilder datesList = new StringBuilder();
            for (com.smart.therapy.flow.session.entity.Session session : sessions) {
                java.time.ZonedDateTime zdt = session.getSessionDate().atZone(timezone);
                datesList.append("<li>")
                        .append(zdt.format(dateFormatter))
                        .append(" at ")
                        .append(zdt.format(timeFormatter))
                        .append(" (")
                        .append(timezone.getId())
                        .append(")</li>");
            }

            com.smart.therapy.flow.session.entity.Session first = sessions.get(0);
            String serviceName = first.getService() != null ? first.getService().getServiceName() : "Therapy Session";
            String htmlBody = EmailHtmlComponents.recurringSeriesConfirmationEmailBody(
                    client.getFullName(),
                    therapist.getFullName(),
                    serviceName,
                    sessions.size(),
                    datesList.toString(),
                    timezone.getId());

            sendEmail(clientEmail, "Recurring Appointment Series Confirmation", htmlBody);

            if (therapist.getEmail() != null && !therapist.getEmail().isBlank()) {
                sendEmail(therapist.getEmail(),
                        "Recurring Series Scheduled - " + client.getFullName(),
                        htmlBody);
            }
        } catch (Exception e) {
            log.error("Failed to send recurring series confirmation email", e);
        }
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendSessionConfirmationEmailAsync(Object recipient, Object session,
            Object otherParty) {
        try {
            sendSessionConfirmationEmail(recipient, session, otherParty);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async session confirmation email sending failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send session cancellation email to client or therapist.
     * Time is displayed in Administration practice timezone.
     * Includes Zoom details when available (for reference).
     */
    public void sendSessionCancellationEmail(Object recipient, Object session, Object otherParty) {
        try {
            String recipientEmail = null;
            String recipientName = null;
            boolean isClient = false;

            if (recipient instanceof com.smart.therapy.flow.client.entity.Client client) {
                recipientEmail = client.getPrimaryEmail();
                recipientName = client.getFullName();
                isClient = true;
            } else if (recipient instanceof com.smart.therapy.flow.auth.entity.User user) {
                recipientEmail = user.getEmail();
                recipientName = user.getFullName();
            }

            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("Cannot send session cancellation email: recipient email is empty");
                return;
            }

            com.smart.therapy.flow.session.entity.Session sessionEntity = (com.smart.therapy.flow.session.entity.Session) session;

            String otherPartyName = null;
            if (otherParty instanceof com.smart.therapy.flow.client.entity.Client) {
                otherPartyName = ((com.smart.therapy.flow.client.entity.Client) otherParty).getFullName();
            } else if (otherParty instanceof com.smart.therapy.flow.auth.entity.User) {
                otherPartyName = ((com.smart.therapy.flow.auth.entity.User) otherParty).getFullName();
            }

            java.time.ZoneId practiceTimezone = resolvePracticeTimezone();
            java.time.ZonedDateTime zonedDateTime = sessionEntity.getSessionDate().atZone(practiceTimezone);
            String sessionDate = zonedDateTime
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            String sessionTime = zonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a z"));
            String timezoneName = practiceTimezone.getId();

            String sessionMode = sessionEntity.getSessionType() != null ? sessionEntity.getSessionType() : "N/A";
            String sessionType = sessionEntity.getClinicalSessionType() != null
                    ? sessionEntity.getClinicalSessionType()
                    : (sessionEntity.getService() != null
                            ? (sessionEntity.getService().getCategory() != null
                                    ? sessionEntity.getService().getCategory()
                                    : sessionEntity.getService().getServiceName())
                            : "N/A");
            Integer duration = sessionEntity.getDuration() != null ? sessionEntity.getDuration() : 60;

            String location = "Office";
            SessionIntegration zoomIntegration = getZoomIntegration(sessionEntity);
            if ("online".equalsIgnoreCase(sessionMode) || zoomIntegration != null) {
                location = "Online";
            }

            String htmlBody = buildSessionCancellationEmailTemplate(
                    recipientName,
                    otherPartyName,
                    sessionDate,
                    sessionTime,
                    timezoneName,
                    duration,
                    sessionType,
                    location,
                    zoomIntegration != null,
                    zoomIntegration != null ? zoomIntegration.getJoinUrl() : null,
                    zoomIntegration != null ? zoomIntegration.getPassword() : null,
                    isClient);

            String subject = isClient
                    ? "Session Cancelled - Therapy Appointment"
                    : "Session Cancelled - " + (otherPartyName != null ? otherPartyName : "Client");

            sendEmail(recipientEmail, subject, htmlBody);
            log.info("Session cancellation email sent: to={}, sessionId={}, isClient={}, timezone={}",
                    maskRecipient(recipientEmail), sessionEntity.getId(), isClient, timezoneName);
        } catch (Exception e) {
            log.error("Failed to send session cancellation email", e);
            throw new RuntimeException("Failed to send session cancellation email", e);
        }
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendSessionCancellationEmailAsync(Object recipient, Object session,
            Object otherParty) {
        try {
            sendSessionCancellationEmail(recipient, session, otherParty);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async session cancellation email sending failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send session reminder email 24 hours before appointment.
     * Time is displayed in recipient's timezone.
     * Includes Zoom join link and password if zoomEnabled=true.
     */
    public void sendSessionReminderEmail(Object recipient, Object session, Object otherParty) {
        try {
            // Determine recipient type and extract information
            String recipientEmail = null;
            String recipientName = null;
            boolean isClient = false;

            if (recipient instanceof com.smart.therapy.flow.client.entity.Client) {
                com.smart.therapy.flow.client.entity.Client client = (com.smart.therapy.flow.client.entity.Client) recipient;
                recipientEmail = client.getPrimaryEmail(); // Use normalized entity
                recipientName = client.getFullName();
                isClient = true;
                // Note: Email notifications check should be done by caller (e.g.,
                // SessionReminderService)
                // as it requires ClientCredentialService which is not available in this common
                // service
            } else if (recipient instanceof com.smart.therapy.flow.auth.entity.User) {
                com.smart.therapy.flow.auth.entity.User user = (com.smart.therapy.flow.auth.entity.User) recipient;
                recipientEmail = user.getEmail();
                recipientName = user.getFullName();
            }

            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("Cannot send session reminder email: recipient email is empty");
                return;
            }

            // Extract session information
            com.smart.therapy.flow.session.entity.Session sessionEntity = (com.smart.therapy.flow.session.entity.Session) session;

            // Extract other party information
            String otherPartyName = null;
            if (otherParty instanceof com.smart.therapy.flow.client.entity.Client) {
                otherPartyName = ((com.smart.therapy.flow.client.entity.Client) otherParty).getFullName();
            } else if (otherParty instanceof com.smart.therapy.flow.auth.entity.User) {
                otherPartyName = ((com.smart.therapy.flow.auth.entity.User) otherParty).getFullName();
            }

            java.time.ZoneId practiceTimezone = resolvePracticeTimezone();
            java.time.ZonedDateTime zonedDateTime = sessionEntity.getSessionDate().atZone(practiceTimezone);
            String sessionDate = zonedDateTime
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            String sessionTime = zonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a z"));
            String timezoneName = practiceTimezone.getId();

            // Get session details
            String sessionMode = sessionEntity.getSessionType() != null ? sessionEntity.getSessionType() : "N/A";
            String sessionType = sessionEntity.getClinicalSessionType() != null
                    ? sessionEntity.getClinicalSessionType()
                    : (sessionEntity.getService() != null
                            ? (sessionEntity.getService().getCategory() != null
                                    ? sessionEntity.getService().getCategory()
                                    : sessionEntity.getService().getServiceName())
                            : "N/A");
            Integer duration = sessionEntity.getDuration() != null ? sessionEntity.getDuration() : 60;

            // Determine location
            String location = "Office";
            // Get Zoom integration
            SessionIntegration zoomIntegration = getZoomIntegration(sessionEntity);
            if ("online".equalsIgnoreCase(sessionMode) || zoomIntegration != null) {
                location = "Online";
            }

            // Build email template
            String htmlBody = buildSessionReminderEmailTemplate(
                    recipientName,
                    otherPartyName,
                    sessionDate,
                    sessionTime,
                    timezoneName,
                    duration,
                    sessionType,
                    location,
                    zoomIntegration != null,
                    zoomIntegration != null ? zoomIntegration.getJoinUrl() : null,
                    zoomIntegration != null ? zoomIntegration.getPassword() : null,
                    isClient);

            String subject = isClient
                    ? "Reminder: Your Therapy Session Tomorrow"
                    : "Reminder: Session Scheduled Tomorrow - " + (otherPartyName != null ? otherPartyName : "Client");

            sendEmail(recipientEmail, subject, htmlBody);
            log.info("Session reminder email sent: to={}, sessionId={}, isClient={}, timezone={}",
                    maskRecipient(recipientEmail), sessionEntity.getId(), isClient, timezoneName);
        } catch (Exception e) {
            log.error("Failed to send session reminder email", e);
            // Don't throw exception for reminders - log and continue
        }
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendSessionReminderEmailAsync(Object recipient, Object session, Object otherParty) {
        try {
            sendSessionReminderEmail(recipient, session, otherParty);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async session reminder email sending failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Administration practice timezone used for session email/notification display.
     */
    private java.time.ZoneId resolvePracticeTimezone() {
        if (timezoneService != null) {
            return timezoneService.getPracticeTimezone();
        }
        return java.time.ZoneId.of("UTC");
    }

    // ========== EMAIL TEMPLATES ==========

    private String buildActivationEmailTemplate(String clientName, String activationUrl) {
        return EmailHtmlComponents.portalActivationEmailBody(escapeHtml(clientName), activationUrl);
    }

    private String buildWelcomeEmailTemplate(String clientName, String therapistName) {
        String therapistSection = "";
        if (therapistName != null && !therapistName.isBlank()) {
            therapistSection = EmailHtmlComponents.paragraph(
                    "You have been assigned to <strong>" + escapeHtml(therapistName)
                            + "</strong>, who will be your primary therapist.");
        }
        return EmailHtmlComponents.clientWelcomeEmailBody(escapeHtml(clientName), therapistSection);
    }

    private String buildUserWelcomeEmailTemplate(String userName, String username,
            String temporaryPassword, String loginUrl) {
        return EmailHtmlComponents.staffWelcomeWithCredentialsEmailBody(
                escapeHtml(userName), username, temporaryPassword, loginUrl);
    }

    private String buildUserWelcomeEmailNoPasswordTemplate(String userName, String username, String loginUrl) {
        return EmailHtmlComponents.staffWelcomeNoPasswordEmailBody(escapeHtml(userName), username, loginUrl);
    }

    private String buildPasswordResetEmailTemplate(String clientName, String resetUrl) {
        return EmailHtmlComponents.passwordResetEmailBody(escapeHtml(clientName), resetUrl, false);
    }

    private String buildStaffPasswordResetEmailTemplate(String staffName, String resetUrl) {
        return EmailHtmlComponents.passwordResetEmailBody(escapeHtml(staffName), resetUrl, true);
    }

    private String buildAppointmentConfirmationTemplate(String clientName, Map<String, String> details) {
        return EmailHtmlComponents.appointmentConfirmationEmailBody(
                escapeHtml(clientName),
                escapeHtml(details.getOrDefault("date", "N/A")),
                escapeHtml(details.getOrDefault("time", "N/A")),
                escapeHtml(details.getOrDefault("duration", "N/A")),
                escapeHtml(details.getOrDefault("sessionType", "N/A")),
                escapeHtml(details.getOrDefault("location", "N/A")));
    }

    private String buildInvoiceEmailTemplate(String clientName, String invoiceHtml) {
        return EmailHtmlComponents.invoiceSimpleEmailBody(escapeHtml(clientName), invoiceHtml);
    }

    private String buildSessionConfirmationEmailTemplate(
            String recipientName,
            String otherPartyName,
            String sessionDate,
            String sessionTime,
            String timezoneName,
            Integer duration,
            String sessionType,
            String location,
            Boolean zoomEnabled,
            String zoomJoinUrl,
            String zoomPassword,
            boolean isClient) {

        String intro = isClient
                ? "Your therapy session with <strong>"
                        + escapeHtml(otherPartyName != null ? otherPartyName : "your therapist")
                        + "</strong> has been confirmed:"
                : "A new therapy session with <strong>"
                        + escapeHtml(otherPartyName != null ? otherPartyName : "a client")
                        + "</strong> has been scheduled:";

        String zoomHtml = Boolean.TRUE.equals(zoomEnabled)
                ? EmailHtmlComponents.zoomMeetingCard(zoomJoinUrl, zoomPassword)
                : "";

        String followUp = isClient
                ? "You can view and manage your appointments in your <a href=\""
                        + escapeHtml(baseUrl) + "/portal/dashboard\" style=\"color:"
                        + EmailHtmlComponents.PRIMARY + ";\">client portal</a>."
                : "You can view and manage this session in your dashboard.";

        String contact = "If you need to reschedule or cancel this appointment, please contact "
                + escapeHtml(isClient
                        ? (otherPartyName != null ? otherPartyName : "your therapist")
                        : (otherPartyName != null ? otherPartyName : "the client"))
                + ".";

        return EmailHtmlComponents.sessionPartyEmailBody(
                EmailHtmlComponents.iconCalendarCheck(),
                "Appointment Confirmation",
                escapeHtml(recipientName),
                intro,
                escapeHtml(sessionDate),
                escapeHtml(sessionTime + " (" + timezoneName + ")"),
                duration != null ? String.valueOf(duration) : null,
                escapeHtml(sessionType != null ? sessionType : "N/A"),
                escapeHtml(location),
                zoomHtml,
                followUp,
                contact);
    }

    private String buildSessionReminderEmailTemplate(
            String recipientName,
            String otherPartyName,
            String sessionDate,
            String sessionTime,
            String timezoneName,
            Integer duration,
            String sessionType,
            String location,
            Boolean zoomEnabled,
            String zoomJoinUrl,
            String zoomPassword,
            boolean isClient) {

        String intro = "This is a reminder that you have a therapy session with <strong>"
                + escapeHtml(otherPartyName != null ? otherPartyName : (isClient ? "your therapist" : "a client"))
                + "</strong> tomorrow:";

        String zoomHtml = Boolean.TRUE.equals(zoomEnabled)
                ? EmailHtmlComponents.zoomMeetingCard(zoomJoinUrl, zoomPassword)
                : "";

        String followUp = isClient
                ? "You can view and manage your appointments in your <a href=\""
                        + escapeHtml(baseUrl) + "/portal/dashboard\" style=\"color:"
                        + EmailHtmlComponents.PRIMARY + ";\">client portal</a>."
                : "You can view and manage this session in your dashboard.";

        String contact = "If you need to reschedule or cancel this appointment, please contact "
                + escapeHtml(isClient
                        ? (otherPartyName != null ? otherPartyName : "your therapist")
                        : (otherPartyName != null ? otherPartyName : "the client"))
                + " as soon as possible.";

        return EmailHtmlComponents.sessionPartyEmailBody(
                EmailHtmlComponents.iconBell(),
                "Session Reminder",
                escapeHtml(recipientName),
                intro,
                escapeHtml(sessionDate),
                escapeHtml(sessionTime + " (" + timezoneName + ")"),
                duration != null ? String.valueOf(duration) : null,
                escapeHtml(sessionType != null ? sessionType : "N/A"),
                escapeHtml(location),
                zoomHtml,
                followUp,
                contact);
    }

    private String buildSessionCancellationEmailTemplate(
            String recipientName,
            String otherPartyName,
            String sessionDate,
            String sessionTime,
            String timezoneName,
            Integer duration,
            String sessionType,
            String location,
            Boolean zoomEnabled,
            String zoomJoinUrl,
            String zoomPassword,
            boolean isClient) {

        String intro = isClient
                ? "Your therapy session with <strong>"
                        + escapeHtml(otherPartyName != null ? otherPartyName : "your therapist")
                        + "</strong> has been cancelled:"
                : "The therapy session with <strong>"
                        + escapeHtml(otherPartyName != null ? otherPartyName : "a client")
                        + "</strong> has been cancelled:";

        String zoomHtml = "";
        if (Boolean.TRUE.equals(zoomEnabled) && zoomJoinUrl != null && !zoomJoinUrl.isBlank()) {
            zoomHtml = EmailHtmlComponents.detailCard(
                    "<p style=\"margin:0 0 8px 0;font-size:14px;font-weight:600;color:"
                            + EmailHtmlComponents.TEXT + ";\">Cancelled Zoom Meeting</p>"
                            + EmailHtmlComponents.detailRow("Meeting URL", escapeHtml(zoomJoinUrl))
                            + (zoomPassword != null && !zoomPassword.isBlank()
                                    ? EmailHtmlComponents.detailRow("Password", escapeHtml(zoomPassword))
                                    : ""));
        }

        String followUp = isClient
                ? "You can view your updated appointments in your <a href=\""
                        + escapeHtml(baseUrl) + "/portal/dashboard\" style=\"color:"
                        + EmailHtmlComponents.PRIMARY + ";\">client portal</a>."
                : "You can view updated session status in your dashboard.";

        String contact = "Please contact "
                + escapeHtml(isClient
                        ? (otherPartyName != null ? otherPartyName : "your therapist")
                        : (otherPartyName != null ? otherPartyName : "the client"))
                + " if you need to reschedule.";

        return EmailHtmlComponents.sessionPartyEmailBody(
                EmailHtmlComponents.iconXCircle(),
                "Session Cancelled",
                escapeHtml(recipientName),
                intro,
                escapeHtml(sessionDate),
                escapeHtml(sessionTime + " (" + timezoneName + ")"),
                duration != null ? String.valueOf(duration) : null,
                escapeHtml(sessionType != null ? sessionType : "N/A"),
                escapeHtml(location),
                zoomHtml,
                followUp,
                contact);
    }

    public void sendTestEmail(String toEmail) {
        String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h1>Test Email</h1>
                    <p>This is a test email from SmartHub.</p>
                    <p>Sender: %s</p>
                    <p>Sent at: %s</p>
                </div>
                """.formatted(fromEmail, java.time.Instant.now().toString());
        sendEmail(toEmail, "Test Email from SmartHub", htmlBody);
    }

    public String getFromAddress() {
        // Return from address from EmailProviderService if available, otherwise use
        // default
        if (emailProviderService != null) {
            return emailProviderService.getFromAddress();
        }
        return fromEmail;
    }

    /**
     * Helper method to get Zoom integration from a session
     */
    private SessionIntegration getZoomIntegration(com.smart.therapy.flow.session.entity.Session session) {
        if (session.getIntegrations() == null || session.getIntegrations().isEmpty()) {
            return null;
        }
        return session.getIntegrations().stream()
                .filter(integration -> "zoom".equals(integration.getProvider()))
                .findFirst()
                .orElse(null);
    }

    /** Mask recipient for logs: first char + *** + domain, or SHA-256 prefix when no @ present. */
    static String maskRecipient(String email) {
        if (!StringUtils.hasText(email)) {
            return "[empty]";
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at > 0 && at < trimmed.length() - 1) {
            return trimmed.charAt(0) + "***@" + trimmed.substring(at + 1);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(trimmed.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return "hash:" + hex;
        } catch (NoSuchAlgorithmException e) {
            return "[redacted]";
        }
    }

    private String buildBrandedHtml(String subject, String tenantName, String contentHtml, String brandColor) {
        // brandColor retained for API compatibility; shell uses Clinical Clarity primary.
        return EmailHtmlComponents.brandedDocument(subject, tenantName, contentHtml);
    }

    public void sendBookingRequestConfirmationEmail(String to, String firstName) {
        String subject = "We've received your SmartHub trial request!";
        String htmlBody = EmailHtmlComponents.heroBadge(EmailHtmlComponents.iconMail())
                + EmailHtmlComponents.heading("Hi " + escapeHtml(firstName) + ",")
                + EmailHtmlComponents.paragraph(
                        "Thank you for requesting a 7-Day Free Trial of SmartHub!")
                + EmailHtmlComponents.paragraph(
                        "We have successfully received your practice information. Our team is currently reviewing your details to ensure we set up the perfect environment tailored to your practice size and specialty.")
                + EmailHtmlComponents.paragraph("<strong>What's next?</strong>")
                + EmailHtmlComponents.paragraph(
                        "You can expect to hear back from us within <strong>2 to 3 business days</strong> with your dedicated trial access link and onboarding instructions.")
                + EmailHtmlComponents.paragraph(
                        "In the meantime, if you have any questions, simply reply to this email.");
        sendEmailAsync(to, subject, htmlBody);
    }

    public void sendBookingRequestAcceptedEmail(String to, String firstName, String statusMessage) {
        String subject = "Welcome to SmartHub!";
        StringBuilder content = new StringBuilder();
        content.append(EmailHtmlComponents.heroBadge(EmailHtmlComponents.iconCheckCircle()));
        content.append(EmailHtmlComponents.heading("Welcome, " + escapeHtml(firstName) + "!"));
        content.append(EmailHtmlComponents.paragraph("Your booking request has been accepted."));
        if (statusMessage != null && !statusMessage.isBlank()) {
            content.append(EmailHtmlComponents.detailCard(
                    "<p style=\"margin:0;\"><strong>Note from our team:</strong> "
                            + escapeHtml(statusMessage) + "</p>"));
        }
        content.append(EmailHtmlComponents.paragraph(
                "We will be in touch shortly with your onboarding instructions!"));
        sendEmailAsync(to, subject, content.toString());
    }

    public void sendBookingRequestRejectedEmail(String to, String firstName, String statusMessage) {
        String subject = "Update on your SmartHub Request";
        StringBuilder content = new StringBuilder();
        content.append(EmailHtmlComponents.heroBadge(EmailHtmlComponents.iconAlertCircle()));
        content.append(EmailHtmlComponents.heading("Hi " + escapeHtml(firstName) + ","));
        content.append(EmailHtmlComponents.paragraph(
                "Thank you for your interest. Unfortunately, we are unable to accept your request at this time."));
        if (statusMessage != null && !statusMessage.isBlank()) {
            content.append(EmailHtmlComponents.detailCard(
                    "<p style=\"margin:0;\"><strong>Reason:</strong> "
                            + escapeHtml(statusMessage) + "</p>"));
        }
        sendEmailAsync(to, subject, content.toString());
    }
}
