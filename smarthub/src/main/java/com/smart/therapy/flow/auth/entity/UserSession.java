package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_session_user_active", columnList = "user_id, is_active"),
    @Index(name = "idx_user_session_expires", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token", nullable = false, unique = true, length = 255)
    private String sessionToken;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_activity", nullable = false)
    @Builder.Default
    private Instant lastActivity = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;
}
