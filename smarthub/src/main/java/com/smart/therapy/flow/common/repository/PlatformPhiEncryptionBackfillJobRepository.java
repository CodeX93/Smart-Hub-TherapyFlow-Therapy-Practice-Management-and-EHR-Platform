package com.smart.therapy.flow.common.repository;

import com.smart.therapy.flow.common.entity.PlatformPhiEncryptionBackfillJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformPhiEncryptionBackfillJobRepository extends JpaRepository<PlatformPhiEncryptionBackfillJob, Long> {

    Optional<PlatformPhiEncryptionBackfillJob> findBySchemaNameAndTableNameAndColumnName(
            String schemaName, String tableName, String columnName);

    List<PlatformPhiEncryptionBackfillJob> findByTableNameAndColumnName(String tableName, String columnName);

    @Query("""
            SELECT j FROM PlatformPhiEncryptionBackfillJob j
            WHERE j.status IN ('PENDING', 'IN_PROGRESS')
            ORDER BY j.updatedAt ASC
            """)
    List<PlatformPhiEncryptionBackfillJob> findRunnableJobs();
}
