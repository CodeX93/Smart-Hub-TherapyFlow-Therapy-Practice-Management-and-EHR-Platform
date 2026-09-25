package com.smart.therapy.flow.publicsite.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Marketing / public-site counseling services (not billing CPT services).
 * Booking uses CONSULTATION working hours; duration/price come from this row.
 */
@Entity
@Table(name = "public_site_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PublicSiteService extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /** System rows (e.g. Consultation) cannot be deleted. */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseRate = BigDecimal.ZERO;
}
