package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Billing notification template response")
@Data
public class BillingNotificationTemplateResponse {
private Long templateId;
private String eventKey;
private String subjectTemplate;
private String bodyTemplate;
private Boolean active;
private Instant updatedAt;
}
