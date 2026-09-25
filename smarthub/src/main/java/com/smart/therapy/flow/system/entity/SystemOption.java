package com.smart.therapy.flow.system.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "system_options", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_system_option_category_key", columnNames = {"category_id", "option_key"})
    },
    indexes = {
        @Index(name = "idx_system_option_category", columnList = "category_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SystemOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private OptionCategory category;

    @Column(name = "option_key", nullable = false, length = 100)
    private String optionKey; // "elementary", "high_school", etc.

    @Column(name = "option_label", nullable = false, length = 255)
    private String optionLabel; // "Elementary School", "High School", etc.

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO; // Price for service codes
}
