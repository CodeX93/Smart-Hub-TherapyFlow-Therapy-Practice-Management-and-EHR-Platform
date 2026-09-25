package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@TenantScoped
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    // ========== Soft Delete Filtering (CRITICAL - Clinical Notes) ==========
    
    @Override
    @Query("SELECT n FROM Note n WHERE n.id = :id AND n.isDeleted = false")
    @NonNull
    java.util.Optional<Note> findById(@NonNull @Param("id") Long id);
    
    @Override
    @Query("SELECT n FROM Note n WHERE n.isDeleted = false")
    @NonNull
    List<Note> findAll();
    
    /**
     * HIPAA compliance: Find note including deleted for audit purposes
     */
    @Query("SELECT n FROM Note n WHERE n.id = :id")
    java.util.Optional<Note> findByIdIncludingDeleted(@Param("id") Long id);
    
    // ========== Custom Queries (with soft delete filter) ==========
    
    @Query("SELECT n FROM Note n WHERE n.client.id = :clientId AND n.isDeleted = false")
    List<Note> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT n FROM Note n WHERE n.createdByUser.id = :authorId AND n.isDeleted = false")
    List<Note> findByAuthorId(@Param("authorId") Long authorId);
    
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.client LEFT JOIN FETCH n.createdByUser WHERE n.client.id = :clientId " +
           "AND (:noteType IS NULL OR n.noteType = :noteType) " +
           "AND (:startDate IS NULL OR n.eventDate >= :startDate) " +
           "AND (:endDate IS NULL OR n.eventDate <= :endDate) " +
           "AND n.isDeleted = false " +
           "ORDER BY n.eventDate DESC")
    List<Note> findByClientWithFilters(
            @Param("clientId") Long clientId,
            @Param("noteType") String noteType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
    
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.client LEFT JOIN FETCH n.createdByUser WHERE n.client.id = :clientId AND n.isDeleted = false")
    List<Note> findByClientIdWithRelations(@Param("clientId") Long clientId);
    
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.client LEFT JOIN FETCH n.createdByUser WHERE n.id = :id AND n.isDeleted = false")
    java.util.Optional<Note> findByIdWithRelations(@Param("id") Long id);
}





