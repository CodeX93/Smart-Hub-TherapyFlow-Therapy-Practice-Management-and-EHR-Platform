package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.RoomBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {
    List<RoomBooking> findByRoomId(Long roomId);
    List<RoomBooking> findBySessionId(Long sessionId);

    @Query("SELECT COUNT(b) FROM RoomBooking b WHERE b.room.id = :roomId")
    long countByRoomId(@Param("roomId") Long roomId);
}





