package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * UserContact - stores contact information for user profiles
 * (emergency contacts, primary contacts, etc.)
 */
@Entity
@Table(name = "user_contacts", indexes = {
    @Index(name = "idx_user_contact_profile_type", columnList = "user_profile_id, type"),
    @Index(name = "idx_user_contact_profile_email", columnList = "user_profile_id, email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"userProfile"})
public class UserContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String relationship; // 'spouse', 'parent', 'friend'

    @Column(nullable = false, length = 50)
    private String type; // 'emergency', 'primary', 'secondary'
}
