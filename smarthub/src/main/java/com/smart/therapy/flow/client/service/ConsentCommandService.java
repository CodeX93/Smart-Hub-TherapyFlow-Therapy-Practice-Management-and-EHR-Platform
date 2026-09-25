package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.enums.ConsentStatus;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.portal.dto.PortalConsentResponse;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentCommandService {

    private final PatientConsentRepository patientConsentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final ClientReportAccessService clientReportAccessService;

    @Transactional
    public PortalConsentResponse recordStaffConsent(Long clientId,
                                                    ConsentType consentType,
                                                    Boolean granted,
                                                    String consentVersion,
                                                    String source,
                                                    String notes,
                                                    String auditReason,
                                                    AuthPrincipal requester,
                                                    String ipAddress,
                                                    String userAgent) {
        if (consentType == null || granted == null) {
            throw new BadRequestException("Consent type and granted are required");
        }
        User actor = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!canWriteStaffConsent(requester)) {
            throw new BadRequestException("Insufficient permissions to record consent");
        }
        Client client = clientReportAccessService.requireClientAccess(clientId, requester);
        Instant now = Instant.now();
        PatientConsent consent = PatientConsent.builder()
                .client(client)
                .createdByUser(actor)
                .consentType(consentType)
                .consentFormVersion(StringUtils.hasText(consentVersion) ? consentVersion.trim() : "1.0")
                .granted(granted)
                .grantedAt(now)
                .withdrawnAt(granted ? null : now)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .notes(buildNotes(source, notes, auditReason, true))
                .consentStatus(granted ? ConsentStatus.GRANTED : ConsentStatus.WITHDRAWN)
                .consentDate(java.time.LocalDate.now())
                .build();
        PatientConsent saved = patientConsentRepository.save(consent);
        auditService.recordAuditEventWithUser(actor.getId(), builder -> builder
                .action(granted ? "staff_consent_granted" : "staff_consent_withdrawn")
                .resourceType("consent")
                .resourceId(saved.getId() != null ? String.valueOf(saved.getId()) : null)
                .hipaaRelevant(true)
                .ipAddress(ipAddress)
                .details(String.format("clientId=%d, consentType=%s, source=%s, reason=%s, userAgent=%s",
                        clientId, consentType.name(), defaultSource(source), safe(auditReason), safe(userAgent))));
        return toResponse(saved);
    }

    @Transactional
    public PortalConsentResponse recordInboundSmsConsent(Long clientId,
                                                         boolean granted,
                                                         String ipAddress,
                                                         String userAgent) {
        return togglePortalConsent(
                clientId,
                ConsentType.SMS_COMMUNICATION,
                granted,
                "1.0",
                granted ? "SMS consent granted via inbound Twilio message" : "SMS consent withdrawn via inbound Twilio message",
                "twilio_inbound",
                ipAddress,
                userAgent);
    }

    @Transactional
    public PortalConsentResponse togglePortalConsent(Long clientId,
                                                     ConsentType consentType,
                                                     Boolean granted,
                                                     String consentVersion,
                                                     String notes,
                                                     String source,
                                                     String ipAddress,
                                                     String userAgent) {
        if (consentType == null || granted == null) {
            throw new BadRequestException("Consent type and granted are required");
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        User createdBy = client.getAssignedTherapist();
        if (createdBy == null) {
            createdBy = userRepository.findById(1L).orElse(null);
        }
        if (createdBy == null) {
            throw new BadRequestException("Unable to resolve consent actor");
        }
        String effectiveVersion = StringUtils.hasText(consentVersion) ? consentVersion.trim() : "1.0";

        Optional<PatientConsent> existing = findIdempotentMatch(client.getId(), consentType, effectiveVersion, granted);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Instant now = Instant.now();
        PatientConsent consent = PatientConsent.builder()
                .client(client)
                .createdByUser(createdBy)
                .consentType(consentType)
                .consentFormVersion(effectiveVersion)
                .granted(granted)
                .grantedAt(now)
                .withdrawnAt(granted ? null : now)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .notes(buildNotes(source, notes, null, false))
                .consentStatus(granted ? ConsentStatus.GRANTED : ConsentStatus.WITHDRAWN)
                .consentDate(java.time.LocalDate.now())
                .build();
        try {
            return toResponse(patientConsentRepository.save(consent));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("Constraint violation while saving portal consent; retrying lookup. clientId={}, consentType={}, version={}",
                    clientId, consentType, effectiveVersion);
            Optional<PatientConsent> retry = findIdempotentMatch(client.getId(), consentType, effectiveVersion, granted);
            if (retry.isPresent()) {
                return toResponse(retry.get());
            }
            throw new ConflictException("Unable to create consent due to concurrent update or data constraint");
        }
    }

    private Optional<PatientConsent> findIdempotentMatch(Long clientId, ConsentType consentType, String version, Boolean granted) {
        return patientConsentRepository.findLatestByClientIdAndConsentType(clientId, consentType)
                .filter(c -> Objects.equals(c.getConsentFormVersion(), version))
                .filter(c -> Objects.equals(Boolean.TRUE.equals(c.getGranted()), Boolean.TRUE.equals(granted)));
    }

    private boolean canWriteStaffConsent(AuthPrincipal requester) {
        return permissionChecker.hasRole(requester, "ADMIN")
                || permissionChecker.hasRole(requester, "SUPERVISOR")
                || permissionChecker.hasRole(requester, "THERAPIST")
                || permissionChecker.hasPermission(requester, "CLIENT_EDIT");
    }

    private String defaultSource(String source) {
        return StringUtils.hasText(source) ? source.trim() : "signed_consent_form";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "n/a";
    }

    private String buildNotes(String source, String notes, String auditReason, boolean includeReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("source=").append(defaultSource(source));
        if (StringUtils.hasText(notes)) {
            sb.append("; notes=").append(notes.trim());
        }
        if (includeReason && StringUtils.hasText(auditReason)) {
            sb.append("; auditReason=").append(auditReason.trim());
        }
        return sb.toString();
    }

    private PortalConsentResponse toResponse(PatientConsent c) {
        return PortalConsentResponse.builder()
                .id(c.getId())
                .clientId(c.getClient() != null ? c.getClient().getId() : null)
                .consentType(c.getConsentType() != null ? c.getConsentType().getDisplayName() : null)
                .consentVersion(c.getConsentFormVersion())
                .granted(c.getGranted())
                .grantedAt(c.getGrantedAt())
                .withdrawnAt(c.getWithdrawnAt())
                .ipAddress(c.getIpAddress())
                .userAgent(c.getUserAgent())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
