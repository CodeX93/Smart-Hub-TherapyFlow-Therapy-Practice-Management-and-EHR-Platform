package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormTemplateVersion;
import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormTemplateVersionRepository extends JpaRepository<FormTemplateVersion, Long> {
    
    List<FormTemplateVersion> findByTemplateIdAndIsDeletedFalseOrderByVersionNumberDesc(Long templateId);
    
    Optional<FormTemplateVersion> findByTemplateIdAndVersionNumberAndIsDeletedFalse(Long templateId, Long versionNumber);
    
    Optional<FormTemplateVersion> findByTemplateIdAndStatusAndIsDeletedFalse(
        Long templateId, 
        FormTemplateVersionStatus status
    );
    
    @Query("SELECT ftv FROM FormTemplateVersion ftv " +
           "LEFT JOIN FETCH ftv.sections " +
           "LEFT JOIN FETCH ftv.fields " +
           "WHERE ftv.id = :id AND ftv.isDeleted = false")
    Optional<FormTemplateVersion> findByIdWithSectionsAndFields(@Param("id") Long id);
    
    @Query("SELECT ftv FROM FormTemplateVersion ftv " +
           "WHERE ftv.template.id = :templateId " +
           "AND ftv.status = :status " +
           "AND ftv.isDeleted = false " +
           "ORDER BY ftv.versionNumber DESC")
    List<FormTemplateVersion> findActiveVersionsByTemplateId(
        @Param("templateId") Long templateId,
        @Param("status") FormTemplateVersionStatus status
    );
    
    Optional<FormTemplateVersion> findFirstByTemplateIdAndIsDeletedFalseOrderByVersionNumberDesc(Long templateId);

    @Query("SELECT COALESCE(MAX(ftv.versionNumber), 0) FROM FormTemplateVersion ftv " +
           "WHERE ftv.template.id = :templateId AND ftv.isDeleted = false")
    Long findMaxVersionNumberByTemplateId(@Param("templateId") Long templateId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FormTemplateVersion ftv " +
           "SET ftv.status = :archivedStatus " +
           "WHERE ftv.template.id = :templateId " +
           "AND ftv.isDeleted = false " +
           "AND ftv.status = :activeStatus " +
           "AND ftv.id <> :excludingVersionId")
    int archiveActiveVersions(@Param("templateId") Long templateId,
                              @Param("excludingVersionId") Long excludingVersionId,
                              @Param("activeStatus") FormTemplateVersionStatus activeStatus,
                              @Param("archivedStatus") FormTemplateVersionStatus archivedStatus);
}




