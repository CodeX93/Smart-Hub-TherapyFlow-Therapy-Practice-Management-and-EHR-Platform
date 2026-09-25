package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Replace dashboard tier aliases request")
public class SuperAdminDashboardTierAliasesReplaceRequest {

    @Valid
    @NotEmpty
    @Schema(description = "Complete alias list to persist (replace-all)")
    private List<Item> aliases;

    @Getter
    @Setter
    @Schema(description = "Tier alias item")
    public static class Item {
        @NotBlank
        @Schema(description = "Tier name", example = "Pro")
        private String tierName;

        @NotBlank
        @Schema(description = "Plan code alias", example = "PROFESSIONAL")
        private String planCode;
    }
}
