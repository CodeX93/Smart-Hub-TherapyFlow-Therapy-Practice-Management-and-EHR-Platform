package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomName(String roomName);
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByIsActive(Boolean isActive);
}





