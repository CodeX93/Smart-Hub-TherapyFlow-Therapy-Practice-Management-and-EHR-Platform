package com.smart.therapy.flow.organisation.entity;

/**
 * Organisation tier for plan enforcement, UI toggles, SSO availability, feature gating.
 * INDIVIDUAL = single therapist; CLINIC = practice; ENTERPRISE = large org.
 */
public enum OrganisationType {
    INDIVIDUAL,
    CLINIC,
    ENTERPRISE
}
