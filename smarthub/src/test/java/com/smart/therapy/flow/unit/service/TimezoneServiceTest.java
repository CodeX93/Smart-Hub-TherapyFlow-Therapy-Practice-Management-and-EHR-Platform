package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.system.entity.PracticeConfiguration;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.system.repository.PracticeConfigurationRepository;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimezoneService Unit Tests")
class TimezoneServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PracticeConfigurationRepository practiceConfigurationRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private TimezoneService timezoneService;

    private UserProfile userProfile;
    private Client client;

    @BeforeEach
    void setUp() {
        userProfile = UserProfile.builder()
                .timezone("America/Los_Angeles")
                .build();

        client = TestDataFactory.createTestClient();
        client.setId(1L);
        client.setTimezone("America/Chicago");
    }

    @Test
    @DisplayName("Should get therapist timezone from profile")
    void shouldGetTherapistTimezoneFromProfile() {
        // Arrange
        Long therapistId = 1L;
        when(userProfileRepository.findByUserId(therapistId)).thenReturn(Optional.of(userProfile));

        // Act
        java.util.Optional<ZoneId> timezoneOpt = timezoneService.getTherapistTimezone(therapistId);

        // Assert
        assertThat(timezoneOpt).isPresent();
        assertThat(timezoneOpt.get().getId()).isEqualTo("America/Los_Angeles");
        verify(userProfileRepository).findByUserId(therapistId);
    }

    @Test
    @DisplayName("Should fall back to practice settings timezone when therapist profile not found")
    void shouldReturnEmptyWhenProfileNotFound() {
        // Arrange
        Long therapistId = 999L;
        when(userProfileRepository.findByUserId(therapistId)).thenReturn(Optional.empty());
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(PracticeConfiguration.builder().timezone("Asia/Karachi").build()));

        // Act
        java.util.Optional<ZoneId> timezoneOpt = timezoneService.getTherapistTimezone(therapistId);

        // Assert
        assertThat(timezoneOpt).isPresent();
        assertThat(timezoneOpt.get().getId()).isEqualTo("Asia/Karachi");
        verify(userProfileRepository).findByUserId(therapistId);
    }

    @Test
    @DisplayName("Should fall back to practice settings timezone when profile timezone is null")
    void shouldReturnEmptyWhenProfileTimezoneIsNull() {
        // Arrange
        Long therapistId = 1L;
        userProfile.setTimezone(null);
        when(userProfileRepository.findByUserId(therapistId)).thenReturn(Optional.of(userProfile));
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(PracticeConfiguration.builder().timezone("Asia/Karachi").build()));

        // Act
        java.util.Optional<ZoneId> timezoneOpt = timezoneService.getTherapistTimezone(therapistId);

        // Assert
        assertThat(timezoneOpt).isPresent();
        assertThat(timezoneOpt.get().getId()).isEqualTo("Asia/Karachi");
    }

    @Test
    @DisplayName("Should resolve the Administration timezone as the practice business timezone")
    void shouldResolvePracticeTimezone() {
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(PracticeConfiguration.builder().timezone("America/Toronto").build()));

        assertThat(timezoneService.getPracticeTimezone()).isEqualTo(ZoneId.of("America/Toronto"));
    }

    @Test
    @DisplayName("Should use UTC instead of the server timezone when practice timezone is missing")
    void shouldFallbackPracticeTimezoneToUtc() {
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThat(timezoneService.getPracticeTimezone()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Should get client timezone from client entity")
    void shouldGetClientTimezoneFromClientEntity() {
        // Arrange
        Long clientId = 1L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act
        java.util.Optional<ZoneId> timezoneOpt = timezoneService.getClientTimezone(clientId);

        // Assert
        assertThat(timezoneOpt).isPresent();
        assertThat(timezoneOpt.get().getId()).isEqualTo("America/Chicago");
        verify(clientRepository).findById(clientId);
    }

    @Test
    @DisplayName("Should return practice timezone fallback when client not found")
    void shouldReturnDefaultTimezoneWhenClientNotFound() {
        // Arrange
        Long clientId = 999L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        // Act
        java.util.Optional<ZoneId> timezoneOpt = timezoneService.getClientTimezone(clientId);

        // Assert
        assertThat(timezoneOpt).isPresent();
        assertThat(timezoneOpt.get().getId()).isEqualTo("UTC");
        verify(clientRepository).findById(clientId);
    }

    @Test
    @DisplayName("Should resolve client portal zone from profile when param omitted")
    void shouldResolveClientPortalZoneFromProfile() {
        Long clientId = 1L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        ZoneId zone = timezoneService.resolveClientPortalZone(clientId, null);

        assertThat(zone.getId()).isEqualTo("America/Chicago");
    }

    @Test
    @DisplayName("Should resolve client portal zone from explicit param")
    void shouldResolveClientPortalZoneFromParam() {
        ZoneId zone = timezoneService.resolveClientPortalZone(1L, "Asia/Karachi");

        assertThat(zone.getId()).isEqualTo("Asia/Karachi");
    }

    @Test
    @DisplayName("Should use the organisation timezone when no practice configuration exists")
    void shouldUseOrganisationTimezoneWhenPracticeConfigurationIsMissing() {
        // Onboarding writes the clinic timezone here; the practice configuration row is
        // only created when someone opens the Administration screen.
        com.smart.therapy.flow.common.tenant.TenantContext.setOrganisationId(42L);
        try {
            when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
            when(organisationRepository.findById(42L)).thenReturn(Optional.of(
                    Organisation.builder().timezone("America/Toronto").build()));

            assertThat(timezoneService.getPracticeTimezone()).isEqualTo(ZoneId.of("America/Toronto"));
        } finally {
            com.smart.therapy.flow.common.tenant.TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should prefer the practice configuration over the organisation timezone")
    void shouldPreferPracticeConfigurationOverOrganisation() {
        com.smart.therapy.flow.common.tenant.TenantContext.setOrganisationId(42L);
        try {
            when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(
                    Optional.of(PracticeConfiguration.builder().timezone("Asia/Karachi").build()));

            assertThat(timezoneService.getPracticeTimezone()).isEqualTo(ZoneId.of("Asia/Karachi"));
            verify(organisationRepository, never()).findById(any());
        } finally {
            com.smart.therapy.flow.common.tenant.TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should fall back to UTC when the organisation has no timezone either")
    void shouldFallBackToUtcWhenOrganisationTimezoneIsBlank() {
        com.smart.therapy.flow.common.tenant.TenantContext.setOrganisationId(42L);
        try {
            when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
            when(organisationRepository.findById(42L)).thenReturn(Optional.of(
                    Organisation.builder().timezone("  ").build()));

            assertThat(timezoneService.getPracticeTimezone()).isEqualTo(ZoneId.of("UTC"));
        } finally {
            com.smart.therapy.flow.common.tenant.TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should fall back to UTC outside any tenant context")
    void shouldFallBackToUtcWithoutTenantContext() {
        com.smart.therapy.flow.common.tenant.TenantContext.clear();
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThat(timezoneService.getPracticeTimezone()).isEqualTo(ZoneId.of("UTC"));
        verify(organisationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should default an unset client timezone to the practice timezone, not UTC")
    void shouldDefaultUnsetClientTimezoneToPracticeTimezone() {
        Long clientId = 1L;
        client.setTimezone(null);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(
                Optional.of(PracticeConfiguration.builder().timezone("America/Toronto").build()));

        ZoneId zone = timezoneService.resolveClientPortalZone(clientId, null);

        assertThat(zone).isEqualTo(ZoneId.of("America/Toronto"));
        assertThat(zone).isNotEqualTo(ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Should default a blank client timezone to the practice timezone")
    void shouldDefaultBlankClientTimezoneToPracticeTimezone() {
        Long clientId = 1L;
        client.setTimezone("   ");
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(
                Optional.of(PracticeConfiguration.builder().timezone("Asia/Karachi").build()));

        assertThat(timezoneService.resolveClientPortalZone(clientId, null))
                .isEqualTo(ZoneId.of("Asia/Karachi"));
    }

    @Test
    @DisplayName("Should fall back to the practice timezone when the client row is missing")
    void shouldFallBackToPracticeTimezoneWhenClientMissing() {
        when(clientRepository.findById(404L)).thenReturn(Optional.empty());
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(
                Optional.of(PracticeConfiguration.builder().timezone("Europe/London").build()));

        assertThat(timezoneService.resolveClientPortalZone(404L, null))
                .isEqualTo(ZoneId.of("Europe/London"));
    }

    @Test
    @DisplayName("Should reject a null client id")
    void shouldRejectNullClientIdForPortalZone() {
        assertThatThrownBy(() -> timezoneService.resolveClientPortalZone(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Client ID is required");
    }

    @Test
    @DisplayName("Should reject an unparseable explicit timezone param")
    void shouldRejectUnparseableTimezoneParam() {
        assertThatThrownBy(() -> timezoneService.resolveClientPortalZone(1L, "Not/AZone"))
                .isInstanceOf(java.time.zone.ZoneRulesException.class);
    }

    @Test
    @DisplayName("Should fall back to UTC only when neither client nor practice timezone is set")
    void shouldFallBackToUtcWhenProfileTimezoneUnset() {
        Long clientId = 1L;
        client.setTimezone(null);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        ZoneId zone = timezoneService.resolveClientPortalZone(clientId, null);

        assertThat(zone).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Should convert LocalDateTime to UTC Instant")
    void shouldConvertLocalDateTimeToUtcInstant() {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.of(2024, 12, 9, 14, 30);
        ZoneId timezone = ZoneId.of("America/New_York");

        // Act
        Instant instant = timezoneService.convertToUtc(localDateTime, timezone);

        // Assert
        assertThat(instant).isNotNull();
        assertThat(instant).isAfter(Instant.EPOCH);
    }

    @Test
    @DisplayName("Should convert UTC Instant to LocalDateTime")
    void shouldConvertUtcInstantToLocalDateTime() {
        // Arrange
        Instant utcTime = Instant.now();
        ZoneId timezone = ZoneId.of("America/New_York");

        // Act
        LocalDateTime localDateTime = timezoneService.convertFromUtc(utcTime, timezone);

        // Assert
        assertThat(localDateTime).isNotNull();
    }

    @Test
    @DisplayName("Should convert between timezones")
    void shouldConvertBetweenTimezones() {
        // Arrange
        LocalDateTime sourceTime = LocalDateTime.of(2024, 12, 9, 14, 30);
        ZoneId sourceTimezone = ZoneId.of("America/New_York");
        ZoneId targetTimezone = ZoneId.of("America/Los_Angeles");

        // Act
        LocalDateTime targetTime = timezoneService.convertBetweenTimezones(sourceTime, sourceTimezone, targetTimezone);

        // Assert
        assertThat(targetTime).isNotNull();
        // LA is 3 hours behind NY, so 14:30 NY = 11:30 LA
        assertThat(targetTime.getHour()).isEqualTo(11);
    }

    @Test
    @DisplayName("Should validate timezone string")
    void shouldValidateTimezoneString() {
        // Act & Assert
        assertThat(timezoneService.isValidTimezone("America/New_York")).isTrue();
        assertThat(timezoneService.isValidTimezone("Europe/London")).isTrue();
        assertThat(timezoneService.isValidTimezone("Invalid/Timezone")).isFalse();
        assertThat(timezoneService.isValidTimezone(null)).isFalse();
        assertThat(timezoneService.isValidTimezone("")).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when therapist ID is null")
    void shouldThrowExceptionWhenTherapistIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> timezoneService.getTherapistTimezone(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Therapist ID is required");
    }

    @Test
    @DisplayName("Should throw exception when client ID is null")
    void shouldThrowExceptionWhenClientIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> timezoneService.getClientTimezone(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Client ID is required");
    }

    @Test
    @DisplayName("Should format session datetime in practice timezone with zone id")
    void shouldFormatSessionDateTimeInPracticeTimezone() {
        PracticeConfiguration config = PracticeConfiguration.builder()
                .timezone("America/Toronto")
                .build();
        when(practiceConfigurationRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));

        Instant utc = Instant.parse("2026-09-17T18:00:00Z");

        assertThat(timezoneService.formatSessionDateTimeForPractice(utc))
                .isEqualTo("Sep 17, 2026 2:00 PM EDT (America/Toronto)");
        assertThat(timezoneService.formatSessionDateOnlyForPractice(utc))
                .isEqualTo("Thursday, Sep 17, 2026");
        assertThat(timezoneService.formatSessionTimeRangeForPractice(utc, 45))
                .isEqualTo("2:00 PM - 2:45 PM EDT (America/Toronto)");
    }
}
