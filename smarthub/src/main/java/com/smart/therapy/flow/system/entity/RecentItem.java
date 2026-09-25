package com.smart.therapy.flow.system.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * RecentItem - tracks recently accessed items per user
 * Uses composite primary key (user_id, item_type, item_id) via EmbeddedId
 */
@Entity
@Table(name = "recent_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RecentItem {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private RecentItemKey id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_accessed_at", nullable = false)
    @Builder.Default
    private Instant lastAccessedAt = Instant.now();

    @Column(name = "access_count")
    private Integer accessCount;

    /**
     * Composite key for RecentItem
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentItemKey implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "item_type", length = 50)
        private String itemType;

        @Column(name = "item_id")
        private Long itemId;
    }
}
