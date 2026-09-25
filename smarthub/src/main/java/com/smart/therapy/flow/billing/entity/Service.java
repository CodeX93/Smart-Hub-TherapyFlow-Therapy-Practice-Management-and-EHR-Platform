package com.smart.therapy.flow.billing.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "services", uniqueConstraints = {
    @UniqueConstraint(name = "uq_services_service_code", columnNames = {"service_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Service extends BaseEntity {

    @Column(name = "service_code", nullable = false, length = 50) // unique per org (uq_services_org_code)
    private String serviceCode; // CPT codes like "90834", "90837"

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName; // "Individual Psychotherapy 45 min"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer duration; // 45, 60, 90 minutes

    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRate;

    @Column(length = 100)
    private String category;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "therapist_visible", nullable = false)
    @Builder.Default
    private Boolean therapistVisible = true;

    @Column(name = "client_portal_visible", nullable = false)
    @Builder.Default
    private Boolean clientPortalVisible = true;

    /** When true, service is offered on the org public marketing site (e.g. Consultation). */
    @Column(name = "public_site_enabled", nullable = false)
    @Builder.Default
    private Boolean publicSiteEnabled = false;
}

