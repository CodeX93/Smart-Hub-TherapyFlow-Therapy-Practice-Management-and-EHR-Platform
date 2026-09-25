package com.smart.therapy.flow.e2e;

import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.session.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.user.entity.ShiftMode;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.user.repository.UserProfileWorkingHoursRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the booking flow the way a client does: log in to the portal, read the offered
 * slots, book one, and be turned away from anything the therapist did not offer.
 */
class ClientPortalBookingFlowE2ETest extends BaseTenantApiTest {

    @Autowired private ClientRepository clients;
    @Autowired private ServiceRepository services;
    @Autowired private SessionRepository sessions;
    @Autowired private UserProfileRepository profiles;
    @Autowired private UserProfileWorkingHoursRepository workingHours;
    @MockBean private SessionService.ZoomService zoomService;

    /** Three days out: past-slot filtering and same-day edges stay out of the way. */
    private LocalDate bookingDate() {
        return LocalDate.now(ZoneOffset.UTC).plusDays(3);
    }

    private Client portalClientOf(com.smart.therapy.flow.auth.entity.User therapist) {
        ensureFixtureRole("CLIENT");
        Client client = persistClient(therapist);
        client.setAssignedTherapist(therapist);
        return clients.save(client);
    }

    private com.smart.therapy.flow.auth.entity.User therapistWithZoom() {
        var therapist = persistStaff(uniqueEmail("portal-booking-therapist"), "password123", "THERAPIST");
        ZoomMeetingResponse meeting = new ZoomMeetingResponse();
        meeting.setMeetingId("portal-booking-meeting");
        meeting.setJoinUrl("https://zoom.example.test/portal-booking-meeting");
        meeting.setPassword("passcode");
        lenient().when(zoomService.isTherapistConfigured(any())).thenReturn(true);
        lenient().when(zoomService.createMeeting(anyMap(), any())).thenReturn(meeting);
        return therapist;
    }

