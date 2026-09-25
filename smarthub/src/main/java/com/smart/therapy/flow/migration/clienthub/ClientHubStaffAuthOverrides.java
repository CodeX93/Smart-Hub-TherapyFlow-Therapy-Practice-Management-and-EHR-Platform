package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffUserRef;

import java.util.Locale;

final class ClientHubStaffAuthOverrides {

    static final String SOURCE_USER_50_USERNAME = "amjed.abojedi.supervisor";
    static final String SOURCE_USER_50_ID = "50";

    private ClientHubStaffAuthOverrides() {
    }

    static String username(SourceStaffUserRef sourceUser) {
        if (SOURCE_USER_50_ID.equals(sourceUser.legacyUserId())) {
            return SOURCE_USER_50_USERNAME;
        }
        return sourceUser.username();
    }

    static String normalisedUsername(SourceStaffUserRef sourceUser) {
        String username = username(sourceUser);
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    static String usernameSqlExpression() {
        return "CASE WHEN id = 50 THEN '" + SOURCE_USER_50_USERNAME + "' ELSE username END";
    }
}
