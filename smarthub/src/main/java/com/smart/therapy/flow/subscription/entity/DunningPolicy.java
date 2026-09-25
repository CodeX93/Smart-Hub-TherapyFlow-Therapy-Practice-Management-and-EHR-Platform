package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "dunning_policies", schema = "public", indexes = {
        @Index(name = "uq_dunning_policy_org", columnList = "organisation_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class DunningPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation; // null = global default policy

    @Column(name = "retry_1_hours", nullable = false)
    @Builder.Default
    private Integer retry1Hours = 24;

    @Column(name = "retry_2_hours", nullable = false)
    @Builder.Default
    private Integer retry2Hours = 72;

    @Column(name = "retry_3_hours", nullable = false)
    @Builder.Default
    private Integer retry3Hours = 120;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String stepsJson = "[]";

    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private Integer gracePeriodDays = 3;

    @Column(name = "auto_lock_on_cancel", nullable = false)
    @Builder.Default
    private Boolean autoLockOnCancel = true;

    @Column(name = "trial_notice_days_before", nullable = false)
    @Builder.Default
    private Integer trialNoticeDaysBefore = 7;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
