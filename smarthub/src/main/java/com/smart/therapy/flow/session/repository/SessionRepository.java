package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface SessionRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {
  // All queries automatically filter out soft-deleted records (isDeleted = false)
  @Query("SELECT s FROM Session s WHERE s.client.id = :clientId AND s.isDeleted = false")
  List<Session> findByClientId(@Param("clientId") Long clientId);

  @Query("SELECT s.id FROM Session s WHERE s.client.id = :clientId AND s.isDeleted = false")
  List<Long> findIdsByClientId(@Param("clientId") Long clientId);

  @Query("SELECT s FROM Session s WHERE s.client.id = :clientId AND s.isDeleted = false")
  Page<Session> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

  @Query("""
      SELECT s FROM Session s
      WHERE s.client.id = :clientId
        AND s.isDeleted = false
        AND LOWER(s.status) IN :statuses
      """)
  Page<Session> findByClientIdAndStatusInIgnoreCase(
      @Param("clientId") Long clientId,
      @Param("statuses") Collection<String> statuses,
      Pageable pageable);

  @Query("SELECT s.client.id FROM Session s WHERE s.id = :sessionId AND s.isDeleted = false")
  Optional<Long> findClientIdBySessionId(@Param("sessionId") Long sessionId);

  @Query("SELECT s FROM Session s WHERE s.therapist.id = :therapistId AND s.isDeleted = false")
  List<Session> findByTherapistId(@Param("therapistId") Long therapistId);

  @Query("SELECT s FROM Session s WHERE s.status = :status AND s.isDeleted = false")
  List<Session> findByStatus(@Param("status") String status);

  @Query(value = """
      SELECT s.*
      FROM sessions s
      JOIN clients c ON c.id = s.client_id
      WHERE s.therapist_id = :therapistId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND c.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      ORDER BY s.session_date ASC
      """, nativeQuery = true)
  List<Session> findConflictingTherapistSessions(
      @Param("therapistId") Long therapistId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  @Query(value = """
      SELECT s.*
      FROM sessions s
      JOIN clients c ON c.id = s.client_id
      WHERE s.room_id = :roomId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND c.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      ORDER BY s.session_date ASC
      """, nativeQuery = true)
  List<Session> findConflictingRoomSessions(
      @Param("roomId") Long roomId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  @Query(value = """
      SELECT s.*
      FROM sessions s
      JOIN clients c ON c.id = s.client_id
      WHERE s.client_id = :clientId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND c.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      ORDER BY s.session_date ASC
      """, nativeQuery = true)
  List<Session> findConflictingClientSessions(
      @Param("clientId") Long clientId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.client LEFT JOIN FETCH s.room " +
      "WHERE s.therapist.id = :therapistId " +
      "AND s.isDeleted = false " +
      "AND s.client.isDeleted = false " +
      "AND s.sessionDate >= :startDate AND s.sessionDate < :endDate " +
      "ORDER BY s.sessionDate ASC")
  List<Session> findByTherapistAndDateRange(
      @Param("therapistId") Long therapistId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Lean day load for availability: no client/therapist hydration (avoids PHI decryption).
   */
  @Query("""
      SELECT s FROM Session s
      LEFT JOIN FETCH s.room
      WHERE s.therapist.id = :therapistId
        AND s.isDeleted = false
        AND s.client.isDeleted = false
        AND s.sessionDate >= :startDate AND s.sessionDate < :endDate
      ORDER BY s.sessionDate ASC
      """)
  List<Session> findByTherapistAndDateRangeWithRoom(
      @Param("therapistId") Long therapistId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * All sessions in a day window with room only — used for room-busy checks.
   */
  @Query("""
      SELECT s FROM Session s
      LEFT JOIN FETCH s.room
      WHERE s.isDeleted = false
        AND s.client.isDeleted = false
        AND s.sessionDate >= :startDate AND s.sessionDate < :endDate
      ORDER BY s.sessionDate ASC
      """)
  List<Session> findByDateRangeWithRoom(
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.room " +
      "WHERE s.room.id = :roomId " +
      "AND s.isDeleted = false " +
      "AND s.client.isDeleted = false " +
      "AND s.sessionDate >= :startDate AND s.sessionDate < :endDate " +
      "ORDER BY s.sessionDate ASC")
  List<Session> findByRoomAndDateRange(
      @Param("roomId") Long roomId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.client LEFT JOIN FETCH s.room " +
      "WHERE s.therapist.id = :therapistId " +
      "AND s.isDeleted = false " +
      "AND s.client.isDeleted = false " +
      "AND s.sessionDate >= :startDate AND s.sessionDate < :endDate " +
      "AND s.status IN :statuses " +
      "ORDER BY s.sessionDate ASC")
  List<Session> findByTherapistAndDateRangeWithStatuses(
      @Param("therapistId") Long therapistId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      @Param("statuses") List<String> statuses);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.client LEFT JOIN FETCH s.room " +
      "WHERE s.isDeleted = false " +
      "AND s.client.isDeleted = false " +
      "AND s.sessionDate >= :startDate AND s.sessionDate < :endDate " +
      "AND s.status IN :statuses " +
      "ORDER BY s.sessionDate ASC")
  List<Session> findByDateRangeWithStatuses(
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      @Param("statuses") List<String> statuses);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.client LEFT JOIN FETCH s.room WHERE s.id = :id AND s.isDeleted = false")
  Optional<Session> findByIdWithRelations(@Param("id") Long id);

  /**
   * Hydrate list/calendar summary associations without N+1 lazy loads.
   * Integrations are loaded separately — JOIN FETCH + DISTINCT breaks on
   * {@code session_integrations.metadata} (Postgres {@code json} has no equality operator).
   */
  @Query("""
      SELECT DISTINCT s FROM Session s
      LEFT JOIN FETCH s.client
      LEFT JOIN FETCH s.therapist
      LEFT JOIN FETCH s.service
      LEFT JOIN FETCH s.room
      LEFT JOIN FETCH s.billing
      WHERE s.id IN :ids AND s.isDeleted = false
      """)
  List<Session> findByIdsForSummary(@Param("ids") Collection<Long> ids);

  /**
   * Calendar-only projection: decrypts client/therapist display names only,
   * not the full PHI-laden Client entity graph.
   */
  @Query("""
      SELECT new com.smart.therapy.flow.session.dto.SessionCalendarItemResponse(
          s.id,
          c.id,
          c.fullName,
          t.id,
          t.fullName,
          s.sessionDate,
          s.duration,
          s.clinicalSessionType,
          s.sessionType,
          s.status,
          svc.serviceName,
          r.id,
          r.roomName,
          r.roomType,
          s.recurrenceGroupId,
          b.id
      )
      FROM Session s
      JOIN s.client c
      JOIN s.therapist t
      LEFT JOIN s.service svc
      LEFT JOIN s.room r
      LEFT JOIN s.billing b
      WHERE s.id IN :ids AND s.isDeleted = false AND c.isDeleted = false
      """)
  List<com.smart.therapy.flow.session.dto.SessionCalendarItemResponse> findCalendarItemsByIds(
      @Param("ids") Collection<Long> ids);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Session s WHERE s.id = :id AND s.isDeleted = false")
  Optional<Session> findByIdForBilling(@Param("id") Long id);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.client WHERE s.client.id = :clientId AND s.isDeleted = false")
  List<Session> findByClientIdWithRelations(@Param("clientId") Long clientId);

  // Override default findById to filter deleted records
  @Override
  @Query("SELECT s FROM Session s WHERE s.id = :id AND s.isDeleted = false")
  @NonNull
  Optional<Session> findById(@NonNull @Param("id") Long id);

  // Override default findAll to filter deleted records
  @Override
  @Query("SELECT s FROM Session s WHERE s.isDeleted = false")
  @NonNull
  List<Session> findAll();

  /**
   * Find sessions that need reminder emails (24 hours before appointment).
   * Returns sessions that:
   * - Are scheduled/confirmed (not cancelled or completed)
   * - Are between 23-25 hours from now (to account for scheduling delays)
   * - Have not been soft-deleted
   * - Eager-load associations used by reminder payload (avoids LazyInitializationException
   *   when {@code @Transactional} does not apply via self-invocation from the scheduler)
   *
   * Integrations are loaded separately — JOIN FETCH + DISTINCT breaks on
   * {@code session_integrations.metadata} (Postgres {@code json} has no equality operator).
   */
  @Query("SELECT DISTINCT s FROM Session s " +
      "LEFT JOIN FETCH s.client " +
      "LEFT JOIN FETCH s.therapist " +
      "LEFT JOIN FETCH s.service " +
      "LEFT JOIN FETCH s.room " +
      "WHERE s.isDeleted = false " +
      "AND s.client.isDeleted = false " +
      "AND s.status IN ('scheduled', 'confirmed') " +
      "AND s.sessionDate >= :reminderStartTime " +
      "AND s.sessionDate <= :reminderEndTime " +
      "ORDER BY s.sessionDate ASC")
  List<Session> findSessionsForReminder(
      @Param("reminderStartTime") Instant reminderStartTime,
      @Param("reminderEndTime") Instant reminderEndTime);

  // Method to find by ID including deleted (for admin recovery purposes)
  @Query("SELECT s FROM Session s WHERE s.id = :id")
  Optional<Session> findByIdIncludingDeleted(@Param("id") Long id);

  @Query("SELECT COUNT(s) FROM Session s WHERE s.room.id = :roomId AND s.isDeleted = false")
  long countByRoomId(@Param("roomId") Long roomId);

  @Query("SELECT COUNT(s) FROM Session s WHERE s.room.id = :roomId AND s.isDeleted = true")
  long countDeletedSessionsByRoomId(@Param("roomId") Long roomId);

  // Native query because JPQL/HQL does not support database-specific interval
  // arithmetic.
  // Overlap condition: [session_start, session_end) overlaps [startTime, endTime)
  // Using TIMESTAMPADD for MySQL compatibility instead of PostgreSQL INTERVAL
  // syntax
  @Query(value = """
      SELECT COUNT(*)
      FROM sessions s
      JOIN clients c ON c.id = s.client_id
      WHERE s.room_id = :roomId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND c.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      """, nativeQuery = true)
  long countOverlappingSessionsForRoom(
      @Param("roomId") Long roomId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Same as {@link #countOverlappingSessionsForRoom} but ignores one session (edit/reschedule).
   */
  @Query(value = """
      SELECT COUNT(*)
      FROM sessions s
      JOIN clients c ON c.id = s.client_id
      WHERE s.room_id = :roomId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND c.is_deleted = false
        AND (:excludeSessionId IS NULL OR s.id <> :excludeSessionId)
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      """, nativeQuery = true)
  long countOverlappingSessionsForRoomExcludingSession(
      @Param("roomId") Long roomId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime,
      @Param("excludeSessionId") Long excludeSessionId);

  /**
   * Count overlapping sessions for a therapist using proper overlap detection.
   * Overlap condition: [session_start, session_end) overlaps [startTime, endTime)
   * This fixes the bug where findConflictingTherapistSessions only finds sessions
   * that START during the requested slot, missing sessions that started before but overlap.
   * 
   * @param therapistId Therapist ID
   * @param startTime Requested session start time
   * @param endTime Requested session end time
   * @return Count of overlapping sessions
   */
  @Query(value = """
      SELECT COUNT(*)
      FROM sessions s
      WHERE s.therapist_id = :therapistId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      """, nativeQuery = true)
  long countOverlappingSessionsForTherapist(
      @Param("therapistId") Long therapistId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Find overlapping sessions for a therapist with proper overlap detection.
   * Returns sessions that overlap with the requested time slot, including sessions
   * that started before the requested start time but end after it.
   * 
   * @param therapistId Therapist ID
   * @param startTime Requested session start time
   * @param endTime Requested session end time
   * @return List of overlapping sessions
   */
  @Query(value = """
      SELECT s.*
      FROM sessions s
      WHERE s.therapist_id = :therapistId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      ORDER BY s.session_date ASC
      """, nativeQuery = true)
  List<Session> findOverlappingTherapistSessions(
      @Param("therapistId") Long therapistId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Find overlapping sessions for a room with proper overlap detection.
   * Returns sessions that overlap with the requested time slot, including sessions
   * that started before the requested start time but end after it.
   * 
   * @param roomId Room ID
   * @param startTime Requested session start time
   * @param endTime Requested session end time
   * @return List of overlapping sessions
   */
  @Query(value = """
      SELECT s.*
      FROM sessions s
      WHERE s.room_id = :roomId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      ORDER BY s.session_date ASC
      """, nativeQuery = true)
  List<Session> findOverlappingRoomSessions(
      @Param("roomId") Long roomId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  @Query(value = """
      SELECT COUNT(*)
      FROM sessions s
      WHERE s.client_id = :clientId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      """, nativeQuery = true)
  long countOverlappingSessionsForClient(
      @Param("clientId") Long clientId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  @Query(value = """
      SELECT s.*
      FROM sessions s
      WHERE s.client_id = :clientId
        AND s.status NOT IN ('CANCELLED', 'COMPLETED')
        AND s.is_deleted = false
        AND s.session_date < :endTime
        AND s.session_date + (COALESCE(s.duration, 60) || ' minutes')::interval > :startTime
      ORDER BY s.session_date ASC
      """, nativeQuery = true)
  List<Session> findOverlappingClientSessions(
      @Param("clientId") Long clientId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  @Query("SELECT COUNT(s) FROM Session s WHERE s.service.id = :serviceId AND s.isDeleted = false")
  long countByServiceId(@Param("serviceId") Long serviceId);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.client LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.room "
      + "WHERE s.recurrenceGroupId = :groupId AND s.isDeleted = false ORDER BY s.sessionDate ASC")
  List<Session> findByRecurrenceGroupId(@Param("groupId") String groupId);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.client LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.room "
      + "WHERE s.recurrenceGroupId = :groupId AND s.isDeleted = false "
      + "AND s.sessionDate >= :fromDate AND s.status IN :statuses ORDER BY s.sessionDate ASC")
  List<Session> findFutureByRecurrenceGroupId(
      @Param("groupId") String groupId,
      @Param("fromDate") Instant fromDate,
      @Param("statuses") List<String> statuses);

  @Query("SELECT s FROM Session s LEFT JOIN FETCH s.therapist LEFT JOIN FETCH s.room "
      + "WHERE s.isDeleted = false AND s.sessionDate >= :startDate AND s.sessionDate <= :endDate "
      + "AND s.status IN :statuses ORDER BY s.sessionDate ASC")
  List<Session> findActiveSessionsInRange(
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      @Param("statuses") List<String> statuses);

  /**
   * Last held session: most recent non-cancelled session at or before {@code asOf}
   * (past + present clock time). Future booked sessions must not appear as last session.
   */
  @Query(value = """
      SELECT s.client_id AS client_id, MAX(s.session_date) AS session_date
      FROM sessions s
      WHERE s.client_id IN (:clientIds)
        AND s.is_deleted = false
        AND s.session_date <= :asOf
        AND LOWER(REPLACE(REPLACE(COALESCE(s.status, ''), '_', ''), ' ', '')) NOT LIKE '%cancel%'
      GROUP BY s.client_id
      """, nativeQuery = true)
  List<Object[]> findLastHeldSessionDatesByClientIds(
      @Param("clientIds") Collection<Long> clientIds,
      @Param("asOf") Instant asOf);

  /**
   * Next appointment: earliest non-cancelled, non-completed session strictly after {@code asOf}.
   */
  @Query(value = """
      SELECT s.client_id AS client_id, MIN(s.session_date) AS session_date
      FROM sessions s
      WHERE s.client_id IN (:clientIds)
        AND s.is_deleted = false
        AND s.session_date > :asOf
        AND LOWER(REPLACE(REPLACE(COALESCE(s.status, ''), '_', ''), ' ', '')) NOT LIKE '%cancel%'
        AND LOWER(REPLACE(REPLACE(COALESCE(s.status, ''), '_', ''), ' ', '')) NOT LIKE '%complete%'
      GROUP BY s.client_id
      """, nativeQuery = true)
  List<Object[]> findNextAppointmentDatesByClientIds(
      @Param("clientIds") Collection<Long> clientIds,
      @Param("asOf") Instant asOf);

  @Query(value = """
      SELECT MAX(s.session_date)
      FROM sessions s
      WHERE s.client_id = :clientId
        AND s.is_deleted = false
        AND s.session_date <= :asOf
        AND LOWER(REPLACE(REPLACE(COALESCE(s.status, ''), '_', ''), ' ', '')) NOT LIKE '%cancel%'
      """, nativeQuery = true)
  Instant findLastHeldSessionDate(
      @Param("clientId") Long clientId,
      @Param("asOf") Instant asOf);

  @Query(value = """
      SELECT MIN(s.session_date)
      FROM sessions s
      WHERE s.client_id = :clientId
        AND s.is_deleted = false
        AND s.session_date > :asOf
        AND LOWER(REPLACE(REPLACE(COALESCE(s.status, ''), '_', ''), ' ', '')) NOT LIKE '%cancel%'
        AND LOWER(REPLACE(REPLACE(COALESCE(s.status, ''), '_', ''), ' ', '')) NOT LIKE '%complete%'
      """, nativeQuery = true)
  Instant findNextAppointmentDate(
      @Param("clientId") Long clientId,
      @Param("asOf") Instant asOf);
}




