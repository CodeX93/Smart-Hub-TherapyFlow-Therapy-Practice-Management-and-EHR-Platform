package com.smart.therapy.flow.session.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * SessionIntegration - stores integration data for sessions
 * (Zoom meetings, Google Meet, Teams, etc.)
 */
@Entity
@Table(name = "session_integrations", indexes = {
    @Index(name = "idx_session_integration_session_provider", columnList = "session_id, provider")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SessionIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, length = 50)
    private String provider; // 'zoom', 'google_meet', 'teams', etc.

    @Column(name = "meeting_id", length = 100)
    private String meetingId;

    @Column(name = "join_url", columnDefinition = "TEXT")
    private String joinUrl;

    @Column(length = 100)
    private String password;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode metadata; // Provider-specific data

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        java.time.Instant now = java.time.Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.Instant.now();
    }
}
