package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "library_categories", indexes = {
    @Index(name = "idx_library_category_parent", columnList = "parent_category_id"),
    @Index(name = "idx_library_category_parent_name", columnList = "parent_category_id, name", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = { "parentCategory", "subcategories", "entries" })
public class LibraryCategory extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private LibraryCategory parentCategory;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LibraryCategory> subcategories = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LibraryEntry> entries = new ArrayList<>();
}
