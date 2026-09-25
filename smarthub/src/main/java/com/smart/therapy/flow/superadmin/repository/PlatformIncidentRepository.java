package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PlatformIncidentRepository extends JpaRepository<PlatformIncident, Long> {
    List<PlatformIncident> findByStatusOrderByStartedAtDesc(String status);

    /** Incidents overlapping [from, to); an unresolved incident is still running, so it always overlaps the tail. */
    @Query("SELECT i FROM PlatformIncident i "
            + "WHERE i.startedAt < :to AND (i.resolvedAt IS NULL OR i.resolvedAt > :from) "
            + "ORDER BY i.startedAt ASC")
    List<PlatformIncident> findOverlapping(@Param("from") Instant from, @Param("to") Instant to);
}
