package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormFieldOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface FormFieldOptionRepository extends JpaRepository<FormFieldOption, Long> {
    
    List<FormFieldOption> findByFieldIdAndIsDeletedFalseOrderBySortOrderAsc(Long fieldId);
    
    @Modifying
    @Query("DELETE FROM FormFieldOption ffo WHERE ffo.field.id = :fieldId")
    void deleteByFieldId(@Param("fieldId") Long fieldId);
}




