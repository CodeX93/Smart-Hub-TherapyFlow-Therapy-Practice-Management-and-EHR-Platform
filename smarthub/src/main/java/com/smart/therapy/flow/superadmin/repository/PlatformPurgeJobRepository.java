package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformPurgeJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PlatformPurgeJobRepository extends JpaRepository<PlatformPurgeJob, Long> {
    boolean existsByOrganisation_IdAndStatusIn(Long organisationId, List<String> statuses);

    List<PlatformPurgeJob> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(String status, Instant at);

    List<PlatformPurgeJob> findByStatusAndWarningSentAtIsNullAndScheduledAtBetweenOrderByScheduledAtAsc(
            String status,
            Instant from,
            Instant to
    );
}
