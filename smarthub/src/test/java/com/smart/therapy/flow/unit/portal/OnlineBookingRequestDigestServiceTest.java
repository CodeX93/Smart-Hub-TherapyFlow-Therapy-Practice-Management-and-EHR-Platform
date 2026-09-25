package com.smart.therapy.flow.unit.portal;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.client.portal.service.OnlineBookingRequestDigestService;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import com.smart.therapy.flow.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnlineBookingRequestDigestServiceTest {

    @Mock private ClientPortalSettingsService portalSettingsService;
    @Mock private EmailService emailService;
    @Mock private TenantExecutionService tenantExecutionService;
    @Mock private SessionService.ZoomService zoomService;

    @InjectMocks private OnlineBookingRequestDigestService digestService;

    private ClientPortalSettings pendingFor(String mrn, String fullName, User therapist) {
        Client client = Client.builder().clientId(mrn).fullName(fullName).assignedTherapist(therapist).build();
        client.setId(mrn.hashCode() & 0xffffL);
        ClientPortalSettings settings = new ClientPortalSettings();
        settings.setClient(client);
        settings.setOnlineBookingRequestedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return settings;
    }

    private User therapist() {
        User therapist = User.builder().fullName("Dr Smith").email("therapist@example.com").build();
        therapist.setId(9L);
        return therapist;
    }

    private void wire() {
        ReflectionTestUtils.setField(digestService, "zoomService", zoomService);
        ReflectionTestUtils.setField(digestService, "zoomSetupUrl", "https://app.example.com/therapist/dashboard");
    }

    @Test
    void sendsOneDigestListingEveryWaitingClientByMrn() {
        wire();
        User therapist = therapist();
        var first = pendingFor("CL-2024-0417", "Sarah Miller", therapist);
        var second = pendingFor("CL-2023-1188", "James Okafor", therapist);
        when(portalSettingsService.findPendingOnlineBookingRequests()).thenReturn(List.of(first, second));
        when(zoomService.isTherapistConfigured(therapist)).thenReturn(false);
        when(portalSettingsService.findLastOnlineBookingDigestAt(9L)).thenReturn(null);

        ReflectionTestUtils.invokeMethod(digestService, "sendOnlineBookingDigestsForTenant");

        verify(emailService).sendEmail(eq("therapist@example.com"),
                eq("2 clients tried to book online sessions"),
                argThat(body -> body.contains("CL-2024-0417")
                        && body.contains("CL-2023-1188")
                        && !body.contains("Sarah Miller")
                        && !body.contains("James Okafor")));
        verify(portalSettingsService).markOnlineBookingRequestsNotified(
                argThat(list -> list.size() == 2), any());
    }

    @Test
    void holdsBackWhenTheTherapistAlreadyGotADigestToday() {
        wire();
        User therapist = therapist();
        when(portalSettingsService.findPendingOnlineBookingRequests())
                .thenReturn(List.of(pendingFor("CL-2024-0417", "Sarah Miller", therapist)));
        when(zoomService.isTherapistConfigured(therapist)).thenReturn(false);
        when(portalSettingsService.findLastOnlineBookingDigestAt(9L))
                .thenReturn(Instant.now().minus(2, ChronoUnit.HOURS));

        ReflectionTestUtils.invokeMethod(digestService, "sendOnlineBookingDigestsForTenant");

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        // Still pending, so tomorrow's run picks it up.
        verify(portalSettingsService, never()).markOnlineBookingRequestsNotified(any(), any());
    }

    @Test
    void clearsRequestsWithoutEmailingWhenZoomGotConnected() {
        wire();
        User therapist = therapist();
        when(portalSettingsService.findPendingOnlineBookingRequests())
                .thenReturn(List.of(pendingFor("CL-2024-0417", "Sarah Miller", therapist)));
        when(zoomService.isTherapistConfigured(therapist)).thenReturn(true);

        ReflectionTestUtils.invokeMethod(digestService, "sendOnlineBookingDigestsForTenant");

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(portalSettingsService).markOnlineBookingRequestsNotified(argThat(list -> list.size() == 1), any());
    }

    @Test
    void leavesRequestsPendingWhenTheEmailFails() {
        wire();
        User therapist = therapist();
        when(portalSettingsService.findPendingOnlineBookingRequests())
                .thenReturn(List.of(pendingFor("CL-2024-0417", "Sarah Miller", therapist)));
        when(zoomService.isTherapistConfigured(therapist)).thenReturn(false);
        when(portalSettingsService.findLastOnlineBookingDigestAt(9L)).thenReturn(null);
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        ReflectionTestUtils.invokeMethod(digestService, "sendOnlineBookingDigestsForTenant");

        verify(portalSettingsService, never()).markOnlineBookingRequestsNotified(any(), any());
    }
}
