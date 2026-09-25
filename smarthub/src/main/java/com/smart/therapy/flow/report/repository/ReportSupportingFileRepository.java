package com.smart.therapy.flow.report.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.report.entity.ReportSupportingFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ReportSupportingFileRepository extends JpaRepository<ReportSupportingFile, Long> {
    List<ReportSupportingFile> findByClient_IdOrderByCreatedAtDesc(Long clientId);
    Optional<ReportSupportingFile> findByIdAndClient_Id(Long id, Long clientId);
}
