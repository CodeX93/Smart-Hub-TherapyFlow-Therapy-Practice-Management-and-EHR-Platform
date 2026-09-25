package com.smart.therapy.flow.auth.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class LoginContextResponse {
    String email;
    String orgSlug;
    Long organisationId;
    String organisationName;
    Branding branding;
    List<String> ssoProviders;
    List<OrgSummary> organisations;
    int count;
    Instant resolvedAt;

    @Value
    @Builder
    public static class OrgSummary {
        Long organisationId;
        String name;
        String slug;
        String subdomain;
        String status;
        Branding branding;
        /** Role names for this email in this organisation (e.g. ADMIN, THERAPIST). */
        List<String> roles;
        /**
         * Login username ({@code auth_identities.login_identifier}) for this org membership.
         * May differ from the profile email when the user has separate identities per org.
         */
        String username;
    }

    @Value
    @Builder
    public static class Branding {
        String logoUrl;
        String brandPrimaryColor;
        String brandSecondaryColor;
        String brandAccentColor;
    }
}

