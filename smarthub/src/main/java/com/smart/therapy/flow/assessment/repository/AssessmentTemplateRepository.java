package com.smart.therapy.flow.assessment.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.assessment.entity.AssessmentSection;
import com.smart.therapy.flow.assessment.entity.AssessmentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface AssessmentTemplateRepository extends JpaRepository<AssessmentTemplate, Long> {
    
    // ========== Soft Delete Filtering ==========
    
    @Override
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.id = :id AND t.isDeleted = false")
    @NonNull
    Optional<AssessmentTemplate> findById(@NonNull @Param("id") Long id);
    
    @Override
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.isDeleted = false")
    @NonNull
    List<AssessmentTemplate> findAll();
    
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.id = :id")
    Optional<AssessmentTemplate> findByIdIncludingDeleted(@Param("id") Long id);
    
    // ========== Custom Queries (with soft delete filter) ==========
    
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.name = :name AND t.isDeleted = false")
    Optional<AssessmentTemplate> findByName(@Param("name") String name);
    
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.category = :category AND t.isDeleted = false")
    List<AssessmentTemplate> findByCategory(@Param("category") String category);
    
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.isActive = :isActive AND t.isDeleted = false ORDER BY t.createdAt DESC, t.id DESC")
    List<AssessmentTemplate> findByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT COUNT(t) FROM AssessmentTemplate t WHERE t.isDeleted = false")
    long countActiveTemplates();
    
    @Query("SELECT DISTINCT t FROM AssessmentTemplate t " +
           "LEFT JOIN FETCH t.sections s " +
           "WHERE t.id = :id AND t.isDeleted = false")
    Optional<AssessmentTemplate> findByIdWithSections(@Param("id") Long id);

    /**
     * Second fetch for questions. Hibernate cannot JOIN FETCH two List bags
     * (sections + questions) in one query — that causes MultipleBagFetchException.
     * Call after {@link #findByIdWithSections} in the same persistence context.
     */
    @Query("SELECT DISTINCT s FROM AssessmentSection s " +
           "LEFT JOIN FETCH s.questions " +
           "WHERE s.template.id = :templateId")
    List<AssessmentSection> findSectionsWithQuestionsByTemplateId(@Param("templateId") Long templateId);
    
    // ========== Version History ==========
    
    /**
     * Find all versions of a template by base name (excluding version suffix).
     * Returns all versions sorted by version number descending.
     */
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.name LIKE :baseNamePattern AND t.isDeleted = false ORDER BY t.versionNumber DESC")
    List<AssessmentTemplate> findVersionsByBaseName(@Param("baseNamePattern") String baseNamePattern);
    
    /**
     * Find all versions of templates with names starting with the base name pattern.
     * Uses pattern matching to find related versions (e.g., "Template (v%" finds all versions).
     * Note: This is a simplified approach - in production, you might want to store base_name separately.
     */
    @Query("SELECT t FROM AssessmentTemplate t WHERE " +
           "t.name LIKE :baseNamePattern AND t.isDeleted = false " +
           "ORDER BY t.versionNumber DESC")
    List<AssessmentTemplate> findVersionsByBaseNameClean(@Param("baseNamePattern") String baseNamePattern);
    
    /**
     * Find template by exact name and version number.
     */
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.name = :name AND t.versionNumber = :versionNumber AND t.isDeleted = false")
    Optional<AssessmentTemplate> findByNameAndVersionNumber(@Param("name") String name, @Param("versionNumber") Integer versionNumber);
    
    /**
     * Get the highest version number for templates matching a base name pattern.
     * Uses pattern matching to find all versions of a template.
     */
    @Query("SELECT MAX(t.versionNumber) FROM AssessmentTemplate t WHERE " +
           "t.name LIKE :baseNamePattern AND t.isDeleted = false")
    Integer findMaxVersionNumberByBaseName(@Param("baseNamePattern") String baseNamePattern);
    
    /**
     * Find all templates with the same category (for finding related templates).
     */
    @Query("SELECT t FROM AssessmentTemplate t WHERE t.category = :category AND t.isDeleted = false ORDER BY t.versionNumber DESC")
    List<AssessmentTemplate> findByCategoryOrderByVersion(@Param("category") String category);
}




