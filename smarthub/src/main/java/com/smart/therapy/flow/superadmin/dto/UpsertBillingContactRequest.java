package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Billing contact upsert request")
@Data
public class UpsertBillingContactRequest {
private String fullName;
private String email;
private Boolean primary;
private Boolean active;
}
