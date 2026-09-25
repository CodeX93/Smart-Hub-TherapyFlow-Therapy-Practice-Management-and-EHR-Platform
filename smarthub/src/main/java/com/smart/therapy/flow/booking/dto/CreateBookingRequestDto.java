package com.smart.therapy.flow.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequestDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Practice/Hospital name is required")
    private String practiceName;

    @NotBlank(message = "Services offered is required")
    private String servicesOffered;

    @NotBlank(message = "Practice size is required")
    private String practiceSize;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Address is required")
    private String address;

    @jakarta.validation.constraints.AssertTrue(message = "You must agree to the terms")
    private Boolean agreeToTerms;
}
