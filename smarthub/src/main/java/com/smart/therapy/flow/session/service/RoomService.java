package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.session.dto.RoomRequest;
import com.smart.therapy.flow.session.dto.RoomResponse;
import com.smart.therapy.flow.session.dto.RoomAvailabilityResponse;
import com.smart.therapy.flow.session.dto.RoomSlotStatusResponse;
import com.smart.therapy.flow.session.dto.TimeSlot;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.session.repository.RoomBookingRepository;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfilePhysicalRoomRepository;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {

    private static final String RESOURCE_TYPE_ROOM = "room";

    private final RoomRepository roomRepository;
    private final SessionRepository sessionRepository;
    private final RoomBookingRepository roomBookingRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfilePhysicalRoomRepository userProfilePhysicalRoomRepository;
    private final com.smart.therapy.flow.billing.repository.ServiceRepository serviceRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final SystemOptionResolverService systemOptionResolverService;
    private final TimezoneService timezoneService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @Cacheable(value = "rooms", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('all:' + (#activeOnly != null ? #activeOnly : 'all'))")
    public List<RoomResponse> getRooms(Boolean activeOnly) {
        List<Room> rooms;
        if (Boolean.TRUE.equals(activeOnly)) {
            rooms = roomRepository.findByIsActive(true);
        } else {
            rooms = roomRepository.findAll();
        }
        return rooms.stream()
                .sorted(Comparator
                        .comparing(Room::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Room::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rooms", key = "T(com.smart.therapy.flow.common.cache.CacheKeyUtil).tenantKey('id:' + #roomId)")
    public RoomResponse getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return toRoomResponse(room);
    }

    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public RoomResponse createRoom(RoomRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can create rooms");

        // Validate room number format
        validateRoomNumber(request.getRoomNumber());

        // Check if room number already exists
        if (roomRepository.findByRoomNumber(request.getRoomNumber()).isPresent()) {
            throw new BadRequestException("Room number already exists");
        }

        // Check if room name already exists
        if (roomRepository.findByRoomName(request.getRoomName()).isPresent()) {
            throw new BadRequestException("Room name already exists");
        }

        Room room = Room.builder()
                .roomNumber(request.getRoomNumber().trim())
                .roomName(request.getRoomName().trim())
                .capacity(request.getCapacity())
                .equipment(request.getEquipment() != null ? request.getEquipment().trim() : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .roomType(request.getRoomType())
                .build();

        Room saved = roomRepository.save(room);
        Long savedId = saved.getId();

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "room_created", savedId, ipAddress);

        return toRoomResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public RoomResponse updateRoom(Long roomId, RoomRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(roomId, "Room ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can update rooms");

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Validate room number format if changed
        if (StringUtils.hasText(request.getRoomNumber()) && !request.getRoomNumber().equals(room.getRoomNumber())) {
            validateRoomNumber(request.getRoomNumber());
            // Check if new room number already exists
            roomRepository.findByRoomNumber(request.getRoomNumber())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(roomId)) {
                            throw new BadRequestException("Room number already exists");
                        }
                    });
        }

        // Check if room name already exists if changed
        if (StringUtils.hasText(request.getRoomName()) && !request.getRoomName().equals(room.getRoomName())) {
            roomRepository.findByRoomName(request.getRoomName())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(roomId)) {
                            throw new BadRequestException("Room name already exists");
                        }
                    });
        }

        // Update fields
        if (StringUtils.hasText(request.getRoomNumber())) {
            room.setRoomNumber(request.getRoomNumber().trim());
        }
        if (StringUtils.hasText(request.getRoomName())) {
            room.setRoomName(request.getRoomName().trim());
        }
        if (request.getCapacity() != null) {
            if (request.getCapacity() <= 0) {
                throw new BadRequestException("Capacity must be a positive number");
            }
            room.setCapacity(request.getCapacity());
        }
        if (request.getEquipment() != null) {
            room.setEquipment(request.getEquipment().trim());
        }
        if (request.getIsActive() != null) {
            room.setIsActive(request.getIsActive());
        }
        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }

        Room updated = roomRepository.save(room);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "room_updated", roomId, ipAddress);

        return toRoomResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public void deleteRoom(Long roomId, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(roomId, "Room ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        assertAdmin(requester, "Only administrators can delete rooms");

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        assertRoomCanBeDeleted(roomId);

        roomRepository.delete(room);
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), "room_deleted", roomId, ipAddress);
    }

    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, Instant startTime, Instant endTime) {
        Objects.requireNonNull(roomId, "Room ID is required");
        Objects.requireNonNull(startTime, "Start time is required");
        Objects.requireNonNull(endTime, "End time is required");

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!Boolean.TRUE.equals(room.getIsActive())) {
            return false;
        }

        // Check for overlapping sessions
        long overlappingSessions = sessionRepository.countOverlappingSessionsForRoom(
                roomId, startTime, endTime);

        return overlappingSessions == 0;
    }

    @Transactional(readOnly = true)
    public com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse checkDetailedAvailability(
            Long roomId, Instant startTime, Instant endTime, Long therapistId, Long clientId) {
        Objects.requireNonNull(startTime, "Start time is required");
        Objects.requireNonNull(endTime, "End time is required");

        java.util.List<com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.ConflictInfo> roomConflicts = new java.util.ArrayList<>();
        java.util.List<com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.ConflictInfo> therapistConflicts = new java.util.ArrayList<>();
        java.util.List<com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.ConflictInfo> clientConflicts = new java.util.ArrayList<>();

        // Check room conflicts if roomId is provided
        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

            if (!Boolean.TRUE.equals(room.getIsActive())) {
                return com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.builder()
                        .available(false)
                        .message("Room is inactive")
                        .build();
            }

            java.util.List<com.smart.therapy.flow.session.entity.Session> conflicts = sessionRepository
                    .findOverlappingRoomSessions(
                            roomId, startTime, endTime);
            roomConflicts = conflicts.stream()
                    .map(s -> toConflictInfo(s, "Room occupied"))
                    .collect(Collectors.toList());
        }

        // Check therapist conflicts if therapistId is provided
        if (therapistId != null) {
            java.util.List<com.smart.therapy.flow.session.entity.Session> conflicts = sessionRepository
                    .findOverlappingTherapistSessions(
                            therapistId, startTime, endTime);
            therapistConflicts = conflicts.stream()
                    .map(s -> toConflictInfo(s, "Therapist busy"))
                    .collect(Collectors.toList());
        }

        // Check client conflicts if clientId is provided
        if (clientId != null) {
            java.util.List<com.smart.therapy.flow.session.entity.Session> conflicts = sessionRepository
                    .findOverlappingClientSessions(
                            clientId, startTime, endTime);
            clientConflicts = conflicts.stream()
                    .map(s -> toConflictInfo(s, "Client busy"))
                    .collect(Collectors.toList());
        }

        boolean available = roomConflicts.isEmpty() && therapistConflicts.isEmpty() && clientConflicts.isEmpty();
        String message = available ? "Slot is available" : "Conflicts detected";

        return com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.builder()
                .available(available)
                .message(message)
                .roomConflicts(roomConflicts)
                .therapistConflicts(therapistConflicts)
                .clientConflicts(clientConflicts)
                .build();
    }

    private com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.ConflictInfo toConflictInfo(
            com.smart.therapy.flow.session.entity.Session session, String reason) {
        return com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse.ConflictInfo.builder()
                .sessionId(session.getId())
                .startTime(session.getSessionDate())
                .endTime(session.getSessionDate().plus(session.getDuration() != null ? session.getDuration() : 60,
                        java.time.temporal.ChronoUnit.MINUTES))
                .reason(reason)
                .build();
    }

    private RoomResponse toRoomResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomName(room.getRoomName())
                .capacity(room.getCapacity())
                .equipment(room.getEquipment())
                .isActive(room.getIsActive())
                .roomType(room.getRoomType())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private void validateRoomNumber(String roomNumber) {
        if (!StringUtils.hasText(roomNumber)) {
            throw new BadRequestException("Room number is required");
        }
        if (roomNumber.length() > 50) {
            throw new BadRequestException("Room number cannot exceed 50 characters");
        }
        // Allow alphanumeric, spaces, hyphens, underscores
        if (!roomNumber.matches("^[a-zA-Z0-9\\s_-]+$")) {
            throw new BadRequestException(
                    "Room number must contain only letters, numbers, spaces, hyphens, and underscores");
        }
    }

    private void recordAuditEvent(Long actorId, String action, Long resourceId, String ipAddress) {
        try {
            auditLogService.recordStaffEvent(actorId, action, RESOURCE_TYPE_ROOM, resourceId, null, ipAddress, false);
        } catch (Exception e) {
            log.error("Failed to record audit event for room: {}", resourceId, e);
        }
    }

    private void assertRoomCanBeDeleted(Long roomId) {
        long activeSessionCount = sessionRepository.countByRoomId(roomId);
        long archivedSessionCount = sessionRepository.countDeletedSessionsByRoomId(roomId);
        long physicalRoomAssignmentCount = userProfilePhysicalRoomRepository.countByRoomId(roomId);
        long virtualRoomAssignmentCount = userProfileRepository.countByVirtualRoomId(roomId);
        long bookingCount = roomBookingRepository.countByRoomId(roomId);

        List<String> blockers = new ArrayList<>();
        if (activeSessionCount > 0) {
            blockers.add(activeSessionCount + " active session(s), including recurring sessions");
        }
        if (archivedSessionCount > 0) {
            blockers.add(archivedSessionCount + " archived session(s)");
        }
        if (physicalRoomAssignmentCount > 0) {
            blockers.add(physicalRoomAssignmentCount + " therapist physical room assignment(s)");
        }
        if (virtualRoomAssignmentCount > 0) {
            blockers.add(virtualRoomAssignmentCount + " therapist virtual room assignment(s)");
        }
        if (bookingCount > 0) {
            blockers.add(bookingCount + " room booking(s)");
        }

        if (blockers.isEmpty()) {
            return;
        }

        throw new BadRequestException(
                "Cannot delete room: it is still linked to "
                        + String.join(", ", blockers)
                        + ". Remove or reassign these records before deleting the room.");
    }

    /**
     * Check if user has ROOM_MANAGE permission (PBAC).
     * This replaces the old role-based admin check.
     */
    private void assertAdmin(AuthPrincipal principal, String message) {
        if (principal == null || !permissionChecker.hasPermission(principal, "ROOM_MANAGE")) {
            throw new ForbiddenException(message);
        }
    }

    @Transactional(readOnly = true)
    public RoomAvailabilityResponse getPublicRoomsAvailability(LocalDate date) {
        Objects.requireNonNull(date, "Date is required");

        // Get all active rooms
        List<Room> activeRooms = roomRepository.findByIsActive(true);

        // Resolve the requested practice-local day to a half-open UTC instant range.
        ZoneId practiceZone = timezoneService.getPracticeTimezone();
        Instant dayStart = date.atStartOfDay(practiceZone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(practiceZone).toInstant();

        // Get all sessions for this day
        List<Session> daySessions = sessionRepository.findAll().stream()
                .filter(s -> s.getSessionDate() != null &&
                        s.getSessionDate().isAfter(dayStart) &&
                        s.getSessionDate().isBefore(dayEnd) &&
                        (s.getStatus() == null || !SystemOptionKeyMatcher.matchesAny(s.getStatus(), "cancelled")))
                .collect(Collectors.toList());

        List<RoomAvailabilityResponse.RoomAvailabilityInfo> roomInfos = new ArrayList<>();

        for (Room room : activeRooms) {
            // Get sessions for this room on this day
            List<Session> roomSessions = daySessions.stream()
                    .filter(s -> s.getRoom() != null && s.getRoom().getId().equals(room.getId()))
                    .collect(Collectors.toList());

            // Generate time slots (8 AM to 8 PM, 30-minute intervals)
            List<TimeSlot> availableSlots = new ArrayList<>();
            List<TimeSlot> bookedSlots = new ArrayList<>();

            for (int hour = 8; hour < 20; hour++) {
                for (int minute = 0; minute < 60; minute += 30) {
                    LocalTime slotTime = LocalTime.of(hour, minute);
                    Instant slotStart = date.atTime(slotTime).atZone(practiceZone).toInstant();
                    Instant slotEnd = slotStart.plus(60, ChronoUnit.MINUTES);

                    // Check if this slot is booked
                    boolean isBooked = roomSessions.stream().anyMatch(session -> {
                        Instant sessionStart = session.getSessionDate();
                        int duration = session.getDuration() != null ? session.getDuration() : 60;
                        Instant sessionEnd = sessionStart.plus(duration, ChronoUnit.MINUTES);
                        return slotStart.isBefore(sessionEnd) && slotEnd.isAfter(sessionStart);
                    });

                    TimeSlot timeSlot = TimeSlot.builder()
                            .time(slotStart)
                            .datetime(slotStart)
                            .available(!isBooked)
                            .therapistBusy(false)
                            .roomBusy(isBooked)
                            .build();

                    if (isBooked) {
                        bookedSlots.add(timeSlot);
                    } else {
                        availableSlots.add(timeSlot);
                    }
                }
            }

            RoomAvailabilityResponse.RoomAvailabilityInfo roomInfo = RoomAvailabilityResponse.RoomAvailabilityInfo.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomName(room.getRoomName())
                    .available(!availableSlots.isEmpty())
                    .availableSlots(availableSlots)
                    .bookedSlots(bookedSlots)
                    .build();

            roomInfos.add(roomInfo);
        }

        return RoomAvailabilityResponse.builder()
                .date(date)
                .rooms(roomInfos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsForSlot(
            Long therapistId,
            Instant sessionDate,
            Integer duration,
            Long serviceId,
            String sessionTypeRaw) {
        return getAvailableRoomsForSlot(therapistId, sessionDate, duration, serviceId, sessionTypeRaw, null);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsForSlot(
            Long therapistId,
            Instant sessionDate,
            Integer duration,
            Long serviceId,
            String sessionTypeRaw,
            Long excludeSessionId) {
        Objects.requireNonNull(therapistId, "Therapist ID is required");
        Objects.requireNonNull(sessionDate, "Session date is required");

        String sessionMode;
        try {
            sessionMode = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.SESSION_MODE, sessionTypeRaw);
        } catch (BadRequestException ex) {
            throw new BadRequestException("Invalid sessionType. Use 'online' or 'in-person'.");
        }

        int effectiveDuration = resolveDuration(duration, serviceId);
        Instant sessionEnd = sessionDate.plus(effectiveDuration, ChronoUnit.MINUTES);

        UserProfile profile = userProfileRepository.findByUserId(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist profile not found"));

        List<Room> candidateRooms = new ArrayList<>();
        if (SystemOptionKeyMatcher.matchesAny(sessionMode, "online")) {
            if (profile.getVirtualRoom() != null) {
                candidateRooms.add(profile.getVirtualRoom());
            }
        } else {
            if (profile.getAvailablePhysicalRooms() != null) {
                candidateRooms = profile.getAvailablePhysicalRooms().stream()
                        .map(pr -> pr.getRoom())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }

        List<Room> availableRooms = candidateRooms.stream()
                .filter(room -> matchesSessionTypeRoomType(room, sessionMode))
                .filter(room -> Boolean.TRUE.equals(room.getIsActive()))
                .filter(room -> {
                    long overlaps = excludeSessionId == null
                            ? sessionRepository.countOverlappingSessionsForRoom(
                                    room.getId(), sessionDate, sessionEnd)
                            : sessionRepository.countOverlappingSessionsForRoomExcludingSession(
                                    room.getId(), sessionDate, sessionEnd, excludeSessionId);
                    int roomCapacity = (room.getCapacity() != null && room.getCapacity() > 0) ? room.getCapacity() : 1;
                    return overlaps < roomCapacity;
                })
                .collect(Collectors.toList());

        return availableRooms.stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomSlotStatusResponse getRoomSlotStatus(
            Long roomId,
            Instant sessionDate,
            Integer duration,
            Long serviceId,
            String sessionTypeRaw) {
        Objects.requireNonNull(roomId, "Room ID is required");
        Objects.requireNonNull(sessionDate, "Session date is required");

        String sessionMode;
        try {
            sessionMode = systemOptionResolverService.requireOptionKey(
                    SystemOptionCategories.SESSION_MODE, sessionTypeRaw);
        } catch (BadRequestException ex) {
            throw new BadRequestException("Invalid sessionType. Use 'online' or 'in-person'.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!matchesSessionTypeRoomType(room, sessionMode)) {
            throw new BadRequestException(SystemOptionKeyMatcher.matchesAny(sessionMode, "online")
                    ? "Online sessions require a virtual room"
                    : "In-person sessions require a physical room");
        }

        int effectiveDuration = resolveDuration(duration, serviceId);
        Instant requestedEnd = sessionDate.plus(effectiveDuration, ChronoUnit.MINUTES);
        int roomCapacity = (room.getCapacity() != null && room.getCapacity() > 0) ? room.getCapacity() : 1;

        long activeBookings = sessionRepository.countOverlappingSessionsForRoom(roomId, sessionDate, requestedEnd);
        boolean isActive = Boolean.TRUE.equals(room.getIsActive());
        boolean isFull = activeBookings >= roomCapacity;
        boolean isAvailable = isActive && !isFull;
        int remainingCapacity = Math.max(0, roomCapacity - (int) activeBookings);

        Instant nextAvailableTime = isAvailable
                ? sessionDate
                : findNextAvailableTime(roomId, sessionDate, effectiveDuration, roomCapacity, isActive);

        String message;
        if (!isActive) {
            message = "Room is inactive";
        } else if (isFull) {
            message = "Room is full for the selected date/time. Please select another room.";
        } else {
            message = "Room is available for the selected date/time";
        }

        return RoomSlotStatusResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomName(room.getRoomName())
                .active(isActive)
                .roomType(room.getRoomType() != null ? room.getRoomType().name() : null)
                .capacity(roomCapacity)
                .activeBookingsAtSlot((int) activeBookings)
                .remainingCapacity(remainingCapacity)
                .full(isFull)
                .available(isAvailable)
                .requestedStartTime(sessionDate)
                .requestedEndTime(requestedEnd)
                .nextAvailableTime(nextAvailableTime)
                .message(message)
                .build();
    }

    private int resolveDuration(Integer duration, Long serviceId) {
        if (duration != null && duration > 0) {
            return duration;
        }
        if (serviceId != null) {
            com.smart.therapy.flow.billing.entity.Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
            if (service.getDuration() != null && service.getDuration() > 0) {
                return service.getDuration();
            }
        }
        return 60;
    }

    private Instant findNextAvailableTime(
            Long roomId,
            Instant requestedStart,
            int durationMinutes,
            int roomCapacity,
            boolean roomActive) {
        if (!roomActive) {
            return null;
        }

        // Search forward in 15-minute increments for up to 14 days.
        for (int i = 1; i <= 14 * 24 * 4; i++) {
            Instant probeStart = requestedStart.plus(i * 15L, ChronoUnit.MINUTES);
            Instant probeEnd = probeStart.plus(durationMinutes, ChronoUnit.MINUTES);
            long overlaps = sessionRepository.countOverlappingSessionsForRoom(roomId, probeStart, probeEnd);
            if (overlaps < roomCapacity) {
                return probeStart;
            }
        }
        return null;
    }

    private boolean matchesSessionTypeRoomType(Room room, String sessionMode) {
        if (room == null || !org.springframework.util.StringUtils.hasText(sessionMode)) {
            return false;
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionMode, "online")) {
            return room.getRoomType() == RoomType.VIRTUAL;
        }
        if (SystemOptionKeyMatcher.matchesAny(sessionMode, "in-person")) {
            return room.getRoomType() == RoomType.PHYSICAL;
        }
        return false;
    }

}
