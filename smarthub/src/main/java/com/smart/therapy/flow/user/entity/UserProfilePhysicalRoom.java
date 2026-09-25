package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.session.entity.Room;
import jakarta.persistence.*;
import lombok.*;

/**
 * Physical room assignments for in-person therapy sessions.
 */
@Entity
@Table(name = "user_profile_physical_rooms", indexes = {
        @Index(name = "idx_physical_room_profile", columnList = "user_profile_id"),
        @Index(name = "idx_physical_room_room", columnList = "room_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfilePhysicalRoom extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false; // Primary/default room

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
