package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.FormTemplate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {
    
    @Query(value = """
            SELECT ft.*
            FROM form_templates ft
            WHERE ft.is_deleted = false
              AND ft.is_active = true
            ORDER BY ft.createdat DESC, ft.id DESC
            """, nativeQuery = true)
    List<FormTemplate> findActiveTemplatesNewestFirst();
    
    Optional<FormTemplate> findByIdAndIsDeletedFalse(Long id);
    
    @Query("SELECT ft FROM FormTemplate ft " +
           "LEFT JOIN FETCH ft.versions " +
           "WHERE ft.id = :id AND ft.isDeleted = false")
    Optional<FormTemplate> findByIdWithVersions(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ft FROM FormTemplate ft WHERE ft.id = :id AND ft.isDeleted = false")
    Optional<FormTemplate> findByIdForUpdate(@Param("id") Long id);
    
    List<FormTemplate> findByCategoryAndIsDeletedFalseAndIsActiveTrue(String category);

    /** Count non-deleted form templates (for plan limit FORM_TEMPLATES). */
    long countByIsDeletedFalse();

    boolean existsByIdAndIsSystemTemplateTrue(Long id);
}





