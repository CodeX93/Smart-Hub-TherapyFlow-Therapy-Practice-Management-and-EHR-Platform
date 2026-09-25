package com.smart.therapy.flow.common.repository;

import com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformPhiBlindIndexBackfillJobRepository
        extends JpaRepository<PlatformPhiBlindIndexBackfillJob, Long> {

    Optional<PlatformPhiBlindIndexBackfillJob> findBySchemaNameAndJobKind(String schemaName, String jobKind);

    @Query("""
            SELECT j FROM PlatformPhiBlindIndexBackfillJob j
            WHERE UPPER(j.status) IN ('PENDING', 'IN_PROGRESS', 'FAILED')
            ORDER BY
              CASE WHEN j.jobKind IN ('NAME_TOKEN_DIGESTS', 'NAME_PREFIX_DIGESTS') THEN 0 ELSE 1 END ASC,
              j.updatedAt ASC
            """)
    List<PlatformPhiBlindIndexBackfillJob> findRunnableJobs();
}
