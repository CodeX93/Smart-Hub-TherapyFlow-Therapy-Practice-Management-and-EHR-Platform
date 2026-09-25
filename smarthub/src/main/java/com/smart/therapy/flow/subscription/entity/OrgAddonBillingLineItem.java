package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "org_addon_billing_line_items", schema = "public", indexes = {
        @Index(name = "idx_org_addon_billing_line_items_org", columnList = "organisation_id, created_at"),
        @Index(name = "idx_org_addon_billing_line_items_purchase", columnList = "org_feature_purchase_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgAddonBillingLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "org_feature_purchase_id", nullable = false)
    private Long orgFeaturePurchaseId;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_value", nullable = false)
    private Integer unitValue;

    @Column(name = "unit_price_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceUsd;

    @Column(name = "total_amount_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmountUsd;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private AddonBillingCycle billingCycle;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
