package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.InvoiceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceAdjustmentRepository extends JpaRepository<InvoiceAdjustment, Long> {

    List<InvoiceAdjustment> findByInvoice_IdOrderByCreatedAtDesc(Long invoiceId);
}
