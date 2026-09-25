package com.smart.therapy.flow.payment.dto;

import com.smart.therapy.flow.organisation.entity.OrgStripeOnboardingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrgStripeConnectStatusResponse {
    private Long organisationId;
    private String connectAccountId;
    private OrgStripeOnboardingStatus onboardingStatus;
    private Boolean chargesEnabled;
    private Boolean payoutsEnabled;
    private Boolean detailsSubmitted;
    private String country;
    private String defaultCurrency;
    private Instant lastSyncedAt;
    private String disabledReason;
    /** Stripe requirement keys still past due (e.g. external_account, individual.id_number). */
    private List<String> pastDueRequirements;
    /** Stripe requirement keys due now or soon. */
    private List<String> currentlyDueRequirements;
}
