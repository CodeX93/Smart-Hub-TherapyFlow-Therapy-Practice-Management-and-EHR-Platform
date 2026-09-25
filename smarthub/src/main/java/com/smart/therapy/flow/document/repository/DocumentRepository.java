package com.smart.therapy.flow.document.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    // All queries automatically filter out soft-deleted records (isDeleted = false)
    long countByClientIdAndIsDeletedFalse(Long clientId);

    @Query("SELECT d FROM Document d WHERE d.client.id = :clientId AND d.isDeleted = false")
    List<Document> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT d FROM Document d WHERE d.category = :category AND d.isDeleted = false")
    List<Document> findByCategory(@Param("category") String category);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.client WHERE d.client.id = :clientId AND d.isSharedInPortal = true AND d.isDeleted = false")
    List<Document> findSharedWithClient(@Param("clientId") Long clientId);

    @Query("SELECT d FROM Document d WHERE d.needsReview = true AND d.reviewStatus = 'PENDING' AND d.isDeleted = false")
    List<Document> findPendingReview();

    @EntityGraph(attributePaths = {"client", "uploadedBy", "reviewedBy"})
    List<Document> findByNeedsReviewTrueAndReviewStatusInAndIsDeletedFalse(List<ReviewStatus> statuses);

    @EntityGraph(attributePaths = {"client", "uploadedBy", "reviewedBy"})
    List<Document> findByNeedsReviewTrueAndReviewStatusInAndIsDeletedFalseAndClient_Id(List<ReviewStatus> statuses, Long clientId);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.client LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.reviewedBy WHERE d.client.id = :clientId AND (d.uploadedBy IS NULL OR d.isSharedInPortal = true) AND d.isDeleted = false ORDER BY d.createdAt DESC")
    List<Document> findPortalDocuments(@Param("clientId") Long clientId);
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.client LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.reviewedBy WHERE d.id = :id AND d.isDeleted = false")
    java.util.Optional<Document> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.client LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.reviewedBy WHERE d.client.id = :clientId AND d.isDeleted = false ORDER BY d.createdAt DESC")
    List<Document> findByClientIdWithRelations(@Param("clientId") Long clientId);
    
    // Override default findById to filter deleted records
    @Override
    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.isDeleted = false")
    @NonNull
    java.util.Optional<Document> findById(@NonNull @Param("id") Long id);
    
    // Override default findAll to filter deleted records
    @Override
    @Query("SELECT d FROM Document d WHERE d.isDeleted = false")
    @NonNull
    List<Document> findAll();
    
    // Method to find by ID including deleted (for admin recovery purposes)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    java.util.Optional<Document> findByIdIncludingDeleted(@Param("id") Long id);
}





