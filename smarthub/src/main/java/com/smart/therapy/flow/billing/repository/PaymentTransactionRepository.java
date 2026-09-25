package com.smart.therapy.flow.billing.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.billing.entity.PaymentTransaction;
import com.smart.therapy.flow.billing.enums.PaymentSource;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
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
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find all transactions for a specific payment
     */
    List<PaymentTransaction> findByPaymentId(Long paymentId);

    /**
     * Find all transactions for a specific payment ordered by created date
     */
    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    /**
     * Find transaction by provider intent ID (e.g., Stripe Payment Intent)
     */
    Optional<PaymentTransaction> findByProviderIntentId(String providerIntentId);

    /**
     * Find transaction by provider charge ID (e.g., Stripe Charge)
     */
    Optional<PaymentTransaction> findByProviderChargeId(String providerChargeId);

    /**
     * Find all transactions for a provider and customer
     */
    List<PaymentTransaction> findByProviderAndProviderCustomerId(PaymentSource provider, String providerCustomerId);

    /**
     * Find transactions by status
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status);

    /**
     * Find transactions by provider
     */
    List<PaymentTransaction> findByProvider(PaymentSource provider);

    /**
     * Check if a transaction exists for a provider intent ID
     */
    boolean existsByProviderIntentId(String providerIntentId);

    /**
     * Find all transactions belonging to a billing record, newest first.
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.payment.sessionBilling.id = :billingId ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findBySessionBillingIdOrderByCreatedAtDesc(@Param("billingId") Long billingId);

    /**
     * Lock one transaction row for update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") Long id);

    /**
     * Lock one transaction row scoped by billing for update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.id = :id AND pt.payment.sessionBilling.id = :billingId")
    Optional<PaymentTransaction> findByIdAndBillingIdForUpdate(@Param("id") Long id, @Param("billingId") Long billingId);

    /**
     * Find latest transaction for a payment
     */
    Optional<PaymentTransaction> findFirstByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}




