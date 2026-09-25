package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Billing contact response")
@Data
public class BillingContactResponse {
private Long contactId;
private Long organisationId;
private String fullName;
private String email;
private Boolean primary;
private Boolean active;
private Instant createdAt;
private Instant updatedAt;
}
