package com.smart.therapy.flow.superadmin.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "platform_dashboard_tier_aliases",
        schema = "public",
        indexes = {
                @Index(name = "idx_platform_dashboard_tier_aliases_tier", columnList = "tier_name"),
                @Index(name = "idx_platform_dashboard_tier_aliases_plan", columnList = "plan_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PlatformDashboardTierAlias extends BaseEntity {

    @Column(name = "tier_name", nullable = false, length = 30)
    private String tierName;

    @Column(name = "plan_code", nullable = false, length = 100)
    private String planCode;
}
