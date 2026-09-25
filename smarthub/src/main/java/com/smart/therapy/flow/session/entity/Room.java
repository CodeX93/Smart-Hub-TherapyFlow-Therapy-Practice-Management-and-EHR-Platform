package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.session.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Room extends BaseEntity {

    @Column(name = "room_number", nullable = false, length = 50) // unique per org (uq_rooms_org_number)
    private String roomNumber;

    @Column(name = "room_name", nullable = false, length = 255)
    private String roomName;

    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String equipment; // Equipment description as text

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    @Builder.Default
    private RoomType roomType = RoomType.PHYSICAL;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @Builder.Default
    private List<RoomBooking> bookings = new ArrayList<>();
}
