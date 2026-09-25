package com.smart.therapy.flow.cms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cms_media_asset", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsMediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "public_url", nullable = false, length = 1024)
    private String publicUrl;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "uploaded_by_auth_id")
    private Long uploadedByAuthId;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
