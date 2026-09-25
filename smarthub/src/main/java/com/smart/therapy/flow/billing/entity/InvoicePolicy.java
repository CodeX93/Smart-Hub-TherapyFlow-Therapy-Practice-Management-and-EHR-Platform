package com.smart.therapy.flow.billing.entity;

import com.smart.therapy.flow.billing.enums.InvoicePolicyPriceType;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_policies", indexes = {
        @Index(name = "idx_invoice_policy_client", columnList = "client_type_key"),
        @Index(name = "idx_invoice_policy_appointment", columnList = "appointment_status_key"),
        @Index(name = "idx_invoice_policy_enabled", columnList = "enabled"),
        @Index(name = "idx_invoice_policy_service", columnList = "service_id"),
        @Index(name = "idx_invoice_policy_priority", columnList = "priority")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class InvoicePolicy extends BaseEntity {

    @Column(name = "client_type_key", nullable = false, length = 100)
    private String clientTypeKey;

    @Column(name = "client_type_label", nullable = false, length = 255)
    private String clientTypeLabel;

    @Column(name = "appointment_status_key", nullable = false, length = 100)
    private String appointmentStatusKey;

    @Column(name = "appointment_status_label", nullable = false, length = 255)
    private String appointmentStatusLabel;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 20)
    private InvoicePolicyPriceType priceType;

    @Column(name = "invoice_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal invoicePrice;

    @Column(name = "policy_name", length = 255)
    private String policyName;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;
}

