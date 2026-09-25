package com.smart.therapy.flow.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortalAccessRequest {

    @NotNull(message = "Enable flag is required")
    private Boolean enable;

    @Email(message = "Invalid email format")
    private String email;
}

