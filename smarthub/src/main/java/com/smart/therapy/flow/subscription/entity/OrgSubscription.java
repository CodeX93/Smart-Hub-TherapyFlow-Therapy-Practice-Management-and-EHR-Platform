package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The heart of SaaS billing. One row per org per plan period (public schema).
 * Answers: "What plan did this clinic have on any date in history?"
 */
@Entity
@Table(name = "org_subscriptions", schema = "public", indexes = {
    @Index(name = "idx_org_subscriptions_org", columnList = "organisation_id"),
    @Index(name = "idx_org_subscriptions_status", columnList = "status"),
    @Index(name = "idx_org_subscriptions_end_at", columnList = "end_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrgSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "active"; // trialing, active, past_due, cancelled

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at") // null = current subscription
    private Instant endAt;

    @Column(name = "trial_end_at")
    private Instant trialEndAt;

    @Column(name = "price_at_time", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtTime;

    @Column(name = "billing_cycle_at_time", nullable = false, length = 20)
    private String billingCycleAtTime; // monthly, yearly

    @Column(name = "provider_customer_id", length = 120)
    private String providerCustomerId;

    @Column(name = "provider_subscription_id", length = 120)
    private String providerSubscriptionId;

    @Column(name = "provider_current_period_end")
    private Instant providerCurrentPeriodEnd;

    @Column(name = "dunning_attempt_count", nullable = false)
    @Builder.Default
    private Integer dunningAttemptCount = 1;

    @Column(name = "next_dunning_at")
    private Instant nextDunningAt;

    @Column(name = "last_payment_failed_at")
    private Instant lastPaymentFailedAt;

    @Column(name = "last_dunning_at")
    private Instant lastDunningAt;

    @Column(name = "notified_trial", nullable = false)
    @Builder.Default
    private Boolean notifiedTrial = false;

    @Column(name = "transaction_execution_id", length = 64)
    private String transactionExecutionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Current subscription: end_at is null. */
    public boolean isCurrent() {
        return endAt == null;
    }

    /** In trial window. */
    public boolean isTrialing() {
        return "trialing".equalsIgnoreCase(status)
            && trialEndAt != null
            && trialEndAt.isAfter(Instant.now());
    }
}
