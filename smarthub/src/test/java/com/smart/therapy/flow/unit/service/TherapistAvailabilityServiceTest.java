package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.user.dto.CreateTherapistBlockedTimeRequest;
import com.smart.therapy.flow.user.dto.TherapistBlockedTimeResponse;
import com.smart.therapy.flow.user.entity.TherapistBlockedTime;
import com.smart.therapy.flow.user.repository.TherapistBlockedTimeRepository;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TherapistAvailabilityService Unit Tests")
class TherapistAvailabilityServiceTest {
    private User admin;

    @Mock
    private TherapistBlockedTimeRepository blockedTimeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimezoneService timezoneService;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @InjectMocks
    private TherapistAvailabilityService therapistAvailabilityService;

    private AuthPrincipal therapistPrincipal;
    private AuthPrincipal adminPrincipal;
    private User therapist;
    private TherapistBlockedTime blockedTime;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST", "CLIENT_VIEW_OWN");

        admin = TestDataFactory.createTestAdmin();
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "CLIENT_VIEW_ALL");

        blockedTime = TherapistBlockedTime.builder()
                .therapist(therapist)
                .startTime(Instant.now().plusSeconds(3600))
                .endTime(Instant.now().plusSeconds(7200))
                .isActive(true)
                .build();
        blockedTime.setId(1L);
    }

    @Test
    @DisplayName("Should get therapist blocked times successfully")
    void shouldGetTherapistBlockedTimesSuccessfully() {
        // Arrange
        Long therapistId = 1L;
        when(blockedTimeRepository.findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(therapistId))
                .thenReturn(List.of(blockedTime));

        // Act
        List<TherapistBlockedTimeResponse> blockedTimes = therapistAvailabilityService.getTherapistBlockedTimes(therapistId);

        // Assert
        assertThat(blockedTimes).isNotNull();
        assertThat(blockedTimes).hasSize(1);
        verify(blockedTimeRepository).findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(therapistId);
    }

    @Test
    @DisplayName("Should get blocked time by ID successfully")
    void shouldGetBlockedTimeByIdSuccessfully() {
        // Arrange
        Long blockedTimeId = 1L;
        when(blockedTimeRepository.findById(blockedTimeId)).thenReturn(Optional.of(blockedTime));

        // Act
        TherapistBlockedTimeResponse response = therapistAvailabilityService.getBlockedTime(blockedTimeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(blockedTimeId);
        verify(blockedTimeRepository).findById(blockedTimeId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when blocked time not found")
    void shouldThrowExceptionWhenBlockedTimeNotFound() {
        // Arrange
        Long blockedTimeId = 999L;
        when(blockedTimeRepository.findById(blockedTimeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> therapistAvailabilityService.getBlockedTime(blockedTimeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Therapist blocked time not found");

        verify(blockedTimeRepository).findById(blockedTimeId);
    }

    @Test
    @DisplayName("Should create blocked time successfully when therapist creates for themselves")
    void shouldCreateBlockedTimeSuccessfullyWhenTherapistCreatesForThemselves() {
        when(currentUserService.getCurrentUserId(therapistPrincipal)).thenReturn(therapist.getId());
        // Arrange
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setTherapistId(1L);
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));
        request.setIsActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
        when(blockedTimeRepository.save(any(TherapistBlockedTime.class))).thenReturn(blockedTime);

        // Act
        TherapistBlockedTimeResponse response = therapistAvailabilityService.createBlockedTime(request, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(userRepository).findById(1L);
        verify(blockedTimeRepository).save(any(TherapistBlockedTime.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when therapist tries to create for another therapist")
    void shouldThrowExceptionWhenTherapistCreatesForAnother() {
        // Arrange
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setTherapistId(999L); // Different therapist ID
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));

        // Act & Assert
        assertThatThrownBy(() -> therapistAvailabilityService.createBlockedTime(request, therapistPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only create blocked times for yourself");

        verify(blockedTimeRepository, never()).save(any(TherapistBlockedTime.class));
    }

    @Test
    @DisplayName("Should allow admin to create blocked time for any therapist")
    void shouldAllowAdminToCreateBlockedTimeForAnyTherapist() {
        // Arrange
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setTherapistId(1L);
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));

        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
        when(blockedTimeRepository.save(any(TherapistBlockedTime.class))).thenReturn(blockedTime);

        // Act
        TherapistBlockedTimeResponse response = therapistAvailabilityService.createBlockedTime(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(blockedTimeRepository).save(any(TherapistBlockedTime.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when start time is after end time")
    void shouldThrowExceptionWhenStartTimeAfterEndTime() {
        when(currentUserService.getCurrentUserId(therapistPrincipal)).thenReturn(therapist.getId());
        // Arrange
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setTherapistId(1L);
        request.setStartTime(Instant.now().plusSeconds(7200));
        request.setEndTime(Instant.now().plusSeconds(3600)); // End before start

        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));

        // Act & Assert
        assertThatThrownBy(() -> therapistAvailabilityService.createBlockedTime(request, therapistPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start time must be before end time");

        verify(blockedTimeRepository, never()).save(any(TherapistBlockedTime.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when therapist not found")
    void shouldThrowExceptionWhenTherapistNotFound() {
        // Arrange
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setTherapistId(999L);
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> therapistAvailabilityService.createBlockedTime(request, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Therapist not found");

        verify(blockedTimeRepository, never()).save(any(TherapistBlockedTime.class));
    }

    @Test
    @DisplayName("Should update blocked time successfully")
    void shouldUpdateBlockedTimeSuccessfully() {
        when(currentUserService.getCurrentUserId(therapistPrincipal)).thenReturn(therapist.getId());
        // Arrange
        Long blockedTimeId = 1L;
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setStartTime(Instant.now().plusSeconds(5400));
        request.setEndTime(Instant.now().plusSeconds(9000));

        when(blockedTimeRepository.findById(blockedTimeId)).thenReturn(Optional.of(blockedTime));
        when(blockedTimeRepository.save(any(TherapistBlockedTime.class))).thenReturn(blockedTime);

        // Act
        TherapistBlockedTimeResponse response = therapistAvailabilityService.updateBlockedTime(blockedTimeId, request, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(blockedTimeRepository).findById(blockedTimeId);
        verify(blockedTimeRepository).save(any(TherapistBlockedTime.class));
    }

    @Test
    @DisplayName("Should delete blocked time successfully")
    void shouldDeleteBlockedTimeSuccessfully() {
        when(currentUserService.getCurrentUserId(therapistPrincipal)).thenReturn(therapist.getId());
        // Arrange
        Long blockedTimeId = 1L;
        when(blockedTimeRepository.findById(blockedTimeId)).thenReturn(Optional.of(blockedTime));
        doNothing().when(blockedTimeRepository).delete(blockedTime);

        // Act
        therapistAvailabilityService.deleteBlockedTime(blockedTimeId, therapistPrincipal);

        // Assert
        verify(blockedTimeRepository).findById(blockedTimeId);
        verify(blockedTimeRepository).delete(blockedTime);
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> therapistAvailabilityService.createBlockedTime(null, therapistPrincipal))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Request is required");
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateTherapistBlockedTimeRequest request = new CreateTherapistBlockedTimeRequest();
        request.setTherapistId(1L);

        // Act & Assert
        assertThatThrownBy(() -> therapistAvailabilityService.createBlockedTime(request, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }
    private void stubBookingWindow() {
        var zone = java.time.ZoneOffset.UTC;
        var date = java.time.LocalDate.of(2026, 9, 14);
        when(timezoneService.getTherapistTimezone(1L)).thenReturn(Optional.of(zone));
        when(timezoneService.getStartOfDay(date, zone)).thenReturn(date.atStartOfDay(zone).toInstant());
        when(timezoneService.getEndOfDay(date, zone)).thenReturn(date.plusDays(1).atStartOfDay(zone).toInstant());
        var profile = com.smart.therapy.flow.user.entity.UserProfile.builder().user(therapist).build();
        profile.getWorkingHours().add(com.smart.therapy.flow.user.entity.UserProfileWorkingHours.builder()
                .userProfile(profile).day("MONDAY").startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(17, 0)).build());
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(blockedTimeRepository.findByTherapistIdAndIsActiveTrueOrderByStartTimeAsc(1L)).thenReturn(List.of());
    }

    private com.smart.therapy.flow.session.entity.Session overlappingSession(long id) {
        var session = com.smart.therapy.flow.session.entity.Session.builder()
                .sessionDate(Instant.parse("2026-09-14T12:00:00Z")).duration(60).status("scheduled").build();
        session.setId(id);
        return session;
    }

    @Test
    void shouldExcludeOnlyTheSessionBeingEditedFromAvailabilityConflicts() {
        stubBookingWindow();
        var session = overlappingSession(10L);
        when(sessionRepository.findByTherapistAndDateRange(eq(1L), any(), any())).thenReturn(List.of(session));
        therapistAvailabilityService.assertTherapistAvailableForBooking(
                1L, session.getSessionDate(), 60, "in_person", null, 10L);
    }

    @Test
    void shouldStillRejectAnotherSessionWhenEditing() {
        stubBookingWindow();
        var session = overlappingSession(10L);
        when(sessionRepository.findByTherapistAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of(session, overlappingSession(11L)));
        assertThatThrownBy(() -> therapistAvailabilityService.assertTherapistAvailableForBooking(
                1L, session.getSessionDate(), 60, "in_person", null, 10L))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("no longer available");
    }

    @Test
    void shouldKeepAllSessionConflictsForNewBookings() {
        stubBookingWindow();
        var session = overlappingSession(10L);
        when(sessionRepository.findByTherapistAndDateRange(eq(1L), any(), any())).thenReturn(List.of(session));
        assertThatThrownBy(() -> therapistAvailabilityService.assertTherapistAvailableForBooking(
                1L, session.getSessionDate(), 60, "in_person"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("no longer available");
    }

}
