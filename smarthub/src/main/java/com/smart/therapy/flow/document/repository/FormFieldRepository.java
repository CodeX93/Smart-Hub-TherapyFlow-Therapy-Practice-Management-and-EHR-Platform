package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface FormFieldRepository extends JpaRepository<FormField, Long> {
    
    List<FormField> findByTemplateVersionIdAndIsDeletedFalseOrderBySortOrderAsc(Long templateVersionId);
    
    List<FormField> findBySectionIdAndIsDeletedFalseOrderBySortOrderAsc(Long sectionId);
    
    @Query("SELECT ff FROM FormField ff " +
           "LEFT JOIN FETCH ff.options " +
           "WHERE ff.templateVersion.id = :templateVersionId " +
           "AND ff.isDeleted = false " +
           "ORDER BY ff.sortOrder ASC")
    List<FormField> findByTemplateVersionIdWithOptions(@Param("templateVersionId") Long templateVersionId);
    
    @Query("SELECT ff FROM FormField ff " +
           "LEFT JOIN FETCH ff.options " +
           "WHERE ff.section.id = :sectionId " +
           "AND ff.isDeleted = false " +
           "ORDER BY ff.sortOrder ASC")
    List<FormField> findBySectionIdWithOptions(@Param("sectionId") Long sectionId);
    
    void deleteByTemplateVersionId(Long templateVersionId);
}





