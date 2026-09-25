package com.smart.therapy.flow.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationTemplateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Template type is required")
    private String type; // email, in_app, sms, etc.

    private String eventType;
    private String subject;
    private String bodyTemplate;
    private Boolean isSystem;
    private Boolean isActive;
}
