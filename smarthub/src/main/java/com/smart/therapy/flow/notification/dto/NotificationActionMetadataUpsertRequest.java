package com.smart.therapy.flow.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationActionMetadataUpsertRequest {

    @NotBlank
    private String actionUrlTemplate;

    @NotBlank
    private String defaultActionLabel;

    private String exampleActionUrl;

    private Integer sortOrder;

    @NotNull
    private Boolean isActive;
}

