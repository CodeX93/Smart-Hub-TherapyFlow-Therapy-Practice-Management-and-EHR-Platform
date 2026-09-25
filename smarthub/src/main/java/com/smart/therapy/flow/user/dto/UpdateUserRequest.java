package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.smart.therapy.flow.common.validation.ValidPhoneNumber;

import java.util.Set;

@Data
@Schema(description = "Request to update an existing user. All fields are optional - only include fields you want to update.")
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Username (optional)", example = "john.doe", minLength = 3, maxLength = 50)
    private String username;

    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "User's full name (optional)", example = "John Doe", maxLength = 150)
    private String fullName;

    @Email(message = "Invalid email address")
    @Schema(description = "User's email address (optional)", example = "john.doe@therapyflow.pro")
    private String email;

    @ValidPhoneNumber
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Schema(description = "User's phone number (optional)", example = "+1-555-123-4567", maxLength = 20)
    private String phone;

    @Schema(description = "Whether the user account is active (optional)", example = "true")
    private Boolean active;

    @Schema(description = "Set of role names assigned to the user (optional)", example = "[\"THERAPIST\", \"SUPERVISOR\"]", allowableValues = {"ADMIN", "SUPERVISOR", "THERAPIST", "BILLING_SPECIALIST"})
    private Set<String> roles;
}


