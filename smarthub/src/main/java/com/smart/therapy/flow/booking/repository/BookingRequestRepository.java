package com.smart.therapy.flow.booking.repository;

import com.smart.therapy.flow.booking.entity.BookingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {
    boolean existsByEmailAndStatusIn(String email, java.util.List<com.smart.therapy.flow.booking.entity.BookingRequestStatus> statuses);
}
