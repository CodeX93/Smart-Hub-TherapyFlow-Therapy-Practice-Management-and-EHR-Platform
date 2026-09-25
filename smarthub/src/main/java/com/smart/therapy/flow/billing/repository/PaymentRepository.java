package com.smart.therapy.flow.billing.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find all payments for a specific session billing
     */
    List<Payment> findBySessionBillingId(Long sessionBillingId);

    /**
     * Find all payments for a specific session billing ordered by payment date (newest first), then by creation time as tiebreaker
     */
    List<Payment> findBySessionBillingIdOrderByPaymentDateDescCreatedAtDesc(Long sessionBillingId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(String status);

    /**
     * Find payments by payment method
     */
    List<Payment> findByPaymentMethod(String paymentMethod);

    /**
     * Find latest payment for a session billing (newest first, with createdAt as tiebreaker)
     */
    Optional<Payment> findFirstBySessionBillingIdOrderByPaymentDateDescCreatedAtDesc(Long sessionBillingId);

    /**
     * Find payments by date range
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByPaymentDateBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find payments by session billing and status
     */
    List<Payment> findBySessionBillingIdAndStatus(Long sessionBillingId, String status);

    /**
     * Find payments by session billing and enum status ordered by most recent first.
     */
    List<Payment> findBySessionBillingIdAndStatusOrderByPaymentDateDesc(Long sessionBillingId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.sessionBilling.id IN :billingIds ORDER BY p.paymentDate DESC, p.createdAt DESC")
    List<Payment> findBySessionBillingIdIn(@Param("billingIds") Collection<Long> billingIds);
}
