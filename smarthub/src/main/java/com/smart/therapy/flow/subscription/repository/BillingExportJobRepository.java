package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.BillingExportJob;
import com.smart.therapy.flow.subscription.enums.BillingExportJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface BillingExportJobRepository extends JpaRepository<BillingExportJob, Long> {

    @Modifying
    @Query("""
            update BillingExportJob job
               set job.tokenConsumed = true,
                   job.tokenConsumedAt = :consumedAt,
                   job.updatedAt = :consumedAt
             where job.id = :jobId
               and job.downloadToken = :tokenHash
               and job.tokenConsumed = false
               and job.status = :requiredStatus
               and job.tokenExpiresAt is not null
               and job.tokenExpiresAt >= :consumedAt
            """)
    int consumeDownloadToken(@Param("jobId") Long jobId,
                             @Param("tokenHash") String tokenHash,
                             @Param("requiredStatus") BillingExportJobStatus requiredStatus,
                             @Param("consumedAt") Instant consumedAt);
}
