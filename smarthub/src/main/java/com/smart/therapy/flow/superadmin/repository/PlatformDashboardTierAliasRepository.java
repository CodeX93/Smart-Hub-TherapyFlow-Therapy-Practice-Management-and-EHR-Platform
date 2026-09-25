package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformDashboardTierAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformDashboardTierAliasRepository extends JpaRepository<PlatformDashboardTierAlias, Long> {
    List<PlatformDashboardTierAlias> findByIsDeletedFalseOrderByTierNameAscPlanCodeAsc();
}