    /** A second clinical row overlapping the fixture's 09:00-17:00 — the duplicate-slot setup. */
    private void addOverlappingClinicalHours(Long therapistId) {
        var profile = profiles.findByUserId(therapistId).orElseThrow();
        for (DayOfWeek day : DayOfWeek.values()) {
            workingHours.save(UserProfileWorkingHours.builder()
                    .userProfile(profile).day(day.name())
                    .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(14, 0))
                    .sessionMode(ShiftMode.BOTH).build());
        }
    }

    /** Consultation hours sit outside the clinical window; the portal must ignore them. */
    private void addConsultationOnlyHours(Long therapistId) {
        Service consultation = services.findByServiceCode("CONSULTATION")
                .orElseGet(() -> services.save(Service.builder()
                        .serviceCode("CONSULTATION").serviceName("Consultation")
                        .duration(30).baseRate(BigDecimal.ZERO)
                        .isActive(true).publicSiteEnabled(true).build()));
        var profile = profiles.findByUserId(therapistId).orElseThrow();
        for (DayOfWeek day : DayOfWeek.values()) {
            workingHours.save(UserProfileWorkingHours.builder()
                    .userProfile(profile).service(consultation).day(day.name())
                    .startTime(LocalTime.of(6, 0)).endTime(LocalTime.of(8, 0))
                    .sessionMode(ShiftMode.BOTH).build());
        }
    }

    private JsonNode fetchSlots(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/portal/available-slots")
                        .headers(createHeaders(token))
                        .param("startDate", bookingDate().toString())
                        .param("endDate", bookingDate().toString())
                        .param("sessionType", "online")
                        .param("serviceId", String.valueOf(fixtureService.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private List<String> startTimesOn(JsonNode slotsResponse) {
        JsonNode day = slotsResponse.path("slotsByDate").path(bookingDate().toString());
        List<String> starts = new ArrayList<>();
        day.forEach(slot -> starts.add(slot.path("start").asText()));
        return starts;
    }

    private String bookingBody(String startUtc) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "sessionStartUtc", startUtc,
                "serviceId", fixtureService.getId(),
                "sessionType", "online"));
    }

    @Test
    void clientSeesOnlyClinicalHoursOnceAndBooksOne() throws Exception {
        var therapist = therapistWithZoom();
        addOverlappingClinicalHours(therapist.getId());
        addConsultationOnlyHours(therapist.getId());
        String token = portalToken(portalClientOf(therapist));

        JsonNode response = fetchSlots(token);
        List<String> starts = startTimesOn(response);

        assertThat(starts).isNotEmpty();
        // Every offered time appears exactly once, despite two overlapping clinical rows.
        assertThat(starts).doesNotHaveDuplicates();
        assertThat(new HashSet<>(starts)).hasSameSizeAs(starts);
        // Confined to the clinical window: the 06:00/07:00 consultation hours never leak in.
        assertThat(starts).doesNotContain("06:00", "07:00", "08:00");
        assertThat(starts).allSatisfy(start ->
                assertThat(LocalTime.parse(start)).isBetween(LocalTime.of(9, 0), LocalTime.of(16, 0)));
        // The response states the zone its times belong to.
        assertThat(response.path("timezone").asText()).isNotBlank();

        JsonNode firstSlot = response.path("slotsByDate").path(bookingDate().toString()).get(0);
        String startUtc = firstSlot.path("startUtc").asText();
        long before = sessions.count();

        mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token)).content(bookingBody(startUtc)))
                .andExpect(status().isCreated());

        assertThat(sessions.count()).isEqualTo(before + 1);
    }

    @Test
    void bookingOutsideTheOfferedHoursIsRejected() throws Exception {
        var therapist = therapistWithZoom();
        String token = portalToken(portalClientOf(therapist));
        long before = sessions.count();

        // 03:00 UTC is nowhere near the 09:00-17:00 the therapist offers.
        String body = mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token))
                        .content(bookingBody(bookingDate().atTime(3, 0).toInstant(ZoneOffset.UTC).toString())))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("outside your therapist's availability");
        assertThat(sessions.count()).isEqualTo(before);
    }

    @Test
    void bookingConsultationOnlyHoursIsRejectedForAClinicalService() throws Exception {
        var therapist = therapistWithZoom();
        addConsultationOnlyHours(therapist.getId());
        String token = portalToken(portalClientOf(therapist));
        long before = sessions.count();

        // 06:30 UTC exists, but only on the consultation schedule.
        String body = mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token))
                        .content(bookingBody(bookingDate().atTime(6, 30).toInstant(ZoneOffset.UTC).toString())))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("outside your therapist's availability");
        assertThat(sessions.count()).isEqualTo(before);
    }

    @Test
    void bookingASlotSomeoneAlreadyTookIsRejected() throws Exception {
        var therapist = therapistWithZoom();
        String token = portalToken(portalClientOf(therapist));
        String startUtc = bookingDate().atTime(10, 0).toInstant(ZoneOffset.UTC).toString();

        mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token)).content(bookingBody(startUtc)))
                .andExpect(status().isCreated());

        long after = sessions.count();
        mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token)).content(bookingBody(startUtc)))
                .andExpect(status().isBadRequest());

        assertThat(sessions.count()).isEqualTo(after);
    }

    @Test
    void bookingATimeThatHasPassedIsRejected() throws Exception {
        var therapist = therapistWithZoom();
        String token = portalToken(portalClientOf(therapist));

        mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token))
                        .content(bookingBody(Instant.now().minusSeconds(3600).toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void slotsAreRefusedWithoutAPortalToken() throws Exception {
        mockMvc.perform(get("/api/v1/portal/available-slots")
                        .headers(createHeaders())
                        .param("startDate", bookingDate().toString())
                        .param("endDate", bookingDate().toString())
                        .param("sessionType", "online")
                        .param("serviceId", String.valueOf(fixtureService.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void slotsFollowTheClientsOwnTimezoneAndBookingNeverOverwritesIt() throws Exception {
        var therapist = therapistWithZoom();
        Client client = portalClientOf(therapist);
        client.setTimezone("Asia/Karachi");
        clients.save(client);
        String token = portalToken(clients.findById(client.getId()).orElseThrow());

        assertThat(fetchSlots(token).path("timezone").asText()).isEqualTo("Asia/Karachi");

        // Book using a +09:00 offset: it pins the instant and nothing more.
        String startUtc = bookingDate().atTime(10, 0).toInstant(ZoneOffset.UTC)
                .atOffset(ZoneOffset.ofHours(9)).toString();
        mockMvc.perform(post("/api/v1/portal/book-appointment")
                        .headers(createHeaders(token)).content(bookingBody(startUtc)))
                .andExpect(status().isCreated());

        // The request left the tenant context on the public schema.
        enterFixtureTenant();
        assertThat(clients.findById(client.getId()).orElseThrow().getTimezone())
                .isEqualTo("Asia/Karachi");
    }
}
