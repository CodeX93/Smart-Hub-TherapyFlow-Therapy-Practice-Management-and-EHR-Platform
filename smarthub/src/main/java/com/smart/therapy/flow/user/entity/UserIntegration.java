package com.smart.therapy.flow.user.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * UserIntegration - stores third-party integration credentials
 * (Zoom, Google Calendar, Slack, etc.)
 */
@Entity
@Table(name = "user_integrations", indexes = {
    @Index(name = "idx_user_integration_user_type", columnList = "user_id, integration_type"),
    @Index(name = "idx_user_integration_external_id", columnList = "external_user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"user"})
public class UserIntegration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "integration_type", nullable = false, length = 50)
    private String integrationType; // 'zoom', 'slack', 'google_calendar', 'stripe'

    @Column(name = "external_user_id", nullable = false, length = 255)
    private String externalUserId; // Zoom user ID, Slack user ID, etc.

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_type", length = 50)
    private String tokenType; // 'Bearer', 'OAuth', etc.

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "json")
    private JsonNode settings; // Integration-specific settings

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
