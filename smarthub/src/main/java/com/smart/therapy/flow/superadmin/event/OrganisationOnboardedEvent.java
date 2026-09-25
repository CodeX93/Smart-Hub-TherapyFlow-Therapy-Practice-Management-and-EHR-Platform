package com.smart.therapy.flow.superadmin.event;

public record OrganisationOnboardedEvent(
        Long organisationId,
        String organisationName,
        String adminEmail,
        String adminName,
        String temporaryPassword,
        Long actorAuthId
) {
}
