package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformNotificationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PlatformNotificationJobRepository extends JpaRepository<PlatformNotificationJob, Long> {
    List<PlatformNotificationJob> findTop200ByOrderByCreatedAtDesc();

    List<PlatformNotificationJob> findByStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            List<String> statuses, Instant at
    );
}
