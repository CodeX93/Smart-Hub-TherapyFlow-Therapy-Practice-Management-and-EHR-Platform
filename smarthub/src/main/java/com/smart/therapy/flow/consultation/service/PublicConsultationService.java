package com.smart.therapy.flow.consultation.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientMrnService;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.consultation.config.ConsultationProperties;
import com.smart.therapy.flow.consultation.dto.PublicBookConsultationRequest;
import com.smart.therapy.flow.consultation.dto.PublicBookConsultationResponse;
import com.smart.therapy.flow.consultation.dto.PublicTherapistResponse;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.service.TwilioSmsService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.publicsite.dto.PublicSiteServiceResponse;
import com.smart.therapy.flow.publicsite.entity.PublicSiteService;
import com.smart.therapy.flow.publicsite.service.PublicSiteServiceAdminService;
import com.smart.therapy.flow.session.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.user.dto.AvailableSlotResponse;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class PublicConsultationService {

    private final TenantDirectoryService tenantDirectoryService;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final ClientMrnService clientMrnService;
    private final BlindIndexService blindIndexService;
    private final SessionRepository sessionRepository;
    private final TherapistAvailabilityService therapistAvailabilityService;
    private final ConsultationProperties consultationProperties;
    private final EmailService emailService;
    private final TimezoneService timezoneService;
    private final PublicSiteServiceAdminService publicSiteServiceAdminService;
    private final PublicConsultationExistingClientGuard existingClientGuard;
    private final ClientService clientService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private TwilioSmsService twilioSmsService;

    /** Same Zoom stack as staff SessionService (ZoomApiService). Optional when zoom.enabled=false. */
    @Autowired(required = false)
    private SessionService.ZoomService zoomService;

    public List<PublicTherapistResponse> listTherapists(String orgSlug) {
        return withOrgTenantRead(orgSlug, () -> userRepository
                .findDistinctByAuthIdentityRolesRoleNameIn(List.of(
                        RoleName.THERAPIST.name(),
                        RoleName.SUPERVISOR.name()))
                .stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> PublicTherapistResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .title(u.getTitle())
                        .phone(u.getPhone())
                        .isActive(u.getIsActive())
                        .build())
                .collect(Collectors.toList()));
    }

    public List<AvailableSlotResponse> getAvailability(
            String orgSlug,
            Long therapistId,
            LocalDate date,
            String serviceCode,
            String sessionType,
            Long publicServiceId) {
        return withOrgTenantRead(orgSlug, () -> {
            Service service = resolveConsultationService(serviceCode);
            userRepository.findById(therapistId)
                    .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
            Integer durationOverride = null;
            if (publicServiceId != null) {
                PublicSiteService publicSvc = publicSiteServiceAdminService.requireEnabled(publicServiceId);
                durationOverride = publicSvc.getDurationMinutes();
            }
            return therapistAvailabilityService.getAvailableTimeSlots(
                    therapistId, date, service.getId(), null, sessionType, durationOverride);
        });
    }

    public List<PublicSiteServiceResponse> listPublicServices(String orgSlug) {
        return withOrgTenantRead(orgSlug, publicSiteServiceAdminService::listEnabled);
    }

    public PublicBookConsultationResponse book(String orgSlug, PublicBookConsultationRequest request) {
        return withOrgTenantWrite(orgSlug, () -> bookInCurrentTenant(request));
    }

    private PublicBookConsultationResponse bookInCurrentTenant(PublicBookConsultationRequest request) {
        Objects.requireNonNull(request, "Request is required");
        if (request.getSessionStartUtc().isBefore(Instant.now())) {
            throw new BadRequestException("Cannot book a consultation in the past");
        }
        existingClientGuard.requireNewClientEmail(request.getClientEmail());

        Service service = resolveConsultationService(consultationProperties.getServiceCode());
        PublicSiteService publicLabel = resolvePublicServiceLabel(request);
        String publicServiceName = publicLabel.getName();
        BigDecimal publicBaseRate = publicLabel.getBaseRate() != null
                ? publicLabel.getBaseRate()
                : BigDecimal.ZERO;

        User therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
        if (!Boolean.TRUE.equals(therapist.getIsActive())) {
            throw new BadRequestException("Therapist is not available for booking");
        }

        // Duration comes from the selected public service; hours still from CONSULTATION schedule.
        int duration = publicLabel.getDurationMinutes() != null && publicLabel.getDurationMinutes() > 0
                ? publicLabel.getDurationMinutes()
                : (service.getDuration() != null ? service.getDuration() : 30);
        // Public visitors do not choose modality — inherit from consultation schedule for this slot.
        String sessionMode = therapistAvailabilityService.resolveSessionModeForPublicBooking(
                therapist.getId(),
                request.getSessionStartUtc(),
                duration,
                service.getId());

        therapistAvailabilityService.assertTherapistAvailableForBooking(
                therapist.getId(),
                request.getSessionStartUtc(),
                duration,
                sessionMode,
                service.getId());

        // Availability checks may hydrate Session→Client graphs; clear before MRN allocate/create
        // so Hibernate does not flush detached collection owners mid-query.
        Long publicServiceId = publicLabel.getId();
        entityManager.clear();
        therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
        service = resolveConsultationService(consultationProperties.getServiceCode());
        publicLabel = publicSiteServiceAdminService.requireEnabled(publicServiceId);
        publicServiceName = publicLabel.getName();
        publicBaseRate = publicLabel.getBaseRate() != null ? publicLabel.getBaseRate() : BigDecimal.ZERO;
        duration = publicLabel.getDurationMinutes() != null && publicLabel.getDurationMinutes() > 0
                ? publicLabel.getDurationMinutes()
                : (service.getDuration() != null ? service.getDuration() : 30);

        Client client = createConsultationClient(request, therapist);
        Session session = Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .sessionDate(request.getSessionStartUtc())
                .duration(duration)
                .sessionType(sessionMode)
                .clinicalSessionType(publicServiceName)
                .status("scheduled")
                .notes(buildNotes(request, publicServiceName, publicBaseRate, duration))
                .build();

        Session saved = sessionRepository.save(session);
        String joinUrl = maybeCreateZoomMeeting(saved, therapist, request.getClientTimezone());
        if (StringUtils.hasText(joinUrl)) {
            // Persist integration attached during Zoom create
            saved = sessionRepository.save(saved);
        }
        notifyBooking(saved, request, therapist, publicServiceName, publicBaseRate, duration);

        return PublicBookConsultationResponse.builder()
                .sessionId(saved.getId())
                .therapistId(therapist.getId())
                .therapistName(therapist.getFullName())
                .sessionStartUtc(saved.getSessionDate())
                .durationMinutes(duration)
                .serviceCode(service.getServiceCode())
                .status(saved.getStatus())
                .message("Consultation booked successfully")
                .joinUrl(null)
                .publicServiceId(publicLabel.getId())
                .publicServiceName(publicServiceName)
                .baseRate(publicBaseRate)
                .build();
    }

    private PublicSiteService resolvePublicServiceLabel(PublicBookConsultationRequest request) {
        if (request.getPublicServiceId() != null) {
            return publicSiteServiceAdminService.requireEnabled(request.getPublicServiceId());
        }
        if (StringUtils.hasText(request.getPublicServiceSlug())) {
            return publicSiteServiceAdminService.requireEnabledBySlug(request.getPublicServiceSlug());
        }
        // Default when client omits selection: Consultation system label
        return publicSiteServiceAdminService.requireEnabledBySlug("consultation");
    }

    /**
     * Create a Zoom meeting for online/virtual public consultations when the therapist
     * has Zoom configured. Soft-fails so the booking still succeeds if Zoom is unavailable.
     */
    private String maybeCreateZoomMeeting(Session session, User therapist, String requestedTimezone) {
        if (!isOnlineOrVirtualMode(session.getSessionType())) {
            return null;
        }
        if (zoomService == null || !zoomService.isTherapistConfigured(therapist)) {
            log.warn(
                    "Public consultation session {} is online but Zoom is not configured for therapist {}",
                    session.getId(), therapist.getId());
            return null;
        }
        try {
            ZoneId meetingTz = resolveMeetingTimezone(therapist.getId(), requestedTimezone);
            Map<String, Object> zoomRequest = new HashMap<>();
            zoomRequest.put("topic", "Consultation with " + therapist.getFullName());
            zoomRequest.put("startTime", session.getSessionDate());
            zoomRequest.put("duration", session.getDuration());
            zoomRequest.put("timezone", meetingTz.getId());
            zoomRequest.put("settings", Map.of(
                    "waiting_room", true,
                    "video_host", true,
                    "video_participant", true,
                    "mute_upon_entry", true));

            ZoomMeetingResponse zoomMeeting = zoomService.createMeeting(zoomRequest, therapist);
            SessionIntegration zoomIntegration = SessionIntegration.builder()
                    .session(session)
                    .provider("zoom")
                    .meetingId(zoomMeeting.getMeetingId())
                    .joinUrl(zoomMeeting.getJoinUrl())
                    .password(zoomMeeting.getPassword())
                    .build();
            if (session.getIntegrations() == null) {
                session.setIntegrations(new ArrayList<>());
            }
            session.getIntegrations().add(zoomIntegration);
            return zoomMeeting.getJoinUrl();
        } catch (Exception e) {
            log.warn(
                    "Failed to create Zoom meeting for public consultation session {}: {}",
                    session.getId(), e.getMessage());
            return null;
        }
    }

    private ZoneId resolveMeetingTimezone(Long therapistId, String requestedTimezone) {
        if (StringUtils.hasText(requestedTimezone)) {
            try {
                return ZoneId.of(requestedTimezone.trim());
            } catch (Exception ignored) {
                // fall through
            }
        }
        return timezoneService.getTherapistTimezone(therapistId).orElse(ZoneId.of("UTC"));
    }

    private static boolean isOnlineOrVirtualMode(String sessionMode) {
        return SystemOptionKeyMatcher.matchesAny(sessionMode, "online", "virtual", "telehealth", "video");
    }

    Client createConsultationClient(PublicBookConsultationRequest request, User therapist) {
        String mrn = allocatePublicMrn();
        Client client = Client.builder()
                .clientId(mrn)
                .fullName(request.getClientFullName().trim())
                .status("active")
                .stage("intake")
                .clientType("individual")
                .assignedTherapist(therapist)
                .timezone(StringUtils.hasText(request.getClientTimezone())
                        ? request.getClientTimezone().trim()
                        : null)
                .notes("Created via public consultation booking")
                .lastUpdateDate(Instant.now())
                .build();

        List<ClientContact> contacts = new ArrayList<>();
        contacts.add(ClientContact.builder()
                .client(client)
                .contactType(ContactType.EMAIL)
                .contactValue(request.getClientEmail().trim())
                .isPrimary(true)
                .label("Primary")
                .build());
        if (StringUtils.hasText(request.getClientPhone())) {
            contacts.add(ClientContact.builder()
                    .client(client)
                    .contactType(ContactType.PHONE)
                    .contactValue(request.getClientPhone().trim())
                    .isPrimary(true)
                    .label("Mobile")
                    .build());
        }
        client.setContacts(contacts);
        blindIndexService.updateBlindIndexes(client, contacts);
        Client saved = clientRepository.saveAndFlush(client);
        clientService.enablePortalForPublicBooking(
                saved,
                request.getClientEmail().trim(),
                therapist.getId(),
                therapist.getFullName());
        return saved;
    }

    /** Unique MRN for public web bookings (avoids broken encrypted legacy MRN scans). */
    private String allocatePublicMrn() {
        return "CL-WEB-" + java.util.UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase();
    }

    private void notifyBooking(
            Session session,
            PublicBookConsultationRequest request,
            User therapist,
            String publicServiceName,
            BigDecimal baseRate,
            int durationMinutes) {
        ZoneId zone = ZoneId.of("UTC");
        if (StringUtils.hasText(request.getClientTimezone())) {
            try {
                zone = ZoneId.of(request.getClientTimezone().trim());
            } catch (Exception ignored) {
                // keep UTC
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' h:mm a z").withZone(zone);
        String when = fmt.format(session.getSessionDate());
        String clientLabel = request.getClientFullName().trim();
        String subject = "New public booking — " + publicServiceName + " — " + clientLabel;
        String rateLabel = baseRate != null ? baseRate.toPlainString() : "0";

        String html = """
                <p>A new consultation was booked on the public site.</p>
                <ul>
                  <li><strong>Service:</strong> %s</li>
                  <li><strong>Duration:</strong> %s min</li>
                  <li><strong>Rate:</strong> %s</li>
                  <li><strong>Client:</strong> %s</li>
                  <li><strong>Email:</strong> %s</li>
                  <li><strong>Phone:</strong> %s</li>
                  <li><strong>Therapist:</strong> %s</li>
                  <li><strong>When:</strong> %s</li>
                  <li><strong>Session ID:</strong> %s</li>
                </ul>
                """.formatted(
                escape(publicServiceName),
                durationMinutes,
                escape(rateLabel),
                escape(clientLabel),
                escape(request.getClientEmail()),
                escape(StringUtils.hasText(request.getClientPhone()) ? request.getClientPhone() : "—"),
                escape(therapist.getFullName()),
                escape(when),
                session.getId());

        if (StringUtils.hasText(therapist.getEmail())) {
            try {
                emailService.sendEmail(therapist.getEmail(), subject, html);
            } catch (Exception e) {
                log.warn("Failed to email therapist for public consultation: therapistId={}", therapist.getId(), e);
            }
        }

        String opsEmail = consultationProperties.getOpsEmail();
        if (StringUtils.hasText(opsEmail)) {
            try {
                emailService.sendEmail(opsEmail, subject, html);
            } catch (Exception e) {
                log.warn("Failed to email ops for public consultation: opsEmail={}", opsEmail, e);
            }
        }

        // Client confirmation uses the same EmailService / spring.mail stack
        if (StringUtils.hasText(request.getClientEmail())) {
            String clientSubject = "Your " + publicServiceName + " is booked — " + therapist.getFullName();
            String clientHtml = """
                    <p>Hi %s,</p>
                    <p>Your <strong>%s</strong> with <strong>%s</strong> is booked for <strong>%s</strong>.</p>
                    <p>If you have questions, reply to this email or contact the clinic.</p>
                    """.formatted(
                    escape(clientLabel),
                    escape(publicServiceName),
                    escape(therapist.getFullName()),
                    escape(when));
            try {
                emailService.sendEmail(request.getClientEmail().trim(), clientSubject, clientHtml);
            } catch (Exception e) {
                log.warn("Failed to email client for public consultation: sessionId={}", session.getId(), e);
            }
        }

        if (twilioSmsService != null && twilioSmsService.isSmsConfigured()
                && StringUtils.hasText(therapist.getPhone())) {
            String e164 = PhoneNormalizationUtil.normalizePhoneE164(therapist.getPhone());
            if (StringUtils.hasText(e164)) {
                String smsBody = "New " + publicServiceName + " booked with " + clientLabel + " on " + when
                        + ". Check TherapyFlow for details.";
                SmsSendResult result = twilioSmsService.sendSms(e164, smsBody);
                if (!result.isSuccess()) {
                    log.info("Therapist SMS skipped/failed for public consultation: therapistId={}, result={}",
                            therapist.getId(), result);
                }
            } else {
                log.info("Skipping therapist SMS — phone not E.164 for therapistId={}", therapist.getId());
            }
        }
    }

    private Service resolveConsultationService(String serviceCode) {
        String code = StringUtils.hasText(serviceCode)
                ? serviceCode.trim()
                : consultationProperties.getServiceCode();
        Service service = serviceRepository.findByServiceCode(code)
                .orElseThrow(() -> new BadRequestException(
                        "Consultation service (" + code + ") is not configured for this organisation"));
        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new BadRequestException("Consultation service is not active");
        }
        if (!Boolean.TRUE.equals(service.getPublicSiteEnabled())
                && !"CONSULTATION".equalsIgnoreCase(service.getServiceCode())) {
            throw new BadRequestException("Service is not enabled for the public site");
        }
        return service;
    }

    private String buildNotes(
            PublicBookConsultationRequest request,
            String publicServiceName,
            BigDecimal baseRate,
            int durationMinutes) {
        StringBuilder sb = new StringBuilder("Public booking: ").append(publicServiceName);
        sb.append(" | ").append(durationMinutes).append(" min");
        if (baseRate != null) {
            sb.append(" | rate ").append(baseRate.toPlainString());
        }
        if (StringUtils.hasText(request.getNotes())) {
            sb.append(" — ").append(request.getNotes().trim());
        }
        sb.append(" | Contact: ").append(request.getClientEmail().trim());
        if (StringUtils.hasText(request.getClientPhone())) {
            sb.append(" / ").append(request.getClientPhone().trim());
        }
        return sb.toString();
    }

    private <T> T withOrgTenantRead(String orgSlug, Supplier<T> action) {
        TenantDirectoryService.TenantInfo info = resolveOrg(orgSlug);
        return tenantTransactionExecutor.executeReadOnlyIsolated(
                info.getOrganisationId(), info.getSchemaName(), action);
    }

    private <T> T withOrgTenantWrite(String orgSlug, Supplier<T> action) {
        TenantDirectoryService.TenantInfo info = resolveOrg(orgSlug);
        return tenantTransactionExecutor.executeWriteIsolated(
                info.getOrganisationId(), info.getSchemaName(), action);
    }

    private TenantDirectoryService.TenantInfo resolveOrg(String orgSlug) {
        if (!StringUtils.hasText(orgSlug)) {
            throw new BadRequestException("Organisation slug is required");
        }
        TenantDirectoryService.TenantInfo info = tenantDirectoryService.findBySlug(orgSlug.trim())
                .or(() -> tenantDirectoryService.findByOrganisationIdOrSlug(orgSlug.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found: " + orgSlug));
        if (!info.isActive() || info.isForceDisabled()) {
            throw new BadRequestException("Organisation is unavailable");
        }
        if (!tenantSchemaHealthService.schemaExists(info.getSchemaName())) {
            throw new BadRequestException("Organisation schema is not provisioned");
        }
        return info;
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
