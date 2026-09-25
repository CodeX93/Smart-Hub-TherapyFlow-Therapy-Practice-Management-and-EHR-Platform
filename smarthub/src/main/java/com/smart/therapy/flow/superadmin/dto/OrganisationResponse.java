package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Schema(description = "Organisation response (platform view)")
@Data
public class OrganisationResponse {
private Long id;
private String name;
private String slug;
private String status;
private String subdomain;
private String schemaName;
private Instant createdAt;
private Instant lastBackupAt;
private String backupStatus;
private String backupLocation;
private String timezone;
private String region;
private String dataResidency;
private String locale;
private String logoUrl;
private String brandPrimaryColor;
private String brandSecondaryColor;
private String brandAccentColor;
/** Primary administrator identity created at onboarding; not the tenant's support address. */
private String primaryAdminEmail;
private String supportEmail;
private String supportAddress;
private Instant terminationEffectiveAt;
private String planInfo;
private SuperAdminSubscriptionDetailsResponse subscriptionDetails;
private Long totalUserCount;
private Map<String, Object> extra;
}
