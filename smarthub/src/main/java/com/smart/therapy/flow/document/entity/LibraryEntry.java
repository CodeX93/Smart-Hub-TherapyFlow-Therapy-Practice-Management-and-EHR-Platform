package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "library_entries", indexes = {
    @Index(name = "idx_library_entry_category", columnList = "category_id"),
    @Index(name = "idx_library_entry_created_by", columnList = "created_by_id"),
    @Index(name = "idx_library_entry_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = { "category", "tags", "connectionsFrom", "connectionsTo", "createdByUser" })
public class LibraryEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private LibraryCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdByUser;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @OneToMany(mappedBy = "libraryEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LibraryEntryTag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "fromEntry", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LibraryEntryConnection> connectionsFrom = new ArrayList<>();

    @OneToMany(mappedBy = "toEntry", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LibraryEntryConnection> connectionsTo = new ArrayList<>();
}
