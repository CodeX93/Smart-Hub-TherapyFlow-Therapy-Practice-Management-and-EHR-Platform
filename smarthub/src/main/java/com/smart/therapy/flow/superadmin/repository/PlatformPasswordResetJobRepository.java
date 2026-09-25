package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformPasswordResetJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PlatformPasswordResetJobRepository extends JpaRepository<PlatformPasswordResetJob, Long> {

    List<PlatformPasswordResetJob> findByStatusAndEffectiveAtLessThanEqualOrderByEffectiveAtAsc(String status, Instant effectiveAt);
}
