package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.session.dto.RoomResponse;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.RoomService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService available rooms exclude session")
class RoomAvailabilityExcludeSessionTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private SystemOptionResolverService systemOptionResolverService;
    @Mock
    private com.smart.therapy.flow.billing.repository.ServiceRepository serviceRepository;
    @Mock
    private TimezoneService timezoneService;

    @InjectMocks
    private RoomService roomService;

    @Test
    @DisplayName("Edit flow excludes current session so occupied room stays selectable")
    void excludesCurrentSessionFromRoomOccupancy() {
        Long therapistId = 42L;
        Long sessionId = 99L;
        Instant start = Instant.parse("2026-09-02T20:30:00Z");

        Room room = Room.builder()
                .roomNumber("1")
                .roomName("Therapy Room")
                .capacity(1)
                .isActive(true)
                .roomType(RoomType.PHYSICAL)
                .build();
        room.setId(7L);

        UserProfilePhysicalRoom assignment = UserProfilePhysicalRoom.builder()
                .room(room)
                .isPrimary(true)
                .build();
        UserProfile profile = UserProfile.builder()
                .availablePhysicalRooms(List.of(assignment))
                .build();

        when(systemOptionResolverService.requireOptionKey(
                eq(SystemOptionCategories.SESSION_MODE), eq("in-person")))
                .thenReturn("in_person");
        when(userProfileRepository.findByUserId(therapistId)).thenReturn(Optional.of(profile));
        when(sessionRepository.countOverlappingSessionsForRoomExcludingSession(
                eq(7L), eq(start), eq(start.plus(60, ChronoUnit.MINUTES)), eq(sessionId)))
                .thenReturn(0L);

        List<RoomResponse> rooms = roomService.getAvailableRoomsForSlot(
                therapistId, start, 60, null, "in-person", sessionId);

        assertThat(rooms).extracting(RoomResponse::getId).containsExactly(7L);
        verify(sessionRepository, never()).countOverlappingSessionsForRoom(any(), any(), any());
        verify(sessionRepository).countOverlappingSessionsForRoomExcludingSession(
                eq(7L), eq(start), eq(start.plus(60, ChronoUnit.MINUTES)), eq(sessionId));
    }

    @Test
    @DisplayName("Create flow keeps counting all overlapping sessions")
    void createFlowCountsAllOverlaps() {
        Long therapistId = 42L;
        Instant start = Instant.parse("2026-09-02T20:30:00Z");

        Room room = Room.builder()
                .roomNumber("1")
                .roomName("Therapy Room")
                .capacity(1)
                .isActive(true)
                .roomType(RoomType.PHYSICAL)
                .build();
        room.setId(7L);

        UserProfilePhysicalRoom assignment = UserProfilePhysicalRoom.builder()
                .room(room)
                .isPrimary(true)
                .build();
        UserProfile profile = UserProfile.builder()
                .availablePhysicalRooms(List.of(assignment))
                .build();

        when(systemOptionResolverService.requireOptionKey(
                eq(SystemOptionCategories.SESSION_MODE), eq("in-person")))
                .thenReturn("in_person");
        when(userProfileRepository.findByUserId(therapistId)).thenReturn(Optional.of(profile));
        when(sessionRepository.countOverlappingSessionsForRoom(
                eq(7L), eq(start), eq(start.plus(60, ChronoUnit.MINUTES))))
                .thenReturn(1L);

        List<RoomResponse> rooms = roomService.getAvailableRoomsForSlot(
                therapistId, start, 60, null, "in-person", null);

        assertThat(rooms).isEmpty();
        verify(sessionRepository).countOverlappingSessionsForRoom(
                eq(7L), eq(start), eq(start.plus(60, ChronoUnit.MINUTES)));
        verify(sessionRepository, never()).countOverlappingSessionsForRoomExcludingSession(
                any(), any(), any(), any());
    }
}
