package com.smart.therapy.flow.system.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.system.entity.OptionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface OptionCategoryRepository extends JpaRepository<OptionCategory, Long> {
    Optional<OptionCategory> findByCategoryName(String categoryName);
    Optional<OptionCategory> findByCategoryKey(String categoryKey);
    List<OptionCategory> findByIsActive(Boolean isActive);
    List<OptionCategory> findByIsSystem(Boolean isSystem);
}





