package com.smart.therapy.flow.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "cms_global_settings", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsGlobalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String content = "{}";

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
        if (content == null) {
            content = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
