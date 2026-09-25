package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.auth.dto.UserStatus;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.user.entity.UserProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Staff user profile. Authentication is via AuthIdentity (auth_id).
 * Roles are via AuthIdentityRole, not direct on User.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_auth_id", columnList = "auth_id"),
        @Index(name = "idx_user_status_active", columnList = "status, is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uq_users_auth_id", columnNames = {"auth_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = { "profile", "sessions" })
@lombok.ToString(callSuper = true, exclude = { "authIdentity", "customRole", "profile", "sessions" })
public class User extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false, unique = true)
    private AuthIdentity authIdentity;

    @Column(nullable = false, length = 150)
    private String email; // unique per organisation (uq_users_org_email)

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_role_id")
    private Role customRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // Profile Information
    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String title;

    @Column(length = 100)
    private String department;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_picture", columnDefinition = "TEXT")
    private String profilePicture;

    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<UserSession> sessions = new HashSet<>();
}
