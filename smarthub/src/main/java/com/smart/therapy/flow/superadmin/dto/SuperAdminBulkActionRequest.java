package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SuperAdminBulkActionRequest {
    @NotBlank
    @Size(max = 32)
    private String action;

    @NotEmpty
    private List<Long> organisationIds;

    @Size(max = 500)
    private String reason;
}
