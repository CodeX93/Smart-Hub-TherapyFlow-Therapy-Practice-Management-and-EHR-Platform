package com.smart.therapy.flow.system.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.system.entity.SystemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface SystemOptionRepository extends JpaRepository<SystemOption, Long> {
    Optional<SystemOption> findByOptionKey(String optionKey);
    List<SystemOption> findByCategoryId(Long categoryId);
    List<SystemOption> findByIsActive(Boolean isActive);
}





