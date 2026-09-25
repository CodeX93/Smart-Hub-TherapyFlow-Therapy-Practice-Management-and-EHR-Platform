package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.smart.therapy.flow.common.validation.ValidPhoneNumber;

import java.util.Set;

@Data
@Schema(description = "Request to create a new user (staff/admin/therapist)")
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Username for login (REQUIRED)", example = "john.doe", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 3, maxLength = 50)
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "User's full name (REQUIRED)", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    @Schema(description = "Password for login (REQUIRED, minimum 6 characters)", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Schema(description = "User's email address (REQUIRED)", example = "john.doe@therapyflow.pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @ValidPhoneNumber
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Schema(description = "User's phone number (optional)", example = "+1-555-123-4567", maxLength = 20)
    private String phone;

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "Set of role names assigned to the user (REQUIRED, at least one role)", example = "[\"THERAPIST\"]", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ADMIN", "SUPERVISOR", "THERAPIST", "BILLING_SPECIALIST"})
    private Set<String> roles;

    @Schema(description = "Whether the user account is active (optional, defaults to true)", example = "true", defaultValue = "true")
    private Boolean active;

    @Schema(description = "Idempotency key to prevent duplicate user creation (optional, UUID recommended)", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String idempotencyKey;
}


