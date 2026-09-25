package com.smart.therapy.flow.booking.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "booking_requests", indexes = {
        @Index(name = "idx_booking_req_status", columnList = "status"),
        @Index(name = "idx_booking_req_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingRequest extends BaseEntity {

    @Column(nullable = false, name = "first_name")
    private String firstName;

    @Column(nullable = false, name = "last_name")
    private String lastName;

    @Column(nullable = false, name = "email")
    private String email;

    @Column(nullable = false, name = "phone")
    private String phone;

    @Column(nullable = false, name = "practice_name")
    private String practiceName;

    @Column(nullable = false, name = "services_offered")
    private String servicesOffered;

    @Column(nullable = false, name = "practice_size")
    private String practiceSize;

    @Column(name = "country")
    private String country;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "status_message", length = 2000)
    private String statusMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private BookingRequestStatus status;

    @Column(nullable = false, name = "agree_to_terms")
    @Builder.Default
    private Boolean agreeToTerms = false;
}
