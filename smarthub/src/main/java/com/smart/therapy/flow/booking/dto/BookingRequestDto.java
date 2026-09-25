package com.smart.therapy.flow.booking.dto;

import com.smart.therapy.flow.booking.entity.BookingRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String practiceName;
    private String servicesOffered;
    private String practiceSize;
    private String country;
    private String address;
    private BookingRequestStatus status;
    private String statusMessage;
    private Instant createdAt;
}
