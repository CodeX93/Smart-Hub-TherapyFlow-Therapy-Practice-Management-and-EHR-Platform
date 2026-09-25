package com.smart.therapy.flow.subscription.repository;

import com.smart.therapy.flow.subscription.entity.InvoiceDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDisputeRepository extends JpaRepository<InvoiceDispute, Long> {

    List<InvoiceDispute> findByInvoice_IdOrderByOpenedAtDesc(Long invoiceId);
}
