package com.smart.therapy.flow.report.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.report.entity.ClientReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientReportRepository extends JpaRepository<ClientReport, Long> {
    List<ClientReport> findByClient_IdOrderByGeneratedAtDesc(Long clientId);

    @Query("SELECT r FROM ClientReport r "
            + "LEFT JOIN FETCH r.client "
            + "LEFT JOIN FETCH r.template "
            + "LEFT JOIN FETCH r.createdByUser "
            + "WHERE r.id = :id")
    Optional<ClientReport> findByIdWithRelations(@Param("id") Long id);
}
