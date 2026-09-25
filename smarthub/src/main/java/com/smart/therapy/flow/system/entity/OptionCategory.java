package com.smart.therapy.flow.system.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "option_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class OptionCategory extends BaseEntity {

    @Column(name = "category_key", nullable = false, unique = true, length = 100)
    private String categoryKey; // "education_levels", "employment_status", etc.

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName; // "Education Levels", "Employment Status", etc.

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SystemOption> options = new ArrayList<>();
}
