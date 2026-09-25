package com.smart.therapy.flow.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderCategoryOptionsRequest {

    @NotEmpty(message = "Options list is required")
    @Valid
    private List<OptionOrderItem> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionOrderItem {
        @NotNull(message = "Option ID is required")
        private Long optionId;

        @NotNull(message = "Sort order is required")
        private Integer sortOrder;
    }
}
