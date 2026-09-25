package com.smart.therapy.flow.common.audit;

import com.smart.therapy.flow.client.entity.Client;
import org.springframework.util.StringUtils;

/**
 * HIPAA-safe labels for audit/notification surfaces.
 * Client actors must be identified by MRN only — never email or display name.
 */
public final class HipaaAuditLabels {

    private HipaaAuditLabels() {
    }

    /**
     * Username/actor label for a client portal action.
     * Prefers MRN ({@link Client#getClientId()}); never returns email or full name.
     */
    public static String clientActor(Client client) {
        if (client != null && StringUtils.hasText(client.getClientId())) {
            return client.getClientId();
        }
        return "client";
    }

    /**
     * Fallback when only a client id is known and the entity is unavailable.
     */
    public static String clientActorFallback() {
        return "client";
    }
}
