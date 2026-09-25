package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RolesPermissionsMatrixUpdateRequest {

    @NotEmpty
    @Valid
    private List<UpdateItem> updates;

    @Data
    public static class UpdateItem {
        @NotBlank
        private String roleName;

        @NotBlank
        private String permissionName;

        @NotNull
        private Boolean granted;
    }
}

