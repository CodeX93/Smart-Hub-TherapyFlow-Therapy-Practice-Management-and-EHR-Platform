package com.smart.therapy.flow.billing.repository;

import com.smart.therapy.flow.billing.entity.InvoicePolicy;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@TenantScoped
public interface InvoicePolicyRepository extends JpaRepository<InvoicePolicy, Long> {

    @Query("""
            select p
            from InvoicePolicy p
            where lower(p.clientTypeKey) = lower(:clientTypeKey)
              and lower(p.appointmentStatusKey) = lower(:appointmentStatusKey)
              and ((:serviceId is null and p.serviceId is null) or p.serviceId = :serviceId)
            """)
    Optional<InvoicePolicy> findByScope(
            @Param("clientTypeKey") String clientTypeKey,
            @Param("appointmentStatusKey") String appointmentStatusKey,
            @Param("serviceId") Long serviceId
    );

    @Query("""
            select p
            from InvoicePolicy p
            where p.enabled = true
              and lower(p.clientTypeKey) in :clientTypeKeys
              and lower(p.appointmentStatusKey) in :appointmentStatusKeys
              and (:serviceId is null or p.serviceId is null or p.serviceId = :serviceId)
              and (p.effectiveFrom is null or p.effectiveFrom <= :billingDate)
              and (p.effectiveTo is null or p.effectiveTo >= :billingDate)
            order by
              case when p.serviceId is not null then 1 else 0 end desc,
              case when lower(p.clientTypeKey) = 'all' then 0 else 1 end desc,
              case when lower(p.appointmentStatusKey) = 'all' then 0 else 1 end desc,
              p.priority desc,
              p.updatedAt desc
            """)
    List<InvoicePolicy> findEnabledMatches(
            @Param("clientTypeKeys") Collection<String> clientTypeKeys,
            @Param("appointmentStatusKeys") Collection<String> appointmentStatusKeys,
            @Param("serviceId") Long serviceId,
            @Param("billingDate") LocalDate billingDate
    );
}
