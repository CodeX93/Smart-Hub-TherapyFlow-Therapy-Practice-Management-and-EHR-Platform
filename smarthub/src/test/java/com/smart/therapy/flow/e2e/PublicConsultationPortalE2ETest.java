package com.smart.therapy.flow.e2e;

import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.publicsite.entity.PublicSiteService;
import com.smart.therapy.flow.publicsite.repository.PublicSiteServiceRepository;
import com.smart.therapy.flow.session.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.user.entity.ShiftMode;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.user.repository.UserProfileWorkingHoursRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicConsultationPortalE2ETest extends BaseTenantApiTest {

    @Autowired private ServiceRepository services;
    @Autowired private PublicSiteServiceRepository publicServices;
    @Autowired private SessionRepository sessions;
    @Autowired private ClientPortalSettingsService portalSettings;
    @Autowired private RoleRepository roles;
    @Autowired private EmailService emailService;
    @Autowired private UserProfileRepository profiles;
    @Autowired private UserProfileWorkingHoursRepository workingHours;
    @MockBean private SessionService.ZoomService zoomService;

    @Test
    void newPublicConsultationEnablesActivationAndSupportsPortalLogin() throws Exception {
        var therapist = persistStaff(uniqueEmail("public-consultation-therapist"), "password123", "THERAPIST");
        // Shared helper so the CLIENT role always carries CLIENT_PORTAL_ACCESS, whichever
        // suite happens to create it first.
        ensureFixtureRole("CLIENT");

        Service consultation = services.findByServiceCode("CONSULTATION")
                .orElseGet(() -> services.save(Service.builder()
                        .serviceCode("CONSULTATION")
                        .serviceName("Consultation")
                        .duration(30)
                        .baseRate(BigDecimal.ZERO)
                        .isActive(true)
                        .publicSiteEnabled(true)
                        .build()));
        consultation.setIsActive(true);
        consultation.setPublicSiteEnabled(true);
        services.save(consultation);

        var therapistProfile = profiles.findByUserId(therapist.getId()).orElseThrow();
        for (DayOfWeek day : DayOfWeek.values()) {
            workingHours.save(UserProfileWorkingHours.builder()
                    .userProfile(therapistProfile)
                    .service(consultation)
                    .day(day.name())
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(17, 0))
                    .sessionMode(ShiftMode.BOTH)
                    .build());
        }

        PublicSiteService publicService = publicServices.findBySlugAndIsDeletedFalse("consultation")
                .orElseGet(() -> publicServices.save(PublicSiteService.builder()
                        .name("Consultation")
                        .slug("consultation")
                        .durationMinutes(30)
                        .baseRate(BigDecimal.ZERO)
                        .enabled(true)
                        .isSystem(true)
                        .build()));

        String email = uniqueEmail("public-consultation-client");
        Instant sessionStart = fixtureSessionDate(2);
        Map<String, Object> booking = Map.of(
                "therapistId", therapist.getId(),
                "sessionStartUtc", sessionStart.toString(),
                "clientFullName", "Public Consultation Client",
                "clientEmail", email,
                "clientTimezone", "UTC",
                "publicServiceId", publicService.getId());

        String response = mockMvc.perform(post("/api/v1/public/orgs/{orgSlug}/consultations", TENANT_SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andReturn().getResponse().getContentAsString();

        enterFixtureTenant();
        var session = sessions.findById(objectMapper.readTree(response).path("sessionId").asLong()).orElseThrow();
        var client = clientRepository.findById(session.getClient().getId()).orElseThrow();
        var identity = authIdentityRepository.findAllByEmailOrUsernameAndIdentityTypeForOrganisation(
                        email.toLowerCase(), IdentityType.CLIENT, tenantOrganisation.getId())
                .stream().findFirst().orElseThrow();
        assertThat(client.getAuthIdentity()).isNotNull();
        assertThat(identity.getIdentityType()).isEqualTo(IdentityType.CLIENT);
        assertThat(identity.getNormalisedLoginIdentifier()).isEqualTo(email.toLowerCase());
        assertThat(portalSettings.hasPortalAccess(client.getId())).isTrue();
        assertThat(portalSettings.isActivated(client.getId())).isFalse();
        String storedActivationToken = identity.getEmailVerificationToken();
        assertThat(storedActivationToken).startsWith("sha256:");
        assertThat(identity.getEmailVerificationExpiry()).isAfter(Instant.now());
        ArgumentCaptor<String> activationTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2_000)).sendActivationEmailAsync(
                org.mockito.ArgumentMatchers.eq(email),
                org.mockito.ArgumentMatchers.eq("Public Consultation Client"),
                activationTokenCaptor.capture());
        String activationToken = activationTokenCaptor.getValue();
        assertThat(activationToken).isNotBlank().doesNotStartWith("sha256:");

        mockMvc.perform(post("/api/v1/public/orgs/{orgSlug}/consultations", TENANT_SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PUBLIC_CLIENT_EXISTS"));

        mockMvc.perform(post("/api/v1/portal/activate")
                        .headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", activationToken,
                                "password", "chosen-password-123"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "chosen-password-123",
                                "orgSlug", TENANT_SLUG))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void virtualPublicConsultationDoesNotSendZoomLink() throws Exception {
        var therapist = persistStaff(uniqueEmail("public-consultation-no-zoom"), "password123", "THERAPIST");

        Service consultation = services.findByServiceCode("CONSULTATION")
                .orElseGet(() -> services.save(Service.builder()
                        .serviceCode("CONSULTATION")
                        .serviceName("Consultation")
                        .duration(30)
                        .baseRate(BigDecimal.ZERO)
                        .isActive(true)
                        .publicSiteEnabled(true)
                        .build()));
        consultation.setIsActive(true);
        consultation.setPublicSiteEnabled(true);
        services.save(consultation);

        var therapistProfile = profiles.findByUserId(therapist.getId()).orElseThrow();
        for (DayOfWeek day : DayOfWeek.values()) {
            workingHours.save(UserProfileWorkingHours.builder()
                    .userProfile(therapistProfile)
                    .service(consultation)
                    .day(day.name())
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(17, 0))
                    .sessionMode(ShiftMode.VIRTUAL)
                    .build());
        }

        PublicSiteService publicService = publicServices.findBySlugAndIsDeletedFalse("consultation")
                .orElseGet(() -> publicServices.save(PublicSiteService.builder()
                        .name("Consultation")
                        .slug("consultation")
                        .durationMinutes(30)
                        .baseRate(BigDecimal.ZERO)
                        .enabled(true)
                        .isSystem(true)
                        .build()));

        ZoomMeetingResponse zoomMeeting = new ZoomMeetingResponse();
        zoomMeeting.setMeetingId("consultation-meeting");
        zoomMeeting.setJoinUrl("https://zoom.example.test/consultation-meeting");
        zoomMeeting.setPassword("secret");
        when(zoomService.isTherapistConfigured(any())).thenReturn(true);
        when(zoomService.createMeeting(anyMap(), any())).thenReturn(zoomMeeting);

        String email = uniqueEmail("no-zoom-client");
        Map<String, Object> booking = Map.of(
                "therapistId", therapist.getId(),
                "sessionStartUtc", fixtureSessionDate(3).toString(),
                "clientFullName", "No Zoom Link Client",
                "clientEmail", email,
                "clientTimezone", "UTC",
                "publicServiceId", publicService.getId());

        String response = mockMvc.perform(post("/api/v1/public/orgs/{orgSlug}/consultations", TENANT_SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var responseJson = objectMapper.readTree(response);
        assertThat(responseJson.path("joinUrl").isMissingNode() || responseJson.path("joinUrl").isNull()).isTrue();

        ArgumentCaptor<String> emailHtml = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(any(), any(), emailHtml.capture());
        assertThat(emailHtml.getAllValues())
                .allSatisfy(html -> assertThat(html)
                        .doesNotContain("https://zoom.example.test/consultation-meeting")
                        .doesNotContain("Join Zoom")
                        .doesNotContain("<strong>Zoom:</strong>"));
    }
}
