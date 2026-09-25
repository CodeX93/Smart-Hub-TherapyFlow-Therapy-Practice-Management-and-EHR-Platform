package com.smart.therapy.flow.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "cms_landing_page", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsLandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_content", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String draftContent = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "published_content", columnDefinition = "jsonb")
    private String publishedContent;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "updated_by_auth_id")
    private Long updatedByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (draftContent == null) {
            draftContent = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
