package com.smart.therapy.flow.admin.service;

import com.smart.therapy.flow.admin.dto.AdminIntegrationHealthResponse;
import com.smart.therapy.flow.admin.dto.IntegrationTestResponse;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.payment.dto.OrgStripeConnectStatusResponse;
import com.smart.therapy.flow.payment.service.OrgStripeConnectService;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.user.dto.AvailableSlotResponse;
import com.smart.therapy.flow.user.entity.UserIntegration;
import com.smart.therapy.flow.user.repository.UserIntegrationRepository;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminIntegrationHealthService {

    private final OrgStripeConnectService orgStripeConnectService;
    private final UserRepository userRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final TherapistAvailabilityService therapistAvailabilityService;

    @Autowired(required = false)
    private SessionService.ZoomService zoomService;

    @Transactional(readOnly = true)
    public AdminIntegrationHealthResponse getIntegrationHealth(
            LocalDate date,
            Long serviceId,
            String sessionType,
            String timezone,
            boolean includeSlots,
            boolean runZoomLiveTest,
            Collection<Long> therapistIds) {
        Long organisationId = requireOrganisationId();
        validateSlotsArgs(includeSlots, date, serviceId);

        OrgStripeConnectStatusResponse stripeStatus = orgStripeConnectService.getStatus(organisationId);
        AdminIntegrationHealthResponse.StripeHealth stripe = mapStripeHealth(stripeStatus);

        List<User> therapists = loadTherapists(therapistIds);
        List<AdminIntegrationHealthResponse.TherapistZoomStatus> therapistStatuses = therapists.stream()
                .map(t -> buildTherapistStatus(t, date, serviceId, sessionType, timezone, includeSlots, runZoomLiveTest))
                .collect(Collectors.toList());

        int configured = (int) therapistStatuses.stream().filter(t -> Boolean.TRUE.equals(t.getZoomConfigured())).count();
        int active = (int) therapistStatuses.stream().filter(t -> Boolean.TRUE.equals(t.getZoomActive())).count();
        int healthy = (int) therapistStatuses.stream().filter(t -> Boolean.TRUE.equals(t.getZoomHealthy())).count();

        AdminIntegrationHealthResponse.ZoomHealth zoom = AdminIntegrationHealthResponse.ZoomHealth.builder()
                .totalTherapists(therapistStatuses.size())
                .configuredTherapists(configured)
                .activeTherapists(active)
                .healthyTherapists(healthy)
                .date(date)
                .serviceId(serviceId)
                .sessionType(sessionType)
                .timezone(timezone)
                .therapists(therapistStatuses)
                .build();

        return AdminIntegrationHealthResponse.builder()
                .organisationId(organisationId)
                .generatedAt(Instant.now())
                .stripe(stripe)
                .zoom(zoom)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminIntegrationHealthResponse.TherapistZoomStatus> getTherapistZoomAvailabilityList(
            LocalDate date,
            Long serviceId,
            String sessionType,
            String timezone,
            boolean runZoomLiveTest,
            Collection<Long> therapistIds) {
        validateSlotsArgs(true, date, serviceId);
        return loadTherapists(therapistIds).stream()
                .map(t -> buildTherapistStatus(t, date, serviceId, sessionType, timezone, true, runZoomLiveTest))
                .collect(Collectors.toList());
    }

    @Transactional
    public IntegrationTestResponse testStripeIntegration(Long actorAuthId) {
        Long organisationId = requireOrganisationId();
        try {
            OrgStripeConnectStatusResponse refreshed = orgStripeConnectService.refresh(organisationId, actorAuthId);
            boolean success = Boolean.TRUE.equals(refreshed.getChargesEnabled())
                    && StringUtils.hasText(refreshed.getConnectAccountId());
            return IntegrationTestResponse.builder()
                    .integration("stripe")
                    .success(success)
                    .message(success ? "Stripe integration is healthy" : "Stripe integration is connected but not fully ready")
                    .testedAt(Instant.now())
                    .details(Map.of(
                            "organisationId", organisationId,
                            "onboardingStatus", String.valueOf(refreshed.getOnboardingStatus()),
                            "chargesEnabled", Boolean.TRUE.equals(refreshed.getChargesEnabled()),
                            "payoutsEnabled", Boolean.TRUE.equals(refreshed.getPayoutsEnabled()),
                            "connectAccountId", refreshed.getConnectAccountId() != null ? refreshed.getConnectAccountId() : ""
                    ))
                    .build();
        } catch (Exception ex) {
            return IntegrationTestResponse.builder()
                    .integration("stripe")
                    .success(false)
                    .message("Stripe integration test failed: " + ex.getMessage())
                    .testedAt(Instant.now())
                    .details(Map.of("organisationId", organisationId))
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public IntegrationTestResponse testTherapistZoomIntegration(Long therapistId) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        UserIntegration integration = userIntegrationRepository.findByUserAndIntegrationType(therapist, "zoom")
                .orElse(null);
        boolean configured = integration != null
                && StringUtils.hasText(integration.getExternalUserId())
                && integration.getSettings() != null;
        boolean active = configured && Boolean.TRUE.equals(integration.getIsActive());

        if (!configured) {
            return IntegrationTestResponse.builder()
                    .integration("zoom")
                    .success(false)
                    .message("Zoom is not configured for this therapist")
                    .testedAt(Instant.now())
                    .details(Map.of("therapistId", therapistId, "therapistName", therapist.getFullName()))
                    .build();
        }

        if (!active) {
            return IntegrationTestResponse.builder()
                    .integration("zoom")
                    .success(false)
                    .message("Zoom integration is configured but inactive")
                    .testedAt(Instant.now())
                    .details(Map.of("therapistId", therapistId, "therapistName", therapist.getFullName()))
                    .build();
        }

        if (zoomService == null) {
            return IntegrationTestResponse.builder()
                    .integration("zoom")
                    .success(false)
                    .message("Zoom service is unavailable in current deployment")
                    .testedAt(Instant.now())
                    .details(Map.of("therapistId", therapistId, "therapistName", therapist.getFullName()))
                    .build();
        }

        try {
            boolean healthy = zoomService.testIntegration(therapist);
            return IntegrationTestResponse.builder()
                    .integration("zoom")
                    .success(healthy)
                    .message(healthy ? "Zoom integration is healthy" : "Zoom integration test failed")
                    .testedAt(Instant.now())
                    .details(Map.of("therapistId", therapistId, "therapistName", therapist.getFullName()))
                    .build();
        } catch (Exception ex) {
            return IntegrationTestResponse.builder()
                    .integration("zoom")
                    .success(false)
                    .message("Zoom integration test failed: " + ex.getMessage())
                    .testedAt(Instant.now())
                    .details(Map.of("therapistId", therapistId, "therapistName", therapist.getFullName()))
                    .build();
        }
    }

    private AdminIntegrationHealthResponse.TherapistZoomStatus buildTherapistStatus(
            User therapist,
            LocalDate date,
            Long serviceId,
            String sessionType,
            String timezone,
            boolean includeSlots,
            boolean runZoomLiveTest) {
        UserIntegration integration = userIntegrationRepository.findByUserAndIntegrationType(therapist, "zoom")
                .orElse(null);

        boolean configured = integration != null
                && StringUtils.hasText(integration.getExternalUserId())
                && integration.getSettings() != null;
        boolean zoomActive = configured && Boolean.TRUE.equals(integration.getIsActive());
        Boolean zoomHealthy = null;
        if (runZoomLiveTest) {
            if (zoomActive && zoomService != null) {
                try {
                    zoomHealthy = zoomService.testIntegration(therapist);
                } catch (Exception ex) {
                    log.warn("Zoom live test failed for therapist {}: {}", therapist.getId(), ex.getMessage());
                    zoomHealthy = false;
                }
            } else {
                zoomHealthy = false;
            }
        }

        List<AdminIntegrationHealthResponse.SlotItem> slotItems = new ArrayList<>();
        if (includeSlots) {
            List<AvailableSlotResponse> slots = therapistAvailabilityService.getAvailableTimeSlots(
                    therapist.getId(),
                    date,
                    serviceId,
                    timezone,
                    normalizeSessionType(sessionType));

            slotItems = slots.stream()
                    .filter(AvailableSlotResponse::isAvailable)
                    .map(s -> AdminIntegrationHealthResponse.SlotItem.builder()
                            .time(s.getTime())
                            .timezone(s.getTimezone())
                            .localTime(s.getLocalTime() != null ? s.getLocalTime().toString() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        return AdminIntegrationHealthResponse.TherapistZoomStatus.builder()
                .therapistId(therapist.getId())
                .therapistName(therapist.getFullName())
                .therapistActive(Boolean.TRUE.equals(therapist.getIsActive()))
                .zoomConfigured(configured)
                .zoomActive(zoomActive)
                .zoomHealthy(zoomHealthy)
                .zoomLastUpdatedAt(integration != null ? integration.getUpdatedAt() : null)
                .availableSlotsCount(slotItems.size())
                .availableSlots(slotItems)
                .build();
    }

    private List<User> loadTherapists(Collection<Long> therapistIds) {
        List<User> therapists = userRepository.findDistinctByAuthIdentityRolesRoleNameIn(
                List.of(RoleName.THERAPIST.name()));
        if (therapistIds == null || therapistIds.isEmpty()) {
            return therapists;
        }
        return therapists.stream()
                .filter(t -> therapistIds.contains(t.getId()))
                .collect(Collectors.toList());
    }

    private AdminIntegrationHealthResponse.StripeHealth mapStripeHealth(OrgStripeConnectStatusResponse status) {
        boolean connected = StringUtils.hasText(status.getConnectAccountId());
        boolean healthy = connected
                && Boolean.TRUE.equals(status.getChargesEnabled())
                && Boolean.TRUE.equals(status.getDetailsSubmitted());
        return AdminIntegrationHealthResponse.StripeHealth.builder()
                .connected(connected)
                .onboardingStatus(status.getOnboardingStatus() != null ? status.getOnboardingStatus().name() : null)
                .chargesEnabled(status.getChargesEnabled())
                .payoutsEnabled(status.getPayoutsEnabled())
                .detailsSubmitted(status.getDetailsSubmitted())
                .connectAccountId(status.getConnectAccountId())
                .lastSyncedAt(status.getLastSyncedAt())
                .disabledReason(status.getDisabledReason())
                .healthy(healthy)
                .build();
    }

    private void validateSlotsArgs(boolean includeSlots, LocalDate date, Long serviceId) {
        if (!includeSlots) {
            return;
        }
        if (date == null) {
            throw new BadRequestException("date is required when includeSlots=true");
        }
        if (serviceId == null) {
            throw new BadRequestException("serviceId is required when includeSlots=true");
        }
    }

    private String normalizeSessionType(String sessionType) {
        if (!StringUtils.hasText(sessionType)) {
            return null;
        }
        String value = sessionType.trim().toLowerCase(Locale.ROOT);
        if ("online".equals(value) || "in-person".equals(value)) {
            return value;
        }
        return sessionType;
    }

    private Long requireOrganisationId() {
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            throw new BadRequestException("Organization context not resolved");
        }
        return organisationId;
    }
}
