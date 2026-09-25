package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Billing notification delivery log response")
@Data
public class BillingNotificationLogResponse {
private Long logId;
private Long organisationId;
private String eventKey;
private String channel;
private String recipient;
private String status;
private String errorMessage;
private Instant createdAt;
}
