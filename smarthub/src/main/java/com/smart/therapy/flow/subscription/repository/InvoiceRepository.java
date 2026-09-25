package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    @Query("""
            SELECT i
            FROM Invoice i
            WHERE (:organisationId IS NULL OR i.subscription.organisation.id = :organisationId)
              AND (:status IS NULL OR i.status = :status)
            """)
    Page<Invoice> search(@Param("organisationId") Long organisationId,
                         @Param("status") InvoiceStatus status,
                         Pageable pageable);

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.subscription s
            JOIN FETCH s.organisation
            JOIN FETCH s.plan
            WHERE i.id = :id
            """)
    java.util.Optional<Invoice> findDetailedById(@Param("id") Long id);

    @Query("SELECT i FROM Invoice i WHERE i.providerInvoiceId = :providerInvoiceId")
    Invoice findByProviderInvoiceId(@Param("providerInvoiceId") String providerInvoiceId);

    @Query("""
            SELECT i
            FROM Invoice i
            WHERE i.createdAt >= :fromAt
              AND i.createdAt < :toAt
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findByCreatedRange(@Param("fromAt") Instant fromAt, @Param("toAt") Instant toAt);

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.subscription s
            JOIN FETCH s.plan
            WHERE i.createdAt >= :fromAt
              AND i.createdAt < :toAt
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findByCreatedRangeWithPlan(@Param("fromAt") Instant fromAt, @Param("toAt") Instant toAt);

    @Query("""
            SELECT COUNT(i) > 0
            FROM Invoice i
            WHERE i.subscription.organisation.id = :organisationId
              AND i.status IN :statuses
            """)
    boolean existsOpenInvoicesByOrganisationId(@Param("organisationId") Long organisationId,
                                               @Param("statuses") List<InvoiceStatus> statuses);
}
