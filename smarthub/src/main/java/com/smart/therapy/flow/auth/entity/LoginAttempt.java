package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "login_attempts", schema = "public", indexes = {
    @Index(name = "idx_login_attempt_username", columnList = "username"),
    @Index(name = "idx_login_attempt_ip", columnList = "ip_address"),
    @Index(name = "idx_login_attempt_created", columnList = "createdat"),
    @Index(name = "idx_login_attempts_organisation_id", columnList = "organisation_id"),
    @Index(name = "idx_login_attempts_org_id", columnList = "organisation_id, id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class LoginAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_id")
    private AuthIdentity auth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_type", length = 50)
    private String deviceType; // 'mobile', 'desktop', 'tablet'

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "failure_reason", length = 100)
    private String failureReason; // 'invalid_password', 'account_locked', etc.
}
