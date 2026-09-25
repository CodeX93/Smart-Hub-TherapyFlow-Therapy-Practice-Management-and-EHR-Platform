package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.SmsBodyGenerator;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsBodyGenerator Unit Tests")
class SmsBodyGeneratorTest {

    @Mock
    private PracticeConfigurationService practiceConfigurationService;

    @InjectMocks
    private SmsBodyGenerator smsBodyGenerator;

    @BeforeEach
    void setUpPracticeConfig() {
        when(practiceConfigurationService.getPracticeConfiguration()).thenReturn(
                PracticeConfigurationResponse.builder()
                        .practiceName("SmartHub")
                        .timezone("America/New_York")
                        .build());
    }

    @Test
    void shouldGenerateSessionScheduledBodyWithoutPhi() {
        String body = smsBodyGenerator.generate(
                NotificationEventCatalog.SESSION_SCHEDULED,
                false,
                Map.of("sessionDate", Instant.parse("2026-06-15T18:00:00Z"),
                        "clientName", "Jane Doe",
                        "therapistName", "Dr. Smith",
                        "sessionType", "CBT"));

        assertThat(body).contains("SmartHub");
        assertThat(body).contains("confirmed");
        assertThat(body).contains("Reply STOP to opt out.");
        assertThat(body).doesNotContain("Jane");
        assertThat(body).doesNotContain("Smith");
        assertThat(body).doesNotContain("CBT");
    }

    @Test
    void shouldGenerateReminderBodyForScheduledTrigger() {
        String body = smsBodyGenerator.generate(
                NotificationEventCatalog.SESSION_REMINDER,
                true,
                Map.of("sessionDate", Instant.parse("2026-06-15T18:00:00Z")));

        assertThat(body).startsWith("Reminder:");
        assertThat(body).contains("Reply STOP to opt out.");
    }

    @Test
    void shouldGenerateSeriesScheduledBody() {
        String body = smsBodyGenerator.generate(
                NotificationEventCatalog.SESSION_SERIES_SCHEDULED,
                false,
                Map.of("sessionCount", 8, "firstSessionDate", Instant.parse("2026-06-15T18:00:00Z")));

        assertThat(body).contains("recurring");
        assertThat(body).contains("8 sessions");
    }

    @Test
    void shouldUseTheRecipientTimezoneStampedByDelivery() {
        // 18:00 UTC is 11:00 PM in Karachi and 2:00 PM in the practice zone (New York).
        String body = smsBodyGenerator.generate(
                NotificationEventCatalog.SESSION_REMINDER,
                true,
                Map.of("sessionDate", Instant.parse("2026-06-15T18:00:00Z"),
                        "recipientTimezone", "Asia/Karachi"));

        assertThat(body).contains("11:00 PM");
        assertThat(body).doesNotContain("2:00 PM");
    }

    @Test
    void shouldFallBackToPracticeTimezoneWhenRecipientZoneIsMissingOrUnusable() {
        String noZone = smsBodyGenerator.generate(
                NotificationEventCatalog.SESSION_REMINDER,
                true,
                Map.of("sessionDate", Instant.parse("2026-06-15T18:00:00Z")));

        String brokenZone = smsBodyGenerator.generate(
                NotificationEventCatalog.SESSION_REMINDER,
                true,
                Map.of("sessionDate", Instant.parse("2026-06-15T18:00:00Z"),
                        "recipientTimezone", "Not/AZone"));

        assertThat(noZone).contains("2:00 PM");
        assertThat(brokenZone).contains("2:00 PM");
    }

    @Test
    void shouldReturnNullForUnsupportedEvent() {
        assertThat(smsBodyGenerator.generate("task_assigned", false, Map.of())).isNull();
    }
}
