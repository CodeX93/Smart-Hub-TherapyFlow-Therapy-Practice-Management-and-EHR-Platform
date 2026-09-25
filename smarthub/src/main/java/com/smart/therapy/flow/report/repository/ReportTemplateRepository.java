package com.smart.therapy.flow.report.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.report.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    List<ReportTemplate> findByIsActiveTrueOrderByNameAsc();
    List<ReportTemplate> findAllByOrderByNameAsc();
    Optional<ReportTemplate> findFirstByNameIgnoreCase(String name);
}
