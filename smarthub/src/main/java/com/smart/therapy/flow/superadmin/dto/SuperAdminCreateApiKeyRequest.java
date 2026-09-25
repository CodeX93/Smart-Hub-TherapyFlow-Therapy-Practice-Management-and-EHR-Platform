package com.smart.therapy.flow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SuperAdminCreateApiKeyRequest {
    @NotBlank
    @Size(min = 2, max = 80)
    @JsonProperty("name")
    @JsonAlias("keyName")
    private String keyName;

    @NotEmpty
    private List<String> scopes;

    private Instant expiresAt;
}
