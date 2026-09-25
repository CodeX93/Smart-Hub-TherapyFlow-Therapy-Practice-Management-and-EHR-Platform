package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormSectionRepository extends JpaRepository<FormSection, Long> {
    
    List<FormSection> findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(Long templateVersionId);
    
    @Query("SELECT fs FROM FormSection fs " +
           "LEFT JOIN FETCH fs.fields " +
           "WHERE fs.id = :id AND fs.isDeleted = false")
    Optional<FormSection> findByIdWithFields(@Param("id") Long id);
    
    @Query("SELECT fs FROM FormSection fs " +
           "WHERE fs.templateVersion.id = :templateVersionId " +
           "AND fs.isDeleted = false " +
           "ORDER BY fs.sortOrder ASC")
    List<FormSection> findByTemplateVersionIdWithFields(@Param("templateVersionId") Long templateVersionId);
}




