package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentReportVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AssessmentReportVersion entity.
 */
@Repository
@TenantScoped
public interface AssessmentReportVersionRepository extends JpaRepository<AssessmentReportVersion, Long> {

    /**
     * Find all versions for a report, ordered by version number
     */
    @Query("SELECT v FROM AssessmentReportVersion v WHERE v.report.id = :reportId ORDER BY v.versionNumber ASC")
    List<AssessmentReportVersion> findByReportIdOrderByVersionNumber(@Param("reportId") Long reportId);

    /**
     * Find latest version for a report
     */
    @Query("SELECT v FROM AssessmentReportVersion v WHERE v.report.id = :reportId ORDER BY v.versionNumber DESC")
    Optional<AssessmentReportVersion> findLatestByReportId(@Param("reportId") Long reportId);

    /**
     * Find specific version number for a report
     */
    @Query("SELECT v FROM AssessmentReportVersion v WHERE v.report.id = :reportId AND v.versionNumber = :versionNumber")
    Optional<AssessmentReportVersion> findByReportIdAndVersionNumber(
            @Param("reportId") Long reportId, 
            @Param("versionNumber") Integer versionNumber
    );

    /**
     * Count versions for a report
     */
    @Query("SELECT COUNT(v) FROM AssessmentReportVersion v WHERE v.report.id = :reportId")
    Long countByReportId(@Param("reportId") Long reportId);
}




