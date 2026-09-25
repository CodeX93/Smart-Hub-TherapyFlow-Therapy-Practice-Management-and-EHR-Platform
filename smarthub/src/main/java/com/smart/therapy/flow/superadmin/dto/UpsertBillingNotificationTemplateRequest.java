package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Billing notification template upsert request")
@Data
public class UpsertBillingNotificationTemplateRequest {
private String subjectTemplate;
private String bodyTemplate;
private Boolean active;
}
