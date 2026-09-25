package com.smart.therapy.flow.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "cms_learning_hub_article", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsLearningHubArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 220, unique = true)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_content", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String draftContent = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "published_content", columnDefinition = "jsonb")
    private String publishedContent;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "chapter", length = 200)
    private String chapter;

    @Column(name = "section_name", length = 200)
    private String sectionName;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "updated_by_auth_id")
    private Long updatedByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (draftContent == null) draftContent = "{}";
        if (sortOrder == null) sortOrder = 0;
        if (isFeatured == null) isFeatured = false;
        if (isPublished == null) isPublished = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
